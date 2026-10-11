package s01_core_ioc.ioc02_constructor_injection;

import org.springframework.beans.factory.annotation.Autowired;

/** Cykl przez POLA: A ma pole B. Celowo BEZ {@code @Component} (rejestrowana w teście). */
public class FieldCycleA {

    @Autowired
    FieldCycleB b;
}
