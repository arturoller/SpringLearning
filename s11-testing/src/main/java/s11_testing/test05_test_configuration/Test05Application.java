package s11_testing.test05_test_configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;

/**
 * Klasa startowa lekcji Test05TestConfiguration (bez weba i bez bazy w logice). Profil {@code test05} wczytuje
 * {@code application-test05.properties} (nazwa sklepu, adres usługi cen, newsletter). Testy lekcji znajdują tę klasę
 * same (szukają {@code @SpringBootConfiguration} w górę pakietów).
 * <p>
 * Uruchomienie (▶ obok {@code main}) wypisuje nazwę sklepu i cenę po promocji według DZISIEJSZEJ daty — w testach
 * tę datę ustala zegar testowy.
 */
@SpringBootApplication
public class Test05Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Test05Application.class);
        app.setAdditionalProfiles("test05");
        app.setWebApplicationType(WebApplicationType.NONE);
        try (ConfigurableApplicationContext context = app.run(args)) {
            ShopInfo shop = context.getBean(ShopInfo.class);
            PromotionService promotions = context.getBean(PromotionService.class);
            System.out.println(shop.name() + ": Kawa 24.90 -> " + promotions.finalPrice(new BigDecimal("24.90"))
                    + (promotions.isBlackWeek() ? " (Black Week!)" : " (bez promocji)"));
        }
    }
}
