package s09_jpa_relations.rel01_many_to_one;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Rel01ManyToOne. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@DataJpaTest
@ActiveProfiles("rel01")
class Rel01ManyToOneExercisesTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    CategoryRepository categories;

    @Autowired
    ProductRepository products;

    @Test
    @DisplayName("Ćwiczenie 1: zapis kategorii i produktu w dobrej kolejności")
    void exercise1() {
        Long id = Rel01ManyToOne.exercise1(categories, products, "Słodycze", "Czekolada", new BigDecimal("8.99"));
        em.flush();
        em.clear();

        Product loaded = em.find(Product.class, id);
        assertThat(loaded.getCategory().getName()).isEqualTo("Słodycze");
    }

    @Test
    @DisplayName("Ćwiczenie 2: nazwy produktów kategorii przez JPQL")
    void exercise2() {
        Category coffee = em.persist(new Category("Kawa"));
        em.persist(new Product("Robusta", BigDecimal.ONE, coffee));
        em.persist(new Product("Arabica", BigDecimal.ONE, coffee));
        em.persist(new Product("Sencha", BigDecimal.ONE, em.persist(new Category("Herbata"))));
        em.flush();

        assertThat(Rel01ManyToOne.exercise2(em.getEntityManager(), "Kawa")).containsExactly("Arabica", "Robusta");
    }

    @Test
    @DisplayName("Ćwiczenie 3: przeniesienie produktu bez wczytywania kategorii")
    void exercise3() {
        Category coffee = em.persist(new Category("Kawa"));
        Category tea = em.persist(new Category("Herbata"));
        Long productId = em.persistAndGetId(new Product("Mieszanka", BigDecimal.ONE, coffee), Long.class);
        em.flush();
        em.clear();

        Rel01ManyToOne.exercise3(em.getEntityManager(), productId, tea.getId());
        Product product = em.find(Product.class, productId);
        assertThat(Hibernate.isInitialized(product.getCategory())).as("kategoria nie powinna być czytana").isFalse();
        em.flush();
        em.clear();

        assertThat(em.find(Product.class, productId).getCategory().getId()).isEqualTo(tea.getId());
    }
}
