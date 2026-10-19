package s05_validation.val04_custom_validators;

import java.math.BigDecimal;

/** HasPriceRange = „ma widełki cenowe od–do” (używane w ćwiczeniu 2 lekcji Val04CustomValidators). */
public interface HasPriceRange {

    BigDecimal minPrice();

    BigDecimal maxPrice();
}
