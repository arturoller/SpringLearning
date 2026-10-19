package s01_core_ioc.ioc06_scopes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Ioc06Scopes. Aplikacja bez serwera WWW: zbuduje kontekst, wypisze log i zakończy się.
 * Skanuje tylko ten pakiet (ioc06_scopes). Klasy Ioc06Exercise3Config i SingletonCartConfig nie mają adnotacji,
 * więc nie są skanowane (gdyby były, w kontekście byłyby dwa beany ShoppingCart).
 */
@SpringBootApplication
public class Ioc06Application {

    public static void main(String[] args) {
        SpringApplication.run(Ioc06Application.class, args);
    }
}
