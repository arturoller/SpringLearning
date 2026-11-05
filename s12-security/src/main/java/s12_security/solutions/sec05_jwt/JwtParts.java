package s12_security.solutions.sec05_jwt;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Rozwiązanie wzorcowe ćwiczenia 1 lekcji Sec05Jwt. */
public final class JwtParts {

    private JwtParts() {
    }

    public static String payloadJson(String token) {
        String[] parts = token.split("\\.", -1);     // -1: nie gub pustych części (np. "a.b.")
        if (parts.length != 3) {
            throw new IllegalArgumentException("Token JWT ma 3 części, a ten ma " + parts.length);
        }
        return new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
    }
}
