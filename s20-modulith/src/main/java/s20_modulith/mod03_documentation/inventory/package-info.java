/**
 * Moduł aplikacji {@code inventory} (magazyn) lekcji Mod03Documentation. Z modułu {@code orders} wolno mu używać
 * tylko nazwanego interfejsu {@code events}: {@code allowedDependencies = "orders::events"} (moduł::interfejs).
 */
@ApplicationModule(displayName = "Magazyn", allowedDependencies = "orders::events")
package s20_modulith.mod03_documentation.inventory;

import org.springframework.modulith.ApplicationModule;
