package s07_data_jdbc.jdbc03_sql_init;

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

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Jdbc03SqlInit. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Jdbc03SqlInitExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: tryb never — tabela employee nie powstaje")
    void exercise1() {
        try (ConfigurableApplicationContext context = Jdbc03TestSupport.start(Jdbc03SqlInit.exercise1())) {
            assertThat(context.getBean(EmployeeQueries.class).tableExists("employee")).isFalse();
        }
    }

    @Test
    @DisplayName("Ćwiczenie 2: skrypt z błędem, a aplikacja startuje (2 wiersze)")
    void exercise2() {
        try (ConfigurableApplicationContext context = Jdbc03TestSupport.start(Jdbc03SqlInit.exercise2())) {
            assertThat(context.getBean(EmployeeQueries.class).count()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("Ćwiczenie 3: obok tabeli employee istnieje widok it_employee")
    void exercise3() {
        try (ConfigurableApplicationContext context = Jdbc03TestSupport.start(Jdbc03SqlInit.exercise3())) {
            EmployeeQueries queries = context.getBean(EmployeeQueries.class);
            assertThat(queries.tableExists("it_employee")).isTrue();
            assertThat(queries.count()).isEqualTo(7);   // data.sql z profilu nadal się wykonał
        }
    }

    @Test
    @DisplayName("Ćwiczenie 4: skrypt idempotentny — po dwóch wykonaniach jeden wiersz")
    void exercise4() {
        EmbeddedDatabase database = new EmbeddedDatabaseBuilder().generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2).build();
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                    new ByteArrayResource(Jdbc03SqlInit.exercise4().getBytes(StandardCharsets.UTF_8)));
            populator.execute(database);
            populator.execute(database);   // drugi raz: bez wyjątku i bez duplikatu
            assertThat(new JdbcTemplate(database).queryForObject("SELECT COUNT(*) FROM department", Integer.class))
                    .isEqualTo(1);
        } finally {
            database.shutdown();
        }
    }
}
