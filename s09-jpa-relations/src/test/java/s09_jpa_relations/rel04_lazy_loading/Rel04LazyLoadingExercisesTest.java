package s09_jpa_relations.rel04_lazy_loading;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import s09_jpa_relations.SqlCapture;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Rel04LazyLoading (LazyLoadingExercises). @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * {@code @Transactional(propagation = NOT_SUPPORTED)} wyłącza transakcję testu (sekcja 2 lekcji): wyniki Twoich
 * metod są używane POZA transakcją, jak w prawdziwej aplikacji. Testy tylko czytają dane, więc nic nie trzeba wycofywać.
 */
@Tag("cwiczenie")
@DataJpaTest(showSql = false, properties = SqlCapture.PROPERTY)
@ActiveProfiles("rel04")
@Import(LazyLoadingExercises.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class Rel04LazyLoadingExercisesTest {

    @Autowired
    private LazyLoadingExercises exercises;

    @BeforeEach
    void setUp() {
        SqlCapture.clear();
    }

    @Test
    @DisplayName("Ćwiczenie 1: nazwisko klienta zamówienia — leniwy klient czytany w transakcji serwisu")
    void exercise1() {
        assertThat(exercises.customerNameOfOrder(3L)).isEqualTo("Jan Kowalski");
        assertThat(exercises.customerNameOfOrder(1L)).isEqualTo("Anna Nowak");
    }

    @Test
    @DisplayName("Ćwiczenie 2: zamówienie z pozycjami i produktami jednym zapytaniem — działa poza transakcją")
    void exercise2() {
        ShopOrder order = exercises.orderWithLinesAndProducts(3L);

        assertThat(order.getLines()).extracting(line -> line.getProduct().getName()).containsExactly("Kubek", "Mysz");
        assertThat(SqlCapture.all()).as("jedno zapytanie z join fetch").hasSize(1);
    }

    @Test
    @DisplayName("Ćwiczenie 3: wszystkie zamówienia z klientami przez @EntityGraph — 1 zapytanie")
    void exercise3() {
        List<ShopOrder> all = exercises.allOrdersWithCustomers();

        assertThat(all).extracting(ShopOrder::getOrderNumber).containsExactly("ZAM-001", "ZAM-002", "ZAM-003");
        assertThat(all).extracting(order -> order.getCustomer().getName())
                .containsExactly("Anna Nowak", "Anna Nowak", "Jan Kowalski");
        assertThat(SqlCapture.all()).hasSize(1);
    }

    @Test
    @DisplayName("Ćwiczenie 4: klient ze zbiorem zamówień zainicjalizowanym w transakcji (Hibernate.initialize)")
    void exercise4() {
        Customer anna = exercises.customerWithOrders(1L);
        assertThat(Hibernate.isInitialized(anna.getOrders())).isTrue();
        assertThat(anna.getOrders()).extracting(ShopOrder::getOrderNumber).containsExactlyInAnyOrder("ZAM-001", "ZAM-002");
        assertThat(SqlCapture.all()).as("klient + zamówienia").hasSize(2);

        Customer ewa = exercises.customerWithOrders(3L);
        assertThat(Hibernate.isInitialized(ewa.getOrders())).isTrue();
        assertThat(ewa.getOrders()).isEmpty();
    }
}
