package s08_data_jpa.jpa01_entities;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Jpa01Entities. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@DataJpaTest
@ActiveProfiles("jpa01")
class Jpa01EntitiesExercisesTest {

    @Autowired
    TestEntityManager tem;

    @Test
    @DisplayName("Ćwiczenie 1: nowa kategoria dostaje id i trafia do bazy")
    void exercise1() {
        EntityManager em = tem.getEntityManager();
        Long id = Jpa01Entities.exercise1(em, "Słodycze");

        assertThat(id).isNotNull();
        tem.flush();
        tem.clear();
        assertThat(tem.find(Category.class, id).getName()).isEqualTo("Słodycze");
    }

    @Test
    @DisplayName("Ćwiczenie 2: zmiana ceny przez dirty checking")
    void exercise2() {
        Long id = tem.persistAndGetId(new Product("Kawa", "CW-KAWA-1", new BigDecimal("10.00")), Long.class);
        tem.flush();
        tem.clear();

        Jpa01Entities.exercise2(tem.getEntityManager(), id, new BigDecimal("11.00"));
        tem.flush();
        tem.clear();

        assertThat(tem.find(Product.class, id).getPrice()).isEqualByComparingTo("11.00");
    }

    @Test
    @DisplayName("Ćwiczenie 3: merge odłączonego produktu zwraca zarządzaną kopię")
    void exercise3() {
        Product detached = tem.persistFlushFind(new Product("Herbata", "CW-HERB-1", new BigDecimal("5.00")));
        tem.clear();
        EntityManager em = tem.getEntityManager();

        Product managed = Jpa01Entities.exercise3(em, detached, new BigDecimal("6.00"));

        assertThat(em.contains(managed)).isTrue();
        assertThat(em.contains(detached)).isFalse();
        tem.flush();
        tem.clear();
        assertThat(tem.find(Product.class, detached.getId()).getPrice()).isEqualByComparingTo("6.00");
    }
}
