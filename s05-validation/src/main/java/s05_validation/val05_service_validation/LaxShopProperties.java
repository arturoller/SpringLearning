package s05_validation.val05_service_validation;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * PUŁAPKA: te same ograniczenia, ale BEZ {@code @Validated} na klasie. Spring Boot wtedy nie waliduje ustawień
 * (adnotacje wiszą "dla ozdoby"). Zob. test Val05ConfigurationPropertiesTest.
 */
@ConfigurationProperties("val05.lax")
public record LaxShopProperties(@NotBlank String name) {

    /** Rejestracja na potrzeby testu (nie jest stereotypem, więc skanowanie jej nie podejmie). */
    @EnableConfigurationProperties(LaxShopProperties.class)
    public static class Config {
    }
}
