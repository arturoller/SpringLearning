package s01_core_ioc.ioc03_stereotypes;

import org.springframework.stereotype.Component;

/** Kalkulator VAT z JAWNIE podaną nazwą beana: "taxCalculator" (zamiast domyślnej "vatCalculator"). */
@Component("taxCalculator")
public class VatCalculator {

    /** Kwota brutto w groszach przy stawce 23%, zaokrąglona do pełnego grosza. */
    public long gross(long netGrosze) {
        return Math.round(netGrosze * 1.23);
    }
}
