package s09_jpa_relations.rel01_many_to_one;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Hibernate;
import org.hibernate.PropertyValueException;
import org.hibernate.SessionFactory;
import org.hibernate.TransientPropertyValueException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Rel01ManyToOne — „WYNIK” każdej sekcji.
 *
 * <p>{@code @DataJpaTest} — wycinek kontekstu z encjami, repozytoriami, Flyway i TestEntityManager; każdy test w
 * transakcji wycofywanej na końcu (rollback). {@code @ActiveProfiles("rel01")} włącza profil lekcji: migracje
 * z db/migration/rel01 i ddl-auto=validate. Schemat tworzy Flyway, a Hibernate tylko go sprawdza.</p>
 *
 * <p>Wzorzec „flush + clear”: flush wysyła zaległe INSERT-y do bazy, clear opróżnia kontekst utrwalania. Dopiero
 * wtedy find naprawdę czyta z bazy — inaczej Hibernate oddałby obiekt z pamięci i nie zobaczylibyśmy LAZY/EAGER.</p>
 */
@DataJpaTest
@ActiveProfiles("rel01")
class Rel01ManyToOneTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    ProductRepository products;

    @Autowired
    CategoryRepository categories;

    @Autowired
    EntityManagerFactory emf;

    @Test
    @DisplayName("1. @ManyToOne + @JoinColumn: id kategorii trafia do kolumny category_id w tabeli products")
    void foreignKeyLivesInProductsTable() {
        Category coffee = em.persist(new Category("Kawa"));
        Product arabica = em.persist(new Product("Arabica", new BigDecimal("29.99"), coffee));
        em.flush();

        Long categoryIdInTable = jdbc.sql("select category_id from products where id = ?").param(arabica.getId())
                .query(Long.class).single();
        assertThat(categoryIdInTable).isEqualTo(coffee.getId());
    }

    @Test
    @DisplayName("2. Domyślny fetch @ManyToOne to EAGER: kategoria wczytana od razu (left join w tym samym SELECT)")
    void defaultFetchIsEager() {
        Category coffee = em.persist(new Category("Kawa"));
        Long id = em.persistAndGetId(new EagerProduct("Robusta", coffee), Long.class);
        em.flush();
        em.clear();

        EagerProduct loaded = em.find(EagerProduct.class, id);

        assertThat(Hibernate.isInitialized(loaded.getCategory())).isTrue();
        assertThat(loaded.getCategory().getClass()).isEqualTo(Category.class);
    }

    @Test
    @DisplayName("2. EAGER w zapytaniu JPQL: 1 SELECT na produkty + osobny SELECT na każdą kategorię (3 kategorie → 4)")
    void eagerInJpqlCausesExtraSelects() {
        for (String name : new String[]{"Kawa", "Herbata", "Kakao"}) {
            em.persist(new EagerProduct("Produkt " + name, em.persist(new Category(name))));
        }
        em.flush();
        em.clear();
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        var all = em.getEntityManager()
                .createQuery("select p from EagerProduct p", EagerProduct.class).getResultList();

        assertThat(all).hasSize(3);
        assertThat(stats.getPrepareStatementCount()).as("1 zapytanie JPQL + 3 dociągnięcia kategorii").isEqualTo(4);
    }

    @Test
    @DisplayName("2. fetch = LAZY: w polu siedzi niezainicjowane proxy; getId go nie ładuje, getName — ładuje")
    void lazyFetchGivesProxy() {
        Category coffee = em.persist(new Category("Kawa"));
        Long id = em.persistAndGetId(new Product("Arabica", new BigDecimal("29.99"), coffee), Long.class);
        em.flush();
        em.clear();

        Product loaded = em.find(Product.class, id);
        Category proxy = loaded.getCategory();

        assertThat(Hibernate.isInitialized(proxy)).as("po find kategoria NIE jest wczytana").isFalse();
        assertThat(proxy.getClass().getSimpleName()).isEqualTo("Category$HibernateProxy");
        assertThat(proxy.getId()).isEqualTo(coffee.getId());
        assertThat(Hibernate.isInitialized(proxy)).as("getId zna proxy bez bazy").isFalse();
        assertThat(proxy.getName()).isEqualTo("Kawa");                    // tu idzie drugi SELECT
        assertThat(Hibernate.isInitialized(proxy)).isTrue();
    }

    @Test
    @DisplayName("3. optional = false: produkt bez kategorii odrzucony już przy persist (PropertyValueException)")
    void requiredCategory() {
        assertThatThrownBy(() -> em.persist(new Product("Bez kategorii", BigDecimal.ONE, null)))
                .isInstanceOf(PropertyValueException.class)
                .hasMessageContaining("not-null property references a null or transient value");
    }

    @Test
    @DisplayName("4. Niezapisana (transient) kategoria: już persist rzuca IllegalStateException z TransientPropertyValueException")
    void transientCategoryWithEntityManager() {
        Category notSaved = new Category("Nowa kategoria");

        // W lambdzie TYLKO persist — dowód, że wyjątek nie czeka na flush.
        assertThatThrownBy(() -> em.persist(new Product("Kakao", BigDecimal.TEN, notSaved)))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(TransientPropertyValueException.class)
                .hasMessageContaining("references an unsaved transient instance");
    }

    @Test
    @DisplayName("4. To samo przez repozytorium (save): Spring tłumaczy wyjątek na InvalidDataAccessApiUsageException")
    void transientCategoryWithRepository() {
        Category notSaved = new Category("Nowa kategoria");

        assertThatThrownBy(() -> products.save(new Product("Kakao", BigDecimal.TEN, notSaved)))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasRootCauseInstanceOf(TransientPropertyValueException.class);
    }

    @Test
    @DisplayName("4. Poprawnie: najpierw zapisana kategoria, potem produkt")
    void persistedCategoryWorks() {
        Category saved = categories.save(new Category("Herbata"));
        Product product = products.saveAndFlush(new Product("Earl Grey", new BigDecimal("12.50"), saved));

        assertThat(product.getId()).isNotNull();
        assertThat(products.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("5. getReferenceById: ustawia klucz obcy bez wczytywania kategorii z bazy")
    void referenceSetsForeignKeyWithoutSelect() {
        Category coffee = em.persist(new Category("Kawa"));
        Category tea = em.persist(new Category("Herbata"));
        Long productId = em.persistAndGetId(new Product("Mieszanka", BigDecimal.ONE, coffee), Long.class);
        em.flush();
        em.clear();

        Product product = em.find(Product.class, productId);
        Category teaReference = categories.getReferenceById(tea.getId());   // proxy, bez SELECT
        product.setCategory(teaReference);
        em.flush();

        assertThat(Hibernate.isInitialized(teaReference)).isFalse();
        Long fkInTable = jdbc.sql("select category_id from products where id = ?").param(productId)
                .query(Long.class).single();
        assertThat(fkInTable).isEqualTo(tea.getId());
    }

    @Test
    @DisplayName("5. PUŁAPKA: getReferenceById ze złym id — błąd dopiero przy użyciu proxy albo przy flush")
    void referenceToMissingRowFailsLater() {
        Category coffee = em.persist(new Category("Kawa"));
        Long productId = em.persistAndGetId(new Product("Mieszanka", BigDecimal.ONE, coffee), Long.class);
        em.flush();
        em.clear();

        Category ghost = categories.getReferenceById(999L);              // brak wyjątku — nikt nie pyta bazy
        assertThat(ghost.getId()).isEqualTo(999L);
        assertThatThrownBy(ghost::getName)                               // dopiero teraz SELECT — i pusto
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("No row with the given identifier exists");

        em.clear();
        Product product = em.find(Product.class, productId);
        product.setCategory(categories.getReferenceById(999L));
        assertThatThrownBy(() -> em.flush())                              // UPDATE łamie klucz obcy
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("6. Zapytanie przez relację: findByCategoryName... łączy products z categories")
    void queryThroughRelation() {
        Category coffee = em.persist(new Category("Kawa"));
        Category tea = em.persist(new Category("Herbata"));
        em.persist(new Product("Robusta", BigDecimal.ONE, coffee));
        em.persist(new Product("Arabica", BigDecimal.TEN, coffee));
        em.persist(new Product("Sencha", BigDecimal.TEN, tea));
        em.flush();
        em.clear();

        assertThat(products.findByCategoryNameOrderByName("Kawa"))
                .extracting(Product::getName)
                .containsExactly("Arabica", "Robusta");
    }
}
