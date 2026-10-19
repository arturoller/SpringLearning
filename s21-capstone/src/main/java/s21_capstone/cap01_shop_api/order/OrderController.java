package s21_capstone.cap01_shop_api.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import s21_capstone.cap01_shop_api.common.PageResponse;
import s21_capstone.cap01_shop_api.order.OrderDtos.ChangeStatusRequest;
import s21_capstone.cap01_shop_api.order.OrderDtos.CreateOrderRequest;
import s21_capstone.cap01_shop_api.order.OrderDtos.OrderResponse;

import java.net.URI;

/**
 * Zamówienia.
 * <pre>
 *   POST  /api/orders              201 + Location / 400 walidacja / 404 nieznany klient albo sku
 *   GET   /api/orders              200 strona OrderSummary, najnowsze pierwsze; opcjonalnie ?status=NOWE, page, size
 *   GET   /api/orders/{id}         200 szczegóły z pozycjami / 404
 *   PATCH /api/orders/{id}/status  200 / 400 (nieznany status) / 404 / 409 (niedozwolone przejście)
 * </pre>
 * Zmiana statusu to PATCH na pod-zasób {@code /status}, a nie PUT całego zamówienia: klient zmienia JEDNO pole,
 * i to według reguł — nie przysyła na nowo pozycji ani sumy.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = orderService.place(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** Sortowanie stałe (najnowsze pierwsze) — klient go nie wybiera, więc nie ma czego sprawdzać. */
    @GetMapping
    public PageResponse<OrderSummary> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "strony liczymy od 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "rozmiar strony: od 1 do 50")
            @Max(value = 50, message = "rozmiar strony: od 1 do 50") int size) {
        return orderService.list(status, PageRequest.of(page, size, Sort.by(Sort.Order.desc("id"))));
    }

    @GetMapping("/{id}")
    public OrderResponse one(@PathVariable long id) {
        return orderService.get(id);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse changeStatus(@PathVariable long id, @Valid @RequestBody ChangeStatusRequest request) {
        return orderService.changeStatus(id, request.status());
    }
}
