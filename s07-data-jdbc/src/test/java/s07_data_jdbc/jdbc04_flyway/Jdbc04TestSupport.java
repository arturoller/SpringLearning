package s07_data_jdbc.jdbc04_flyway;

import org.flywaydb.core.Flyway;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Pomocnik testów lekcji Jdbc04Flyway: uruchamianie aplikacji z ustawieniami, świeże bazy H2 i programowa
 * konfiguracja Flyway na katalogu z plikami. Ustawienia aplikacji podajemy jako ARGUMENTY (najwyższy priorytet).
 */
public final class Jdbc04TestSupport {

    public static final String PROFILE = "spring.profiles.active=jdbc04";
    public static final String CLASSPATH_MIGRATIONS = "classpath:db/migration/jdbc04";

    private Jdbc04TestSupport() {
    }

    /** Adres H2 w pamięci o unikalnej nazwie; DB_CLOSE_DELAY=-1 = baza żyje po zamknięciu ostatniego połączenia. */
    public static String memUrl() {
        return "jdbc:h2:mem:jdbc04-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    }

    public static DataSource newDataSource() {
        return new DriverManagerDataSource(memUrl(), "sa", "");
    }

    public static ConfigurableApplicationContext start(String... properties) {
        return start(Arrays.asList(properties));
    }

    public static ConfigurableApplicationContext start(Collection<String> properties) {
        List<String> arguments = new ArrayList<>();
        properties.forEach(property -> arguments.add("--" + property));
        return new SpringApplicationBuilder(Jdbc04Application.class)
                .web(WebApplicationType.NONE)
                .run(arguments.toArray(String[]::new));
    }

    /** Flyway czytający pliki z podanego katalogu (lokalizacja „filesystem:”) na podanej bazie. */
    public static Flyway flywayOn(DataSource dataSource, Path directory) {
        return Flyway.configure().dataSource(dataSource).locations("filesystem:" + directory).load();
    }

    /** Flyway czytający migracje lekcji z classpath ORAZ dodatkowe pliki z katalogu (dwie lokalizacje). */
    public static Flyway flywayOnLessonAnd(DataSource dataSource, Path directory) {
        return Flyway.configure().dataSource(dataSource)
                .locations(CLASSPATH_MIGRATIONS, "filesystem:" + directory).load();
    }

    public static void write(Path directory, String fileName, String sql) {
        try {
            Files.writeString(directory.resolve(fileName), sql);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static JdbcClient jdbcOn(DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }
}
