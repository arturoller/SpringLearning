package s07_data_jdbc.jdbc03_sql_init;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Pomocnik testów lekcji Jdbc03SqlInit: uruchamia aplikację lekcji z podanymi ustawieniami.
 * Ustawienia podajemy jako ARGUMENTY (najwyższy priorytet), bo "właściwości domyślne" przegrałyby z plikiem profilu.
 */
public final class Jdbc03TestApps {

    public static final String PROFILE = "spring.profiles.active=jdbc03";

    private Jdbc03TestApps() {
    }

    /** Adres H2 w pamięci o unikalnej nazwie — żeby testy nigdy nie współdzieliły bazy. */
    public static String memUrl() {
        return "spring.datasource.url=jdbc:h2:mem:jdbc03-" + UUID.randomUUID();
    }

    /** Adres H2 w pliku (baza TRWAŁA, niewbudowana w rozumieniu Boota). */
    public static String fileUrl(Path directory) {
        return "spring.datasource.url=jdbc:h2:file:" + directory.resolve("shop");
    }

    public static ConfigurableApplicationContext start(String... properties) {
        return start(Arrays.asList(properties));
    }

    public static ConfigurableApplicationContext start(Collection<String> properties) {
        List<String> arguments = new ArrayList<>();
        properties.forEach(property -> arguments.add("--" + property));
        return new SpringApplicationBuilder(Jdbc03Application.class)
                .web(WebApplicationType.NONE)
                .run(arguments.toArray(String[]::new));
    }

    /** Liczba wierszy w tabeli product (rzuca wyjątek, gdy tabeli nie ma). */
    public static int countProducts(ConfigurableApplicationContext context) {
        return context.getBean(JdbcTemplate.class).queryForObject("SELECT COUNT(*) FROM product", Integer.class);
    }
}
