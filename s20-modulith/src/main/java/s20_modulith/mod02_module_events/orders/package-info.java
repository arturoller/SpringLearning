/**
 * Moduł aplikacji {@code orders} (zamówienia) lekcji Mod02ModuleEvents. Moduł bazowy: nie zależy od żadnego innego
 * modułu ({@code allowedDependencies = {}}) — o tym, co dzieje się po złożeniu zamówienia, informuje zdarzeniem
 * {@code OrderPlaced}.
 */
@ApplicationModule(displayName = "Zamówienia", allowedDependencies = {})
package s20_modulith.mod02_module_events.orders;

import org.springframework.modulith.ApplicationModule;
