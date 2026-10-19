package s07_data_jdbc.solutions.jdbc03_sql_init;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import s07_data_jdbc.jdbc03_sql_init.EmployeeQueries;
import s07_data_jdbc.jdbc03_sql_init.Jdbc03TestSupport;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Jdbc03SqlInitExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Uruchamiają aplikację lekcji przez Jdbc03TestSupport (każdy scenariusz na własnej, świeżej bazie).
 */
@Tag("wzorzec")
class Jdbc03SqlInitSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: tryb never — tabela employee nie powstaje")
    void solution1() {
        try (ConfigurableApplicationContext context = Jdbc03TestSupport.start(Jdbc03SqlInitSolutions.solution1())) {
            assertThat(context.getBean(EmployeeQueries.class).tableExists("employee")).isFalse();
        }
    }

    @Test
    @DisplayName("Wzorzec 2: skrypt z błędem, a aplikacja startuje (2 wiersze)")
    void solution2() {
        try (ConfigurableApplicationContext context = Jdbc03TestSupport.start(Jdbc03SqlInitSolutions.solution2())) {
            assertThat(context.getBean(EmployeeQueries.class).count()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("Wzorzec 3: obok tabeli employee istnieje widok it_employee")
    void solution3() {
        try (ConfigurableApplicationContext context = Jdbc03TestSupport.start(Jdbc03SqlInitSolutions.solution3())) {
            EmployeeQueries queries = context.getBean(EmployeeQueries.class);
            assertThat(queries.tableExists("it_employee")).isTrue();
            assertThat(queries.count()).isEqualTo(7);
        }
    }

    @Test
    @DisplayName("Wzorzec 4: skrypt idempotentny — po dwóch wykonaniach jeden wiersz")
    void solution4() {
        EmbeddedDatabase database = new EmbeddedDatabaseBuilder().generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2).build();
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                    new ByteArrayResource(Jdbc03SqlInitSolutions.solution4().getBytes(StandardCharsets.UTF_8)));
            populator.execute(database);
            populator.execute(database);
            assertThat(new JdbcTemplate(database).queryForObject("SELECT COUNT(*) FROM department", Integer.class))
                    .isEqualTo(1);
        } finally {
            database.shutdown();
        }
    }
}
