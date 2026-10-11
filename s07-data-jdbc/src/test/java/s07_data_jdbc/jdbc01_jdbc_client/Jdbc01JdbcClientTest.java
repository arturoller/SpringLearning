package s07_data_jdbc.jdbc01_jdbc_client;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jdbc01JdbcClient.
 *
 * <ul>
 *   <li>{@code @SpringBootTest} — startuje kontekst Jdbc01Application (potrzebujemy prawdziwego DataSource z autokonfiguracji).</li>
 *   <li>{@code @ActiveProfiles("jdbc01")} — włącza profil z application-jdbc01.properties (skrypty db/jdbc01).</li>
 *   <li>{@code @Transactional} (transakcyjny) — każdy test działa w transakcji, którą Spring na końcu COFA (rollback).
 *       Dzięki temu zmiany z jednego testu nie psują drugiego i kolejność testów nie ma znaczenia.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("jdbc01")
@Transactional
class Jdbc01JdbcClientTest {

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcClient jdbcClient;

    @Test
    @DisplayName("1. Boot sam tworzy DataSource (pula HikariCP, H2 w pamięci o unikalnej nazwie) i JdbcClient")
    void autoConfiguredDataSource() throws Exception {
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        HikariDataSource hikari = (HikariDataSource) dataSource;
        assertThat(hikari.getJdbcUrl()).startsWith("jdbc:h2:mem:");
        assertThat(jdbcClient).isNotNull();
        // dane startowe z data.sql są już w bazie
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM products").query(Integer.class).single()).isEqualTo(4);
    }

    @Test
    @DisplayName("2. Parametry pozycyjne (?) i nazwane (:nazwa) dają ten sam wynik")
    void positionalAndNamedParameters() {
        List<String> positional = jdbcClient
                .sql("SELECT name FROM products WHERE price_cents < ? AND stock > ? ORDER BY name")
                .param(1000).param(50)
                .query(String.class).list();
        List<String> named = jdbcClient
                .sql("SELECT name FROM products WHERE price_cents < :max AND stock > :minStock ORDER BY name")
                .param("minStock", 50).param("max", 1000)   // kolejność dowolna
                .query(String.class).list();
        assertThat(positional).containsExactly("Mleko");
        assertThat(named).isEqualTo(positional);
    }

    @Test
    @DisplayName("3. query: list, optional, single, mapowanie na rekord i na zwykły typ")
    void queryAndMapping() {
        List<Product> all = jdbcClient.sql("SELECT id, name, price_cents, stock FROM products ORDER BY name")
                .query(Product.class).list();
        assertThat(all).extracting(Product::name).containsExactly("Czekolada", "Herbata", "Kawa", "Mleko");
        // kolumna price_cents trafiła do komponentu priceCents
        assertThat(all.get(2)).extracting(Product::priceCents, Product::stock).containsExactly(2999, 10);

        Optional<Product> found = jdbcClient.sql("SELECT * FROM products WHERE name = :name")
                .param("name", "Kawa").query(Product.class).optional();
        Optional<Product> missing = jdbcClient.sql("SELECT * FROM products WHERE name = :name")
                .param("name", "Nie ma takiego").query(Product.class).optional();
        assertThat(found).isPresent();
        assertThat(missing).isEmpty();

        Product single = jdbcClient.sql("SELECT * FROM products WHERE name = ?")
                .param("Herbata").query(Product.class).single();
        assertThat(single.priceCents()).isEqualTo(1599);

        // własna lambda (RowMapper) zamiast klasy
        List<String> labels = jdbcClient.sql("SELECT name, price_cents FROM products ORDER BY id")
                .query((rs, rowNum) -> rs.getString("name") + "=" + rs.getInt("price_cents"))
                .list();
        assertThat(labels).containsExactly("Kawa=2999", "Herbata=1599", "Czekolada=899", "Mleko=349");
    }

    @Test
    @DisplayName("4. update() obsługuje INSERT/UPDATE/DELETE i zwraca liczbę zmienionych wierszy (0 to nie błąd)")
    void update() {
        int inserted = jdbcClient.sql("INSERT INTO products (name, price_cents, stock) VALUES (:name, :price, :stock)")
                .param("name", "Cukier").param("price", 450).param("stock", 60).update();
        int updated = jdbcClient.sql("UPDATE products SET stock = stock - :q WHERE name = :name")
                .param("q", 5).param("name", "Cukier").update();
        int updatedNothing = jdbcClient.sql("UPDATE products SET stock = 0 WHERE name = :name")
                .param("name", "Nie ma takiego").update();
        int deleted = jdbcClient.sql("DELETE FROM products WHERE name = :name").param("name", "Cukier").update();

        assertThat(inserted).isEqualTo(1);
        assertThat(updated).isEqualTo(1);
        assertThat(updatedNothing).isZero();     // brak dopasowania: zero wierszy, bez wyjątku
        assertThat(deleted).isEqualTo(1);
    }

    @Test
    @DisplayName("5. SQL injection: sklejony SQL zwraca wszystko, ten sam napis jako parametr nie zwraca nic")
    void sqlInjection() {
        String input = "x' OR '1'='1";

        // ZŁE — dane użytkownika stają się kawałkiem polecenia SQL
        List<String> glued = jdbcClient.sql("SELECT name FROM products WHERE name = '" + input + "'")
                .query(String.class).list();
        // DOBRE — parametr jedzie do bazy osobno, jako zwykła wartość
        List<String> parametrized = jdbcClient.sql("SELECT name FROM products WHERE name = :name")
                .param("name", input).query(String.class).list();

        assertThat(glued).hasSize(4);
        assertThat(parametrized).isEmpty();
    }

    @Test
    @DisplayName("6. single() rzuca wyjątek przy 0 wierszy (EmptyResult...) i przy 2+ wierszach (IncorrectResultSize...)")
    void singleExceptions() {
        assertThatThrownBy(() -> jdbcClient.sql("SELECT * FROM products WHERE id = :id")
                .param("id", 999).query(Product.class).single())
                .isInstanceOf(EmptyResultDataAccessException.class);

        assertThatThrownBy(() -> jdbcClient.sql("SELECT * FROM products").query(Product.class).single())
                .isInstanceOf(IncorrectResultSizeDataAccessException.class)
                .isNotInstanceOf(EmptyResultDataAccessException.class);
    }
}
