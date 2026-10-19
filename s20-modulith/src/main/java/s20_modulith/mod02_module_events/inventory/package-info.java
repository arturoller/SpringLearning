/**
 * Moduł aplikacji {@code inventory} (magazyn) lekcji Mod02ModuleEvents. Zależy TYLKO od modułu {@code orders}, i to
 * wyłącznie przez typ zdarzenia {@code OrderPlaced}, którego słucha. Nie wywołuje żadnej metody modułu zamówień.
 */
@ApplicationModule(displayName = "Magazyn", allowedDependencies = "orders")
package s20_modulith.mod02_module_events.inventory;

import org.springframework.modulith.ApplicationModule;
