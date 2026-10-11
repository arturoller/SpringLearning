package s08_data_jpa.jpa01_entities;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.hibernate.InstantiationException;
import org.hibernate.PropertyValueException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jpa01Entities — „WYNIK” każdej sekcji.
 *
 * <p>{@code @DataJpaTest} (test danych JPA) to wycinek (slice) kontekstu: tylko encje, repozytoria, DataSource,
 * Hibernate i TestEntityManager — bez reszty aplikacji. Każdy test biegnie w transakcji, która na końcu jest
 * WYCOFYWANA (rollback), więc testy nie widzą nawzajem swoich danych. Klasę startową (Jpa01Application) test znajduje
 * sam, idąc w górę drzewa pakietów.</p>
 *
 * <p>TestEntityManager — pomocnik do testów: persist (utrwal), flush (wypchnij zmiany do bazy), clear (wyczyść kontekst
 * utrwalania), find (znajdź). JdbcClient — czyste zapytania SQL, żeby zajrzeć do tabeli „obok” Hibernate. W testach
 * NIE sprawdzamy konkretnych wartości id — autonumeracja w bazie nie cofa się przy rollbacku.</p>
 */
@DataJpaTest
@ActiveProfiles("jpa01")
class Jpa01EntitiesTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    EntityManagerFactory emf;

    @Test
    @DisplayName("1. Encja trafia do tabeli products, a lekcja widzi TYLKO swoje encje (izolacja przez pakiet)")
    void entityMapsToTable() {
        Product kawa = em.persistAndFlush(new Product("Kawa ziarnista", "KAWA-001", new BigDecimal("29.99")));

        String nameInTable = jdbc.sql("select name from products where id = ?").param(kawa.getId())
                .query(String.class).single();
        assertThat(nameInTable).isEqualTo("Kawa ziarnista");
        assertThat(emf.getMetamodel().getEntities()).extracting(EntityType::getName)
                .containsExactlyInAnyOrder("Product", "Category", "LegacyProduct", "NaiveCustomer", "Customer",
                        "RecordProduct");
    }

    @Test
    @DisplayName("2. IDENTITY: INSERT od razu przy persist; SEQUENCE: id z sekwencji, INSERT dopiero przy flush")
    void identityVersusSequence() {
        Product product = new Product("Herbata", "HERB-001", new BigDecimal("15.99"));
        em.persist(product);
        assertThat(product.getId()).as("IDENTITY — id nadane przez bazę").isNotNull();
        assertThat(countRows("products")).as("IDENTITY — wiersz już jest w tabeli").isEqualTo(1);

        Category category = new Category("Napoje");
        em.persist(category);
        assertThat(category.getId()).as("SEQUENCE — id pobrane z sekwencji").isNotNull();
        assertThat(countRows("categories")).as("SEQUENCE — INSERT jeszcze czeka").isZero();

        em.flush();
        assertThat(countRows("categories")).as("po flush INSERT poszedł do bazy").isEqualTo(1);
    }

    @Test
    @DisplayName("3. @Column(nullable = false) — Hibernate odrzuca null już przy persist (PropertyValueException)")
    void notNullColumn() {
        assertThatThrownBy(() -> em.persist(new Product(null, "PUSTY-001", BigDecimal.ONE)))
                .isInstanceOf(PropertyValueException.class)
                .hasMessageContaining("not-null property");
    }

    @Test
    @DisplayName("3. @Column(unique = true) — powtórzony SKU łamie ograniczenie UNIQUE w bazie")
    void uniqueColumn() {
        em.persist(new Product("Kawa A", "DUBEL-001", BigDecimal.ONE));

        // IDENTITY → INSERT od razu przy persist, więc błąd bazy leci już tutaj, nie dopiero przy flush.
        assertThatThrownBy(() -> em.persist(new Product("Kawa B", "DUBEL-001", BigDecimal.TEN)))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("4. Enum: STRING zapisuje nazwę stałej, ORDINAL — jej numer pozycji")
    void enumMapping() {
        Product product = new Product("Czekolada", "CZEK-001", new BigDecimal("8.99"));
        product.setStatus(ProductStatus.HIDDEN);
        em.persist(product);
        LegacyProduct legacy = em.persist(new LegacyProduct("Stara czekolada", ProductStatus.HIDDEN));
        em.flush();

        assertThat(jdbc.sql("select status from products where id = ?").param(product.getId())
                .query(String.class).single()).isEqualTo("HIDDEN");
        assertThat(jdbc.sql("select status from legacy_products where id = ?").param(legacy.getId())
                .query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    @DisplayName("5. Odczyt z bazy tworzy NOWY obiekt przez konstruktor bez argumentów")
    void readCreatesNewObject() {
        Product saved = em.persistFlushFind(new Product("Kakao", "KAKAO-001", new BigDecimal("12.00")));
        em.clear();

        Product loaded = em.find(Product.class, saved.getId());

        assertThat(loaded).isNotSameAs(saved);
        assertThat(loaded.getName()).isEqualTo("Kakao");
        assertThat(loaded).as("equals po id").isEqualTo(saved);
    }

    @Test
    @DisplayName("5. Rekord jako encja: zapis działa, odczyt kończy się InstantiationException (brak konstruktora bez argumentów)")
    void recordAsEntityCannotBeRead() {
        em.persistAndFlush(new RecordProduct(1L, "Kawa w rekordzie"));
        em.clear();

        assertThatThrownBy(() -> em.find(RecordProduct.class, 1L))
                .isInstanceOf(InstantiationException.class)
                .hasMessageContaining("No default constructor");
    }

    @Test
    @DisplayName("6. PUŁAPKA: hashCode z id — encja dodana do HashSet przed zapisem „ginie” w zbiorze po zapisie")
    void naiveHashCodeBreaksHashSet() {
        Set<NaiveCustomer> naiveSet = new HashSet<>();
        NaiveCustomer naive = new NaiveCustomer("anna@sklep.pl");
        naiveSet.add(naive);
        em.persist(naive);                              // id null → liczba, hashCode się zmienił

        assertThat(naiveSet.contains(naive)).isFalse();

        Set<NaiveCustomer> twoNew = new HashSet<>();
        twoNew.add(new NaiveCustomer("ola@sklep.pl"));
        twoNew.add(new NaiveCustomer("piotr@sklep.pl"));
        assertThat(twoNew).as("dwa NOWE obiekty mają id == null, więc są „równe”").hasSize(1);
    }

    @Test
    @DisplayName("6. DOBRA PRAKTYKA: stały hashCode i equals po nie-null id — zbiór działa przed i po zapisie")
    void correctHashCodeKeepsHashSetWorking() {
        Set<Customer> customers = new HashSet<>();
        Customer customer = new Customer("anna@sklep.pl");
        customers.add(customer);
        customers.add(new Customer("ola@sklep.pl"));
        em.persist(customer);

        assertThat(customers.contains(customer)).isTrue();
        assertThat(customers).hasSize(2);
    }

    @Test
    @DisplayName("7. Boot dla bazy wbudowanej bez Flyway ustawia ddl-auto = create-drop")
    void defaultDdlAuto() {
        assertThat(emf.getProperties().get("hibernate.hbm2ddl.auto")).isEqualTo("create-drop");
    }

    @Test
    @DisplayName("8. Dirty checking: zmiana zarządzanej encji w transakcji to UPDATE bez wołania save/persist")
    void dirtyChecking() {
        Long id = em.persistAndGetId(new Product("Kawa mielona", "KAWA-002", new BigDecimal("10.00")), Long.class);
        em.flush();
        em.clear();                                      // pusty kontekst — kolejny find naprawdę czyta z bazy

        Product managed = em.find(Product.class, id);    // encja ZARZĄDZANA (managed)
        managed.setPrice(new BigDecimal("12.50"));       // tylko setter — żadnego save!
        em.flush();                                      // Hibernate porównuje ze „zdjęciem” i wysyła UPDATE

        BigDecimal priceInTable = jdbc.sql("select price from products where id = ?").param(id)
                .query(BigDecimal.class).single();
        assertThat(priceInTable).isEqualByComparingTo("12.50");
    }

    @Test
    @DisplayName("8. Encja odłączona (detached) po clear — zmiany NIE trafiają do bazy")
    void detachedEntityIsNotTracked() {
        Product product = em.persistFlushFind(new Product("Herbata zielona", "HERB-002", new BigDecimal("9.00")));
        em.clear();                                      // product jest teraz odłączony

        product.setPrice(new BigDecimal("99.00"));
        em.flush();

        assertThat(em.getEntityManager().contains(product)).isFalse();
        BigDecimal priceInTable = jdbc.sql("select price from products where id = ?").param(product.getId())
                .query(BigDecimal.class).single();
        assertThat(priceInTable).isEqualByComparingTo("9.00");
    }

    private long countRows(String table) {
        return jdbc.sql("select count(*) from " + table).query(Long.class).single();
    }
}
