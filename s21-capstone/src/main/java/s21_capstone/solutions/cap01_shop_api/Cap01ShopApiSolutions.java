package s21_capstone.solutions.cap01_shop_api;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s21_capstone.cap01_shop_api.Cap01Exercises.CategorySales;
import s21_capstone.cap01_shop_api.common.ConflictException;
import s21_capstone.cap01_shop_api.common.NotFoundException;
import s21_capstone.cap01_shop_api.discount.DiscountCode;
import s21_capstone.cap01_shop_api.discount.DiscountCodeRepository;
import s21_capstone.cap01_shop_api.order.OrderDtos.CreateOrderRequest;
import s21_capstone.cap01_shop_api.order.OrderDtos.LineRequest;
import s21_capstone.cap01_shop_api.order.OrderDtos.OrderResponse;
import s21_capstone.cap01_shop_api.order.OrderRepository;
import s21_capstone.cap01_shop_api.order.OrderService;
import s21_capstone.cap01_shop_api.order.OrderStatus;
import s21_capstone.cap01_shop_api.order.ShopOrder;
import s21_capstone.cap01_shop_api.product.Product;
import s21_capstone.cap01_shop_api.product.ProductRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Rozwiązania wzorcowe ćwiczeń projektu Cap01ShopApi — zajrzyj dopiero po własnej próbie!
 * Bean spoza skanowanego pakietu projektu — test rozwiązań rejestruje go w {@code @SpringBootTest(classes = ...)}.
 */
@Service
public class Cap01ShopApiSolutions {

    private final OrderRepository orders;
    private final ProductRepository products;
    private final DiscountCodeRepository discountCodes;
    private final OrderService orderService;
    private final EntityManager entityManager;
    private final Clock clock;

    public Cap01ShopApiSolutions(OrderRepository orders, ProductRepository products,
                                 DiscountCodeRepository discountCodes, OrderService orderService,
                                 EntityManager entityManager, Clock clock) {
        this.orders = orders;
        this.products = products;
        this.discountCodes = discountCodes;
        this.orderService = orderService;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    /** ĆWICZENIE 1: „dziś” z zegara, porównanie isAfter (w dniu validUntil kod działa), zaokrąglenie do groszy. */
    @Transactional(readOnly = true)
    public BigDecimal totalWithDiscount(long orderId, String code) {
        ShopOrder order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("zamówienie", ShopOrder.numberOf(orderId)));
        String normalized = code.strip().toUpperCase(Locale.ROOT);
        DiscountCode discount = discountCodes.findById(normalized)
                .orElseThrow(() -> new NotFoundException("kod rabatowy", normalized));
        if (LocalDate.now(clock).isAfter(discount.getValidUntil())) {
            throw new ConflictException("DISCOUNT_EXPIRED",
                    "Kod " + normalized + " był ważny do " + discount.getValidUntil(),
                    Map.of("code", normalized, "validUntil", discount.getValidUntil().toString()));
        }
        return order.getTotal()
                .multiply(BigDecimal.valueOf(100 - discount.getPercent()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * ĆWICZENIE 2: jedno zapytanie z projekcją do rekordu. Rekord ZAGNIEŻDŻONY podajemy nazwą binarną, z dolarem:
     * Cap01Exercises$CategorySales. Z kropką (Cap01Exercises.CategorySales) Hibernate 7.4 zgłasza
     * „Could not resolve class '...Cap01Exercises.CategorySales' named for instantiation” (sprawdzone uruchomieniem).
     * {@code sum(l.quantity)} zwraca {@code Long} — Hibernate dopasuje go do parametru {@code long} konstruktora.
     */
    @Transactional(readOnly = true)
    public List<CategorySales> salesByCategory() {
        return entityManager.createQuery("""
                        select new s21_capstone.cap01_shop_api.Cap01Exercises$CategorySales(
                                   p.category, sum(l.quantity), sum(l.unitPrice * l.quantity))
                        from OrderLine l
                            join l.product p
                            join l.order o
                        where o.status <> :cancelled
                        group by p.category
                        order by sum(l.unitPrice * l.quantity) desc""", CategorySales.class)
                .setParameter("cancelled", OrderStatus.ANULOWANE)
                .getResultList();
    }

    /**
     * ĆWICZENIE 3: jedna transakcja na całość. Gdy druga pozycja rzuci wyjątek, zmiana stanu pierwszej (już zrobiona
     * na encji w pamięci) NIE trafi do bazy — rollback. Bez {@code @Transactional} encje z findBySkuIn byłyby
     * odłączone (każde wywołanie repozytorium = osobna, od razu zakończona transakcja) i stany nie zmieniłyby się
     * nigdy — także przy udanym zamówieniu.
     */
    @Transactional
    public OrderResponse placeWithStockCheck(CreateOrderRequest request) {
        Map<String, Product> bySku = products.findBySkuIn(request.lines().stream().map(LineRequest::sku).toList())
                .stream().collect(java.util.stream.Collectors.toMap(Product::getSku, p -> p));
        for (LineRequest line : request.lines()) {
            Product product = bySku.get(line.sku());
            if (product == null) {
                throw new NotFoundException("produkt", line.sku());
            }
            if (product.getStock() < line.quantity()) {
                throw new ConflictException("OUT_OF_STOCK", "Brak towaru: " + product.getSku(),
                        Map.of("sku", product.getSku(), "available", product.getStock(), "requested", line.quantity()));
            }
            product.removeFromStock(line.quantity());
        }
        return orderService.place(request);   // dołącza do TEJ transakcji (REQUIRED)
    }
}
