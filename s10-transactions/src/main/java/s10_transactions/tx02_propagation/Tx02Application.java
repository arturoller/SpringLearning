package s10_transactions.tx02_propagation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Tx02Propagation. Włącza profil „tx02” (własna baza H2 jdbc:h2:mem:tx02, migracje
 * z db/migration/tx02, logi DEBUG menedżera transakcji — w nich widać „Suspending current transaction, creating new
 * transaction” dla REQUIRES_NEW). Skanuje tylko ten pakiet.
 */
@SpringBootApplication
public class Tx02Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Tx02Application.class);
        app.setAdditionalProfiles("tx02");
        app.run(args);
    }
}
