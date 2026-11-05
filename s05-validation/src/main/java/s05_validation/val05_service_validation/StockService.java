package s05_validation.val05_service_validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * Serwis magazynu z walidacją metod. {@code @Validated} na KLASIE włącza walidację: Spring owija bean w proxy
 * (pośrednika), który przed wywołaniem sprawdza ograniczenia na parametrach, a po wywołaniu — na wyniku.
 */
@Service
@Validated   // validated = zwalidowany; znacznik dla MethodValidationPostProcessor
public class StockService {

    private final List<String> log = new ArrayList<>();

    /** Ograniczenia na PARAMETRACH: wywołanie z pustym SKU albo ilością ≤ 0 zakończy się ConstraintViolationException. */
    public int reserve(@NotBlank String sku, @Positive int quantity) {
        log.add("reserve " + sku + " x" + quantity);
        return quantity;
    }

    /** {@code @Valid} na parametrze = "wejdź do środka obiektu" (kaskada); ścieżki: {@code register.item.sku}. */
    public void register(@Valid @NotNull NewItem item) {
        log.add("register " + item.sku() + " x" + item.quantity());
    }

    /** Bez {@code @Valid} na parametrze: adnotacje WEWNĄTRZ {@link NewItem} nie są sprawdzane (brak kaskady). */
    public void registerWithoutCascade(@NotNull NewItem item) {
        log.add("registerWithoutCascade " + item.sku());
    }

    /** Ograniczenie na WYNIKU: błąd w kodzie serwisu (pusty wynik) też zostanie złapany. */
    public @NotBlank String label(String sku, boolean buggy) {
        return buggy ? "" : "SKU: " + sku;
    }

    /**
     * Wywołanie własnej metody (self-invocation): {@code reserve(...)} woła tu "this", czyli goły obiekt,
     * z pominięciem proxy. Ograniczenia z {@link #reserve} NIE zostaną sprawdzone.
     */
    public int reserveTwiceBroken(String sku, int quantity) {
        reserve(sku, quantity);
        return reserve(sku, quantity);
    }

    public List<String> log() {
        return List.copyOf(log);
    }
}
