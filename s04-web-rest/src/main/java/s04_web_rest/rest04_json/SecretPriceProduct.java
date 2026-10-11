package s04_web_rest.rest04_json;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * SecretPriceProduct = produkt z tajną ceną — ZŁY PRZYKŁAD do PUŁAPKI w sekcji 2 lekcji Rest04Json.
 * {@code @JsonIgnore} na składniku rekordu typu PROSTEGO (int): przy zapisie pole znika z JSON-a (dobrze), ale przy
 * ODCZYCIE Jackson nie ma skąd wziąć wartości, wstawia null — a null do int w Jacksonie 3 to błąd
 * (FAIL_ON_NULL_FOR_PRIMITIVES — „błąd przy null dla typów prostych” — jest domyślnie włączone).
 */
public record SecretPriceProduct(String name, @JsonIgnore int purchasePriceGrosze) {
}
