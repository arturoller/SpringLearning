package s08_data_jpa.solutions.jpa06_flyway_with_jpa;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Jpa06FlywayWithJpaExercisesTest na rozwiązaniach wzorcowych. {@code @Tag("wzorzec")} — biegną zawsze.
 * Kontekst: Jpa06SolutionsApplication z tego pakietu (encja Customer z rozwiązania) + profil jpa06 (migracje lekcji).
 */
@DataJpaTest
@ActiveProfiles("jpa06")
@Tag("wzorzec")
class Jpa06FlywayWithJpaSolutionsTest {

    @Autowired
    TestEntityManager entityManager;

    @Test
    @DisplayName("Wzorzec 1: migracja V4 (katalog jpa06-solutions) dodaje kolumnę weight_grams (integer, może być pusta)")
    void solution1() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource("jdbc:h2:mem:jpa06solution1", true);
        try {
            Flyway flyway = Flyway.configure().dataSource(dataSource)
                    .locations("classpath:db/migration/jpa06", "classpath:db/migration/jpa06-solutions")
                    .load();
            flyway.migrate();

            MigrationInfo current = flyway.info().current();
            assertThat(current.getVersion().getVersion()).isEqualTo("4");
            assertThat(current.getDescription()).isEqualTo("add weight column");
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            assertThat(jdbc.queryForObject("select data_type from information_schema.columns "
                    + "where table_name = 'PRODUCTS' and column_name = 'WEIGHT_GRAMS'", String.class)).isEqualTo("INTEGER");
            assertThat(jdbc.queryForObject("select is_nullable from information_schema.columns "
                    + "where table_name = 'PRODUCTS' and column_name = 'WEIGHT_GRAMS'", String.class)).isEqualTo("YES");
        } finally {
            dataSource.destroy();
        }
    }

    @Test
    @DisplayName("Wzorzec 2: encja Customer pasuje do tabeli customers z migracji (validate ją przepuścił)")
    void solution2() {
        Customer jan = entityManager.find(Customer.class, 1L);
        assertThat(jan.getName()).isEqualTo("Jan Kowalski");
        assertThat(jan.getCity()).isEqualTo("Warszawa");
        assertThat(jan.isVip()).isTrue();
        assertThat(entityManager.find(Customer.class, 2L).getEmail()).isNull();

        Customer anna = entityManager.persistFlushFind(new Customer("Anna Wójcik", "Łódź", "anna@example.com", false));
        assertThat(anna.getId()).isGreaterThan(7L);
    }

    @Test
    @DisplayName("Wzorzec 3: diagnoza startu — null przy udanym starcie, najgłębsza przyczyna przy nieudanym")
    void solution3() {
        assertThat(Jpa06FlywayWithJpaSolutions.solution3("--spring.profiles.active=jpa06",
                "--spring.datasource.url=jdbc:h2:mem:jpa06solution3a")).isNull();
        assertThat(Jpa06FlywayWithJpaSolutions.solution3("--spring.profiles.active=jpa06",
                "--spring.datasource.url=jdbc:h2:mem:jpa06solution3b", "--spring.flyway.target=1"))
                .isEqualTo("Schema validation: missing column [stock] in table [products]");
    }
}
