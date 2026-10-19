package s10_transactions.tx04_read_only_isolation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Tx04ReadOnlyIsolation. Włącza profil „tx04” (własna baza H2 jdbc:h2:mem:tx04, migracje
 * z db/migration/tx04, logi DEBUG menedżera transakcji — w nich widać poziom izolacji, readOnly i timeout każdej
 * nowej transakcji). Skanuje tylko ten pakiet. Uruchomiona tworzy tabele, wypisuje log i kończy działanie.
 */
@SpringBootApplication
public class Tx04Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Tx04Application.class);
        app.setAdditionalProfiles("tx04");
        app.run(args);
    }
}
