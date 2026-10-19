package s22_extras.ext06_native_image;

import java.math.BigDecimal;

/**
 * Promocja w JSON-ie (lekcja Ext06NativeImage, sekcja 4). Jackson tworzy i czyta rekord REFLEKSJĄ — gdy rekord nie
 * jest typem parametru/wyniku kontrolera (Spring wtedy sam to wykrywa), trzeba to zgłosić:
 * {@code @RegisterReflectionForBinding(PromotionDto.class)}.
 */
public record PromotionDto(String description, BigDecimal priceBefore, BigDecimal priceAfter) {
}
