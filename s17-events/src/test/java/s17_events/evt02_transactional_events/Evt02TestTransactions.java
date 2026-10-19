package s17_events.evt02_transactional_events;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * Pomocnik testów ćwiczeń: mały kontekst Springa z prawdziwą bazą H2 (bez tabel), menedżerem transakcji
 * i {@code TransactionTemplate}. {@code @EnableTransactionManagement} rejestruje też obsługę
 * {@code @TransactionalEventListener}. Klasa celowo NIE ma {@code @Configuration}, żeby skanowanie pakietu lekcji
 * (testy {@code @SpringBootTest}) jej nie podchwyciło — tworzymy z niej kontekst ręcznie.
 */
@EnableTransactionManagement
public class Evt02TestTransactions {

    @Bean
    DataSource dataSource() {
        return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
    }

    @Bean
    PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    /** Tworzy kontekst z tą konfiguracją oraz podanymi klasami (słuchaczami). */
    public static AnnotationConfigApplicationContext contextWith(Class<?>... listeners) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(Evt02TestTransactions.class);
        context.register(listeners);
        context.refresh();
        return context;
    }
}
