package s13_rest_client.rc03_http_interfaces;

import java.math.BigDecimal;

/**
 * SupplierOffer = oferta dostawcy (supplier = dostawca, offer = oferta) — własna kopia lekcji Rc03 (lekcje nie dzielą klas).
 * JSON: {"sku":"ELE-001","name":"Laptop Pro 14","price":5499.99,"available":7}.
 * Cena w złotych jako {@code BigDecimal} (dokładne kwoty — t15_numbers/Numbers01BigDecimal),
 * available = ile sztuk dostawca ma w swoim magazynie.
 * Rekord (niezmienny): Jackson 3 tworzy go przez konstruktor kanoniczny — nazwy pól JSON = nazwy składowych rekordu.
 */
public record SupplierOffer(String sku, String name, BigDecimal price, int available) {
}
