package s07_data_jdbc.solutions.jdbc01_jdbc_client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import s07_data_jdbc.jdbc01_jdbc_client.Jdbc01Application;
import s07_data_jdbc.jdbc01_jdbc_client.Product;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Jdbc01JdbcClientExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Pakiet solutions nie ma własnej klasy startowej, więc wskazujemy ją wprost: {@code classes = Jdbc01Application.class}.
 */
@Tag("wzorzec")
@SpringBootTest(classes = Jdbc01Application.class)
@ActiveProfiles("jdbc01")
@Transactional
class Jdbc01JdbcClientSolutionsTest {

    @Autowired
    JdbcClient jdbcClient;

    @Test
    @DisplayName("Wzorzec 1: nazwy produktów tańszych niż podana cena")
    void solution1() {
        assertThat(Jdbc01JdbcClientSolutions.solution1(jdbcClient, 1000)).containsExactly("Czekolada", "Mleko");
        assertThat(Jdbc01JdbcClientSolutions.solution1(jdbcClient, 100)).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 2: produkt po nazwie albo Optional.empty, odporne na SQL injection")
    void solution2() {
        assertThat(Jdbc01JdbcClientSolutions.solution2(jdbcClient, "Kawa")).get().extracting(Product::priceCents).isEqualTo(2999);
        assertThat(Jdbc01JdbcClientSolutions.solution2(jdbcClient, "Nie ma takiego")).isEmpty();
        assertThat(Jdbc01JdbcClientSolutions.solution2(jdbcClient, "x' OR '1'='1")).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 3: przecena o procent, zwraca liczbę zmienionych produktów")
    void solution3() {
        assertThat(Jdbc01JdbcClientSolutions.solution3(jdbcClient, 10)).isEqualTo(4);
        assertThat(jdbcClient.sql("SELECT price_cents FROM products ORDER BY id").query(Integer.class).list())
                .containsExactly(2699, 1439, 809, 314);
    }
}
