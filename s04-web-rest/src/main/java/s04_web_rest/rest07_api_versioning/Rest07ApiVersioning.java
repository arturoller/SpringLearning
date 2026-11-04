package s04_web_rest.rest07_api_versioning;

/**
 * <pre>
 * TEMAT: Wersjonowanie API — wbudowane w Spring Framework 7 (atrybut version, ApiVersionStrategy, spring.mvc.apiversion.*)
 *        (versioning = wersjonowanie; API version = wersja API; strategy = strategia: sposób wyboru wersji)
 *
 * W SKRÓCIE:
 *   API to umowa z klientami. Gdy musisz ją zmienić niekompatybilnie (inny kształt JSON, usunięte pole), starzy klienci
 *   (aplikacje mobilne, których nikt nie zaktualizował) przestaliby działać. Rozwiązanie: kilka WERSJI tego samego
 *   endpointu naraz. Od Spring Framework 7 jest to wbudowane: piszesz {@code @GetMapping(path = "/{id}", version = "2.0")},
 *   a w konfiguracji mówisz, SKĄD wziąć wersję z zapytania (nagłówek, parametr, segment ścieżki, parametr typu
 *   mediów). Spring sam wybiera metodę, odrzuca nieznane wersje (400) i umie oznaczać wersje wycofywane nagłówkami.
 *
 * ANALOGIA: wydania podręcznika w szkole.
 *   Część klasy ma wydanie 1, część — wydanie 2 z innymi numerami stron. Nauczyciel (serwer) pyta „które masz wydanie?”
 *   (nagłówek API-Version) i podaje numer strony właściwy dla tego wydania (metoda dla wersji 1.0 albo 2.0).
 *   Kto nie powie, dostaje wersję domyślną. Wydania 3 jeszcze nie ma — „nie ma takiego wydania” (400).
 *   Na starym wydaniu jest naklejka „wycofane, od września tylko wydanie 2” (nagłówki Deprecation i Sunset).
 *
 * JAK TO DZIAŁA:
 *   1. Boot czyta spring.mvc.apiversion.* (WebMvcProperties.Apiversion) i woła ApiVersionConfigurer (konfigurator
 *      wersji); potem wywołuje configureApiVersioning(...) z Twoich beanów WebMvcConfigurer — na tym samym obiekcie.
 *   2. Z konfiguratora powstaje bean ApiVersionStrategy (strategia wersji; domyślnie DefaultApiVersionStrategy):
 *      ApiVersionResolver (odczytywacz wersji: z nagłówka, parametru, ścieżki), ApiVersionParser (parser — domyślnie
 *      semantyczny: „1” = „1.0” = „1.0.0”), wersja domyślna, wymagalność, lista obsługiwanych wersji.
 *   3. Przy zapytaniu RequestMappingHandlerMapping pyta strategię o wersję, sprawdza ją (brak → 400 albo domyślna;
 *      nieznana → 400) i wybiera metodę, której atrybut version pasuje — tak jak wcześniej wybierał po ścieżce i metodzie
 *      HTTP (s04_web_rest/Rest01Controllers).
 *   4. Po wyborze metody ApiVersionDeprecationHandler (obsługa wycofania) może dopisać nagłówki Deprecation/Sunset/Link.
 *
 * SŁÓWKA:
 *   version = wersja; breaking change = zmiana niekompatybilna („psująca” starych klientów); backward compatible =
 *   zgodny wstecz; baseline = wersja bazowa („od tej wersji w górę”); default = domyślny; required = wymagany;
 *   supported = obsługiwany; deprecated = wycofywany (jeszcze działa, ale odradzany); sunset = data wyłączenia
 *   („zachód słońca”); resolver = odczytywacz; parser = parser (zamienia napis na obiekt); inserter = „wstawiacz”.
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest01Controllers (jak HandlerMapping wybiera metodę),
 *             s04_web_rest/Rest05CrudApi (API produktów — tu pokazujemy, jak je rozwijać bez psucia klientów),
 *             s04_web_rest/Rest04Json (kształt JSON, BigDecimal w cenach),
 *             s02_configuration/Cfg03Profiles (profil rest07 i plik application-rest07.properties),
 *             s14_openapi (dokumentowanie wersji API w OpenAPI), s13_rest_client (klient RestClient z apiVersion).
 * </pre>
 */
public final class Rest07ApiVersioning {

    private Rest07ApiVersioning() {
    }

    // =================================================================================================
    // 1. PO CO WERSJONOWANIE
    // =================================================================================================
    //
    // Wersja 1.0 API zwracała cenę jako liczbę groszy:   {"id":1,"name":"Kawa","priceGrosze":2490}
    // Sklep wchodzi na rynek zagraniczny i potrzebuje waluty: {"id":1,"name":"Kawa","price":{"amount":24.90,"currency":"PLN"}}
    //
    // To zmiana NIEKOMPATYBILNA: aplikacja mobilna, która czyta priceGrosze, po zmianie dostanie brak pola i pokaże
    // „0 zł” albo się wywróci. Aplikacji u klientów nie zaktualizujesz jednym kliknięciem — część ludzi ma starą wersję
    // przez miesiące. Dlatego serwer przez jakiś czas obsługuje OBIE wersje i daje klientom czas na przejście.
    //
    //   Zmiana                                        | Zgodna wstecz? | Potrzebna nowa wersja?
    //   ----------------------------------------------+----------------+-----------------------------------------
    //   nowe, opcjonalne pole w odpowiedzi            | tak            | zwykle nie (klienci ignorują nieznane pola)
    //   nowy endpoint                                 | tak            | nie (ewentualnie „od wersji X”: baseline)
    //   zmiana nazwy/typu pola, usunięcie pola        | NIE            | tak
    //   nowe WYMAGANE pole w żądaniu                  | NIE            | tak
    //   zmiana znaczenia (np. cena brutto → netto)    | NIE            | tak — najgroźniejsza, bo JSON wygląda tak samo
    //
    // DOBRA PRAKTYKA: najpierw próbuj zmian zgodnych wstecz (dodaj pole, nie zmieniaj starego); nową wersję twórz tylko
    //   dla zmian niekompatybilnych. Dlaczego? Każda wersja to kod do utrzymania, testowania i dokumentowania.
    //   PYTANIE REKRUTACYJNE: „Jakie zmiany w API REST są zgodne wstecz, a jakie wymagają nowej wersji?”

    // =================================================================================================
    // 2. WERSJA W NAGŁÓWKU + ATRYBUT version (ProductController, application-rest07.properties)
    // =================================================================================================
    //
    // Konfiguracja (application-rest07.properties, profil włączany przez Rest07Application i testy @ActiveProfiles):
    //
    //   spring.mvc.apiversion.use.header=API-Version     ← skąd brać wersję
    //   spring.mvc.apiversion.default=1.0                ← co, gdy klient nie poda (sekcja 4)
    //
    // Kontroler — ten sam adres, różne wersje, różne metody i różne DTO:
    //
    //   @GetMapping(path = "/{id}", version = "1.0")  public ResponseEntity<ProductV1> byIdV1(...)
    //   @GetMapping(path = "/{id}", version = "2.0")  public ResponseEntity<ProductV2> byIdV2(...)
    //
    // Test versionFromHeader (uruchomiony): API-Version: 1.0 → priceGrosze 2490 i brak pola price; API-Version: 2.0 →
    // price.amount 24.90, price.currency PLN i brak priceGrosze. Dane są wspólne (ProductCatalog), a wersje różnią się
    // tylko „tłumaczeniem” na DTO — logika biznesowa się nie dubluje.
    //
    // Wersje są SEMANTYCZNE (SemanticApiVersionParser: major.minor.patch). Test semanticVersions: „1”, „v1” i „1.0.0”
    // trafiają do metody z version = "1.0". Porównanie jest liczbowe, nie tekstowe: 1.10 jest nowsze niż 1.9 (ćwiczenie 4).
    //
    // MockMvc w Spring 7 ma w budowniczym zapytania metodę apiVersion(...). Sama nie wie, gdzie wstawić wersję —
    // potrzebuje ApiVersionInserter (test mockMvcApiVersion):
    //
    //   get("/rest07/products/1").apiVersionInserter(ApiVersionInserter.useHeader("API-Version")).apiVersion("2.0")
    //
    // Bez insertera test kończy się IllegalStateException „No ApiVersionInserter” (sprawdzone uruchomieniem). Ten sam
    // ApiVersionInserter ustawia się w kliencie RestClient (RestClient.builder().apiVersionInserter(...) i
    // .apiVersion(...) przy zapytaniu) — więcej w dziale s13_rest_client.
    //
    // Stare tutoriale (przed Spring 7) robią to ręcznie, np. @GetMapping(path = "/{id}", headers = "API-Version=2")
    // albo osobnymi ścieżkami /v1/… i /v2/…. Działa, ale bez walidacji wersji, wersji domyślnej, „1.1+” i nagłówków
    // wycofania — to wszystko trzeba było pisać samemu.

    // =================================================================================================
    // 3. INNE STRATEGIE: PARAMETR ZAPYTANIA, SEGMENT ŚCIEŻKI, TYP MEDIÓW
    // =================================================================================================
    //
    //   Właściwość Boot                                   | Konfiguracja w Javie (ApiVersionConfigurer) | Zapytanie
    //   --------------------------------------------------+---------------------------------------------+---------------------------
    //   spring.mvc.apiversion.use.header=API-Version      | useRequestHeader("API-Version")             | API-Version: 2.0
    //   spring.mvc.apiversion.use.query-parameter=api-version | useQueryParam("api-version")            | /products/1?api-version=2.0
    //   spring.mvc.apiversion.use.path-segment=1          | usePathSegment(1)                           | /api/v2/orders/5
    //   spring.mvc.apiversion.use.media-type-parameter.…  | useMediaTypeParameter(typ, "v")             | Accept: application/json;v=2
    //   (media-type-parameter to mapa „typ mediów → nazwa parametru”; tej strategii w lekcji nie uruchamiamy)
    //
    // Parametr zapytania — test Rest07QueryParameterTest: TEN SAM ProductController, tylko inna właściwość
    // (@WebMvcTest(properties = "spring.mvc.apiversion.use.query-parameter=api-version")). ?api-version=2.0 daje kształt
    // z wersji 2.0, a nagłówek API-Version jest wtedy ignorowany. Kontroler nie zmienił się ani o znak — strategia
    // to sprawa konfiguracji, a nie kodu endpointów.
    //
    // Segment ścieżki — test Rest07JavaConfigTest.versionFromPathSegment. Mapowanie musi mieć miejsce na wersję
    // ({version}), a indeks liczy się od zera (segment 0 = "api", segment 1 = wersja):
    //
    //   @RequestMapping("/api/{version}/orders")                 configurer.usePathSegment(1);
    //   @GetMapping(path = "/{id}", version = "1") …
    //
    // Wynik: /api/v1/orders/5 i /api/1/orders/5 → wersja 1, /api/v2/orders/5 → 2, /api/v3/orders/5 → 400.
    // Ten test używa konfiguracji w JAVIE (WebMvcConfigurer.configureApiVersioning) w osobnym, małym kontekście —
    // strategia „ze ścieżki” narzuca układ adresów CAŁEJ aplikacji, więc nie mieszamy jej z resztą lekcji.
    // (Gdy wersja ma być w ścieżce tylko części adresów, jest wariant usePathSegment(indeks, warunekNaŚcieżkę).)
    //
    // Kiedy właściwości, a kiedy Java? Właściwości wystarczą do wyboru strategii, wersji domyślnej, wymagalności i listy
    // wersji. Javy (WebMvcConfigurer.configureApiVersioning) potrzebujesz do rzeczy „z kodem”: obsługi wycofania
    // (ApiDeprecationConfig, sekcja 4), własnego parsera albo odczytywacza wersji, warunku dla segmentu ścieżki.

    // =================================================================================================
    // 4. „1.1+”, WERSJA DOMYŚLNA, WYMAGANA, NIEOBSŁUGIWANA, WYCOFANIE (Deprecation/Sunset)
    // =================================================================================================
    //
    // Baseline (wersja bazowa) — version = "1.1+": endpoint /products/{id}/stock dodano w 1.1 i ma działać w każdej
    // NOWSZEJ wersji bez kopiowania metody (test baselineVersion: 1.1 i 2.0 → 200, 1.0 → 404, bo dla 1.0 tego
    // endpointu po prostu nie ma). Mapowanie BEZ atrybutu version (/products/ping) pasuje do każdej wersji.
    //
    // Co się dzieje z wersją z zapytania (wszystko sprawdzone testami):
    //
    //   Sytuacja                                              | Wynik
    //   ------------------------------------------------------+----------------------------------------------------------
    //   brak wersji, jest default=1.0                         | używa 1.0 (defaultVersionAndUnversionedMapping)
    //   brak wersji, brak default (wtedy wersja jest wymagana)| 400 „API version is required.” — MissingApiVersionException
    //                                                         | (Rest07QueryParameterTest.missingVersionIsRequired)
    //   brak wersji, required=false i brak default            | NAJNOWSZA wersja, tu 2.0 (Rest07OptionalVersionTest)
    //   wersja nieznana: 3.0 albo „abc”                       | 400 „Invalid API version: '3.0.0'.” — InvalidApiVersionException
    //   wersja znana (1.1 — z „1.1+”), ale /products/{id}     | 400 — NotAcceptableApiVersionException
    //   nie ma metody dla 1.1                                 |   (supportedButNotMappedVersion)
    //   wersja starsza niż baseline (1.0 na „1.1+”)           | 404 (baselineVersion)
    //
    // Skąd Spring wie, które wersje są „znane”? Domyślnie WYKRYWA je z atrybutów version w kontrolerach (tu: 1.0, 1.1,
    // 2.0; właściwość spring.mvc.apiversion.detect-supported). Dodatkowe wersje dopisujesz listą
    // spring.mvc.apiversion.supported — test supportedVersions: z supported=1.5 wersja 1.5 jest przyjmowana i pasuje do
    // „1.1+” (200), a 1.7 — nie (400).
    //
    // PUŁAPKA: required=false bez wersji domyślnej. Dlaczego groźne? Klient, który nie wysyła wersji (np. stara aplikacja
    //   napisana PRZED wprowadzeniem wersjonowania), dostaje NAJNOWSZĄ wersję — czyli właśnie ten kształt JSON, którego
    //   nie rozumie. Ustaw default na wersję, którą znali dotychczasowi klienci (tu 1.0), albo wymagaj wersji.
    //
    // PUŁAPKA: oczekiwanie 404 dla nieobsługiwanej wersji. Dlaczego mylące? Spring odpowiada 400 — błąd jest w zapytaniu
    //   (zła wersja), a nie „brak zasobu”. 404 dostaniesz tylko, gdy wersja jest poprawna, ale ścieżka w tej wersji nie
    //   istnieje (baseline). Klient powinien rozróżniać te przypadki.
    //
    // Wycofanie wersji (deprecation) — ApiDeprecationConfig (WebMvcConfigurer.configureApiVersioning):
    //
    //   StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler();
    //   handler.configureVersion("1.0")
    //           .setDeprecationDate(ZonedDateTime.parse("2026-01-01T00:00:00Z"))
    //           .setSunsetDate(ZonedDateTime.parse("2027-01-01T00:00:00Z"))
    //           .setSunsetLink(URI.create("https://sklep.example.com/docs/api/migracja-v2"));
    //   configurer.setDeprecationHandler(handler);
    //
    // Test deprecationHeaders — odpowiedź w wersji 1.0 ma (wersja 2.0 nie ma):
    //   Deprecation: @1767225600                ← od kiedy wycofana (standard RFC 9745: „@” + sekundy od 1970-01-01)
    //   Sunset: Fri, 1 Jan 2027 00:00:00 GMT    ← od kiedy przestanie działać (RFC 8594, data w formacie HTTP)
    //   Link: <https://sklep.example.com/docs/api/migracja-v2>; rel="sunset"; type="text/html"   ← gdzie czytać dalej
    // Daty są stałe (nie „teraz”), więc test jest powtarzalny. Uwaga: wersja domyślna 1.0 też dostaje te nagłówki
    // (ten sam test, zapytanie bez API-Version) — klient bez nagłówka wersji dowie się, że korzysta z wycofywanej wersji.
    //
    // DOBRA PRAKTYKA: wycofanie ogłaszaj nagłówkami z wyprzedzeniem (Deprecation, potem Sunset), a dopiero po dacie
    //   Sunset usuń metody wersji 1.0. Dlaczego? Klienci i ich monitoring widzą ostrzeżenie w każdej odpowiedzi, zanim coś
    //   przestanie działać — zamiast nagłego 400 pewnego poranka.

    // =================================================================================================
    // 5. PUŁAPKI I PORÓWNANIE STRATEGII
    // =================================================================================================
    //
    // PUŁAPKA: ręczne „/v1” i „/v2” w każdej ścieżce (@GetMapping("/v1/products/{id}"), @GetMapping("/v2/products/{id}")).
    //   Dlaczego gorsze od wbudowanego wsparcia? Spring nie wie, że to wersje: nieznana wersja /v3/… daje 404 zamiast 400,
    //   nie ma wersji domyślnej ani „od wersji 1.1 w górę” (każdy endpoint trzeba skopiować do każdej wersji), a wycofanie
    //   ogłaszasz ręcznie. Jeśli chcesz wersję w adresie — użyj usePathSegment (sekcja 3), a Spring zrobi resztę.
    //
    // PUŁAPKA: atrybut version bez skonfigurowanej strategii. Co się stanie? Aplikacja NIE STARTUJE: „API version
    //   specified, but no ApiVersionStrategy configured” (test versionWithoutStrategyFailsAtStartup). Dlatego wszystkie
    //   testy tej lekcji włączają profil rest07 albo podają spring.mvc.apiversion.* we właściwościach testu.
    //
    //   Strategia          | Zalety                                              | Wady
    //   -------------------+-----------------------------------------------------+-----------------------------------------
    //   nagłówek           | adres zasobu się nie zmienia; czysty URL             | niewidoczna w pasku przeglądarki/linku;
    //                      |                                                     | pamięci podręczne muszą uwzględniać nagłówek
    //   parametr zapytania | łatwo testować w przeglądarce i curl                 | miesza się z parametrami filtrowania
    //   segment ścieżki    | najbardziej widoczna, łatwa do logów i routingu     | ten sam zasób ma różne adresy; narzuca
    //                      | (bramki API, proxy)                                  | układ wszystkich ścieżek
    //   typ mediów         | zgodna z ideą „negocjacji treści” (Accept)          | najtrudniejsza dla klientów i narzędzi
    //
    // DOBRA PRAKTYKA: wybierz JEDNĄ strategię dla całego API i opisz ją w dokumentacji (s14_openapi). Dlaczego?
    //   Klient musi wiedzieć, gdzie podać wersję; mieszanie strategii w jednym API to przepis na pomyłki.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @GetMapping(path = "/{id}", version = "2.0") — metoda dla wersji; "1.1+" = od 1.1 w górę; bez version = każda.
     *   • Skąd wersja: spring.mvc.apiversion.use.header / .query-parameter / .path-segment / .media-type-parameter
     *     (albo WebMvcConfigurer.configureApiVersioning: useRequestHeader, useQueryParam, usePathSegment…).
     *   • spring.mvc.apiversion.default — wersja przy braku; .required — czy wymagana (bez default: tak);
     *     .supported — dodatkowe wersje; .detect-supported — wykrywanie z kontrolerów (domyślnie włączone).
     *   • Brak wymaganej wersji → 400 „API version is required.”; nieznana → 400 „Invalid API version”; poniżej baseline → 404.
     *   • Wersje semantyczne: „1” = „v1” = „1.0.0”; 1.10 nowsze niż 1.9.
     *   • Wycofanie: StandardApiVersionDeprecationHandler.configureVersion(...).setDeprecationDate/setSunsetDate/
     *     setSunsetLink → nagłówki Deprecation, Sunset, Link.
     *   • Testy: header("API-Version", "2.0") albo apiVersionInserter(...).apiVersion("2.0") w MockMvc.
     *   • version bez strategii → aplikacja nie startuje.
     *
     * PYTANIA KONTROLNE:
     *   1. Które zmiany w API wymagają nowej wersji: dodanie opcjonalnego pola, zmiana nazwy pola, nowy endpoint,
     *      zmiana ceny z brutto na netto bez zmiany nazwy pola?
     *   2. Co oznacza version = "1.1+" i dla jakich wersji zadziała taki endpoint?
     *   3. Co się stanie? Klient wysyła API-Version: 3.0, a w aplikacji są tylko metody dla 1.0 i 2.0.
     *   4. Co się stanie? Ustawiono spring.mvc.apiversion.use.header=API-Version i required=false, bez default,
     *      a stara aplikacja mobilna pyta GET /rest07/products/1 bez nagłówka wersji.
     *   5. ZNAJDŹ BŁĄD: w nowej lekcji dodano {@code @GetMapping(path = "/{id}", version = "1")}, ale nie ustawiono
     *      żadnej właściwości spring.mvc.apiversion.*. Co się stanie przy starcie?
     *   6. ZNAJDŹ BŁĄD: test MockMvc {@code get("/rest07/products/1").apiVersion("2.0")} kończy się wyjątkiem. Czego brakuje?
     *   7. Jakie nagłówki informują klienta o wycofaniu wersji i co oznacza każdy z nich?
     *   8. Porównaj wersję w nagłówku i w ścieżce — po jednej zalecie i wadzie każdej.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielety: ExerciseController (1–3), ExerciseVersions (4–5);
    //             testy: Rest07ApiVersioningExercisesTest (@Tag("cwiczenie"), profil rest07 — wersja z nagłówka API-Version)
    // =================================================================================================
    //
    // Mapowania z wersjami w ExerciseController są gotowe — piszesz ciała metod. Rozwiązania wzorcowe:
    // Rest07SolutionsController i Rest07SolutionsVersions (s04_web_rest.solutions.rest07_api_versioning).
    //
    // ĆWICZENIE 1 (łatwe): byIdV1(id) — wersja 1.0 zwraca CustomerV1(id, "Jan Kowalski").
    //   Podpowiedź: zwykłe „return new …” — wersję wybrał już Spring na podstawie version = "1.0" (sekcja 2).
    //
    // ĆWICZENIE 2 (łatwe): byIdV2(id) — wersja 2.0 zwraca CustomerV2(id, "Jan", "Kowalski"). Test sprawdza też,
    //   że w JSON-ie wersji 2.0 NIE ma pola fullName. Podpowiedź: inny rekord = inny kształt JSON.
    //
    // ĆWICZENIE 3 (średnie): orders(id) — endpoint z version = "1.1+" zwraca listę "Z-<id>-1", "Z-<id>-2". Przed
    //   uruchomieniem testu odpowiedz sobie: jaki status dostanie wersja 1.0 i dlaczego? Podpowiedź: sekcja 4, baseline.
    //
    // ĆWICZENIE 4 (średnie): ExerciseVersions.isNewer(candidate, current) — porównaj wersje jak Spring: "1.10" nowsza niż
    //   "1.9", "2" równa "2.0.0", "v2" nowsza niż "1.5". Podpowiedź: new SemanticApiVersionParser().parseVersion(napis)
    //   zwraca obiekt Comparable — użyj compareTo (sekcja 2).
    //
    // ĆWICZENIE 5 (trudniejsze): ExerciseVersions.deprecationOf1dot1() — StandardApiVersionDeprecationHandler dla wersji
    //   1.1: wycofana od 2026-06-01T00:00Z, wyłączenie 2027-06-30T00:00Z, link https://sklep.example.com/docs/api/v2.
    //   Test wywołuje handleVersion(...) na sztucznym zapytaniu i sprawdza nagłówki Deprecation, Sunset i Link.
    //   Podpowiedź: wzór w ApiDeprecationConfig (sekcja 4).

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Nowej wersji wymagają: zmiana nazwy pola i zmiana znaczenia (brutto → netto) — ta druga jest najgroźniejsza,
     *      bo JSON wygląda tak samo, a klient po cichu liczy źle. Opcjonalne pole i nowy endpoint są zgodne wstecz.
     *   2. Wersja bazowa: endpoint działa dla 1.1 i każdej nowszej obsługiwanej wersji (np. 2.0), a dla starszej (1.0)
     *      mapowania nie ma → 404. Sprawdzone testem baselineVersion.
     *   3. 400 Bad Request z powodem „Invalid API version: '3.0.0'.” (InvalidApiVersionException) — wersja spoza listy
     *      obsługiwanych. Sprawdzone testem unsupportedVersion.
     *   4. Dostanie NAJNOWSZĄ wersję (2.0, cena jako obiekt) — kształt, którego stara aplikacja nie zna. Sprawdzone testem
     *      Rest07OptionalVersionTest.noVersionMeansLatest. Naprawa: default=1.0 albo wymaganie wersji.
     *   5. Aplikacja nie wystartuje: „API version specified, but no ApiVersionStrategy configured” — atrybut version
     *      wymaga strategii (np. spring.mvc.apiversion.use.header). Sprawdzone testem versionWithoutStrategyFailsAtStartup.
     *   6. Brakuje ApiVersionInserter — bez niego MockMvc nie wie, gdzie wstawić wersję, i rzuca IllegalStateException
     *      „No ApiVersionInserter”. Dodaj .apiVersionInserter(ApiVersionInserter.useHeader("API-Version")) albo wyślij
     *      nagłówek ręcznie: .header("API-Version", "2.0"). Sprawdzone uruchomieniem (test mockMvcApiVersion pokazuje
     *      poprawną wersję).
     *   7. Deprecation — od kiedy wersja jest wycofana (odradzana, ale działa); Sunset — od kiedy przestanie działać;
     *      Link z rel="sunset" — gdzie przeczytać, jak przejść na nowszą wersję. Sprawdzone testem deprecationHeaders.
     *   8. Nagłówek: adres zasobu się nie zmienia, ale wersji nie widać w linku i pamięci podręczne muszą brać nagłówek
     *      pod uwagę. Ścieżka: wersja widoczna od razu (logi, linki, bramki API), ale ten sam zasób ma kilka adresów,
     *      a strategia narzuca układ wszystkich ścieżek.
     */
    // </editor-fold>
}
