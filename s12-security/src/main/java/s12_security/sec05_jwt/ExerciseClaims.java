package s12_security.sec05_jwt;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;

import java.time.Instant;
import java.util.List;

/**
 * SZKIELET ĆWICZENIA 2 lekcji Sec05Jwt: zestaw twierdzeń (claims) dla tokenu ćwiczeń.
 */
public final class ExerciseClaims {

    private ExerciseClaims() {
    }

    /**
     * Ćwiczenie 2: wystawca {@code "sklep-cw"}, podmiot = {@code username}, wystawiony {@code now}, ważny 10 minut,
     * twierdzenie {@code roles} = podana lista.
     */
    public static JwtClaimsSet claimsFor(String username, List<String> roles, Instant now) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
