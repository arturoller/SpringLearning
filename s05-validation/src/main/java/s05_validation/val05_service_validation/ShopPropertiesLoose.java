package s05_validation.val05_service_validation;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * ShopPropertiesLoose = to samo co ShopProperties, ale pole zagnieżdżone BEZ @Valid — pułapka z sekcji 4: reguły
 * w środku nie są sprawdzane.
 */
@ConfigurationProperties("val05.loose")
@Validated
public record ShopPropertiesLoose(@NotBlank String name, Support support) {

    /** Support = zagnieżdżone ustawienia (reguła @NotBlank nie zadziała bez @Valid przy polu wyżej). */
    public record Support(@NotBlank String email) {
    }
}
