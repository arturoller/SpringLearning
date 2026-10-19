package s08_data_jpa.jpa01_entities;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.hibernate.PropertyValueException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jpa01Entities.
 *
 * <p>{@code @DataJpaTest} (test wycinkowy JPA — slice) zamiast {@code @SpringBootTest}: buduje tylko to, co potrzebne
 * do JPA (DataSource, Hibernate, repozytoria), podmienia bazę na świeżą H2 o losowej nazwie, a KAŻDY test biegnie
 * w transakcji wycofywanej na końcu — testy nie widzą swoich danych. Dodatkowo włącza show-sql, więc w wyjściu
 * widać linie {@code Hibernate: ...}; {@link OutputCaptureExtension} (przechwytywanie wyjścia) pozwala sprawdzić je
 * asercjami. {@code @ActiveProfiles("jpa01")} wczytuje ustawienia lekcji (Flyway wyłączony, logger SQL).</p>
 */
@DataJpaTest
@ActiveProfiles("jpa01")
@ExtendWith(OutputCaptureExtension.class)
class Jpa01EntitiesTest {

    @Autowired
    EntityManager entityManager;

    /** Tekst wypisany od pozycji {@code start} — żeby sprawdzać tylko SQL z danego kroku testu. */
    private static String since(CapturedOutput output, int start) {
        return output.toString().substring(start);
    }

    private static int occurrences(String text, String fragment) {
        int count = 0;
        for (int i = text.indexOf(fragment); i >= 0; i = text.indexOf(fragment, i + fragment.length())) {
            count++;
        }
        return count;
    }

    private Object single(String nativeSql) {
        return entityManager.createNativeQuery(nativeSql).getSingleResult();
    }

    @Test
    @DisplayName("1. Encja zapisuje się i wczytuje; rekord jako encja startuje, ale pierwszy odczyt rzuca InstantiationException")
    void entityBasicsAndRecordPitfall() {
        Product laptop = new Product("ELE-001", "Laptop Pro 14", Category.ELEKTRONIKA, new BigDecimal("5499.99"), 7);
        entityManager.persist(laptop);
        entityManager.flush();
        entityManager.clear();

        Product loaded = entityManager.find(Product.class, laptop.getId());
        assertThat(loaded).isNotSameAs(laptop);                   // nowy obiekt zbudowany z wiersza tabeli
        assertThat(loaded.getName()).isEqualTo("Laptop Pro 14");
        assertThat(loaded.getPrice()).isEqualByComparingTo("5499.99");
        assertThat(entityManager.getMetamodel().getEntities()).extracting(EntityType::getName)
                .contains("Product", "Customer", "Employee", "RecordProduct");

        // PUŁAPKA: rekord — kontekst wystartował, INSERT działa...
        entityManager.persist(new RecordProduct("SPO-001", "Kawa ziarnista 1kg"));
        entityManager.flush();
        entityManager.clear();
        // ...a odczyt nie, bo Hibernate nie ma czym utworzyć pustego obiektu
        assertThatThrownBy(() -> entityManager.find(RecordProduct.class, "SPO-001"))
                .isInstanceOf(org.hibernate.InstantiationException.class)
                .hasMessageContaining("No default constructor for entity");
    }

    @Test
    @DisplayName("2. SEQUENCE: id od razu, INSERT dopiero przy flush; IDENTITY: INSERT natychmiast przy persist")
    void generatedValueStrategies(CapturedOutput output) {
        // IDENTITY — numer nadaje baza, więc INSERT idzie od razu
        int start = output.length();
        Customer jan = new Customer("Jan Kowalski", "Warszawa", "jan@example.com", true);
        entityManager.persist(jan);
        assertThat(jan.getId()).isNotNull();
        assertThat(since(output, start)).contains("Hibernate: insert into customers");

        // SEQUENCE (AUTO na H2) — id z sekwencji, INSERT odłożony do flush
        start = output.length();
        List<Product> products = List.of(
                new Product("KSI-001", "Czysty kod", Category.KSIAZKI, new BigDecimal("79.00"), 15),
                new Product("KSI-002", "Java. Podstawy", Category.KSIAZKI, new BigDecimal("129.00"), 9),
                new Product("KSI-003", "Wzorce projektowe", Category.KSIAZKI, new BigDecimal("99.00"), 3),
                new Product("DOM-001", "Ekspres do kawy", Category.DOM, new BigDecimal("1899.00"), 2),
                new Product("DOM-002", "Lampka biurkowa", Category.DOM, new BigDecimal("129.00"), 18));
        products.forEach(entityManager::persist);
        String afterPersist = since(output, start);
        assertThat(products).allSatisfy(p -> assertThat(p.getId()).isNotNull());
        assertThat(afterPersist).doesNotContain("insert into products");
        assertThat(occurrences(afterPersist, "Hibernate: select next value for products_seq"))
                .as("numery z sekwencji rezerwowane paczkami po 50 — mniej zapytań niż zapisów").isLessThan(5);

        entityManager.flush();
        assertThat(occurrences(since(output, start), "Hibernate: insert into products")).isEqualTo(5);

        // DDL: sekwencja z krokiem 50 i kolumna IDENTITY
        Object[] sequence = (Object[]) single(
                "select sequence_name, increment from information_schema.sequences where sequence_name = 'PRODUCTS_SEQ'");
        assertThat(((Number) sequence[1]).longValue()).isEqualTo(50);
        assertThat(single("select is_identity from information_schema.columns "
                + "where table_name = 'CUSTOMERS' and column_name = 'ID'")).isEqualTo("YES");
    }

    @Test
    @DisplayName("3. @Column i @Enumerated(STRING) kształtują kolumny; null łapie Hibernate, długość i unikalność — baza")
    void columnsAndEnums() {
        entityManager.persist(new Product("ELE-003", "Słuchawki BT", Category.ELEKTRONIKA, new BigDecimal("349.90"), 25));
        entityManager.flush();

        // EnumType.STRING: w kolumnie leży NAZWA stałej
        assertThat(single("select category from products where sku = 'ELE-003'").toString()).isEqualTo("ELEKTRONIKA");
        // @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
        Object[] price = (Object[]) single("select data_type, numeric_precision, numeric_scale, is_nullable "
                + "from information_schema.columns where table_name = 'PRODUCTS' and column_name = 'UNIT_PRICE'");
        assertThat(price[0]).isEqualTo("NUMERIC");
        assertThat(((Number) price[1]).intValue()).isEqualTo(10);
        assertThat(((Number) price[2]).intValue()).isEqualTo(2);
        assertThat(price[3]).isEqualTo("NO");
        // @Column(length = 20, nullable = false)
        Object[] sku = (Object[]) single("select data_type, character_maximum_length, is_nullable "
                + "from information_schema.columns where table_name = 'PRODUCTS' and column_name = 'SKU'");
        assertThat(sku[0]).isEqualTo("CHARACTER VARYING");
        assertThat(((Number) sku[1]).intValue()).isEqualTo(20);

        // nullable = false: Hibernate sprawdza już przy persist (w tym module nie ma Bean Validation)
        assertThatThrownBy(() -> entityManager.persist(
                new Product("ELE-004", null, Category.ELEKTRONIKA, new BigDecimal("1299.00"), 4)))
                .isInstanceOf(PropertyValueException.class)
                .hasMessageContaining("not-null property references a null or transient value");
        entityManager.clear();   // po wyjątku obiekt i tak został w kontekście — wyrzucamy go, żeby nie psuł flush

        // length = 20: persist przechodzi, protestuje dopiero baza przy flush
        entityManager.persist(new Product("ELE-004-MONITOR-27-CALI", "Monitor 27 cali", Category.ELEKTRONIKA,
                new BigDecimal("1299.00"), 4));
        assertThatThrownBy(entityManager::flush).isInstanceOf(DataException.class);
        entityManager.clear();   // wyrzuca z kontekstu nieudany INSERT

        // unique = true: drugi produkt z tym samym sku — też dopiero baza przy flush
        entityManager.persist(new Product("ELE-003", "Podróbka", Category.ELEKTRONIKA, new BigDecimal("9.99"), 1));
        assertThatThrownBy(entityManager::flush).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("4. Baza wbudowana bez Flyway: Boot ustawia ddl-auto=create-drop i Hibernate sam tworzy tabele")
    void ddlAutoDefault(@Autowired EntityManagerFactory entityManagerFactory,
                        @Autowired PlatformTransactionManager transactionManager) {
        assertThat(entityManagerFactory.getProperties().get("hibernate.hbm2ddl.auto")).isEqualTo("create-drop");
        assertThat(transactionManager).isInstanceOf(JpaTransactionManager.class);
        List<Object> tables = entityManager.createNativeQuery(
                "select table_name from information_schema.tables where table_schema = 'PUBLIC'", Object.class).getResultList();
        assertThat(tables).contains("PRODUCTS", "CUSTOMERS", "EMPLOYEES", "RECORD_PRODUCTS");

        // to samo w prawdziwej aplikacji lekcji (profil jpa01, baza jdbc:h2:mem:jpa01 — też „wbudowana”)
        try (ConfigurableApplicationContext app = new SpringApplicationBuilder(Jpa01Application.class)
                .profiles("jpa01").web(WebApplicationType.NONE).run()) {
            assertThat(app.getBean(EntityManagerFactory.class).getProperties().get("hibernate.hbm2ddl.auto"))
                    .isEqualTo("create-drop");
        }
    }

    @Test
    @DisplayName("5. show-sql pisze „Hibernate: ...” na System.out, logger org.hibernate.SQL — przez system logowania")
    void sqlPreview(CapturedOutput output) {
        int start = output.length();
        entityManager.persist(new Product("ODZ-001", "Kurtka zimowa", Category.ODZIEZ, new BigDecimal("459.00"), 12));
        entityManager.flush();
        String sql = since(output, start);

        // show-sql (włączone przez @DataJpaTest): goła linia bez daty i poziomu
        assertThat(sql).contains("Hibernate: insert into products (category,name,unit_price,sku,stock,id) values (?,?,?,?,?,?)");
        // logger org.hibernate.SQL na poziomie DEBUG (profil jpa01): data, poziom, nazwa loggera
        assertThat(sql.lines())
                .anySatisfy(line -> assertThat(line).contains("DEBUG").contains("org.hibernate.SQL")
                        .contains("insert into products"));
    }

    @Test
    @DisplayName("6. Kontekst utrwalania: jeden obiekt na id, UPDATE bez save (dirty checking), obiekt odłączony nie jest śledzony")
    void persistenceContext(CapturedOutput output) {
        Product coffee = new Product("SPO-001", "Kawa ziarnista 1kg", Category.SPOZYWCZE, new BigDecimal("64.99"), 120);
        entityManager.persist(coffee);
        entityManager.flush();
        entityManager.clear();

        int start = output.length();
        Product first = entityManager.find(Product.class, coffee.getId());
        Product second = entityManager.find(Product.class, coffee.getId());
        assertThat(first).isSameAs(second);                                 // gwarancja tożsamości
        assertThat(occurrences(since(output, start), "Hibernate: select")).isEqualTo(1);   // drugi find z pamięci

        // dirty checking: zmiana pola zarządzanego obiektu, BEZ żadnego save/persist
        first.setPrice(new BigDecimal("59.99"));
        start = output.length();
        entityManager.flush();
        assertThat(since(output, start))
                .contains("Hibernate: update products set category=?,name=?,unit_price=?,sku=?,stock=? where id=?");

        // obiekt odłączony (po clear): zmiany nikt nie śledzi
        entityManager.clear();
        Product detached = first;
        assertThat(entityManager.contains(detached)).isFalse();
        detached.setPrice(new BigDecimal("1.00"));
        entityManager.flush();
        entityManager.clear();
        assertThat(entityManager.find(Product.class, coffee.getId()).getPrice()).isEqualByComparingTo("59.99");
    }

    @Test
    @DisplayName("7. equals/hashCode z generowanego id psuje HashSet; klucz biznesowy i „bezpieczne id” działają")
    void equalsAndHashCode() {
        // ŹLE: Employee — equals/hashCode z id
        Employee anna = new Employee("Anna Nowak", "IT", 14500);
        Employee piotr = new Employee("Piotr Kowalski", "IT", 9800);
        assertThat(new HashSet<>(List.of(anna, piotr))).as("dwaj różni nowi pracownicy sklejeni w jednego").hasSize(1);

        Set<Employee> team = new HashSet<>();
        team.add(anna);
        entityManager.persist(anna);                                     // id: null → liczba, hashCode się zmienia
        assertThat(anna.getId()).isNotNull();
        assertThat(team.contains(anna)).as("ten sam obiekt „zniknął” z HashSet").isFalse();

        // DOBRZE: Product — klucz biznesowy sku
        Product monitor = new Product("ELE-004", "Monitor 27 cali", Category.ELEKTRONIKA, new BigDecimal("1299.00"), 4);
        Set<Product> cart = new HashSet<>();
        cart.add(monitor);
        entityManager.persist(monitor);
        assertThat(cart.contains(monitor)).isTrue();
        assertThat(monitor).isEqualTo(new Product("ELE-004", "inna nazwa", Category.DOM, BigDecimal.ONE, 0));

        // DOBRZE: Customer — nowe obiekty równe tylko sobie, stały hashCode
        Customer maria = new Customer("Maria Nowak", "Kraków", null, false);
        Customer adam = new Customer("Adam Mazur", "Gdańsk", "adam.mazur@example.com", false);
        Set<Customer> customers = new HashSet<>(List.of(maria, adam));
        assertThat(customers).hasSize(2);
        entityManager.persist(maria);
        assertThat(customers.contains(maria)).isTrue();
        entityManager.flush();
        entityManager.clear();
        assertThat(entityManager.find(Customer.class, maria.getId())).isEqualTo(maria);   // inny obiekt, to samo id
    }
}
