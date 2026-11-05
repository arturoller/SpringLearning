package s12_security.sec05_jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 1 lekcji Sec05Jwt: budowa tokenu JWT — zwykły test jednostkowy, bez Springa. Zegar zatrzymany
 * ({@code Clock.fixed}), więc daty w tokenie są zawsze te same. Klucz losowy, TYLKO DO NAUKI.
 */
class Sec05JwtAnatomyTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");
    private static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final Sec05SecurityConfig config = new Sec05SecurityConfig();

    private static String issueForAnna(SecretKey key) {
        TokenService service = new TokenService(NimbusJwtEncoder.withSecretKey(key).build(), FIXED);
        return service.issue(new UsernamePasswordAuthenticationToken("anna", null,
                AuthorityUtils.createAuthorityList("ROLE_CUSTOMER", "FACTOR_PASSWORD"))).accessToken();
    }

    private static JsonNode part(String token, int index) {
        byte[] json = Base64.getUrlDecoder().decode(token.split("\\.")[index]);   // Base64URL — bez żadnego klucza!
        return JSON.readTree(new String(json, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Sekcja 1: token to trzy części Base64URL: nagłówek (alg HS256), dane (iss, sub, iat, exp, roles) i podpis")
    void threeParts() throws Exception {
        String token = issueForAnna(config.jwtSecretKey());
        assertThat(token.split("\\.")).hasSize(3);

        JsonNode header = part(token, 0);
        assertThat(header.get("alg").asString()).isEqualTo("HS256");

        JsonNode payload = part(token, 1);
        assertThat(payload.get("iss").asString()).isEqualTo("sklep-sec05");
        assertThat(payload.get("sub").asString()).isEqualTo("anna");
        assertThat(payload.get("iat").asLong()).isEqualTo(NOW.getEpochSecond());              // sekundy od 1970-01-01
        assertThat(payload.get("exp").asLong() - payload.get("iat").asLong()).isEqualTo(900);  // 15 minut
        assertThat(payload.get("roles").toString()).isEqualTo("[\"CUSTOMER\"]");                // bez FACTOR_PASSWORD
    }

    @Test
    @DisplayName("Sekcja 1 (pułapka): dane tokenu przeczyta każdy — Base64URL to zapis, nie szyfr; nie wkładaj tam tajemnic")
    void payloadIsNotEncrypted() throws Exception {
        String token = issueForAnna(config.jwtSecretKey());
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        assertThat(payload).contains("\"sub\":\"anna\"", "\"roles\":[\"CUSTOMER\"]");
    }

    @Test
    @DisplayName("Sekcja 1: podpis chroni przed zmianą — anna dopisuje sobie rolę ADMIN i dekoder odrzuca token (Invalid signature)")
    void tamperedPayloadIsRejected() throws Exception {
        SecretKey key = config.jwtSecretKey();
        String[] parts = issueForAnna(key).split("\\.");
        String forgedPayload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                .replace("\"CUSTOMER\"", "\"ADMIN\"");
        String forged = parts[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(forgedPayload.getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];                                       // stary podpis — nowego anna nie policzy bez klucza

        JwtDecoder decoder = config.jwtDecoder(key, FIXED);
        assertThatThrownBy(() -> decoder.decode(forged))
                .isInstanceOf(BadJwtException.class)
                .hasMessageContaining("Invalid signature");
        Jwt original = decoder.decode(String.join(".", parts));          // oryginał dalej jest w porządku
        assertThat(original.getClaimAsStringList("roles")).containsExactly("CUSTOMER");
    }

    @Test
    @DisplayName("Sekcja 1: token podpisany INNYM kluczem (np. z innej aplikacji) też jest odrzucany")
    void foreignKeyIsRejected() throws Exception {
        String foreign = issueForAnna(config.jwtSecretKey());
        JwtDecoder decoder = config.jwtDecoder(config.jwtSecretKey(), FIXED);    // drugi, inny losowy klucz
        assertThatThrownBy(() -> decoder.decode(foreign)).hasMessageContaining("Invalid signature");
    }
}
