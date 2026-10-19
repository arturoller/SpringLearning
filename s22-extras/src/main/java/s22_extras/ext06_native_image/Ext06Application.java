package s22_extras.ext06_native_image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

/**
 * Klasa startowa lekcji Ext06NativeImage — aplikacja BEZ serwera WWW (WebApplicationType.NONE): wczytuje promocje,
 * wypisuje je jako JSON i kończy działanie. ▶ przy main (zwykła JVM) albo opis budowy obrazu natywnego w lekcji.
 */
@SpringBootApplication
@ImportRuntimeHints(Ext06RuntimeHints.class)            // podpowiedzi: plik promocji + klasy tworzone refleksją
@RegisterReflectionForBinding(PromotionDto.class)       // podpowiedzi dla Jacksona: konstruktor, pola, akcesory rekordu
public class Ext06Application {

    private static final Logger log = LoggerFactory.getLogger(Ext06Application.class);

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Ext06Application.class);
        application.setWebApplicationType(WebApplicationType.NONE);   // bez Tomcata/Netty — tylko kontekst Springa
        application.run(args);
    }

    /** Po starcie: każda promocja zastosowana do ceny Słuchawek BT (349.90 zł), wypisana jako JSON. */
    @Bean
    ApplicationRunner printPromotions(PromotionLoader loader, JsonMapper jsonMapper) {
        return args -> {
            BigDecimal price = new BigDecimal("349.90");
            for (PromotionRule rule : loader.loadPromotions()) {
                PromotionDto dto = new PromotionDto(rule.description(), price, rule.apply(price));
                log.info("Promocja: {}", jsonMapper.writeValueAsString(dto));
            }
        };
    }
}
