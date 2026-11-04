package s10_transactions.solutions.tx01_transactional;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import s10_transactions.tx01_transactional.Wallet;

/**
 * Klasa startowa testów rozwiązań Tx01. Encja i repozytorium są w pakiecie lekcji, więc wskazujemy je jawnie
 * ({@code @EntityScan} — skanowanie encji, {@code @EnableJpaRepositories} — włącz repozytoria JPA). Skanowanie
 * komponentów obejmuje tylko ten pakiet: serwisy lekcji (i szkielet ćwiczeń) nie trafiają do tego kontekstu.
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Wallet.class)
@EnableJpaRepositories(basePackageClasses = Wallet.class)
public class Tx01SolutionsApplication {
}
