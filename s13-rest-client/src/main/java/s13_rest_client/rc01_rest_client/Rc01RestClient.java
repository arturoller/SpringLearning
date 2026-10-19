package s13_rest_client.rc01_rest_client;

import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;

/**
 * <pre>
 * TEMAT: RestClient — klient HTTP Springa: nasza aplikacja pyta CUDZE API
 *        (client = klient; request = zapytanie; response = odpowiedź; base URL = adres bazowy; header = nagłówek)
 *
 * W SKRÓCIE:
 *   Do tej pory to NASZA aplikacja odpowiadała na zapytania (kontrolery, s04_web_rest). Teraz role się odwracają:
 *   sklep sam wysyła zapytania do zewnętrznego API dostawcy — „ile macie laptopów?”, „zamawiam 3 książki”.
 *   W Springu robi to RestClient (od Spring Framework 6.1): płynne (fluent) API — get().uri(...).retrieve().body(...)
 *   — które samo zamienia JSON na rekordy Javy i z powrotem (Jackson 3). Boot daje gotowy, skonfigurowany
 *   RestClient.Builder. Klienta testujemy BEZ internetu: MockRestServiceServer udaje dostawcę w pamięci,
 *   a test na losowym porcie sprawdza drogę przez prawdziwy serwer HTTP.
 *
 * ANALOGIA: zamawianie towaru w hurtowni przez telefon.
 *   Numer hurtowni zapisujesz raz w telefonie (adres bazowy), a przedstawiasz się zawsze tak samo: „sklep nr 01”
 *   (nagłówek domyślny X-Shop-Id). Potem tylko mówisz, czego chcesz: „poproszę cenę laptopa” (GET) albo „zamawiam
 *   3 sztuki” (POST). SupplierClient to pracownik, który zna numer i formułki — reszta sklepu prosi go zwykłym zdaniem
 *   (metodą Javy), a nie dzwoni sama. MockRestServiceServer to aktor udający hurtownię na szkoleniu: odpowiada
 *   z kartki i notuje, czy zadzwoniłeś z właściwym pytaniem.
 *
 * JAK TO DZIAŁA:
 *   RestClient.Builder (od Boota, nowy przy każdym wstrzyknięciu)
 *       .baseUrl("http://localhost:8080")          ← adres dostawcy z application-rc01.properties
 *       .defaultHeader("X-Shop-Id", "sklep-01")    ← dokładany do KAŻDEGO zapytania
 *       .build()                                   → RestClient: tworzysz raz, używasz wiele razy
 *
 *   restClient.get()                               ← metoda HTTP
 *       .uri("/supplier/api/offers/{sku}", "ELE-001")  ← adres: baseUrl + ścieżka, {sku} → ELE-001
 *       .retrieve()                                ← „pobierz” — opis, jeszcze bez wysyłania
 *       .body(SupplierOffer.class)                 ← TERAZ: wysłanie → 200 OK → JSON → rekord (Jackson)
 *
 *   GET /supplier/api/offers/ELE-001  →  {"sku":"ELE-001","name":"Laptop Pro 14","price":5499.99,"available":7}
 *                                     →  SupplierOffer[sku=ELE-001, name=Laptop Pro 14, price=5499.99, available=7]
 *
 * SŁÓWKA:
 *   RestClient = klient REST (synchroniczny: wątek czeka na odpowiedź); builder = budowniczy (obiekt do konfiguracji);
 *   retrieve = pobierz; body = ciało (treść odpowiedzi/zapytania); entity = encja (tu: cała odpowiedź HTTP);
 *   ResponseEntity = encja odpowiedzi (status + nagłówki + ciało); URI template = szablon adresu ({sku});
 *   query parameter = parametr zapytania (?category=...); ParameterizedTypeReference = odwołanie do typu z parametrem
 *   (np. {@code List<SupplierOffer>}); mock = atrapa; expectation = oczekiwanie;
 *   verify = sprawdź; slice = wycinek (test ładujący tylko część aplikacji); supplier = dostawca; offer = oferta.
 *
 * ZOBACZ TEŻ: t28_networking_http/Http02HttpClient (klient HTTP z JDK, na którym RestClient tu działa),
 *             t28_networking_http/Http04JsonApi (JSON przez HTTP bez Springa), t11_generics/Generics06ErasureLimits
 *             (wymazywanie typów), s00_start/Start05HttpFiles (RestClient w teście na losowym porcie),
 *             s04_web_rest/Rest04Json (JSON po stronie serwera), s13_rest_client/Rc02ErrorsAndTimeouts (błędy),
 *             s13_rest_client/Rc03HttpInterfaces (klient jako interfejs).
 * </pre>
 */
public final class Rc01RestClient {

    private Rc01RestClient() {
    }

    // =================================================================================================
    // 1. BUDOWANIE KLIENTA — RestClient.Builder od Boota (SupplierClient, konstruktor)
    // =================================================================================================
    //
    // Najkrócej: RestClient.create("http://localhost:8080"). Działa, ale to klient „goły” — bez ustawień Boota.
    // W aplikacji Boota wstrzykuj RestClient.Builder: Boot przygotował go z:
    //   • konwerterami wiadomości (message converters) — m.in. JSON przez Jacksona 3 z ustawieniami Boota,
    //   • fabryką zapytań (request factory) dla wykrytej biblioteki HTTP — tu klient z JDK (java.net.http.HttpClient),
    //     bo w module nie ma Apache HttpClient, Jetty ani Reactor Netty (Boot wybiera w tej kolejności),
    //   • ustawieniami z właściwości spring.http.clients.* (np. limity czasu — lekcja Rc02),
    //   • dodatkami (customizers — dostosowywacze), np. pomiarami do metryk albo — w testach — MockRestServiceServer.
    // Builder ma zasięg prototype (prototyp): KAŻDE wstrzyknięcie dostaje nowy egzemplarz, więc dwa klienty
    // (dostawca, kurier) nie nadpisują sobie adresów.
    //
    // baseUrl — wspólny początek adresów; defaultHeader — nagłówek dokładany do każdego zapytania (tu X-Shop-Id,
    // bez którego dostawca odpowiada 400 — sprawdza to Rc01RestClientServerTest).
    //
    // DOBRA PRAKTYKA: zbuduj RestClient RAZ (w konstruktorze beana) i trzymaj w polu final. Dlaczego? Zbudowany klient
    //   jest przeznaczony do wielokrotnego użycia, także z wielu wątków; budowanie przy każdym wywołaniu to zbędna
    //   praca, a konfiguracja rozsiana po metodach łatwo się rozjeżdża.
    // DOBRA PRAKTYKA: adres dostawcy w application.properties, nie w kodzie. Dlaczego? Na testach, na produkcji
    //   i u kolegi adres jest inny — zmieniasz plik ustawień albo zmienną środowiska, a nie kod.
    // PUŁAPKA: RestClient.create(...) w aplikacji Boota. Dlaczego to błąd? Taki klient nie widzi ustawień Boota
    //   (limitów czasu z właściwości, metryk), a w teście @RestClientTest nie podepnie się pod niego
    //   MockRestServiceServer — test spróbuje łączyć się naprawdę.

    // =================================================================================================
    // 2. GET — retrieve() i body(...): JSON → rekord (SupplierClient.offer)
    // =================================================================================================
    //
    // get() → uri(...) → retrieve() → body(Typ.class). retrieve() tylko OPISUJE, jak odczytać odpowiedź — zapytanie
    // wychodzi dopiero przy body(...)/requiredBody(...)/toEntity(...)/toBodilessEntity() (sprawdza to test).
    // Zmienne szablonu: uri("/offers/{sku}", sku) — RestClient wstawia wartość i koduje znaki specjalne.
    // body(...) zwraca null, gdy odpowiedź nie ma ciała; requiredBody(...) (nowość Spring Framework 7.0.4) rzuca wtedy
    // IllegalStateException — wygodne, gdy ciało MUSI być.
    //
    // JSON → rekord robi Jackson 3 (pakiet tools.jackson; Jackson 2 miał pakiet com.fasterxml.jackson — spotkasz go
    // w starszych tutorialach). Nazwy pól JSON = nazwy składowych rekordu.
    //   • Dostawca dodał NOWE pole, którego rekord nie ma → zostaje pominięte, klient działa (sprawdzone testem).
    //   • Dostawca NIE przysłał pola dla składowej typu prostego (int available) → wyjątek RestClientException:
    //     „Error while extracting response for type [...SupplierOffer] and content type [application/json]”,
    //     a w przyczynie: „Cannot map `null` into type `int`”.
    //
    // PUŁAPKA: typ prosty (int, boolean) dla pola, którego dostawca może nie wysłać. Dlaczego to błąd? Brak pola = null,
    //   a null nie mieści się w int — cały odczyt pada. Użyj typu obiektowego (Integer) i obsłuż null albo ustal
    //   z dostawcą, że pole jest zawsze.
    // PUŁAPKA: sklejanie adresu plusem: uri("/offers/" + sku). Dlaczego to błąd? Wartość z „/”, „?” albo spacją zmieni
    //   znaczenie adresu (inny zasób, inny parametr). Szablon {sku} koduje wartość bezpiecznie.

    // =================================================================================================
    // 3. toEntity(...) — status, nagłówki i ciało (SupplierClient.offerWithHeaders)
    // =================================================================================================
    //
    // body(...) daje tylko ciało. toEntity(Typ.class) daje ResponseEntity: getStatusCode(), getHeaders(), getBody().
    // Tu dostawca odsyła Cache-Control: max-age=60 — „wynik możesz trzymać w pamięci 60 sekund”. Inne typowe nagłówki
    // z cudzych API: Location (adres utworzonego zasobu), ETag (wersja), limity zapytań (rate limit).
    // toBodilessEntity() — gdy ciało nie jest potrzebne (np. DELETE), a status i nagłówki tak.
    //
    // DOBRA PRAKTYKA: zwracaj z klienta body(...), a toEntity(...) tylko tam, gdzie nagłówki/status są naprawdę
    //   potrzebne. Dlaczego? Reszta aplikacji nie musi wiedzieć, że pod spodem jest HTTP — dostaje rekord.

    // =================================================================================================
    // 4. LISTY — ParameterizedTypeReference i parametr zapytania (SupplierClient.offersInCategory)
    // =================================================================================================
    //
    // Klasa List.class nie mówi „lista CZEGO” — generyki znikają po kompilacji (wymazywanie typów, type erasure).
    // ParameterizedTypeReference: new ParameterizedTypeReference<List<SupplierOffer>>() {} — anonimowa podklasa
    // zapisuje pełny typ w metadanych klasy, a Spring/Jackson go odczytują.
    // Parametr zapytania: uri(uriBuilder -> uriBuilder.path("/supplier/api/offers").queryParam("category", c).build())
    // → .../offers?category=ELEKTRONIKA (adres bazowy dokłada się sam).
    //
    // PUŁAPKA: body(List.class). Dlaczego to błąd? Kompiluje się (z ostrzeżeniem), ale Jackson nie wie, na co
    //   zamieniać elementy, więc robi z nich mapy (LinkedHashMap). Wyjątek wybucha dopiero przy użyciu elementu:
    //   ClassCastException „class java.util.LinkedHashMap cannot be cast to class ...SupplierOffer” — daleko
    //   od miejsca błędu (sprawdzone testem).

    // =================================================================================================
    // 5. POST — rekord jako JSON w ciele (SupplierClient.placeOrder)
    // =================================================================================================
    //
    // post().uri(...).contentType(MediaType.APPLICATION_JSON).body(new SupplierOrder("KSI-001", 3)).retrieve()
    // → RestClient wybiera konwerter po typie obiektu i Content-Type, Jackson pisze {"sku":"KSI-001","quantity":3}.
    // Dostawca odpowiada 201 Created + Location: /supplier/api/orders/DOST-1001 + potwierdzenie (toEntity).
    //
    // PUŁAPKA: POST wysłany dwa razy (np. „spróbuj jeszcze raz” po błędzie sieci) to DWA zamówienia u dostawcy.
    //   Dlaczego? POST nie jest idempotentny (idempotentny = powtórzenie daje ten sam efekt). Bezpieczne ponawianie
    //   — lekcja Rc04Resilience.

    // =================================================================================================
    // 6. TESTY KLIENTA — MockRestServiceServer kontra prawdziwy serwer
    // =================================================================================================
    //
    // Rc01RestClientTest — @RestClientTest(components = SupplierClient.class) (Boot 4: pakiet
    // org.springframework.boot.restclient.test.autoconfigure, starter spring-boot-starter-restclient-test; w Boot 3
    // ta sama adnotacja była w org.springframework.boot.test.autoconfigure.web.client):
    //   • ładuje TYLKO klienta i infrastrukturę RestClienta (wycinek kontekstu, szybko),
    //   • podpina MockRestServiceServer pod builder od Boota — zapytania NIE wychodzą do sieci,
    //   • server.expect(requestTo(...)).andExpect(header(...)).andRespond(withSuccess(json, JSON)) — oczekiwanie
    //     i przygotowana odpowiedź; server.verify() — czy wszystkie oczekiwane zapytania padły.
    // Rc01RestClientServerTest — @SpringBootTest(webEnvironment = RANDOM_PORT): prawdziwy Tomcat z udawanym
    //   dostawcą (FakeSupplierController), zapytania przez localhost. Sprawdza to, czego atrapa nie widzi: JSON
    //   pisany przez prawdziwy kontroler, wymagany nagłówek X-Shop-Id (400 bez niego).
    //
    // DOBRA PRAKTYKA: w testach nigdy nie łącz się z prawdziwym API dostawcy. Dlaczego? Test zależałby od internetu,
    //   od cudzej awarii i od cudzych danych (dziś laptop jest, jutro nie), a POST tworzyłby prawdziwe zamówienia.
    // PUŁAPKA: w @RestClientTest dwa beany wstrzykujące RestClient.Builder. Dlaczego to błąd? Automatyczny
    //   MockRestServiceServer obsługuje JEDEN builder; przy dwóch pierwsze server.expect(...) rzuca
    //   IllegalStateException „Unable to use auto-configured MockRestServiceServer since
    //   MockServerRestClientCustomizer has been bound to more than one RestClient” (sprawdzone uruchomieniem).
    //   Wtedy wstrzyknij MockServerRestClientCustomizer i pobierz atrapę dla konkretnego buildera
    //   (getServer(builder)) albo zbuduj ją ręcznie: MockRestServiceServer.bindTo(builder).build().

    // =================================================================================================
    // 7. RestClient, RestTemplate, WebClient — który wybrać? (PYTANIE REKRUTACYJNE)
    // =================================================================================================
    //
    //   RestTemplate (od Spring 3.0) — starszy styl: getForObject(url, Typ.class), postForEntity(...). W Spring
    //     Framework 7.0 NIE jest oznaczony jako przestarzały, ale jego dokumentacja mówi wprost, że nowe funkcje
    //     trafiają do RestClient. Spotkasz go w istniejącym kodzie — działa (test sekcji 7), a do RestClienta można
    //     przejść stopniowo: RestClient.create(restTemplate) użyje tej samej konfiguracji.
    //   RestClient (od 6.1) — płynne API jak w WebClient, ale synchroniczne. Wybór domyślny dla zwykłej aplikacji MVC.
    //     Z wątkami wirtualnymi (s16_cache_scheduling_async/Async05VirtualThreads) czekanie na odpowiedź jest tanie.
    //   WebClient — klient REAKTYWNY (Mono/Flux) z modułu spring-webflux. Potrzebny w aplikacjach WebFlux i przy
    //     strumieniowaniu; do zwykłych wywołań w aplikacji MVC nie jest potrzebny (tu nie ma go nawet na classpath).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Wstrzykuj RestClient.Builder (prototype od Boota), buduj RAZ w konstruktorze: baseUrl, defaultHeader, build().
     *   • GET: get().uri("/x/{id}", id).retrieve().body(Typ.class) / requiredBody(...) / toEntity(...).
     *   • retrieve() nic nie wysyła — wysyła dopiero body/requiredBody/toEntity/toBodilessEntity.
     *   • Lista: body(new ParameterizedTypeReference<List<Typ>>() {}) — NIGDY body(List.class).
     *   • Parametry: uri(b -> b.path("/x").queryParam("k", v).build()); nigdy sklejanie adresu plusem.
     *   • POST: post().uri(...).contentType(APPLICATION_JSON).body(rekord).retrieve().toEntity(...).
     *   • JSON: nieznane pola są pomijane; brak pola dla int/boolean → RestClientException (użyj Integer).
     *   • Testy: @RestClientTest + MockRestServiceServer (expect/andRespond/verify) albo RANDOM_PORT; nigdy prawdziwe API.
     *   • RestTemplate — starszy, działa; WebClient — reaktywny (webflux); RestClient — wybór domyślny w MVC.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego w aplikacji Boota lepiej wstrzyknąć RestClient.Builder niż wywołać RestClient.create(...)?
     *   2. Co się stanie? Napiszesz restClient.get().uri("/supplier/api/offers/{sku}", "ELE-001").retrieve(); i nic
     *      więcej. Ile zapytań dostanie dostawca?
     *   3. Co się stanie? Dostawca dodał do JSON-a oferty nowe pole "promocja", którego SupplierOffer nie ma.
     *   4. Co się stanie? Dostawca przestał wysyłać pole "available" (w rekordzie: int available).
     *   5. ZNAJDŹ BŁĄD: List<SupplierOffer> offers = restClient.get().uri("/supplier/api/offers").retrieve()
     *      .body(List.class); String sku = offers.getFirst().sku(); — co i GDZIE wybuchnie?
     *   6. Co sprawdza @RestClientTest z MockRestServiceServer, a czego nie sprawdzi bez testu na RANDOM_PORT?
     *   7. PYTANIE REKRUTACYJNE: RestTemplate, RestClient, WebClient — czym się różnią i co wybrać w aplikacji MVC?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Rc01RestClientExercisesTest (dostawcę udaje MockRestServiceServer)
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): pobierz ofertę GET /supplier/api/offers/{sku} i zwróć samą NAZWĘ produktu.
     * Klient ma już ustawiony adres bazowy — podaj tylko ścieżkę.
     * Przykład: exercise1(client, "ELE-001") → "Laptop Pro 14".
     * Podpowiedź: get().uri("...{sku}", sku).retrieve().requiredBody(SupplierOffer.class).name().
     */
    static String exercise1(RestClient client, String sku) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): pobierz listę ofert GET /supplier/api/offers?category={category} i zwróć kody (sku)
     * tylko tych ofert, które dostawca MA ({@code available() > 0}), posortowane rosnąco.
     * Przykład: dla ELEKTRONIKA z ofertami ELE-003 (25 szt.), ELE-002 (0 szt.), ELE-001 (7 szt.) → [ELE-001, ELE-003].
     * Podpowiedź: ParameterizedTypeReference, queryParam, potem strumień: filter, map, sorted, toList.
     */
    static List<String> exercise2(RestClient client, String category) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): złóż zamówienie POST /supplier/api/orders z ciałem {@code new SupplierOrder(sku, quantity)}
     * jako JSON i zwróć adres z nagłówka Location odpowiedzi (np. /supplier/api/orders/DOST-1001).
     * Podpowiedź: contentType(MediaType.APPLICATION_JSON), body(...), retrieve().toBodilessEntity().getHeaders().getLocation().
     */
    static URI exercise3(RestClient client, String sku, int quantity) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): z podanego buildera zbuduj klienta, który w KAŻDYM zapytaniu: używa adresu bazowego
     * baseUrl, wysyła nagłówek X-Shop-Id z wartością shopId i nagłówek Accept: application/json.
     * Test wyśle przez niego zapytanie i sprawdzi adres oraz nagłówki.
     * Podpowiedź: baseUrl(...), defaultHeader(...) dwa razy, build() — jak w konstruktorze SupplierClient.
     */
    static RestClient exercise4(RestClient.Builder builder, String baseUrl, String shopId) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Builder od Boota ma już konwertery JSON z ustawieniami Boota, wykrytą bibliotekę HTTP, limity czasu
     *      z właściwości spring.http.clients.* i dodatki (metryki, a w testach MockRestServiceServer). RestClient.create()
     *      tego nie ma — np. test @RestClientTest nie przechwyci jego zapytań.
     *   2. Zero. retrieve() tylko opisuje odczyt odpowiedzi; zapytanie wychodzi dopiero przy body/requiredBody/
     *      toEntity/toBodilessEntity. Sprawdzone testem: MockRestServiceServer.verify() zgłasza
     *      „Further request(s) expected leaving 1 unsatisfied expectation(s).” i „0 request(s) executed.”.
     *   3. Nic złego: nieznane pole jest pomijane, oferta czyta się poprawnie (test section2UnknownField).
     *   4. RestClientException „Error while extracting response for type
     *      [s13_rest_client.rc01_rest_client.SupplierOffer] and content type [application/json]”, a w przyczynie
     *      (Jackson) „Cannot map `null` into type `int`”. Rozwiązanie: Integer zamiast int i obsługa null
     *      (test section2MissingPrimitive).
     *   5. Kod się kompiluje (tylko ostrzeżenie „unchecked”). Odczyt się udaje, ale elementy listy to LinkedHashMap,
     *      nie rekordy. Wybucha DOPIERO offers.getFirst().sku(): ClassCastException „class java.util.LinkedHashMap
     *      cannot be cast to class s13_rest_client.rc01_rest_client.SupplierOffer”. Poprawka:
     *      body(new ParameterizedTypeReference<List<SupplierOffer>>() {}) (test section4RawList).
     *   6. MockRestServiceServer sprawdza, CO klient wysyła (adres, metoda, nagłówki, ciało) i jak czyta przygotowaną
     *      odpowiedź — szybko i bez sieci. Nie sprawdzi prawdziwego serwera: jak naprawdę wygląda jego JSON, czy wymaga
     *      nagłówka (400 bez X-Shop-Id), jak przechodzi to przez sieć — to robi test na RANDOM_PORT.
     *   7. RestTemplate — starszy, synchroniczny, metody-szablony (getForObject); działa i nie jest przestarzały
     *      w Framework 7.0, ale nowe funkcje dostaje RestClient. RestClient — synchroniczny, płynne API, wybór
     *      domyślny w aplikacji MVC. WebClient — reaktywny (Mono/Flux), wymaga spring-webflux; do WebFlux i strumieni.
     */
    // </editor-fold>
}
