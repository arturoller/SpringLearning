package s05_validation.val03_nested_and_groups;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** OrderController = przyjmuje zamówienia — z kaskadą walidacji i (dla porównania) bez niej. */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    /** {@code OrderRequest} ma {@code @Valid} na polach zagnieżdżonych, więc walidacja schodzi do adresu i pozycji. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)   // response status = kod statusu odpowiedzi (201 Created)
    public Map<String, Object> create(@Valid @RequestBody OrderRequest request) {
        return Map.of("id", "ZAM-001", "lines", request.lines().size());
    }

    /** PUŁAPKA: rekord bez {@code @Valid} na polach zagnieżdżonych — walidowane są tylko jego własne pola. */
    @PostMapping("/no-cascade")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createNoCascade(@Valid @RequestBody OrderRequestNoCascade request) {
        return Map.of("id", "ZAM-001", "lines", request.lines().size());
    }
}
