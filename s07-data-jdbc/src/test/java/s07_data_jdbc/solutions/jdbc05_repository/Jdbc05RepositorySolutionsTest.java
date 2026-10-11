package s07_data_jdbc.solutions.jdbc05_repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import s07_data_jdbc.jdbc05_repository.Jdbc05Application;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Jdbc05RepositoryExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Pakiet solutions nie ma własnej klasy startowej, więc wskazujemy ją wprost przez {@code @ContextConfiguration}.
 */
@Tag("wzorzec")
@JdbcTest
@ContextConfiguration(classes = Jdbc05Application.class)
@ActiveProfiles("jdbc05")
class Jdbc05RepositorySolutionsTest {

    @Autowired
    JdbcClient jdbcClient;

    @Test
    @DisplayName("Wzorzec 1: czy produkt o nazwie istnieje")
    @Sql("/db/jdbc05/two-products.sql")
    void solution1() {
        assertThat(Jdbc05RepositorySolutions.solution1(jdbcClient, "Kawa")).isTrue();
        assertThat(Jdbc05RepositorySolutions.solution1(jdbcClient, "Nie ma takiego")).isFalse();
    }

    @Test
    @DisplayName("Wzorzec 2: wstaw produkt i zwróć klucz nadany przez bazę")
    void solution2() {
        long id = Jdbc05RepositorySolutions.solution2(jdbcClient, "Mleko", 349);
        assertThat(jdbcClient.sql("SELECT name FROM products WHERE id = :id").param("id", id)
                .query(String.class).single()).isEqualTo("Mleko");
    }

    @Test
    @DisplayName("Wzorzec 3: wstawienie zwraca true, a zajęta nazwa false (bez wyjątku)")
    @Sql("/db/jdbc05/two-products.sql")
    void solution3() {
        assertThat(Jdbc05RepositorySolutions.solution3(jdbcClient, "Mleko", 349)).isTrue();
        assertThat(Jdbc05RepositorySolutions.solution3(jdbcClient, "Kawa", 1)).isFalse();
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM products").query(Integer.class).single()).isEqualTo(3);
    }
}
