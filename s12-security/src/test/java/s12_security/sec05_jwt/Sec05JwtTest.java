package s12_security.sec05_jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Sec05Jwt. {@code RANDOM_PORT} + {@code java.net.http.HttpClient} — widzimy dokładnie to, co dostaje
 * klient: nagłówki WWW-Authenticate z opisem błędu tokenu. Tokeny wystawiamy prawdziwym punktem końcowym
 * POST /api/auth/token (sekcja 3), a „zepsute” tokeny — koderem z kontekstu albo cudzym kluczem.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Sec05JwtTest {

    static final String ERROR_URI = "error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\"";

    @LocalServerPort
    int port;

    @Autowired
    TokenService tokenService;

    @Autowired
    JwtEncoder jwtEncoder;

    final HttpClient http = HttpClient.newHttpClient();
    final JsonMapper json = JsonMapper.builder().build();

    HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    }

    HttpResponse<String> send(HttpRequest.Builder request) throws IOException, InterruptedException {
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Logowanie hasłem → token (tak jak zapytanie 1 w pliku .http). */
    String tokenFor(String user) throws IOException, InterruptedException {
        String basic = Base64.getEncoder().encodeToString((user + ":" + user + "-test-123").getBytes());
        HttpResponse<String> response = send(request("/api/auth/token")
                .header("Authorization", "Basic " + basic)
                .POST(HttpRequest.BodyPublishers.noBody()));
        assertThat(response.statusCode()).isEqualTo(200);
        return json.readTree(response.body()).get("access_token").asString();
    }

    HttpResponse<String> getWithToken(String path, String token) throws IOException, InterruptedException {
        return send(request(path).header("Authorization", "Bearer " + token));
    }

    JsonNode decodePart(String token, int part) {
        String partJson = new String(Base64.getUrlDecoder().decode(token.split("\\.")[part]), StandardCharsets.UTF_8);
        return json.readTree(partJson);
    }

    String resourceMetadata() {
        return "resource_metadata=\"http://localhost:" + port + "/.well-known/oauth-protected-resource\"";
    }

    @Test
    @DisplayName("1. JWT = nagłówek.ładunek.podpis; nagłówek i ładunek to tylko Base64URL — każdy je przeczyta")
    void tokenStructure() throws Exception {
        String token = tokenFor("anna");
        assertThat(token.split("\\.")).hasSize(3);

        JsonNode header = decodePart(token, 0);
        assertThat(header.get("alg").asString()).isEqualTo("RS256");
        assertThat(header.get("typ").asString()).isEqualTo("JWT");
        assertThat(header.has("kid")).as("identyfikator klucza").isTrue();

        JsonNode payload = decodePart(token, 1);
        assertThat(payload.get("iss").asString()).isEqualTo("https://auth.sklep.example");
        assertThat(payload.get("sub").asString()).isEqualTo("anna");
        assertThat(payload.get("scope").asString()).isEqualTo("products:read products:write");
        assertThat(payload.get("roles").get(0).asString()).isEqualTo("ADMIN");
        assertThat(payload.get("exp").asLong() - payload.get("iat").asLong()).as("ważny 1 godzinę").isEqualTo(3600);
    }

    @Test
    @DisplayName("3. POST /api/auth/token: hasło → access_token, token_type Bearer, expires_in 3600; bez hasła → 401 Basic")
    void tokenEndpoint() throws Exception {
        String basic = Base64.getEncoder().encodeToString("piotr:piotr-test-123".getBytes());
        HttpResponse<String> response = send(request("/api/auth/token")
                .header("Authorization", "Basic " + basic).POST(HttpRequest.BodyPublishers.noBody()));
        JsonNode body = json.readTree(response.body());
        assertThat(body.get("token_type").asString()).isEqualTo("Bearer");
        assertThat(body.get("expires_in").asInt()).isEqualTo(3600);
        assertThat(body.get("access_token").asString()).startsWith("eyJ");   // {" w Base64URL

        HttpResponse<String> noPassword = send(request("/api/auth/token").POST(HttpRequest.BodyPublishers.noBody()));
        assertThat(noPassword.statusCode()).isEqualTo(401);
        assertThat(noPassword.headers().firstValue("WWW-Authenticate")).hasValue("Basic realm=\"Realm\", charset=\"UTF-8\"");
    }

    @Test
    @DisplayName("2. Resource Server: bez tokenu → 401 i WWW-Authenticate: Bearer resource_metadata=...; z tokenem → 200")
    void resourceServerNeedsToken() throws Exception {
        HttpResponse<String> noToken = send(request("/api/products"));
        assertThat(noToken.statusCode()).isEqualTo(401);
        assertThat(noToken.headers().firstValue("WWW-Authenticate")).hasValue("Bearer " + resourceMetadata());
        assertThat(noToken.body()).isEmpty();

        assertThat(getWithToken("/api/products", tokenFor("piotr")).statusCode()).isEqualTo(200);

        // hasło (Basic) w łańcuchu Resource Server nic nie znaczy — liczy się tylko token
        String basic = Base64.getEncoder().encodeToString("anna:anna-test-123".getBytes());
        assertThat(send(request("/api/products").header("Authorization", "Basic " + basic)).statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("4. scope → SCOPE_..., roles → ROLE_..., a Security 7 dokłada FACTOR_BEARER; uwierzytelnienie — JwtAuthenticationToken")
    void claimsBecomeAuthorities() throws Exception {
        JsonNode me = json.readTree(getWithToken("/api/me", tokenFor("anna")).body());
        assertThat(me.get("subject").asString()).isEqualTo("anna");
        assertThat(me.get("issuer").asString()).isEqualTo("https://auth.sklep.example");
        assertThat(me.get("authenticationType").asString()).isEqualTo("JwtAuthenticationToken");
        assertThat(me.get("authorities").toString())
                .isEqualTo("[\"FACTOR_BEARER\",\"ROLE_ADMIN\",\"SCOPE_products:read\",\"SCOPE_products:write\"]");
    }

    @Test
    @DisplayName("4. Za mały zakres: piotr (products:read) POST → 403 i WWW-Authenticate: Bearer error=\"insufficient_scope\"")
    void insufficientScope() throws Exception {
        String piotr = tokenFor("piotr");
        HttpResponse<String> post = send(request("/api/products")
                .header("Authorization", "Bearer " + piotr)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"sku\":\"KSI-001\",\"name\":\"Czysty kod\"}")));
        assertThat(post.statusCode()).isEqualTo(403);
        assertThat(post.headers().firstValue("WWW-Authenticate")).hasValue("Bearer error=\"insufficient_scope\", "
                + "error_description=\"The request requires higher privileges than provided by the access token.\", "
                + ERROR_URI);

        HttpResponse<String> annaPost = send(request("/api/products")
                .header("Authorization", "Bearer " + tokenFor("anna"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"sku\":\"KSI-001\",\"name\":\"Czysty kod\"}")));
        assertThat(annaPost.statusCode()).isEqualTo(201);

        assertThat(getWithToken("/api/admin/report", piotr).statusCode()).as("brak ROLE_ADMIN").isEqualTo(403);
    }

    @Test
    @DisplayName("5. Token przeterminowany → 401, Bearer error=\"invalid_token\", „Jwt expired at ...”")
    void expiredToken() throws Exception {
        String expired = tokenService.issue("anna", "products:read", List.of("ADMIN"),
                Instant.parse("2020-01-01T10:00:00Z"), Duration.ofHours(1));

        HttpResponse<String> response = getWithToken("/api/products", expired);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("WWW-Authenticate")).hasValue("Bearer error=\"invalid_token\", "
                + "error_description=\"An error occurred while attempting to decode the Jwt: Jwt expired at "
                + "2020-01-01T11:00:00Z\", " + ERROR_URI + ", " + resourceMetadata());
    }

    @Test
    @DisplayName("5. Zmieniony ładunek → zły podpis; obcy wystawca → „The iss claim is not valid”; śmieci → „Malformed token”")
    void invalidTokens() throws Exception {
        String token = tokenFor("piotr");
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                .replace("\"scope\":\"products:read\"", "\"scope\":\"products:read products:write\"");
        String tampered = parts[0] + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];
        assertThat(getWithToken("/api/products", tampered).headers().firstValue("WWW-Authenticate"))
                .hasValueSatisfying(h -> assertThat(h).contains("Signed JWT rejected: Invalid signature"));

        Instant now = Instant.now();
        String foreignIssuer = jwtEncoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
                .issuer("https://zly.example").subject("anna").issuedAt(now).expiresAt(now.plusSeconds(600))
                .claim("scope", "products:read").build())).getTokenValue();
        assertThat(getWithToken("/api/products", foreignIssuer).headers().firstValue("WWW-Authenticate"))
                .hasValueSatisfying(h -> assertThat(h).contains("The iss claim is not valid"));

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair otherKeys = generator.generateKeyPair();   // np. aplikacja po restarcie albo cudzy serwer
        String otherKeyToken = NimbusJwtEncoder.withKeyPair((RSAPublicKey) otherKeys.getPublic(),
                (RSAPrivateKey) otherKeys.getPrivate()).build().encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
                .issuer(Sec05JwtConfig.ISSUER).subject("anna").issuedAt(now).expiresAt(now.plusSeconds(600))
                .claim("scope", "products:read").build())).getTokenValue();
        HttpResponse<String> other = getWithToken("/api/products", otherKeyToken);
        assertThat(other.statusCode()).isEqualTo(401);
        assertThat(other.headers().firstValue("WWW-Authenticate"))
                .hasValueSatisfying(h -> assertThat(h).contains("Signed JWT rejected: Invalid signature"));

        assertThat(getWithToken("/api/products", "abc.def.ghi").headers().firstValue("WWW-Authenticate"))
                .hasValueSatisfying(h -> assertThat(h).contains("An error occurred while attempting to decode the Jwt: Malformed token"));
    }
}
