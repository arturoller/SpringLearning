package s09_jpa_relations.rel03_many_to_many;

import jakarta.persistence.EntityManager;
import org.hibernate.AssertionFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import s09_jpa_relations.SqlCapture;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Rel03ManyToMany na wycinku {@code @DataJpaTest}. Dane: 5 produktów, 5 tagów, 9 powiązań i 3 ceny
 * dostawców z migracji db/migration/rel03/V2; każdy test jest wycofywany. List przy @ManyToMany i kaskada na N—N —
 * Rel03BrokenMappingsTest w pakiecie rel03_pitfalls (złe encje poza pakietem lekcji).
 */
@DataJpaTest(showSql = false, properties = SqlCapture.PROPERTY)
@ActiveProfiles("rel03")
class Rel03ManyToManyTest {

    private static final String SELECT_TAGS_OF_PRODUCT = "select t1_0.product_id,t1_1.id,t1_1.name from product_tag t1_0 "
            + "join tag t1_1 on t1_1.id=t1_0.tag_id where t1_0.product_id=?";
    private static final String SELECT_PRODUCTS_OF_TAG = "select p1_0.tag_id,p1_1.id,p1_1.name,p1_1.price from product_tag p1_0 "
            + "join product p1_1 on p1_1.id=p1_0.product_id where p1_0.tag_id=?";

    @Autowired
    private ProductRepository products;

    @Autowired
    private TagRepository tags;

    @Autowired
    private SupplierRepository suppliers;

    @Autowired
    private ProductSupplierRepository productSuppliers;

    @Autowired
    private TestEntityManager tem;

    @BeforeEach
    void setUp() {
        SqlCapture.clear();
    }

    private static List<String> names(List<Product> list) {
        return list.stream().map(Product::getName).toList();
    }

    private Tag tag(String name) {
        return tags.findByName(name).orElseThrow();
    }

    // ---- 1 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 1: obie strony N—N wczytują się leniwie, każda jednym zapytaniem z JOIN przez product_tag")
    void section1BothSidesLoadThroughJoinTable() {
        Product mouse = tem.find(Product.class, 2L);
        assertThat(mouse.getTags()).extracting(Tag::getName)
                .containsExactlyInAnyOrder("biuro", "gaming", "promocja", "bezprzewodowe");
        assertThat(SqlCapture.all()).hasSize(2).last().isEqualTo(SELECT_TAGS_OF_PRODUCT);

        Tag office = tem.find(Tag.class, 1L);
        SqlCapture.clear();
        assertThat(office.getProducts()).extracting(Product::getName)
                .containsExactlyInAnyOrder("Klawiatura", "Mysz", "Lampka");
        assertThat(SqlCapture.all()).containsExactly(SELECT_PRODUCTS_OF_TAG);
    }

    @Test
    @DisplayName("Sekcja 1: addTag od strony właściciela → INSERT do product_tag (removeTag wczytuje też tag.products)")
    void section1AddTagThroughOwningSide() {
        Product monitor = products.findById(3L).orElseThrow();
        Tag fresh = tag("nowość");
        SqlCapture.clear();

        monitor.addTag(fresh);
        products.flush();

        assertThat(SqlCapture.all()).as("helper dotknął obu kolekcji — obie się wczytały, potem jeden INSERT")
                .containsExactly(SELECT_TAGS_OF_PRODUCT, SELECT_PRODUCTS_OF_TAG,
                        "insert into product_tag (product_id,tag_id) values (?,?)");
        tem.clear();
        assertThat(products.findByTagsNameOrderByName("nowość")).extracting(Product::getName).containsExactly("Monitor");
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: Set — usunięcie jednego tagu to JEDEN DELETE wskazujący parę (product_id, tag_id)")
    void section2SetRemovesSingleRow() {
        Product mouse = products.findById(2L).orElseThrow();
        Tag gaming = tag("gaming");
        mouse.removeTag(gaming);
        SqlCapture.clear();

        products.flush();

        assertThat(SqlCapture.all()).containsExactly("delete from product_tag where product_id=? and tag_id=?");
        tem.clear();
        assertThat(products.findById(2L).orElseThrow().getTags()).extracting(Tag::getName)
                .containsExactlyInAnyOrder("biuro", "promocja", "bezprzewodowe");
    }

    @Test
    @DisplayName("Sekcja 2: usunięcie produktu bez kaskady — znikają jego wiersze w product_tag, tagi zostają")
    void section2DeletingProductKeepsTags() {
        products.delete(products.findById(4L).orElseThrow()); // Kubek (promocja)
        products.flush();

        assertThat(SqlCapture.all()).contains("delete from product_tag where product_id=?", "delete from product where id=?");
        assertThat(tags.count()).isEqualTo(5);
        tem.clear();
        assertThat(tag("promocja").getProducts()).extracting(Product::getName).containsExactly("Mysz");
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4: encja pośrednia — dane powiązania (cena zakupu), zwykłe zapytania po @ManyToOne")
    void section4IntermediateEntity() {
        List<ProductSupplier> offers = productSuppliers.findByProductIdOrderByPurchasePrice(1L);

        assertThat(offers).extracting(offer -> offer.getSupplier().getName())
                .containsExactly("Hurtownia Beta", "Hurtownia Alfa");
        assertThat(offers).extracting(ProductSupplier::getPurchasePrice)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("105.00"), new BigDecimal("110.00"));

        ProductSupplier added = productSuppliers.save(new ProductSupplier(products.getReferenceById(3L),
                suppliers.getReferenceById(2L), new BigDecimal("700.00")));
        productSuppliers.flush();
        assertThat(added.getId()).isNotNull();
        assertThat(productSuppliers.findByProductIdOrderByPurchasePrice(3L)).hasSize(1);
    }

    @Test
    @DisplayName("Sekcja 4: ta sama para produkt–dostawca drugi raz → UNIQUE w bazie: DataIntegrityViolationException")
    void section4UniquePairInDatabase() {
        ProductSupplier duplicate = new ProductSupplier(products.getReferenceById(2L), suppliers.getReferenceById(1L),
                new BigDecimal("39.00")); // Mysz u Alfy już jest

        assertThatThrownBy(() -> productSuppliers.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Unique index or primary key violation")
                .hasMessageContaining("UK_PRODUCT_SUPPLIER");
    }

    @Test
    @DisplayName("Sekcja 4: PUŁAPKA — dalsza praca w tej samej sesji po wyjątku z bazy → AssertionFailure „null identifier”")
    void section4SessionUnusableAfterException() {
        ProductSupplier duplicate = new ProductSupplier(products.getReferenceById(2L), suppliers.getReferenceById(1L),
                new BigDecimal("39.00"));
        assertThatThrownBy(() -> productSuppliers.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> productSuppliers.findByProductIdOrderByPurchasePrice(1L))
                .isInstanceOf(AssertionFailure.class)
                .hasMessageContaining("has a null identifier (this can happen if the session is flushed after an exception occurs)");
    }

    // ---- 5 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 5: findByTagsName → LEFT JOIN przez tabelę łączącą; JPQL join p.tags → JOIN")
    void section5QueriesThroughJoinTable() {
        assertThat(names(products.findByTagsNameOrderByName("biuro"))).containsExactly("Klawiatura", "Lampka", "Mysz");
        assertThat(SqlCapture.last()).isEqualTo("select p1_0.id,p1_0.name,p1_0.price from product p1_0 "
                + "left join product_tag t1_0 on p1_0.id=t1_0.product_id left join tag t1_1 on t1_1.id=t1_0.tag_id "
                + "where t1_1.name=? order by p1_0.name");

        assertThat(names(products.findWithAnyTag(List.of("biuro", "gaming"))))
                .as("encje-korzenie bez duplikatów, choć SQL zwraca 6 wierszy")
                .containsExactly("Klawiatura", "Lampka", "Monitor", "Mysz");
        assertThat(SqlCapture.last()).isEqualTo("select p1_0.id,p1_0.name,p1_0.price from product p1_0 "
                + "join product_tag t1_0 on p1_0.id=t1_0.product_id join tag t1_1 on t1_1.id=t1_0.tag_id "
                + "where t1_1.name in (?,?) order by p1_0.name");

        assertThat(names(products.findDistinctWithAnyTag(List.of("biuro", "gaming"))))
                .containsExactly("Klawiatura", "Lampka", "Monitor", "Mysz");
        assertThat(SqlCapture.last()).startsWith("select distinct p1_0.id,p1_0.name,p1_0.price from product p1_0");
    }

    @Test
    @DisplayName("Sekcja 5: PUŁAPKA — wartości (p.name) po join kolekcji mają duplikaty; distinct je usuwa")
    void section5DuplicatesForValuesNotForEntities() {
        EntityManager em = tem.getEntityManager();
        String where = " from Product p join p.tags t where t.name in ('biuro', 'gaming') order by p.name";

        assertThat(em.createNativeQuery("select p.name from product p join product_tag pt on pt.product_id = p.id "
                + "join tag t on t.id = pt.tag_id where t.name in ('biuro', 'gaming') order by p.name").getResultList())
                .as("baza: jeden wiersz na każdy pasujący tag")
                .containsExactly("Klawiatura", "Klawiatura", "Lampka", "Monitor", "Mysz", "Mysz");
        assertThat(em.createQuery("select p.name" + where, String.class).getResultList())
                .containsExactly("Klawiatura", "Klawiatura", "Lampka", "Monitor", "Mysz", "Mysz");
        assertThat(em.createQuery("select distinct p.name" + where, String.class).getResultList())
                .containsExactly("Klawiatura", "Lampka", "Monitor", "Mysz");
        assertThat(em.createQuery("select p" + where, Product.class).getResultList()).extracting(Product::getName)
                .as("encje — Hibernate 6+ sam usuwa powtórzenia")
                .containsExactly("Klawiatura", "Lampka", "Monitor", "Mysz");
    }

    @Test
    @DisplayName("Sekcja 5: liczba produktów na tag — left join daje też tag bez produktów (0)")
    void section5CountPerTagWithLeftJoin() {
        assertThat(tags.countProductsPerTag()).containsExactly(
                new TagCount("bezprzewodowe", 1), new TagCount("biuro", 3), new TagCount("gaming", 3),
                new TagCount("nowość", 0), new TagCount("promocja", 2));
        assertThat(SqlCapture.last()).isEqualTo("select t1_0.name,count(p1_0.product_id) from tag t1_0 "
                + "left join product_tag p1_0 on t1_0.id=p1_0.tag_id group by t1_0.name order by t1_0.name");

        assertThat(tem.getEntityManager().createQuery(
                        "select t.name from Tag t join t.products p group by t.name order by t.name", String.class)
                .getResultList()).as("zwykły join — „nowość” znika").doesNotContain("nowość").hasSize(4);
    }
}
