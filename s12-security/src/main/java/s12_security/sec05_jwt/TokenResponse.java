package s12_security.sec05_jwt;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Odpowiedź {@code POST /sec05/token} w formacie znanym z OAuth 2 (RFC 6749): sam token, jego typ ("Bearer" = "okaziciel" —
 * kto ma token, ten wchodzi) i czas życia w sekundach. Nazwy pól z podkreśleniem, bo tak wygląda standard.
 */
public record TokenResponse(@JsonProperty("access_token") String accessToken,
                            @JsonProperty("token_type") String tokenType,
                            @JsonProperty("expires_in") long expiresIn) {
}
