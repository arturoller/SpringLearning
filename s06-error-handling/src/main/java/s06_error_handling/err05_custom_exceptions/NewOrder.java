package s06_error_handling.err05_custom_exceptions;

import jakarta.validation.constraints.Min;

/** Dane nowego zamówienia. Poprawność FORMALNĄ (ilość dodatnia) sprawdza walidacja; reguły biznesowe sprawdza serwis. */
public record NewOrder(
        long productId,
        @Min(value = 1, message = "Ilość musi wynosić co najmniej 1") int quantity) {
}
