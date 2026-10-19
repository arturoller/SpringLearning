package s21_capstone.cap01_shop_api.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** DTO zamówień: złożenie zamówienia, zmiana statusu i odpowiedź ze szczegółami. */
public final class OrderDtos {

    private OrderDtos() {
    }

    /**
     * POST /api/orders. {@code @Valid} na liście — bez niego Bean Validation NIE zajrzy do pozycji
     * (s05_validation/Val03NestedAndGroups), a zamówienie z ilością 0 przeszłoby walidację.
     */
    public record CreateOrderRequest(
            @NotNull(message = "klient jest wymagany")
            Long customerId,
            @NotEmpty(message = "zamówienie musi mieć co najmniej jedną pozycję")
            @Size(max = 20, message = "najwyżej 20 pozycji")
            List<@Valid @NotNull(message = "pozycja nie może być pusta") LineRequest> lines) {
    }

    public record LineRequest(
            @NotBlank(message = "sku jest wymagane")
            String sku,
            @NotNull(message = "ilość jest wymagana")
            @Positive(message = "ilość musi być dodatnia")
            Integer quantity) {
    }

    /** PATCH /api/orders/{id}/status — nieznana wartość („ZAGUBIONE”) kończy się 400 już przy czytaniu JSON-a. */
    public record ChangeStatusRequest(
            @NotNull(message = "status jest wymagany")
            OrderStatus status) {
    }

    public record CustomerRef(Long id, String name) {
    }

    public record LineResponse(String sku, String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    /**
     * Szczegóły zamówienia. {@code allowedStatusChanges} mówi klientowi, jakie przyciski pokazać
     * („Opłać”, „Anuluj”) — bez kopiowania reguł statusów do aplikacji klienta.
     */
    public record OrderResponse(Long id, String number, CustomerRef customer, OrderStatus status, BigDecimal total,
                                LocalDateTime createdAt, List<LineResponse> lines,
                                List<OrderStatus> allowedStatusChanges) {

        public static OrderResponse from(ShopOrder order) {
            List<LineResponse> lines = order.getLines().stream()
                    .map(line -> new LineResponse(line.getProduct().getSku(), line.getProduct().getName(),
                            line.getQuantity(), line.getUnitPrice(), line.lineTotal()))
                    .toList();
            return new OrderResponse(order.getId(), order.number(),
                    new CustomerRef(order.getCustomer().getId(), order.getCustomer().getName()),
                    order.getStatus(), order.getTotal(), order.getCreatedAt(), lines,
                    order.getStatus().allowedNext().stream().sorted().toList());
        }
    }
}
