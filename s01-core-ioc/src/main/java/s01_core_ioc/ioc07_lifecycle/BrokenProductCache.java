package s01_core_ioc.ioc07_lifecycle;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * ZEPSUTY cache produktów (cache = pamięć podręczna). Klasa celowo NIE ma {@code @Component} — test rejestruje ją
 * ręcznie, żeby błąd nie psuł aplikacji lekcji. Błąd: konstruktor używa katalogu, który jest wstrzykiwany do POLA,
 * czyli dopiero PO konstruktorze. W konstruktorze pole {@code catalog} ma jeszcze wartość null.
 */
public class BrokenProductCache {

    @Autowired
    private ProductCatalog catalog;

    private final List<String> products = new ArrayList<>();

    public BrokenProductCache() {
        products.addAll(catalog.names()); // NullPointerException: catalog == null
    }

    public int size() {
        return products.size();
    }
}
