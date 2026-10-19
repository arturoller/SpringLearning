package s09_jpa_relations.solutions.rel05_n_plus_one;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import s09_jpa_relations.rel05_n_plus_one.Customer;
import s09_jpa_relations.rel05_n_plus_one.OrderLineCount;
import s09_jpa_relations.rel05_n_plus_one.Rel05Application;
import s09_jpa_relations.rel05_n_plus_one.ShopOrder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Rel05NPlusOneExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Pakiet solutions nie ma klasy startowej, więc wskazujemy ją wprost: {@code @ContextConfiguration(classes = ...)}.
 */
@Tag("wzorzec")
@DataJpaTest
@ContextConfiguration(classes = Rel05Application.class)
@ActiveProfiles("rel05")
class Rel05NPlusOneSolutionsTest {

    @Autowired
    EntityManager em;

    @Autowired
    EntityManagerFactory emf;

    Statistics stats;

    @BeforeEach
    void clearStatistics() {
        stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
    }

    @Test
    @DisplayName("Wzorzec 1: zamówienia o statusie razem z klientami — 1 zapytanie mimo pętli po klientach")
    void solution1() {
        List<ShopOrder> result = Rel05NPlusOneSolutions.solution1(em, "NOWE");
        List<String> names = result.stream().map(order -> order.getCustomer().getName()).toList();

        assertThat(result).extracting(ShopOrder::getNumber).containsExactly("ZAM-001", "ZAM-005");
        assertThat(names).containsExactly("Jan Kowalski", "Jan Kowalski");
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Wzorzec 2: liczba pozycji każdego zamówienia — projekcja DTO, 1 zapytanie, 0 encji")
    void solution2() {
        List<OrderLineCount> counts = Rel05NPlusOneSolutions.solution2(em);

        assertThat(counts).hasSize(6).contains(new OrderLineCount("ZAM-003", 3), new OrderLineCount("ZAM-006", 1));
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        assertThat(stats.getEntityLoadCount()).isZero();
    }

    @Test
    @DisplayName("Wzorzec 3: strona zamówień z pozycjami w 2 zapytaniach (najpierw id, potem join fetch)")
    void solution3() {
        List<ShopOrder> page = Rel05NPlusOneSolutions.solution3(em, 1, 2);

        assertThat(page).extracting(ShopOrder::getNumber).containsExactly("ZAM-003", "ZAM-004");
        assertThat(page).allMatch(order -> Hibernate.isInitialized(order.getLines()));
        assertThat(page.getFirst().getLines()).hasSize(3);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
        assertThat(stats.getEntityLoadCount()).as("tylko 2 zamówienia + 4 ich pozycje").isEqualTo(6);
    }

    @Test
    @DisplayName("Wzorzec 4: klienci z zamówieniami i pozycjami w 2 zapytaniach (bez MultipleBagFetchException)")
    void solution4() {
        List<Customer> result = Rel05NPlusOneSolutions.solution4(em);

        assertThat(result).extracting(Customer::getName).containsExactly(
                "Adam Mazur", "Jan Kowalski", "Maria Nowak", "Ola Pawlak", "Zofia Krawczyk");
        assertThat(result).allMatch(customer -> Hibernate.isInitialized(customer.getOrders()));
        assertThat(result).flatExtracting(Customer::getOrders)
                .allMatch(order -> Hibernate.isInitialized(order.getLines()));
        assertThat(result.get(1).getOrders()).as("Jan ma 2 zamówienia — bez powtórzeń").hasSize(2);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
    }
}
