package s06_error_handling.err05_custom_exceptions;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Zamówienie z ciała zapytania. Lista może być pusta (to sprawdza serwis: 422), ale nie może być brakiem pola (400). */
public record NewOrder(
        @NotBlank(message = "Podaj imię i nazwisko klienta") String customer,
        @NotNull(message = "Brak listy pozycji") List<@Valid OrderLine> lines) {
}
