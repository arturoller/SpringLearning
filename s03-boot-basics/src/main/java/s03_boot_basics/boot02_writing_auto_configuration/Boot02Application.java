package s03_boot_basics.boot02_writing_auto_configuration;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Klasa startowa lekcji Boot02WritingAutoConfiguration. Włącza profil {@code boot02}
 * (plik application-boot02.properties z {@code boot02.price.enabled=true}), więc autokonfiguracja cen z pliku imports
 * się włącza, a serwis {@link ProductLabels} dostaje PriceFormatter i PriceTagPrinter.
 * Aplikacja bez serwera WWW — buduje kontekst i kończy działanie.
 */
@SpringBootApplication
public class Boot02Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Boot02Application.class).profiles("boot02").run(args);
    }
}
