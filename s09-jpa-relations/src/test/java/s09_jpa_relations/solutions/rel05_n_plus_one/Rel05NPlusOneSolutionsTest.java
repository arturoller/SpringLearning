package s09_jpa_relations.solutions.rel05_n_plus_one;

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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import s09_jpa_relations.SqlCapture;
import s09_jpa_relations.rel05_n_plus_one.CustomerSpending;
import s09_jpa_relations.rel05_n_plus_one.ShopOrder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Rel05NPlusOneExercisesTest na rozwiązaniach wzorcowych (SolutionNPlusOneService).
 * @Tag("wzorzec") — biegną zawsze: dowód, że ćwiczenia da się rozwiązać, a ich testy są poprawne.
 */
@Tag("wzorzec")
@DataJpaTest(showSql = false, properties = SqlCapture.PROPERTY)
@ActiveProfiles("rel05")
@Import(SolutionNPlusOneService.class)
class Rel05NPlusOneSolutionsTest {

    @Autowired
    private SolutionNPlusOneService exercises;

    @Autowired
    private EntityManagerFactory emf;

    private Statistics stats;

    @BeforeEach
    void setUp() {
        stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        SqlCapture.clear();
    }

    @Test
    @DisplayName("Wzorzec 1: numer zamówienia → klient, jednym zapytaniem")
    void exercise1() {
        Map<String, String> names = exercises.customerNameByOrderNumber();

        assertThat(names).hasSize(10).containsEntry("ZAM-003", "Jan Kowalski").containsEntry("ZAM-010", "Anna Nowak");
        assertThat(names.keySet()).first().isEqualTo("ZAM-001");
        assertThat(SqlCapture.all()).hasSize(1);
    }

    @Test
    @DisplayName("Wzorzec 2: numer zamówienia → produkty pozycji, jednym zapytaniem, puste ZAM-010 też")
    void exercise2() {
        Map<String, List<String>> products = exercises.productNamesByOrderNumber();

        assertThat(products).hasSize(10)
                .containsEntry("ZAM-003", List.of("Kubek", "Mysz", "Słuchawki"))
                .containsEntry("ZAM-010", List.of());
        assertThat(SqlCapture.all()).hasSize(1);
    }

    @Test
    @DisplayName("Wzorzec 3: @EntityGraph z pozycjami i produktami — nazwy produktów bez dodatkowych zapytań")
    void exercise3() {
        List<ShopOrder> ofJan = exercises.ordersOfCustomerWithProducts(2L);

        assertThat(ofJan).extracting(ShopOrder::getOrderNumber).containsExactly("ZAM-003", "ZAM-007");
        assertThat(ofJan.getFirst().getLines()).extracting(line -> line.getProduct().getName())
                .containsExactly("Kubek", "Mysz", "Słuchawki");
        assertThat(ofJan.getLast().getLines()).extracting(line -> line.getProduct().getName())
                .containsExactly("Klawiatura", "Kubek");
        assertThat(SqlCapture.all()).hasSize(1);
    }

    @Test
    @DisplayName("Wzorzec 4: wydatki klientów jednym zapytaniem, bez encji")
    void exercise4() {
        List<CustomerSpending> spending = exercises.spendingPerCustomer();

        assertThat(spending).extracting(CustomerSpending::customerName)
                .containsExactly("Anna Nowak", "Ewa Wiśniewska", "Jan Kowalski", "Piotr Zieliński");
        assertThat(spending).extracting(CustomerSpending::orderCount).containsExactly(4L, 2L, 2L, 2L);
        assertThat(spending.getFirst().total()).isEqualByComparingTo("1450.39");
        assertThat(spending.getLast().total()).isEqualByComparingTo("2121.48");
        assertThat(SqlCapture.all()).hasSize(1);
        assertThat(stats.getEntityLoadCount()).isZero();
    }

    @Test
    @DisplayName("Wzorzec 5: pozycje i płatności wszystkich zamówień — dokładnie 2 zapytania")
    void exercise5() {
        List<ShopOrder> all = exercises.ordersWithLinesAndPayments();

        assertThat(all).extracting(ShopOrder::getOrderNumber).first().isEqualTo("ZAM-001");
        assertThat(all).hasSize(10).allMatch(order -> Hibernate.isInitialized(order.getLines())
                && Hibernate.isInitialized(order.getPayments()));
        assertThat(all).extracting(order -> order.getPayments().size()).containsExactly(1, 2, 1, 1, 2, 1, 1, 1, 1, 0);
        assertThat(SqlCapture.all()).hasSize(2);
    }
}
