package s08_data_jpa.solutions.jpa01_entities;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import s08_data_jpa.jpa01_entities.OrderStatus;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Jpa01EntitiesExercisesTest na rozwiązaniach wzorcowych. {@code @Tag("wzorzec")} — biegną zawsze.
 * {@code @DataJpaTest} znajduje konfigurację Jpa01SolutionsApplication w tym samym pakiecie.
 */
@DataJpaTest
@ActiveProfiles("jpa01")
@Tag("wzorzec")
class Jpa01EntitiesSolutionsTest {

    @Autowired
    EntityManager entityManager;

    private Object single(String nativeSql) {
        return entityManager.createNativeQuery(nativeSql).getSingleResult();
    }

    @Test
    @DisplayName("Wzorzec 1: ShopOrder to encja w tabeli orders, status zapisany tekstem, kwota numeric(10,2) NOT NULL")
    void solution1() {
        entityManager.persist(new ShopOrder("ZAM-001", OrderStatus.OPLACONE, new BigDecimal("5564.98")));
        entityManager.flush();
        entityManager.clear();

        ShopOrder loaded = entityManager.find(ShopOrder.class, "ZAM-001");
        assertThat(loaded.getStatus()).isEqualTo(OrderStatus.OPLACONE);
        assertThat(loaded.getTotal()).isEqualByComparingTo("5564.98");
        assertThat(single("select status from orders where number = 'ZAM-001'").toString()).isEqualTo("OPLACONE");
        Object[] total = (Object[]) single("select data_type, numeric_precision, numeric_scale, is_nullable "
                + "from information_schema.columns where table_name = 'ORDERS' and column_name = 'TOTAL'");
        assertThat(total[0]).isEqualTo("NUMERIC");
        assertThat(((Number) total[1]).intValue()).isEqualTo(10);
        assertThat(((Number) total[2]).intValue()).isEqualTo(2);
        assertThat(total[3]).isEqualTo("NO");
    }

    @Test
    @DisplayName("Wzorzec 2: równość po numerze zamówienia; zamówienie nie znika z HashSet po zapisie")
    void solution2() {
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
    @DisplayName("Wzorzec 3: zmiana statusu bez persist/merge — dirty checking wysyła UPDATE")
    void solution3() {
        entityManager.persist(new ShopOrder("ZAM-004", OrderStatus.OPLACONE, new BigDecimal("349.90")));
        entityManager.flush();
        entityManager.clear();

        Jpa01EntitiesSolutions.solution3(entityManager, "ZAM-004");
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(ShopOrder.class, "ZAM-004").getStatus()).isEqualTo(OrderStatus.WYSLANE);
    }
}
