package s09_jpa_relations.rel02_one_to_many;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import s09_jpa_relations.SqlCapture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Testy ĆWICZEŃ lekcji Rel02OneToMany (OrderLineExercises). @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * {@code @Import} — @DataJpaTest nie skanuje serwisów, więc klasę z ćwiczeniami dołączamy ręcznie.
 */
@Tag("cwiczenie")
@DataJpaTest(showSql = false, properties = SqlCapture.PROPERTY)
@ActiveProfiles("rel02")
@Import(OrderLineExercises.class)
class Rel02OneToManyExercisesTest {

    @Autowired
    private OrderLineExercises exercises;

    @Autowired
    private TestEntityManager tem;

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
    @DisplayName("Ćwiczenie 1: dodanie produktu do pustego zamówienia — pozycję zapisuje kaskada")
    void exercise1() {
        exercises.addProduct(3L, 4L, 3); // Kubek ×3 do ZAM-003
        tem.flush();
        tem.clear();

        ShopOrder order = tem.find(ShopOrder.class, 3L);
        assertThat(order.getLines()).hasSize(1).first().satisfies(line -> {
            assertThat(line.getProduct().getName()).isEqualTo("Kubek");
            assertThat(line.getQuantity()).isEqualTo(3);
            assertThat(line.getUnitPrice()).isEqualByComparingTo("24.50");
        });
    }

    @Test
    @DisplayName("Ćwiczenie 2: usunięcie produktu z zamówienia — orphanRemoval robi DELETE")
    void exercise2() {
        exercises.removeProduct(1L, 2L); // Mysz z ZAM-001
        exercises.removeProduct(1L, 99L); // brak takiej pozycji — nic się nie dzieje
        SqlCapture.clear();
        tem.flush();

        assertThat(SqlCapture.all()).containsExactly("delete from order_line where id=?");
        tem.clear();
        assertThat(tem.find(ShopOrder.class, 1L).getLines()).extracting(line -> line.getProduct().getName())
                .containsExactly("Klawiatura");
    }

    @Test
    @DisplayName("Ćwiczenie 3: wartość zamówienia jednym zapytaniem, bez encji; 0 dla pustego")
    void exercise3() {
        assertThat(exercises.orderTotal(2L)).isEqualByComparingTo("1056.99");
        assertThat(exercises.orderTotal(3L)).isNotNull().isEqualByComparingTo("0");
        assertThat(SqlCapture.all()).as("dwa wywołania = dwa zapytania").hasSize(2);
        assertThat(stats.getEntityLoadCount()).as("żadnej wczytanej encji").isZero();
    }

    @Test
    @DisplayName("Ćwiczenie 4: sprzedana ilość każdego produktu jednym zapytaniem")
    void exercise4() {
        assertThat(exercises.quantitySoldByProduct()).containsExactly(
                entry("Klawiatura", 1L), entry("Kubek", 4L), entry("Monitor", 1L), entry("Mysz", 3L));
        assertThat(SqlCapture.all()).hasSize(1);
    }

    @Test
    @DisplayName("Ćwiczenie 5: kopia zamówienia — jeden save, pozycje z cenami ze źródła")
    void exercise5() {
        ShopOrder copy = exercises.copyOrder(2L, "ZAM-002-K");
        tem.flush();

        assertThat(SqlCapture.startingWith("insert")).as("1 zamówienie + 3 pozycje").hasSize(4);
        tem.clear();
        ShopOrder fresh = tem.find(ShopOrder.class, copy.getId());
        assertThat(fresh.getOrderNumber()).isEqualTo("ZAM-002-K");
        assertThat(fresh.getLines()).extracting(line -> line.getProduct().getName())
                .containsExactly("Monitor", "Mysz", "Kubek");
        assertThat(fresh.total()).isEqualByComparingTo("1056.99");
    }
}
