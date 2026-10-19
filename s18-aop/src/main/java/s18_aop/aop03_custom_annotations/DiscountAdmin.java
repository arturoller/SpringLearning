package s18_aop.aop03_custom_annotations;

/**
 * Panel rabatów (do ćwiczenia 1). Nadanie rabatu wymaga roli KIEROWNIK, przegląd rabatów — nie.
 * Bez {@code @Service}: test ćwiczenia sam zakłada na niego aspekt (AspectJProxyFactory).
 */
public class DiscountAdmin {

    @RequiresRole("KIEROWNIK")
    public String grantDiscount(String customerName, int percent) {
        return "Rabat " + percent + "% dla " + customerName;
    }

    public String showDiscounts() {
        return "Rabaty: Jan Kowalski 10%, Zofia Krawczyk 10%";
    }
}
