package s07_data_jdbc.jdbc01_jdbc_client;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Zapytania lekcji Jdbc01JdbcClient — po jednej metodzie na każdą sekcję. {@code JdbcClient} (klient JDBC) jest
 * beanem (ziarnem) utworzonym przez autokonfigurację Boota na podstawie {@code DataSource} (źródła danych = puli
 * połączeń); wstrzykujemy go konstruktorem jak każdą zależność.
 */
@Component
public class ProductQueries {

    private final JdbcClient jdbc;

    public ProductQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // --- sekcja 1: zapytanie → lista rekordów ---------------------------------------------------------

    public List<Product> findAll() {
        return jdbc.sql("SELECT * FROM product ORDER BY id")
                .query(Product.class)   // mapowanie wiersza na rekord Product (po nazwach kolumn)
                .list();                // wszystkie wiersze jako List
    }

    // --- sekcja 2: parametry pozycyjne (?) i nazwane (:nazwa) ------------------------------------------

    /** Parametr pozycyjny: znak {@code ?} w SQL i kolejne {@code .param(wartość)}. */
    public List<Product> findByCategory(String category) {
        return jdbc.sql("SELECT * FROM product WHERE category = ? ORDER BY name")
                .param(category)
                .query(Product.class)
                .list();
    }

    /** Parametry nazwane: {@code :nazwa} w SQL i {@code .param("nazwa", wartość)} — kolejność nie ma znaczenia. */
    public List<Product> findByCategoryWithStock(String category, int minStock) {
        return jdbc.sql("SELECT * FROM product WHERE category = :category AND stock >= :minStock ORDER BY name")
                .param("minStock", minStock)
                .param("category", category)
                .query(Product.class)
                .list();
    }

    /** Błąd początkującego: w jednym zapytaniu i {@code ?}, i {@code :nazwa}. Zobacz test sekcji 2. */
    public List<Product> mixedParameters(String category, int minStock) {
        return jdbc.sql("SELECT * FROM product WHERE category = ? AND stock >= :minStock")
                .param(category)
                .param("minStock", minStock)
                .query(Product.class)
                .list();
    }

    // --- sekcja 3: single() kontra optional() -----------------------------------------------------------

    /** {@code single()}: dokładnie jeden wiersz albo wyjątek (zero lub kilka wierszy to błąd). */
    public Product getBySku(String sku) {
        return jdbc.sql("SELECT * FROM product WHERE sku = :sku")
                .param("sku", sku)
                .query(Product.class)
                .single();
    }

    /** {@code optional()}: zero wierszy → {@code Optional.empty()}, jeden → wartość, kilka → wyjątek. */
    public Optional<Product> findBySku(String sku) {
        return jdbc.sql("SELECT * FROM product WHERE sku = :sku")
                .param("sku", sku)
                .query(Product.class)
                .optional();
    }

    /** Zapytanie, które może zwrócić KILKA wierszy, zakończone {@code single()} — błąd. */
    public Product anyFromCategorySingle(String category) {
        return jdbc.sql("SELECT * FROM product WHERE category = :category")
                .param("category", category)
                .query(Product.class)
                .single();
    }

    // --- sekcja 4: wartości proste i własny RowMapper ---------------------------------------------------

    /** Pojedyncza wartość: {@code query(Integer.class)} (agregat COUNT, SUM itd.). */
    public int countAll() {
        return jdbc.sql("SELECT COUNT(*) FROM product")
                .query(Integer.class)
                .single();
    }

    /** Kolumna jako lista napisów. */
    public List<String> namesOfCategory(String category) {
        return jdbc.sql("SELECT name FROM product WHERE category = :category ORDER BY name")
                .param("category", category)
                .query(String.class)
                .list();
    }

    /** Własny {@code RowMapper} (mapowanie jednego wiersza ręcznie) — lambda {@code (ResultSet rs, int numerWiersza)}. */
    public List<String> labels(String category) {
        return jdbc.sql("SELECT sku, name, price FROM product WHERE category = :category ORDER BY sku")
                .param("category", category)
                .query((rs, rowNum) -> rs.getString("sku") + " — " + rs.getString("name")
                        + " (" + rs.getBigDecimal("price") + " zł)")
                .list();
    }

    // --- sekcja 5: INSERT / UPDATE / DELETE i klucz generowany -------------------------------------------

    /** {@code update()} zwraca liczbę zmienionych wierszy. */
    public int changeStock(String sku, int newStock) {
        return jdbc.sql("UPDATE product SET stock = :stock WHERE sku = :sku")
                .param("stock", newStock)
                .param("sku", sku)
                .update();
    }

    /** INSERT i odczyt klucza wygenerowanego przez bazę (kolumna {@code id}) przez {@code KeyHolder}. */
    public long insert(String sku, String name, String category, BigDecimal price, int stock) {
        KeyHolder keyHolder = new GeneratedKeyHolder();   // „kieszeń” na klucz wygenerowany przez bazę
        jdbc.sql("INSERT INTO product (sku, name, category, price, stock) VALUES (:sku, :name, :category, :price, :stock)")
                .param("sku", sku)
                .param("name", name)
                .param("category", category)
                .param("price", price)
                .param("stock", stock)
                .update(keyHolder, "id");                  // "id" = która kolumna ma być odczytana
        return keyHolder.getKey().longValue();
    }

    /** Parametry nazwane wzięte z pól rekordu: {@code :sku} ← {@code product.sku()}. */
    public int insertRecord(Product product) {
        return jdbc.sql("INSERT INTO product (sku, name, category, price, stock) VALUES (:sku, :name, :category, :price, :stock)")
                .paramSource(product)
                .update();
    }

    public int delete(String sku) {
        return jdbc.sql("DELETE FROM product WHERE sku = :sku")
                .param("sku", sku)
                .update();
    }

    // --- sekcja 6: wstrzykiwanie SQL ----------------------------------------------------------------------

    /** ŹLE — NIE NAŚLADUJ: SQL sklejany z tekstu użytkownika (podatny na wstrzykiwanie SQL, SQL injection). */
    public List<Product> findByNameUnsafe(String name) {
        return jdbc.sql("SELECT * FROM product WHERE name = '" + name + "' ORDER BY id")
                .query(Product.class)
                .list();
    }

    /** DOBRZE: wartość jest PARAMETREM, nie częścią tekstu SQL. */
    public List<Product> findByName(String name) {
        return jdbc.sql("SELECT * FROM product WHERE name = :name ORDER BY id")
                .param("name", name)
                .query(Product.class)
                .list();
    }
}
