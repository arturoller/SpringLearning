package s08_data_jpa.jpa02_repositories;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jpa02Repositories (sekcje 1, 2, 3 i 5) na wycinku {@code @DataJpaTest}. Wycinek zawiera encje
 * i repozytoria Spring Data, ale NIE serwisy — ProductService dokładamy przez {@code @Import} (sekcja 5).
 * Baza: wycinek podmienia DataSource na wbudowaną H2 z losową nazwą, a Flyway lekcji (profil jpa02) i tak na niej
 * działa — tabela i dane pochodzą z migracji. Każdy test jest wycofywany (rollback).
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("jpa02")
@Import(ProductService.class)
class Jpa02RepositoriesTest {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private ProductService service;

    @Autowired
    private TestEntityManager tem;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private EntityManagerFactory emf;

    private Statistics stats;

    @BeforeEach
    void setUp() {
        stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
    }

    private static Product notebook() {
        return new Product("Zeszyt", Category.OFFICE, new BigDecimal("4.50"), 40, Instant.parse("2026-10-01T08:00:00Z"));
    }

    private BigDecimal priceInDatabase(long id) {
        return jdbc.sql("SELECT price FROM product WHERE id = :id").param("id", id).query(BigDecimal.class).single();
    }

    // ---- 1 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 1: bean repozytorium to proxy JDK z interfejsem ProductRepository, a pracę wykonuje SimpleJpaRepository")
    void section1RepositoryIsProxy() throws Exception {
        assertThat(Proxy.isProxyClass(repository.getClass())).as("klasa wygenerowana w locie").isTrue();
        assertThat(AopUtils.isAopProxy(repository)).isTrue();
        Object target = ((Advised) repository).getTargetSource().getTarget(); // advised = obiekt z poradami AOP
        assertThat(target).isInstanceOf(SimpleJpaRepository.class);
    }

    @Test
    @DisplayName("Sekcja 1: metody z JpaRepository działają bez implementacji, findAll zwraca List (ListCrudRepository)")
    void section1InheritedMethods() {
        List<Product> all = repository.findAll(); // List, a nie Iterable — dzięki ListCrudRepository
        assertThat(all).hasSize(5);
        assertThat(repository.count()).isEqualTo(5);
    }

    @Test
    @DisplayName("Sekcja 1: metamodel lekcji zawiera tylko jej encję — Product z Jpa01 do niego nie trafia")
    void section1OnlyLessonEntities() {
        assertThat(emf.getMetamodel().getEntities()).extracting(type -> (Object) type.getJavaType())
                .containsExactly(Product.class)
                .doesNotContain(s08_data_jpa.jpa01_entities.Product.class);
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: save nowej encji (id == null) robi persist, nadaje id i zwraca TEN SAM obiekt")
    void section2SaveNew() {
        Product product = notebook();

        Product saved = repository.save(product);

        assertThat(saved).isSameAs(product);
        assertThat(saved.getId()).isGreaterThan(5L);
        assertThat(stats.getEntityInsertCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Sekcja 2: save odłączonej encji (id != null) robi merge: SELECT, potem UPDATE, i zwraca INNY obiekt")
    void section2SaveDetachedMerges() {
        Product mouse = repository.findById(2L).orElseThrow();
        tem.detach(mouse);
        mouse.setPrice(new BigDecimal("49.00"));
        stats.clear();

        Product saved = repository.save(mouse);
        tem.flush();

        assertThat(saved).as("merge zwraca zarządzaną kopię").isNotSameAs(mouse);
        assertThat(stats.getEntityLoadCount()).as("merge wczytał wiersz (SELECT)").isEqualTo(1);
        assertThat(stats.getEntityUpdateCount()).isEqualTo(1);
        assertThat(priceInDatabase(2)).isEqualByComparingTo("49.00");
    }

    @Test
    @DisplayName("Sekcja 2: PUŁAPKA — ręczne id przy @GeneratedValue: merge, SELECT i ObjectOptimisticLockingFailureException")
    void section2ManualIdTrap() {
        Product product = notebook();
        product.setId(777L); // „chcę, żeby miał numer 777”

        assertThatThrownBy(() -> repository.save(product))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class)
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasCauseInstanceOf(StaleObjectStateException.class) // oryginalny wyjątek Hibernate
                .hasMessageContaining("Row was already updated or deleted by another transaction");
        assertThat(stats.getPrepareStatementCount()).as("same SELECT-y szukające wiersza 777 (Hibernate 7.4: dwa)").isEqualTo(2);
        assertThat(stats.getEntityInsertCount()).as("żadnego INSERT").isZero();
    }

    @Test
    @DisplayName("Sekcja 2: PUŁAPKA — ręczne id ISTNIEJĄCEGO wiersza: save po cichu nadpisuje cały wiersz")
    void section2ManualIdOverwrites() {
        Product product = notebook();
        product.setId(2L); // to numer Myszy!

        repository.save(product);
        tem.flush();
        tem.clear();

        assertThat(repository.findById(2L)).get().extracting(Product::getName).isEqualTo("Zeszyt");
        assertThat(repository.count()).as("nic nie dodano — Mysz zniknęła").isEqualTo(5);
    }

    // ---- 3 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 3: findById zwraca Optional; existsById i count to pojedyncze zapytania")
    void section3FindExistsCount() {
        assertThat(repository.findById(2L)).get().extracting(Product::getName).isEqualTo("Mysz");
        assertThat(repository.findById(999L)).isEmpty();

        stats.clear();
        assertThat(repository.existsById(4L)).isTrue();
        assertThat(repository.existsById(999L)).isFalse();
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
        assertThat(stats.getEntityLoadCount()).as("existsById nie wczytuje encji").isZero();
    }

    @Test
    @DisplayName("Sekcja 3: getReferenceById daje proxy bez zapytania; dla brakującego id wyjątek dopiero przy użyciu")
    void section3GetReferenceById() {
        Product reference = repository.getReferenceById(3L);
        assertThat(Hibernate.isInitialized(reference)).isFalse();
        assertThat(stats.getPrepareStatementCount()).as("bez SELECT").isZero();
        assertThat(reference.getName()).isEqualTo("Monitor");
        assertThat(stats.getPrepareStatementCount()).as("SELECT przy pierwszym użyciu").isEqualTo(1);

        Product missing = repository.getReferenceById(999L); // jeszcze bez błędu
        assertThatThrownBy(missing::getName)
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("No row with the given identifier exists");
    }

    @Test
    @DisplayName("Sekcja 3: deleteById usuwa wiersz, a dla nieistniejącego id NIC nie robi (bez wyjątku)")
    void section3DeleteById() {
        repository.deleteById(4L);
        repository.deleteById(999L); // Spring Data 3+: po cichu ignoruje
        tem.flush();

        assertThat(repository.count()).isEqualTo(4);
        assertThat(repository.existsById(4L)).isFalse();
    }

    // ---- 5 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 5: serwis z @Import w wycinku — zmiana bez save widoczna w bazie dopiero po flush")
    void section5ServiceInSlice() {
        service.changePrice(2L, new BigDecimal("45.00"));

        assertThat(priceInDatabase(2)).as("transakcja testu trwa, flush jeszcze nie było").isEqualByComparingTo("59.00");
        tem.flush();
        assertThat(priceInDatabase(2)).isEqualByComparingTo("45.00");
    }

    @Test
    @DisplayName("Sekcja 5: wycinek uruchomił migracje Flyway lekcji (V1 i V2) na podmienionej bazie")
    void section5FlywayRanInSlice() {
        assertThat(jdbc.sql("SELECT \"version\" FROM \"flyway_schema_history\" WHERE \"version\" IS NOT NULL ORDER BY \"installed_rank\"")
                .query(String.class).list()).containsExactly("1", "2");
    }
}
