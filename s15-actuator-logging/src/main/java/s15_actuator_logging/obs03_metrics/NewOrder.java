package s15_actuator_logging.obs03_metrics;

/**
 * Dane nowego zamówienia (rekord). {@code channel} to kanał sprzedaży: "WEB" albo "SKLEP" — mała, z góry znana
 * liczba wartości, więc nadaje się na tag metryki (patrz PUŁAPKA w Obs03Metrics).
 */
public record NewOrder(String sku, int quantity, String channel) {
}
