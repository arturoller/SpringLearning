package s01_core_ioc.ioc02_constructor_injection;

import org.springframework.beans.factory.annotation.Autowired;

/** Cykl przez POLA: B ma pole A. Celowo BEZ {@code @Component}. */
public class FieldCycleB {

    @Autowired
    FieldCycleA a;
}
