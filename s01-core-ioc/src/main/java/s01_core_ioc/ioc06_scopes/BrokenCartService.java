package s01_core_ioc.ioc06_scopes;

import org.springframework.stereotype.Service;

/**
 * ZEPSUTY serwis koszyków: singleton (domyślny zasięg) dostaje prototype przez konstruktor. Konstruktor wywołuje się
 * RAZ, więc koszyk jest wstrzyknięty RAZ — i ten sam koszyk obsługuje wszystkich klientów aż do końca życia kontekstu.
 * Zasięg prototype nie pomaga: nikt już nie prosi kontenera o nowy obiekt.
 */
@Service
public class BrokenCartService {

    private final ShoppingCart cart;

    public BrokenCartService(ShoppingCart cart) {
        this.cart = cart;
    }

    /** Dodaje produkt do "koszyka klienta" i zwraca liczbę produktów w koszyku. */
    public int addAndCount(String product) {
        cart.add(product);
        return cart.size();
    }

    /** Koszyk, którego serwis używa (zawsze ten sam obiekt). */
    public ShoppingCart cart() {
        return cart;
    }
}
