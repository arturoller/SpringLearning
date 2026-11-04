package s12_security.sec05_jwt;

/**
 * SZKIELET ĆWICZENIA 1 lekcji Sec05Jwt: odczyt danych tokenu "gołymi rękami" — bez klucza i bez biblioteki JWT.
 */
public final class JwtParts {

    private JwtParts() {
    }

    /**
     * Ćwiczenie 1: zwróć środkową część tokenu (payload — dane) zdekodowaną z Base64URL jako tekst JSON (UTF-8).
     * Token, który nie ma DOKŁADNIE trzech części rozdzielonych kropką, to {@code IllegalArgumentException}.
     */
    public static String payloadJson(String token) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
