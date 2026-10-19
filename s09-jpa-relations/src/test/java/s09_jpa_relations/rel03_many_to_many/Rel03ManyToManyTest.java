package s09_jpa_relations.rel03_many_to_many;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.TransientPropertyValueException;
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
 * Testy lekcji Rel03ManyToMany — „WYNIK” każdej sekcji. {@code @DataJpaTest} + profil rel03 (migracje Flyway,
 * ddl-auto=validate). Profil włącza statystyki Hibernate (hibernate.generate_statistics=true):
 * {@code getPrepareStatementCount()} = ile instrukcji SQL Hibernate przygotował (wysłał do bazy) od ostatniego
 * {@code clear()} statystyk. JdbcClient zagląda do tabel łączących „obok” Hibernate.
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
        ProductTag vegan = em.persist(new ProductTag("wegańskie"));
        ProductTag promo = em.persist(new ProductTag("promocja"));
        Product chocolate = new Product("Czekolada gorzka", new BigDecimal("7.49"));
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
    @DisplayName("1. Strona odwrotna sama nic nie zapisuje: produkt dopisany tylko do tag.products — brak wiersza")
    void inverseSideAloneWritesNothing() {
        ProductTag bio = em.persist(new ProductTag("bio"));
        Product coffee = em.persist(new Product("Kawa ziarnista 1kg", new BigDecimal("64.99")));
        em.flush();

        bio.productsInternal().add(coffee);          // TYLKO lustro — product.tags dalej pusty
        em.flush();

        assertThat(countRows("product_tags")).isZero();
    }

    @Test
    @DisplayName("2. Set: usunięcie jednego tagu = 1 instrukcja SQL (DELETE jednego wiersza z product_tags)")
    void removeFromSetDeletesOneRow() {
        Long id = productWithTags("a", "b", "c");
        Product product = em.find(Product.class, id);
        ProductTag first = tagNamed(product, "a");
        first.getProducts().size();                   // wczytaj drugą stronę PRZED pomiarem (patrz następny test)
        Statistics stats = statistics();

        product.removeTag(first);
        em.flush();

        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        assertThat(countRows("product_tags")).isEqualTo(2);
    }

    @Test
    @DisplayName("2. Metoda pomocnicza dotyka tag.products — leniwa kolekcja drugiej strony dociąga się SELECT-em (1 + 1)")
    void helperLoadsInverseCollection() {
        Long id = productWithTags("a", "b", "c");
        Product product = em.find(Product.class, id);
        ProductTag first = tagNamed(product, "a");
        Statistics stats = statistics();

        product.removeTag(first);                     // tag.productsInternal().remove(...) → SELECT produktów tagu
        em.flush();                                   // DELETE z product_tags

        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
        assertThat(stats.getCollectionLoadCount()).as("wczytana kolekcja ProductTag.products").isEqualTo(1);
    }

    @Test
    @DisplayName("2. List (bag): usunięcie jednego tagu = DELETE wszystkich wierszy + INSERT pozostałych (1 + 2 = 3)")
    void removeFromBagRecreatesCollection() {
        ListProduct loaded = listProductWithTags("a", "b", "c");
        loaded.getTags().size();                      // wczytaj kolekcję PRZED pomiarem
        Statistics stats = statistics();

        loaded.getTags().removeIf(tag -> tag.getName().equals("a"));
        em.flush();

        assertThat(stats.getPrepareStatementCount()).isEqualTo(3);
        assertThat(countRows("list_product_tags")).isEqualTo(2);
    }

    @Test
    @DisplayName("2. List (bag): nawet DODANIE tagu = DELETE wszystkich wierszy + INSERT wszystkich (1 + 4 = 5)")
    void addToBagRecreatesCollection() {
        Long extraId = em.persistAndGetId(new ProductTag("d"), Long.class);
        ListProduct loaded = listProductWithTags("a", "b", "c");
        loaded.getTags().size();                      // wczytaj kolekcję PRZED pomiarem
        ProductTag extra = em.find(ProductTag.class, extraId);      // i nowy tag (po clear() trzeba go wczytać ponownie)
        Statistics stats = statistics();

        loaded.getTags().add(extra);
        em.flush();

        assertThat(stats.getPrepareStatementCount()).isEqualTo(5);
        assertThat(countRows("list_product_tags")).isEqualTo(4);
    }

    @Test
    @DisplayName("3. Usunięcie tagu przypiętego do produktu (produkt nie w pamięci) → naruszenie klucza obcego w bazie")
    void removingLinkedTagViolatesForeignKey() {
        productWithTags("bio");                       // kończy się em.clear() — produktu nie ma w pamięci
        ProductTag bio = em.getEntityManager().createQuery("select t from ProductTag t where t.name = 'bio'", ProductTag.class)
                .getSingleResult();

        em.remove(bio);                               // ProductTag to strona odwrotna — product_tags nikt nie czyści

        assertThatThrownBy(() -> em.flush()).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("3. To samo, gdy produkt JEST w pamięci: Hibernate łapie to przy flush — TransientPropertyValueException")
    void removingLinkedTagDetectedInMemory() {
        Long productId = productWithTags("bio");
        ProductTag bio = em.find(Product.class, productId).getTags().iterator().next();

        em.remove(bio);

        assertThatThrownBy(() -> em.flush())
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(TransientPropertyValueException.class)
                .hasMessageContaining("references an unsaved transient instance")
                .hasMessageContaining("(persist the transient instance before flushing)");
    }

    @Test
    @DisplayName("3. Poprawnie: najpierw odpiąć tag od wszystkich produktów, potem usunąć")
    void removingTagAfterUnlinking() {
        Long productId = productWithTags("bio", "promocja");
        Product product = em.find(Product.class, productId);
        ProductTag bio = tagNamed(product, "bio");

        List.copyOf(bio.getProducts()).forEach(p -> p.removeTag(bio));   // kopia — nie zmieniamy zbioru w pętli
        em.remove(bio);
        em.flush();

        assertThat(countRows("tags")).isEqualTo(1);
        assertThat(countRows("products")).isEqualTo(1);
    }

    @Test
    @DisplayName("4. PUŁAPKA: cascade = REMOVE na @ManyToMany usuwa tag wspólny z innym produktem → błąd klucza obcego")
    void cascadeRemoveOnManyToManyIsDangerous() {
        ProductTag promo = em.persist(new ProductTag("promocja"));
        DangerousProduct first = new DangerousProduct("Kawa");
        DangerousProduct second = new DangerousProduct("Herbata");
        first.getTags().add(promo);
        second.getTags().add(promo);
        em.persist(first);
        em.persist(second);
        em.flush();
        em.clear();

        em.remove(em.find(DangerousProduct.class, first.getId()));   // kaskada: usuń też tag „promocja”

        assertThatThrownBy(() -> em.flush()).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("4. Gdy tag nie ma innych produktów, kaskada „po cichu” go usuwa — znika z całego sklepu")
    void cascadeRemoveSilentlyDeletesTag() {
        ProductTag promo = em.persist(new ProductTag("promocja"));
        DangerousProduct only = new DangerousProduct("Kawa");
        only.getTags().add(promo);
        em.persist(only);
        em.flush();
        em.clear();

        em.remove(em.find(DangerousProduct.class, only.getId()));
        em.flush();

        assertThat(countRows("tags")).as("tag usunięty razem z produktem").isZero();
    }

    @Test
    @DisplayName("4. Bez kaskady: usunięcie produktu kasuje tylko jego wiersze w product_tags — tagi zostają")
    void removingOwnerDeletesOnlyLinks() {
        Long productId = productWithTags("a", "b");

        em.remove(em.find(Product.class, productId));
        em.flush();

        assertThat(countRows("product_tags")).isZero();
        assertThat(countRows("tags")).isEqualTo(2);
    }

    @Test
    @DisplayName("5. Relacja z dodatkową kolumną: encja OrderLine (dwa @ManyToOne + quantity) i suma w JPQL")
    void linkEntityWithExtraColumn() {
        Product coffee = em.persist(new Product("Kawa ziarnista 1kg", new BigDecimal("64.99")));
        Product chocolate = em.persist(new Product("Czekolada gorzka", new BigDecimal("7.49")));
        ShopOrder jan = em.persist(new ShopOrder("Jan Kowalski"));
        ShopOrder maria = em.persist(new ShopOrder("Maria Nowak"));
        em.persist(new OrderLine(jan, coffee, 2));
        em.persist(new OrderLine(jan, chocolate, 1));
        em.persist(new OrderLine(maria, coffee, 3));
        em.flush();

        List<ProductSales> sales = em.getEntityManager().createQuery("""
                        select new s09_jpa_relations.rel03_many_to_many.ProductSales(l.product.name, sum(l.quantity))
                        from OrderLine l
                        group by l.product.name
                        order by l.product.name""", ProductSales.class)
                .getResultList();

        assertThat(sales).containsExactly(
                new ProductSales("Czekolada gorzka", 1),
                new ProductSales("Kawa ziarnista 1kg", 5));
    }

    private Long productWithTags(String... tagNames) {
        Product product = new Product("Produkt", BigDecimal.ONE);
        for (String name : tagNames) {
            product.addTag(em.persist(new ProductTag(name)));
        }
        em.persist(product);
        em.flush();
        em.clear();
        return product.getId();
    }

    private ListProduct listProductWithTags(String... tagNames) {
        ListProduct product = new ListProduct("Kawa");
        for (String name : tagNames) {
            product.getTags().add(em.persist(new ProductTag(name)));
        }
        em.persist(product);
        em.flush();
        em.clear();
        return em.find(ListProduct.class, product.getId());
    }

    private static ProductTag tagNamed(Product product, String name) {
        return product.getTags().stream().filter(t -> t.getName().equals(name)).findFirst().orElseThrow();
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
