package s22_extras.solutions.ext03_oauth2_keycloak;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Ext03OAuth2Keycloak — zajrzyj dopiero po własnej próbie! */
public final class Ext03OAuth2KeycloakSolutions {

    private Ext03OAuth2KeycloakSolutions() {
    }

    static String solution1(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isBlank()) {
            return name;
        }
        String username = jwt.getClaimAsString("preferred_username");
        if (username != null && !username.isBlank()) {
            return username;
        }
        return jwt.getSubject();
    }

    static OAuth2TokenValidator<Jwt> solution2() {
        OAuth2Error notForUs = new OAuth2Error("invalid_token", "Token nie jest przeznaczony dla API sklepu", null);
        return jwt -> {
            List<String> audience = jwt.getAudience();              // pusta lista, gdy brak claimu "aud"
            boolean forOurApi = audience != null && audience.contains("sklep-api");
            boolean fromOurClient = "sklep-cli".equals(jwt.getClaimAsString("azp"));
            return forOurApi || fromOurClient
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(notForUs);
        };
    }

    static String solution3() {
        // [klucz] czyta z mapy claimów; klucz z myślnikiem — w apostrofach; .![...] = dla każdego elementu listy
        return "[resource_access]['sklep-api'][roles].![toUpperCase()]";
    }
}
