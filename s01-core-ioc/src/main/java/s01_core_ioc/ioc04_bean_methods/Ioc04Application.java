package s01_core_ioc.ioc04_bean_methods;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Ioc04BeanMethods (aplikacja bez serwera WWW — wystartuje, zbuduje kontekst i się zakończy).
 * Skanuje swój pakiet: znajdzie {@code ShopConfig}, {@code FullModeConfig} i {@code LiteModeConfig} (oznaczone
 * {@code @Configuration}). Klasy demonstracyjne BEZ adnotacji (np. ćwiczenia) nie są skanowane.
 */
@SpringBootApplication
public class Ioc04Application {

    public static void main(String[] args) {
        SpringApplication.run(Ioc04Application.class, args);
    }
}
