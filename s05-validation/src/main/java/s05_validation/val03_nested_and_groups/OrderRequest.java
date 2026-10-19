package s05_validation.val03_nested_and_groups;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * OrderRequest = zamówienie z zagnieżdżonym adresem i listą pozycji. Kaskada (cascade = kaskada, przejście walidacji
 * „w głąb”) włącza się adnotacją {@code @Valid} na polu: bez niej walidator zatrzyma się na samym polu i nie zajrzy
 * do środka obiektu.
 */
public record OrderRequest(
        @NotBlank @Email String customerEmail,
        @NotNull @Valid ShippingAddress address,                  // kaskada do pól adresu
        @NotEmpty List<@Valid OrderLine> lines,                   // kaskada do KAŻDEGO elementu listy
        List<@NotBlank(message = "Uwaga nie może być pusta") String> notes) {   // ograniczenie na elementach listy
}
