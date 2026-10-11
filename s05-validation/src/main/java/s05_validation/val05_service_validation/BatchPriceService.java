package s05_validation.val05_service_validation;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * BatchPriceService = naprawa pułapki z sekcji 2: logika zbiorcza jest w INNYM beanie, który woła PriceService
 * przez wstrzyknięty bean (czyli przez proxy) — więc ograniczenia z PriceService.gross działają.
 */
@Service
public class BatchPriceService {

    private final PriceService priceService;   // wstrzyknięty jest PROXY, nie goły obiekt

    public BatchPriceService(PriceService priceService) {
        this.priceService = priceService;
    }

    public int grossOfAll(List<Integer> netsGrosze, int vatPercent) {
        int sum = 0;
        for (int net : netsGrosze) {
            sum += priceService.gross(net, vatPercent);   // przez proxy → walidacja działa
        }
        return sum;
    }
}
