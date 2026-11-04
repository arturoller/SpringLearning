package s12_security.sec02_authorization;

import org.springframework.stereotype.Service;

/**
 * SZKIELET ĆWICZENIA 3 lekcji Sec02Authorization: bezpieczeństwo metod. Dopisz adnotacje {@code @PreAuthorize} i treść metod.
 * Test (Sec02MethodSecurityExercisesTest) woła ten bean przez proxy, z różnymi użytkownikami z {@code @WithMockUser}.
 */
@Service
public class ExerciseOrderService {

    /**
     * Ćwiczenie 3a: zwróć {@code "Anulowano zamówienie " + id + " klienta " + owner}. Wolno: sam właściciel ({@code owner})
     * albo ADMIN.
     */
    public String cancel(String owner, long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 3b: zwróć {@code "Zwrot " + amount + " zł"}. Wolno tylko roli EMPLOYEE (w teście ADMIN NIE ma hierarchii). */
    public String refund(int amount) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
