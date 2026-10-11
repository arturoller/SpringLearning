package s01_core_ioc.ioc02_constructor_injection;

/** A potrzebuje B w konstruktorze. Razem z CycleB tworzy cykl. Celowo BEZ {@code @Component} (rejestrowana w teście). */
public class CycleA {

    public CycleA(CycleB b) {
    }
}
