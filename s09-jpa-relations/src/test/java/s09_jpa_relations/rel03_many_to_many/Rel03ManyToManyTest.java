package s09_jpa_relations.rel03_many_to_many;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Rel03ManyToMany — „WYNIK” każdej sekcji. Profil rel03 włącza statystyki Hibernate
 * (hibernate.generate_statistics=true): {@code getPrepareStatementCount()} = ile instrukcji SQL Hibernate przygotował
 * (wysłał do bazy) od ostatniego {@code clear()} statystyk.
 */
@DataJpaTest
@ActiveProfiles("rel03")
class Rel03ManyToManyTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    EntityManagerFactory emf;

    @Test
    @DisplayName("1. @ManyToMany + @JoinTable: każda para produkt–tag to wiersz w product_tags")
    void joinTableRows() {
        Tag vegan = em.persist(new Tag("wegańskie"));
        Tag promo = em.persist(new Tag("promocja"));
        Product chocolate = new Product("Czekolada gorzka", new BigDecimal("8.99"));
        chocolate.addTag(vegan);
        chocolate.addTag(promo);
        em.persist(chocolate);
        em.flush();

        assertThat(jdbc.sql("select tag_id from product_tags where product_id = ? order by tag_id")
                .param(chocolate.getId()).query(Long.class).list())
                .containsExactly(vegan.getId(), promo.getId());
        assertThat(vegan.getProducts()).as("metoda pomocnicza ustawiła drugą stronę").contains(chocolate);
    }

    @Test
    @DisplayName("2. Set: usunięcie jednego tagu = 1 instrukcja SQL (DELETE jednego wiersza)")
    void removeFromSetDeletesOneRow() {
        Long id = productWithTags(new String[]{"a", "b", "c"});
        Product product = em.find(Product.class, id);
        Tag first = product.getTags().stream().filter(t -> t.getName().equals("a")).findFirst().orElseThrow();
        product.getTags().size();                     // wczytaj kolekcję PRZED pomiarem
        Statistics stats = statistics();

        product.removeTag(first);
        em.flush();

        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        assertThat(countRows("product_tags")).isEqualTo(2);
    }

    @Test
    @DisplayName("2. List (bag): usunięcie jednego tagu = DELETE wszystkich wierszy + INSERT pozostałych (1 + 2 = 3)")
    void removeFromBagRecreatesCollection() {
        Tag a = em.persist(new Tag("a"));
        Tag b = em.persist(new Tag("b"));
        Tag c = em.persist(new Tag("c"));
        ListProduct product = new ListProduct("Kawa");
        product.getTags().addAll(List.of(a, b, c));
        em.persist(product);
        em.flush();
        em.clear();
        ListProduct loaded = em.find(ListProduct.class, product.getId());
        loaded.getTags().size();                      // wczytaj kolekcję PRZED pomiarem
        Statistics stats = statistics();

        loaded.getTags().removeIf(tag -> tag.getName().equals("a"));
        em.flush();

        assertThat(stats.getPrepareStatementCount()).isEqualTo(3);
        assertThat(countRows("list_product_tags")).isEqualTo(2);
    }

    @Test
    @DisplayName("3. Usunięcie tagu, który jest jeszcze przypięty do produktu → naruszenie klucza obcego")
    void removingLinkedTagViolatesForeignKey() {
        Long productId = productWithTags(new String[]{"bio"});
        Tag bio = em.find(Product.class, productId).getTags().iterator().next();

        em.remove(bio);                               // Tag to strona odwrotna — product_tags zostaje

        assertThatThrownBy(() -> em.flush()).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("3. Poprawnie: najpierw odpiąć tag od wszystkich produktów, potem usunąć")
    void removingTagAfterUnlinking() {
        Long productId = productWithTags(new String[]{"bio", "promocja"});
        Product product = em.find(Product.class, productId);
        Tag bio = product.getTags().stream().filter(t -> t.getName().equals("bio")).findFirst().orElseThrow();

        List.copyOf(bio.getProducts()).forEach(p -> p.removeTag(bio));   // kopia — nie zmieniamy zbioru w pętli
        em.remove(bio);
        em.flush();

        assertThat(countRows("tags")).isEqualTo(1);
        assertThat(countRows("products")).isEqualTo(1);
    }

    @Test
    @DisplayName("4. PUŁAPKA: cascade = REMOVE na @ManyToMany próbuje usunąć tag wspólny z innym produktem")
    void cascadeRemoveOnManyToManyIsDangerous() {
        Tag promo = em.persist(new Tag("promocja"));
        DangerousProduct first = new DangerousProduct("Kawa");
        DangerousProduct second = new DangerousProduct("Herbata");
        first.getTags().add(promo);
        second.getTags().add(promo);
        em.persist(first);
        em.persist(second);
        em.flush();

        em.remove(first);                             // kaskada: usuń też tag „promocja”

        assertThatThrownBy(() -> em.flush()).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("5. Relacja z dodatkową kolumną: encja OrderLine (dwa @ManyToOne + quantity)")
    void linkEntityWithExtraColumn() {
        Product coffee = em.persist(new Product("Kawa", new BigDecimal("29.99")));
        Product tea = em.persist(new Product("Herbata", new BigDecimal("15.99")));
        ShopOrder anna = em.persist(new ShopOrder("Anna"));
        ShopOrder piotr = em.persist(new ShopOrder("Piotr"));
        em.persist(new OrderLine(anna, coffee, 2));
        em.persist(new OrderLine(anna, tea, 1));
        em.persist(new OrderLine(piotr, coffee, 3));
        em.flush();

        List<ProductSales> sales = em.getEntityManager().createQuery("""
                        select new s09_jpa_relations.rel03_many_to_many.ProductSales(l.product.name, sum(l.quantity))
                        from OrderLine l
                        group by l.product.name
                        order by l.product.name""", ProductSales.class)
                .getResultList();

        assertThat(sales).containsExactly(new ProductSales("Herbata", 1), new ProductSales("Kawa", 5));
    }

    private Long productWithTags(String[] tagNames) {
        Product product = new Product("Produkt", BigDecimal.ONE);
        for (String name : tagNames) {
            product.addTag(em.persist(new Tag(name)));
        }
        em.persist(product);
        em.flush();
        em.clear();
        return product.getId();
    }

    private Statistics statistics() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        return stats;
    }

    private long countRows(String table) {
        return jdbc.sql("select count(*) from " + table).query(Long.class).single();
    }
}
