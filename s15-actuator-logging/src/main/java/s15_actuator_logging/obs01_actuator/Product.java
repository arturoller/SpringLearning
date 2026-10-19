package s15_actuator_logging.obs01_actuator;

import java.math.BigDecimal;

/** Produkt sklepu (rekord = niezmienna klasa danych) — mały model na potrzeby lekcji Obs01Actuator. */
public record Product(String sku, String name, String category, BigDecimal price, int stock) {
}
