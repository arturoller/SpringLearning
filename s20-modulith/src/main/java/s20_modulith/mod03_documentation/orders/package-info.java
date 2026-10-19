/**
 * Moduł aplikacji {@code orders} (zamówienia) lekcji Mod03Documentation. Ma dwa interfejsy: nienazwany (pakiet
 * bazowy: {@code OrderManagement}) i nazwany {@code events} (podpakiet z {@code @NamedInterface}). Wolno mu
 * zależeć tylko od modułu {@code products}.
 */
@ApplicationModule(displayName = "Zamówienia", allowedDependencies = "products")
package s20_modulith.mod03_documentation.orders;

import org.springframework.modulith.ApplicationModule;
