package s21_capstone.cap01_shop_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa projektu Cap01ShopApi. Włącza profil „cap01” (application-cap01.properties: baza jdbc:h2:mem:cap01,
 * migracje z db/migration/cap01, ddl-auto=validate, open-in-view=false). Skanuje tylko pakiet cap01_shop_api
 * i jego podpakiety (product, customer, order, common). Scenariusz zapytań: s21-capstone/http/cap01_shop_api.http.
 */
@SpringBootApplication
public class Cap01Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Cap01Application.class);
        app.setAdditionalProfiles("cap01");
        app.run(args);
    }
}
