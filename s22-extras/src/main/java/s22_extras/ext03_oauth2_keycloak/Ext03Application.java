package s22_extras.ext03_oauth2_keycloak;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Ext03OAuth2Keycloak. Włącza profil ext03 (application-ext03.properties: issuer-uri Keycloaka).
 *
 * <p>Najpierw uruchom Keycloaka z dziedziną „sklep” (s22-extras/compose/ext03/compose.yaml — opis w pliku), potem
 * ▶ przy main i zapytania z s22-extras/http/ext03_oauth2_keycloak.http (najpierw zapytanie po token).</p>
 *
 * <p>To JEDYNA lekcja modułu bez łańcucha permitAll — tu zabezpieczenia są tematem (Ext03SecurityConfig).</p>
 */
@SpringBootApplication
public class Ext03Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Ext03Application.class);
        application.setAdditionalProfiles("ext03");
        application.run(args);
    }
}
