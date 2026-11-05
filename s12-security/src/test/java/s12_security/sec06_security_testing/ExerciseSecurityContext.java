package s12_security.sec06_security_testing;

import java.util.function.Supplier;

/**
 * SZKIELET ĆWICZENIA 4 lekcji Sec06SecurityTesting: to, co w środku robi {@code @WithMockUser} — napisane samodzielnie.
 */
public final class ExerciseSecurityContext {

    private ExerciseSecurityContext() {
    }

    /**
     * Ćwiczenie 4: wykonaj {@code action} jako zalogowany użytkownik {@code username} z jedną rolą {@code role} (bez prefiksu
     * ROLE_), a potem — ZAWSZE, także po wyjątku — przywróć poprzedni kontekst bezpieczeństwa.
     */
    public static <T> T runAs(String username, String role, Supplier<T> action) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
