package s01_core_ioc.ioc01_container;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * OrderCalculator = kalkulator zamówienia — wersja PO: zależność (PriceList) przychodzi z ZEWNĄTRZ, przez konstruktor.
 * Kalkulator nie wie, kto i jak utworzył cennik. Tworzy go i podaje kontener Springa (albo test — przez new).
 */
@Service
public class OrderCalculator {

    private final PriceList priceList;

    public OrderCalculator(PriceList priceList) {
        this.priceList = priceList;
    }

    /** Suma zamówienia w groszach: produkt → liczba sztuk. */
    public int total(Map<String, Integer> items) {
        return items.entrySet().stream()
                .mapToInt(item -> priceList.priceOf(item.getKey()) * item.getValue())
                .sum();
    }
}
