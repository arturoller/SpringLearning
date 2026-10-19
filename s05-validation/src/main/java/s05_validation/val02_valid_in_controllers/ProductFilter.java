package s05_validation.val02_valid_in_controllers;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * ProductFilter = filtr wyszukiwania składany z parametrów adresu ({@code ?category=...&page=...}). Rekord bez adnotacji
 * przy parametrze kontrolera jest wiązany jak {@code @ModelAttribute} (atrybut modelu — obiekt z parametrów zapytania).
 */
public record ProductFilter(
        @Size(min = 3, message = "Kategoria: co najmniej 3 znaki") String category,
        @Min(value = 0, message = "Strona nie może być ujemna") @Max(1000) Integer page) {
}
