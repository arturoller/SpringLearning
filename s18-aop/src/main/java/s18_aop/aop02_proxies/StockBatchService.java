package s18_aop.aop02_proxies;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Obejście nr 1 (NAJLEPSZE): pętla po SKU w OSOBNYM beanie. stockService to wstrzyknięte proxy, więc każde
 * stockService.reserve(...) przechodzi przez porady — bez żadnych sztuczek.
 */
@Service
public class StockBatchService {

    private final StockService stockService;

    public StockBatchService(StockService stockService) {
        this.stockService = stockService;
    }

    public List<String> reserveAll(List<String> skus) {
        return skus.stream().map(sku -> stockService.reserve(sku, 1)).toList();
    }
}
