package s21_capstone.cap01_shop_api.order;

import java.util.EnumSet;
import java.util.Set;

/**
 * Status zamówienia i DOZWOLONE przejścia — mała maszyna stanów (state machine) w jednym miejscu:
 * <pre>
 *   NOWE ──→ OPLACONE ──→ WYSLANE ──→ DOSTARCZONE
 *     │          │
 *     └──────────┴──→ ANULOWANE          (DOSTARCZONE i ANULOWANE to stany końcowe)
 * </pre>
 * Reguły siedzą w enumie, a nie w kontrolerze ani w kilku if-ach rozsianych po serwisie: jedno miejsce do czytania,
 * jeden test jednostkowy (Cap01ShopApiUnitTest) i API, które może pokazać klientowi „co dalej wolno”.
 */
public enum OrderStatus {
    NOWE, OPLACONE, WYSLANE, DOSTARCZONE, ANULOWANE;

    /**
     * Statusy, na które wolno przejść z tego. Metoda ze {@code switch}, a nie pole ustawiane w konstruktorze stałej:
     * {@code NOWE(EnumSet.of(OPLACONE))} się nie skompiluje — stała OPLACONE jest zadeklarowana PÓŹNIEJ
     * (illegal forward reference — niedozwolone odwołanie w przód).
     */
    public Set<OrderStatus> allowedNext() {
        return switch (this) {
            case NOWE -> EnumSet.of(OPLACONE, ANULOWANE);
            case OPLACONE -> EnumSet.of(WYSLANE, ANULOWANE);
            case WYSLANE -> EnumSet.of(DOSTARCZONE);
            case DOSTARCZONE, ANULOWANE -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canChangeTo(OrderStatus next) {
        return allowedNext().contains(next);
    }
}
