package s07_data_jdbc.jdbc01_jdbc_client;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Zapytania sklepu napisane na {@code JdbcClient} — kod omawiany w lekcji Jdbc01JdbcClient.
 * Zwykła klasa (nie bean): test podaje jej {@code JdbcClient} przez konstruktor.
 */
public class Jdbc01ShopQueries {

    /** Lekki rekord tylko na etykietę produktu — do własnego RowMapper (sekcja 2). */
    public record ProductLabel(String text) {
    }

    private final JdbcClient jdbc;

    public Jdbc01ShopQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Wszystkie produkty. ORDER BY jest konieczny: bez niego baza nie obiecuje żadnej kolejności. */
    public List<Product> findAll() {
        return jdbc.sql("SELECT * FROM product ORDER BY id")
                .query(Product.class) // mapowanie wiersza na rekord: kolumna discount_percent → pole discountPercent
                .list();
    }

    /** Parametr pozycyjny: pierwszy {@code ?} dostaje pierwszy {@code .param(...)}. */
    public List<String> namesInCategory(String category) {
        return jdbc.sql("SELECT name FROM product WHERE category = ? ORDER BY name")
                .param(category)
                .query(String.class) // zapytanie o jedną kolumnę → lista zwykłych wartości
                .list();
    }

    /** Parametry nazwane: {@code :category} i {@code :maxPrice} — kolejność .param(...) nie ma znaczenia. */
    public List<Product> cheaperThanInCategory(String category, BigDecimal maxPrice) {
        return jdbc.sql("SELECT * FROM product WHERE category = :category AND price < :maxPrice ORDER BY id")
                .param("maxPrice", maxPrice)
                .param("category", category)
                .query(Product.class)
                .list();
    }

    /** {@code .single()}: dokładnie jeden wiersz albo wyjątek. */
    public Product getById(long id) {
        return jdbc.sql("SELECT * FROM product WHERE id = :id")
                .param("id", id)
                .query(Product.class)
                .single();
    }

    /** {@code .optional()}: zero albo jeden wiersz; brak wiersza to zwykły {@code Optional.empty()}, bez wyjątku. */
    public Optional<Product> findById(long id) {
        return jdbc.sql("SELECT * FROM product WHERE id = :id")
                .param("id", id)
                .query(Product.class)
                .optional();
    }

    /** Własny RowMapper (lambda): sam decydujesz, jak z wiersza zrobić obiekt. */
    public List<ProductLabel> labels() {
        return jdbc.sql("SELECT name, price FROM product ORDER BY id")
                .query((resultSet, rowNumber) ->
                        new ProductLabel(resultSet.getString("name") + " (" + resultSet.getBigDecimal("price") + " zł)"))
                .list();
    }

    /** Skalar: {@code COUNT(*)} zwraca jedną liczbę, więc pytamy o {@code Long}. */
    public long count() {
        return jdbc.sql("SELECT COUNT(*) FROM product")
                .query(Long.class)
                .single();
    }

    /** {@code .paramSource(rekord)}: nazwy {@code :name}, {@code :price}… są brane z akcesorów rekordu. */
    public int insertWithoutKey(Product product) {
        return jdbc.sql("""
                        INSERT INTO product (name, category, price, stock, discount_percent)
                        VALUES (:name, :category, :price, :stock, :discountPercent)""")
                .paramSource(product)
                .update(); // update() zwraca liczbę zmienionych wierszy
    }

    /** INSERT i odczyt klucza wygenerowanego przez bazę (AUTO_INCREMENT). */
    public long insert(Product product) {
        KeyHolder keyHolder = new GeneratedKeyHolder(); // keyHolder = „kieszeń” na wygenerowany klucz
        jdbc.sql("""
                        INSERT INTO product (name, category, price, stock, discount_percent)
                        VALUES (:name, :category, :price, :stock, :discountPercent)""")
                .paramSource(product)
                .update(keyHolder, "id"); // "id" = nazwa kolumny z kluczem, który chcemy odzyskać
        return keyHolder.getKey().longValue();
    }

    /** UPDATE z parametrami nazwanymi; wynik to liczba zmienionych wierszy (0, gdy nie ma takiego id). */
    public int changeStock(long id, int newStock) {
        return jdbc.sql("UPDATE product SET stock = :stock WHERE id = :id")
                .param("stock", newStock)
                .param("id", id)
                .update();
    }

    /** DELETE — też zwraca liczbę usuniętych wierszy. */
    public int delete(long id) {
        return jdbc.sql("DELETE FROM product WHERE id = ?")
                .param(id)
                .update();
    }

    /** ŹLE (PUŁAPKA — SQL injection): nazwa sklejona ze Stringiem staje się CZĘŚCIĄ polecenia SQL. */
    public List<Product> findByNameUnsafe(String name) {
        return jdbc.sql("SELECT * FROM product WHERE name = '" + name + "' ORDER BY id")
                .query(Product.class)
                .list();
    }

    /** DOBRZE: wartość idzie jako parametr — baza traktuje ją wyłącznie jako dane, nigdy jako polecenie. */
    public List<Product> findByNameSafe(String name) {
        return jdbc.sql("SELECT * FROM product WHERE name = :name ORDER BY id")
                .param("name", name)
                .query(Product.class)
                .list();
    }

    /** Rekord z prymitywem {@code int} w polu, które w bazie bywa NULL — źródło błędu w sekcji 5. */
    public record ProductWithPrimitiveDiscount(Long id, String name, int discountPercent) {
    }

    /** PUŁAPKA: NULL w kolumnie i pole {@code int}. Zobacz test sekcji 5. */
    public List<ProductWithPrimitiveDiscount> discountsAsPrimitive() {
        return jdbc.sql("SELECT id, name, discount_percent FROM product ORDER BY id")
                .query(ProductWithPrimitiveDiscount.class)
                .list();
    }
}
