package s04_web_rest.rest07_api_versioning;

import java.util.List;

/**
 * <pre>
 * TEMAT: Wersjonowanie API wbudowane w Spring Framework 7 — configureApiVersioning(ApiVersionConfigurer),
 *        atrybut version w @GetMapping ("2.0" i „baseline” "1.1+"), brakująca i nieobsługiwana wersja,
 *        ustawienia spring.mvc.apiversion.* w Boot 4, wersja po stronie klienta (RestClient)
 *        (API versioning = wersjonowanie API; backward compatibility = zgodność wstecz; breaking change = zmiana
 *        łamiąca; baseline = wersja bazowa; deprecation = wycofywanie, oznaczenie jako przestarzałe; sunset = zachód,
 *        data wyłączenia)
 *
 * W SKRÓCIE:
 *   Gdy API ma klientów (aplikacja mobilna, sklep partnera), nie możesz z dnia na dzień zmienić kształtu odpowiedzi —
 *   stare wersje aplikacji przestałyby działać. Zamiast tego wydajesz NOWĄ WERSJĘ API, a starą utrzymujesz przez jakiś
 *   czas. Spring Framework 7 ma to wbudowane: konfigurujesz, SKĄD brać wersję (nagłówek, parametr, segment ścieżki,
 *   parametr typu nośnika), a w mapowaniu piszesz version = "2.0" (dokładnie ta) albo version = "1.1+" (ta i nowsze).
 *   Brak wersji → 400; wersja spoza listy obsługiwanych → 400. Wcześniej (Spring 6 / Boot 3) robiło się to ręcznie:
 *   osobne adresy /api/v1/..., /api/v2/... albo warunki headers = "API-Version=2" w mapowaniu.
 *
 * ANALOGIA: rozkład jazdy z wersjami.
 *   Przewoźnik zmienia rozkład, ale pasażerowie mają wydrukowane stare rozkłady. Na przystanku wiszą więc dwa: „wersja
 *   2025” i „wersja 2026”, a pasażer mówi kierowcy, z którego korzysta (nagłówek API-Version). Stary rozkład ma naklejkę
 *   „ważny do 1 marca 2027” (nagłówki Deprecation i Sunset). Gdy pasażer pokaże rozkład z 1999 roku — kierowca odmawia
 *   (400 — wersja nieobsługiwana).
 *
 * JAK TO DZIAŁA:
 *   GET /api/products/1    API-Version: 2.0
 *     → ApiVersionStrategy (strategia wersji, zbudowana z ApiVersioningConfig):
 *         HeaderApiVersionResolver czyta "2.0" → SemanticApiVersionParser zamienia na wersję 2.0.0 ("2" = "2.0" = "2.0.0")
 *         brak nagłówka → MissingApiVersionException → 400; wersja spoza obsługiwanych → InvalidApiVersionException → 400
 *     → RequestMappingHandlerMapping: wśród metod dla adresu /api/products/{id} szuka pasującego warunku wersji
 *       (VersionRequestCondition): "1.0+" pasuje do 1.0 i 1.1; "2.0" — tylko do 2.0 → wybiera oneV2
 *     → ApiVersionDeprecationHandler dokłada nagłówki Deprecation/Sunset/Link, jeśli wersja jest wycofywana
 *
 * SŁÓWKA:
 *   version = wersja; supported = obsługiwana; required = wymagana; default version = wersja domyślna;
 *   resolver = „odczytywacz” (skąd wziąć wersję); parser = parser (zamiana tekstu na wersję); semantic version =
 *   wersja semantyczna major.minor.patch (główna.poboczna.poprawka); baseline = wersja bazowa ("1.1+");
 *   inserter = „wstawiacz” (po stronie klienta: gdzie wpisać wersję); deprecation = wycofywanie; sunset = data wyłączenia.
 *
 * ZOBACZ TEŻ: t34_toward_spring/Spring03RestConcepts (kontrakt API), s04_web_rest/Rest01Controllers (jak
 *             DispatcherServlet wybiera metodę), s04_web_rest/Rest04Json (zmiana kształtu JSON-a),
 *             s13_rest_client (RestClient i wersja po stronie klienta), s14_openapi (dokumentowanie wersji API).
 * </pre>
 */
public final class Rest07ApiVersioning {

    private Rest07ApiVersioning() {
    }

    // =================================================================================================
    // 1. PO CO WERSJE — zgodność wstecz (Rest07ApiVersioningTest, sekcja 1)
    // =================================================================================================
    //
    // Zmiana ZGODNA wstecz (bezpieczna): nowe pole w odpowiedzi, nowy nieobowiązkowy parametr, nowy punkt końcowy.
    //   Stary klient nowe pole po prostu ignoruje (Jackson 3 domyślnie ignoruje nieznane pola — Rest04Json).
    // Zmiana ŁAMIĄCA: usunięcie lub zmiana nazwy pola, zmiana typu ("priceGrosze":2999 → "price":{"amount":29.99,...}),
    //   nowe pole wymagane w zapytaniu. Stary klient się wysypie → potrzebna nowa wersja.
    // W tej lekcji: 1.0 → 1.1 dodaje obiekt strony do listy (zmiana listy), 2.0 zmienia kształt ceny (zmiana łamiąca).
    //
    // DOBRA PRAKTYKA: wersjonuj tylko przy zmianach łamiących, a zgodne wprowadzaj bez nowej wersji. Dlaczego? Każda
    //   wersja to kod do utrzymania i testowania; im mniej wersji jednocześnie, tym mniej błędów.

    // =================================================================================================
    // 2. WŁĄCZENIE — WebMvcConfigurer#configureApiVersioning (sekcja 2)
    // =================================================================================================
    //
    //   @Override public void configureApiVersioning(ApiVersionConfigurer configurer) {
    //       configurer.useRequestHeader("API-Version")              ← skąd wersja: nagłówek
    //                 .addSupportedVersions("1.0", "1.1", "2.0")    ← obsługiwane (inne → 400)
    //                 .setDeprecationHandler(deprecation);          ← nagłówki wycofania dla 1.0
    //   }
    // Inne źródła wersji (metody ApiVersionConfigurer, sprawdzone w spring-webmvc 7.0.9):
    //   useQueryParam("v")           → /api/products?v=2.0
    //   usePathSegment(1)            → /api/v2.0/products (segment o indeksie 1 — liczone od zera)
    //   useMediaTypeParameter(MediaType.APPLICATION_JSON, "version") → Accept: application/json;version=2.0
    // Dodatkowo: setDefaultVersion("1.0") (wersja, gdy klient nie poda), setVersionRequired(false),
    // detectSupportedVersions(true) — domyślnie wersje z atrybutów version w mapowaniach też są „obsługiwane”.
    //
    // W Boot 4 to samo bez kodu, właściwościami (sprawdzone w metadanych spring-boot-webmvc 4.1.1 i testem
    // Rest07ApiVersioningPropertiesTest):
    //   spring.mvc.apiversion.use.header=API-Version      (albo .use.query-parameter, .use.path-segment,
    //   spring.mvc.apiversion.supported=1.0,1.1,2.0         .use.media-type-parameter)
    //   spring.mvc.apiversion.default=1.0
    //   spring.mvc.apiversion.required / spring.mvc.apiversion.detect-supported
    //
    // PUŁAPKA: gdy wersjonowanie jest włączone i nie ma wersji domyślnej, wersja jest WYMAGANA dla KAŻDEGO zapytania —
    //   także do punktów końcowych bez atrybutu version (GET /api/health bez nagłówka → 400 „API version is
    //   required.”). Dlaczego? Strategia wersji działa dla całej aplikacji, zanim Spring wybierze metodę.
    //   Sprawdzone testem. Rozwiązanie: setDefaultVersion(...) albo osobna aplikacja/ścieżka dla takich adresów.

    // =================================================================================================
    // 3. version = "2.0" i version = "1.1+" — stała wersja i baseline (sekcja 3)
    // =================================================================================================
    //
    //   @GetMapping(path = "/{id}", version = "1.0+")   oneV1   ← 1.0 i nowsze...
    //   @GetMapping(path = "/{id}", version = "2.0")    oneV2   ← ...aż do 2.0, która ma własną metodę
    //   @GetMapping(version = "1.0")                    allV1   ← tylko 1.0
    //   @GetMapping(version = "1.1+")                   allSince11 ← 1.1 i nowsze, także 2.0
    //
    //   zapytanie                         wynik (sprawdzone testem)
    //   /api/products/1   API-Version: 1.0  → {"id":1,"name":"Kawa","priceGrosze":2999} + nagłówki Deprecation, Sunset
    //   /api/products/1   API-Version: 1.1  → to samo (baseline 1.0+), bez nagłówków wycofania
    //   /api/products/1   API-Version: 2    → {"id":1,"name":"Kawa","price":{"amount":29.99,"currency":"PLN"}} ("2" = "2.0")
    //   /api/products     API-Version: 2.0  → {"items":[...],"total":2} — baseline 1.1+ obejmuje 2.0
    //
    // Reguła wyboru (sprawdzona testami): dla żądanej wersji R Spring bierze metodę o NAJWYŻSZEJ zadeklarowanej wersji
    // nie większej niż R. Jeśli to wersja stała — pasuje tylko, gdy jest równa R; jeśli baseline („+”) — pasuje.
    // Dlatego baseline „1.1+” przy metodzie „2.0” dla tego samego adresu NIE obejmie wersji 3.0.
    //
    // DOBRA PRAKTYKA: dla metod, które się nie zmieniają, używaj baseline ("1.0+"). Dlaczego? Wydając wersję 1.1 nie
    //   musisz kopiować ani dopisywać nic do metod, których ta wersja nie dotyczy — baseline obejmie ją automatycznie.

    // =================================================================================================
    // 4. BRAK WERSJI, ZŁA WERSJA, WERSJA BEZ METODY (sekcja 4)
    // =================================================================================================
    //
    //   bez nagłówka API-Version                → 400 MissingApiVersionException („API version is required.”)
    //   API-Version: 9                          → 400 InvalidApiVersionException (spoza obsługiwanych)
    //   API-Version: abc                        → 400 InvalidApiVersionException (nie da się odczytać)
    //   /api/products/1/price, API-Version: 1.1 → 400 NotAcceptableApiVersionException — wersja obsługiwana,
    //                                             ale dla tego adresu są tylko metody „1.0” i „2.0” (stałe)
    //
    // PUŁAPKA: stała wersja ("1.0") NIE przechodzi na kolejne wersje. Dlaczego to boli? Wydajesz 1.1, która zmienia
    //   jeden punkt końcowy, a nagle wszystkie inne adresy z version = "1.0" odpowiadają 400 dla klientów 1.1.
    //   Sprawdzone testem (/price). Stosuj baseline ("1.0+") wszędzie tam, gdzie nowa wersja niczego nie zmienia.

    // =================================================================================================
    // 5. WYCOFYWANIE i KLIENT — Deprecation/Sunset oraz RestClient.apiVersion (sekcja 5, Rest07ApiVersioningServerTest)
    // =================================================================================================
    //
    // StandardApiVersionDeprecationHandler: configureVersion("1.0").setDeprecationDate(...).setSunsetDate(...)
    // .setDeprecationLink(...) → odpowiedzi w wersji 1.0 dostają nagłówki Deprecation (od kiedy przestarzała),
    // Sunset (kiedy zniknie) i Link (gdzie dokumentacja). Klient widzi ostrzeżenie, zanim wersja zniknie.
    // Po stronie klienta (Spring Framework 7) RestClient też zna wersje:
    //   RestClient.builder().baseUrl(...).apiVersionInserter(ApiVersionInserter.useHeader("API-Version")).build()
    //   client.get().uri("/api/products/1").apiVersion("2.0")...   ← RestClient sam dopisze nagłówek
    //
    // DOBRA PRAKTYKA: ogłaszaj wycofanie wersji nagłówkami Deprecation/Sunset i w dokumentacji (s14_openapi) z dużym
    //   wyprzedzeniem. Dlaczego? Klienci (np. aplikacje mobilne) aktualizują się tygodniami; nagłówki pozwalają im
    //   wykryć problem automatycznie, zanim stara wersja zostanie wyłączona.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Spring Framework 7: WebMvcConfigurer#configureApiVersioning(ApiVersionConfigurer) — useRequestHeader,
     *     useQueryParam, usePathSegment, useMediaTypeParameter; addSupportedVersions; setDefaultVersion; setVersionRequired.
     *   • Boot 4: spring.mvc.apiversion.use.header / .use.query-parameter / .use.path-segment / .use.media-type-parameter,
     *     .supported, .default, .required, .detect-supported.
     *   • @GetMapping(path = "...", version = "2.0") — dokładnie ta; version = "1.1+" — ta i nowsze (do następnej
     *     zadeklarowanej dla adresu). "2" = "2.0" = "2.0.0".
     *   • Brak wersji → 400 (gdy brak wersji domyślnej) — dla WSZYSTKICH adresów; wersja nieobsługiwana → 400;
     *     obsługiwana, ale bez metody dla adresu → 400.
     *   • Wycofywanie: StandardApiVersionDeprecationHandler → nagłówki Deprecation, Sunset, Link.
     *   • Klient: RestClient.builder().apiVersionInserter(ApiVersionInserter.useHeader("API-Version")) + .apiVersion("2.0").
     *   • Nowa wersja tylko dla zmian łamiących; zmiany zgodne wstecz — bez nowej wersji.
     *
     * PYTANIA KONTROLNE:
     *   1. Co to jest zmiana łamiąca? Podaj dwa przykłady i jeden przykład zmiany zgodnej wstecz.
     *   2. Czym różni się version = "1.1" od version = "1.1+"?
     *   3. Co się stanie? GET /api/products z nagłówkiem API-Version: 2.0 — która metoda odpowie i dlaczego?
     *   4. Co się stanie? GET /api/health bez nagłówka API-Version (metoda nie ma atrybutu version).
     *   5. ZNAJDŹ BŁĄD: po wydaniu wersji 1.1 klienci 1.1 dostają 400 na GET /api/products/1/price, choć adres
     *      działa dla 1.0. Co jest w mapowaniu i jak to naprawić?
     *   6. Jakie trzy sposoby przekazania wersji (poza nagłówkiem) oferuje ApiVersionConfigurer?
     *   7. Jak powiedzieć klientom, że wersja 1.0 zniknie 1 marca 2027?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — ćwiczenie 1 i 3 poniżej, ćwiczenie 2 w CustomerVersionedController. Testy: Rest07ApiVersioningExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zamień tekst wersji na pełną postać major.minor.patch, uzupełniając brakujące części zerami
     * (tak jak SemanticApiVersionParser): "2" → "2.0.0", "1.1" → "1.1.0", "1.2.3" → "1.2.3". Pusty tekst, część
     * nieliczbowa ("abc", "1.x") albo więcej niż trzy części ("1.2.3.4") → IllegalArgumentException.
     * Podpowiedź: split("\\.") i Integer.parseInt w try/catch (NumberFormatException) — t04_strings/Strings05Regex.
     */
    static String exercise1(String version) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zasymuluj wybór metody według wersji (reguła z sekcji 3). declared = wersje metod
     * jednego adresu, np. ["1.0", "1.1+", "2.0"]; requested = żądana wersja, np. "1.5". Zwróć zadeklarowaną wersję
     * (dokładnie ten tekst z listy, np. "1.1+") metody, która obsłuży zapytanie, albo null, gdy żadna. Reguła: weź
     * deklarację o NAJWYŻSZEJ wersji nie większej niż żądana; stała pasuje tylko, gdy jest równa żądanej, baseline
     * („+”) — zawsze. Porównuj wersje LICZBOWO po częściach: jako tekst "1.10" wypada przed "1.9"
     * ({@code "1.10".compareTo("1.9") < 0}), choć to wersja nowsza.
     * Przykłady (zgodne z zachowaniem Springa — sprawdzone testem): (["1.0","1.1+","2.0"], "1.5") → "1.1+";
     * (..., "2.0") → "2.0"; (..., "3.0") → null; (["1.0+","1.5+"], "3.0") → "1.5+".
     * Podpowiedź: użyj exercise1 do ujednolicenia i porównuj tablice int[] {major, minor, patch}.
     */
    static String exercise3(List<String> declared, String requested) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Zmiana, po której stary klient przestaje działać: usunięcie/zmiana nazwy pola, zmiana typu pola (int →
     *      obiekt), nowe wymagane pole w zapytaniu. Zgodna wstecz: nowe pole w odpowiedzi albo nowy punkt końcowy.
     *   2. "1.1" pasuje tylko do wersji 1.1. "1.1+" (baseline) pasuje do 1.1 i wszystkich nowszych — aż do najbliższej
     *      wyższej wersji zadeklarowanej dla tego samego adresu.
     *   3. allSince11 (baseline "1.1+") — {"items":[...],"total":2}. Dla /api/products najwyższa zadeklarowana wersja
     *      nie większa niż 2.0 to 1.1+, a baseline obejmuje nowsze. Sprawdzone testem (sekcja 3).
     *   4. 400 „API version is required.” — wersjonowanie dotyczy całej aplikacji, a wersji domyślnej brak.
     *      Sprawdzone testem (sekcja 2).
     *   5. Metody mają stałe wersje version = "1.0" i "2.0" — stała nie obejmuje 1.1, więc dla 1.1 nie ma metody →
     *      400 (NotAcceptableApiVersionException). Naprawa: version = "1.0+" w metodzie dla wersji 1.x.
     *      Sprawdzone testem (sekcja 4).
     *   6. useQueryParam (parametr zapytania), usePathSegment (segment ścieżki), useMediaTypeParameter (parametr typu
     *      nośnika w Accept/Content-Type). Jest też useVersionResolver — własny sposób.
     *   7. StandardApiVersionDeprecationHandler: configureVersion("1.0").setSunsetDate(1 marca 2027) (+ data wycofania
     *      i link do dokumentacji) i setDeprecationHandler(...) w konfiguracji → nagłówki Sunset, Deprecation, Link.
     *      Sprawdzone testem (sekcja 5) — nagłówek Sunset: Mon, 1 Mar 2027 00:00:00 GMT.
     */
    // </editor-fold>
}
