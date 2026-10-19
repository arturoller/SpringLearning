package s08_data_jpa.jpa01_entities;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Jpa01Entities. {@code @Tag("cwiczenie")} — nie biegną w zwykłym {@code mvnw test};
 * uruchamiasz je sam (▶ obok klasy albo {@code mvnw -pl s08-data-jpa test -Dgroups=cwiczenie -DexcludedGroups=none}).
 * Nie zmieniaj nazw pól w ShopOrder — test czyta kolumny number, status i total wprost z tabeli orders.
 */
@DataJpaTest
@ActiveProfiles("jpa01")
@Tag("cwiczenie")
class Jpa01EntitiesExercisesTest {

    @Autowired
    EntityManager entityManager;

    private Object single(String nativeSql) {
        return entityManager.createNativeQuery(nativeSql).getSingleResult();
    }

    @Test
    @DisplayName("Ćwiczenie 1: ShopOrder to encja w tabeli orders, status zapisany tekstem, kwota numeric(10,2) NOT NULL")
    void exercise1() {
        entityManager.persist(new ShopOrder("ZAM-001", OrderStatus.OPLACONE, new BigDecimal("5564.98")));
        entityManager.flush();
        entityManager.clear();

        ShopOrder loaded = entityManager.find(ShopOrder.class, "ZAM-001");
        assertThat(loaded.getStatus()).isEqualTo(OrderStatus.OPLACONE);
        assertThat(loaded.getTotal()).isEqualByComparingTo("5564.98");
        assertThat(single("select status from orders where number = 'ZAM-001'").toString())
                .as("status jako NAZWA stałej (EnumType.STRING)").isEqualTo("OPLACONE");
        Object[] total = (Object[]) single("select data_type, numeric_precision, numeric_scale, is_nullable "
                + "from information_schema.columns where table_name = 'ORDERS' and column_name = 'TOTAL'");
        assertThat(total[0]).isEqualTo("NUMERIC");
        assertThat(((Number) total[1]).intValue()).isEqualTo(10);
        assertThat(((Number) total[2]).intValue()).isEqualTo(2);
        assertThat(total[3]).as("kwota NOT NULL").isEqualTo("NO");
    }

    @Test
    @DisplayName("Ćwiczenie 2: równość po numerze zamówienia; zamówienie nie znika z HashSet po zapisie")
    void exercise2() {
        ShopOrder order = new ShopOrder("ZAM-002", OrderStatus.NOWE, new BigDecimal("64.99"));
        assertThat(order).isEqualTo(new ShopOrder("ZAM-002", OrderStatus.OPLACONE, new BigDecimal("64.99")));
        assertThat(order.hashCode())
                .isEqualTo(new ShopOrder("ZAM-002", OrderStatus.ANULOWANE, BigDecimal.ONE).hashCode());
        assertThat(order).isNotEqualTo(new ShopOrder("ZAM-003", OrderStatus.NOWE, new BigDecimal("64.99")));

        Set<ShopOrder> orders = new HashSet<>();
        orders.add(order);
        entityManager.persist(order);
        entityManager.flush();
        assertThat(orders.contains(order)).isTrue();
        entityManager.clear();
        assertThat(entityManager.find(ShopOrder.class, "ZAM-002")).isEqualTo(order);
    }

    @Test
    @DisplayName("Ćwiczenie 3: zmiana statusu bez persist/merge — dirty checking wysyła UPDATE (wymaga ćwiczenia 1)")
    void exercise3() {
        entityManager.persist(new ShopOrder("ZAM-004", OrderStatus.OPLACONE, new BigDecimal("349.90")));
        entityManager.flush();
        entityManager.clear();

        Jpa01Entities.exercise3(entityManager, "ZAM-004");
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(ShopOrder.class, "ZAM-004").getStatus()).isEqualTo(OrderStatus.WYSLANE);
    }
}
