package s08_data_jpa.solutions.jpa01_entities;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import s08_data_jpa.jpa01_entities.Category;
import s08_data_jpa.jpa01_entities.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Jpa01EntitiesExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
@DataJpaTest(showSql = false)
@ActiveProfiles("jpa01")
class Jpa01EntitiesSolutionsTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private TestEntityManager tem;

    private static Product product(String name, int stock) {
        return new Product(name, Category.OFFICE, new BigDecimal("10.00"), stock, Instant.parse("2026-10-01T08:00:00Z"));
    }

    @Test
    @DisplayName("Wzorzec 1: persist i numer nadany przez bazę")
    void solution1() {
        Long id = Jpa01EntitiesSolutions.solution1Persist(em, product("Zeszyt", 5));
        assertThat(id).isNotNull();
        tem.clear();
        assertThat(em.find(Product.class, id).getName()).isEqualTo("Zeszyt");
    }

    @Test
    @DisplayName("Wzorzec 2: zmiana ceny bez save (dirty checking) i false dla nieistniejącego produktu")
    void solution2() {
        Long id = tem.persistAndFlush(product("Długopis", 5)).getId();

        assertThat(Jpa01EntitiesSolutions.solution2ChangePrice(em, id, new BigDecimal("3.50"))).isTrue();
        assertThat(Jpa01EntitiesSolutions.solution2ChangePrice(em, 999L, BigDecimal.ONE)).isFalse();

        tem.flush();
        tem.clear();
        assertThat(em.find(Product.class, id).getPrice()).isEqualByComparingTo("3.50");
    }

    @Test
    @DisplayName("Wzorzec 3: zapis odłączonej encji przez merge zwraca zarządzaną kopię")
    void solution3() {
        Product detached = tem.persistAndFlush(product("Segregator", 5));
        tem.detach(detached);
        detached.setStock(42);

        Product managed = Jpa01EntitiesSolutions.solution3SaveDetached(em, detached);

        assertThat(managed).isNotSameAs(detached);
        assertThat(em.contains(managed)).isTrue();
        tem.flush();
        tem.clear();
        assertThat(em.find(Product.class, detached.getId()).getStock()).isEqualTo(42);
    }

    @Test
    @DisplayName("Wzorzec 4: usunięcie produktów z zerowym stanem, pominięcie nieistniejących")
    void solution4() {
        Long empty1 = tem.persist(product("Taśma", 0)).getId();
        Long full = tem.persist(product("Klej", 3)).getId();
        Long empty2 = tem.persist(product("Zszywacz", 0)).getId();
        tem.flush();

        assertThat(Jpa01EntitiesSolutions.solution4RemoveOutOfStock(em, List.of(empty1, full, 999L, empty2))).isEqualTo(2);

        tem.flush();
        tem.clear();
        assertThat(em.find(Product.class, empty1)).isNull();
        assertThat(em.find(Product.class, empty2)).isNull();
        assertThat(em.find(Product.class, full)).isNotNull();
    }
}
