package s05_validation.val02_valid_in_controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ExerciseController = kontroler — SZKIELET ćwiczeń 1-3 z lekcji Val02ValidInControllers
 * (opisy ćwiczeń: Val02ValidInControllers). Test: Val02ValidInControllersExercisesTest (@WebMvcTest).
 */
@RestController
@RequestMapping("/exercise")
public class ExerciseController {

    /** OrderRequest = zamówienie: klient i ilość (reguły są gotowe). */
    public record OrderRequest(@NotBlank String customer, @Positive int quantity) {
    }

    /** CustomerRequest = klient: nazwa i e-mail (reguły są gotowe). */
    public record CustomerRequest(@NotBlank String name, @Email String email) {
    }

    /** Ćwiczenie 1: ciało żądania ma być sprawdzane — złe dane mają dać 400. */
    @PostMapping("/orders")
    public Map<String, Object> createOrder(@RequestBody OrderRequest request) {   // TODO: dopisz brakującą adnotację
        return Map.of("customer", request.customer(), "quantity", request.quantity());
    }

    /** Ćwiczenie 2: parametry mają być sprawdzane — id co najmniej 1, quantity od 1 do 99; złe wartości → 400. */
    @GetMapping("/orders/{id}")
    public Map<String, Object> findOrder(@PathVariable long id,            // TODO: ograniczenie na id
                                         @RequestParam int quantity) {      // TODO: ograniczenie na quantity
        return Map.of("id", id, "quantity", quantity);
    }

    /** Ćwiczenie 3: ciało jest już sprawdzane (@Valid), ale brakuje obsługi błędu w JSON — dopisz poniżej @ExceptionHandler. */
    @PostMapping("/customers")
    public Map<String, Object> createCustomer(@Valid @RequestBody CustomerRequest request) {
        return Map.of("name", request.name());
    }

    // TODO: ćwiczenie 3 — dopisz metodę z @ExceptionHandler(MethodArgumentNotValidException.class)
    //       zwracającą 400 i JSON {"errors": {"pole": "komunikat", ...}}
}
