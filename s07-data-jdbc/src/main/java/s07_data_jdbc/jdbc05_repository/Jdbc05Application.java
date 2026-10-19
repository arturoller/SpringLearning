package s07_data_jdbc.jdbc05_repository;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;

/**
 * Klasa startowa lekcji Jdbc05Repository. Włącza profil {@code jdbc05}: Flyway tworzy tabelę product (migracje z
 * db/migration/jdbc05), aplikacja zapisuje jeden produkt przez repozytorium, wypisuje liczbę produktów i kończy się.
 */
@SpringBootApplication
public class Jdbc05Application {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Jdbc05Application.class)
                .profiles("jdbc05").web(WebApplicationType.NONE).run(args)) {
            ProductRepository repository = context.getBean(ProductRepository.class);
            Product saved = repository.save(new NewProduct("DOM-003", "Odkurzacz", "DOM", new BigDecimal("599.00"), 3));
            System.out.println("Zapisano: " + saved + ", produktów w bazie: " + repository.count());
        }
    }
}
