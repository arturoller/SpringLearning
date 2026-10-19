package s21_capstone.cap01_shop_api.order;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s21_capstone.cap01_shop_api.common.NotFoundException;
import s21_capstone.cap01_shop_api.common.PageResponse;
import s21_capstone.cap01_shop_api.customer.Customer;
import s21_capstone.cap01_shop_api.customer.CustomerRepository;
import s21_capstone.cap01_shop_api.order.OrderDtos.CreateOrderRequest;
import s21_capstone.cap01_shop_api.order.OrderDtos.LineRequest;
import s21_capstone.cap01_shop_api.order.OrderDtos.OrderResponse;
import s21_capstone.cap01_shop_api.product.Product;
import s21_capstone.cap01_shop_api.product.ProductRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Logika zamówień: złożenie (z ceną z chwili zamówienia), lista, szczegóły, zmiana statusu.
 * Czas „teraz” bierzemy z beana {@link Clock}, nie z {@code LocalDateTime.now()} — test ustawi zegar na stałą chwilę.
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orders;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final Clock clock;

    public OrderService(OrderRepository orders, CustomerRepository customers, ProductRepository products, Clock clock) {
        this.orders = orders;
        this.customers = customers;
        this.products = products;
        this.clock = clock;
    }

    /**
     * Złożenie zamówienia w JEDNEJ transakcji: albo zapisze się zamówienie ze wszystkimi pozycjami, albo nic
     * (nieznany sku w trzeciej pozycji = brak zamówienia, a nie zamówienie z dwiema pozycjami).
     * Produkty czytamy jednym zapytaniem {@code where sku in (...)}, a nie po jednym na pozycję.
     */
    @Transactional
    public OrderResponse place(CreateOrderRequest request) {
        Customer customer = customers.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException("klient", request.customerId()));
        Map<String, Product> bySku = products.findBySkuIn(request.lines().stream().map(LineRequest::sku).toList())
                .stream().collect(Collectors.toMap(Product::getSku, Function.identity()));

        // co do sekundy: tak samo w odpowiedzi i w bazie (zegar systemu Windows daje 7 cyfr po przecinku, kolumna — 6)
        ShopOrder order = new ShopOrder(customer, LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
        for (LineRequest line : request.lines()) {
            Product product = bySku.get(line.sku());
            if (product == null) {
                throw new NotFoundException("produkt", line.sku());
            }
            order.addLine(product, line.quantity());
        }
        orders.save(order);   // cascade ALL zapisze też pozycje
        return OrderResponse.from(order);
    }

    public OrderResponse get(long id) {
        return OrderResponse.from(findDetailed(id));
    }

    public PageResponse<OrderSummary> list(OrderStatus status, Pageable pageable) {
        return PageResponse.from(orders.findSummaries(status, pageable));
    }

    /** Reguły przejść są w encji i enumie — serwis tylko ładuje, woła i zwraca DTO. UPDATE wyśle commit. */
    @Transactional
    public OrderResponse changeStatus(long id, OrderStatus next) {
        ShopOrder order = findDetailed(id);
        order.changeStatus(next);
        return OrderResponse.from(order);
    }

    private ShopOrder findDetailed(long id) {
        return orders.findDetailedById(id).orElseThrow(() -> new NotFoundException("zamówienie", ShopOrder.numberOf(id)));
    }
}
