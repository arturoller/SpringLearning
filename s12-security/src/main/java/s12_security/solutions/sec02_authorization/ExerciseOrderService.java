package s12_security.solutions.sec02_authorization;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/** Rozwiązanie wzorcowe ćwiczenia 3 lekcji Sec02Authorization. */
@Service
public class ExerciseOrderService {

    @PreAuthorize("#owner == authentication.name or hasRole('ADMIN')")
    public String cancel(String owner, long id) {
        return "Anulowano zamówienie " + id + " klienta " + owner;
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    public String refund(int amount) {
        return "Zwrot " + amount + " zł";
    }
}
