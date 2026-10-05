package s00_start.start04_testing_intro;

import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * PriceCalculator = kalkulator cen. Serwis liczący cenę brutto (z VAT) z ceny netto. Kwoty w GROSZACH (int), żeby
 * uniknąć błędów zaokrągleń typu double (t15_numbers/Numbers01BigDecimal).
 */
@Service
public class PriceCalculator {

    /** Stawki VAT obowiązujące w Polsce, w procentach. */
    static final Set<Integer> ALLOWED_VAT = Set.of(0, 5, 8, 23);

    /**
     * gross = brutto. Wynik zaokrąglony do najbliższego grosza (połówki w górę): 999 gr netto + 23% = 1228,77 → 1229 gr.
     *
     * @throws IllegalArgumentException gdy cena jest ujemna albo stawka VAT nie istnieje
     */
    public int gross(int netGrosze, int vatPercent) {
        if (netGrosze < 0) {
            throw new IllegalArgumentException("Cena netto nie może być ujemna: " + netGrosze);
        }
        if (!ALLOWED_VAT.contains(vatPercent)) {
            throw new IllegalArgumentException("Nieznana stawka VAT: " + vatPercent);
        }
        return (netGrosze * (100 + vatPercent) + 50) / 100;   // + 50, potem dzielenie całkowite = zaokrąglenie
    }
}
