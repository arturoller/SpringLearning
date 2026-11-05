package s03_boot_basics.boot03_spring_application;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * „Drogi” bean z licznikiem konstrukcji: pokazuje, KIEDY Spring tworzy obiekt (przy starcie czy dopiero przy użyciu).
 * Licznik jest statyczny, bo każdy kontekst tworzy własny egzemplarz — test porównuje stan przed i po.
 */
@Component
public class LazyReportGenerator {

    private static final AtomicInteger CREATED = new AtomicInteger();

    public LazyReportGenerator() {
        CREATED.incrementAndGet();
    }

    public static int createdCount() {
        return CREATED.get();
    }
}
