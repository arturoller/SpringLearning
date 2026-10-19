/**
 * Moduł inventory złej aplikacji z lekcji Mod03Documentation: z modułu orders wolno mu używać tylko interfejsu events.
 */
@ApplicationModule(allowedDependencies = "orders::events")
package s20_modulith.mod03_broken.named_interface.inventory;

import org.springframework.modulith.ApplicationModule;
