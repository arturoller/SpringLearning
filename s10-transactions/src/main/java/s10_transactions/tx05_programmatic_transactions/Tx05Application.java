package s10_transactions.tx05_programmatic_transactions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Tx05ProgrammaticTransactions (bez weba). Profil {@code tx05} wczytuje
 * {@code application-tx05.properties}: własną bazę H2 i Flyway z {@code db/migration/tx05}. Boot tworzy tu dwa beany,
 * z których korzysta lekcja: {@code transactionManager} ({@code JpaTransactionManager}) i {@code transactionTemplate}.
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Tx05Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Tx05Application.class);
        app.setAdditionalProfiles("tx05"); // profile = profil ustawień
        app.run(args).close();
    }
}
