package s10_transactions.tx04_pitfalls.validate_existing;

import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

/**
 * Dodatkowa konfiguracja do Tx04SpecialSetupTest (sekcja 5 lekcji Tx04ReadOnlyIsolation). Leży w OSOBNYM pakiecie
 * testów, żeby skanowanie Tx04Application jej nie znalazło — dołączamy ją jawnie tylko w jednym teście.
 *
 * <p>{@code TransactionManagerCustomizer} (dostrajacz menedżera transakcji; w Boot 4 w pakiecie
 * {@code org.springframework.boot.transaction.autoconfigure}) — Boot wywołuje go na autokonfigurowanym
 * {@code JpaTransactionManager}. Włączamy {@code validateExistingTransaction}: dołączenie do trwającej transakcji
 * z niezgodną izolacją albo readOnly ma rzucać wyjątek zamiast po cichu ignorować ustawienia.</p>
 */
@Configuration(proxyBeanMethods = false)
public class ValidatingTransactionsConfig {

    @Bean
    TransactionManagerCustomizer<AbstractPlatformTransactionManager> validateExistingTransaction() {
        return transactionManager -> transactionManager.setValidateExistingTransaction(true);
    }
}
