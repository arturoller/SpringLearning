package s18_aop.aop03_custom_annotations;

/**
 * Bieżący użytkownik (do ćwiczenia 1) — sama rola, którą test może zmienić. Uproszczenie: w prawdziwej aplikacji
 * zalogowanego użytkownika zna Spring Security.
 */
public class CurrentUser {

    private String role;

    public CurrentUser(String role) {
        this.role = role;
    }

    public String role() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
