package s10_transactions.tx05_programmatic_transactions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Tx05ProgrammaticTransactions. Włącza profil „tx05” (własna baza H2 jdbc:h2:mem:tx05,
 * migracje z db/migration/tx05, logi DEBUG menedżera transakcji). Skanuje tylko ten pakiet. Uruchomiona tworzy
 * tabele, wypisuje log i kończy działanie.
 */
@SpringBootApplication
public class Tx05Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Tx05Application.class);
        app.setAdditionalProfiles("tx05");
        app.run(args);
    }
}
