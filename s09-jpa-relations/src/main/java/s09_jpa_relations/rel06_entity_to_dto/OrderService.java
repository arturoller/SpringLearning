package s09_jpa_relations.rel06_entity_to_dto;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Serwis zamówień lekcji Rel06EntityToDto — GRANICA między światem encji a światem DTO (sekcja 4).
 * Do środka wchodzą żądania (rekordy), na zewnątrz wychodzą DTO; encje nie opuszczają transakcji serwisu.
 * {@code @Transactional(readOnly = true)} na klasie, a metody zmieniające dane nadpisują to zwykłym
 * {@code @Transactional} (transakcja do zapisu).
 */
@Service // service = usługa; bean (ziarno — obiekt zarządzany przez Springa) warstwy logiki
@Transactional(readOnly = true)
public class OrderService {

    private final ShopOrderRepository orders;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrderMapper mapper;

    public OrderService(ShopOrderRepository orders, CustomerRepository customers, ProductRepository products,
                        OrderMapper mapper) {
        this.orders = orders;
        this.customers = customers;
        this.products = products;
        this.mapper = mapper;
    }

    /** Szczegóły zamówienia: 1 zapytanie (join fetch), mapowanie w transakcji, na zewnątrz rekord. */
    public OrderDetailsDto details(String orderNumber) {
        ShopOrder order = orders.findDetailsByOrderNumber(orderNumber)
                .orElseThrow(() -> new NoSuchElementException("Brak zamówienia " + orderNumber));
        return mapper.toDetails(order);
    }

    /** Lista zamówień: projekcja prosto z zapytania — 1 zapytanie, 0 encji. */
    public List<OrderSummaryDto> summaries() {
        return orders.findSummaries();
    }

    /** Klient bez skrótu hasła — mapowanie metodą statyczną DTO. */
    public CustomerDto customer(Long customerId) {
        return CustomerDto.from(customers.findById(customerId).orElseThrow());
    }

    /**
     * Sekcja 3: żądanie → encja. Ceny bierzemy z PRODUKTÓW w bazie (klient ich nie podaje), produkty wczytujemy
     * jednym zapytaniem ({@code findAllById} → {@code where id in (…)}), a numer i status nadaje serwer.
     */
    @Transactional // zapis — nadpisuje readOnly z klasy
    public OrderDetailsDto placeOrder(CreateOrderRequest request) {
        Customer customer = customers.findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Nieznany klient: " + request.customerId()));
        List<Long> productIds = request.items().stream().map(CreateOrderRequest.Item::productId).toList();
        Map<Long, Product> byId = products.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        ShopOrder order = new ShopOrder(nextOrderNumber(), customer, request.note());
        for (CreateOrderRequest.Item item : request.items()) {
            Product product = byId.get(item.productId());
            if (product == null) {
                throw new IllegalArgumentException("Nieznany produkt: " + item.productId());
            }
            order.addLine(product, item.quantity(), product.getPrice()); // cena z BAZY, nie z żądania
        }
        return mapper.toDetails(orders.save(order)); // kaskada zapisze pozycje
    }

    /**
     * Sekcja 4: aktualizacja = zmiana ZARZĄDZANEJ encji. Żadnego {@code save}: przy zatwierdzeniu transakcji
     * dirty checking (wykrywanie zmian) sam wyśle UPDATE.
     */
    @Transactional
    public void changeNote(String orderNumber, UpdateNoteRequest request) {
        ShopOrder order = orders.findByOrderNumber(orderNumber).orElseThrow();
        order.setNote(request.note());
    }

    private String nextOrderNumber() {
        return "ZAM-%03d".formatted(orders.count() + 1);
    }
}
