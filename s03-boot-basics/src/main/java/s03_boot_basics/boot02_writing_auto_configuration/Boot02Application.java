package s03_boot_basics.boot02_writing_auto_configuration;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Klasa startowa lekcji Boot02WritingAutoConfiguration — „aplikacja, która korzysta z biblioteki sklepu”.
 * Włącza profil {@code boot02} (plik application-boot02.properties), a w nim {@code s03.greeter.enabled=true}.
 * Sama nie definiuje beana {@link ShopGreeter} — dostaje go z autokonfiguracji zarejestrowanej w pliku
 * {@code .imports}. Bez serwera WWW: wypisuje powitanie i kończy działanie.
 */
@SpringBootApplication
public class Boot02Application {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context =
                     new SpringApplicationBuilder(Boot02Application.class).profiles("boot02").run(args)) {
            System.out.println(context.getBean(ShopGreeter.class).greet("Jan Kowalski"));
        }
    }
}
