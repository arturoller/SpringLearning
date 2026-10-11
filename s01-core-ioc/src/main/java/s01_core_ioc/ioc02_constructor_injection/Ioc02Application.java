package s01_core_ioc.ioc02_constructor_injection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Ioc02ConstructorInjection. Aplikacja bez serwera WWW: zbuduje kontekst i od razu się zakończy.
 * Klasy z błędami (CycleA, AmbiguousConstructorsService...) nie mają {@code @Component}, więc skanowanie ich pomija.
 */
@SpringBootApplication
public class Ioc02Application {

    public static void main(String[] args) {
        SpringApplication.run(Ioc02Application.class, args);
    }
}
