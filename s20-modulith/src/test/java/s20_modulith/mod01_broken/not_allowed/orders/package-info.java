/**
 * Moduł orders złej aplikacji z lekcji Mod01Modules: wolno mu zależeć TYLKO od modułu products.
 */
@ApplicationModule(allowedDependencies = "products")
package s20_modulith.mod01_broken.not_allowed.orders;

import org.springframework.modulith.ApplicationModule;
