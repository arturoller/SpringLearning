package s01_core_ioc.ioc06_scopes;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * POPRAWNY serwis koszyków. Zamiast koszyka dostaje {@code ObjectProvider<ShoppingCart>} (dostawca obiektów —
 * "automat", z którego pobierasz bean, kiedy chcesz). Każde {@code getObject()} (daj obiekt) pyta kontener o bean,
 * a dla prototype kontener tworzy wtedy NOWY koszyk.
 */
@Service
public class CartService {

    private final ObjectProvider<ShoppingCart> cartProvider;

    public CartService(ObjectProvider<ShoppingCart> cartProvider) {
        this.cartProvider = cartProvider;
    }

    /** Tworzy nowy koszyk, dodaje do niego produkt i zwraca liczbę produktów (zawsze 1). */
    public int addAndCount(String product) {
        ShoppingCart cart = cartProvider.getObject();
        cart.add(product);
        return cart.size();
    }

    /** Nowy, pusty koszyk przy każdym wywołaniu. */
    public ShoppingCart newCart() {
        return cartProvider.getObject();
    }
}
