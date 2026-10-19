/**
 * Moduł aplikacji {@code orders} (zamówienia) lekcji Mod01Modules. Zależy od API modułów {@code products}
 * i {@code customers} — i tylko od nich: {@code allowedDependencies} wypisuje dozwolone moduły (lista dozwolonych,
 * whitelist). Nowa zależność (np. od modułu płatności) wymaga świadomej zmiany tej listy.
 */
@ApplicationModule(displayName = "Zamówienia", allowedDependencies = {"products", "customers"})
package s20_modulith.mod01_modules.orders;

import org.springframework.modulith.ApplicationModule;
