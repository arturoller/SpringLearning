package s09_jpa_relations.solutions.rel02_one_to_many;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import s09_jpa_relations.rel02_one_to_many.NaiveOrder;
import s09_jpa_relations.rel02_one_to_many.NaiveOrderLine;
import s09_jpa_relations.rel02_one_to_many.OrderLine;
import s09_jpa_relations.rel02_one_to_many.Rel02Application;
import s09_jpa_relations.rel02_one_to_many.ShopOrder;
import s09_jpa_relations.rel02_one_to_many.ShopOrderRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Rel02OneToManyExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Pakiet solutions nie ma klasy startowej, więc wskazujemy ją wprost: {@code @ContextConfiguration(classes = ...)}.
 */
@Tag("wzorzec")
@DataJpaTest
@ContextConfiguration(classes = Rel02Application.class)
@ActiveProfiles("rel02")
class Rel02OneToManySolutionsTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    ShopOrderRepository orders;

    @Test
    @DisplayName("Wzorzec 1: zamówienie z dwiema pozycjami zapisane jednym save")
    void solution1() {
        Long id = Rel02OneToManySolutions.solution1(orders, "Anna");
        em.flush();

        assertThat(jdbc.sql("select product_name from order_lines where order_id = ? order by product_name")
                .param(id).query(String.class).list()).containsExactly("Herbata", "Kawa");
    }

    @Test
    @DisplayName("Wzorzec 2: usunięcie pozycji przez orphanRemoval")
    void solution2() {
        ShopOrder order = new ShopOrder("Ola");
        order.addLine(new OrderLine("Kawa", 1));
        order.addLine(new OrderLine("Kakao", 2));
        Long id = em.persistAndGetId(order, Long.class);
        em.flush();
        em.clear();

        Rel02OneToManySolutions.solution2(em.getEntityManager(), id, "Kawa");
        em.flush();

        assertThat(jdbc.sql("select product_name from order_lines where order_id = ?")
                .param(id).query(String.class).list()).containsExactly("Kakao");
    }

    @Test
    @DisplayName("Wzorzec 3: podpięcie osieroconej pozycji — obie strony relacji")
    void solution3() {
        NaiveOrder order = em.persist(new NaiveOrder("Piotr"));
        NaiveOrderLine orphan = em.persist(new NaiveOrderLine("Herbata", 3));
        em.flush();

        Rel02OneToManySolutions.solution3(em.getEntityManager(), order.getId(), orphan.getId());
        assertThat(order.getLines()).as("lista w pamięci").contains(orphan);
        em.flush();

        assertThat(jdbc.sql("select order_id from naive_order_lines where id = ?").param(orphan.getId())
                .query(Long.class).single()).isEqualTo(order.getId());
    }
}
