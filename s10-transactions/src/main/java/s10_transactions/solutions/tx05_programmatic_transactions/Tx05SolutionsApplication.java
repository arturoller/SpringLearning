package s10_transactions.solutions.tx05_programmatic_transactions;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import s10_transactions.tx05_programmatic_transactions.FakeEmailSender;

/**
 * Klasa startowa testów rozwiązań Tx05. Skanuje tylko ten pakiet; z lekcji dołącza jawnie FakeEmailSender
 * ({@code @Import} — importuj), którego używa rozwiązanie ćwiczenia 4. Encji nie ma (sam JdbcClient).
 */
@SpringBootApplication
@Import(FakeEmailSender.class)
public class Tx05SolutionsApplication {
}
