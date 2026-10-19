package s09_jpa_relations.rel04_lazy_loading;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Rel04LazyLoading. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Testy NIE są transakcyjne — dokładnie tak jak kod, który woła serwis z kontrolera.
 */
@Tag("cwiczenie")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("rel04")
class Rel04LazyLoadingExercisesTest {

    @Autowired
    TransactionTemplate tx;

    @Autowired
    ShopOrderRepository orders;

    @Autowired
    OrderService orderService;

    @Autowired
    EntityManagerFactory emf;

    @PersistenceContext
    EntityManager em;

    @Test
    @DisplayName("Ćwiczenie 1: nazwa klienta zamówienia — praca w transakcji, bez LazyInitializationException")
    void exercise1() {
        assertThat(Rel04LazyLoading.exercise1(tx, orders, "ZAM-002")).isEqualTo("Maria Nowak");
    }

    @Test
    @DisplayName("Ćwiczenie 2: zamówienie z klientem i pozycjami jednym zapytaniem — oba pola gotowe po transakcji")
    void exercise2() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        ShopOrder order = tx.execute(status -> Rel04LazyLoading.exercise2(em, "ZAM-001"));

        assertThat(stats.getPrepareStatementCount()).as("dokładnie jedno zapytanie").isEqualTo(1);
        assertThat(Hibernate.isInitialized(order.getCustomer())).isTrue();
        assertThat(Hibernate.isInitialized(order.getLines())).isTrue();
        assertThat(order.getCustomer().getName()).isEqualTo("Jan Kowalski");
        assertThat(order.getLines()).hasSize(2);
    }

    @Test
    @DisplayName("Ćwiczenie 3: prawdziwa klasa encji bez inicjalizowania proxy (poza sesją)")
    void exercise3() {
        Customer proxy = orderService.findOrder("ZAM-001").getCustomer();   // sesja zamknięta

        assertThat(Rel04LazyLoading.exercise3(proxy)).isEqualTo(Customer.class);
        assertThat(Hibernate.isInitialized(proxy)).as("proxy nadal niezainicjowane").isFalse();
        assertThat(Rel04LazyLoading.exercise3("zwykły obiekt")).isEqualTo(String.class);
    }
}
