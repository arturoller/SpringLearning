package s05_validation.solutions.val02_valid_in_controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import s05_validation.val02_valid_in_controllers.Customer;
import s05_validation.val02_valid_in_controllers.Customers;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Rozwiązania wzorcowe ćwiczeń lekcji Val02ValidInControllers — zajrzyj dopiero po własnej próbie! */
@RestController
@RequestMapping("/val02/cw/customers")
public class Val02SolutionsController {

    public record CustomerRequest(@NotBlank String name, @NotBlank @Email String email) {
    }

    private final Customers customers;

    public Val02SolutionsController(Customers customers) {
        this.customers = customers;
    }

    @PostMapping
    public ResponseEntity<Customer> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customers.add(request.name(), request.email()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> get(@PathVariable @Min(1) long id) {
        return ResponseEntity.of(customers.find(id));
    }

    @PostMapping("/manual")
    public ResponseEntity<?> createManual(@Valid @RequestBody CustomerRequest request, BindingResult errors) {
        if (errors.hasErrors()) {
            List<String> problems = errors.getFieldErrors().stream()
                    .map(error -> error.getField() + ":" + error.getCode())
                    .sorted()
                    .toList();
            return ResponseEntity.badRequest().body(problems);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(customers.add(request.name(), request.email()));
    }

    public static Map<String, String> messagesByField(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField).thenComparing(error -> String.valueOf(error.getDefaultMessage())))
                .collect(Collectors.toMap(FieldError::getField, error -> String.valueOf(error.getDefaultMessage()),
                        (first, second) -> first + "; " + second, TreeMap::new));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> replace(@PathVariable @Min(1) long id, @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.of(customers.update(id, request.name(), request.email()));
    }
}
