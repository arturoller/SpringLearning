package s13_rest_client.rc01_rest_client;

import java.math.BigDecimal;

/**
 * SupplierOffer = oferta dostawcy (supplier = dostawca, offer = oferta): to, co API dostawcy odsyła o jednym produkcie.
 * JSON: {"sku":"ELE-001","name":"Laptop Pro 14","price":5499.99,"available":7}.
 * Cena w złotych jako {@code BigDecimal} (dokładne kwoty — t15_numbers/Numbers01BigDecimal),
 * available = ile sztuk dostawca ma w swoim magazynie.
 * Rekord (niezmienny): Jackson 3 tworzy go przez konstruktor kanoniczny — nazwy pól JSON = nazwy składowych rekordu.
 */
public record SupplierOffer(String sku, String name, BigDecimal price, int available) {
}
