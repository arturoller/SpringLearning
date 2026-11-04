package s07_data_jdbc.jdbc02_jdbc_template;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Operacje na produktach napisane na {@code JdbcTemplate} i {@code NamedParameterJdbcTemplate} — kod lekcji
 * Jdbc02JdbcTemplate. Zwykła klasa (nie bean): test podaje jej oba szablony przez konstruktor.
 */
public class Jdbc02ShopDao {

    /** RowMapper dla rekordów: DataClassRowMapper tworzy rekord konstruktorem, kolumny dobiera po nazwach. */
    private static final RowMapper<Product> PRODUCT_MAPPER = new DataClassRowMapper<>(Product.class);

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedTemplate;

    public Jdbc02ShopDao(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedTemplate = namedTemplate;
    }

    /** query(sql, rowMapper, parametry...) — lista wyników. */
    public List<Product> findByCategory(String category) {
        return jdbcTemplate.query("SELECT * FROM product WHERE category = ? ORDER BY id", PRODUCT_MAPPER, category);
    }

    /** queryForObject: dokładnie jeden wynik, inaczej wyjątek (także dla ZERA wierszy!). */
    public Product getById(long id) {
        return jdbcTemplate.queryForObject("SELECT * FROM product WHERE id = ?", PRODUCT_MAPPER, id);
    }

    /** Bezpieczny odpowiednik: query(...) zwraca listę (pustą, gdy brak), a stream().findFirst() daje Optional. */
    public Optional<Product> findById(long id) {
        return jdbcTemplate.query("SELECT * FROM product WHERE id = ?", PRODUCT_MAPPER, id).stream().findFirst();
    }

    /** Skalar: queryForObject(sql, Typ.class, parametry...). */
    public long countByCategory(String category) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product WHERE category = ?", Long.class, category);
        return count == null ? 0 : count;
    }

    /** Lista wartości z jednej kolumny. */
    public List<String> names() {
        return jdbcTemplate.queryForList("SELECT name FROM product ORDER BY id", String.class);
    }

    /** update(sql, parametry...) — liczba zmienionych wierszy. */
    public int changeStock(long id, int stock) {
        return jdbcTemplate.update("UPDATE product SET stock = ? WHERE id = ?", stock, id);
    }

    /** NamedParameterJdbcTemplate + MapSqlParameterSource: parametry po nazwach, bez pilnowania kolejności. */
    public List<Product> search(String category, BigDecimal minPrice, BigDecimal maxPrice) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("category", category)
                .addValue("minPrice", minPrice)
                .addValue("maxPrice", maxPrice);
        return namedTemplate.query("""
                SELECT * FROM product
                WHERE category = :category AND price BETWEEN :minPrice AND :maxPrice
                ORDER BY price""", params, PRODUCT_MAPPER);
    }

    /** Lista w klauzuli IN (:ids): szablon nazwany sam rozwija ją do (?, ?, ?). Zwykły JdbcTemplate tego nie umie. */
    public List<Product> findByIds(List<Long> ids) {
        return namedTemplate.query("SELECT * FROM product WHERE id IN (:ids) ORDER BY id",
                new MapSqlParameterSource("ids", ids), PRODUCT_MAPPER);
    }

    /** BeanPropertySqlParameterSource: parametry :name, :price… są brane z obiektu (tu: z akcesorów rekordu). */
    public int insert(Product product) {
        return namedTemplate.update("""
                INSERT INTO product (name, category, price, stock)
                VALUES (:name, :category, :price, :stock)""", new BeanPropertySqlParameterSource(product));
    }

    /** batchUpdate: jedno polecenie SQL i wiele zestawów parametrów — zwraca tablicę liczników (po jednym na zestaw). */
    public int[] insertAll(List<Product> products) {
        List<Object[]> argumentSets = products.stream()
                .map(p -> new Object[]{p.name(), p.category(), p.price(), p.stock()})
                .toList();
        return jdbcTemplate.batchUpdate(
                "INSERT INTO product (name, category, price, stock) VALUES (?, ?, ?, ?)", argumentSets);
    }

    /** To samo z parametrami nazwanymi: SqlParameterSourceUtils.createBatch zamienia obiekty na zestawy parametrów. */
    public int[] insertAllNamed(List<Product> products) {
        return namedTemplate.batchUpdate("""
                INSERT INTO product (name, category, price, stock)
                VALUES (:name, :category, :price, :stock)""", SqlParameterSourceUtils.createBatch(products));
    }
}
