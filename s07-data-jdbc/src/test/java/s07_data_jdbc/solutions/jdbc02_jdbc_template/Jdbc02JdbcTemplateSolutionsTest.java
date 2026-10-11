package s07_data_jdbc.solutions.jdbc02_jdbc_template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import s07_data_jdbc.jdbc02_jdbc_template.Customer;
import s07_data_jdbc.jdbc02_jdbc_template.Jdbc02Application;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Jdbc02JdbcTemplateExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Pakiet solutions nie ma własnej klasy startowej, więc wskazujemy ją wprost: {@code classes = Jdbc02Application.class}.
 */
@Tag("wzorzec")
@SpringBootTest(classes = Jdbc02Application.class)
@ActiveProfiles("jdbc02")
@Transactional
class Jdbc02JdbcTemplateSolutionsTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    NamedParameterJdbcTemplate namedTemplate;

    @Test
    @DisplayName("Wzorzec 1: klienci z miasta posortowani po nazwie")
    void solution1() {
        assertThat(Jdbc02JdbcTemplateSolutions.solution1(jdbcTemplate, "Warszawa"))
                .extracting(Customer::name).containsExactly("Anna Nowak", "Ewa Wisniewska");
        assertThat(Jdbc02JdbcTemplateSolutions.solution1(jdbcTemplate, "Gdynia")).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 2: klient po id albo Optional.empty (bez wyjątku)")
    void solution2() {
        assertThat(Jdbc02JdbcTemplateSolutions.solution2(jdbcTemplate, 2)).get()
                .extracting(Customer::name).isEqualTo("Jan Kowalski");
        assertThat(Jdbc02JdbcTemplateSolutions.solution2(jdbcTemplate, 99)).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 3: wsadowe wstawienie klientów")
    void solution3() {
        List<Customer> newOnes = List.of(new Customer(0, "Adam", "Lodz"), new Customer(0, "Bartek", "Lodz"),
                new Customer(0, "Celina", "Lodz"));
        assertThat(Jdbc02JdbcTemplateSolutions.solution3(namedTemplate, newOnes)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customers", Integer.class)).isEqualTo(6);
    }
}
