package s22_extras.ext06_native_image;

import java.math.BigDecimal;

/** Promocja: 20 zł mniej (ale nie poniżej zera). Publiczny konstruktor bez argumentów — tworzona refleksją. */
public class FixedDiscountRule implements PromotionRule {

    private static final BigDecimal DISCOUNT = new BigDecimal("20.00");

    @Override
    public String description() {
        return "20 zł mniej";
    }

    @Override
    public BigDecimal apply(BigDecimal price) {
        return price.subtract(DISCOUNT).max(BigDecimal.ZERO);
    }
}
