package s07_data_jdbc.jdbc02_jdbc_template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jdbc02JdbcTemplate — „wynik” każdej sekcji.
 *
 * <p>{@code @SpringBootTest}: potrzebujemy prawdziwej puli, bazy H2 i beanów szablonów z autokonfiguracji.
 * {@code @Transactional} na klasie testowej wycofuje zmiany po każdym teście (testy nie psują sobie danych).</p>
 */
@SpringBootTest
@ActiveProfiles("jdbc02")
@Transactional
class Jdbc02JdbcTemplateTest {

    @Autowired
    private ShopTemplates shop;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    @DisplayName("1. query, queryForObject, queryForList i update")
    void section1() {
        assertThat(shop.productsOfCategory("DOM")).extracting(Product::sku).containsExactly("DOM-001", "DOM-002");
        assertThat(shop.countOutOfStock()).isEqualTo(2);   // Smartfon X i Oliwa z oliwek
        assertThat(shop.productNames("KSIAZKI")).containsExactly("Czysty kod", "Java. Podstawy", "Wzorce projektowe");
        assertThat(shop.rawRows()).containsExactly(
                Map.of("SKU", "DOM-001", "STOCK", 2), Map.of("SKU", "DOM-002", "STOCK", 18));   // klucze: wielkie litery
        assertThat(shop.changeStock("DOM-001", 7)).isEqualTo(1);
        assertThat(shop.changeStock("NIE-MA", 7)).isZero();
    }

    @Test
    @DisplayName("2. DataClassRowMapper wypełnia rekord, BeanPropertyRowMapper klasę z setterami")
    void section2() {
        assertThat(shop.customersFromCity("Warszawa")).containsExactly(
                new Customer(1, "Jan Kowalski", "Warszawa", "jan@example.com", true),
                new Customer(4, "Zofia Krawczyk", "Warszawa", "zofia@example.com", true));

        List<ShopTemplates.LegacyCustomer> legacy = shop.legacyCustomers();
        assertThat(legacy).hasSize(7);
        assertThat(legacy.get(0).getName()).isEqualTo("Jan Kowalski");
        assertThat(legacy.get(1).getEmail()).isNull();   // Maria Nowak: brak e-maila
    }

    @Test
    @DisplayName("3. queryForObject: brak wiersza to wyjątek, NULL w kolumnie to null")
    void section3() {
        assertThatThrownBy(() -> shop.customerById(99))
                .isInstanceOf(EmptyResultDataAccessException.class)
                .hasMessage("Incorrect result size: expected 1, actual 0");
        assertThat(shop.customerByIdOrNull(99)).isNull();
        assertThat(shop.customerById(3).name()).isEqualTo("Adam Mazur");

        assertThat(shop.emailOf(2)).isNull();                          // Maria Nowak: wiersz jest, e-maila brak
        assertThat(shop.emailOf(3)).isEqualTo("adam.mazur@example.com");
    }

    @Test
    @DisplayName("4. Parametry nazwane z mapy i z pól rekordu")
    void section4() {
        assertThat(shop.productsBetween(new BigDecimal("40"), new BigDecimal("100")))
                .extracting(Product::sku)
                .containsExactly("SPO-003", "ODZ-002", "SPO-001", "KSI-001", "KSI-003");   // sortowanie po cenie
        assertThat(shop.insert(new Product(0, "DOM-003", "Odkurzacz", "DOM", new BigDecimal("599.00"), 3)))
                .isEqualTo(1);
        assertThat(shop.productsOfCategory("DOM")).hasSize(3);
    }

    @Test
    @DisplayName("5. batchUpdate zwraca po jednym wyniku na zestaw parametrów")
    void section5() {
        int[] results = shop.changeStocks(Map.of("DOM-001", 5, "DOM-002", 6, "NIE-MA", 1));
        assertThat(results).hasSize(3);
        assertThat(Arrays.stream(results).sum()).isEqualTo(2);   // dwa istniejące SKU zmienione, trzecie 0 (H2 podaje liczby)
        assertThat(shop.productsOfCategory("DOM")).extracting(Product::stock).containsExactly(5, 6);

        int[] inserted = shop.insertAll(List.of(
                new Product(0, "T-1", "Test 1", "DOM", BigDecimal.ONE, 1),
                new Product(0, "T-2", "Test 2", "DOM", BigDecimal.ONE, 1)));
        assertThat(inserted).containsExactly(1, 1);
    }

    @Test
    @DisplayName("6. Błędy SQL zamieniają się na podtypy DataAccessException")
    void section6() {
        // powtórzony numer zamówienia (UNIQUE)
        assertThatThrownBy(() -> shop.insertOrder("ZAM-001", 1))
                .isInstanceOf(DuplicateKeyException.class)
                .isInstanceOf(DataIntegrityViolationException.class)     // DuplicateKeyException jest jego podklasą
                .hasCauseInstanceOf(SQLException.class);                  // oryginalny wyjątek JDBC zostaje jako przyczyna

        // nieistniejący klient (klucz obcy): ogólny DataIntegrityViolationException, ale NIE DuplicateKeyException
        assertThatThrownBy(() -> shop.insertOrder("ZAM-099", 999))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(DuplicateKeyException.class);

        // NOT NULL
        assertThatThrownBy(shop::insertWithoutName).isInstanceOf(DataIntegrityViolationException.class);

        // literówka w nazwie tabeli
        assertThatThrownBy(shop::brokenSql)
                .isInstanceOf(BadSqlGrammarException.class)
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("7. JdbcTemplate i JdbcClient dzielą DataSource i transakcję")
    void section7() {
        jdbcTemplate.update("UPDATE product SET stock = 77 WHERE sku = 'KSI-001'");   // zmiana szablonem...
        Integer seenByClient = jdbcClient.sql("SELECT stock FROM product WHERE sku = 'KSI-001'")
                .query(Integer.class).single();                                         // ...widoczna dla klienta
        assertThat(seenByClient).isEqualTo(77);
    }
}
