package s14_openapi.api03_groups_and_versions;

import java.util.List;

/**
 * <pre>
 * TEMAT: Grupy dokumentów, informacje ogólne, schemat zabezpieczeń i wersje API
 *        (group = grupa; version = wersja; security scheme = schemat zabezpieczeń; contact = kontakt; license = licencja)
 *
 * W SKRÓCIE:
 *   Jedno API bywa czytane przez różnych odbiorców: klienci sklepu (publiczne /api/**) i pracownicy (administracyjne
 *   /admin/**). Springdoc pozwala podzielić dokument na GRUPY ({@code GroupedOpenApi}) — każda ma własny adres
 *   /v3/api-docs/<grupa> i pozycję w liście rozwijanej Swagger UI. Bean {@code OpenAPI} ustawia informacje ogólne
 *   (tytuł, wersja, kontakt, licencja) i opis zabezpieczeń. A wersje API ze Spring Framework 7 (atrybut {@code version}
 *   w mapowaniach) springdoc 3.1.1 rozumie: dopisuje parametr nagłówka (albo zapytania) z wersją — ale scala wersje jednej
 *   ścieżki w jedną operację, więc czytelniejszy dokument dostajesz, robiąc grupę na każdą wersję.
 *
 * ANALOGIA: katalog sklepu w trzech wersjach.
 *   Ten sam sklep ma katalog dla klientów (bez zaplecza), katalog dla pracowników (z zapleczem i napisem „wymaga
 *   przepustki”) i stare wydanie katalogu sprzed zmiany cen. Karta tytułowa każdego katalogu jest taka sama (nazwa
 *   sklepu, kontakt) — to bean OpenAPI. Napis „wymaga przepustki” to opis; ochroniarz przy drzwiach to osobna sprawa
 *   (Spring Security).
 *
 * JAK TO DZIAŁA:
 *   1. @Bean OpenAPI   → pola info (title, version, description, contact, license) i components.securitySchemes.
 *      Dotyczy WSZYSTKICH dokumentów, także grup.
 *   2. @Bean GroupedOpenApi (group, pathsToMatch, addOpenApiCustomizer, addOpenApiMethodFilter) → osobny dokument
 *      /v3/api-docs/<group>; Swagger UI dostaje ich listę z /v3/api-docs/swagger-config (pole "urls").
 *   3. OpenApiCustomizer — kod, który dostaje gotowy obiekt OpenAPI grupy i może go zmienić (tu: dopisuje wymaganie
 *      zabezpieczenia "bearerAuth" do operacji grupy admin).
 *   4. Wersje API: springdoc opakowuje ApiVersionStrategy Springa i odczytuje wersje z mapowań ({@code version = "2.0+"});
 *      do operacji dopisuje parametr o nazwie z konfiguracji (nagłówek API-Version albo parametr zapytania).
 *
 *   /v3/api-docs/swagger-config:
 *     {"urls":[{"url":"/v3/api-docs/admin","name":"Administracja"},{"url":"/v3/api-docs/public","name":"Publiczne"},...]}
 *
 * SŁÓWKA:
 *   GroupedOpenApi = grupa dokumentu; pathsToMatch = ścieżki do uwzględnienia; customizer = dostosowywacz (zmienia gotowy
 *   dokument); method filter = filtr metod; info = informacje ogólne; contact = kontakt; license = licencja;
 *   security scheme = schemat zabezpieczeń (opis sposobu uwierzytelniania); bearer token = token okaziciela (JWT w nagłówku
 *   Authorization); baseline version = wersja bazowa ("1.0+" — od 1.0 wzwyż); oneOf = „jedno z” (odpowiedź może mieć
 *   jeden z kilku kształtów).
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest07ApiVersioning (wersjonowanie API w Spring Framework 7), s14_openapi/Api01Springdoc
 *             (właściwości springdoc.*), s14_openapi/Api02DocumentingApi (adnotacje), s12_security/Sec01Basics
 *             (prawdziwe zabezpieczenia), s12_security/Sec05Jwt (tokeny JWT), s02_configuration/Cfg03Profiles (profile).
 * </pre>
 */
public final class Api03GroupsAndVersions {

    private Api03GroupsAndVersions() {
    }

    // =================================================================================================
    // 1. INFORMACJE OGÓLNE — bean OpenAPI (test: openApiBeanSetsGeneralInfo)
    // =================================================================================================
    //
    // Domyślnie info.title = "OpenAPI definition", version = "v0" (Api01Springdoc). Własny bean OpenAPI (klasa
    // io.swagger.v3.oas.models.OpenAPI — model z biblioteki swagger-core, NIE adnotacja) zmienia to:
    //   new OpenAPI().info(new Info().title("Sklep — API").version("2.0.0").contact(...).license(...))
    // Zobacz OpenApiConfig.shopOpenApi(). Ten sam bean dotyczy wszystkich grup.
    //
    // PUŁAPKA: info.version to wersja DOKUMENTU/API wpisana ręcznie — nie ma nic wspólnego z wersjami w mapowaniach
    //   Springa (sekcja 5). Dlaczego to ważne? Łatwo zapomnieć o jej podniesieniu po zmianie API; klient widzi wtedy
    //   w dokumencie starą wersję. Rozważ wpisywanie jej z właściwości (np. wersja aplikacji z pliku konfiguracji).

    // =================================================================================================
    // 2. GRUPY — GroupedOpenApi (testy: groupsSplitTheDocument, swaggerConfigListsGroups)
    // =================================================================================================
    //
    // GroupedOpenApi.builder().group("admin").pathsToMatch("/admin/**").displayName("Administracja").build()
    //   → dokument /v3/api-docs/admin zawiera tylko ścieżki pasujące do wzorca,
    //   → Swagger UI pokazuje listę wyboru z nazwami wyświetlanymi (displayName).
    // Inne kryteria: packagesToScan, pathsToExclude, headersToMatch, producesToMatch.
    // Dokument bez nazwy grupy (/v3/api-docs) nadal zawiera WSZYSTKO — sprawdzone testem. Odbiorcom publicznym podawaj adres
    // grupy, nie ten ogólny.
    //
    // DOBRA PRAKTYKA: dziel dokumentację na grupy według ODBIORCÓW (publiczne, partnerzy, administracja), a nie według
    //   klas. Dlaczego? Odbiorca widzi tylko to, z czego może korzystać; zespół administracyjny nie zaśmieca dokumentu
    //   klienta, a wygenerowany klient (np. dla aplikacji mobilnej) nie dostaje metod, których nie powinien znać.
    //
    // PUŁAPKA: grupa „admin” to tylko filtr dokumentu. Dlaczego nie chroni? Ścieżka /admin/stats nadal odpowiada każdemu
    //   (w teście: 200 bez tokenu). Dostępu pilnuje Spring Security (s12_security), nie dokumentacja.

    // =================================================================================================
    // 3. SCHEMAT ZABEZPIECZEŃ — TYLKO OPIS (test: securitySchemeIsOnlyDescription)
    // =================================================================================================
    //
    // W components.securitySchemes opisujesz SPOSÓB uwierzytelniania (tu: HTTP bearer, format JWT), a wymaganie
    // ("security": [{"bearerAuth": []}]) dopisujesz operacjom — w tej lekcji OpenApiCustomizer grupy admin robi to dla
    // każdej operacji. Swagger UI pokazuje wtedy przycisk „Authorize” i kłódki; wpisany token wysyła w nagłówku
    // Authorization. Alternatywa: adnotacje {@code @SecurityScheme} (definicja) i {@code @SecurityRequirement}
    // (na kontrolerze lub metodzie) z pakietu io.swagger.v3.oas.annotations.security.
    // W tym module nie ma Spring Security — to wyłącznie opis; prawdziwą ochronę dopiszesz w s12_security.
    //
    // PUŁAPKA: wymaganie zabezpieczenia w dokumencie bez prawdziwej ochrony w aplikacji. Dlaczego niebezpieczne? Dokument
    //   obiecuje „wymaga tokenu”, a punkt jest otwarty — fałszywe poczucie bezpieczeństwa. Odwrotnie też: zabezpieczony
    //   punkt bez wpisu w dokumencie zostawia odbiorcę bez wiedzy, jak się uwierzytelnić.

    // =================================================================================================
    // 4. WERSJE API (Spring Framework 7) W DOKUMENCIE (test: springdocUnderstandsApiVersions)
    // =================================================================================================
    //
    // VersionedProductController ma dwie metody dla GET /api/products/{sku}: version = "1.0+" (cena jako liczba) i
    // version = "2.0+" (cena jako obiekt). Wersję czyta Spring z nagłówka API-Version (profil api03:
    // spring.mvc.apiversion.use.header, default=1.0) — mechanizm opisuje s04_web_rest/Rest07ApiVersioning.
    // Springdoc 3.1.1 (sprawdzone uruchomieniem z Spring Boot 4.1.1):
    //   • ROZUMIE atrybut version i konfigurację — dopisuje do operacji parametr {"name":"API-Version","in":"header",
    //     "schema":{"type":"string","enum":[...]}}; enum składa się z wersji zadeklarowanych w mapowaniach;
    //   • SCALA obie metody jednej ścieżki w JEDNĄ operację (GET /api/products/{sku}); odpowiedź ma schemat
    //     {"oneOf":[ProductV1, ProductV2]} — odbiorca nie widzi, która wersja daje który kształt;
    //   • wartość domyślna parametru jest zapisana jako "1.0.0" (wersja po sparsowaniu), choć w konfiguracji jest "1.0".
    // Strategie: nagłówek (sprawdzone), parametr zapytania (sprawdzone testem Api03GroupsAndVersionsQueryParamTest —
    // wtedy parametr dokumentu ma "in":"query"). W jarze springdoc są też strategie ścieżki i typu mediów (PATH,
    // MEDIA_TYPE) — ich NIE sprawdzaliśmy uruchomieniem.
    //
    // PUŁAPKA: oczekiwanie, że springdoc wygeneruje osobny, czysty opis dla każdej wersji. Dlaczego nie? Dokument ma
    //   jedną operację na parę „metoda HTTP + ścieżka”, więc różne kształty odpowiedzi lądują w oneOf. Jeśli chcesz czystej
    //   dokumentacji per wersja — sekcja 5.

    // =================================================================================================
    // 5. OBEJŚCIE: GRUPA NA KAŻDĄ WERSJĘ (test: groupPerVersionGivesCleanDocuments)
    // =================================================================================================
    //
    // GroupedOpenApi umie filtrować metody: {@code addOpenApiMethodFilter(OpenApiMethodFilter)} — filtr dostaje metodę
    // Javy i mówi, czy ją uwzględnić. VersionGroupFilter(N) zostawia metodę, która obsłuży wersję N: wersja bazowa
    // metody <= N i brak metody z wyższą wersją bazową (nadal <= N) dla tego samego adresu. Wynik:
    //   /v3/api-docs/v1 → GET /{sku} = oneV1 → ProductV1;  /v3/api-docs/v2 → GET /{sku} = oneV2 → ProductV2,
    // a metoda skus() ("1.0+") jest w obu, bo w wersji 2 też działa.
    // Filtr z lekcji jest uproszczony (numer główny, tylko @GetMapping) — w prawdziwym projekcie dostosuj go do swoich
    // mapowań albo trzymaj wersje w osobnych kontrolerach.
    //
    // UWAGA (rzecz zaobserwowana w springdoc 3.1.1): w dokumencie grupy v2 parametr API-Version dla operacji skus()
    //   ma enum ["1.0"] (bo metoda zadeklarowała "1.0+"), choć działa też w wersji 2.0. Parametr jest w dokumencie jako
    //   opcjonalny (jest wersja domyślna). Przy wersji w parametrze zapytania ten sam parametr ma "required": true.
    //   Traktuj te pola jako wskazówkę, nie jako ścisłą umowę — i sprawdzaj dokument testem (Api02DocumentingApi).
    //
    // DOBRA PRAKTYKA: opisz w dokumencie (description) politykę wersji: która jest bieżąca, która przestarzała, kiedy
    //   zostanie wyłączona. Dlaczego? Odbiorca musi wiedzieć, ile ma czasu na migrację; sam enum wersji tego nie mówi.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @Bean OpenAPI: info (title, version, description, contact, license) + components.securitySchemes; wspólne dla grup.
     *   • @Bean GroupedOpenApi: group, displayName, pathsToMatch/pathsToExclude/packagesToScan, addOpenApiCustomizer,
     *     addOpenApiMethodFilter → /v3/api-docs/<grupa>; lista grup w /v3/api-docs/swagger-config ("urls").
     *   • /v3/api-docs (bez grupy) nadal zawiera wszystko.
     *   • Schemat zabezpieczeń + wymaganie (security) to OPIS; ochrona to Spring Security.
     *   • Springdoc 3.1.1 rozumie wersje API Spring Framework 7: parametr nagłówka/zapytania z enum wersji, ale scala
     *     metody jednej ścieżki w jedną operację (oneOf).
     *   • Czysty dokument per wersja: grupa na wersję z addOpenApiMethodFilter.
     *
     * PYTANIA KONTROLNE:
     *   1. Do czego służy GroupedOpenApi i gdzie Swagger UI bierze listę grup?
     *   2. Czy informacje z beana OpenAPI (tytuł, kontakt) dotyczą tylko jednej grupy?
     *   3. Co się stanie? Zdefiniujesz grupy public i admin i wejdziesz na /v3/api-docs (bez nazwy grupy) — czy zobaczysz
     *      ścieżki /admin?
     *   4. ZNAJDŹ BŁĄD: „Grupa admin ma w dokumencie wymaganie bearerAuth, więc /admin/stats bez tokenu da 401.” Dlaczego to
     *      nieprawda w tym module?
     *   5. Co się stanie? Wywołasz GET /api/products/ELE-001 bez nagłówka API-Version (profil api03) — jaki kształt ceny?
     *   6. ZNAJDŹ BŁĄD: „Dla dwóch metod z version = \"1.0+\" i \"2.0+\" springdoc zrobi dwie operacje w dokumencie.” Jak jest
     *      naprawdę i jak zdobyć osobne opisy?
     *   7. Dlaczego sam {@code info.version} nie wystarcza do informowania o wersjach API?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Api03GroupsAndVersionsExercisesTest
    // =================================================================================================
    //
    // Znów małe „lintery” tekstu JSON (Jackson 3: {@code JsonMapper.builder().build().readTree(json)}, {@code get},
    // {@code propertyNames()}, iteracja po tablicy, {@code asString()}).

    /**
     * ĆWICZENIE 1 (łatwe): z JSON-a swagger-config ({@code {"urls":[{"url":"...","name":"..."}]}}) zwróć posortowaną
     * listę nazw grup (pola "name"). Gdy nie ma pola "urls" — pusta lista.
     */
    static List<String> exercise1(String swaggerConfigJson) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć posortowaną listę operacji dokumentu OpenAPI, które mają pole "security" — w postaci
     * "METODA ścieżka" (metoda wielkimi literami). Sprawdzaj tylko metody get, put, post, delete, patch.
     */
    static List<String> exercise2(String openApiJson) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zwróć posortowaną listę wartości "enum" parametru o nazwie {@code parameterName} (np.
     * "API-Version") z operacji o podanej metodzie (małymi literami) i ścieżce. Gdy operacja nie ma takiego parametru albo
     * parametr nie ma "enum" w "schema" — pusta lista. Gdy nie ma operacji — IllegalArgumentException.
     * Podpowiedź: parametry to tablica "parameters"; wartości enum są w parameter.schema.enum.
     */
    static List<String> exercise3(String openApiJson, String path, String method, String parameterName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Dzieli dokument na osobne dokumenty (np. publiczne i administracyjne) według ścieżek, pakietów itd. Swagger UI
     *      bierze listę z /v3/api-docs/swagger-config, z pola "urls" (url + nazwa wyświetlana).
     *   2. Nie — bean OpenAPI jest wspólny, więc info (tytuł, kontakt, licencja) i securitySchemes są w każdej grupie
     *      (sprawdzone: /v3/api-docs/admin ma ten sam tytuł).
     *   3. Tak. Dokument bez nazwy grupy zawiera wszystko, także /admin/stats (sprawdzone testem groupsSplitTheDocument).
     *   4. Schemat i wymaganie to tylko opis w dokumencie. Moduł nie ma Spring Security, więc /admin/stats odpowiada 200
     *      bez tokenu (sprawdzone testem securitySchemeIsOnlyDescription).
     *   5. Cena jako jedna liczba (wersja domyślna 1.0 → metoda oneV1): {"sku":"ELE-001","name":"Laptop Pro 14",
     *      "price":5499.99}. Sprawdzone testem springdocUnderstandsApiVersions.
     *   6. Springdoc scala je w JEDNĄ operację GET /api/products/{sku} z odpowiedzią oneOf [ProductV1, ProductV2] i
     *      parametrem nagłówka API-Version. Osobne opisy: grupa na wersję z addOpenApiMethodFilter (test
     *      groupPerVersionGivesCleanDocuments).
     *   7. To ręcznie wpisana wersja całego dokumentu. Nie mówi, które wersje wywołań (nagłówek API-Version) są wspierane
     *      ani który kształt odpowiedzi odpowiada której wersji — to widać dopiero w parametrze wersji i grupach.
     */
    // </editor-fold>
}
