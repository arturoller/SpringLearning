package s10_transactions.solutions.tx04_read_only_isolation;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import s10_transactions.tx04_read_only_isolation.OtherUser;
import s10_transactions.tx04_read_only_isolation.TopUpService;
import s10_transactions.tx04_read_only_isolation.Wallet;
import s10_transactions.tx04_read_only_isolation.WalletRepository;

/**
 * Klasa startowa testów rozwiązań Tx04. Skanuje tylko ten pakiet; encje i repozytoria lekcji wskazuje jawnie
 * ({@code @EntityScan} — skanuj encje, {@code @EnableJpaRepositories} — włącz repozytoria JPA), a z beanów lekcji
 * dołącza ({@code @Import}) TopUpService (używa go ćwiczenie 4) i OtherUser („drugi użytkownik” w testach).
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Wallet.class)
@EnableJpaRepositories(basePackageClasses = WalletRepository.class)
@Import({TopUpService.class, OtherUser.class})
public class Tx04SolutionsApplication {
}
