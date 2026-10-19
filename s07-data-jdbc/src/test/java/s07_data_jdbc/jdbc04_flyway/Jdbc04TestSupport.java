package s07_data_jdbc.jdbc04_flyway;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pomocnik testów lekcji Jdbc04Flyway. Migracje lekcji kopiujemy do katalogu tymczasowego, żeby test mógł je
 * ZMIENIAĆ (jak programista, który edytuje już wykonaną migrację) bez ruszania plików w repozytorium; aplikację
 * uruchamiamy ze wskazaniem tego katalogu ({@code spring.flyway.locations=filesystem:...}).
 */
public final class Jdbc04TestSupport {

    /** Pliki migracji lekcji (w src/main/resources/db/migration/jdbc04). */
    public static final List<String> LESSON_MIGRATIONS = List.of(
            "V1__create_product.sql", "V2__insert_products.sql", "V3__add_stock_column.sql",
            "R__expensive_products_view.sql");

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private Jdbc04TestSupport() {
    }

    /** Nowy, unikalny adres bazy w pamięci, która przeżywa zamknięcie kontekstu (symulacja trwałej bazy). */
    public static String newDatabaseUrl() {
        return "jdbc:h2:mem:jdbc04_test_" + COUNTER.incrementAndGet() + ";DB_CLOSE_DELAY=-1";
    }

    /** Kopiuje migracje lekcji z classpath do podanego katalogu. */
    public static void copyLessonMigrations(Path directory) {
        try {
            Files.createDirectories(directory);
            for (String file : LESSON_MIGRATIONS) {
                try (InputStream in = Jdbc04TestSupport.class.getResourceAsStream("/db/migration/jdbc04/" + file)) {
                    Files.write(directory.resolve(file), in.readAllBytes());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Uruchamia aplikację lekcji (profil jdbc04) na bazie o podanym adresie, z migracjami z katalogu i ustawieniami. */
    public static ConfigurableApplicationContext start(String databaseUrl, Path migrationsDirectory, String... settings) {
        List<String> args = new ArrayList<>();
        args.add("--spring.datasource.url=" + databaseUrl);
        if (migrationsDirectory != null) {
            args.add("--spring.flyway.locations=filesystem:" + migrationsDirectory.toString().replace('\\', '/'));
        }
        for (String setting : settings) {
            args.add("--" + setting);
        }
        return new SpringApplicationBuilder(Jdbc04Application.class)
                .web(WebApplicationType.NONE)
                .profiles("jdbc04")
                .run(args.toArray(new String[0]));
    }
}
