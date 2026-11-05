package s08_data_jpa.jpa01_entities;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Jpa01Entities (sekcje 1–4) na wycinku {@code @DataJpaTest} (data JPA test = test warstwy JPA).
 * Wycinek tworzy tylko to, co potrzebne do JPA: bazę, EntityManagerFactory, encje i repozytoria — bez serwisów
 * i bez weba. Każdy test to jedna transakcja wycofywana na końcu (rollback), więc testy nie widzą swoich danych.
 *
 * <p>Uwaga o bazie: {@code @DataJpaTest} domyślnie PODMIENIA DataSource na wbudowaną bazę z losową nazwą
 * (adres z profilu jdbc:h2:mem:jpa01 jest pomijany), ale pozostałe ustawienia profilu jpa01 (ddl-auto, statystyki,
 * logger SQL) działają. {@code showSql = false} wyłącza drugi, zbędny wydruk SQL (wycinek domyślnie włącza
 * {@code spring.jpa.show-sql}, a profil ma już logger {@code org.hibernate.SQL}).</p>
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("jpa01")
class Jpa01EntitiesTest {

    @Autowired
    private EntityManager em; // współdzielone proxy — w teście wskazuje EntityManager transakcji testu

    @Autowired
    private TestEntityManager tem; // pomocnik testowy: persistAndFlush, flush, clear

    @Autowired
    private JdbcClient jdbc; // odczyt „prosto z bazy”, z pominięciem kontekstu utrwalania

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Statistics stats;

    @BeforeEach
    void setUp() {
        stats = emf.unwrap(SessionFactory.class).getStatistics(); // unwrap = rozpakuj do typu Hibernate
        stats.clear();
    }

    private static Product mouse() {
        return new Product("Mysz", Category.COMPUTERS, new BigDecimal("59.00"), 30, Instant.parse("2026-10-01T08:00:00Z"));
    }

    private Map<String, Object> column(String table, String column) {
        return jdbc.sql("""
                        SELECT data_type, character_maximum_length, numeric_precision, numeric_scale, is_nullable
                        FROM information_schema.columns
                        WHERE table_schema = 'PUBLIC' AND table_name = :table AND column_name = :column""")
                .param("table", table).param("column", column)
                .query().singleRow();
    }

    // ---- 1 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 1: Boot tworzy EntityManagerFactory (Hibernate SessionFactory) i JpaTransactionManager, a metamodel ma tylko encje lekcji")
    void section1Layers() {
        assertThat(emf.unwrap(SessionFactory.class)).as("implementacją JPA jest Hibernate").isNotNull();
        assertThat(transactionManager).isInstanceOf(JpaTransactionManager.class);
        assertThat(emf.getMetamodel().getEntities()).extracting(type -> (Object) type.getJavaType())
                .as("encje z pakietu klasy startowej lekcji — i żadne inne")
                .containsExactlyInAnyOrder(Product.class, Customer.class);
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: create-drop tworzy tabelę product zgodnie z adnotacjami (@Column, @Enumerated(STRING), @Transient)")
    void section2GeneratedDdl() {
        assertThat(column("PRODUCT", "NAME")).containsEntry("DATA_TYPE", "CHARACTER VARYING")
                .containsEntry("CHARACTER_MAXIMUM_LENGTH", 100L).containsEntry("IS_NULLABLE", "NO");
        assertThat(column("PRODUCT", "PRICE")).containsEntry("DATA_TYPE", "NUMERIC")
                .containsEntry("NUMERIC_PRECISION", 10).containsEntry("NUMERIC_SCALE", 2);
        assertThat(column("PRODUCT", "STOCK")).containsEntry("DATA_TYPE", "INTEGER").containsEntry("IS_NULLABLE", "NO");
        assertThat(column("PRODUCT", "CATEGORY")).containsEntry("DATA_TYPE", "ENUM");
        assertThat(column("PRODUCT", "CREATED_AT")).containsEntry("DATA_TYPE", "TIMESTAMP WITH TIME ZONE");
        assertThat(column("CUSTOMER", "REGISTERED_ON")).as("registeredOn → registered_on").containsEntry("DATA_TYPE", "DATE");
        assertThat(column("CUSTOMER", "EMAIL")).containsEntry("CHARACTER_MAXIMUM_LENGTH", 255L).containsEntry("IS_NULLABLE", "YES");

        assertThat(jdbc.sql("SELECT column_name FROM information_schema.columns WHERE table_name = 'PRODUCT' ORDER BY column_name")
                .query(String.class).list())
                .as("pole @Transient basketQuantity nie ma kolumny")
                .containsExactly("CATEGORY", "CREATED_AT", "ID", "NAME", "PRICE", "STOCK");

        Product saved = tem.persistFlushFind(mouse());
        assertThat(jdbc.sql("SELECT category FROM product WHERE id = :id").param("id", saved.getId())
                .query(String.class).single()).as("STRING = nazwa stałej").isEqualTo("COMPUTERS");
    }

    @Test
    @DisplayName("Sekcja 2: @GeneratedValue bez strategii (AUTO) daje na H2 sekwencję customer_seq z krokiem 50")
    void section2AutoMeansSequence() {
        Map<String, Object> sequence = jdbc.sql("""
                        SELECT sequence_name, increment FROM information_schema.sequences WHERE sequence_schema = 'PUBLIC'""")
                .query().singleRow();
        assertThat(sequence).containsEntry("SEQUENCE_NAME", "CUSTOMER_SEQ").containsEntry("INCREMENT", 50L);
        assertThat(column("PRODUCT", "ID")).as("IDENTITY: bez sekwencji, autonumeracja kolumny").containsEntry("IS_NULLABLE", "NO");
    }

    @Test
    @DisplayName("Sekcja 2: PUŁAPKA — enum bez @Enumerated (ORDINAL) zapisuje NUMER stałej w kolumnie TINYINT")
    void section2OrdinalTrap() {
        Customer anna = tem.persistAndFlush(new Customer("anna@example.com", CustomerLevel.GOLD, LocalDate.of(2026, 3, 15)));

        assertThat(column("CUSTOMER", "LEVEL")).containsEntry("DATA_TYPE", "TINYINT");
        assertThat(jdbc.sql("SELECT level FROM customer WHERE id = :id").param("id", anna.getId())
                .query(Integer.class).single()).as("GOLD to druga stała → 1").isEqualTo(1);
    }

    // ---- 3 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 3: persist przy IDENTITY wysyła INSERT od razu, przy SEQUENCE dopiero przy flush")
    void section3PersistAndIdStrategies() {
        Product product = mouse();
        assertThat(product.getId()).isNull();

        em.persist(product);
        assertThat(product.getId()).as("IDENTITY: id znane zaraz po persist").isNotNull();
        assertThat(stats.getEntityInsertCount()).as("IDENTITY: INSERT już poszedł").isEqualTo(1);

        stats.clear();
        Customer customer = new Customer("jan@example.com", CustomerLevel.STANDARD, LocalDate.of(2026, 1, 2));
        em.persist(customer);
        assertThat(customer.getId()).as("SEQUENCE: id z sekwencji").isNotNull();
        assertThat(stats.getEntityInsertCount()).as("SEQUENCE: INSERT czeka do flush").isZero();
        assertThat(stats.getPrepareStatementCount()).as("tylko 'select next value for customer_seq'").isEqualTo(1);

        em.flush();
        assertThat(stats.getEntityInsertCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Sekcja 3: dwa find tego samego id → ten sam obiekt i jeden SELECT; find nieistniejącego → null")
    void section3FirstLevelCache() {
        Long id = tem.persistAndFlush(mouse()).getId();
        tem.clear(); // kontekst pusty — następny find musi iść do bazy
        stats.clear();

        Product first = em.find(Product.class, id);
        Product second = em.find(Product.class, id);

        assertThat(second).isSameAs(first);
        assertThat(stats.getPrepareStatementCount()).as("drugi find z pamięci kontekstu").isEqualTo(1);
        assertThat(em.find(Product.class, 999L)).as("brak wiersza = null, nie wyjątek").isNull();
    }

    @Test
    @DisplayName("Sekcja 3: remove oznacza encję do usunięcia, DELETE idzie do bazy przy flush")
    void section3Remove() {
        Product product = tem.persistAndFlush(mouse());

        em.remove(product);
        assertThat(jdbc.sql("SELECT count(*) FROM product").query(Long.class).single()).as("przed flush wiersz jest").isEqualTo(1);

        em.flush();
        assertThat(jdbc.sql("SELECT count(*) FROM product").query(Long.class).single()).isZero();
        assertThat(em.find(Product.class, product.getId())).isNull();
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4: stany encji — transient, managed, detached, removed (em.contains)")
    void section4States() {
        Product product = mouse();
        assertThat(em.contains(product)).as("TRANSIENT").isFalse();

        em.persist(product);
        assertThat(em.contains(product)).as("MANAGED").isTrue();

        em.detach(product);
        assertThat(em.contains(product)).as("DETACHED").isFalse();
        assertThat(product.getId()).as("odłączona encja nadal ma id").isNotNull();

        Product managed = em.find(Product.class, product.getId());
        em.remove(managed);
        assertThat(em.contains(managed)).as("REMOVED — już nie należy do kontekstu").isFalse();
    }

    @Test
    @DisplayName("Sekcja 4: dirty checking — zmiana pola encji zarządzanej daje UPDATE przy flush bez żadnego save")
    void section4DirtyChecking() {
        Product product = tem.persistAndFlush(mouse());
        stats.clear();

        product.setPrice(new BigDecimal("49.00")); // tylko setter!
        em.flush();

        assertThat(stats.getEntityUpdateCount()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT price FROM product WHERE id = :id").param("id", product.getId())
                .query(BigDecimal.class).single()).isEqualByComparingTo("49.00");

        stats.clear();
        em.flush();
        assertThat(stats.getPrepareStatementCount()).as("bez zmian — flush nic nie wysyła").isZero();
    }

    @Test
    @DisplayName("Sekcja 4: zmiana encji odłączonej nie trafia do bazy; merge zwraca ZARZĄDZANĄ kopię")
    void section4DetachAndMerge() {
        Product product = tem.persistAndFlush(mouse());
        em.detach(product);

        product.setStock(5);
        em.flush();
        assertThat(jdbc.sql("SELECT stock FROM product WHERE id = :id").param("id", product.getId())
                .query(Integer.class).single()).as("detached: zmiana zignorowana").isEqualTo(30);

        Product managed = em.merge(product);
        assertThat(managed).as("merge zwraca inny obiekt").isNotSameAs(product);
        assertThat(em.contains(managed)).isTrue();
        assertThat(em.contains(product)).as("argument merge nadal odłączony").isFalse();

        product.setStock(1); // PUŁAPKA: zmiana oryginału PO merge
        em.flush();
        assertThat(jdbc.sql("SELECT stock FROM product WHERE id = :id").param("id", product.getId())
                .query(Integer.class).single()).as("zapisany stan z chwili merge, nie późniejsza zmiana").isEqualTo(5);
    }

    @Test
    @DisplayName("Sekcja 4: PUŁAPKA — bez flush/clear find czyta pamięć kontekstu, a baza ma jeszcze starą wartość")
    void section4ReadFromMemoryTrap() {
        Product product = tem.persistAndFlush(mouse());

        product.setPrice(new BigDecimal("1.00"));

        assertThat(em.find(Product.class, product.getId()).getPrice()).as("pamięć").isEqualByComparingTo("1.00");
        assertThat(jdbc.sql("SELECT price FROM product WHERE id = :id").param("id", product.getId())
                .query(BigDecimal.class).single()).as("baza (JDBC, bez flush)").isEqualByComparingTo("59.00");

        tem.flush();
        tem.clear();
        assertThat(em.find(Product.class, product.getId()).getPrice()).as("po flush + clear: odczyt z bazy")
                .isEqualByComparingTo("1.00");
    }
}
