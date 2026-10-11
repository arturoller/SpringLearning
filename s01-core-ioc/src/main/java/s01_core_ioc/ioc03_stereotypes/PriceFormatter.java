package s01_core_ioc.ioc03_stereotypes;

import org.springframework.stereotype.Component;

/** Zwykły {@code @Component} — klasa pomocnicza, która nie pasuje do żadnej warstwy. Nazwa: "priceFormatter". */
@Component
public class PriceFormatter {

    /** 2999 → "29,99 zł". */
    public String format(int grosze) {
        return "%d,%02d zł".formatted(grosze / 100, grosze % 100);
    }
}
