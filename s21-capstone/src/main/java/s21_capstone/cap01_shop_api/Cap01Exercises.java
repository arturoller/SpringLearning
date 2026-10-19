package s21_capstone.cap01_shop_api;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import s21_capstone.cap01_shop_api.discount.DiscountCodeRepository;
import s21_capstone.cap01_shop_api.order.OrderDtos.CreateOrderRequest;
import s21_capstone.cap01_shop_api.order.OrderDtos.OrderResponse;
import s21_capstone.cap01_shop_api.order.OrderRepository;
import s21_capstone.cap01_shop_api.order.OrderService;
import s21_capstone.cap01_shop_api.product.Category;
import s21_capstone.cap01_shop_api.product.ProductRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

/**
 * ĆWICZENIA projektu Cap01ShopApi = ROZSZERZENIA sklepu (opis każdego w Javadocu metody). Klasa jest beanem
 * (serwisem), więc możesz dopisać {@code @Transactional} i wstrzykiwać repozytoria. Dopóki nie rozwiążesz ćwiczeń,
 * metody rzucają UnsupportedOperationException — aplikacja i domyślne testy działają normalnie.
 * Testy: Cap01ShopApiExercisesTest (uruchamiasz sam ▶). Rozwiązania: s21_capstone.solutions.cap01_shop_api.
 *
 * <p>Wszystko, czego potrzebujesz, jest gotowe: tabela i encja kodów rabatowych (V3__discount_codes.sql, DiscountCode),
 * repozytoria, {@code Product.removeFromStock}, {@code OrderService.place}, zegar {@link Clock}.
 * Chcesz wystawić funkcję przez HTTP? Dopisz punkt końcowy w kontrolerze — testy ćwiczeń wołają metody wprost.</p>
 */
@Service
public class Cap01Exercises {

    /**
     * Wiersz raportu sprzedaży (ĆWICZENIE 2).
     *
     * @param pieces  ile sztuk sprzedano w kategorii
     * @param revenue przychód: suma (cena z zamówienia × ilość)
     */
    public record CategorySales(Category category, long pieces, BigDecimal revenue) {
    }

    private final OrderRepository orders;
    private final ProductRepository products;
    private final DiscountCodeRepository discountCodes;
    private final OrderService orderService;
    private final EntityManager entityManager;
    private final Clock clock;

    public Cap01Exercises(OrderRepository orders, ProductRepository products, DiscountCodeRepository discountCodes,
                          OrderService orderService, EntityManager entityManager, Clock clock) {
        this.orders = orders;
        this.products = products;
        this.discountCodes = discountCodes;
        this.orderService = orderService;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    /**
     * ĆWICZENIE 1 (łatwe): kody rabatowe. Zwróć sumę zamówienia {@code orderId} po rabacie z kodu {@code code}.
     * <ul>
     *   <li>kod bez względu na wielkość liter („jesien10” = „JESIEN10”), białe znaki na brzegach ignoruj;</li>
     *   <li>nie ma zamówienia → {@code NotFoundException("zamówienie", ...)}; nie ma kodu →
     *       {@code NotFoundException("kod rabatowy", kod)};</li>
     *   <li>kod przeterminowany (dziś — z {@code clock}! — jest PO {@code validUntil}) →
     *       {@code ConflictException("DISCOUNT_EXPIRED", ...)}; w dniu {@code validUntil} kod jeszcze działa;</li>
     *   <li>wynik: suma × (100 − procent) / 100, zaokrąglona do groszy HALF_UP, np. 764.79 z 10% → 688.31.</li>
     * </ul>
     * Podpowiedź: {@code LocalDate.now(clock)}, {@code BigDecimal.valueOf(100 - percent)},
     * {@code divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)}. Metoda tylko CZYTA — wystarczy
     * {@code @Transactional(readOnly = true)}.
     */
    public BigDecimal totalWithDiscount(long orderId, String code) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): raport sprzedaży po kategoriach. Dla każdej kategorii, w której COKOLWIEK sprzedano:
     * liczba sztuk i przychód (suma {@code unitPrice × quantity} z pozycji — cena Z ZAMÓWIENIA, nie aktualna cena
     * produktu). Pomiń zamówienia ANULOWANE. Kolejność: przychód malejąco.
     * Dla danych startowych: ELEKTRONIKA 3 szt. 6199.79 · DOM 1 szt. 1899.00 · KSIAZKI 2 szt. 208.00 · SPOZYWCZE 3 szt.
     * 194.97 (ODZIEZ nie ma — jej jedyne zamówienie jest anulowane).
     * Podpowiedź: JEDNO zapytanie JPQL z projekcją do rekordu, np.
     * {@code entityManager.createQuery("select new ...CategorySales(p.category, sum(l.quantity), sum(l.unitPrice * l.quantity)) from OrderLine l join l.product p join l.order o where ... group by ... order by ...", CategorySales.class)}
     * — rekord jest ZAGNIEŻDŻONY w tej klasie, więc w JPQL potrzebuje nazwy binarnej z dolarem:
     * s21_capstone.cap01_shop_api.Cap01Exercises$CategorySales (z kropką Hibernate go nie znajdzie). Statusu nie wpisuj
     * jako tekst w zapytaniu — użyj parametru {@code :cancelled}. {@code sum(l.quantity)} zwraca {@code Long}.
     */
    public List<CategorySales> salesByCategory() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): blokada zamówienia przy braku towaru. Jak {@code orderService.place(request)}, ale:
     * <ul>
     *   <li>przed złożeniem zamówienia zdejmij towar z magazynu ({@code product.removeFromStock(quantity)}) dla KAŻDEJ
     *       pozycji;</li>
     *   <li>gdy którejś pozycji brakuje → {@code ConflictException("OUT_OF_STOCK", "Brak towaru: SPO-003",
     *       Map.of("sku", ..., "available", ..., "requested", ...))} i NIC się nie zmienia: ani zamówienie, ani stany
     *       innych produktów (także tych z wcześniejszych pozycji!);</li>
     *   <li>nieznany sku → ten sam NotFoundException co w {@code place}.</li>
     * </ul>
     * Podpowiedź: wszystko w JEDNEJ transakcji ({@code @Transactional} — ta klasa jest beanem, więc proxy zadziała);
     * {@code orderService.place(...)} dołączy do niej (propagacja REQUIRED — s10_transactions/Tx02Propagation).
     * Zmienione encje zapisze commit — nie trzeba {@code save}. Co się stanie, jeśli zapomnisz o {@code @Transactional}?
     * Test sprawdza stan ELE-003 po nieudanym zamówieniu.
     */
    public OrderResponse placeWithStockCheck(CreateOrderRequest request) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
