package s12_security.sec01_basics;

/**
 * SZKIELET ĆWICZEŃ 2 i 3 lekcji Sec01Basics: pomocnik, który czyta zalogowanego użytkownika z {@code SecurityContextHolder}
 * (schowka na kontekst bezpieczeństwa) — bez wstrzykiwania {@code Authentication} do metody kontrolera.
 * Zwykła klasa (nie bean), więc testujesz ją bez Springa: test ustawia kontekst ręcznie.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /**
     * Ćwiczenie 2: nazwa zalogowanego użytkownika albo {@code "anonim"}, gdy w kontekście nie ma uwierzytelnienia ({@code null})
     * albo jest tam {@code AnonymousAuthenticationToken} (token anonima).
     */
    public static String name() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 3: czy zalogowany użytkownik ma rolę {@code role} (podaną BEZ prefiksu, np. {@code "ADMIN"}).
     * Brak uwierzytelnienia = {@code false}.
     */
    public static boolean hasRole(String role) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
