package s04_web_rest.rest07_api_versioning;

/**
 * <pre>
 * TEMAT: Wersjonowanie API wbudowane w Spring Framework 7 — atrybut {@code version} w {@code @GetMapping},
 *        źródło wersji (nagłówek, parametr, ścieżka, typ mediów), właściwości spring.mvc.apiversion.*, wersja bazowa
 *        „1.0+”, wersja domyślna, wspierane wersje, błędy 400/404 i wycofywanie wersji (Deprecation, Sunset)
 *        (API versioning = wersjonowanie API; baseline = wersja bazowa; deprecation = oznaczenie jako przestarzałe;
 *        sunset = data wyłączenia)
 *
 * W SKRÓCIE:
 *   API żyje latami, a klienci (aplikacje mobilne, partnerzy) aktualizują się wolno. Gdy zmieniasz kształt odpowiedzi,
 *   stara wersja musi działać dalej. Dawniej robiło się to ręcznie: /api/v1/products i /api/v2/products. Spring
 *   Framework 7 (Boot 4) ma wersjonowanie WBUDOWANE: ten sam adres, a o wyborze metody decyduje wersja z zapytania
 *   (np. nagłówek API-Version: 2.0) i atrybut {@code @GetMapping(path = "/{sku}", version = "2.0+")}.
 *   Skąd brać wersję i jakie są wspierane — ustawiasz właściwościami spring.mvc.apiversion.* albo w kodzie
 *   (WebMvcConfigurer.configureApiVersioning). Nieznana wersja → 400. Starą wersję możesz oznaczyć jako
 *   przestarzałą — klient dostanie nagłówki Deprecation i Sunset.
 *
 * ANALOGIA: instrukcje obsługi ekspresu do kawy.
 *   Sklep sprzedaje ekspres w modelu 2024 i 2026. Klient dzwoni na infolinię (ten sam numer = ten sam adres) i podaje
 *   model (wersję). Konsultant wyciąga właściwą instrukcję. Model, którego nie było (2030) → „nie znamy takiego” (400).
 *   Klient nie podał modelu → dostaje instrukcję domyślną. Model 2024 dostaje dopisek „wsparcie kończy się
 *   31 marca 2027” (Sunset). Ręczne wersjonowanie to osobny numer infolinii dla każdego modelu.
 *
 * JAK TO DZIAŁA:
 *   1. Przy starcie Spring MVC buduje ApiVersionStrategy (bean mvcApiVersionStrategy): listę ApiVersionResolver
 *      (skąd czytać wersję), parser (domyślnie SemanticApiVersionParser: „1” = „1.0” = „1.0.0”), wersję domyślną,
 *      wspierane wersje i ApiVersionDeprecationHandler. Boot wypełnia je z spring.mvc.apiversion.*.
 *   2. RequestMappingHandlerMapping dla każdej metody z version tworzy warunek VersionRequestCondition
 *      i zapamiętuje wersję jako „wykrytą” (detect-supported).
 *   3. Przy KAŻDYM zapytaniu: wersja z nagłówka/parametru → parsowanie → walidacja (brak? nieznana?) → wybór metody:
 *      spośród pasujących wygrywa NAJWYŻSZA wersja nie większa niż żądana; wersja stała ("1.0") musi być równa
 *      żądanej. Po wyborze — nagłówki wycofania, jeśli wersja jest oznaczona.
 *
 * SŁÓWKA:
 *   version resolver = „odczytywacz” wersji (skąd ją wziąć); parser = analizator (tekst → wersja); semantic version =
 *   wersja semantyczna (major.minor.patch, np. 1.2.0); supported = wspierany; required = wymagany; default = domyślny;
 *   path segment = segment ścieżki (/api/v1/... — „v1” to segment 1); media type parameter = parametr typu mediów
 *   (application/json;v=2.0); not acceptable = nie do przyjęcia; breaking change = zmiana łamiąca zgodność.
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest01Controllers (mapowanie), s04_web_rest/Rest03Responses (produces/consumes),
 *             s02_configuration/Cfg03Profiles (profil rest07 tej lekcji), s13_rest_client/Rc01RestClient (RestClient
 *             też umie wysyłać wersję: apiVersionInserter), s14_openapi/Api03GroupsAndVersions (dokumentacja wersji API).
 * </pre>
 */
public final class Rest07ApiVersioning {

    private Rest07ApiVersioning() {
    }

    // =================================================================================================
    // 1. RĘCZNIE (/api/v1/...) KONTRA WBUDOWANE (version = "2.0+") (ManualVersionController; test manualVersioning)
    // =================================================================================================
    //
    //   ręcznie:     @GetMapping("/api/v1/catalog/{sku}")  i  @GetMapping("/api/v2/catalog/{sku}")
    //   wbudowane:   @GetMapping(path = "/{sku}", version = "1.0+")  i  @GetMapping(path = "/{sku}", version = "2.0+")
    //
    // Ręcznie: proste i widoczne w adresie, ale każda wersja to kopia WSZYSTKICH adresów (także tych, które się nie
    // zmieniły), a „nieznana wersja” to po prostu 404. Wbudowane: jedna metoda obsługuje wiele wersji (wersja bazowa),
    // nowa wersja dotyka tylko tego, co się zmieniło, Spring sam waliduje wersję (400 dla nieznanej), zna wersję
    // domyślną i umie oznaczać wersje jako przestarzałe. Wersję możesz nadal trzymać w ścieżce (sekcja 3).
    //
    // DOBRA PRAKTYKA: nowa wersja tylko przy zmianie ŁAMIĄCEJ zgodność (usunięcie/zmiana nazwy pola, zmiana typu).
    //   Dlaczego? Dodanie nowego pola nie psuje klientów (Jackson domyślnie ignoruje nieznane pola —
    //   s04_web_rest/Rest04Json), a każda wersja to kod, który trzeba utrzymywać i testować.

    // =================================================================================================
    // 2. ATRYBUT version: STAŁA "1.0" I BAZOWA "1.0+" (VersionedProductController)
    // =================================================================================================
    //
    //   version = "1.0+"   wersja bazowa: obsługuje 1.0, 1.1, 1.5... aż do wersji, w której adres przejmie inna metoda
    //   version = "2.0+"   przejmuje adres od 2.0 wzwyż
    //   version = "1.1+"   adres DODANY w 1.1 — dla zapytania w wersji 1.0 tego adresu nie ma → 404
    //   version = "1.0"    wersja stała: TYLKO 1.0; zapytanie 1.1 → 400 (NotAcceptableApiVersionException)
    //   bez version        metoda dla każdej wersji (jak ManualVersionController)
    //
    // Jak Spring wybiera? Spośród metod pasujących adresem bierze tę z NAJWYŻSZĄ wersją, która nie jest większa od
    // żądanej. Dla API-Version: 1.5 → "1.0+" (bo "2.0+" > 1.5). Dla 2.0 → "2.0+". Gdy wybrana metoda ma wersję stałą
    // różną od żądanej — 400.
    //
    // DOBRA PRAKTYKA: domyślnie wersje bazowe ("+"). Dlaczego? Gdy wydasz wersję 1.5 (zmiana w zamówieniach), adresy
    //   produktów działają w 1.5 bez dopisywania czegokolwiek. Wersja stała zmusza do kopiowania metody dla każdej
    //   nowej wersji — inaczej klient nowej wersji dostanie 400.

    // =================================================================================================
    // 3. SKĄD WERSJA: nagłówek, parametr, ścieżka, typ mediów (application-rest07.properties; StrategiesTest)
    // =================================================================================================
    //
    //   właściwość Boota                                              kod (ApiVersionConfigurer)        przykład zapytania
    //   spring.mvc.apiversion.use.header=API-Version                  useRequestHeader("API-Version")   API-Version: 2.0
    //   spring.mvc.apiversion.use.query-parameter=version             useQueryParam("version")          ?version=2.0
    //   spring.mvc.apiversion.use.path-segment=1                      usePathSegment(1)                 /api/v2/catalog/...
    //   spring.mvc.apiversion.use.media-type-parameter[application/json]=v
    //       kod: useMediaTypeParameter(MediaType.APPLICATION_JSON, "v")     zapytanie: Accept: application/json;v=2.0
    //
    // Kilka źródeł naraz → sprawdzane po kolei, pierwsze znalezione wygrywa (u nas nagłówek przed parametrem).
    // Wersja w ścieżce: wzorzec adresu ma zmienną {version} (/api/{version}/catalog/{sku}), a wybór metody i tak robi
    // atrybut version. Parser pomija litery na początku, więc „v2” w ścieżce to 2.0.
    //
    // PUŁAPKA: przy usePathSegment(1) KAŻDY adres aplikacji ma w segmencie 1 „wersję”. Zwykłe /api/health (bez wersji
    //   w ścieżce) dostaje 400, bo „health” zostaje odczytane jako wersja i nie jest nią (test pathSegment).
    //   Dlaczego? Ten odczytywacz nigdy nie zwraca „brak wersji”. Adresy bez wersji obsłużysz wariantem z predykatem
    //   usePathSegment(index, predicate) (Spring 7.0.6+), który mówi, które ścieżki są wersjonowane.
    //
    // PUŁAPKA: (w testach) MockMvc .param("version", "2.0") NIE działa dla wersji z parametru — QueryApiVersionResolver
    //   czyta surowy query string (request.getQueryString()), a param() go nie ustawia. Dlaczego? param() symuluje
    //   parametr formularza. W teście użyj .queryParam("version", "2.0") albo wpisz ?version=2.0 w adres.

    // =================================================================================================
    // 4. BRAK WERSJI, NIEZNANA WERSJA, WSPIERANE WERSJE (Rest07ApiVersioningTest, StrategiesTest)
    // =================================================================================================
    //
    //   spring.mvc.apiversion.default=1.0        zapytanie bez wersji = 1.0 (wtedy wersja NIE jest wymagana)
    //   spring.mvc.apiversion.required=true      brak wersji (i brak domyślnej) → 400, MissingApiVersionException:
    //                                            „API version is required.”
    //   brak default i required=false            zapytanie bez wersji dostaje NAJNOWSZĄ wersję (test notRequiredNoDefault…)
    //   spring.mvc.apiversion.supported=1.5      dodatkowe wspierane wersje (oprócz wykrytych z mapowań)
    //   spring.mvc.apiversion.detect-supported=false   tylko wersje z listy supported (i domyślna) są wspierane
    //
    // Wspierane = wykryte z atrybutów version (1.0, 1.1, 2.0) + supported (1.5) + domyślna. Inna wersja → 400 i
    // InvalidApiVersionException: „Invalid API version: '3.0.0'.” (wersja po parsowaniu, stąd 3.0.0). Tekst, który
    // nie jest wersją („abc”) → „Invalid API version: 'abc'.”.
    //
    // PUŁAPKA: wersjonowanie działa dla CAŁEJ aplikacji, nie tylko dla metod z version. Zapytanie z API-Version: 3.0 do
    //   /api/v1/catalog (bez atrybutu version) też dostaje 400. Dlaczego? Wersja jest czytana i walidowana dla każdego
    //   zapytania, zanim Spring wybierze metodę. Klient, który wysyła nagłówek „na wszelki wypadek”, musi wysyłać
    //   wersję poprawną.
    //
    // PUŁAPKA: atrybut version bez skonfigurowanego wersjonowania (żadnej właściwości use.*, żadnego
    //   configureApiVersioning) — aplikacja NIE wystartuje: „API version specified, but no ApiVersionStrategy
    //   configured”. Dlatego Rest07Application włącza profil rest07; ten sam błąd zobaczysz, uruchamiając ją przez
    //   SpringApplication.run bez profilu (sprawdzone uruchomieniem).

    // =================================================================================================
    // 5. WYCOFYWANIE WERSJI: Deprecation, Link, Sunset (ApiDeprecationConfig; test deprecationHeaders)
    // =================================================================================================
    //
    //   StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler();
    //   handler.configureVersion("1.0")
    //          .setDeprecationDate(2026-09-01T00:00Z)          → Deprecation: @1788220800   (sekundy od 1970-01-01)
    //          .setDeprecationLink(URI.create("https://..."))  → Link: <https://...>; rel="deprecation"; type="text/html"
    //          .setSunsetDate(2027-03-31T00:00Z);              → Sunset: Wed, 31 Mar 2027 00:00:00 GMT
    //   configurer.setDeprecationHandler(handler);            (albo bean ApiVersionDeprecationHandler — Boot go użyje)
    //
    // Nagłówki dostaje KAŻDA odpowiedź dla wersji 1.0 — także zapytania BEZ wersji, bo domyślna to 1.0 (i nawet
    // adresy bez atrybutu version). To celowe: najstarsze klienty, które nie wysyłają wersji, dowiadują się, że czas
    // na migrację. Deprecation i Sunset to standardowe nagłówki HTTP (RFC 9745 i RFC 8594) — klient może je odczytać
    // i np. zapisać w logu ostrzeżenie „ta wersja API zostanie wyłączona 31.03.2027”.
    //
    // DOBRA PRAKTYKA: daty wycofania jako stałe w konfiguracji (albo właściwości), nie „teraz + 6 miesięcy”. Dlaczego?
    //   Klient musi dostawać tę samą datę przy każdym zapytaniu i po każdym restarcie serwera; „teraz + 6 miesięcy”
    //   przesuwa datę wyłączenia przy każdym uruchomieniu aplikacji.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @GetMapping(path = "/{sku}", version = "2.0+") — "+" = wersja bazowa (od tej wzwyż), bez "+" = tylko ta.
     *   • Źródło wersji: spring.mvc.apiversion.use.header / .query-parameter / .path-segment / .media-type-parameter[...]
     *     albo WebMvcConfigurer.configureApiVersioning(ApiVersionConfigurer) — useRequestHeader, useQueryParam...
     *   • spring.mvc.apiversion.default (brak wersji = ta), .required, .supported, .detect-supported.
     *   • Wersje semantyczne: „1” = „1.0” = „1.0.0”; „v2” = 2.0.
     *   • Nieznana/zła wersja → 400 InvalidApiVersionException; brak wymaganej → 400 MissingApiVersionException;
     *     wersja stała ≠ żądana → 400 NotAcceptableApiVersionException; adres dodany później → 404.
     *   • version w mapowaniu bez konfiguracji wersjonowania → błąd startu.
     *   • Wycofanie: StandardApiVersionDeprecationHandler → nagłówki Deprecation, Link, Sunset.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się version = "1.0" od version = "1.0+"?
     *   2. Co się stanie? GET /api/products/ELE-001 z nagłówkiem API-Version: 3.0 — status i komunikat błędu.
     *   3. Co się stanie? GET /api/products/ELE-001/stock BEZ nagłówka wersji (wersja domyślna 1.0, metoda ma "1.1+").
     *   4. ZNAJDŹ BŁĄD: w nowej aplikacji kontroler ma @GetMapping(path = "/api/hello", version = "1.0"), ale nikt nie
     *      ustawił spring.mvc.apiversion.* ani configureApiVersioning. Co się stanie przy starcie?
     *   5. Jakie nagłówki dostaje klient wersji wycofywanej i co oznacza wartość @1788220800?
     *   6. Co się stanie? Wersja niewymagana, bez wersji domyślnej, metody "1.0" i "2.0" na tym samym adresie —
     *      zapytanie bez wersji.
     *   7. ZNAJDŹ BŁĄD: test wysyła mockMvc.perform(get("/api/products/ELE-001").param("version", "2.0")), a dostaje
     *      odpowiedź w wersji 1.0. Dlaczego?
     *   8. Kiedy warto wydać nową wersję API, a kiedy wystarczy dodać pole?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — Rest07ExerciseController (ćwiczenia 1–2) i Rest07Exercises (ćwiczenia 3–4),
    // testy: Rest07ApiVersioningExercisesTest. Rozwiązania wzorcowe: s04_web_rest.solutions.rest07_api_versioning.
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. "1.0" — wersja stała: metoda obsługuje tylko 1.0, a zapytanie 1.1 do tego adresu kończy się 400
     *      (NotAcceptableApiVersionException). "1.0+" — wersja bazowa: obsługuje 1.0 i każdą wyższą, dopóki adresu nie
     *      przejmie metoda z wyższą wersją. Sprawdzone testami fixedVersionRejectsOthers i baselineVersions.
     *   2. 400 Bad Request, InvalidApiVersionException z powodem „Invalid API version: '3.0.0'.” — 3.0 nie jest ani
     *      wykryta z mapowań, ani na liście supported. Na prawdziwym serwerze ciało to JSON błędu Boota (bez powodu).
     *      Sprawdzone testem unsupportedAndInvalidVersions.
     *   3. 404 Not Found (NoResourceFoundException) — w wersji 1.0 adres /stock nie istnieje, bo metoda ma "1.1+".
     *      Sprawdzone testem endpointAddedLater (dla jawnego 1.0) i uruchomieniem (bez nagłówka = domyślne 1.0).
     *   4. Aplikacja nie wystartuje: BeanCreationException przy tworzeniu requestMappingHandlerMapping, przyczyna
     *      IllegalStateException „API version specified, but no ApiVersionStrategy configured”. Sprawdzone testem
     *      Rest07ApiVersioningStrategiesTest.versionWithoutStrategyFailsAtStartup.
     *   5. Deprecation (data oznaczenia jako przestarzała — tu jako @ i liczba sekund od 1970-01-01, czyli
     *      2026-09-01T00:00Z), Link z rel="deprecation" (adres z opisem migracji) i Sunset (data wyłączenia w formacie
     *      daty HTTP). Sprawdzone testem deprecationHeaders.
     *   6. Dostanie NAJNOWSZĄ wersję (2.0) — zapytanie bez wersji „akceptuje każdą”, a Spring wybiera najwyższą.
     *      Sprawdzone testem Rest07ApiVersioningStrategiesTest.notRequiredNoDefaultGivesLatest.
     *   7. param() w MockMvc nie ustawia query stringa, a QueryApiVersionResolver czyta request.getQueryString(). Spring
     *      nie widzi wersji, więc używa domyślnej 1.0. Poprawka: .queryParam("version", "2.0").
     *   8. Nowa wersja — przy zmianie łamiącej zgodność (usunięcie pola, zmiana nazwy lub typu, inny kształt
     *      odpowiedzi). Nowe pole w odpowiedzi nie łamie klientów (nieznane pola są ignorowane), więc nie wymaga wersji.
     */
    // </editor-fold>
}
