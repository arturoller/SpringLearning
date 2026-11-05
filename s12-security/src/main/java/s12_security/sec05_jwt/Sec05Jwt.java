package s12_security.sec05_jwt;

/**
 * <pre>
 * TEMAT: Tokeny JWT — budowa tokenu, wydawanie (JwtEncoder), sprawdzanie w Resource Server (JwtDecoder), role w tokenie,
 *        ograniczenia tokenów
 *        (JWT — JSON Web Token = token internetowy w formacie JSON; claim = twierdzenie, pole w tokenie; signature = podpis;
 *         encoder / decoder = koder / dekoder; resource server = serwer zasobów; bearer = okaziciel)
 *
 * W SKRÓCIE:
 *   Sec04StatelessApi wysyłała hasło w KAŻDYM żądaniu. Lepiej zalogować się hasłem RAZ i dostać token: krótki napis, który
 *   mówi "to jest anna, rola CUSTOMER, ważne do 10:15" i jest PODPISANY przez serwer. Kolejne żądania niosą
 *   {@code Authorization: Bearer <token>}. Serwer nie pamięta tokenów i nie pyta bazy — sprawdza tylko podpis i datę ważności.
 *   JWT to trzy części Base64URL rozdzielone kropkami: nagłówek, dane (twierdzenia) i podpis. Dane może przeczytać KAŻDY —
 *   podpis chroni tylko przed ZMIANĄ. W Springu: {@code JwtEncoder} (NimbusJwtEncoder) wydaje tokeny,
 *   {@code oauth2ResourceServer(o -> o.jwt(...))} + {@code JwtDecoder} je sprawdza, a {@code JwtAuthenticationConverter}
 *   zamienia twierdzenia na uprawnienia (role).
 *
 * ANALOGIA: opaska na festiwalu.
 *   Przy bramie pokazujesz dowód i bilet (hasło) — dostajesz opaskę z hologramem (podpis), datą (exp) i strefą (rola).
 *   Ochroniarze przy scenach nie dzwonią do kasy — patrzą tylko na hologram i datę. Każdy może PRZECZYTAĆ, co jest napisane
 *   na opasce, ale nikt nie podrobi hologramu. Wada: zgubioną opaskę znalazca może nosić aż do końca dnia — ochroniarze nie
 *   mają listy "unieważnionych opasek" (brak wylogowania, sekcja 5).
 *
 * JAK TO DZIAŁA:
 *   1. {@code POST /sec05/token} + Basic → łańcuch {@code tokenChain} sprawdza hasło → {@code TokenService} buduje twierdzenia
 *      (iss, sub, iat, exp, roles), a {@code NimbusJwtEncoder} podpisuje je kluczem HMAC → JSON z {@code access_token}.
 *   2. {@code GET /sec05/orders} + {@code Authorization: Bearer …} → {@code BearerTokenAuthenticationFilter} wyjmuje token →
 *      {@code JwtAuthenticationProvider} woła {@code JwtDecoder}: podpis (ten sam klucz), potem walidatory (daty z naszym
 *      {@code Clock}, 60 s tolerancji) → {@code JwtAuthenticationConverter} robi uprawnienia → {@code JwtAuthenticationToken}
 *      w {@code SecurityContextHolder} → reguły adresów jak zawsze.
 *   3. Zły / zmieniony / wygasły token → {@code BearerTokenAuthenticationEntryPoint}: 401 z
 *      {@code WWW-Authenticate: Bearer error="invalid_token", error_description="…"}. Brak uprawnień → 403 z
 *      {@code error="insufficient_scope"}.
 *
 * SŁÓWKA:
 *   token = żeton; claim = twierdzenie; issuer (iss) = wystawca; subject (sub) = podmiot (kogo dotyczy); issued at (iat) =
 *   wystawiony o; expires (exp) = wygasa; not before (nbf) = nie wcześniej niż; scope = zakres; header = nagłówek;
 *   payload = ładunek, dane; signature = podpis; HMAC = podpis wspólnym sekretem; RSA = para kluczy (prywatny podpisuje,
 *   publiczny sprawdza); key id (kid) = identyfikator klucza; decode = dekodować; validator = walidator, sprawdzacz;
 *   clock skew = rozjazd zegarów (tolerancja); converter = przelicznik; refresh token = token odświeżania;
 *   revoke = unieważnić; authorization server = serwer autoryzacji (wydaje tokeny).
 *
 * ZOBACZ TEŻ: s12_security/Sec04StatelessApi (API bez sesji, kiedy CSRF można wyłączyć),
 *             s12_security/Sec02Authorization (role, ROLE_, hasRole), s12_security/Sec03UsersPasswords (logowanie hasłem),
 *             s12_security/Sec06SecurityTesting (with(jwt()) — testy bez prawdziwego tokenu),
 *             s22_extras (serwer autoryzacji, OAuth 2 / OpenID Connect w praktyce).
 * </pre>
 */
public final class Sec05Jwt {

    private Sec05Jwt() {
    }

    // =================================================================================================
    // 1. CZYM JEST JWT: nagłówek.dane.podpis (Sec05JwtAnatomyTest)
    // =================================================================================================
    //
    // Prawdziwy token z tej lekcji (skrócony podpis):
    //
    //     eyJraWQiOiJVajdM…IiwiYWxnIjoiSFMyNTYifQ.eyJpc3MiOiJza2xlcC1zZWMwNSIsInN1YiI6ImFubmEiLCJleHAiOjE3….6FiPJxWVQP_W3Hz…
    //     └──────────── nagłówek ───────────────┘ └─────────────────── dane (payload) ─────────────────┘ └── podpis ──┘
    //
    //   Po zdekodowaniu Base64URL (bez żadnego klucza!):
    //     nagłówek: {"kid":"Uj7LVo3_…","alg":"HS256"}            — algorytm podpisu; kid = identyfikator klucza (dodaje Nimbus)
    //     dane:     {"iss":"sklep-sec05","sub":"anna","exp":1793808638,"iat":1793807738,"roles":["CUSTOMER"]}
    //     podpis:   HMAC-SHA256(nagłówek + "." + dane, tajny klucz)
    //
    //   • Daty to sekundy od 1970-01-01 UTC ("czas uniksowy"). {@code exp - iat = 900} — token żyje 15 minut (test threeParts).
    //   • Standardowe twierdzenia (RFC 7519): {@code iss}, {@code sub}, {@code aud} (dla kogo), {@code exp}, {@code nbf},
    //     {@code iat}, {@code jti} (identyfikator tokenu). Własne (np. {@code roles}) dopisujesz sam.
    //
    // HS256 kontra RS256:
    //
    //     | algorytm | klucz                              | kto może WYDAĆ token   | kto może SPRAWDZIĆ token       |
    //     |----------|------------------------------------|------------------------|--------------------------------|
    //     | HS256    | jeden wspólny sekret (HMAC)        | każdy, kto zna sekret  | każdy, kto zna sekret          |
    //     | RS256    | para: prywatny + publiczny (RSA)   | tylko właściciel klucza prywatnego | każdy (klucz publiczny jest jawny) |
    //
    //   HS256 jest prosty, gdy ta sama aplikacja wydaje i sprawdza tokeny (jak tu). Gdy tokeny wydaje JEDEN serwer, a sprawdza
    //   wiele usług, używa się RS256: usługi dostają tylko klucz publiczny (często z adresu JWK Set — zbioru kluczy), więc żadna
    //   z nich nie może podrobić tokenu. W Springu: {@code NimbusJwtEncoder.withKeyPair(publicKey, privateKey)} i
    //   {@code NimbusJwtDecoder.withPublicKey(publicKey)} (sprawdzone w Security 7.1).
    //
    // PUŁAPKA: dane tokenu NIE są zaszyfrowane. Dlaczego to ważne? Base64URL to tylko zapis, który każdy odwróci jedną linijką
    //   (test payloadIsNotEncrypted). Nie wkładaj do tokenu haseł, numeru PESEL, adresu, stanu konta. Token trafia do
    //   przeglądarki, logów serwerów pośrednich, czasem do adresu URL.
    //
    // Podpis chroni przed ZMIANĄ: anna zmienia w danych "CUSTOMER" na "ADMIN", ale nie umie policzyć nowego podpisu bez klucza.
    // Dekoder: {@code BadJwtException: … Signed JWT rejected: Invalid signature} (test tamperedPayloadIsRejected). Token
    // podpisany innym kluczem — to samo (test foreignKeyIsRejected).

    // =================================================================================================
    // 2. WYDAWANIE: POST /sec05/token + JwtEncoder (TokenService, TokenController, Sec05SecurityConfig.tokenChain,
    //    Sec05ResourceServerTest.issueToken)
    // =================================================================================================
    //
    //     @Bean SecretKey jwtSecretKey() { return KeyGenerator.getInstance("HmacSHA256").generateKey(); }  // 256 bitów
    //     @Bean JwtEncoder jwtEncoder(SecretKey key) { return NimbusJwtEncoder.withSecretKey(key).build(); }
    //
    //     JwtClaimsSet claims = JwtClaimsSet.builder()
    //             .issuer(ISSUER).subject(authentication.getName())
    //             .issuedAt(now).expiresAt(now.plus(LIFETIME))          // now = clock.instant() — wstrzykiwany Clock!
    //             .claim("roles", roles)
    //             .build();
    //     String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
    //             .getTokenValue();
    //
    //   • Security 7 ma wygodne budowniczne (builders): {@code NimbusJwtEncoder.withSecretKey(…)} i {@code .withKeyPair(…)}.
    //     W starszych poradnikach zobaczysz ręczne {@code new NimbusJwtEncoder(new ImmutableSecret<>(key))} — działa, ale dłużej.
    //   • Nagłówek z {@code MacAlgorithm.HS256} podajemy jawnie — mówi koderowi, jakim algorytmem podpisać.
    //   • Odpowiedź w formacie OAuth 2: {@code {"access_token": "…", "token_type": "Bearer", "expires_in": 900}}.
    //   • {@code tokenChain} ({@code securityMatcher("/sec05/token")}) to JEDYNE miejsce, gdzie przychodzi hasło (Basic).
    //     Złe hasło → 401 {@code WWW-Authenticate: Basic …}.
    //
    // PUŁAPKA: wszystkie uprawnienia z logowania wrzucone do tokenu. Dlaczego? Po logowaniu hasłem Security 7 dokłada uprawnienie
    //   {@code FACTOR_PASSWORD} ("logował się hasłem" — podstawa logowania wieloskładnikowego). To nie jest rola. TokenService
    //   bierze tylko uprawnienia {@code ROLE_…} i zapisuje je bez prefiksu (test threeParts: {@code roles: ["CUSTOMER"]}).
    //
    // DOBRA PRAKTYKA: krótki czas życia ({@code exp} — minuty, nie dni) i czas z wstrzykiwanego {@code Clock}. Dlaczego?
    //   Skradziony token działa do {@code exp} (sekcja 5) — im krócej, tym mniejsza szkoda. A {@code Clock} pozwala w teście
    //   wystawić token "2 godziny temu" bez czekania (test expiredToken).
    //
    // PUŁAPKA: klucz wpisany w kod albo w {@code application.properties} w repozytorium. Dlaczego? Kto zna klucz HMAC, ten
    //   wystawi sobie token admina. Klucz z tej lekcji jest losowany przy starcie (TYLKO DO NAUKI); na produkcji przychodzi
    //   ze zmiennej środowiska lub menedżera sekretów (s19_deploy) i ma co najmniej 256 bitów.

    // =================================================================================================
    // 3. SPRAWDZANIE: oauth2ResourceServer(o -> o.jwt(...)) + JwtDecoder (Sec05SecurityConfig.apiChain, jwtDecoder,
    //    Sec05ResourceServerTest.bearerToken, invalidToken, expiredToken, Sec05ExpiryTest)
    // =================================================================================================
    //
    //     http.oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(rolesConverter)))
    //
    //     @Bean JwtDecoder jwtDecoder(SecretKey key, Clock clock) {
    //         NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    //         JwtTimestampValidator timestamps = new JwtTimestampValidator();   // exp i nbf, 60 s tolerancji
    //         timestamps.setClock(clock);                                       // ten sam zegar co TokenService
    //         decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(timestamps));
    //         return decoder;
    //     }
    //
    // Co odpowiada API (sprawdzone testami; MockMvc):
    //
    //     | żądanie                         | status | WWW-Authenticate                                                     |
    //     |---------------------------------|--------|----------------------------------------------------------------------|
    //     | Bearer poprawny                 | 200    | —                                                                    |
    //     | bez tokenu                      | 401    | Bearer resource_metadata="http://localhost/.well-known/oauth-protected-resource" |
    //     | samo Basic (hasło)              | 401    | jak wyżej — API przyjmuje TYLKO tokeny                               |
    //     | zmieniony podpis                | 401    | Bearer error="invalid_token", error_description="… Invalid signature", … |
    //     | śmieci ("abc.def.ghi")          | 401    | Bearer error="invalid_token", error_description="… Malformed token", …   |
    //     | wygasły                         | 401    | Bearer error="invalid_token", error_description="… Jwt expired at …", …  |
    //     | brak roli                       | 403    | Bearer error="insufficient_scope", …                                 |
    //
    //   {@code resource_metadata} to nowość Security 7 (RFC 9728): adres opisu serwera zasobów, z którego klient może się
    //   dowiedzieć, gdzie zdobyć token. Format {@code error="…"} pochodzi z RFC 6750 (tokeny Bearer).
    //
    // Tolerancja zegarów (clock skew): serwery mają nieco różne zegary, więc {@code JwtTimestampValidator} domyślnie przymyka oko
    // na 60 sekund. Token z {@code exp} 10:15:00 przechodzi jeszcze o 10:15:30, o 10:17:00 już nie (test expiryWithClockSkew,
    // zegar przesunięty {@code Clock.offset} — zero czekania). Ćwiczenie 4: dekoder bez tolerancji.
    //
    // PUŁAPKA: {@code JwtDecoder} z domyślnym zegarem, a {@code TokenService} z wstrzykiwanym. Dlaczego? Test z zegarem
    //   przesuniętym w jednym miejscu daje sprzeczne wyniki ("token z przyszłości", "token wiecznie ważny"). Jeden {@code Clock}
    //   dla wydawania i sprawdzania.
    //
    // Skąd Boot bierze dekoder bez naszego beana? Z właściwości {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}
    // (adres serwera autoryzacji) albo {@code .jwk-set-uri} (adres kluczy publicznych). Tu tokeny wydajemy sami, więc dekoder
    // jest naszym beanem.

    // =================================================================================================
    // 4. ROLE Z TOKENU: JwtAuthenticationConverter (Sec05SecurityConfig.rolesConverter, Sec05RolesConverterTest,
    //    Sec05ResourceServerTest.rolesFromToken, sec05_pitfalls: Sec05DefaultConverterPitfallTest)
    // =================================================================================================
    //
    // Domyślnie Spring czyta twierdzenie {@code scope} (albo {@code scp}) — napisy rozdzielone spacją — i dokleja {@code SCOPE_}:
    //   {@code scope: "orders:read products:write"} → {@code SCOPE_orders:read}, {@code SCOPE_products:write}. To standard OAuth 2:
    //   zakres mówi, CO aplikacja kliencka może robić w imieniu użytkownika. Reguła: {@code hasAuthority("SCOPE_orders:read")}.
    //
    // Nasze tokeny mają role ({@code roles: ["ADMIN"]}), a reguły {@code hasRole("ADMIN")}. Trzeba powiedzieć Springowi, gdzie
    // szukać:
    //
    //     JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
    //     roles.setAuthoritiesClaimName("roles");     // zamiast "scope"
    //     roles.setAuthorityPrefix("ROLE_");          // zamiast "SCOPE_"
    //     JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    //     converter.setJwtGrantedAuthoritiesConverter(roles);
    //
    //   Wynik dla anny ({@code GET /sec05/me}): {@code ["FACTOR_BEARER", "ROLE_CUSTOMER"]}. {@code FACTOR_BEARER} Security 7
    //   dodaje SAM — "zalogowany tokenem" (odpowiednik FACTOR_PASSWORD z sekcji 2). Panel admina: anna 403
    //   ({@code insufficient_scope}), admin 200 (test rolesFromToken).
    //
    // PUŁAPKA: role w twierdzeniu {@code roles}, a konwerter domyślny. Dlaczego? Domyślny czyta tylko {@code scope}, więc admin
    //   z {@code roles: ["ADMIN"]} nie ma {@code ROLE_ADMIN} i dostaje 403 — bez żadnego ostrzeżenia w logu (test
    //   rolesClaimIgnoredByDefault). Gdy {@code hasRole} "nie działa z JWT", wypisz {@code authentication.getAuthorities()}.
    //
    // DOBRA PRAKTYKA: konwerter podawaj jawnie w {@code jwt(jwt -> jwt.jwtAuthenticationConverter(…))}. Dlaczego? Od razu widać
    //   w konfiguracji, skąd biorą się role. Boot ma też właściwości
    //   {@code spring.security.oauth2.resourceserver.jwt.authorities-claim-name} i {@code .authority-prefix}, ale działają tylko
    //   z konwerterem tworzonym przez Boota.

    // =================================================================================================
    // 5. OGRANICZENIA I DOBRE PRAKTYKI (Sec05ResourceServerTest.tokenOutlivesAccount)
    // =================================================================================================
    //
    // • Brak wylogowania. Serwer nie trzyma listy tokenów, więc nie ma czego "usunąć". Test tokenOutlivesAccount: konto anny
    //   usunięte, logowanie hasłem → 401, ale jej token dalej daje 200 — aż do {@code exp}. Ratunek: krótki {@code exp},
    //   lista unieważnionych identyfikatorów {@code jti} (ale wtedy znowu pytamy bazę przy każdym żądaniu) albo zmiana klucza
    //   (unieważnia WSZYSTKIE tokeny).
    // • Refresh token (token odświeżania): krótki access token (minuty) + długi refresh token (godziny/dni), którym klient
    //   po cichu pobiera nowy access token. Refresh token serwer zapisuje — więc da się go unieważnić przy wylogowaniu.
    // • Gdzie przeglądarka ma trzymać token?
    //
    //     | miejsce                      | XSS (obcy skrypt na stronie)            | CSRF                                     |
    //     |------------------------------|-----------------------------------------|------------------------------------------|
    //     | localStorage                 | skrypt PRZECZYTA token i wyśle go dalej  | odporne — skrypt sam dokłada nagłówek     |
    //     | ciasteczko HttpOnly, Secure, SameSite | skrypt tokenu nie przeczyta     | przeglądarka dołącza je sama → włącz CSRF (Sec04StatelessApi) |
    //
    //   Nie ma rozwiązania bez wad. Częsty wybór dla przeglądarki: ciasteczko HttpOnly + ochrona CSRF, albo wzorzec BFF
    //   (Backend For Frontend — serwer pośredni trzyma tokeny, przeglądarka ma tylko sesję).
    //
    // DOBRA PRAKTYKA: w prawdziwych projektach nie pisz własnego wydawania tokenów — użyj serwera autoryzacji (Keycloak, Spring
    //   Authorization Server, usługi chmurowe) i OAuth 2 / OpenID Connect. Dlaczego? Logowanie wieloskładnikowe, refresh
    //   tokeny, unieważnianie, rotacja kluczy RSA, zgody użytkownika — to dużo trudnego kodu, który ktoś już przetestował.
    //   Twoje API zostaje wtedy samym Resource Serverem: {@code issuer-uri} w properties i reguły adresów (dział s22_extras).
    //   Własny {@code POST /token} z tej lekcji jest dobry do nauki i do małych usług wewnętrznych.
    //
    // PYTANIE REKRUTACYJNE: "Czym różni się sesja od JWT? Jak wylogować użytkownika z JWT?" — sesja to stan na serwerze
    //   (identyfikator w ciasteczku, serwer wie wszystko i może ją usunąć). JWT to podpisany stan u klienta (serwer niczego nie
    //   pamięta, skaluje się łatwo, ale nie może tokenu cofnąć). Wylogowanie z JWT: krótki exp + unieważnienie refresh tokenu,
    //   ewentualnie lista zablokowanych jti.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • JWT = base64url(nagłówek).base64url(dane).podpis; dane CZYTELNE dla każdego, podpis chroni przed zmianą.
     *   • Twierdzenia: iss, sub, aud, exp, nbf, iat, jti (+ własne, np. roles); daty w sekundach od 1970.
     *   • HS256 = jeden sekret (wydaje i sprawdza ta sama strona); RS256 = prywatny podpisuje, publiczny sprawdza.
     *   • Wydawanie: NimbusJwtEncoder.withSecretKey(key).build(); encoder.encode(JwtEncoderParameters.from(
     *     JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue(); czas z wstrzykiwanego Clock.
     *   • Sprawdzanie: oauth2ResourceServer(rs -> rs.jwt(...)) + NimbusJwtDecoder.withSecretKey(key).macAlgorithm(HS256);
     *     JwtTimestampValidator (60 s tolerancji) + setClock(clock).
     *   • 401 Bearer error="invalid_token" (podpis, format, exp); 403 error="insufficient_scope"; bez tokenu 401 Bearer
     *     resource_metadata=… (Security 7).
     *   • Domyślnie scope → SCOPE_x; role: JwtGrantedAuthoritiesConverter (claim "roles", prefiks "ROLE_"). Security 7 dodaje
     *     FACTOR_BEARER (token) i FACTOR_PASSWORD (hasło) — nie wkładaj ich do tokenu.
     *   • Token żyje do exp nawet po usunięciu konta — krótki exp, refresh token, serwer autoryzacji.
     *
     * PYTANIA KONTROLNE:
     *   1. Co się stanie? Anna dekoduje swój token, zmienia w danych "CUSTOMER" na "ADMIN", koduje z powrotem i wysyła go z
     *      oryginalnym podpisem do {@code /sec05/admin/stats}. Jaki status i jaki {@code error_description}?
     *   2. ZNAJDŹ BŁĄD: zespół zapisał w tokenie twierdzenie {@code "card": "4111 1111 1111 1111"}, "bo token jest podpisany".
     *   3. Co się stanie? Token ma {@code exp} = 10:15:00, a zegar serwera pokazuje 10:15:40. Domyślny {@code JwtTimestampValidator}.
     *   4. ZNAJDŹ BŁĄD: token ma {@code roles: ["ADMIN"]}, łańcuch ma {@code .oauth2ResourceServer(rs -> rs.jwt(withDefaults()))}
     *      i regułę {@code hasRole("ADMIN")}. Admin dostaje 403. Dlaczego?
     *   5. Czym różni się HS256 od RS256 i kiedy wybierzesz RS256?
     *   6. PYTANIE REKRUTACYJNE: dlaczego wylogowanie z JWT jest trudne i jak sobie z tym radzić?
     *   7. Co się stanie? Klient wysyła do {@code /sec05/orders} poprawne hasło w nagłówku Basic (bez tokenu).
     *   8. Dlaczego TokenService i JwtDecoder dostają ten sam wstrzykiwany {@code Clock}?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielet: JwtParts, ExerciseClaims, ExerciseRolesConverter, ExerciseJwtConfig;
    // testy: Sec05JwtExercisesTest (@Tag("cwiczenie"))
    // =================================================================================================
    //
    // Rozwiązania wzorcowe: pakiet s12_security.solutions.sec05_jwt. Wszystkie ćwiczenia to zwykłe testy jednostkowe.
    //
    // ĆWICZENIE 1 (łatwe): JwtParts.payloadJson — środkowa część tokenu zdekodowana z Base64URL jako tekst; inna liczba części
    //   niż 3 → IllegalArgumentException. Podpowiedź: {@code token.split("\\.", -1)}, {@code Base64.getUrlDecoder()} (nie
    //   {@code getDecoder()} — w Base64URL są znaki {@code -} i {@code _}).
    //
    // ĆWICZENIE 2 (łatwe): ExerciseClaims.claimsFor — iss "sklep-cw", sub, iat = now, exp = now + 10 minut, roles.
    //   Podpowiedź: {@code JwtClaimsSet.builder()} jak w TokenService.
    //
    // ĆWICZENIE 3 (średnie): ExerciseRolesConverter.convert — {@code roles} → {@code ROLE_…}; bez twierdzenia → pusta lista.
    //   Podpowiedź: {@code jwt.getClaimAsStringList("roles")} zwraca {@code null}, gdy twierdzenia nie ma;
    //   {@code new SimpleGrantedAuthority("ROLE_" + role)}.
    //
    // ĆWICZENIE 4 (trudniejsze): ExerciseJwtConfig.decoder — dekoder HS256 z zegarem {@code clock} i ZEROWĄ tolerancją.
    //   Podpowiedź: {@code new JwtTimestampValidator(Duration.ZERO)}, {@code setClock}, {@code JwtValidators
    //   .createDefaultWithValidators(…)}; wzór: Sec05SecurityConfig.jwtDecoder.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. 401 z WWW-Authenticate: Bearer error="invalid_token", error_description="An error occurred while attempting to
     *      decode the Jwt: Signed JWT rejected: Invalid signature" — podpis liczono z oryginalnych danych, a bez klucza anna nie
     *      policzy nowego (testy tamperedPayloadIsRejected, invalidToken). Do sprawdzania ról w ogóle nie dochodzi.
     *   2. Dane tokenu nie są szyfrowane — numer karty przeczyta każdy, kto zobaczy token (przeglądarka, logi, pośrednicy).
     *      Podpis chroni tylko przed zmianą. W tokenie: identyfikator i uprawnienia; dane wrażliwe zostają na serwerze.
     *   3. Token przejdzie — domyślna tolerancja zegarów to 60 sekund, a 10:15:40 mieści się w 10:15:00 + 60 s (test
     *      expiryWithClockSkew: 30 s po exp ważny, 2 minuty po — "Jwt expired at …").
     *   4. Domyślny JwtAuthenticationConverter czyta tylko scope/scp i robi SCOPE_…; twierdzenie roles jest ignorowane, więc
     *      admin nie ma ROLE_ADMIN (test rolesClaimIgnoredByDefault). Naprawa: JwtGrantedAuthoritiesConverter z
     *      setAuthoritiesClaimName("roles") i setAuthorityPrefix("ROLE_").
     *   5. HS256 — jeden wspólny sekret: kto może sprawdzić token, ten może go też wystawić. RS256 — klucz prywatny podpisuje,
     *      publiczny tylko sprawdza. RS256, gdy tokeny sprawdza wiele usług (albo cudze usługi): dostają tylko klucz publiczny.
     *   6. Serwer nie pamięta tokenów, więc nie ma czego usunąć — token działa do exp (test tokenOutlivesAccount). Sposoby:
     *      krótki exp, refresh token zapisany na serwerze i unieważniany przy wylogowaniu, lista zablokowanych jti, zmiana klucza.
     *   7. 401 z WWW-Authenticate: Bearer … — łańcuch API ma tylko Resource Server i nie ma httpBasic, więc hasło jest
     *      ignorowane (test bearerToken). Hasło przyjmuje wyłącznie POST /sec05/token.
     *   8. Żeby testy mogły sterować czasem w jednym miejscu: token "wystawiony 2 godziny temu" musi być wygasły także dla
     *      dekodera. Dwa różne zegary dają sprzeczne wyniki (np. token z przyszłości).
     */
    // </editor-fold>
}
