package s05_validation.val05_service_validation;

import org.springframework.stereotype.Service;

/**
 * DOBRA PRAKTYKA: wywołanie przez INNY bean. {@code stockService} to proxy wstrzyknięte przez Springa, więc każde wywołanie
 * przechodzi przez walidację — w odróżnieniu od {@link StockService#reserveTwiceBroken}.
 */
@Service
public class BatchService {

    private final StockService stockService;

    public BatchService(StockService stockService) {
        this.stockService = stockService;
    }

    public int reserveTwice(String sku, int quantity) {
        stockService.reserve(sku, quantity);
        return stockService.reserve(sku, quantity);
    }
}
