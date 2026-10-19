package s05_validation.val03_nested_and_groups;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * OrderRequestNoCascade = to samo zamówienie, ale BEZ {@code @Valid} na polach zagnieżdżonych — pokazuje pułapkę:
 * adres i pozycje mają własne ograniczenia, lecz nikt do nich nie zagląda.
 */
public record OrderRequestNoCascade(
        @NotBlank @Email String customerEmail,
        @NotNull ShippingAddress address,
        @NotEmpty List<OrderLine> lines,
        List<String> notes) {
}
