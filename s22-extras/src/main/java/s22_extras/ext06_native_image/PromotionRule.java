package s22_extras.ext06_native_image;

import java.math.BigDecimal;

/**
 * Reguła promocji (lekcja Ext06NativeImage). Implementacje są wybierane po NAZWIE KLASY z pliku
 * ext06/promocje.txt i tworzone REFLEKSJĄ — dokładnie tego obraz natywny „nie widzi” bez podpowiedzi.
 */
public interface PromotionRule {

    /** Opis do wyświetlenia klientowi. */
    String description();

    /** Cena po promocji (w złotych). */
    BigDecimal apply(BigDecimal price);
}
