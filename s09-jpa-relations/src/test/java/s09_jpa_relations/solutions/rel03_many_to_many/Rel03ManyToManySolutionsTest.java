package s09_jpa_relations.solutions.rel03_many_to_many;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import s09_jpa_relations.rel03_many_to_many.Product;
import s09_jpa_relations.rel03_many_to_many.ProductTag;
import s09_jpa_relations.rel03_many_to_many.ProductTagCount;
import s09_jpa_relations.rel03_many_to_many.Rel03Application;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Rel03ManyToManyExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Pakiet solutions nie ma klasy startowej, więc wskazujemy ją wprost: {@code @ContextConfiguration(classes = ...)}.
 */
@Tag("wzorzec")
@DataJpaTest
@ContextConfiguration(classes = Rel03Application.class)
@ActiveProfiles("rel03")
class Rel03ManyToManySolutionsTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    JdbcClient jdbc;

    @Test
    @DisplayName("Wzorzec 1: nowy produkt z istniejącymi tagami (bez tworzenia nowych tagów)")
    void solution1() {
        Long bioId = em.persistAndGetId(new ProductTag("bio"), Long.class);
        Long promoId = em.persistAndGetId(new ProductTag("promocja"), Long.class);
        em.persist(new ProductTag("nowość"));
        em.flush();

        Long id = Rel03ManyToManySolutions.solution1(em.getEntityManager(), "Oliwa z oliwek", new BigDecimal("42.00"),
                List.of("bio", "promocja"));
        em.flush();

        assertThat(jdbc.sql("select tag_id from product_tags where product_id = ? order by tag_id")
                .param(id).query(Long.class).list()).containsExactly(bioId, promoId);
        assertThat(jdbc.sql("select count(*) from tags").query(Long.class).single())
                .as("żadnych nowych tagów").isEqualTo(3);
    }

    @Test
    @DisplayName("Wzorzec 2: nazwy produktów z danym tagiem, alfabetycznie")
    void solution2() {
        var bio = em.persist(new ProductTag("bio"));
        var promo = em.persist(new ProductTag("promocja"));
        product("Oliwa z oliwek", bio);
        product("Kawa ziarnista 1kg", bio, promo);
        product("Czekolada gorzka", promo);
        em.flush();
        em.clear();

        assertThat(Rel03ManyToManySolutions.solution2(em.getEntityManager(), "bio"))
                .containsExactly("Kawa ziarnista 1kg", "Oliwa z oliwek");
    }

    @Test
    @DisplayName("Wzorzec 3: usunięcie tagu ze sklepu bez błędu klucza obcego — produkty zostają")
    void solution3() {
        var bio = em.persist(new ProductTag("bio"));
        var promo = em.persist(new ProductTag("promocja"));
        product("Oliwa z oliwek", bio);
        product("Kawa ziarnista 1kg", bio, promo);
        em.flush();
        em.clear();

        Rel03ManyToManySolutions.solution3(em.getEntityManager(), "bio");
        em.flush();

        assertThat(jdbc.sql("select name from tags").query(String.class).list()).containsExactly("promocja");
        assertThat(jdbc.sql("select count(*) from products").query(Long.class).single()).isEqualTo(2);
        assertThat(jdbc.sql("select count(*) from product_tags").query(Long.class).single()).isEqualTo(1);
    }

    @Test
    @DisplayName("Wzorzec 4: liczba tagów każdego produktu — także produktu bez tagów (LEFT JOIN)")
    void solution4() {
        var bio = em.persist(new ProductTag("bio"));
        var promo = em.persist(new ProductTag("promocja"));
        product("Oliwa z oliwek", bio);
        product("Kawa ziarnista 1kg", bio, promo);
        product("Lampka biurkowa");
        em.flush();
        em.clear();

        assertThat(Rel03ManyToManySolutions.solution4(em.getEntityManager())).containsExactly(
                new ProductTagCount("Kawa ziarnista 1kg", 2),
                new ProductTagCount("Lampka biurkowa", 0),
                new ProductTagCount("Oliwa z oliwek", 1));
    }

    private void product(String name, ProductTag... tags) {
        Product product = new Product(name, BigDecimal.TEN);
        for (var tag : tags) {
            product.addTag(tag);
        }
        em.persist(product);
    }
}
