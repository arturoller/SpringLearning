package s05_validation.val03_nested_and_groups;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * ReturnRequest = zgłoszenie zwrotu towaru — SZKIELET do ćwiczenia 2 lekcji Val03NestedAndGroups.
 * TODO: adres zwrotu ma być podany ORAZ sprawdzany w środku (ShippingAddress), a lista pozycji — niepusta i z każdą
 * pozycją sprawdzaną w środku (OrderLine). Rozwiązanie wzorcowe: solutions.val03_nested_and_groups.
 */
public record ReturnRequest(
        @NotBlank String orderId,
        ShippingAddress address,
        List<OrderLine> lines) {
}
