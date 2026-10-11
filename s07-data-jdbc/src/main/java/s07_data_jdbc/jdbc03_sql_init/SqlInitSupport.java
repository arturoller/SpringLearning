package s07_data_jdbc.jdbc03_sql_init;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;
import java.util.List;
import java.util.stream.Stream;

/**
 * Narzędzia lekcji Jdbc03SqlInit do doświadczeń BEZ pełnej aplikacji lekcji.
 *
 * <p>{@link #start(String...)} uruchamia MAŁY kontekst Springa, w którym działają tylko dwie autokonfiguracje:
 * DataSourceAutoConfiguration (tworzy DataSource, w tym bazę H2 w pamięci o unikalnej nazwie) i
 * DataSourceInitializationAutoConfiguration (wykonuje skrypty {@code spring.sql.init.*}). Kontekst NIE czyta
 * application.properties (opcja {@code spring.config.name} wskazuje nieistniejący plik), więc widzimy DOMYŚLNE
 * zachowanie Boota, a właściwości podajemy ręcznie jako argumenty, np. {@code "spring.sql.init.mode=never"}.</p>
 *
 * <p>Gdy skrypt się nie powiedzie, {@code start} rzuca wyjątek (tak jak przy prawdziwym starcie aplikacji).
 * Zwrócony kontekst zamykaj w try-with-resources.</p>
 */
public final class SqlInitSupport {

    private SqlInitSupport() {
    }

    /** Klasa źródłowa małego kontekstu: nie jest @Configuration, więc skanowanie pakietu jej nie "podniesie". */
    @ImportAutoConfiguration({DataSourceAutoConfiguration.class, DataSourceInitializationAutoConfiguration.class})
    static class DatabaseOnly {
    }

    /** Startuje mały kontekst (DataSource + skrypty SQL) z podanymi właściwościami w postaci "klucz=wartość". */
    public static ConfigurableApplicationContext start(String... properties) {
        String[] args = Stream.concat(
                        Stream.of("spring.main.banner-mode=off", "spring.config.name=nie-ma-takiego-pliku"),
                        Stream.of(properties))
                .map(property -> "--" + property)
                .toArray(String[]::new);
        return new SpringApplicationBuilder(DatabaseOnly.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    /** Posortowane nazwy tabel użytkownika (schemat PUBLIC) w bazie z podanego kontekstu. */
    public static List<String> tableNames(ApplicationContext context) {
        return JdbcClient.create(context.getBean(DataSource.class))
                .sql("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' ORDER BY TABLE_NAME")
                .query(String.class)
                .list();
    }
}
