package s08_data_jpa.solutions.jpa01_entities;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import s08_data_jpa.jpa01_entities.Category;
import s08_data_jpa.jpa01_entities.Jpa01Application;
import s08_data_jpa.jpa01_entities.Product;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Jpa01EntitiesExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Pakiet solutions nie ma klasy startowej, więc wskazujemy ją wprost: {@code @ContextConfiguration(classes = ...)}.
 */
@Tag("wzorzec")
@DataJpaTest
@ContextConfiguration(classes = Jpa01Application.class)
@ActiveProfiles("jpa01")
class Jpa01EntitiesSolutionsTest {

    @Autowired
    TestEntityManager tem;

    @Test
    @DisplayName("Wzorzec 1: nowa kategoria dostaje id i trafia do bazy")
    void solution1() {
        Long id = Jpa01EntitiesSolutions.solution1(tem.getEntityManager(), "Słodycze");

        assertThat(id).isNotNull();
        tem.flush();
        tem.clear();
        assertThat(tem.find(Category.class, id).getName()).isEqualTo("Słodycze");
    }

    @Test
    @DisplayName("Wzorzec 2: zmiana ceny przez dirty checking")
    void solution2() {
        Long id = tem.persistAndGetId(new Product("Kawa", "WZ-KAWA-1", new BigDecimal("10.00")), Long.class);
        tem.flush();
        tem.clear();

        Jpa01EntitiesSolutions.solution2(tem.getEntityManager(), id, new BigDecimal("11.00"));
        tem.flush();
        tem.clear();

        assertThat(tem.find(Product.class, id).getPrice()).isEqualByComparingTo("11.00");
    }

    @Test
    @DisplayName("Wzorzec 3: merge odłączonego produktu zwraca zarządzaną kopię")
    void solution3() {
        Product detached = tem.persistFlushFind(new Product("Herbata", "WZ-HERB-1", new BigDecimal("5.00")));
        tem.clear();
        EntityManager em = tem.getEntityManager();

        Product managed = Jpa01EntitiesSolutions.solution3(em, detached, new BigDecimal("6.00"));

        assertThat(em.contains(managed)).isTrue();
        assertThat(em.contains(detached)).isFalse();
        tem.flush();
        tem.clear();
        assertThat(tem.find(Product.class, detached.getId()).getPrice()).isEqualByComparingTo("6.00");
    }
}
