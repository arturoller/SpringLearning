package s01_core_ioc.ioc04_bean_methods;

/** Kalkulator cen brutto; zależy od {@link TaxTable}. Metoda {@code taxTable()} pozwala w teście sprawdzić, którą tabelę dostał. */
public class PriceCalculator {

    private final TaxTable taxTable;

    public PriceCalculator(TaxTable taxTable) {
        this.taxTable = taxTable;
    }

    public TaxTable taxTable() {
        return taxTable;
    }

    /** Cena brutto w groszach (zaokrąglenie w dół). */
    public int gross(int netCents) {
        return netCents * (100 + taxTable.vatPercent()) / 100;
    }
}
