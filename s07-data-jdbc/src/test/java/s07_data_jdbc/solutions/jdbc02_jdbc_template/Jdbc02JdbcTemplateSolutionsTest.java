package s07_data_jdbc.solutions.jdbc02_jdbc_template;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import s07_data_jdbc.jdbc02_jdbc_template.Customer;
import s07_data_jdbc.jdbc02_jdbc_template.Jdbc02TestDatabase;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Jdbc02JdbcTemplateExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Jdbc02JdbcTemplateSolutionsTest {

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
    @DisplayName("Wzorzec 1: liczba produktów w kategorii")
    void solution1() {
        assertThat(Jdbc02JdbcTemplateSolutions.solution1(jdbcTemplate, "KSIAZKI")).isEqualTo(3);
        assertThat(Jdbc02JdbcTemplateSolutions.solution1(jdbcTemplate, "NIE-MA")).isZero();
    }

    @Test
    @DisplayName("Wzorzec 2: klient jako Optional, bez wyjątku dla nieistniejącego")
    void solution2() {
        assertThat(Jdbc02JdbcTemplateSolutions.solution2(jdbcTemplate, 2)).get()
                .extracting(Customer::name).isEqualTo("Maria Nowak");
        assertThat(Jdbc02JdbcTemplateSolutions.solution2(jdbcTemplate, 99)).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 3: paczka zmian stanów, suma zmienionych wierszy")
    void solution3() {
        int changed = Jdbc02JdbcTemplateSolutions.solution3(new NamedParameterJdbcTemplate(jdbcTemplate),
                Map.of("DOM-001", 5, "DOM-002", 6, "NIE-MA", 1));
        assertThat(changed).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT stock FROM product WHERE sku = 'DOM-002'", Integer.class))
                .isEqualTo(6);
    }

    @Test
    @DisplayName("Wzorzec 4: wstawienie zamówienia, duplikat i brak klienta")
    void solution4() {
        assertThat(Jdbc02JdbcTemplateSolutions.solution4(jdbcTemplate, "ZAM-010", 1)).isEqualTo("OK");
        assertThat(Jdbc02JdbcTemplateSolutions.solution4(jdbcTemplate, "ZAM-010", 1)).isEqualTo("DUPLIKAT");
        assertThat(Jdbc02JdbcTemplateSolutions.solution4(jdbcTemplate, "ZAM-011", 999)).isEqualTo("BRAK_KLIENTA");
    }
}
