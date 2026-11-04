package s07_data_jdbc.jdbc03_sql_init;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static s07_data_jdbc.jdbc03_sql_init.Jdbc03TestApps.PROFILE;
import static s07_data_jdbc.jdbc03_sql_init.Jdbc03TestApps.countProducts;
import static s07_data_jdbc.jdbc03_sql_init.Jdbc03TestApps.fileUrl;
import static s07_data_jdbc.jdbc03_sql_init.Jdbc03TestApps.memUrl;
import static s07_data_jdbc.jdbc03_sql_init.Jdbc03TestApps.start;

/** Testy ĆWICZEŃ lekcji Jdbc03SqlInit. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Jdbc03SqlInitExercisesTest {

    @TempDir
    Path tempDirectory;

    @Test
    @DisplayName("Ćwiczenie 1: skrypty wykonują się także na bazie plikowej")
    void exercise1() {
        try (ConfigurableApplicationContext app = start(fileUrl(tempDirectory),
                "spring.sql.init.schema-locations=classpath:jdbc03/schema.sql",
                "spring.sql.init.data-locations=classpath:jdbc03/data.sql",
                Jdbc03SqlInit.exercise1ModeProperty())) {
            assertThat(countProducts(app)).isEqualTo(3);
        }
    }

    @Test
    @DisplayName("Ćwiczenie 2: aplikację można uruchomić dwa razy na tym samym pliku bazy")
    void exercise2() {
        List<String> settings = List.of(PROFILE, fileUrl(tempDirectory),
                "spring.sql.init.schema-locations=" + Jdbc03SqlInit.exercise2RepeatableSchemaLocation(),
                "spring.sql.init.data-locations=");
        try (ConfigurableApplicationContext first = start(settings)) {
            assertThat(countProducts(first)).isZero();
        }
        try (ConfigurableApplicationContext second = start(settings)) {
            assertThat(countProducts(second)).isZero();
        }
    }

    @Test
    @DisplayName("Ćwiczenie 3: błędny skrypt danych nie zatrzymuje startu, w tabeli są 2 produkty")
    void exercise3() {
        List<String> settings = new ArrayList<>(List.of(PROFILE, memUrl()));
        settings.addAll(Jdbc03SqlInit.exercise3ContinueOnErrorProperties());
        try (ConfigurableApplicationContext app = start(settings)) {
            assertThat(countProducts(app)).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("Ćwiczenie 4: skrypt dodaje dwa produkty i zmienia stan Lampki")
    void exercise4() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:jdbc03-cw4-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new org.springframework.core.io.ClassPathResource("jdbc03/schema.sql"),
                new org.springframework.core.io.ClassPathResource("jdbc03/data.sql")).execute(dataSource);
        new ResourceDatabasePopulator(new ByteArrayResource(Jdbc03SqlInit.exercise4Script().getBytes(StandardCharsets.UTF_8)))
                .execute(dataSource);

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForList("SELECT name FROM product ORDER BY id", String.class))
                .containsExactly("Mysz", "Kubek", "Lampka", "Zeszyt", "Teczka");
        assertThat(jdbc.queryForObject("SELECT stock FROM product WHERE name = 'Lampka'", Integer.class)).isEqualTo(7);
        assertThat(jdbc.queryForObject("SELECT price FROM product WHERE name = 'Teczka'", java.math.BigDecimal.class))
                .isEqualByComparingTo("7.00");
    }

    @Test
    @DisplayName("Ćwiczenie 5: platforma wczytuje skrypty z korzenia classpath")
    void exercise5() {
        try (ConfigurableApplicationContext app = start(memUrl(), Jdbc03SqlInit.exercise5PlatformProperty())) {
            assertThat(countProducts(app)).isEqualTo(1);
        }
    }
}
