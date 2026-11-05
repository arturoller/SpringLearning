package s08_data_jpa.jpa02_repositories;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy sekcji 4 na PEŁNEJ aplikacji lekcji (zwykły JUnit, każdy test z własną bazą). Nie {@code @DataJpaTest}:
 * tam test sam jest jedną transakcją i encja byłaby zarządzana przez cały test — nie zobaczylibyśmy, że bez
 * {@code @Transactional} w serwisie encja po findById jest już odłączona. Tu nikt nie trzyma transakcji „z zewnątrz”.
 */
class Jpa02ServiceTransactionTest {

    private ConfigurableApplicationContext app;
    private ProductService service;
    private JdbcClient jdbc;

    @BeforeEach
    void start() {
        app = new SpringApplicationBuilder(Jpa02Application.class)
                .web(WebApplicationType.NONE)
                .run("--spring.profiles.active=jpa02",
                        "--spring.datasource.url=jdbc:h2:mem:jpa02-" + UUID.randomUUID());
        service = app.getBean(ProductService.class);
        jdbc = app.getBean(JdbcClient.class);
    }

    @AfterEach
    void stop() {
        app.close();
    }

    private BigDecimal priceInDatabase(long id) {
        return jdbc.sql("SELECT price FROM product WHERE id = :id").param("id", id).query(BigDecimal.class).single();
    }

    @Test
    @DisplayName("Sekcja 4: SimpleJpaRepository jest @Transactional(readOnly = true), a metody zapisu mają zwykłe @Transactional")
    void section4RepositoryTransactions() throws Exception {
        Transactional onClass = SimpleJpaRepository.class.getAnnotation(Transactional.class);
        assertThat(onClass.readOnly()).as("domyślnie dla wszystkich metod — odczyty").isTrue();
        Transactional onSave = SimpleJpaRepository.class.getMethod("save", Object.class).getAnnotation(Transactional.class);
        assertThat(onSave.readOnly()).as("save nadpisuje: transakcja do zapisu").isFalse();
        assertThat(SimpleJpaRepository.class.getMethod("deleteById", Object.class).getAnnotation(Transactional.class)).isNotNull();
        assertThat(AopUtils.isAopProxy(service)).as("serwis z @Transactional to proxy").isTrue();
    }

    @Test
    @DisplayName("Sekcja 4: serwis @Transactional — zmiana encji z findById trafia do bazy bez save (dirty checking)")
    void section4TransactionalService() {
        service.changePrice(2L, new BigDecimal("45.00"));

        assertThat(priceInDatabase(2)).isEqualByComparingTo("45.00");
    }

    @Test
    @DisplayName("Sekcja 4: bez @Transactional encja po findById jest odłączona — zmiana NIE trafia do bazy")
    void section4WithoutTransaction() {
        service.changePriceWithoutTransaction(2L, new BigDecimal("45.00"));

        assertThat(priceInDatabase(2)).isEqualByComparingTo("59.00");
    }

    @Test
    @DisplayName("Sekcja 4: w transakcji readOnly = true zmiana encji też przepada (brak flush)")
    void section4ReadOnlyTransaction() {
        service.changePriceInReadOnlyTransaction(2L, new BigDecimal("45.00"));

        assertThat(priceInDatabase(2)).isEqualByComparingTo("59.00");
    }
}
