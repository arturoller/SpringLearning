package s07_data_jdbc.jdbc01_jdbc_client;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Klasa startowa lekcji Jdbc01JdbcClient. Uruchamiana z IntelliJ, włącza profil {@code jdbc01} (baza H2 + schema.sql
 * + data.sql tej lekcji), buduje kontekst, wypisuje kilka produktów i kończy się (brak serwera WWW).
 */
@SpringBootApplication
public class Jdbc01Application {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Jdbc01Application.class)
                .profiles("jdbc01").web(WebApplicationType.NONE).run(args)) {
            context.getBean(ProductQueries.class).findByCategory("KSIAZKI")
                    .forEach(product -> System.out.println(product));
        }
    }
}
