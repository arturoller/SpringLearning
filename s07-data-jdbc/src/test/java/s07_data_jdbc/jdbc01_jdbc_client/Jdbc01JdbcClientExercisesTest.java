package s07_data_jdbc.jdbc01_jdbc_client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Jdbc01JdbcClient. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Każdy test działa w transakcji cofanej na końcu, więc ćwiczenie 3 nie zmienia danych dla pozostałych.
 */
@Tag("cwiczenie")
@SpringBootTest
@ActiveProfiles("jdbc01")
@Transactional
class Jdbc01JdbcClientExercisesTest {

    @Autowired
    JdbcClient jdbcClient;

    @Test
    @DisplayName("Ćwiczenie 1: nazwy produktów tańszych niż podana cena")
    void exercise1() {
        assertThat(Jdbc01JdbcClient.exercise1(jdbcClient, 1000)).containsExactly("Czekolada", "Mleko");
        assertThat(Jdbc01JdbcClient.exercise1(jdbcClient, 100)).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 2: produkt po nazwie albo Optional.empty, odporne na SQL injection")
    void exercise2() {
        assertThat(Jdbc01JdbcClient.exercise2(jdbcClient, "Kawa")).get().extracting(Product::priceCents).isEqualTo(2999);
        assertThat(Jdbc01JdbcClient.exercise2(jdbcClient, "Nie ma takiego")).isEmpty();
        assertThat(Jdbc01JdbcClient.exercise2(jdbcClient, "x' OR '1'='1")).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 3: przecena o procent, zwraca liczbę zmienionych produktów")
    void exercise3() {
        assertThat(Jdbc01JdbcClient.exercise3(jdbcClient, 10)).isEqualTo(4);
        assertThat(jdbcClient.sql("SELECT price_cents FROM products ORDER BY id").query(Integer.class).list())
                .containsExactly(2699, 1439, 809, 314);
    }
}
