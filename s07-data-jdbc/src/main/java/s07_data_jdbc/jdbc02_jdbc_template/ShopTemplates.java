package s07_data_jdbc.jdbc02_jdbc_template;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Zapytania lekcji Jdbc02JdbcTemplate — starsze API ({@code JdbcTemplate}, {@code NamedParameterJdbcTemplate}),
 * które nadal spotkasz w większości tutoriali i w starszych projektach. Oba beany tworzy autokonfiguracja Boota.
 */
@Component
public class ShopTemplates {

    /** RowMapper (mapowanie jednego wiersza) napisany ręcznie — działa zawsze, wszędzie, w każdej wersji Springa. */
    private static final RowMapper<Product> PRODUCT_MAPPER = (rs, rowNum) -> new Product(
            rs.getLong("id"), rs.getString("sku"), rs.getString("name"),
            rs.getString("category"), rs.getBigDecimal("price"), rs.getInt("stock"));

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedTemplate;

    public ShopTemplates(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedTemplate = namedTemplate;
    }

    // --- sekcja 1: query / queryForObject / queryForList / update ---------------------------------------

    /** {@code query(sql, rowMapper, parametry...)} — UWAGA na kolejność argumentów: najpierw mapper, potem parametry. */
    public List<Product> productsOfCategory(String category) {
        return jdbcTemplate.query("SELECT * FROM product WHERE category = ? ORDER BY name", PRODUCT_MAPPER, category);
    }

    /** {@code queryForObject(sql, typ, parametry...)} — jedna wartość. */
    public Integer countOutOfStock() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product WHERE stock = ?", Integer.class, 0);
    }

    /** {@code queryForList(sql, typ)} — kolumna jako lista wartości. */
    public List<String> productNames(String category) {
        return jdbcTemplate.queryForList("SELECT name FROM product WHERE category = ? ORDER BY name",
                String.class, category);
    }

    /** {@code queryForList(sql)} bez typu — lista map „nazwa kolumny → wartość” (wygodne do podglądu, kiepskie w kodzie). */
    public List<Map<String, Object>> rawRows() {
        return jdbcTemplate.queryForList("SELECT sku, stock FROM product WHERE category = 'DOM' ORDER BY sku");
    }

    public int changeStock(String sku, int newStock) {
        return jdbcTemplate.update("UPDATE product SET stock = ? WHERE sku = ?", newStock, sku);
    }

    // --- sekcja 2: gotowe RowMappery ---------------------------------------------------------------------

    /** {@code DataClassRowMapper} — mapuje wiersz na rekord (albo klasę z konstruktorem) po nazwach kolumn. */
    public List<Customer> customersFromCity(String city) {
        return jdbcTemplate.query("SELECT * FROM customer WHERE city = ? ORDER BY id",
                new DataClassRowMapper<>(Customer.class), city);
    }

    /** {@code BeanPropertyRowMapper} — starszy odpowiednik dla klas z setterami (JavaBean); rekordu NIE wypełni. */
    public List<LegacyCustomer> legacyCustomers() {
        return jdbcTemplate.query("SELECT * FROM customer ORDER BY id", new BeanPropertyRowMapper<>(LegacyCustomer.class));
    }

    /** Klasa „starego typu” (JavaBean): pusty konstruktor + gettery i settery. Dla {@code BeanPropertyRowMapper}. */
    public static class LegacyCustomer {
        private long id;
        private String name;
        private String email;

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    // --- sekcja 3: queryForObject i pułapka braku wyniku -----------------------------------------------

    /** Jeden klient po id. PUŁAPKA: gdy wiersza nie ma → {@code EmptyResultDataAccessException}. */
    public Customer customerById(long id) {
        return jdbcTemplate.queryForObject("SELECT * FROM customer WHERE id = ?",
                new DataClassRowMapper<>(Customer.class), id);
    }

    /** Bezpieczniej: {@code query(...)} zwraca listę (może być pusta) — pierwszy element albo {@code null}. */
    public Customer customerByIdOrNull(long id) {
        List<Customer> found = jdbcTemplate.query("SELECT * FROM customer WHERE id = ?",
                new DataClassRowMapper<>(Customer.class), id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Wartość kolumny, która może być NULL: {@code queryForObject} zwraca wtedy {@code null}, a NIE wyjątek. */
    public String emailOf(long customerId) {
        return jdbcTemplate.queryForObject("SELECT email FROM customer WHERE id = ?", String.class, customerId);
    }

    // --- sekcja 4: NamedParameterJdbcTemplate -----------------------------------------------------------

    /** Parametry nazwane z mapy ({@code MapSqlParameterSource}). */
    public List<Product> productsBetween(BigDecimal min, BigDecimal max) {
        SqlParameterSource params = new MapSqlParameterSource("min", min).addValue("max", max);
        return namedTemplate.query("SELECT * FROM product WHERE price BETWEEN :min AND :max ORDER BY price",
                params, PRODUCT_MAPPER);
    }

    /** Parametry nazwane z pól rekordu/obiektu ({@code BeanPropertySqlParameterSource}): {@code :sku} ← {@code sku()}. */
    public int insert(Product product) {
        return namedTemplate.update(
                "INSERT INTO product (sku, name, category, price, stock) VALUES (:sku, :name, :category, :price, :stock)",
                new BeanPropertySqlParameterSource(product));
    }

    // --- sekcja 5: batchUpdate -------------------------------------------------------------------------

    /** Wiele UPDATE-ów jedną „paczką” (batch): jeden zestaw parametrów na wiersz. Zwraca liczby zmienionych wierszy. */
    public int[] changeStocks(Map<String, Integer> newStocks) {
        List<Object[]> rows = newStocks.entrySet().stream()
                .map(entry -> new Object[]{entry.getValue(), entry.getKey()})
                .toList();
        return jdbcTemplate.batchUpdate("UPDATE product SET stock = ? WHERE sku = ?", rows);
    }

    /** Wersja z nazwanymi parametrami: tablica źródeł parametrów, po jednym na wiersz. */
    public int[] insertAll(List<Product> products) {
        SqlParameterSource[] batch = products.stream()
                .map(BeanPropertySqlParameterSource::new)
                .toArray(SqlParameterSource[]::new);
        return namedTemplate.batchUpdate(
                "INSERT INTO product (sku, name, category, price, stock) VALUES (:sku, :name, :category, :price, :stock)",
                batch);
    }

    // --- sekcja 6: wyjątki ---------------------------------------------------------------------------------

    /** Wstawia zamówienie dla klienta o podanym id. Kolumna {@code number} jest UNIQUE, {@code customer_id} to klucz obcy. */
    public void insertOrder(String number, long customerId) {
        jdbcTemplate.update("INSERT INTO shop_order (number, customer_id, status) VALUES (?, ?, 'NOWE')", number, customerId);
    }

    /** Niepoprawny SQL (literówka w nazwie tabeli). */
    public void brokenSql() {
        jdbcTemplate.queryForList("SELECT * FROM produkty");
    }

    /** Naruszenie NOT NULL: produkt bez nazwy. */
    public void insertWithoutName() {
        jdbcTemplate.update("INSERT INTO product (sku, name, category, price, stock) VALUES ('X-1', NULL, 'DOM', 1, 1)");
    }
}
