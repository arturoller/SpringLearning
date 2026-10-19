package s01_core_ioc.ioc01_container;

import java.util.Map;

/**
 * ManualOrderCalculator = kalkulator „ręczny” — wersja PRZED: sam tworzy swoją zależność przez new.
 * Działa, ale jest na sztywno związany z konkretną klasą PriceList: nie da się podać innego cennika (np. promocyjnego
 * albo testowego) bez zmiany kodu tej klasy. Nie ma adnotacji, więc skanowanie Ioc01Application go pomija — w tej
 * aplikacji to NIE jest bean (beanem stałby się dopiero po jawnej rejestracji, patrz sekcja 3 lekcji).
 */
public class ManualOrderCalculator {

    private final PriceList priceList = new PriceList();   // „sam sobie robię” — tu leży problem

    public int total(Map<String, Integer> items) {
        return items.entrySet().stream()
                .mapToInt(item -> priceList.priceOf(item.getKey()) * item.getValue())
                .sum();
    }
}
