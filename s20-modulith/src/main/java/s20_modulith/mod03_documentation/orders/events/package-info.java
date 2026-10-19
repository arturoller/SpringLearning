/**
 * Nazwany interfejs {@code events} modułu {@code orders}: zdarzenia, które moduł publikuje dla innych.
 *
 * <p>{@code @NamedInterface} (nazwany interfejs) na pakiecie wystawia ten podpakiet na zewnątrz pod nazwą
 * {@code events}. Bez adnotacji byłby zwykłym podpakietem modułu, czyli częścią wewnętrzną. Inny moduł może
 * pozwolić sobie wyłącznie na ten fragment: {@code allowedDependencies = "orders::events"}.</p>
 */
@NamedInterface("events")
package s20_modulith.mod03_documentation.orders.events;

import org.springframework.modulith.NamedInterface;
