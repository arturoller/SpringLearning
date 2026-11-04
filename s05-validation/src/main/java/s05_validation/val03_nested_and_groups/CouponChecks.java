package s05_validation.val03_nested_and_groups;

import jakarta.validation.GroupSequence;

/** Grupy do {@link CouponForm}: Basic (tanie), Advanced (drogie) i InOrder = sekwencja "najpierw Basic, potem Advanced". */
public final class CouponChecks {

    private CouponChecks() {
    }

    public interface Basic {
    }

    public interface Advanced {
    }

    /** Jeśli grupa Basic znajdzie choć jeden błąd, grupa Advanced NIE zostanie w ogóle sprawdzona. */
    @GroupSequence({Basic.class, Advanced.class})
    public interface InOrder {
    }
}
