package s15_actuator_logging.obs04_logging;

/** Dane składanego zamówienia: produkt, liczba sztuk, cena sztuki w groszach i e-mail klienta (dana osobowa!). */
public record NewCheckout(String sku, int quantity, long priceGrosze, String email) {
}
