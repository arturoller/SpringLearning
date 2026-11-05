package s05_validation.val02_valid_in_controllers;

import java.math.BigDecimal;

/** Produkt zwracany klientowi (odpowiedź). */
public record Product(long id, String name, BigDecimal price, int quantity) {
}
