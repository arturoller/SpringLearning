package s10_transactions.tx02_propagation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.support.JdbcTransactionManager;

import javax.sql.DataSource;

/**
 * Klasa startowa lekcji Tx02Propagation (bez weba). Profil {@code tx02} wczytuje {@code application-tx02.properties}.
 *
 * <p>Drugi menedżer transakcji {@code jdbcTransactionManager} służy WYŁĄCZNIE sekcji 4 (NESTED — savepointy).
 * {@code defaultCandidate = false} (domyślny kandydat = nie): bean nie jest brany pod uwagę przy zwykłym wstrzykiwaniu
 * po typie, więc (sprawdzone testem) Boot dalej tworzy swój {@code JpaTransactionManager}, a {@code @Transactional}
 * bez atrybutu {@code transactionManager} używa właśnie jego. Do tego beana trzeba się odwołać po nazwie:
 * {@code @Transactional(transactionManager = "jdbcTransactionManager")}.</p>
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Tx02Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Tx02Application.class);
        app.setAdditionalProfiles("tx02"); // profile = profil ustawień
        app.run(args).close();
    }

    /** Menedżer transakcji czystego JDBC — w przeciwieństwie do JpaTransactionManager obsługuje NESTED (savepointy). */
    @Bean(defaultCandidate = false)
    JdbcTransactionManager jdbcTransactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }
}
