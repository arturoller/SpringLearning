package s00_start.solutions.start04_testing_intro;

import s00_start.start04_testing_intro.PriceCalculator;

import java.util.List;
import java.util.Set;

/** Rozwiązania wzorcowe ćwiczeń lekcji Start04TestingIntro — zajrzyj dopiero po własnej próbie! */
public final class Start04TestingIntroSolutions {

    private static final Set<Integer> ALLOWED_VAT = Set.of(0, 5, 8, 23);
    private static final PriceCalculator CALCULATOR = new PriceCalculator();   // zwykły obiekt — bez Springa

    private Start04TestingIntroSolutions() {
    }

    static boolean solution1(int vatPercent) {
        return ALLOWED_VAT.contains(vatPercent);
    }

    static int solution2(int netGrosze, int vatPercent, int discountPercent) {
        if (discountPercent < 0 || discountPercent > 100) {
            throw new IllegalArgumentException("Rabat musi być w zakresie 0..100: " + discountPercent);
        }
        int discountedNet = (netGrosze * (100 - discountPercent) + 50) / 100;
        return CALCULATOR.gross(discountedNet, vatPercent);
    }

    static int solution3(List<Integer> netPricesGrosze, int vatPercent) {
        return netPricesGrosze.stream()
                .mapToInt(net -> CALCULATOR.gross(net, vatPercent))
                .sum();
    }
}
