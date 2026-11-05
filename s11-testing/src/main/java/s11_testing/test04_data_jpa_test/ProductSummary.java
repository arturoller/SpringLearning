package s11_testing.test04_data_jpa_test;

import java.math.BigDecimal;

/** Projekcja interfejsowa (projection = rzut): zapytanie pobiera tylko nazwę i cenę, bez całej encji. */
public interface ProductSummary {

    String getName();

    BigDecimal getPrice();
}
