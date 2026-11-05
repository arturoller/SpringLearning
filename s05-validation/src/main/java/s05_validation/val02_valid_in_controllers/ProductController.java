package s05_validation.val02_valid_in_controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Kontroler lekcji Val02ValidInControllers — BEZ własnej obsługi błędów, więc pokazuje domyślne zachowanie Springa.
 * Jego odpowiednik z własnym {@code @ExceptionHandler} to {@link FriendlyProductController}.
 */
@RestController
@RequestMapping("/val02/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    /** Sekcja 1: {@code @Valid} przy {@code @RequestBody} — błędne dane to 400, a ta metoda się NIE wykonuje. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody ProductRequest request) {
        return service.create(request);
    }

    /** Sekcja 1, PUŁAPKA: BEZ {@code @Valid} adnotacje w ProductRequest są ignorowane. */
    @PostMapping("/unchecked")
    @ResponseStatus(HttpStatus.CREATED)
    public Product createUnchecked(@RequestBody ProductRequest request) {
        return service.create(request);
    }

    /** Sekcja 3: {@code BindingResult} zaraz po {@code @Valid} — kontroler sam decyduje, co zrobić z błędami. */
    @PostMapping("/manual")
    public ResponseEntity<?> createManual(@Valid @RequestBody ProductRequest request, BindingResult errors) {
        if (errors.hasErrors()) {
            List<String> problems = errors.getFieldErrors().stream()
                    .map(error -> error.getField() + ":" + error.getCode())
                    .sorted()
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(problems);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    /** Sekcja 4: ograniczenie na parametrze ścieżki. */
    @GetMapping("/{id}")
    public ResponseEntity<Product> find(@PathVariable @Min(1) long id) {
        return ResponseEntity.of(service.find(id));
    }

    /** Sekcja 4: ograniczenie na parametrze zapytania ({@code ?q=…}). */
    @GetMapping
    public List<Product> search(@RequestParam(required = false) @Size(max = 10) String q) {
        return service.search(q);
    }

    /** Sekcja 4: ograniczenie na parametrze ścieżki ORAZ {@code @Valid} na ciele — razem w jednej metodzie. */
    @PutMapping("/{id}")
    public Product replace(@PathVariable @Min(1) long id, @Valid @RequestBody ProductRequest request) {
        return service.create(request);
    }
}
