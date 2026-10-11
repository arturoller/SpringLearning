package s07_data_jdbc.jdbc02_jdbc_template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Jdbc02JdbcTemplate. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@SpringBootTest
@ActiveProfiles("jdbc02")
@Transactional
class Jdbc02JdbcTemplateExercisesTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    NamedParameterJdbcTemplate namedTemplate;

    @Test
    @DisplayName("Ćwiczenie 1: klienci z miasta posortowani po nazwie")
    void exercise1() {
        assertThat(Jdbc02JdbcTemplate.exercise1(jdbcTemplate, "Warszawa"))
                .extracting(Customer::name).containsExactly("Anna Nowak", "Ewa Wisniewska");
        assertThat(Jdbc02JdbcTemplate.exercise1(jdbcTemplate, "Gdynia")).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 2: klient po id albo Optional.empty (bez wyjątku)")
    void exercise2() {
        assertThat(Jdbc02JdbcTemplate.exercise2(jdbcTemplate, 2)).get()
                .extracting(Customer::name).isEqualTo("Jan Kowalski");
        assertThat(Jdbc02JdbcTemplate.exercise2(jdbcTemplate, 99)).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 3: wsadowe wstawienie klientów")
    void exercise3() {
        List<Customer> newOnes = List.of(new Customer(0, "Adam", "Lodz"), new Customer(0, "Bartek", "Lodz"),
                new Customer(0, "Celina", "Lodz"));
        assertThat(Jdbc02JdbcTemplate.exercise3(namedTemplate, newOnes)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customers", Integer.class)).isEqualTo(6);
    }
}
