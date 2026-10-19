package s07_data_jdbc.jdbc01_jdbc_client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jdbc01JdbcClient — to jest „wynik” każdej sekcji.
 *
 * <p>{@code @SpringBootTest} (pełny kontekst), bo potrzebujemy prawdziwej puli połączeń, bazy H2 i beana
 * JdbcClient z autokonfiguracji; {@code @ActiveProfiles("jdbc01")} włącza ustawienia lekcji.
 * {@code @Transactional} na klasie testowej: każdy test działa w transakcji, którą Spring wycofuje po teście
 * (rollback = wycofanie zmian), więc INSERT-y i UPDATE-y jednego testu nie psują drugiego.</p>
 */
@SpringBootTest
@ActiveProfiles("jdbc01")
@Transactional
class Jdbc01JdbcClientTest {

    @Autowired
    private ProductQueries queries;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("1. Zapytanie mapuje wiersze na rekordy po nazwach kolumn; brakująca kolumna to błąd")
    void section1() {
        assertThat(queries.findAll()).hasSize(14);
        assertThat(queries.findAll().get(0))
                .isEqualTo(new Product(1, "ELE-001", "Laptop Pro 14", "ELEKTRONIKA", new BigDecimal("5499.99"), 7));

        // PUŁAPKA: rekord ma pola, których nie ma w wyniku zapytania
        assertThatThrownBy(() -> jdbc.sql("SELECT id, sku FROM product").query(Product.class).list())
                .isInstanceOf(BadSqlGrammarException.class);
    }

    @Test
    @DisplayName("2. Parametry pozycyjne i nazwane dają ten sam wynik; mieszanie stylów to błąd")
    void section2() {
        assertThat(queries.findByCategory("KSIAZKI")).extracting(Product::sku)
                .containsExactly("KSI-001", "KSI-002", "KSI-003");   // sortowanie po nazwie: Czysty, Java, Wzorce
        assertThat(queries.findByCategoryWithStock("KSIAZKI", 10)).extracting(Product::sku)
                .containsExactly("KSI-001");   // stany: 15, 9, 3 — tylko Czysty kod ma co najmniej 10

        assertThatThrownBy(() -> queries.mixedParameters("KSIAZKI", 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Configure either named or indexed parameters, not both");
    }

    @Test
    @DisplayName("3. single() wymaga dokładnie jednego wiersza, optional() toleruje brak")
    void section3() {
        assertThat(queries.getBySku("KSI-001").name()).isEqualTo("Czysty kod");
        assertThat(queries.findBySku("KSI-001")).isPresent();
        assertThat(queries.findBySku("XXX-999")).isEmpty();

        assertThatThrownBy(() -> queries.getBySku("XXX-999"))
                .isInstanceOf(EmptyResultDataAccessException.class)
                .hasMessage("Incorrect result size: expected 1, actual 0");
        assertThatThrownBy(() -> queries.anyFromCategorySingle("KSIAZKI"))
                .isInstanceOf(IncorrectResultSizeDataAccessException.class)
                .hasMessage("Incorrect result size: expected 1, actual 3");
    }

    @Test
    @DisplayName("4. Wartości proste i własny RowMapper")
    void section4() {
        assertThat(queries.countAll()).isEqualTo(14);
        assertThat(queries.namesOfCategory("KSIAZKI"))
                .containsExactly("Czysty kod", "Java. Podstawy", "Wzorce projektowe");
        assertThat(queries.labels("DOM")).containsExactly(
                "DOM-001 — Ekspres do kawy (1899.00 zł)", "DOM-002 — Lampka biurkowa (129.00 zł)");
    }

    @Test
    @DisplayName("5. update() zwraca liczbę wierszy (0 nie jest błędem), INSERT oddaje klucz z bazy")
    void section5() {
        assertThat(queries.changeStock("KSI-001", 99)).isEqualTo(1);
        assertThat(queries.getBySku("KSI-001").stock()).isEqualTo(99);
        assertThat(queries.changeStock("LITEROWKA", 5)).isZero();   // brak wyjątku, tylko 0

        long id = queries.insert("DOM-003", "Odkurzacz", "DOM", new BigDecimal("599.00"), 3);
        assertThat(id).isEqualTo(15L);   // kolejny numer IDENTITY po 14 produktach z data.sql
        assertThat(queries.getBySku("DOM-003").id()).isEqualTo(id);

        assertThat(queries.insertRecord(new Product(0, "DOM-004", "Czajnik", "DOM", new BigDecimal("89.00"), 10)))
                .isEqualTo(1);
        assertThat(queries.delete("DOM-004")).isEqualTo(1);
        assertThat(queries.delete("DOM-004")).isZero();
    }

    @Test
    @DisplayName("6. Parametr chroni przed wstrzyknięciem SQL, sklejanie tekstu nie")
    void section6() {
        String attack = "x' OR '1'='1";

        assertThat(queries.findByNameUnsafe(attack)).hasSize(14);   // sens zapytania zmieniony: wszystkie produkty
        assertThat(queries.findByName(attack)).isEmpty();           // wartość potraktowana jak zwykły tekst

        assertThatThrownBy(() -> queries.findByNameUnsafe("O'Reilly"))   // sam apostrof psuje zapytanie
                .isInstanceOf(BadSqlGrammarException.class);
        assertThat(queries.findByName("O'Reilly")).isEmpty();            // parametr: bez błędu
    }

    @Test
    @DisplayName("7. JdbcClient daje ten sam wynik co czysty JDBC, za to w kilku liniach")
    void section7() {
        PlainJdbcProducts plain = new PlainJdbcProducts(dataSource);
        assertThat(queries.findByCategory("SPOZYWCZE")).isEqualTo(plain.findByCategory("SPOZYWCZE"));
    }
}
