package s03_boot_basics.boot03_spring_application;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

/**
 * Klasa startowa lekcji Boot03SpringApplication. Zamiast jednolinijkowego {@code SpringApplication.run(...)} buduje
 * obiekt SpringApplication i ustawia go krok po kroku (metoda createApplication — używają jej też testy).
 *
 * <p>Uruchom z argumentami (IntelliJ: Run → Edit Configurations → Program arguments), np.:
 * {@code --import.file=ceny.csv pilne --boot03.exit-code=3} — w logu zobaczysz wpisy runnerów, a proces zakończy się
 * kodem 3 (IntelliJ pokaże „Process finished with exit code 3”).</p>
 */
@SpringBootApplication
public class Boot03Application {

    /** Skonfigurowana, jeszcze NIEuruchomiona aplikacja. */
    public static SpringApplication createApplication() {
        SpringApplication application = new SpringApplication(Boot03Application.class);
        application.setWebApplicationType(WebApplicationType.NONE);   // bez serwera WWW, nawet gdyby był na classpath
        application.setBannerMode(Banner.Mode.OFF);                   // bez napisu „Spring” z gwiazdek przy starcie
        application.setAdditionalProfiles("boot03");                  // zawsze aktywny profil boot03
        application.setDefaultProperties(Map.of(                      // wartości domyślne — najniższy priorytet
                "boot03.shop-name", "Sklep Domyślny"));
        return application;
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = createApplication().run(args);
        context.getBean(RunLog.class).entries().forEach(System.out::println);
        // exit zamyka kontekst i zwraca kod z beanów ExitCodeGenerator; System.exit przekazuje go systemowi
        System.exit(SpringApplication.exit(context));
    }
}
