package s12_security.sec05_jwt;

import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Duration;
import java.time.Instant;

/**
 * <pre>
 * TEMAT: JWT — budowa tokenu, wystawianie (JwtEncoder), sprawdzanie (Resource Server, JwtDecoder), scope a uprawnienia,
 *        token przeterminowany (JWT = JSON Web Token, „żeton sieciowy w JSON”; claim = twierdzenie/pole tokenu)
 *
 * W SKRÓCIE:
 *   • JWT to trzy części rozdzielone kropkami: nagłówek.ładunek.podpis. Nagłówek i ładunek to JSON zakodowany Base64URL
 *     — każdy może je PRZECZYTAĆ. Podpis gwarantuje, że nikt ich nie ZMIENIŁ.
 *   • Klient raz wymienia login i hasło na token (POST /api/auth/token), a potem wysyła tylko nagłówek
 *     Authorization: Bearer token. Serwer nie trzyma sesji — wszystko, czego potrzebuje, jest w tokenie.
 *   • Resource Server (serwer zasobów): http.oauth2ResourceServer(rs -> rs.jwt(...)) + bean JwtDecoder, który sprawdza
 *     podpis, ważność (exp) i wystawcę (iss).
 *   • Claim „scope” zamienia się na uprawnienia SCOPE_...: hasAuthority("SCOPE_products:write").
 *   • Token przeterminowany, podrobiony albo z obcym wystawcą → 401 i opis w nagłówku WWW-Authenticate: Bearer.
 *
 * ANALOGIA: bilet na koncert z hologramem.
 *   Kasa (serwer tożsamości) sprawdza dowód i wydaje bilet: imię, sektor, data ważności — napisane jawnie, każdy
 *   może przeczytać. Hologram (podpis) robi tylko kasa i nie da się go podrobić. Bramkarz (Resource Server) nie dzwoni
 *   do kasy przy każdym wejściu: patrzy na hologram, datę i sektor. Zmienisz długopisem sektor na VIP — hologram nie
 *   będzie pasował. Zgubiony bilet działa do końca ważności — dlatego bilety są krótkie.
 *
 * JAK TO DZIAŁA:
 *   1. POST /api/auth/token + Authorization: Basic (anna:hasło) → tokenChain → TokenService → JwtEncoder (klucz
 *      PRYWATNY) → {"access_token":"eyJ...","token_type":"Bearer","expires_in":3600}
 *   2. GET /api/products + Authorization: Bearer eyJ... → resourceServerChain → BearerTokenAuthenticationFilter
 *      → JwtDecoder (klucz PUBLICZNY: podpis; walidatory: typ, exp/nbf z tolerancją 60 s, iss)
 *      → JwtAuthenticationConverter (scope → SCOPE_..., roles → ROLE_..., + FACTOR_BEARER)
 *      → JwtAuthenticationToken w SecurityContextHolder → AuthorizationFilter (hasAuthority("SCOPE_products:read"))
 *   3. Błąd tokenu → BearerTokenAuthenticationEntryPoint → 401 + WWW-Authenticate: Bearer error="invalid_token", ...
 *
 * SŁÓWKA:
 *   token = żeton; bearer = okaziciel („kto ma, ten wchodzi”); claim = twierdzenie (pole ładunku); issuer (iss) = wystawca;
 *   subject (sub) = podmiot (kogo dotyczy); issued at (iat) = wystawiony o; expires at (exp) = ważny do;
 *   scope = zakres (uprawnienia tokenu); signature = podpis; key pair = para kluczy (prywatny podpisuje, publiczny
 *   sprawdza); encoder/decoder = koder/dekoder; resource server = serwer zasobów (API, które przyjmuje tokeny);
 *   identity provider / authorization server = serwer tożsamości/autoryzacji (wydaje tokeny); clock skew = rozjazd zegarów.
 *
 * ZOBACZ TEŻ: s12_security/Sec04StatelessApi (API bez sesji, CSRF), s12_security/Sec02Authorization (hasAuthority
 *             a hasRole), t31_jdk_toolbox/Toolbox01UuidBase64 (Base64), t31_jdk_toolbox/Toolbox02HashingSecurity
 *             (skróty i podpisy), t17_datetime/DateTime04ZonesInstant (Instant i Clock), s22_extras (OAuth2, Keycloak).
 * </pre>
 */
public final class Sec05Jwt {

    private Sec05Jwt() {
    }

    // =================================================================================================
    // 1. BUDOWA JWT — nagłówek.ładunek.podpis
    // =================================================================================================
    //
    // Token anny z prawdziwego uruchomienia (skrócony):
    //   eyJraWQiOiIzTGp2ZFp4Z0Rfc254...IiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ   ← nagłówek
    //   .eyJpc3MiOiJodHRwczovL2F1dGguc2tsZXAuZXhhbXBsZSIsInN1YiI6ImFubmEi...    ← ładunek
    //   .RyKyHWNwGHrSQtynMmwl_ylA0RY1z8cEiA9Qu_k0gpz1fdHZpci5igLQWDlH6xXo...    ← podpis
    // Po zdekodowaniu Base64URL (Sec05JwtTest.tokenStructure):
    //   nagłówek: {"kid":"3LjvdZxgD_snxjc3T-45FGVY95int-RO8E6QpSMccrA","typ":"JWT","alg":"RS256"}
    //             kid = identyfikator klucza, typ = typ tokenu, alg = algorytm podpisu
    //   ładunek:  {"iss":"https://auth.sklep.example","sub":"anna","exp":1792405478,"iat":1792401878,
    //              "scope":"products:read products:write","roles":["ADMIN"]}
    // exp i iat to sekundy od 1970-01-01 (czas uniksowy); exp - iat = 3600 (godzina). Podpis RS256 = podpis RSA
    // ze skrótu SHA-256 tekstu „nagłówek.ładunek”, złożony kluczem PRYWATNYM; sprawdza się go kluczem PUBLICZNYM.
    //
    // PUŁAPKA: ładunek NIE jest zaszyfrowany — to tylko Base64URL. Nie wkładaj do tokenu haseł, numerów PESEL, pensji ani
    //   niczego tajnego. Dlaczego? Token trafia do przeglądarki, logów, narzędzi deweloperskich; każdy go odczyta
    //   (choćby na stronie jwt.io). Podpis chroni przed ZMIANĄ, nie przed ODCZYTEM.
    //
    // DOBRA PRAKTYKA: w tokenie tylko to, czego API potrzebuje do decyzji: kto (sub), co mu wolno (scope/roles), do kiedy
    //   (exp), kto wydał (iss). Dlaczego? Mały token = mniej danych do wycieku i krótsze nagłówki w każdym zapytaniu.

    // =================================================================================================
    // 2. RESOURCE SERVER — sprawdzanie tokenu
    // =================================================================================================
    //
    //   http.oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
    //   @Bean JwtDecoder jwtDecoder(...) {
    //       NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
    //       decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(timestamps, new JwtIssuerValidator(ISSUER)));
    //   }
    // Filtr BearerTokenAuthenticationFilter wyjmuje token z nagłówka Authorization: Bearer ..., dekoder sprawdza podpis
    // i walidatory: typ tokenu, czas (exp/nbf, tolerancja 60 s na rozjechane zegary) i wystawcę (iss).
    //
    // Bez tokenu → 401, puste ciało i nagłówek (Sec05JwtTest):
    //   WWW-Authenticate: Bearer resource_metadata="http://localhost:8080/.well-known/oauth-protected-resource"
    // Parametr resource_metadata to nowość Security 7 (RFC 9728 — klient może tam sprawdzić, skąd wziąć token);
    // starsze poradniki pokazują samo „Bearer”. Nagłówek Basic w tym łańcuchu nic nie znaczy — też 401.
    //
    // Boot umie zbudować JwtDecoder sam z ustawień spring.security.oauth2.resourceserver.jwt.issuer-uri, jwk-set-uri
    // albo public-key-location — tak się robi przy prawdziwym serwerze tożsamości. My mamy klucz w pamięci, więc bean
    // piszemy ręcznie. Bean JwtDecoder wyłącza też użytkownika „user” z wygenerowanym hasłem (Sec01Basics).

    // =================================================================================================
    // 3. WYSTAWIANIE TOKENU — JwtEncoder i punkt końcowy logowania
    // =================================================================================================
    //
    //   JwtEncoder encoder = NimbusJwtEncoder.withKeyPair(publicKey, privateKey).build();   // Security 7: gotowy budowniczy
    //   JwtClaimsSet claims = JwtClaimsSet.builder().issuer(ISSUER).subject("anna")
    //           .issuedAt(now).expiresAt(now.plus(1h)).claim("scope", "products:read products:write").build();
    //   String token = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();   // nagłówek: RS256, typ JWT, kid
    // Starsze poradniki budują RSAKey + ImmutableJWKSet + new NimbusJwtEncoder(jwkSource) — to nadal działa, ale
    // withKeyPair(...) robi to samo jedną linijką.
    //
    // POST /api/auth/token (tokenChain: HTTP Basic, bez sesji) → TokenService.issue(authentication) → JSON
    // {"access_token":"eyJ...","expires_in":3600,"token_type":"Bearer"}. Czas „teraz” bierzemy z beana Clock, nie
    // z Instant.now() — test z zatrzymanym zegarem (Sec05JwtClockTest) wystawia token „z 2020 roku” i dekoder, używający
    // tego samego zegara, uznaje go za ważny.
    //
    // Klucz: para RSA generowana przy starcie (Sec05JwtConfig.jwtKeyPair) — w repozytorium nie ma żadnego pliku z kluczem.
    // Skutek uboczny: po restarcie stare tokeny są nieważne (inny klucz → „Invalid signature”).
    // Alternatywa: tajny klucz HMAC (NimbusJwtEncoder.withSecretKey / NimbusJwtDecoder.withSecretKey) — ten sam sekret
    // podpisuje i sprawdza, więc KAŻDY, kto umie sprawdzać, umie też podrabiać. RSA jest lepsze, gdy tokeny sprawdza
    // kilka usług: dostają tylko klucz publiczny.
    //
    // DOBRA PRAKTYKA: w prawdziwym systemie tokeny wydaje serwer tożsamości (Keycloak, Spring Authorization Server,
    //   Auth0...), a aplikacja jest TYLKO Resource Serverem i zna tylko klucz publiczny (pobiera go z jwk-set-uri).
    //   Dlaczego? Logowanie, hasła, MFA, odświeżanie i unieważnianie tokenów są w jednym, dobrze pilnowanym miejscu
    //   (zapowiedź: s22_extras). Nasz /api/auth/token pokazuje mechanikę — nie jest wzorem produkcyjnym.

    // =================================================================================================
    // 4. CLAIMY → UPRAWNIENIA — JwtAuthenticationConverter, prefiks SCOPE_
    // =================================================================================================
    //
    // Domyślnie (JwtGrantedAuthoritiesConverter): claim „scope” (napis rozdzielany spacjami) albo „scp” → uprawnienia
    // z prefiksem SCOPE_. Dodaliśmy drugi konwerter: claim „roles” → ROLE_..., złożone przez
    // DelegatingJwtGrantedAuthoritiesConverter. Uprawnienia anny (Sec05JwtTest, /api/me):
    //   [FACTOR_BEARER, ROLE_ADMIN, SCOPE_products:read, SCOPE_products:write]
    // FACTOR_BEARER dopisuje sam Security 7 („zalogowany tokenem Bearer” — jak FACTOR_PASSWORD w Sec03UsersPasswords).
    // Principal to obiekt Jwt (@AuthenticationPrincipal Jwt jwt), a uwierzytelnienie — JwtAuthenticationToken.
    // Bean JwtAuthenticationConverter Security znalazłby sam; podajemy go jawnie, żeby było widać, skąd biorą się uprawnienia.
    //
    //   piotr (scope products:read) → POST /api/products → 403 i
    //   WWW-Authenticate: Bearer error="insufficient_scope", error_description="The request requires higher privileges
    //   than provided by the access token.", error_uri="https://tools.ietf.org/html/rfc6750#section-3.1"
    // (ten sam opis „insufficient_scope” dostaniesz też przy braku ROLE_ADMIN — tak działa BearerTokenAccessDeniedHandler).
    //
    // PUŁAPKA: hasRole("ADMIN") przy tokenie z samym scope → 403 dla wszystkich. Dlaczego? Domyślny konwerter tworzy tylko
    //   SCOPE_...; ROLE_ADMIN pojawi się dopiero, gdy sam zamienisz claim z rolami (jak nasz konwerter „roles”).
    //
    // PUŁAPKA: jwt.getIssuer() zwraca URL. Gdyby iss było zwykłym napisem (np. „sklep-lekcja”), wywołanie rzuci
    //   IllegalArgumentException „Unable to convert claim 'iss' of type 'class java.lang.String' to URL.” — a klient
    //   dostanie 500 (sprawdzone uruchomieniem). Dlatego ISSUER to adres (https://auth.sklep.example), a dla dowolnego
    //   napisu bezpieczniej użyć jwt.getClaimAsString("iss").

    // =================================================================================================
    // 5. WAŻNOŚĆ I BŁĘDY TOKENU
    // =================================================================================================
    //
    // Każdy błąd tokenu → 401, puste ciało, opis w nagłówku (skopiowane z Sec05JwtTest):
    //   przeterminowany: Bearer error="invalid_token", error_description="An error occurred while attempting to decode
    //                    the Jwt: Jwt expired at 2020-01-01T11:00:00Z", error_uri="...", resource_metadata="..."
    //   zmieniony ładunek:  ... Signed JWT rejected: Invalid signature
    //   obcy wystawca:      ... The iss claim is not valid
    //   inny klucz:         ... Signed JWT rejected: Invalid signature (np. token sprzed restartu aplikacji)
    //   śmieci:             ... Malformed token
    // Tolerancja 60 s: token jest odrzucany dopiero, gdy „teraz minus 60 s” jest po exp (JwtTimestampValidator) — serwery
    // rzadko mają idealnie zgodne zegary.
    //
    // PUŁAPKA: wydanego tokenu nie da się łatwo „wylogować” — działa do exp, nawet po zmianie hasła czy zablokowaniu konta.
    //   Dlatego tokeny dostępu żyją krótko (minuty, godzina), a długie sesje robi się tokenem odświeżania (refresh
    //   token) wydawanym przez serwer tożsamości.
    //
    // DOBRA PRAKTYKA: token tylko przez HTTPS i tylko w nagłówku Authorization — nigdy w adresie (?token=...). Dlaczego?
    //   Adresy lądują w logach serwerów, historii przeglądarki i nagłówku Referer; kto ma token, ten jest Tobą (bearer).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • JWT = Base64URL(nagłówek).Base64URL(ładunek).podpis — czytelny dla każdego, podpis chroni przed zmianą.
     *   • Claimy: iss (wystawca), sub (kto), iat (wystawiony), exp (ważny do), scope (zakres), własne (roles).
     *   • Wystawianie: NimbusJwtEncoder.withKeyPair(pub, priv).build(); encoder.encode(JwtEncoderParameters.from(claims)).
     *   • Sprawdzanie: http.oauth2ResourceServer(rs -> rs.jwt(...)) + JwtDecoder (NimbusJwtDecoder.withPublicKey(...));
     *     walidatory: JwtValidators.createDefaultWithValidators(new JwtTimestampValidator(...), new JwtIssuerValidator(iss)).
     *   • scope → SCOPE_x (hasAuthority("SCOPE_x")); własne claimy → JwtGrantedAuthoritiesConverter + prefiks;
     *     Security 7 dodaje FACTOR_BEARER.
     *   • Bez tokenu: 401 + Bearer resource_metadata="..."; zły token: invalid_token; za mały zakres: 403 insufficient_scope.
     *   • Czas z beana Clock (wystawca i JwtTimestampValidator.setClock) — testowalne bez czekania.
     *   • Produkcja: tokeny wydaje serwer tożsamości; aplikacja zna tylko klucz publiczny; krótkie exp; tylko HTTPS.
     *
     * PYTANIA KONTROLNE:
     *   1. Z jakich trzech części składa się JWT i która z nich chroni przed podrobieniem?
     *   2. ZNAJDŹ BŁĄD: TokenService dopisuje do tokenu .claim("password", haslo), „żeby API mogło sprawdzić hasło”.
     *   3. Co się stanie? Weźmiesz token piotra, zmienisz w ładunku scope na „products:read products:write” (Base64URL)
     *      i wyślesz POST /api/products.
     *   4. Co się stanie? Zrestartujesz Sec05Application i użyjesz tokenu wydanego przed restartem.
     *   5. Dlaczego reguła hasAuthority("SCOPE_products:write") działa, a hasRole("products:write") — nie?
     *   6. Co się stanie? Token z exp = 2020-01-01T11:00:00Z wyślesz dziś do /api/products. Jaki status i jaki nagłówek?
     *   7. Po co bean Clock, skoro można napisać Instant.now()?
     *   8. Dlaczego w prawdziwym systemie aplikacja nie powinna sama wydawać tokenów, tylko je sprawdzać?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Sec05JwtExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć ładunek (drugą część) tokenu JWT jako tekst JSON — bez sprawdzania podpisu.
     * Przykład: "eyJhbGciOiJub25lIn0.eyJzdWIiOiJhbm5hIn0.x" → {"sub":"anna"}. Zły format (nie 3 części) →
     * IllegalArgumentException.
     * Podpowiedź: split("\\."), Base64.getUrlDecoder(), new String(bajty, StandardCharsets.UTF_8).
     */
    static String exercise1(String token) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): czy token jest przeterminowany — tak jak liczy JwtTimestampValidator: przeterminowany,
     * gdy (now minus clockSkew) jest PO expiresAt. Przykład: exp 12:00:00, skew 60 s: now 12:00:59 → false,
     * now 12:01:01 → true.
     * Podpowiedź: Instant.minus(Duration), isAfter.
     */
    static boolean exercise2(Instant expiresAt, Instant now, Duration clockSkew) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zbuduj JwtAuthenticationConverter, który bierze uprawnienia z claimu „permissions”
     * (lista napisów) BEZ żadnego prefiksu: ["report:read"] → uprawnienie „report:read”.
     * Podpowiedź: Sec05JwtConfig.jwtAuthenticationConverter — setAuthoritiesClaimName i setAuthorityPrefix("").
     * Zauważ w teście: Security 7 i tak dopisze FACTOR_BEARER.
     */
    static JwtAuthenticationConverter exercise3() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): wystaw token koderem {@code encoder}: iss = Sec05JwtConfig.ISSUER, sub = username,
     * iat = now, exp = now + 15 minut, scope = "products:read". Zwróć sam tekst tokenu.
     * Podpowiedź: TokenService.issue(...) — JwtClaimsSet.builder() i JwtEncoderParameters.from(...).
     */
    static String exercise4(JwtEncoder encoder, String username, Instant now) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Nagłówek (algorytm, typ, kid), ładunek (claimy) i podpis. Przed podrobieniem chroni podpis: liczony kluczem
     *      prywatnym z nagłówka i ładunku, sprawdzany kluczem publicznym.
     *   2. Ładunek nie jest szyfrowany — hasło przeczyta każdy, kto zobaczy token (logi, przeglądarka, jwt.io). Tokenu
     *      i tak używa się ZAMIAST hasła; API nie potrzebuje hasła, tylko podpisanego „kto” i „co mu wolno”.
     *   3. 401 — zmieniony ładunek nie pasuje do podpisu: WWW-Authenticate: Bearer error="invalid_token",
     *      error_description="An error occurred while attempting to decode the Jwt: Signed JWT rejected: Invalid
     *      signature", ... Sprawdzone testem Sec05JwtTest.invalidTokens.
     *   4. 401 i „Signed JWT rejected: Invalid signature” — przy starcie powstała nowa para kluczy, więc podpis starego
     *      tokenu nie pasuje do nowego klucza publicznego. Sprawdzone testem Sec05JwtTest.invalidTokens (token podpisany
     *      innym kluczem).
     *   5. Domyślny konwerter zamienia scope na uprawnienie „SCOPE_products:write”. hasRole("products:write") szuka
     *      „ROLE_products:write” — takiego uprawnienia nie ma.
     *   6. 401 i nagłówek: Bearer error="invalid_token", error_description="An error occurred while attempting to decode
     *      the Jwt: Jwt expired at 2020-01-01T11:00:00Z", error_uri="https://tools.ietf.org/html/rfc6750#section-3.1",
     *      resource_metadata="http://localhost:8080/.well-known/oauth-protected-resource". Sprawdzone testem
     *      Sec05JwtTest.expiredToken.
     *   7. Z beanem Clock test może zatrzymać czas (Clock.fixed) i sprawdzić ważność tokenu bez czekania i bez zależności
     *      od dzisiejszej daty — Sec05JwtClockTest. Instant.now() w logice nie da się podmienić.
     *   8. Wydawanie tokenów wymaga przechowywania haseł, kluczy prywatnych, odświeżania i unieważniania tokenów, MFA —
     *      lepiej mieć to raz, w wyspecjalizowanym serwerze tożsamości. Aplikacje-API znają tylko klucz publiczny, więc
     *      włamanie do jednej z nich nie pozwala podrabiać tokenów.
     */
    // </editor-fold>
}
