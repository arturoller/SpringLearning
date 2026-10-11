package s03_boot_basics.boot02_writing_auto_configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Klasa startowa lekcji Boot02WritingAutoConfiguration. Włącza profil {@code boot02}, czyli plik
 * {@code application-boot02.properties} z {@code boot02.greeting.enabled=true} — dopiero wtedy nasza autokonfiguracja
 * działa. Uruchomiona wypisze powitanie i zakończy się (moduł nie ma serwera WWW).
 */
@SpringBootApplication
public class Boot02Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Boot02Application.class);
        application.setAdditionalProfiles("boot02");   // additional profiles = dodatkowe profile
        try (ConfigurableApplicationContext context = application.run(args)) {
            System.out.println(context.getBean(GreetingService.class).greet("Ania"));
        }
    }
}
