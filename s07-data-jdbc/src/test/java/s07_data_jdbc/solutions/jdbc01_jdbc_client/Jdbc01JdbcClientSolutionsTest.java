package s07_data_jdbc.solutions.jdbc01_jdbc_client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import s07_data_jdbc.jdbc01_jdbc_client.Product;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Jdbc01JdbcClientExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
@JdbcTest
@ActiveProfiles("jdbc01")
class Jdbc01JdbcClientSolutionsTest {

    @Autowired
    private JdbcClient jdbc;

    @Test
    @DisplayName("Wzorzec 1: liczba produktów w kategorii")
    void solution1() {
        assertThat(Jdbc01JdbcClientSolutions.solution1CountInCategory(jdbc, "komputery")).isEqualTo(3);
        assertThat(Jdbc01JdbcClientSolutions.solution1CountInCategory(jdbc, "dom")).isEqualTo(2);
        assertThat(Jdbc01JdbcClientSolutions.solution1CountInCategory(jdbc, "ogród")).isZero();
    }

    @Test
    @DisplayName("Wzorzec 2: szukanie po nazwie bez wyjątku dla braku")
    void solution2() {
        assertThat(Jdbc01JdbcClientSolutions.solution2FindByName(jdbc, "Mysz")).isPresent()
                .get().extracting(Product::stock).isEqualTo(30);
        assertThat(Jdbc01JdbcClientSolutions.solution2FindByName(jdbc, "Nie ma takiego")).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 3: zwiększenie stanu magazynu zwraca liczbę wierszy")
    void solution3() {
        assertThat(Jdbc01JdbcClientSolutions.solution3IncreaseStock(jdbc, 5, 10)).isEqualTo(1);
        assertThat(jdbc.sql("SELECT stock FROM product WHERE id = 5").query(Integer.class).single()).isEqualTo(10);
        assertThat(Jdbc01JdbcClientSolutions.solution3IncreaseStock(jdbc, 999, 10)).isZero();
    }

    @Test
    @DisplayName("Wzorzec 4: wstawienie produktu i odczyt wygenerowanego klucza")
    void solution4() {
        long id = Jdbc01JdbcClientSolutions.solution4InsertAndReturnId(jdbc,
                new Product(null, "Zeszyt", "dom", new BigDecimal("4.50"), 40, null));
        assertThat(id).isGreaterThan(5);
        assertThat(jdbc.sql("SELECT name FROM product WHERE id = :id").param("id", id).query(String.class).single())
                .isEqualTo("Zeszyt");
    }

    @Test
    @DisplayName("Wzorzec 5: nazwy produktów z przedziału cen, posortowane")
    void solution5() {
        assertThat(Jdbc01JdbcClientSolutions.solution5NamesInPriceRange(jdbc, new BigDecimal("24.50"), new BigDecimal("149.90")))
                .containsExactly("Kubek", "Mysz", "Lampka", "Klawiatura");
        assertThat(Jdbc01JdbcClientSolutions.solution5NamesInPriceRange(jdbc, new BigDecimal("1000"), new BigDecimal("2000")))
                .isEmpty();
    }
}
