package s07_data_jdbc.jdbc05_repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Jdbc05Repository. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * {@code @JdbcTest} cofa zmiany po każdym teście, a {@code @Sql} dostarcza dwa produkty (Kawa, Herbata).
 */
@Tag("cwiczenie")
@JdbcTest
@ActiveProfiles("jdbc05")
class Jdbc05RepositoryExercisesTest {

    @Autowired
    JdbcClient jdbcClient;

    @Test
    @DisplayName("Ćwiczenie 1: czy produkt o nazwie istnieje")
    @Sql("/db/jdbc05/two-products.sql")
    void exercise1() {
        assertThat(Jdbc05Repository.exercise1(jdbcClient, "Kawa")).isTrue();
        assertThat(Jdbc05Repository.exercise1(jdbcClient, "Nie ma takiego")).isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 2: wstaw produkt i zwróć klucz nadany przez bazę")
    void exercise2() {
        long id = Jdbc05Repository.exercise2(jdbcClient, "Mleko", 349);
        assertThat(jdbcClient.sql("SELECT name FROM products WHERE id = :id").param("id", id)
                .query(String.class).single()).isEqualTo("Mleko");
    }

    @Test
    @DisplayName("Ćwiczenie 3: wstawienie zwraca true, a zajęta nazwa false (bez wyjątku)")
    @Sql("/db/jdbc05/two-products.sql")
    void exercise3() {
        assertThat(Jdbc05Repository.exercise3(jdbcClient, "Mleko", 349)).isTrue();
        assertThat(Jdbc05Repository.exercise3(jdbcClient, "Kawa", 1)).isFalse();
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM products").query(Integer.class).single()).isEqualTo(3);
    }
}
