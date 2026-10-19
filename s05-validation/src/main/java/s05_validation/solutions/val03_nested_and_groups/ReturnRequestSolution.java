package s05_validation.solutions.val03_nested_and_groups;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import s05_validation.val03_nested_and_groups.OrderLine;
import s05_validation.val03_nested_and_groups.ShippingAddress;

import java.util.List;

/** Rozwiązanie wzorcowe ćwiczenia 2 lekcji Val03NestedAndGroups (ReturnRequest z kaskadą). */
public record ReturnRequestSolution(
        @NotBlank String orderId,
        @NotNull @Valid ShippingAddress address,        // @NotNull — brak adresu; @Valid — sprawdź jego pola
        @NotEmpty List<@Valid OrderLine> lines) {       // @NotEmpty — lista niepusta; @Valid na elemencie — każda pozycja
}
