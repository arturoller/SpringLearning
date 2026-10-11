package s01_core_ioc.ioc06_scopes;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ShoppingCart = koszyk na zakupy. Każdy klient powinien mieć WŁASNY koszyk, więc bean ma zasięg prototype
 * (prototyp — nowy obiekt przy każdym pobraniu z kontenera). {@code @Scope} (zasięg) mówi kontenerowi, ile
 * instancji ma utworzyć i jak długo je trzymać. Stała {@code SCOPE_PROTOTYPE} to po prostu tekst "prototype".
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ShoppingCart {

    private final List<String> products = new ArrayList<>();

    /** Dodaje produkt do koszyka. */
    public void add(String product) {
        products.add(product);
    }

    /** Liczba produktów w koszyku. */
    public int size() {
        return products.size();
    }

    /** Kopia zawartości koszyka (żeby nikt z zewnątrz nie zmieniał listy). */
    public List<String> products() {
        return List.copyOf(products);
    }
}
