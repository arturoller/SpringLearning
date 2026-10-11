package s02_configuration.solutions.cfg01_properties;

import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;

/** Rozwiązania wzorcowe ćwiczeń lekcji Cfg01Properties — zajrzyj dopiero po własnej próbie! */
public final class Cfg01PropertiesSolutions {

    private Cfg01PropertiesSolutions() {
    }

    /** Rozwiązanie 1: @Value z placeholderem przy parametrze konstruktora. */
    public static class Greeter {
        private final String shopName;

        Greeter(@Value("${cfg01.shop.name}") String shopName) {
            this.shopName = shopName;
        }

        String greet() {
            return "Witaj w " + shopName + "!";
        }
    }

    /** Rozwiązanie 2: wartości domyślne po dwukropku; "5s" zamienia Duration konwersja Boota. */
    public static class Limits {
        private final int retries;
        private final Duration timeout;
        private final boolean enabled;

        Limits(@Value("${cfg01.exercise.retries:3}") int retries,
               @Value("${cfg01.exercise.timeout:5s}") Duration timeout,
               @Value("${cfg01.exercise.enabled:true}") boolean enabled) {
            this.retries = retries;
            this.timeout = timeout;
            this.enabled = enabled;
        }

        int retries() {
            return retries;
        }

        Duration timeout() {
            return timeout;
        }

        boolean enabled() {
            return enabled;
        }
    }

    /** Rozwiązanie 3: limit jako parametr konstruktora — jest już dostępny, gdy konstruktor działa. */
    public static class Counter {
        final int doubled;

        Counter(@Value("${cfg01.exercise.limit}") int limit) {
            this.doubled = limit * 2;
        }
    }
}
