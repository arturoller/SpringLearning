package s08_data_jpa.jpa02_repositories;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jpa02Repositories. {@code @DataJpaTest} — tylko warstwa JPA: encje, repozytoria, Hibernate, baza H2
 * (podmieniona na wbudowaną bazę testową) i TestEntityManager. Każdy test w transakcji wycofywanej na końcu.
 */
@DataJpaTest
@ActiveProfiles("jpa02")
class Jpa02RepositoriesTest {

    @Autowired
    CustomerRepository customers;

    @Autowired
    ProductRepository products;

    @Autowired
    TestEntityManager em;

    @Test
    @DisplayName("1. Repozytorium to proxy (JDK) nad SimpleJpaRepository i ma całą rodzinę interfejsów")
    void repositoryIsProxyOverSimpleJpaRepository() {
        assertThat(AopUtils.isJdkDynamicProxy(customers)).isTrue();
        assertThat(AopProxyUtils.ultimateTargetClass(customers)).isEqualTo(SimpleJpaRepository.class);
        assertThat(customers).isInstanceOf(JpaRepository.class)
                .isInstanceOf(ListCrudRepository.class)
                .isInstanceOf(CrudRepository.class)
                .isInstanceOf(PagingAndSortingRepository.class);
        assertThat(products).isInstanceOf(ListCrudRepository.class)
                .isNotInstanceOf(JpaRepository.class);
    }

    @Test
    @DisplayName("2. CRUD bez jednej linii SQL: saveAll, findAll, count, existsById, findAllById")
    void crudMethods() {
        List<Customer> saved = customers.saveAll(List.of(
                new Customer("anna@sklep.pl", "Anna"),
                new Customer("piotr@sklep.pl", "Piotr")));
        Long annaId = saved.get(0).getId();

        assertThat(customers.count()).isEqualTo(2);
        assertThat(customers.existsById(annaId)).isTrue();
        assertThat(customers.findAll()).extracting(Customer::getName).containsExactlyInAnyOrder("Anna", "Piotr");
        assertThat(customers.findAllById(List.of(annaId))).extracting(Customer::getEmail)
                .containsExactly("anna@sklep.pl");
    }

    @Test
    @DisplayName("3. save nowej encji (id == null) → persist: zwraca TEN SAM obiekt, już z id")
    void saveNewEntityPersists() {
        Customer anna = new Customer("anna@sklep.pl", "Anna");

        Customer returned = customers.save(anna);

        assertThat(returned).isSameAs(anna);
        assertThat(anna.getId()).isNotNull();
    }

    @Test
    @DisplayName("3. save odłączonej encji (id != null) → merge: zwraca INNY obiekt — zarządzaną kopię")
    void saveDetachedEntityMerges() {
        Customer detached = customers.saveAndFlush(new Customer("ola@sklep.pl", "Ola"));
        em.clear();                                       // ola jest teraz odłączona
        detached.setName("Aleksandra");

        Customer managed = customers.save(detached);

        assertThat(managed).isNotSameAs(detached);
        assertThat(em.getEntityManager().contains(managed)).isTrue();
        assertThat(em.getEntityManager().contains(detached)).isFalse();
        assertThat(managed.getName()).isEqualTo("Aleksandra");
    }

    @Test
    @DisplayName("3. PUŁAPKA: ręcznie nadane id → save zawsze robi merge (SELECT + INSERT) i zwraca kopię")
    void assignedIdAlwaysMerges() {
        Product kawa = new Product("KAWA-001", "Kawa", new BigDecimal("29.99"));

        Product returned = products.save(kawa);          // w logu: select ... where sku=?, potem insert

        assertThat(returned).isNotSameAs(kawa);
        assertThat(products.findById("KAWA-001")).isPresent();
    }

    @Test
    @DisplayName("4. findById zwraca Optional — pusty, gdy wiersza nie ma")
    void findByIdReturnsOptional() {
        Long id = customers.save(new Customer("ewa@sklep.pl", "Ewa")).getId();

        Optional<Customer> found = customers.findById(id);
        Optional<Customer> missing = customers.findById(-1L);

        assertThat(found).map(Customer::getName).contains("Ewa");
        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("4. getReferenceById dla brakującego id — wyjątek dopiero przy dotknięciu pola")
    void getReferenceByIdFailsLazily() {
        Customer reference = customers.getReferenceById(-1L);   // brak SELECT, brak wyjątku — tylko pośrednik

        assertThatThrownBy(reference::getName)
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("No row with the given identifier exists");
    }

    @Test
    @DisplayName("5. deleteById na nieistniejącym id NIE rzuca wyjątku (Spring Data 3+; w 2.x był wyjątek)")
    void deleteByIdOnMissingIdIsSilent() {
        assertThatCode(() -> customers.deleteById(-1L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("5. deleteById istniejącego klienta — wiersz znika")
    void deleteByIdRemovesRow() {
        Long id = customers.save(new Customer("jan@sklep.pl", "Jan")).getId();

        customers.deleteById(id);
        customers.flush();

        assertThat(customers.existsById(id)).isFalse();
    }

    @Test
    @DisplayName("6. @DataJpaTest: test biegnie w transakcji oznaczonej do wycofania (rollback)")
    void testRunsInRolledBackTransaction() {
        customers.save(new Customer("tymczasowy@sklep.pl", "Tymczasowy"));

        assertThat(TestTransaction.isActive()).isTrue();
        assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
    }

    @Test
    @DisplayName("6. Błąd bazy przez repozytorium → DataIntegrityViolationException (tłumaczenie wyjątków Springa)")
    void repositoryTranslatesExceptions() {
        customers.saveAndFlush(new Customer("anna@sklep.pl", "Anna"));

        assertThatThrownBy(() -> customers.saveAndFlush(new Customer("anna@sklep.pl", "Druga Anna")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
