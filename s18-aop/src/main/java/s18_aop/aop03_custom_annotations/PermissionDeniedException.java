package s18_aop.aop03_custom_annotations;

/** Wyjątek „brak uprawnień” (permission denied = odmowa dostępu) — do ćwiczenia 1. */
public class PermissionDeniedException extends RuntimeException {

    public PermissionDeniedException(String message) {
        super(message);
    }
}
