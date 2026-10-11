package s05_validation.val05_service_validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * PriceService = serwis cen z walidacją na granicy wywołania metody (sekcje 1 i 2 lekcji Val05ServiceValidation).
 * {@code @Validated} na klasie → Spring owija bean w proxy, które sprawdza ograniczenia parametrów i wyniku.
 */
@Service
@Validated   // validated = zwalidowany; znacznik dla MethodValidationPostProcessor
public class PriceService {

    /** Parametry: kwota netto nieujemna, VAT 0..100. Przy złych wartościach metoda się nie wykona. */
    public int gross(@Min(0) int netGrosze, @Min(0) @Max(100) int vatPercent) {
        return netGrosze + netGrosze * vatPercent / 100;
    }

    /** Wynik: ograniczenie przy typie wyniku. Ta metoda ZAWSZE zwraca błędną wartość — pokazuje walidację wyniku. */
    public @Positive int faultyDiscount() {
        return -5;
    }

    /** Parametr-obiekt: @Valid kaskaduje do ProductCommand, @NotNull łapie null; wynik nie może być pusty. */
    public @NotBlank String describe(@Valid @NotNull ProductCommand command) {
        return command.name() + " za " + command.priceGrosze() + " gr";
    }

    /**
     * PUŁAPKA: (sekcja 2) ta metoda woła {@code gross} przez {@code this}, czyli OMIJA proxy — ograniczenia na
     * parametrach {@code gross} nie działają. Zła stawka VAT (np. 500) przejdzie bez błędu.
     */
    public int grossOfAll(List<Integer> netsGrosze, int vatPercent) {
        int sum = 0;
        for (int net : netsGrosze) {
            sum += this.gross(net, vatPercent);   // wywołanie wewnętrzne: bez proxy, bez walidacji
        }
        return sum;
    }
}
