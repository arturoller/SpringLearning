package s10_transactions.tx03_proxy_pitfalls;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Tx03ProxyPitfalls. Włącza profil „tx03” (własna baza H2 jdbc:h2:mem:tx03, migracje
 * z db/migration/tx03, logi DEBUG menedżera transakcji). Skanuje tylko ten pakiet. Klasa FinalPriceService celowo
 * NIE jest beanem — gdyby była, aplikacja by nie wystartowała (sekcja 3 lekcji).
 */
@SpringBootApplication
public class Tx03Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Tx03Application.class);
        app.setAdditionalProfiles("tx03");
        app.run(args);
    }
}
