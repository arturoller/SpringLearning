package s05_validation.val05_service_validation;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** ShopPropertiesUnchecked = reguły są, ale brakuje {@code @Validated} na klasie — nic nie jest sprawdzane (sekcja 4). */
@ConfigurationProperties("val05.unchecked")
public record ShopPropertiesUnchecked(@NotBlank String name) {
}
