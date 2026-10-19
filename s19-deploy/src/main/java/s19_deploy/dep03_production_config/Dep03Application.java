package s19_deploy.dep03_production_config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Klasa startowa lekcji Dep03ProductionConfig. Uruchomiona z IntelliJ włącza profil „dep03” (lokalne, TESTOWE ustawienia
 * z application-dep03.properties). Na produkcji tego profilu NIE włączasz — ustawienia przychodzą z zewnątrz.
 */
@SpringBootApplication
@EnableConfigurationProperties(PaymentSettings.class)   // rejestruje rekord PaymentSettings jako bean z kluczy shop.payment.*
public class Dep03Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Dep03Application.class);
        application.setAdditionalProfiles("dep03");   // tylko przy uruchomieniu lokalnym (main), nie w testach
        application.run(args);
    }
}
