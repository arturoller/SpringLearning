package s16_cache_scheduling_async.cache02_cache_management;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Punkty końcowe (endpoints) do ręcznego sprawdzenia @CachePut i @CacheEvict z pliku .http. */
@RestController
public class PriceController {

    private final PriceBoard board;

    public PriceController(PriceBoard board) {
        this.board = board;
    }

    @GetMapping("/api/prices/{sku}")
    public long price(@PathVariable String sku) {
        return board.priceOf(sku);
    }

    @PutMapping("/api/prices/{sku}")
    public long change(@PathVariable String sku, @RequestParam long grosze) {
        return board.changePrice(sku, grosze);
    }

    @DeleteMapping("/api/prices")
    public void reload() {
        board.reloadAll();
    }

    @GetMapping("/api/prices-stats")
    public Map<String, Integer> stats() {
        return Map.of("loads", board.loads());
    }
}
