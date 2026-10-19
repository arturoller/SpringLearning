package s19_deploy.dep04_startup_and_shutdown;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/** „Kosztowny” bean (np. wczytujący duży słownik) — licznik instancji pokazuje, KIEDY Spring go tworzy. */
@Component
public class ExpensiveBean {

    static final AtomicInteger INSTANCES = new AtomicInteger();

    public ExpensiveBean() {
        INSTANCES.incrementAndGet();
    }
}
