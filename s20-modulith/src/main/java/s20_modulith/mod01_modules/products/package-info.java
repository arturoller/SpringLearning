/**
 * Moduł aplikacji {@code products} (produkty) lekcji Mod01Modules. Pakiet bazowy = API modułu ({@code ProductCatalog},
 * {@code ProductView}); podpakiet {@code internal} = szczegóły ukryte przed innymi modułami.
 *
 * <p>{@code @ApplicationModule} (moduł aplikacji) na pakiecie: {@code displayName} (nazwa wyświetlana) trafia do
 * opisu modułów i dokumentacji, a {@code allowedDependencies = {}} (dozwolone zależności: żadne) mówi, że produkty
 * są modułem bazowym — nie wolno im zależeć od żadnego innego modułu.</p>
 */
@ApplicationModule(displayName = "Produkty", allowedDependencies = {})
package s20_modulith.mod01_modules.products;

import org.springframework.modulith.ApplicationModule;
