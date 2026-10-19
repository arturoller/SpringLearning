package s07_data_jdbc.jdbc03_sql_init;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Klasa startowa lekcji Jdbc03SqlInit. Włącza profil {@code jdbc03}: przy starcie Boot wykona skrypty
 * db/jdbc03/schema.sql i db/jdbc03/data.sql na bazie H2 w pamięci; aplikacja wypisze liczbę pracowników i zakończy się.
 */
@SpringBootApplication
public class Jdbc03Application {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Jdbc03Application.class)
                .profiles("jdbc03").web(WebApplicationType.NONE).run(args)) {
            System.out.println("Pracowników w bazie: " + context.getBean(EmployeeQueries.class).count());
        }
    }
}
