package s07_data_jdbc.jdbc02_jdbc_template;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Jdbc02JdbcTemplate. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Jdbc02JdbcTemplateExercisesTest {

    private EmbeddedDatabase database;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        database = Jdbc02TestDatabase.create();
        jdbcTemplate = new JdbcTemplate(database);
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    @DisplayName("Ćwiczenie 1: liczba produktów w kategorii")
    void exercise1() {
        assertThat(Jdbc02JdbcTemplate.exercise1(jdbcTemplate, "KSIAZKI")).isEqualTo(3);
        assertThat(Jdbc02JdbcTemplate.exercise1(jdbcTemplate, "NIE-MA")).isZero();
    }

    @Test
    @DisplayName("Ćwiczenie 2: klient jako Optional, bez wyjątku dla nieistniejącego")
    void exercise2() {
        assertThat(Jdbc02JdbcTemplate.exercise2(jdbcTemplate, 2)).get()
                .extracting(Customer::name).isEqualTo("Maria Nowak");
        assertThat(Jdbc02JdbcTemplate.exercise2(jdbcTemplate, 99)).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 3: paczka zmian stanów, suma zmienionych wierszy")
    void exercise3() {
        int changed = Jdbc02JdbcTemplate.exercise3(new NamedParameterJdbcTemplate(jdbcTemplate),
                Map.of("DOM-001", 5, "DOM-002", 6, "NIE-MA", 1));
        assertThat(changed).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT stock FROM product WHERE sku = 'DOM-002'", Integer.class))
                .isEqualTo(6);
    }

    @Test
    @DisplayName("Ćwiczenie 4: wstawienie zamówienia, duplikat i brak klienta")
    void exercise4() {
        assertThat(Jdbc02JdbcTemplate.exercise4(jdbcTemplate, "ZAM-010", 1)).isEqualTo("OK");
        assertThat(Jdbc02JdbcTemplate.exercise4(jdbcTemplate, "ZAM-010", 1)).isEqualTo("DUPLIKAT");
        assertThat(Jdbc02JdbcTemplate.exercise4(jdbcTemplate, "ZAM-011", 999)).isEqualTo("BRAK_KLIENTA");
    }
}
