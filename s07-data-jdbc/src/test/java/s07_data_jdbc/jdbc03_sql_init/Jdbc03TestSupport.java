package s07_data_jdbc.jdbc03_sql_init;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pomocnik testów lekcji Jdbc03SqlInit: uruchamia aplikację lekcji z podanymi ustawieniami (jako argumenty wiersza
 * poleceń — mają najwyższy priorytet) i unikalną nazwą bazy w pamięci, więc scenariusze testów nie widzą swoich danych.
 */
public final class Jdbc03TestSupport {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private Jdbc03TestSupport() {
    }

    /** Aplikacja z profilem jdbc03 (jego skrypty i ustawienia) i świeżą bazą. {@code settings}: „klucz=wartość”. */
    public static ConfigurableApplicationContext start(String... settings) {
        return start(true, "jdbc:h2:mem:jdbc03_test_" + COUNTER.incrementAndGet(), settings);
    }

    /** To samo, ale z podanym adresem bazy (żeby uruchomić aplikację dwa razy na tej samej bazie). */
    public static ConfigurableApplicationContext startOn(String url, String... settings) {
        return start(true, url, settings);
    }

    /** Aplikacja BEZ profilu jdbc03 (bez jego schema-locations) — potrzebna do pokazania domyślnych lokalizacji. */
    public static ConfigurableApplicationContext startWithoutProfile(String... settings) {
        return start(false, "jdbc:h2:mem:jdbc03_test_" + COUNTER.incrementAndGet(), settings);
    }

    private static ConfigurableApplicationContext start(boolean profile, String url, String... settings) {
        List<String> args = new ArrayList<>();
        args.add("--spring.datasource.url=" + url);
        if (!profile) {
            args.add("--spring.flyway.enabled=false");   // profil jdbc03 tego nie ustawia, więc robimy to ręcznie
        }
        for (String setting : settings) {
            args.add("--" + setting);
        }
        SpringApplicationBuilder builder = new SpringApplicationBuilder(Jdbc03Application.class)
                .web(WebApplicationType.NONE);
        if (profile) {
            builder.profiles("jdbc03");
        }
        return builder.run(args.toArray(new String[0]));
    }
}
