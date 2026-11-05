package s09_jpa_relations.rel04_lazy_loading;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * Serwis odczytu zamówień lekcji Rel04LazyLoading — sposób (a) z sekcji 3: wszystko, czego potrzeba z leniwych
 * relacji, czytamy WEWNĄTRZ transakcji serwisu, a na zewnątrz oddajemy gotowy wynik.
 *
 * <p>{@code @Transactional(readOnly = true)} (transactional = transakcyjny; read only = tylko do odczytu) na klasie:
 * każda publiczna metoda działa w transakcji, a z nią w JEDNEJ sesji Hibernate (EntityManager) — leniwe relacje
 * mogą się dociągać aż do końca metody. readOnly dodatkowo mówi Hibernate, że nic nie zmieniamy: nie robi on
 * zrzutu stanu encji do porównań (dirty checking — wykrywanie zmian) ani flush przed zatwierdzeniem.</p>
 */
@Service // service = usługa; bean (ziarno — obiekt zarządzany przez Springa) warstwy logiki
@Transactional(readOnly = true)
public class OrderReadService {

    private final ShopOrderRepository orders;

    public OrderReadService(ShopOrderRepository orders) {
        this.orders = orders;
    }

    /**
     * Opis zamówienia złożony W TRANSAKCJI, np. {@code "ZAM-001 | Anna Nowak | Klawiatura ×1, Mysz ×2"}.
     * Leniwe relacje dociągają się po kolei (5 zapytań: zamówienie, pozycje, 2 produkty, klient — test), ale
     * działają, bo sesja jest otwarta do końca metody. Na zewnątrz wychodzi String — nic leniwego.
     */
    public String describe(Long orderId) {
        ShopOrder order = orders.findById(orderId).orElseThrow();
        String lines = order.getLines().stream()
                .map(line -> line.getProduct().getName() + " ×" + line.getQuantity())
                .collect(Collectors.joining(", "));
        return order.getOrderNumber() + " | " + order.getCustomer().getName() + " | " + lines;
    }

    /**
     * Sekcja 5: zwraca ENCJĘ z listą pozycji zainicjalizowaną przez {@code Hibernate.initialize} — poza transakcją
     * lista działa. PUŁAPKA: initialize dociąga TYLKO wskazany obiekt. Produkty pozycji nadal są proxy, więc
     * {@code line.getProduct().getName()} poza transakcją rzuci LazyInitializationException (test).
     */
    public ShopOrder findWithLinesInitialized(Long orderId) {
        ShopOrder order = orders.findById(orderId).orElseThrow();
        Hibernate.initialize(order.getLines()); // initialize = zainicjalizuj: SELECT pozycji teraz, w transakcji
        return order;
    }
}
