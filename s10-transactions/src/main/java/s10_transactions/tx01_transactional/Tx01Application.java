package s10_transactions.tx01_transactional;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Tx01Transactional. Włącza profil „tx01” (plik application-tx01.properties: własna baza H2
 * jdbc:h2:mem:tx01, migracje Flyway z db/migration/tx01, logi DEBUG menedżera transakcji). Skanuje TYLKO ten pakiet,
 * więc widzi wyłącznie encje i beany tej lekcji. Uruchomiona tworzy tabelę, wypisuje log i kończy działanie —
 * moduł nie ma serwera WWW.
 */
@SpringBootApplication
public class Tx01Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Tx01Application.class);
        app.setAdditionalProfiles("tx01");
        app.run(args);
    }
}
