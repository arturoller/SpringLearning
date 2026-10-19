package s21_capstone.cap01_shop_api.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import s21_capstone.cap01_shop_api.common.ConflictException;
import s21_capstone.cap01_shop_api.customer.Customer;
import s21_capstone.cap01_shop_api.product.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Zamówienie — „korzeń” (aggregate root) dla swoich pozycji: pozycje dodaje się TYLKO przez {@link #addLine},
 * status zmienia się TYLKO przez {@link #changeStatus}. Dzięki temu dwie reguły nie mogą się rozjechać:
 * <ul>
 *   <li>{@code total} zawsze równa się sumie pozycji (przeliczana przy każdej zmianie pozycji),</li>
 *   <li>status zmienia się wyłącznie zgodnie z {@link OrderStatus#allowedNext()}.</li>
 * </ul>
 * Nazwa klasy to ShopOrder, a tabeli — orders: „Order” koliduje z adnotacją Springa {@code @Order},
 * a {@code order} to słowo zastrzeżone SQL (ORDER BY).
 */
@Entity
@Table(name = "orders")
public class ShopOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    /** Suma zapisana w tabeli — lista zamówień nie musi czytać pozycji, żeby ją pokazać. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * cascade ALL — pozycje zapisują się razem z zamówieniem; orphanRemoval — pozycja usunięta z listy znika z bazy;
     * {@code @OrderBy("id")} — pozycje zawsze w kolejności dodania (bez tego kolejność z bazy jest przypadkowa).
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderLine> lines = new ArrayList<>();

    protected ShopOrder() {
    }

    public ShopOrder(Customer customer, LocalDateTime createdAt) {
        this.customer = customer;
        this.createdAt = createdAt;
        this.status = OrderStatus.NOWE;
        this.total = BigDecimal.ZERO.setScale(2);
    }

    /** Numer dla ludzi: ZAM-001, ZAM-002... — wyliczany z id, więc nie trzeba osobnej kolumny ani licznika. */
    public static String numberOf(Long id) {
        return "ZAM-%03d".formatted(id);
    }

    public String number() {
        return numberOf(id);
    }

    public void addLine(Product product, int quantity) {
        if (status != OrderStatus.NOWE) {
            throw new ConflictException("ORDER_LOCKED", "Pozycje można zmieniać tylko w zamówieniu NOWE",
                    Map.of("status", status.name()));
        }
        lines.add(new OrderLine(this, product, quantity));
        recalculateTotal();
    }

    /** Suma pozycji, zaokrąglona do groszy (HALF_UP — „szkolne” zaokrąglanie, t15_numbers/Numbers01BigDecimal). */
    void recalculateTotal() {
        total = lines.stream()
                .map(OrderLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Zmiana statusu zgodnie z regułami. Niedozwolone przejście to wyjątek domeny z polami dla klienta
     * (porada zamieni go na 409 z {@code currentStatus}, {@code requestedStatus} i listą {@code allowed}).
     */
    public void changeStatus(OrderStatus next) {
        if (!status.canChangeTo(next)) {
            throw new ConflictException("ORDER_STATUS_CHANGE",
                    "Nie można zmienić statusu zamówienia " + number() + " z " + status + " na " + next,
                    Map.of("currentStatus", status.name(), "requestedStatus", next.name(),
                            "allowed", status.allowedNext().stream().map(Enum::name).sorted().toList()));
        }
        status = next;
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** Widok tylko do odczytu — kto chce zmienić pozycje, musi przejść przez addLine (i przeliczenie sumy). */
    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
