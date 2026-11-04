package s10_transactions.solutions.tx02_propagation;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import s10_transactions.tx02_propagation.StockService;

/**
 * Klasa startowa testów rozwiązań Tx02. Skanuje tylko ten pakiet; z lekcji dołącza jawnie StockService
 * ({@code @Import} — importuj), którego używa rozwiązanie ćwiczenia 2. Encji nie ma (sam JdbcClient).
 */
@SpringBootApplication
@Import(StockService.class)
public class Tx02SolutionsApplication {
}
