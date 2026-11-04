package s07_data_jdbc.jdbc01_jdbc_client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import s07_data_jdbc.jdbc01_jdbc_client.Jdbc01ShopQueries.ProductLabel;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jdbc01JdbcClient. Najwęższy wycinek: @JdbcTest (sama warstwa JDBC + skrypty SQL, bez kontrolerów
 * i serwisów). Profil "jdbc01" dokłada schemat i dane lekcji; wzorzec: ten sam profil w ćwiczeniach.
 */
@JdbcTest
@ActiveProfiles("jdbc01")
class Jdbc01JdbcClientTest {

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private DataSource dataSource;

    private Jdbc01ShopQueries shop() {
        return new Jdbc01ShopQueries(jdbc);
    }

    // ---- 1 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 1: wycinek ma JdbcClient i dane startowe, test trwa w transakcji, a baza jest podmieniona")
    void section1SliceAndRollback() throws Exception {
        assertThat(jdbc).isNotNull();
        assertThat(TestTransaction.isActive()).as("test działa w transakcji, którą Spring wycofa").isTrue();
        assertThat(shop().count()).isEqualTo(5);
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL())
                    .as("DataSource podmieniony na wbudowaną bazę o losowej nazwie")
                    .startsWith("jdbc:h2:mem:")
                    .doesNotContain("jdbc01");
        }
    }

    @Test
    @DisplayName("Sekcja 1: aplikacja lekcji uruchomiona normalnie łączy się z bazą jdbc:h2:mem:jdbc01")
    void section1ApplicationUsesOwnDatabase() throws Exception {
        try (ConfigurableApplicationContext app = new SpringApplicationBuilder(Jdbc01Application.class)
                .web(WebApplicationType.NONE)
                .profiles("jdbc01")
                .run()) {
            try (Connection connection = app.getBean(DataSource.class).getConnection()) {
                assertThat(connection.getMetaData().getURL()).isEqualTo("jdbc:h2:mem:jdbc01");
            }
            assertThat(new Jdbc01ShopQueries(app.getBean(JdbcClient.class)).count()).isEqualTo(5);
        }
    }

    @Test
    @DisplayName("Sekcja 1 (rollback) — test A: dodaje produkt i widzi 6 sztuk")
    void section1RollbackA() {
        shop().insert(new Product(null, "Podkładka", "komputery", new BigDecimal("9.99"), 1, null));
        assertThat(shop().count()).isEqualTo(6);
    }

    @Test
    @DisplayName("Sekcja 1 (rollback) — test B: też dodaje produkt i też widzi 6 (nie 7), niezależnie od kolejności")
    void section1RollbackB() {
        shop().insert(new Product(null, "Podkładka", "komputery", new BigDecimal("9.99"), 1, null));
        assertThat(shop().count()).isEqualTo(6);
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: list/single/optional/skalary/własny RowMapper — mapowanie snake_case na camelCase")
    void section2Queries() {
        List<Product> all = shop().findAll();
        assertThat(all).extracting(Product::name).containsExactly("Klawiatura", "Mysz", "Monitor", "Kubek", "Lampka");
        Product mouse = all.get(1);
        assertThat(mouse.price()).isEqualByComparingTo("59.00");
        assertThat(mouse.discountPercent()).isEqualTo(10); // kolumna discount_percent → pole discountPercent
        assertThat(all.get(0).discountPercent()).isNull(); // NULL w bazie → null w Integer

        assertThat(shop().getById(3).name()).isEqualTo("Monitor");
        assertThat(shop().findById(3)).isPresent();
        assertThat(shop().findById(999)).isEmpty(); // optional(): brak wiersza to nie wyjątek
        assertThat(shop().namesInCategory("dom")).containsExactly("Kubek", "Lampka");
        assertThat(shop().count()).isEqualTo(5);
        assertThat(shop().labels().get(0)).isEqualTo(new ProductLabel("Klawiatura (149.90 zł)"));

        List<java.util.Map<String, Object>> rows = jdbc.sql("SELECT id, name FROM product WHERE id = 1")
                .query().listOfRows();
        assertThat(rows.get(0)).containsEntry("NAME", "Klawiatura"); // H2 zwraca nazwy kolumn WIELKIMI literami
    }

    // ---- 3 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 3: parametry pozycyjne, nazwane (w dowolnej kolejności) i paramSource z rekordu")
    void section3Parameters() {
        List<Product> positional = jdbc.sql("SELECT * FROM product WHERE category = ? AND price < ? ORDER BY id")
                .param("komputery")
                .param(new BigDecimal("100"))
                .query(Product.class).list();
        assertThat(positional).extracting(Product::name).containsExactly("Mysz");

        List<Product> viaParams = jdbc.sql("SELECT * FROM product WHERE category = ? AND price < ? ORDER BY id")
                .params("komputery", new BigDecimal("100"))
                .query(Product.class).list();
        assertThat(viaParams).isEqualTo(positional);

        // nazwane: w metodzie sklepu .param("maxPrice", …) stoi PRZED .param("category", …) — kolejność nie ma znaczenia
        assertThat(shop().cheaperThanInCategory("komputery", new BigDecimal("100")))
                .extracting(Product::name).containsExactly("Mysz");

        assertThatThrownBy(() -> jdbc.sql("SELECT * FROM product WHERE id = ? AND name = :name")
                .param(1L)
                .param("name", "Klawiatura")
                .query(Product.class).list())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Configure either named or indexed parameters, not both");

        int inserted = shop().insertWithoutKey(new Product(null, "Zeszyt", "dom", new BigDecimal("4.50"), 40, 15));
        assertThat(inserted).isEqualTo(1);
        assertThat(jdbc.sql("SELECT discount_percent FROM product WHERE name = :name")
                .param("name", "Zeszyt").query(Integer.class).single()).isEqualTo(15);
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4: update zwraca liczbę wierszy, klucz generowany jest odczytany, 0 wierszy to nie błąd")
    void section4Updates() {
        long id = shop().insert(new Product(null, "Zeszyt", "dom", new BigDecimal("4.50"), 40, null));
        assertThat(id).isGreaterThan(5);
        assertThat(shop().getById(id).name()).isEqualTo("Zeszyt");

        assertThat(shop().changeStock(id, 7)).isEqualTo(1);
        assertThat(shop().getById(id).stock()).isEqualTo(7);

        assertThat(shop().changeStock(999, 1)).as("brak takiego id: 0 wierszy, bez wyjątku").isZero();
        assertThat(shop().delete(999)).isZero();
        assertThat(shop().delete(id)).isEqualTo(1);
        assertThat(shop().findById(id)).isEmpty();
    }

    // ---- 5 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 5: single() — 0 wierszy to EmptyResultDataAccessException, 2 wiersze to IncorrectResultSize…")
    void section5SingleRowCount() {
        assertThatThrownBy(() -> shop().getById(999))
                .isInstanceOf(EmptyResultDataAccessException.class)
                .hasMessageContaining("expected 1, actual 0");
        assertThatThrownBy(() -> jdbc.sql("SELECT * FROM product WHERE category = 'dom'")
                .query(Product.class).single())
                .isExactlyInstanceOf(IncorrectResultSizeDataAccessException.class)
                .hasMessageContaining("expected 1, actual 2");
        assertThat(EmptyResultDataAccessException.class).isAssignableTo(IncorrectResultSizeDataAccessException.class);
    }

    @Test
    @DisplayName("Sekcja 5: SQL injection — sklejony SQL zwraca wszystko i pozwala skasować tabelę, parametr nie")
    void section5SqlInjection() {
        String attack = "x' OR '1'='1";
        assertThat(shop().findByNameUnsafe(attack)).as("sklejony SQL: warunek zawsze prawdziwy").hasSize(5);
        assertThat(shop().findByNameSafe(attack)).as("parametr: to tylko dziwna nazwa produktu").isEmpty();

        // drugi atak: dołożenie kolejnego polecenia (H2 wykonuje je w jednym wywołaniu); wycofa go rollback testu
        shop().findByNameUnsafe("x'; DELETE FROM product; --");
        assertThat(shop().count()).as("tabela skasowana przez wstrzyknięte polecenie").isZero();

        shop().insertWithoutKey(new Product(null, "Nowy", "dom", BigDecimal.ONE, 1, null));
        shop().findByNameSafe("x'; DELETE FROM product; --");
        assertThat(shop().count()).as("z parametrem nic nie zostało skasowane").isEqualTo(1);
    }

    @Test
    @DisplayName("Sekcja 5: NULL w kolumnie a pole int — ConversionFailedException; Integer i optional() radzą sobie")
    void section5NullAndPrimitive() {
        assertThatThrownBy(() -> shop().discountsAsPrimitive())
                .isInstanceOf(ConversionFailedException.class)
                .hasMessageContaining("[null]")
                .hasMessageContaining("[int]");
        assertThat(shop().findById(1).orElseThrow().discountPercent()).isNull();

        assertThat(jdbc.sql("SELECT discount_percent FROM product WHERE id = 1").query(Integer.class).optional()).isEmpty();
        assertThatThrownBy(() -> jdbc.sql("SELECT discount_percent FROM product WHERE id = 1")
                .query(Integer.class).single())
                .isInstanceOf(TypeMismatchDataAccessException.class)
                .hasMessageContaining("Result value is null");
    }
}
