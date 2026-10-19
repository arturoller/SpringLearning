package s04_web_rest.rest04_json;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.MapperFeature;

/**
 * JsonConfig = konfiguracja Jacksona w tej lekcji. NIE tworzymy własnego JsonMappera — tylko DOSTRAJAMY ten, który
 * buduje Boot, przez {@code JsonMapperBuilderCustomizer} (customizer = „dostrajacz”; Boot 4, pakiet
 * org.springframework.boot.jackson.autoconfigure). W Boot 3 ten sam pomysł nazywał się
 * Jackson2ObjectMapperBuilderCustomizer.
 */
@Configuration
public class JsonConfig {

    /**
     * Enumy w JSON-ie bez rozróżniania wielkości liter: "ksiazki", "Ksiazki" i "KSIAZKI" → Category.KSIAZKI.
     * To samo da się zrobić właściwością: spring.jackson.mapper.accept-case-insensitive-enums=true.
     */
    @Bean
    JsonMapperBuilderCustomizer caseInsensitiveEnums() {
        return builder -> builder.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    }
}
