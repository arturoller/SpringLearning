package s14_openapi.api01_springdoc;

import java.util.List;
import java.util.Set;

/**
 * <pre>
 * TEMAT: OpenAPI i springdoc — opis API jako dane, generowany z kontrolerów
 *        (OpenAPI = otwarty standard opisu interfejsów HTTP; documentation = dokumentacja; generate = generować)
 *
 * W SKRÓCIE:
 *   OpenAPI to plik (JSON albo YAML), który mówi: „to API ma takie adresy, takie metody, takie parametry, przyjmuje takie
 *   ciała i odpowiada takimi kształtami JSON”. Biblioteka springdoc-openapi buduje ten plik SAMA — czyta Twoje kontrolery
 *   Spring MVC i na ich podstawie wystawia dokument pod /v3/api-docs oraz stronę Swagger UI (ui = interfejs użytkownika),
 *   na której klikasz „Try it out” (wypróbuj) i wysyłasz zapytania bez pisania pliku .http. Nie dopisujesz ani jednej
 *   adnotacji — kontroler z lekcji (ProductController) nie ma żadnej adnotacji dokumentującej, a dokument i tak powstaje.
 *
 * ANALOGIA: spis treści i mapa sklepu przy wejściu.
 *   Sklep ma setki produktów na półkach (kontrolery). Mapa przy wejściu (dokument OpenAPI) mówi, co gdzie leży i jak o to
 *   poprosić. Springdoc to pracownik, który obchodzi sklep i rysuje mapę sam — dlatego mapa jest zgodna z półkami. Ale
 *   obchodzi tylko to, co WIDAĆ z korytarza (sygnatury metod): nie zagląda do zaplecza (ciał metod), więc nie wie, co
 *   dokładnie zrobi metoda w środku.
 *
 * JAK TO DZIAŁA:
 *   1. Start aplikacji: autokonfiguracja springdoc rejestruje beany i dwa adresy: /v3/api-docs i /swagger-ui.html.
 *   2. Zapytanie GET /v3/api-docs: springdoc bierze z Spring MVC listę zarejestrowanych mapowań (adres + metoda HTTP +
 *      metoda kontrolera), czyta parametry ({@code @PathVariable}, {@code @RequestParam}, {@code @RequestBody}),
 *      typ zwracany i adnotację {@code @ResponseStatus}.
 *   3. Typy z sygnatur (rekordy Product, NewProduct) zamienia na schematy; budowa modelu to biblioteka swagger-core
 *      (springdoc 3.1.1 używa swagger-core 2.2.x, razem z własnym Jacksonem 2 — obok Jacksona 3 z Boota 4; to wewnętrzna
 *      sprawa biblioteki, Twoje DTO nadal są serializowane Jacksonem 3).
 *   4. Wynik to JSON w standardzie OpenAPI 3.1.0. Swagger UI (webjar swagger-ui) pobiera ten JSON i rysuje z niego stronę.
 *
 *   Skrót dokumentu z tej lekcji (GET /v3/api-docs):
 *     {"openapi":"3.1.0","info":{"title":"OpenAPI definition","version":"v0"},
 *      "paths":{"/api/products":{"get":{...},"post":{...}},"/api/products/{sku}":{"get":{...},"delete":{...}}},
 *      "components":{"schemas":{"Product":{...},"NewProduct":{...}}}}
 *
 * SŁÓWKA:
 *   OpenAPI = standard opisu API (dawniej Swagger Specification); springdoc = biblioteka generująca dokument ze Springa;
 *   Swagger UI = strona do przeglądania i wypróbowywania API; paths = ścieżki; operation = operacja (metoda HTTP na ścieżce);
 *   operationId = identyfikator operacji (tu: nazwa metody kontrolera); schema = schemat (opis kształtu obiektu JSON);
 *   components = komponenty (współdzielone schematy); tag = znacznik (grupa operacji w UI); property = właściwość/pole;
 *   enabled = włączony; profile = profil (zestaw ustawień na środowisko).
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest01Controllers (adnotacje mapowań), s04_web_rest/Rest03Responses (statusy odpowiedzi),
 *             s04_web_rest/Rest05CrudApi (CRUD), s02_configuration/Cfg03Profiles (profile), s00_start/Start05HttpFiles
 *             (pliki .http — ręczne zapytania), s14_openapi/Api02DocumentingApi (opisy i przykłady w dokumencie).
 * </pre>
 */
public final class Api01Springdoc {

    private Api01Springdoc() {
    }

    // =================================================================================================
    // 1. DOKUMENT Z KONTROLERA BEZ ADNOTACJI (test: documentExistsWithoutAnnotations)
    // =================================================================================================
    //
    // Wystarczy zależność springdoc-openapi-starter-webmvc-ui w pom.xml (wersja spoza BOM-a Boota — w tym kursie
    // ${springdoc.version} z parent pom). Bez żadnej konfiguracji:
    //   GET /v3/api-docs        → dokument JSON (openapi: "3.1.0")
    //   GET /v3/api-docs.yaml   → ten sam dokument jako YAML
    //   GET /swagger-ui.html    → przekierowanie (302) na /swagger-ui/index.html — stronę Swagger UI
    // Informacje ogólne są domyślne: info.title = "OpenAPI definition", info.version = "v0" (zmienisz je w Api03).
    //
    // PRZED (ręczny opis w pliku, który trzeba pisać i pilnować):  openapi.yaml na 500 linii obok kodu.
    // PO (springdoc):  zero linii — dokument powstaje z kodu, więc nie może „zapomnieć” o nowej metodzie.
    //
    // DOBRA PRAKTYKA: dodanie zależności i zerknięcie na /swagger-ui.html to dobry pierwszy krok w każdym API. Dlaczego?
    //   Natychmiast widzisz, co aplikacja NAPRAWDĘ wystawia — często okazuje się, że jest tam coś, czego nie planowałeś
    //   (np. metoda debugowa), albo że brakuje czegoś, co miało być.

    // =================================================================================================
    // 2. ŚCIEŻKI, OPERACJE, PARAMETRY (testy: pathsAndOperationsComeFromController, parametersAreReadFromMethodSignature)
    // =================================================================================================
    //
    // Każde mapowanie kontrolera to w dokumencie para „ścieżka + metoda HTTP”:
    //   @GetMapping("/{sku}") w @RequestMapping("/api/products")  →  paths["/api/products/{sku}"].get
    // Nazwa metody Javy staje się operationId (all, one, create, delete) — dlatego nazwy metod kontrolera są częścią
    // „publicznego” opisu API (generatory klientów robią z nich nazwy funkcji).
    // Znacznik (tag) to domyślnie nazwa klasy kontrolera w stylu kebab-case: "product-controller" — Swagger UI grupuje
    // operacje po znacznikach.
    // Parametry: {@code @PathVariable} → in: "path", required: true; {@code @RequestParam(required = false)} → in: "query",
    // required: false; {@code @RequestBody} → requestBody (required: true).
    //
    // PUŁAPKA: w dokumencie jest dokładnie to, co mówi SYGNATURA, a nie to, co dzieje się w ciele metody. Dlaczego? Springdoc
    //   nie wykonuje i nie analizuje Twojego kodu — czyta typy i adnotacje. Metoda zwracająca Object albo Map zamiast
    //   rekordu da w dokumencie schemat „dowolny obiekt” — bezużyteczny dla odbiorcy API.

    // =================================================================================================
    // 3. SCHEMATY Z REKORDÓW (test: recordsBecomeSchemas)
    // =================================================================================================
    //
    // Rekord Product(String sku, String name, String category, BigDecimal price, int stock) zamienia się na schemat:
    //   "Product": {"type":"object","properties":{"sku":{"type":"string"}, "price":{"type":"number"},
    //               "stock":{"type":"integer","format":"int32"}, ...}}
    // BigDecimal → number, int → integer/int32, String → string. Schematy trafiają do components.schemas, a operacje
    // wskazują na nie przez {"$ref":"#/components/schemas/Product"} (reference = odwołanie) — jeden schemat, wiele użyć.
    // Bez adnotacji walidacji (Bean Validation) żadne pole nie jest „required” (wymagane) — pole required pojawi się
    // dopiero z {@code @NotNull} itd. (s14_openapi/Api02DocumentingApi).
    //
    // DOBRA PRAKTYKA: zwracaj z kontrolerów KONKRETNE rekordy DTO (Data Transfer Object = obiekt do przesyłania danych), a
    //   nie encje i nie Map. Dlaczego? Z rekordu springdoc zbuduje czytelny schemat, a Ty nie wystawisz przypadkiem pól,
    //   których klient nie powinien widzieć (hasła, wewnętrzne identyfikatory).

    // =================================================================================================
    // 4. PUŁAPKA: DOKUMENT NIE WIDZI STATUSU WYBRANEGO W CIELE METODY (test: documentCannotSeeStatusChosenInsideMethodBody)
    // =================================================================================================
    //
    // ProductController.create zwraca {@code ResponseEntity.created(...)} → naprawdę przychodzi 201 Created. Ale dokument
    // mówi: responses → "200" (a "201" nie ma). Podobnie GET /{sku} może zwrócić 404 — w dokumencie jest tylko 200.
    // Wyjątek: {@code @ResponseStatus(HttpStatus.NO_CONTENT)} na metodzie jest widoczne bez wykonania kodu, więc DELETE
    // ma w dokumencie 204.
    //
    // PUŁAPKA: przyjmujesz dokument za prawdę o statusach. Dlaczego to błąd? Status wybrany w ciele metody ({@code
    //   ResponseEntity.created}, {@code notFound}, {@code ResponseStatusException}) jest dla springdoc niewidoczny.
    //   Odbiorca przeczyta „200” i napisze klienta, który nie obsłuży 201 ani 404. Rozwiązanie: opisać odpowiedzi
    //   jawnie adnotacją {@code @ApiResponse} (lekcja Api02DocumentingApi).

    // =================================================================================================
    // 5. SWAGGER UI I FORMATY (test: swaggerUiAndYaml)
    // =================================================================================================
    //
    // Swagger UI (strona) nie ma własnej wiedzy o Twoim API — pobiera dokument adresem z /v3/api-docs/swagger-config
    // (pole "url") i rysuje z niego listę operacji. Przycisk „Try it out” wysyła prawdziwe zapytania do aplikacji,
    // dlatego Swagger UI działa jak „plik .http w przeglądarce”. Dokument jest też dostępny jako YAML
    // (Content-Type: application/vnd.oai.openapi).
    //
    // DOBRA PRAKTYKA: w repozytorium trzymaj tylko kod; dokument generuj z działającej aplikacji. Dlaczego? Plik opisu
    //   przepisany ręcznie rozjedzie się z kodem już po pierwszej poprawce (zob. test w Api02DocumentingApi, który
    //   pilnuje najważniejszych elementów dokumentu).

    // =================================================================================================
    // 6. WŁAŚCIWOŚCI springdoc.* (testy: klasy zagnieżdżone CustomPaths, UiDisabled, ProductionProfile, PathFilter)
    // =================================================================================================
    //
    //   springdoc.api-docs.path=/docs/openapi        zmienia adres dokumentu (.yaml i swagger-config idą za nim)
    //   springdoc.swagger-ui.path=/docs/ui.html      zmienia adres strony UI; zasoby UI przenoszą się wtedy pod
    //                                                /docs/swagger-ui/..., a stary /swagger-ui.html daje 404
    //   springdoc.swagger-ui.enabled=false           wyłącza TYLKO stronę UI; dokument nadal działa
    //   springdoc.api-docs.enabled=false             wyłącza dokument, a RAZEM Z NIM Swagger UI (UI bez dokumentu nie ma
    //                                                czego pokazać) — oba adresy dają 404
    //   springdoc.paths-to-match=/api/products/{sku} w dokumencie zostają tylko pasujące ścieżki
    //   springdoc.default-produces-media-type=application/json   typ odpowiedzi w dokumencie zamiast domyślnego */*
    // (wszystkie sprawdzone testami na springdoc 3.1.1 + Spring Boot 4.1.1).
    //
    // Profil lekcji: plik application-api01prod.properties (w resources) zawiera springdoc.api-docs.enabled=false.
    // Włączasz go jak każdy profil: --spring.profiles.active=api01prod (w teście: {@code @ActiveProfiles}).
    //
    // PUŁAPKA: zostawiasz Swagger UI i /v3/api-docs włączone na produkcji, w internecie. Dlaczego to ryzyko? Dokument to
    //   kompletna mapa Twojego API — każdy adres, parametr i kształt danych. Nie zastępuje to zabezpieczeń (atakujący i
    //   tak może zgadywać adresy), ale oddaje mu pracę rozpoznawczą za darmo. Na produkcji udostępniaj dokument tylko tym,
    //   którzy go potrzebują (zabezpieczony adres, sieć wewnętrzna) albo wyłącz go profilem.
    //
    // DOBRA PRAKTYKA: ustawienie wyłączające trzymaj w pliku profilu produkcyjnego (application-prod.properties), a nie
    //   w kodzie. Dlaczego? Zmieniasz zachowanie środowiska przełącznikiem, bez przebudowy, a w dev i testach dokument
    //   nadal jest.

    // =================================================================================================
    // 7. ZGODNOŚĆ Z BOOTEM 4 (informacja)
    // =================================================================================================
    //
    // W tym kursie działa springdoc-openapi-starter-webmvc-ui 3.1.1 z Spring Bootem 4.1.1 — dokument, Swagger UI (webjar
    // swagger-ui 5.x) i właściwości springdoc.* zostały sprawdzone uruchomieniem. Linia 3.x springdoc jest przeznaczona dla
    // Boota 4; linia 2.x — dla Boota 3. W starych tutorialach zobaczysz artefakt springdoc-openapi-starter-webmvc-ui w
    // wersji 2.x — z Bootem 4 go nie mieszaj.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Zależność: springdoc-openapi-starter-webmvc-ui (wersja spoza BOM-a Boota). Zero adnotacji → działa.
     *   • GET /v3/api-docs (JSON, OpenAPI 3.1.0), /v3/api-docs.yaml, /swagger-ui.html (→ /swagger-ui/index.html).
     *   • Ścieżka + metoda HTTP → operacja; nazwa metody → operationId; klasa kontrolera → znacznik (tag).
     *   • @PathVariable → in: path, required; @RequestParam(required=false) → in: query; @RequestBody → requestBody.
     *   • Rekordy → components.schemas; BigDecimal → number, int → integer/int32; bez walidacji brak "required".
     *   • Dokument NIE widzi statusu wybranego w ciele metody (ResponseEntity.created → w dokumencie 200!). Widzi
     *     @ResponseStatus.
     *   • springdoc.api-docs.path / swagger-ui.path / swagger-ui.enabled / api-docs.enabled / paths-to-match.
     *   • api-docs.enabled=false wyłącza też Swagger UI; swagger-ui.enabled=false wyłącza tylko UI.
     *   • Na produkcji: wyłącz lub zabezpiecz dokument.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym jest dokument OpenAPI i co robi springdoc? Skąd bierze informacje o API?
     *   2. Czy aby dostać dokument, trzeba oznaczyć kontroler adnotacjami springdoc? Co zobaczysz w nim bez nich?
     *   3. Co się stanie? Ustawisz springdoc.api-docs.enabled=false i wejdziesz na /swagger-ui.html — jaki status?
     *   4. Co się stanie? Ustawisz springdoc.swagger-ui.enabled=false i wejdziesz na /v3/api-docs — jaki status?
     *   5. ZNAJDŹ BŁĄD: „W dokumencie POST /api/products ma odpowiedź 200, więc kontroler zwraca 200 OK.” Co jest nie tak?
     *   6. Co się stanie? Ustawisz springdoc.api-docs.path=/docs/openapi i springdoc.swagger-ui.path=/docs/ui.html — co da
     *      wejście na dawny adres /swagger-ui.html?
     *   7. ZNAJDŹ BŁĄD: „Dokument API na produkcji jest nieszkodliwy, bo to tylko opis.” Dlaczego to złe założenie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Api01SpringdocExercisesTest
    // =================================================================================================
    //
    // Ćwiczenia pracują na tekście dokumentu OpenAPI (JSON), tak jak zrobiłby to test pilnujący dokumentu. Czytaj go
    // Jacksonem 3: {@code JsonMapper} (pakiet tools.jackson.databind.json), {@code readTree(String)} zwraca drzewo
    // {@code JsonNode}; {@code node.get("pole")} zwraca podwęzeł (albo null), {@code node.propertyNames()} — nazwy pól
    // obiektu (w Jacksonie 2 nazywało się fieldNames — w 3 zmieniono na propertyNames).

    /**
     * ĆWICZENIE 1 (łatwe): zwróć posortowaną alfabetycznie listę ścieżek z sekcji "paths" dokumentu.
     * Przykład: dokument ze ścieżkami "/b" i "/a" → ["/a", "/b"].
     * Podpowiedź: {@code JsonMapper.builder().build().readTree(json).get("paths").propertyNames()}; sortuj przed zwróceniem.
     */
    static List<String> exercise1(String openApiJson) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć posortowaną listę operacji w postaci "METODA ścieżka", metoda WIELKIMI literami,
     * np. "GET /api/products", "POST /api/products". Kluczami obiektu ścieżki są metody HTTP małymi literami
     * (get, post, delete...), ale mogą tam być też inne pola (np. "parameters", "summary") — pomiń je: weź tylko klucze
     * get, put, post, delete, patch.
     * Podpowiedź: Set.of("get", "put", "post", "delete", "patch") i toUpperCase() (t04_strings/Strings01Basics).
     */
    static List<String> exercise2(String openApiJson) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): sprawdź, czego dokumentacji brakuje. Dla operacji o podanej metodzie (małymi literami) i
     * ścieżce zwróć posortowaną listę kodów statusu z {@code expected}, których NIE MA w jej sekcji "responses".
     * Gdy ścieżki albo metody nie ma w dokumencie — rzuć IllegalArgumentException z komunikatem zawierającym ścieżkę.
     * Przykład: dokument opisuje POST /api/products tylko kodem "200", expected = {"201", "400", "200"} → ["201", "400"].
     * Podpowiedź: get(...) może zwrócić null — sprawdź to przed dalszym czytaniem.
     */
    static List<String> exercise3(String openApiJson, String path, String method, Set<String> expected) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. OpenAPI to standard opisu API w JSON/YAML (adresy, metody, parametry, kształty danych). Springdoc buduje go
     *      sam, czytając zarejestrowane mapowania Spring MVC: adnotacje kontrolerów i typy w sygnaturach metod.
     *   2. Nie. Bez adnotacji springdoc pokazuje ścieżki, metody, parametry, schematy rekordów i statusy widoczne z
     *      sygnatury, z domyślnym tytułem "OpenAPI definition" i wersją "v0" oraz znacznikiem z nazwy klasy.
     *   3. 404 — api-docs.enabled=false wyłącza dokument, a razem z nim Swagger UI (sprawdzone testem ProductionProfile).
     *   4. 200 — wyłączona jest tylko strona UI, dokument działa dalej (sprawdzone testem UiDisabled; /swagger-ui.html
     *      daje wtedy 404).
     *   5. Dokument nie widzi statusu wybranego w ciele metody. Kontroler zwraca ResponseEntity.created(...), więc
     *      naprawdę jest 201 z nagłówkiem Location, a springdoc zakłada "200". Sprawdzone testem
     *      documentCannotSeeStatusChosenInsideMethodBody; naprawa: @ApiResponse (Api02DocumentingApi).
     *   6. 404 — stary adres przestaje istnieć (sprawdzone testem CustomPaths). UI jest pod /docs/ui.html, który
     *      przekierowuje na /docs/swagger-ui/index.html.
     *   7. Dokument to kompletna mapa API: każdy adres, parametr i kształt danych. Nie jest tajemnicą bezpieczeństwa, ale
     *      ułatwia rozpoznanie i atak, więc na produkcji wyłącz go lub zabezpiecz.
     */
    // </editor-fold>
}
