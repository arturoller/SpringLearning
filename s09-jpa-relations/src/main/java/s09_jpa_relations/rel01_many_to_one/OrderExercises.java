package s09_jpa_relations.rel01_many_to_one;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * ĆWICZENIA lekcji Rel01ManyToOne (testy: Rel01ManyToOneExercisesTest — uruchamiasz sam ▶).
 * Zastąp każde {@code throw} własnym kodem; potrzebne zapytania dopisz w {@link ExerciseShopOrderRepository}.
 * Dane: migracja db/migration/rel01/V2 (Anna: ZAM-001, ZAM-003; Jan: ZAM-002, ZAM-005; Ewa: ZAM-004; Piotr: brak).
 */
@Service // service = usługa; bean (ziarno — obiekt zarządzany przez Springa) warstwy logiki
public class OrderExercises {

    private final ExerciseShopOrderRepository orders;
    private final CustomerRepository customers;

    public OrderExercises(ExerciseShopOrderRepository orders, CustomerRepository customers) {
        this.orders = orders;
        this.customers = customers;
    }

    /**
     * ĆWICZENIE 1 (łatwe): numery zamówień klienta o podanym imieniu i nazwisku, rosnąco
     * ({@code "Anna Nowak"} → ZAM-001, ZAM-003; nieznany klient → pusta lista).
     * Podpowiedź: sekcja 4 — metoda z nazwy przechodząca po relacji: {@code findByCustomerNameOrderBy…}.
     */
    public List<String> orderNumbersOfCustomerNamed(String customerName) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zapisz nowe zamówienie klienta o podanym id i zwróć je. Warunek testu: do bazy idzie
     * TYLKO jedno polecenie — INSERT do orders (żadnego SELECT-a klienta).
     * Podpowiedź: sekcja 3 — {@code customers.getReferenceById(…)}.
     */
    public ShopOrder placeOrder(Long customerId, String orderNumber, LocalDate orderedOn) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): liczba zamówień klienta o podanym e-mailu (anna@sklep.pl → 2, piotr@sklep.pl → 0).
     * Podpowiedź: metoda z nazwy {@code countByCustomerEmail} (count = policz) — sprawdź, że SQL robi JOIN.
     */
    public long countOrdersOf(String email) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): liczba zamówień każdego klienta, który ma jakiekolwiek zamówienie, JEDNYM
     * zapytaniem (mapa posortowana po nazwisku-kluczu: {Anna Nowak=2, Ewa Wiśniewska=1, Jan Kowalski=2}).
     * Podpowiedź: {@code @Query("select o.customer.name, count(o) from ShopOrder o group by o.customer.name")}
     * z typem wyniku {@code List<CustomerOrderCount>} (s08_data_jpa/Jpa04CustomQueries, sekcja 3 i 5), potem
     * {@code Collectors.toMap(…, …, (a, b) -> a, TreeMap::new)}.
     */
    public Map<String, Long> orderCountByCustomerName() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): przepisz zamówienie na innego klienta. Warunek testu: po flush do bazy poszły
     * dokładnie dwa polecenia — SELECT zamówienia i UPDATE orders (klienta NIE wczytujemy).
     * Podpowiedź: findById zamówienia + setCustomer(getReferenceById(…)); dirty checking (Jpa01, sekcja 4) zrobi UPDATE.
     */
    public ShopOrder reassign(Long orderId, Long newCustomerId) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }
}
