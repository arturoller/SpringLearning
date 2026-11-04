package s09_jpa_relations.rel05_n_plus_one;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * ĆWICZENIA lekcji Rel05NPlusOne (testy: Rel05NPlusOneExercisesTest — uruchamiasz sam ▶). Każdy test LICZY zapytania
 * SQL — rozwiązanie „działające”, ale z N+1, nie przejdzie. Zastąp każde {@code throw} własnym kodem; zapytania
 * dopisz w {@link ExerciseShopOrderRepository}. Dane: migracja db/migration/rel05/V2 (10 zamówień, ZAM-010 puste).
 */
@Service // service = usługa; bean (ziarno — obiekt zarządzany przez Springa) warstwy logiki
public class NPlusOneExercises {

    private final ExerciseShopOrderRepository orders;

    public NPlusOneExercises(ExerciseShopOrderRepository orders) {
        this.orders = orders;
    }

    /**
     * ĆWICZENIE 1 (łatwe): mapa numer zamówienia → nazwisko klienta dla wszystkich zamówień, posortowana po numerze
     * (ZAM-001=Anna Nowak, …, ZAM-010=Anna Nowak). Dokładnie 1 zapytanie (bez pomocy: 1 + 4).
     * Podpowiedź: sekcja 2 — {@code @Query("select o from ShopOrder o join fetch o.customer")} i
     * {@code Collectors.toMap(…, …, (a, b) -> a, TreeMap::new)}.
     */
    public Map<String, String> customerNameByOrderNumber() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): mapa numer zamówienia → nazwy produktów z jego pozycji (w kolejności pozycji), także dla
     * pustego ZAM-010 (pusta lista). Dokładnie 1 zapytanie — pozycje i produkty dociągnięte od razu.
     * Podpowiedź: sekcja 2 — {@code left join fetch o.lines l left join fetch l.product}; czemu LEFT? (ZAM-010).
     */
    public Map<String, List<String>> productNamesByOrderNumber() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zamówienia klienta (po numerze) z pozycjami ORAZ produktami pozycji wczytanymi przez
     * {@code @EntityGraph} — bez JPQL. Po wywołaniu test czyta nazwy produktów i sprawdza, że poszło 1 zapytanie.
     * Podpowiedź: sekcja 3 — ścieżka z kropką: {@code @EntityGraph(attributePaths = {"lines", "lines.product"})}
     * na metodzie {@code findByCustomerIdOrderByOrderNumber}.
     */
    public List<ShopOrder> ordersOfCustomerWithProducts(Long customerId) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): dla każdego klienta (po nazwisku): liczba zamówień i łączna wartość pozycji —
     * JEDNYM zapytaniem, bez wczytywania encji. Anna: 4 zamówienia, 1450.39; Ewa: 2, 1307.89; Jan: 2, 530.40;
     * Piotr: 2, 2121.48.
     * Podpowiedź: sekcja 5 — {@code select new …CustomerSpending(c.name, count(distinct o), coalesce(sum(l.quantity * l.unitPrice), 0))}
     * {@code from ShopOrder o join o.customer c left join o.lines l group by c.name order by c.name}.
     * Dlaczego {@code count(distinct o)}, a nie {@code count(o)}? (JOIN z pozycjami powtarza zamówienie w wierszach.)
     */
    public List<CustomerSpending> spendingPerCustomer() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): wszystkie zamówienia (po numerze) z wczytanymi OBIEMA listami: pozycjami i
     * płatnościami — dokładnie 2 zapytania i bez MultipleBagFetchException.
     * Podpowiedź: sekcja 4 — pierwsze zapytanie: {@code left join fetch o.lines}; drugie:
     * {@code select o from ShopOrder o left join fetch o.payments where o in :orders} — Hibernate dopisze płatności do
     * zamówień, które już są w kontekście. Zwróć wynik PIERWSZEGO zapytania (ma dobrą kolejność).
     */
    public List<ShopOrder> ordersWithLinesAndPayments() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }
}
