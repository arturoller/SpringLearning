package s05_validation.val03_nested_and_groups;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** ShippingAddress = adres dostawy (obiekt ZAGNIEŻDŻONY w zamówieniu). */
public record ShippingAddress(
        @NotBlank(message = "Ulica jest wymagana") String street,
        @NotBlank(message = "Miasto jest wymagane") String city,
        @Pattern(regexp = "[0-9]{2}-[0-9]{3}", message = "Kod pocztowy ma format 00-000") String zip) {
}
