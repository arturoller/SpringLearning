package s09_jpa_relations.rel01_many_to_one;

import org.hibernate.Hibernate;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Testy ĆWICZEŃ lekcji Rel01ManyToOne (OrderExercises). @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * {@code @Import} (import = dołącz) — @DataJpaTest nie skanuje serwisów, więc OrderExercises dołączamy ręcznie.
 */
@Tag("cwiczenie")
@DataJpaTest(showSql = false, properties = SqlCapture.PROPERTY)
@ActiveProfiles("rel01")
@Import(OrderExercises.class)
class Rel01ManyToOneExercisesTest {

    @Autowired
    private OrderExercises exercises;

    @Autowired
    private TestEntityManager tem;

    @BeforeEach
    void setUp() {
        SqlCapture.clear();
    }

    @Test
    @DisplayName("Ćwiczenie 1: numery zamówień klienta o danym imieniu i nazwisku")
    void exercise1() {
        assertThat(exercises.orderNumbersOfCustomerNamed("Anna Nowak")).containsExactly("ZAM-001", "ZAM-003");
        assertThat(exercises.orderNumbersOfCustomerNamed("Nikt Taki")).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 2: zapis zamówienia istniejącego klienta jednym INSERT-em, bez SELECT-a klienta")
    void exercise2() {
        ShopOrder order = exercises.placeOrder(3L, "ZAM-200", LocalDate.of(2026, 11, 2));
        tem.flush();

        assertThat(SqlCapture.all()).as("tylko INSERT do orders").hasSize(1).first().asString().startsWith("insert into orders");
        assertThat(Hibernate.isInitialized(order.getCustomer())).as("klient niewczytany").isFalse();
        tem.clear();
        assertThat(tem.find(ShopOrder.class, order.getId()).getCustomer().getName()).isEqualTo("Ewa Wiśniewska");
    }

    @Test
    @DisplayName("Ćwiczenie 3: liczba zamówień klienta po e-mailu")
    void exercise3() {
        assertThat(exercises.countOrdersOf("anna@sklep.pl")).isEqualTo(2);
        assertThat(exercises.countOrdersOf("piotr@sklep.pl")).isZero();
        assertThat(SqlCapture.last()).as("e-mail jest w tabeli customer → JOIN").contains("join customer");
    }

    @Test
    @DisplayName("Ćwiczenie 4: liczba zamówień na klienta jednym zapytaniem")
    void exercise4() {
        assertThat(exercises.orderCountByCustomerName())
                .containsExactly(entry("Anna Nowak", 2L), entry("Ewa Wiśniewska", 1L),
                        entry("Jan Kowalski", 2L));
        assertThat(SqlCapture.all()).as("jedno zapytanie z group by").hasSize(1);
    }

    @Test
    @DisplayName("Ćwiczenie 5: przepisanie zamówienia na innego klienta — SELECT zamówienia + UPDATE, bez SELECT-a klienta")
    void exercise5() {
        exercises.reassign(4L, 1L);
        tem.flush();

        List<String> sql = SqlCapture.all();
        assertThat(sql).hasSize(2);
        assertThat(sql.get(0)).startsWith("select").contains("from orders");
        assertThat(sql.get(1)).startsWith("update orders set customer_id=?");
        tem.clear();
        assertThat(tem.find(ShopOrder.class, 4L).getCustomer().getName()).isEqualTo("Anna Nowak");
    }
}
