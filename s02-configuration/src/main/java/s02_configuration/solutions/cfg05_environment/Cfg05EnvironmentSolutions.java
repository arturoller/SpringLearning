package s02_configuration.solutions.cfg05_environment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.Map;

/** Rozwiązania wzorcowe ćwiczeń lekcji Cfg05Environment — zajrzyj dopiero po własnej próbie! */
public final class Cfg05EnvironmentSolutions {

    private Cfg05EnvironmentSolutions() {
    }

    /** Rozwiązanie 1: getProperty z typem i wartością domyślną. */
    public static int solution1(Environment environment) {
        return environment.getProperty("cfg05.exercise.retries", Integer.class, 3);
    }

    /** Rozwiązanie 2: bez matchIfMissing — brak klucza oznacza brak beana. */
    @ConditionalOnProperty(name = "cfg05.exercise.express", havingValue = "true")
    public static class ExpressShipping {
    }

    /** Rozwiązanie 3: sztuczne źródło zmiennych systemowych + Binder z luźnym dopasowaniem nazw. */
    public static int solution3(Map<String, Object> fakeOsEnvironment) {
        StandardEnvironment environment = new StandardEnvironment();
        // nazwa źródła MUSI być "systemEnvironment" — tylko wtedy Boot stosuje reguły nazw zmiennych środowiska
        // (replace = zastąp: prawdziwe zmienne komputera nie mieszają się z mapą z testu)
        String name = StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;
        environment.getPropertySources().replace(name, new SystemEnvironmentPropertySource(name, fakeOsEnvironment));
        return new Binder(ConfigurationPropertySources.get(environment))
                .bind("cfg05.exercise.max-days", Integer.class)
                .orElse(-1);
    }
}
