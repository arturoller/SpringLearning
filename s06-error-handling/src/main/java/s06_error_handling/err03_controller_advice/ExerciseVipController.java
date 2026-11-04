package s06_error_handling.err03_controller_advice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ćwiczenie 3: kontroler klientów VIP. Rzuć {@link ExerciseController.CustomerNotFoundException} i dopisz TUTAJ lokalny
 * {@code @ExceptionHandler}, który wygra z handlerem z {@link ExerciseAdvice}.
 */
@RestController
@RequestMapping("/err03/cw/vip")
public class ExerciseVipController {

    @GetMapping("/{id}")
    public String vip(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // TODO (ćwiczenie 3): lokalny handler dla CustomerNotFoundException → 404, code "VIP_NOT_FOUND", message "Nie ma klienta VIP <id>"
    //   (id weź z ostatniego fragmentu adresu albo z komunikatu wyjątku — możesz też dodać getter do wyjątku, ale nie jest to wymagane).
}
