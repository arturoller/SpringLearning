package s22_extras.ext03_oauth2_keycloak;

import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * <pre>
 * TEMAT: OAuth2 i OpenID Connect w skrócie — aplikacja jako Resource Server, Keycloak jako serwer tożsamości,
 *        role Keycloaka zamieniane na uprawnienia Springa, testy z jwt()
 *        (OAuth2 = standard delegowania dostępu tokenami; OIDC = OpenID Connect, warstwa tożsamości nad OAuth2;
 *        Keycloak = darmowy serwer tożsamości; resource server = serwer zasobów — API, które przyjmuje tokeny)
 *
 * W SKRÓCIE:
 *   • Logowaniem zajmuje się OSOBNY serwer (Keycloak): zna hasła, wydaje tokeny JWT podpisane swoim kluczem.
 *     Nasza aplikacja (Resource Server) tylko SPRAWDZA tokeny — nie zna haseł ani klucza prywatnego.
 *   • Jedna właściwość: spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/sklep —
 *     Boot pobiera z niej (leniwie) klucze publiczne i sprawdza podpis, ważność i wystawcę (iss).
 *   • Keycloak trzyma role w claimie realm_access.roles — Spring sam go nie czyta. Konwerter zamienia
 *     ["klient"] na ROLE_KLIENT, więc działa hasRole("KLIENT").
 *   • Testy: jwt() z spring-security-test podstawia gotowe uwierzytelnienie — bez Keycloaka. UWAGA: pomija Twój
 *     konwerter. Pełną ścieżkę (dekoder + konwerter) sprawdza test z kluczem testowym.
 *
 * ANALOGIA: hotel i recepcja.
 *   Recepcja (Keycloak) sprawdza dowód osobisty i wydaje kartę-klucz z zapisanymi uprawnieniami („pokój 12, siłownia”).
 *   Drzwi pokoju i siłowni (Resource Server) nie znają Twojego dowodu i nie dzwonią na recepcję przy każdym wejściu —
 *   sprawdzają tylko, czy karta jest prawdziwa (podpis), ważna (exp) i z TEGO hotelu (iss). Karta z hotelu obok
 *   (inny wystawca) nie otworzy drzwi, nawet jeśli jest prawdziwa.
 *
 * JAK TO DZIAŁA:
 *   1. Klient (aplikacja webowa, mobilna, plik .http) loguje użytkownika w Keycloaku → dostaje access token (JWT).
 *   2. GET /api/me + Authorization: Bearer eyJ... → BearerTokenAuthenticationFilter
 *      → JwtDecoder (Boot z issuer-uri; SupplierJwtDecoder — przy PIERWSZYM tokenie pobiera
 *        {issuer-uri}/.well-known/openid-configuration i klucze JWKS) → podpis, exp/nbf, iss
 *      → JwtAuthenticationConverter (nasz bean): SCOPE_... ze „scope” + ROLE_... z realm_access.roles
 *        + FACTOR_BEARER (Security 7), nazwa z preferred_username
 *      → JwtAuthenticationToken → AuthorizationFilter: hasRole("PRACOWNIK")? → kontroler albo 403.
 *   3. Brak/zły token → 401 + WWW-Authenticate: Bearer ...; za małe uprawnienia → 403 + error="insufficient_scope".
 *
 * SŁÓWKA:
 *   authorization server / identity provider = serwer autoryzacji / dostawca tożsamości (wydaje tokeny);
 *   realm = dziedzina (w Keycloaku: osobny „świat” użytkowników, ról i klientów); client = klient (aplikacja, która
 *   prosi o token); grant / flow = sposób uzyskania tokenu (przepływ); issuer (iss) = wystawca; JWKS = zestaw kluczy
 *   publicznych wystawcy; discovery = odkrywanie konfiguracji (/.well-known/openid-configuration); claim = pole tokenu;
 *   scope = zakres; audience (aud) = odbiorca tokenu; bearer = okaziciel; granted authority = przyznane uprawnienie.
 *
 * ZOBACZ TEŻ: s12_security/Sec05Jwt (JWT od środka — tam aplikacja SAMA wydaje tokeny), s12_security/Sec02Authorization
 *             (hasRole a hasAuthority), s12_security/Sec06SecurityTesting (testy zabezpieczeń),
 *             s12_security/Sec04StatelessApi (API bez sesji), t09_records/Records01Basics.
 * </pre>
 */
public final class Ext03OAuth2Keycloak {

    private Ext03OAuth2Keycloak() {
    }

    // =================================================================================================
    // 1. OAuth2 I OIDC W SKRÓCIE — KTO JEST KIM
    // =================================================================================================
    //
    //   Użytkownik (resource owner)  — Jan, ma login i hasło.
    //   Klient (client)              — aplikacja, która prosi o token w imieniu Jana (strona w przeglądarce,
    //                                  aplikacja mobilna, u nas: plik .http, klient „sklep-cli”).
    //   Serwer autoryzacji           — Keycloak: sprawdza hasło, wydaje tokeny, zna klucz PRYWATNY.
    //   Serwer zasobów               — nasze API: przyjmuje tokeny, zna tylko klucze PUBLICZNE.
    //
    // PYTANIE REKRUTACYJNE: „Czym różni się OAuth2 od OpenID Connect?”. OAuth2 mówi, jak dać aplikacji DOSTĘP (token
    // z uprawnieniami). OIDC dokłada TOŻSAMOŚĆ: kim jest użytkownik (ID token, claimy name/email, adres
    // /.well-known/openid-configuration z całą konfiguracją). Keycloak obsługuje oba.
    //
    // Przepływy (flows), po które sięga się w praktyce:
    //   • Authorization Code + PKCE — przeglądarka i aplikacje mobilne (użytkownik loguje się NA STRONIE Keycloaka),
    //   • Client Credentials — usługa rozmawia z usługą, bez użytkownika,
    //   • Password (Resource Owner Password Credentials) — aplikacja zbiera hasło sama. Usunięty z OAuth 2.1; używamy
    //     go TYLKO lokalnie w pliku .http, bo to najprostszy sposób na token bez przeglądarki.
    //
    // DOBRA PRAKTYKA: nie pisz własnego logowania, gdy firma ma serwer tożsamości. Dlaczego? Hasła, blokady kont,
    //   logowanie dwuskładnikowe, reset hasła i jedno logowanie do wielu aplikacji (SSO) robi Keycloak — Twoje API
    //   tylko sprawdza podpis. Mniej kodu = mniej dziur.

    // =================================================================================================
    // 2. APLIKACJA JAKO RESOURCE SERVER — issuer-uri (application-ext03.properties, Ext03SecurityConfig)
    // =================================================================================================
    //
    //   pom.xml:  spring-boot-starter-security-oauth2-resource-server   (Boot 4; dawna nazwa
    //             spring-boot-starter-oauth2-resource-server jest w 4.1 przestarzała — działa, ale zniknie)
    //   spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/sklep
    //
    //   http.oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
    //       .sessionManagement(s -> s.sessionCreationPolicy(STATELESS)).csrf(csrf -> csrf.disable())
    //
    // Co robi Boot (sprawdzone): bean JwtDecoder typu SupplierJwtDecoder — „dekoder na żądanie”. Przy STARCIE nie łączy
    // się z Keycloakiem; przy pierwszym tokenie pobiera discovery i JWKS, potem sprawdza: podpis (RS256), exp/nbf
    // i iss == issuer-uri. Dodatkowo: spring.security.oauth2.resourceserver.jwt.audiences — wymagany odbiorca (aud).
    // Różnica względem s12_security/Sec05Jwt: tam aplikacja sama wydawała tokeny i trzymała klucz prywatny; tu klucza
    // prywatnego i haseł w aplikacji NIE MA.
    //
    // PUŁAPKA: Keycloak wyłączony, a aplikacja dostaje token (sprawdzone uruchomieniem). Klient widzi 401 — wygląda jak
    //   „zły token”! Prawdziwa przyczyna jest tylko w logu (ERROR): JwtDecoderInitializationException „Failed to lazily
    //   resolve the supplied JwtDecoder instance” ← IllegalArgumentException „Unable to resolve the Configuration with
    //   the provided Issuer of "http://localhost:8180/realms/sklep"” ← Connection refused. Dlaczego 401? Wyjątek trafia
    //   do obsługi błędów (/error), a ta też jest chroniona. Gdy „nagle wszystkie tokeny są złe” — sprawdź log i Keycloaka.
    //
    // PUŁAPKA: wystawca musi się zgadzać CO DO ZNAKU. Token pobrany z http://127.0.0.1:8180/... ma
    //   iss = "http://127.0.0.1:8180/realms/sklep", a aplikacja oczekuje localhost → 401 error_description „An error
    //   occurred while attempting to decode the Jwt: The iss claim is not valid” (sprawdzone). Klasyka w Dockerze:
    //   przeglądarka pobiera token z localhost:8180, a aplikacja w kontenerze widzi Keycloaka jako keycloak:8080.
    //   Dlaczego? Keycloak wpisuje do iss adres, pod którym go zapytano. Rozwiązanie: stały adres (KC_HOSTNAME).

    // =================================================================================================
    // 3. KEYCLOAK W DOCKERZE (s22-extras/compose/ext03/compose.yaml + realm-sklep.json)
    // =================================================================================================
    //
    // compose.yaml: obraz quay.io/keycloak/keycloak:26.4.0, polecenie „start-dev --import-realm”, port 8180 → 8080,
    // plik realm-sklep.json montowany do /opt/keycloak/data/import. Dziedzina „sklep”: role klient i pracownik, klient
    // „sklep-cli” (publiczny, z logowaniem hasłem), użytkownicy jan (klient) i anna (pracownik, klient) — hasła TESTOWE.
    //
    // Token anny z prawdziwego uruchomienia (Keycloak 26.4.0, ładunek, skrócony):
    //   {"iss":"http://localhost:8180/realms/sklep", "sub":"ef5e0e7f-...", "typ":"Bearer", "azp":"sklep-cli",
    //    "realm_access":{"roles":["pracownik","klient"]}, "scope":"profile email", "preferred_username":"anna",
    //    "name":"Anna Nowak", "email":"anna@example.com", "exp":..., "iat":...}
    // Uwaga: sub to UUID (nieczytelny), a ważność tokenu to 5 minut (accessTokenLifespan=300 w pliku realm).
    //
    // DOBRA PRAKTYKA: dziedzina w pliku JSON w repozytorium (import przy starcie). Dlaczego? Każdy w zespole ma te same
    //   role i użytkowników testowych jednym poleceniem, bez „wyklikiwania” w konsoli administratora.

    // =================================================================================================
    // 4. ROLE KEYCLOAKA → UPRAWNIENIA SPRINGA (KeycloakRealmRoleConverter, Ext03SecurityConfig)
    // =================================================================================================
    //
    // Domyślnie Spring zamienia na uprawnienia TYLKO claim „scope” („profile email” → SCOPE_profile, SCOPE_email).
    // realm_access.roles jest ignorowany, więc hasRole("PRACOWNIK") (= hasAuthority("ROLE_PRACOWNIK")) zawsze odmawia.
    // Nasz bean JwtAuthenticationConverter:
    //   • DelegatingJwtGrantedAuthoritiesConverter(new JwtGrantedAuthoritiesConverter(), new KeycloakRealmRoleConverter())
    //     — łączy SCOPE_... i ROLE_...,
    //   • setPrincipalClaimName("preferred_username") — getName() zwraca „anna” zamiast UUID.
    // Wynik dla anny (sprawdzone z prawdziwym Keycloakiem, GET /api/me):
    //   {"name":"anna","email":"anna@example.com",
    //    "authorities":["FACTOR_BEARER","ROLE_KLIENT","ROLE_PRACOWNIK","SCOPE_email","SCOPE_profile"]}
    // FACTOR_BEARER dodaje Spring Security 7 — „uwierzytelniony tokenem” (podstawa reguł wieloskładnikowych).
    //
    // NOWOŚĆ w Boot 4.1 — to samo bez kodu (sprawdzone uruchomieniem, gdy NIE ma własnego beana konwertera):
    //   spring.security.oauth2.resourceserver.jwt.authorities-claim-expressions=[realm_access][roles].![toUpperCase()]
    //   spring.security.oauth2.resourceserver.jwt.authority-prefix=ROLE_
    //   spring.security.oauth2.resourceserver.jwt.principal-claim-name=preferred_username
    // Wyrażenie to SpEL liczony na mapie claimów: [klucz] czyta wartość z mapy, .![...] przekształca każdy element listy
    // (projekcja). Wynik: name=anna, [ROLE_PRACOWNIK, ROLE_KLIENT, FACTOR_BEARER] — ale BEZ SCOPE_..., bo wyrażenia
    // zastępują domyślne mapowanie scope, a przedrostek jest jeden dla wszystkich wyrażeń. Brak claimu = brak uprawnień
    // (nie błąd). Gdy potrzebujesz więcej logiki — własny konwerter, jak w tej lekcji.
    //
    // PUŁAPKA: role KLIENTA (client roles) Keycloak trzyma gdzie indziej: resource_access.<clientId>.roles. Konwerter
    //   ról dziedziny ich nie widzi. Dlaczego ważne? Rola nadana w konsoli „na kliencie” zamiast „w dziedzinie” daje 403,
    //   choć w tokenie ją widać (ćwiczenie 2 i 3).

    // =================================================================================================
    // 5. TESTY — jwt() I PEŁNA ŚCIEŻKA Z KLUCZEM TESTOWYM
    // =================================================================================================
    //
    // a) jwt() (Ext03OAuth2KeycloakTest) — najszybciej, bez Keycloaka:
    //      mvc.get().uri("/api/orders/my").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_KLIENT")))
    //    Post-processor wkłada do kontekstu GOTOWY JwtAuthenticationToken. Pomija JwtDecoder (brak podpisu, issuer-uri
    //    nie jest potrzebne) ORAZ Twój JwtAuthenticationConverter.
    //
    // PUŁAPKA: jwt().jwt(j -> j.claim("realm_access", Map.of("roles", List.of("pracownik")))) → 403 (sprawdzone).
    //   Dlaczego? Uprawnienia jwt() bierze z claimu „scope” domyślnym konwerterem — Twojego KeycloakRealmRoleConverter
    //   nikt nie woła. Tak samo nazwa: getName() to „sub” (domyślnie „user”), a nie preferred_username. Poprawnie:
    //   .authorities(new SimpleGrantedAuthority("ROLE_PRACOWNIK")) albo .authorities(new KeycloakRealmRoleConverter()).
    //
    // b) pełna ścieżka (Ext03OAuth2KeycloakTokenTest): test podmienia bean JwtDecoder na dekoder z kluczem RSA
    //    wygenerowanym w teście (klucz NIE leży w repozytorium) i z tym samym walidatorem wystawcy. Tokeny wyglądają jak
    //    z Keycloaka (realm_access, preferred_username), są podpisane i idą w nagłówku Authorization — działa prawdziwy
    //    filtr, dekoder i NASZ konwerter.
    //
    // DOBRA PRAKTYKA: większość testów reguł dostępu pisz z jwt() (szybkie, czytelne), a mapowanie tokenu → uprawnienia
    //   sprawdź osobno: test jednostkowy konwertera + kilka testów pełnej ścieżki. Dlaczego? jwt() nie przetestuje
    //   konwertera, a pełna ścieżka w każdym teście to zbędny koszt.

    // =================================================================================================
    // 6. ODPOWIEDZI 401 I 403 — CO MÓWI NAGŁÓWEK WWW-Authenticate (sprawdzone uruchomieniem)
    // =================================================================================================
    //
    //   brak tokenu      → 401, WWW-Authenticate: Bearer resource_metadata="http://localhost:8122/.well-known/
    //                      oauth-protected-resource"
    //   zły podpis       → 401, Bearer error="invalid_token", error_description="An error occurred while attempting to
    //                      decode the Jwt: Signed JWT rejected: Invalid signature", ...
    //   za mało uprawnień → 403, Bearer error="insufficient_scope", error_description="The request requires higher
    //                      privileges than provided by the access token.", ...
    // resource_metadata to nowość Spring Security 7 (RFC 9728): pod /.well-known/oauth-protected-resource API opisuje
    // samo siebie ({"resource":"http://localhost:8122","bearer_methods_supported":["header"],...}).
    //
    // CO CZYTAĆ DALEJ: dokumentacja Spring Security — OAuth2 Resource Server (JWT, opaque token), OAuth2 Client
    // (logowanie przez Keycloaka w aplikacji z przeglądarką: spring-boot-starter-security-oauth2-client), Spring
    // Authorization Server (własny serwer autoryzacji); dokumentacja Keycloaka — Server Administration Guide.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Starter: spring-boot-starter-security-oauth2-resource-server; właściwość: ...resourceserver.jwt.issuer-uri.
     *   • Łańcuch: oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(...))), STATELESS, bez CSRF.
     *   • Keycloak: role dziedziny w realm_access.roles, role klienta w resource_access.<klient>.roles.
     *   • Konwerter: DelegatingJwtGrantedAuthoritiesConverter(scope, role) + setPrincipalClaimName("preferred_username").
     *   • Boot 4.1 bez kodu: authorities-claim-expressions (SpEL), authority-prefix, principal-claim-name.
     *   • iss musi być identyczny z issuer-uri; dekoder jest leniwy (start bez Keycloaka działa).
     *   • Testy: jwt().authorities(...) — pomija dekoder i konwerter; pełna ścieżka — własny JwtDecoder z kluczem testowym.
     *   • 401 = kim jesteś? (brak/zły token), 403 = wiem, kim jesteś, ale nie wolno (insufficient_scope).
     *
     * PYTANIA KONTROLNE:
     *   1. Kto w OAuth2 wydaje tokeny, a kto je sprawdza? Czy nasza aplikacja zna hasło Jana?
     *   2. Co się stanie? Uruchomisz Ext03Application bez Keycloaka. Czy wstanie? Co dostanie klient z ważnym tokenem?
     *   3. ZNAJDŹ BŁĄD: test oczekuje 200, a dostaje 403:
     *        mvc.get().uri("/api/admin/orders")
     *           .with(jwt().jwt(j -> j.claim("realm_access", Map.of("roles", List.of("pracownik")))))
     *   4. Co się stanie? Pobierzesz token z http://127.0.0.1:8180/realms/sklep/... i wyślesz go do aplikacji
     *      z issuer-uri=http://localhost:8180/realms/sklep.
     *   5. Token ma realm_access.roles = ["pracownik"]. Dlaczego bez KeycloakRealmRoleConverter hasRole("PRACOWNIK")
     *      odmawia?
     *   6. Czym różni się odpowiedź 401 od 403 w Resource Serverze? Co jest w nagłówku WWW-Authenticate?
     *   7. Dlaczego logowanie hasłem (password grant) z pliku .http jest w porządku tylko lokalnie? Jakiego przepływu
     *      użyje aplikacja w przeglądarce?
     *   8. Jakie uprawnienia zwróci GET /api/me dla anny (realm_access ["pracownik","klient"], scope "profile email")?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Ext03OAuth2KeycloakExercisesTest (zwykłe testy jednostkowe na obiektach Jwt)
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): nazwa do wyświetlenia z tokenu Keycloaka: claim "name" (np. „Anna Nowak”), a gdy go nie ma
     * albo jest pusty — "preferred_username", a gdy i tego nie ma — subject (sub).
     * Podpowiedź: jwt.getClaimAsString("name"), jwt.getSubject().
     */
    static String exercise1(Jwt jwt) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): walidator odbiorcy tokenu (OAuth2TokenValidator). Token jest „dla nas”, gdy jego audience
     * (claim "aud") zawiera "sklep-api" ALBO claim "azp" (authorized party — klient, który go dostał) to "sklep-cli".
     * Wtedy OAuth2TokenValidatorResult.success(). Inaczej failure z OAuth2Error kodu "invalid_token" i opisu
     * "Token nie jest przeznaczony dla API sklepu".
     * Taki walidator dokłada się do dekodera (decoder.setJwtValidator(...)); prostszy wariant dla samego „aud”
     * Boot ma we właściwości spring.security.oauth2.resourceserver.jwt.audiences.
     * Podpowiedź: jwt.getAudience() (lista, może być pusta), jwt.getClaimAsString("azp").
     */
    static OAuth2TokenValidator<Jwt> exercise2() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): wyrażenie dla właściwości Boota 4.1
     * spring.security.oauth2.resourceserver.jwt.authorities-claim-expressions, które z ról KLIENTA „sklep-api”
     * (claim resource_access → "sklep-api" → roles, np. ["magazynier"]) zrobi uprawnienia WIELKIMI LITERAMI.
     * Przedrostek ROLE_ ustawia osobna właściwość authority-prefix — wyrażenie go NIE dodaje.
     * Test liczy je tak jak Boot: new ExpressionJwtGrantedAuthoritiesConverter(parser.parseExpression(wyrażenie))
     * + setAuthorityPrefix("ROLE_") → [ROLE_MAGAZYNIER].
     * Podpowiedź: wyrażenie z lekcji (sekcja 4) działa dla realm_access; klucz z myślnikiem w SpEL zapisujesz w apostrofach.
     */
    static String exercise3() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Wydaje serwer autoryzacji (Keycloak) — sprawdza hasło i podpisuje token kluczem prywatnym. Sprawdza Resource
     *      Server (nasze API) kluczem publicznym pobranym z JWKS. Aplikacja NIE zna hasła Jana — widzi tylko token.
     *   2. Wstanie — JwtDecoder z issuer-uri jest leniwy (SupplierJwtDecoder; sprawdzone testem i uruchomieniem). Klient
     *      z tokenem dostanie 401 (!), a w logu jest ERROR: JwtDecoderInitializationException „Failed to lazily resolve
     *      the supplied JwtDecoder instance” ← „Unable to resolve the Configuration with the provided Issuer of
     *      "http://localhost:8180/realms/sklep"” ← Connection refused. Sprawdzone uruchomieniem.
     *   3. jwt() nie woła Twojego konwertera — uprawnienia liczy z „scope” domyślnym konwerterem, więc ROLE_PRACOWNIK nie
     *      powstaje → 403 (sprawdzone testem). Poprawka: .authorities(new SimpleGrantedAuthority("ROLE_PRACOWNIK")) albo
     *      .authorities(new KeycloakRealmRoleConverter()).
     *   4. 401, WWW-Authenticate: ... error="invalid_token", error_description="An error occurred while attempting to
     *      decode the Jwt: The iss claim is not valid" — iss w tokenie to http://127.0.0.1:8180/realms/sklep, a oczekiwany
     *      http://localhost:8180/realms/sklep. Sprawdzone z prawdziwym Keycloakiem i testem z kluczem testowym.
     *   5. Domyślny JwtGrantedAuthoritiesConverter czyta tylko claim „scope” (albo „scp”). realm_access to struktura
     *      specyficzna dla Keycloaka — ktoś musi ją zamienić na ROLE_... (konwerter albo właściwości Boota 4.1).
     *   6. 401 — nie wiadomo, kim jesteś: brak tokenu (Bearer resource_metadata=...) albo zły token (error="invalid_token"
     *      z opisem: podpis, iss, exp). 403 — token dobry, ale uprawnień za mało: error="insufficient_scope".
     *   7. Bo aplikacja (tu: plik .http) widzi hasło użytkownika — cała idea OAuth2 to NIE dawać hasła aplikacjom; OAuth 2.1
     *      usuwa ten przepływ. Przeglądarka: Authorization Code + PKCE (logowanie na stronie Keycloaka, aplikacja dostaje
     *      tylko kod i token).
     *   8. ["FACTOR_BEARER","ROLE_KLIENT","ROLE_PRACOWNIK","SCOPE_email","SCOPE_profile"] (posortowane), name „anna”.
     *      Sprawdzone z prawdziwym Keycloakiem 26.4.0 i testem Ext03OAuth2KeycloakTokenTest.
     */
    // </editor-fold>
}
