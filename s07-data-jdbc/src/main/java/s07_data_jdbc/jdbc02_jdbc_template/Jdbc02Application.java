package s07_data_jdbc.jdbc02_jdbc_template;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Klasa startowa lekcji Jdbc02JdbcTemplate. Włącza profil {@code jdbc02} (baza H2 + schema.sql + data.sql tej lekcji),
 * wypisuje klientów z Warszawy i kończy się (brak serwera WWW).
 */
@SpringBootApplication
public class Jdbc02Application {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Jdbc02Application.class)
                .profiles("jdbc02").web(WebApplicationType.NONE).run(args)) {
            context.getBean(ShopTemplates.class).customersFromCity("Warszawa")
                    .forEach(customer -> System.out.println(customer));
        }
    }
}
