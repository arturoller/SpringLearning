package s09_jpa_relations.rel04_lazy_loading;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ĆWICZENIA lekcji Rel04LazyLoading (testy: Rel04LazyLoadingExercisesTest — uruchamiasz sam ▶).
 * Zastąp każde {@code throw} własnym kodem; zapytania dopisz w {@link ExerciseShopOrderRepository}.
 * UWAGA: testy ćwiczeń działają BEZ transakcji testu ({@code Propagation.NOT_SUPPORTED} — sekcja 2 lekcji), więc
 * każde sięgnięcie po leniwą relację poza transakcją skończy się {@code LazyInitializationException} — tak jak
 * w prawdziwej aplikacji. Dane: migracja db/migration/rel04/V2.
 */
@Service // service = usługa; bean (ziarno — obiekt zarządzany przez Springa) warstwy logiki
public class LazyLoadingExercises {

    private final ExerciseShopOrderRepository orders;
    private final CustomerRepository customers;

    public LazyLoadingExercises(ExerciseShopOrderRepository orders, CustomerRepository customers) {
        this.orders = orders;
        this.customers = customers;
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć nazwisko klienta zamówienia (zamówienie 3 → "Jan Kowalski").
     * Najpierw napisz {@code return orders.findById(orderId).orElseThrow().getCustomer().getName();} i uruchom test —
     * zobaczysz LazyInitializationException. Potem napraw to JEDNĄ adnotacją na metodzie.
     * Podpowiedź: sekcja 3a — {@code @Transactional(readOnly = true)} (org.springframework.transaction.annotation).
     */
    public String customerNameOfOrder(Long orderId) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć zamówienie razem z pozycjami i ich produktami wczytanymi JEDNYM zapytaniem —
     * wynik ma działać poza transakcją (test czyta nazwy produktów i liczy zapytania: dokładnie 1).
     * Podpowiedź: sekcja 3b — w ExerciseShopOrderRepository
     * {@code @Query("select o from ShopOrder o join fetch o.lines l join fetch l.product where o.id = :id")}.
     */
    public ShopOrder orderWithLinesAndProducts(Long orderId) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć WSZYSTKIE zamówienia (posortowane po numerze) z wczytanymi klientami — bez JPQL,
     * jedną adnotacją na metodzie repozytorium; 1 zapytanie.
     * Podpowiedź: sekcja 3c — {@code @EntityGraph(attributePaths = "customer") List<ShopOrder> findAllByOrderByOrderNumber();}
     */
    public List<ShopOrder> allOrdersWithCustomers() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć klienta ze zbiorem zamówień zainicjalizowanym W TRANSAKCJI przez
     * {@code Hibernate.initialize} (klient 1 → 2 zamówienia, klient 3 → pusty zbiór). Poza metodą
     * {@code Hibernate.isInitialized(customer.getOrders())} ma być true. 2 zapytania: klient + zamówienia.
     * Podpowiedź: sekcja 5 — {@code @Transactional(readOnly = true)}, {@code customers.findById(...)},
     * {@code Hibernate.initialize(customer.getOrders())}.
     */
    public Customer customerWithOrders(Long customerId) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }
}
