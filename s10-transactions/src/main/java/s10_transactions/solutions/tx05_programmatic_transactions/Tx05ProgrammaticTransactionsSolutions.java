package s10_transactions.solutions.tx05_programmatic_transactions;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import s10_transactions.tx05_programmatic_transactions.Product;
import s10_transactions.tx05_programmatic_transactions.ProductRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Tx05ProgrammaticTransactions — zajrzyj dopiero po własnej próbie!
 * Bean spoza skanowanego pakietu lekcji — test rozwiązań rejestruje go w {@code @SpringBootTest(classes = ...)}.
 */
@Service
public class Tx05ProgrammaticTransactionsSolutions {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ProductRepository products;
    private final JdbcClient jdbc;
    private final TransactionTemplate transactionTemplate;
    private final PlatformTransactionManager transactionManager;

    public Tx05ProgrammaticTransactionsSolutions(ProductRepository products, JdbcClient jdbc,
                                                 TransactionTemplate transactionTemplate,
                                                 PlatformTransactionManager transactionManager) {
        this.products = products;
        this.jdbc = jdbc;
        this.transactionTemplate = transactionTemplate;
        this.transactionManager = transactionManager;
    }

    private static BigDecimal percentOf(BigDecimal price, int percent) {
        return price.multiply(BigDecimal.valueOf(percent)).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal raisePriceByPercent(String sku, int percent) {
        return transactionTemplate.execute(status -> {
            Product product = products.findById(sku).orElseThrow();
            BigDecimal newPrice = percentOf(product.getPrice(), 100 + percent);
            product.changePrice(newPrice);
            return newPrice;
        });
    }

    public BigDecimal previewCatalogValueAfterDiscount(int percent) {
        return transactionTemplate.execute(status -> {
            for (Product product : products.findAll()) {
                product.changePrice(percentOf(product.getPrice(), 100 - percent));
            }
            products.flush();                                   // bez flush SQL widziałby stare ceny
            BigDecimal total = jdbc.sql("select sum(price) from products").query(BigDecimal.class).single();
            status.setRollbackOnly();                           // tylko symulacja
            return total;
        });
    }

    public int importInChunks(Map<String, BigDecimal> prices, int chunkSize) {
        List<Map.Entry<String, BigDecimal>> entries = new ArrayList<>(prices.entrySet());
        int saved = 0;
        for (int from = 0; from < entries.size(); from += chunkSize) {
            List<Map.Entry<String, BigDecimal>> chunk = entries.subList(from, Math.min(from + chunkSize, entries.size()));
            try {
                transactionTemplate.executeWithoutResult(status -> chunk.forEach(entry ->
                        products.findById(entry.getKey()).orElseThrow().changePrice(entry.getValue())));
                saved += chunk.size();
            } catch (RuntimeException chunkFailed) {
                // cała paczka wycofana — idziemy dalej
            }
        }
        return saved;
    }

    public void changePricesManually(Map<String, BigDecimal> prices) {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            prices.forEach((sku, price) -> products.findById(sku).orElseThrow().changePrice(price));
        } catch (RuntimeException | Error e) {
            transactionManager.rollback(status);
            throw e;
        }
        transactionManager.commit(status);                      // poza try: przy błędzie commit sam wycofuje
    }
}
