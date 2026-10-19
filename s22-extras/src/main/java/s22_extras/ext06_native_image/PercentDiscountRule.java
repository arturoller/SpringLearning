package s22_extras.ext06_native_image;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Promocja: 10% taniej. Publiczny konstruktor bez argumentów — tworzona refleksją (PromotionLoader). */
public class PercentDiscountRule implements PromotionRule {

    @Override
    public String description() {
        return "10% taniej";
    }

    @Override
    public BigDecimal apply(BigDecimal price) {
        return price.multiply(new BigDecimal("0.90")).setScale(2, RoundingMode.HALF_UP);
    }
}
