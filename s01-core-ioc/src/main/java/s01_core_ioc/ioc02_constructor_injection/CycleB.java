package s01_core_ioc.ioc02_constructor_injection;

/** B potrzebuje A w konstruktorze — drugi koniec cyklu. Celowo BEZ {@code @Component}. */
public class CycleB {

    public CycleB(CycleA a) {
    }
}
