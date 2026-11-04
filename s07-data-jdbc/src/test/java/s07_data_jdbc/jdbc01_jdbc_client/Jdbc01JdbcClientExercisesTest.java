package s07_data_jdbc.jdbc01_jdbc_client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Jdbc01JdbcClient. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@JdbcTest
@ActiveProfiles("jdbc01")
class Jdbc01JdbcClientExercisesTest {

    @Autowired
    private JdbcClient jdbc;

    @Test
    @DisplayName("Ćwiczenie 1: liczba produktów w kategorii")
    void exercise1() {
        assertThat(Jdbc01JdbcClient.exercise1CountInCategory(jdbc, "komputery")).isEqualTo(3);
        assertThat(Jdbc01JdbcClient.exercise1CountInCategory(jdbc, "dom")).isEqualTo(2);
        assertThat(Jdbc01JdbcClient.exercise1CountInCategory(jdbc, "ogród")).isZero();
    }

    @Test
    @DisplayName("Ćwiczenie 2: szukanie po nazwie bez wyjątku dla braku")
    void exercise2() {
        assertThat(Jdbc01JdbcClient.exercise2FindByName(jdbc, "Mysz")).isPresent()
                .get().extracting(Product::stock).isEqualTo(30);
        assertThat(Jdbc01JdbcClient.exercise2FindByName(jdbc, "Nie ma takiego")).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 3: zwiększenie stanu magazynu zwraca liczbę wierszy")
    void exercise3() {
        assertThat(Jdbc01JdbcClient.exercise3IncreaseStock(jdbc, 5, 10)).isEqualTo(1);
        assertThat(jdbc.sql("SELECT stock FROM product WHERE id = 5").query(Integer.class).single()).isEqualTo(10);
        assertThat(Jdbc01JdbcClient.exercise3IncreaseStock(jdbc, 999, 10)).isZero();
    }

    @Test
    @DisplayName("Ćwiczenie 4: wstawienie produktu i odczyt wygenerowanego klucza")
    void exercise4() {
        long id = Jdbc01JdbcClient.exercise4InsertAndReturnId(jdbc,
                new Product(null, "Zeszyt", "dom", new BigDecimal("4.50"), 40, null));
        assertThat(id).isGreaterThan(5);
        assertThat(jdbc.sql("SELECT name FROM product WHERE id = :id").param("id", id).query(String.class).single())
                .isEqualTo("Zeszyt");
    }

    @Test
    @DisplayName("Ćwiczenie 5: nazwy produktów z przedziału cen, posortowane")
    void exercise5() {
        assertThat(Jdbc01JdbcClient.exercise5NamesInPriceRange(jdbc, new BigDecimal("24.50"), new BigDecimal("149.90")))
                .containsExactly("Kubek", "Mysz", "Lampka", "Klawiatura");
        assertThat(Jdbc01JdbcClient.exercise5NamesInPriceRange(jdbc, new BigDecimal("1000"), new BigDecimal("2000")))
                .isEmpty();
    }
}
