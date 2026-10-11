package s01_core_ioc.ioc07_lifecycle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Ioc07Lifecycle. Aplikacja bez serwera WWW: zbuduje kontekst, wypisze log i zakończy się.
 * Klasy demonstracyjne bez adnotacji (BrokenProductCache, AllCallbacksBean, Ioc07DemoConfigs...) nie są skanowane —
 * testy rejestrują je ręcznie.
 */
@SpringBootApplication
public class Ioc07Application {

    public static void main(String[] args) {
        SpringApplication.run(Ioc07Application.class, args);
    }
}
