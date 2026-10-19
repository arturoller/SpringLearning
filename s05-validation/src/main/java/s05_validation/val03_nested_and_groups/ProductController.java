package s05_validation.val03_nested_and_groups;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ProductController = ten sam rekord ProductDto, trzy różne zestawy reguł. {@code @Validated(Grupa.class)} (przy
 * PARAMETRZE, a nie przy klasie!) wybiera grupę; zwykłe {@code @Valid} sprawdza tylko grupę Default.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    /** Tworzenie: grupa OnCreate (id musi być puste) + Default (nazwa, cena). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Validated(OnCreate.class) @RequestBody ProductDto product) {
        return Map.of("name", product.name());
    }

    /** Aktualizacja: grupa OnUpdate (id wymagane) + Default. */
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable long id, @Validated(OnUpdate.class) @RequestBody ProductDto product) {
        return Map.of("id", id, "name", product.name());
    }

    /** Samo {@code @Valid}: tylko grupa Default — reguły z {@code groups} (id, kategoria) są ignorowane. */
    @PostMapping("/default")
    public Map<String, Object> defaultGroupOnly(@Valid @RequestBody ProductDto product) {
        return Map.of("name", product.name());
    }
}
