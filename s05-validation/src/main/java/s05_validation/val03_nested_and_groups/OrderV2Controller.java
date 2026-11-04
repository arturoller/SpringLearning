package s05_validation.val03_nested_and_groups;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Wariant z sekcji 5 lekcji: osobne DTO dla tworzenia i zmiany ZAMIAST grup. Każde ma swoje, proste reguły bez grup,
 * a DTO tworzenia w ogóle NIE MA pola id (nie da się go wysłać "przypadkiem").
 */
@RestController
@RequestMapping("/val03/v2/orders")
public class OrderV2Controller {

    /** Wejście tworzenia: bez id. */
    public record CreateOrder(
            @NotNull @Valid Address address,
            @NotEmpty List<@NotNull @Valid OrderLine> lines,
            List<@NotBlank String> tags) {
    }

    /** Wejście zmiany: dodatkowo wymagana wersja (do wykrywania równoczesnych zmian); id jest w adresie. */
    public record UpdateOrder(
            @NotNull Integer version,
            @NotNull @Valid Address address,
            @NotEmpty List<@NotNull @Valid OrderLine> lines,
            List<@NotBlank String> tags) {
    }

    private final OrderService service;

    public OrderV2Controller(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> create(@Valid @RequestBody CreateOrder order) {
        return Map.of("id", service.create(order.address(), order.lines(), order.tags()));
    }

    @PutMapping("/{id}")
    public Map<String, Long> update(@PathVariable long id, @Valid @RequestBody UpdateOrder order) {
        service.update(id, order.address(), order.lines(), order.tags());
        return Map.of("id", id);
    }
}
