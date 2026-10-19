package s10_transactions.tx04_read_only_isolation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test sekcji 6 lekcji Tx04ReadOnlyIsolation: domyślny limit czasu dla WSZYSTKICH transakcji z właściwości Boota
 * {@code spring.transaction.default-timeout}. Osobna klasa, bo potrzebuje innych właściwości (a więc innego
 * kontekstu) i własnej bazy (jdbc:h2:mem:tx04timeout).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tx04timeout",
        "spring.transaction.default-timeout=7s"
})
@ActiveProfiles("tx04")
class Tx04ReadOnlyIsolationDefaultTimeoutTest {

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("6. spring.transaction.default-timeout=7s: menedżer transakcji dostaje domyślny limit 7 s")
    void defaultTimeoutFromBootProperty() {
        assertThat(((AbstractPlatformTransactionManager) transactionManager).getDefaultTimeout()).isEqualTo(7);

        Integer queryTimeout = new TransactionTemplate(transactionManager).execute(status ->
                jdbcTemplate.execute((StatementCallback<Integer>) Statement::getQueryTimeout));
        assertThat(queryTimeout).as("transakcja BEZ własnego timeout dostała domyślny").isBetween(1, 7);
    }
}
