package s09_jpa_relations.rel06_entity_to_dto;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ĆWICZENIA lekcji Rel06EntityToDto (testy: Rel06EntityToDtoExercisesTest — uruchamiasz sam ▶). Na zewnątrz metod
 * wychodzą WYŁĄCZNIE DTO (rekordy z tego pakietu). Zastąp każde {@code throw} własnym kodem; zapytania dopisz
 * w {@link ExerciseShopOrderRepository}. Dane: migracja db/migration/rel06/V2.
 */
@Service // service = usługa; bean (ziarno — obiekt zarządzany przez Springa) warstwy logiki
public class DtoExercises {

    private final ExerciseShopOrderRepository orders;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrderMapper mapper;

    public DtoExercises(ExerciseShopOrderRepository orders, CustomerRepository customers, ProductRepository products,
                        OrderMapper mapper) {
        this.orders = orders;
        this.customers = customers;
        this.products = products;
        this.mapper = mapper;
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć klienta jako {@link CustomerDto} (bez skrótu hasła!). Nieznane id →
     * {@code NoSuchElementException}.
     * Podpowiedź: sekcja 2 — {@code customers.findById(id).map(CustomerDto::from).orElseThrow()}.
     */
    public CustomerDto customer(Long customerId) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): lista {@link OrderSummaryDto} zamówień JEDNEGO klienta, po numerze — jednym zapytaniem,
     * bez wczytywania encji (Anna: ZAM-001 267.90, ZAM-003 98.00).
     * Podpowiedź: sekcja 2 i Rel05 sekcja 5 — {@code select new …OrderSummaryDto(…)} jak w ShopOrderRepository.findSummaries,
     * z warunkiem {@code where c.id = :customerId}.
     */
    public List<OrderSummaryDto> summariesOfCustomer(Long customerId) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zmień ilość pozycji z danym produktem w zamówieniu. BEZ {@code save} — test sprawdza,
     * że przy flush idzie dokładnie jeden {@code update order_line …}. Brak takiej pozycji → {@code NoSuchElementException}.
     * Podpowiedź: sekcja 4 — {@code orders.findById}, znajdź pozycję w {@code getLines()} po {@code getProduct().getName()}
     * i wywołaj {@code line.setQuantity(quantity)}; dirty checking zrobi resztę.
     */
    public void changeQuantity(Long orderId, String productName, int quantity) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): szczegóły ({@link OrderDetailsDto}) WSZYSTKICH zamówień klienta, po numerze — encje
     * wczytane JEDNYM zapytaniem (klient, pozycje, produkty), mapowanie przez {@code mapper.toDetails}.
     * Podpowiedź: {@code select o from ShopOrder o join fetch o.customer left join fetch o.lines l left join fetch l.product}
     * {@code where o.customer.id = :customerId order by o.orderNumber}; pamiętaj, że to ma działać bez N+1 (test liczy).
     */
    public List<OrderDetailsDto> detailsOfCustomer(Long customerId) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): dodaj do ISTNIEJĄCEGO zamówienia pozycje z żądania ({@link CreateOrderRequest.Item})
     * i zwróć nowe szczegóły. Ceny z bazy, wszystkie produkty JEDNYM zapytaniem ({@code products.findAllById}), nieznany
     * produkt → {@code IllegalArgumentException("Nieznany produkt: " + id)} ZANIM cokolwiek dodasz.
     * Podpowiedź: sekcja 3 — jak OrderService.placeOrder, tylko zamówienie z {@code orders.findById}, a zapis zrobi
     * kaskada (zamówienie jest zarządzane — save niepotrzebny).
     */
    public OrderDetailsDto addItems(Long orderId, List<CreateOrderRequest.Item> items) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }
}
