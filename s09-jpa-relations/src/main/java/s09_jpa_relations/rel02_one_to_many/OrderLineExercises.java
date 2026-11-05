package s09_jpa_relations.rel02_one_to_many;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * ĆWICZENIA lekcji Rel02OneToMany (testy: Rel02OneToManyExercisesTest — uruchamiasz sam ▶).
 * Zastąp każde {@code throw} własnym kodem; zapytania dopisz w {@link ExerciseOrderLineRepository}.
 * Dane: migracja db/migration/rel02/V2 (ZAM-001: Klawiatura ×1, Mysz ×2; ZAM-002: Monitor ×1, Mysz ×1, Kubek ×4;
 * ZAM-003: puste). Test działa w transakcji, więc leniwe listy możesz czytać bez {@code @Transactional}.
 */
@Service // service = usługa; bean (ziarno — obiekt zarządzany przez Springa) warstwy logiki
public class OrderLineExercises {

    private final ShopOrderRepository orders;
    private final ProductRepository products;
    private final ExerciseOrderLineRepository lines;

    public OrderLineExercises(ShopOrderRepository orders, ProductRepository products, ExerciseOrderLineRepository lines) {
        this.orders = orders;
        this.products = products;
        this.lines = lines;
    }

    /**
     * ĆWICZENIE 1 (łatwe): dodaj do zamówienia pozycję z produktem i ilością; cena jednostkowa = aktualna cena produktu.
     * Nie wywołuj save pozycji — ma ją zapisać kaskada.
     * Podpowiedź: sekcja 1 i 3 — {@code findById} zamówienia i produktu, potem {@code order.addLine(product, quantity)}.
     */
    public OrderLine addProduct(Long orderId, Long productId, int quantity) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): usuń z zamówienia pozycję z danym produktem (jeśli jej nie ma — nic nie rób).
     * Warunek testu: przy flush idzie {@code delete from order_line where id=?}.
     * Podpowiedź: sekcja 3 — znajdź pozycję w {@code order.getLines()} po {@code getProduct().getId()} i wywołaj
     * {@code order.removeLine(line)} (orphanRemoval zrobi DELETE). Uwaga: nie usuwaj z listy w trakcie pętli for-each
     * (ConcurrentModificationException — t12_collections/Collections03IterationModification).
     */
    public void removeProduct(Long orderId, Long productId) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): wartość zamówienia JEDNYM zapytaniem, bez wczytywania encji (ZAM-002 → 1056.99,
     * puste ZAM-003 → 0, nie null).
     * Podpowiedź: sekcja 5 — {@code select coalesce(sum(l.quantity * l.unitPrice), 0) from OrderLine l where …}.
     */
    public BigDecimal orderTotal(Long orderId) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): łączna sprzedana ilość każdego produktu JEDNYM zapytaniem, jako mapa posortowana po
     * nazwie: {Klawiatura=1, Kubek=4, Monitor=1, Mysz=3}.
     * Podpowiedź: {@code select l.product.name, sum(l.quantity) from OrderLine l group by l.product.name} z wynikiem
     * {@code List<ProductQuantity>} (sum pola int to Long — s08_data_jpa/Jpa04CustomQueries, sekcja 5) i
     * {@code Collectors.toMap(…, …, (a, b) -> a, TreeMap::new)}.
     */
    public Map<String, Long> quantitySoldByProduct() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): skopiuj zamówienie pod nowym numerem — te same produkty, ilości i ceny jednostkowe
     * (cena z pozycji źródłowej, nie aktualna cena produktu!). Jedno {@code orders.save(copy)}: pozycje zapisze kaskada.
     * Podpowiedź: {@code new ShopOrder(number)}, potem dla każdej pozycji źródła
     * {@code copy.addLine(new OrderLine(line.getProduct(), line.getQuantity(), line.getUnitPrice()))}.
     */
    public ShopOrder copyOrder(Long sourceOrderId, String newOrderNumber) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }
}
