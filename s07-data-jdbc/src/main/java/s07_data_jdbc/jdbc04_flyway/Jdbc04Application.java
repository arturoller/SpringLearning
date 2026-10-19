package s07_data_jdbc.jdbc04_flyway;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Klasa startowa lekcji Jdbc04Flyway. Włącza profil {@code jdbc04}: przy starcie Flyway wykona migracje z katalogu
 * db/migration/jdbc04 na bazie H2 w pamięci, aplikacja wypisze historię migracji i zakończy się.
 */
@SpringBootApplication
public class Jdbc04Application {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Jdbc04Application.class)
                .profiles("jdbc04").web(WebApplicationType.NONE).run(args)) {
            context.getBean(MigrationHistory.class).describe().forEach(line -> System.out.println(line));
        }
    }
}
