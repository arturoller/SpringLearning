package s09_jpa_relations.solutions.rel04_lazy_loading;

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
import s09_jpa_relations.rel04_lazy_loading.Customer;
import s09_jpa_relations.rel04_lazy_loading.OrderService;
import s09_jpa_relations.rel04_lazy_loading.Rel04Application;
import s09_jpa_relations.rel04_lazy_loading.ShopOrder;
import s09_jpa_relations.rel04_lazy_loading.ShopOrderRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Rel04LazyLoadingExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Pakiet solutions nie ma klasy startowej, więc wskazujemy ją wprost: {@code @SpringBootTest(classes = ...)}.
 * Testy NIE są transakcyjne — dokładnie tak jak kod, który woła serwis z kontrolera.
 */
@Tag("wzorzec")
@SpringBootTest(classes = Rel04Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("rel04")
class Rel04LazyLoadingSolutionsTest {

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
    @DisplayName("Wzorzec 1: nazwa klienta zamówienia — praca w transakcji, bez LazyInitializationException")
    void solution1() {
        assertThat(Rel04LazyLoadingSolutions.solution1(tx, orders, "ZAM-002")).isEqualTo("Maria Nowak");
    }

    @Test
    @DisplayName("Wzorzec 2: zamówienie z klientem i pozycjami jednym zapytaniem — oba pola gotowe po transakcji")
    void solution2() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        ShopOrder order = tx.execute(status -> Rel04LazyLoadingSolutions.solution2(em, "ZAM-001"));

        assertThat(stats.getPrepareStatementCount()).as("dokładnie jedno zapytanie").isEqualTo(1);
        assertThat(Hibernate.isInitialized(order.getCustomer())).isTrue();
        assertThat(Hibernate.isInitialized(order.getLines())).isTrue();
        assertThat(order.getCustomer().getName()).isEqualTo("Jan Kowalski");
        assertThat(order.getLines()).hasSize(2);
    }

    @Test
    @DisplayName("Wzorzec 3: prawdziwa klasa encji bez inicjalizowania proxy (poza sesją)")
    void solution3() {
        Customer proxy = orderService.findOrder("ZAM-001").getCustomer();   // sesja zamknięta

        assertThat(Rel04LazyLoadingSolutions.solution3(proxy)).isEqualTo(Customer.class);
        assertThat(Hibernate.isInitialized(proxy)).as("proxy nadal niezainicjowane").isFalse();
        assertThat(Rel04LazyLoadingSolutions.solution3("zwykły obiekt")).isEqualTo(String.class);
    }
}
