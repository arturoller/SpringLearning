package s13_rest_client.rc03_http_interfaces;

import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * <pre>
 * TEMAT: Interfejsy HTTP — klient opisany interfejsem: {@code @HttpExchange}, {@code @GetExchange}, {@code @PostExchange}
 *        i grupy klientów w Spring Framework 7 / Boot 4 ({@code @ImportHttpServices}, spring.http.serviceclient.*)
 *        (interface = interfejs; exchange = wymiana; proxy = obiekt pośredniczący; group = grupa)
 *
 * W SKRÓCIE:
 *   Zamiast pisać w każdej metodzie get().uri(...).retrieve().body(...), opisujesz API dostawcy INTERFEJSEM Javy
 *   z adnotacjami — tak jak kontroler opisuje Twoje API (@GetMapping ↔ @GetExchange). Implementację tworzy Spring:
 *   obiekt pośredniczący (proxy), który z adnotacji i argumentów składa zapytanie i wysyła je przez RestClient.
 *   W Spring Framework 6 / Boot 3 proxy tworzyło się ręcznie (HttpServiceProxyFactory w metodzie @Bean).
 *   W Spring Framework 7 / Boot 4 wystarczy @ImportHttpServices(group = "dostawca", types = SupplierApi.class),
 *   a adres, nagłówki i limity czasu grupy ustawiasz właściwościami spring.http.serviceclient.dostawca.*.
 *
 * ANALOGIA: formularz zamówienia z hurtowni zamiast rozmowy telefonicznej.
 *   Hurtownia daje Ci gotowy druk: pole „kod produktu”, pole „ilość”, adres do wysyłki nadrukowany w rogu (interfejs
 *   z adnotacjami). Ty tylko wpisujesz wartości (argumenty metody), a poczta (proxy + RestClient) dba o kopertę,
 *   znaczek i doręczenie. Grupa klientów to przegródka w biurze „sprawy z hurtownią X” — jeden adres, jedna pieczątka
 *   (nagłówek X-Shop-Id) dla wszystkich jej formularzy.
 *
 * JAK TO DZIAŁA:
 *   {@code @HttpExchange(url = "/supplier/api", accept = "application/json")}
 *   interface SupplierApi {
 *       {@code @GetExchange("/offers/{sku}")}  SupplierOffer offer({@code @PathVariable} String sku);
 *   }
 *   supplierApi.offer("ELE-001")
 *     → proxy czyta adnotacje: GET + "/supplier/api" + "/offers/{sku}", {sku} = "ELE-001", Accept: application/json
 *     → RestClient grupy „dostawca”: base-url + nagłówek X-Shop-Id z application-rc03.properties
 *     → GET http://localhost:8080/supplier/api/offers/ELE-001 → JSON → SupplierOffer (typ z sygnatury metody)
 *
 *   Boot 4: @ImportHttpServices(group = "dostawca", types = SupplierApi.class)
 *     → bean „dostawca#s13_rest_client.rc03_http_interfaces.SupplierApi” (proxy JDK)
 *     → spring.http.serviceclient.dostawca.base-url / .default-header.X-Shop-Id / .read-timeout ...
 *
 * SŁÓWKA:
 *   HTTP interface = interfejs HTTP (deklaratywny klient); declarative = deklaratywny („opisz CO, nie JAK”);
 *   proxy = obiekt pośredniczący; adapter = przejściówka; HttpServiceProxyFactory = fabryka proxy usług HTTP;
 *   group = grupa (klienci z tym samym RestClientem i ustawieniami); registry = rejestr; decorator = dekorator
 *   (obiekt owijający inny, by zmienić jego zachowanie); configurer = konfigurator; resolver = rozwiązywacz
 *   (tu: kto zamienia argument metody na część zapytania).
 *
 * ZOBACZ TEŻ: s13_rest_client/Rc01RestClient (RestClient, na którym działa proxy), t19_annotations_reflection/Annotations06DynamicProxy
 *             (jak działa proxy JDK), s04_web_rest/Rest02RequestData (@PathVariable, @RequestParam po stronie serwera),
 *             s18_aop/Aop02Proxies (proxy w Springu), s02_configuration/Cfg01Properties (właściwości),
 *             s13_rest_client/Rc04Resilience (ponawianie).
 * </pre>
 */
public final class Rc03HttpInterfaces {

    private Rc03HttpInterfaces() {
    }

    // =================================================================================================
    // 1. INTERFEJS Z ADNOTACJAMI (SupplierApi)
    // =================================================================================================
    //
    // @HttpExchange na interfejsie — wspólne dla wszystkich metod: początek ścieżki (url), accept, contentType,
    //   headers. @GetExchange, @PostExchange, @PutExchange, @PatchExchange, @DeleteExchange — metoda HTTP + ścieżka.
    // Parametry metody opisują części zapytania — te same adnotacje co w kontrolerze:
    //   @PathVariable → {sku} w ścieżce, @RequestParam → ?category=..., @RequestBody → ciało (JSON),
    //   @RequestHeader → nagłówek, URI jako zwykły parametr → inny adres dla tego jednego wywołania.
    // Typ zwracany decyduje, co dostaniesz: SupplierOffer (ciało), List<SupplierOffer> (typ z generykiem odczytany
    //   z SYGNATURY — bez ParameterizedTypeReference!), ResponseEntity (status + nagłówki + ciało), Optional, void.
    //
    // PUŁAPKA: parametr bez adnotacji: @GetExchange("/offers/{sku}") SupplierOffer offer(String sku). Dlaczego to błąd?
    //   Spring nie zgadnie, czym jest argument. Proxy powstaje, ale WYWOŁANIE kończy się IllegalStateException
    //   „Could not resolve parameter [0] in public abstract ...offer(java.lang.String): No suitable resolver”
    //   (sprawdzone uruchomieniem) — i żadne zapytanie nie wychodzi.
    // DOBRA PRAKTYKA: w interfejsie tylko ścieżki, adres serwera — w konfiguracji grupy. Dlaczego? Ten sam interfejs
    //   działa wtedy z adresem testowym, produkcyjnym i z atrapą w teście.

    // =================================================================================================
    // 2. PROXY RĘCZNIE — HttpServiceProxyFactory + RestClientAdapter (SupplierApiFactory; styl Framework 6 / Boot 3)
    // =================================================================================================
    //
    //   HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build().createClient(SupplierApi.class)
    // RestClientAdapter łączy fabrykę z RestClientem (adres, nagłówki domyślne, limity — z RestClienta).
    // Wynik to proxy JDK (java.lang.reflect.Proxy): każde wywołanie metody trafia do przechwytywacza, który dla
    // metody z adnotacją wysyła zapytanie, a metodę default interfejsu po prostu wykonuje.
    // W Boot 3 ten kod siedział w metodzie @Bean — spotkasz go w wielu tutorialach; w Boot 4 nadal działa.
    //
    // PUŁAPKA: proxy na „gołym” RestClient.create() w aplikacji Boota. Dlaczego to błąd? Z tych samych powodów co
    //   w Rc01: brak ustawień Boota (limity, metryki) i brak atrapy w testach. Buduj RestClient z buildera od Boota —
    //   albo użyj grup (sekcja 3), które robią to same.

    // =================================================================================================
    // 3. GRUPY KLIENTÓW W BOOT 4 — @ImportHttpServices + spring.http.serviceclient.<grupa>.* (Rc03Application)
    // =================================================================================================
    //
    // @ImportHttpServices(group = "dostawca", types = SupplierApi.class) — na klasie @Configuration (u nas na klasie
    // startowej). Spring Framework 7 dla każdej GRUPY tworzy jeden RestClient i jedną fabrykę proxy, a dla każdego
    // interfejsu bean o nazwie „grupa#pełna.nazwa.Interfejsu” — tu „dostawca#s13_rest_client.rc03_http_interfaces.SupplierApi”.
    // Zamiast types można skanować pakiet: basePackageClasses / basePackages. Bez group — grupa „default”.
    // Boot 4 konfiguruje RestClient grupy z właściwości (application-rc03.properties):
    //   spring.http.serviceclient.dostawca.base-url=http://localhost:8080
    //   spring.http.serviceclient.dostawca.default-header.X-Shop-Id=sklep-01
    //   spring.http.serviceclient.dostawca.connect-timeout=2s / .read-timeout=3s   (nadpisują spring.http.clients.*)
    // oraz stosuje do grupy wszystkie beany RestClientCustomizer — dlatego @AutoConfigureMockRestServiceServer
    // w teście podpina atrapę także pod grupę (Rc03HttpInterfacesGroupTest).
    // Dostosowanie grupy w kodzie: bean RestClientHttpServiceGroupConfigurer, np.
    //   groups -> groups.filterByName("dostawca").forEachClient((group, builder) -> builder.requestInterceptor(...))
    //
    // PUŁAPKA: interfejs z adnotacjami, ale bez @ImportHttpServices (ani ręcznego @Bean). Dlaczego to błąd? Boot NIE
    //   skanuje sam interfejsów @HttpExchange — nie ma beana, a kontekst nie wstaje: „No qualifying bean of type
    //   's13_rest_client.rc03_http_interfaces.SupplierApi' available” (sprawdzone testem).
    // DOBRA PRAKTYKA: jedna grupa = jeden zewnętrzny system (dostawca, kurier, bramka płatności). Dlaczego? Każdy ma
    //   inny adres, inne uwierzytelnienie i inne limity czasu — a grupa trzyma je razem w jednym miejscu.

    // =================================================================================================
    // 4. BŁĘDY W INTERFEJSACH HTTP — 404 i NotFoundRestClientAdapterDecorator (Framework 7)
    // =================================================================================================
    //
    // Proxy używa RestClienta, więc błędy są te same co w Rc02: 404 → HttpClientErrorException.NotFound itd.
    // Uwaga: także gdy metoda zwraca ResponseEntity albo Optional — domyślnie 404 to WYJĄTEK, a nie ResponseEntity 404
    // czy Optional.empty() (sprawdzone testem section4Default).
    // Gdy „nie ma” ma być zwykłą odpowiedzią: NotFoundRestClientAdapterDecorator (Spring Framework 7) owija adapter i
    // zamienia 404 na: null (typ zwykły), ResponseEntity 404, Optional.empty(). Ręcznie:
    //   HttpServiceProxyFactory.builderFor(adapter).exchangeAdapterDecorator(NotFoundRestClientAdapterDecorator::new)
    // W Boot — bean konfiguratora grupy (Rc03HttpInterfacesNotFoundTest):
    //   groups -> groups.filterByName("dostawca")
    //                   .forEachProxyFactory((group, factory) -> factory.exchangeAdapterDecorator(NotFoundRestClientAdapterDecorator::new))
    // Inne statusy i błędy sieci zachowują się jak w Rc02 — do własnych wyjątków dla CAŁEJ grupy służy
    // builder.defaultStatusHandler(...) w forEachClient.
    //
    // DOBRA PRAKTYKA: wybierz jedno zachowanie dla grupy i zapisz je w nazwie/typie metody (findOffer → Optional,
    //   offer → wyjątek). Dlaczego? Wołający od razu widzi, czy „brak” jest normalny, czy jest błędem.

    // =================================================================================================
    // 5. RestClient CZY INTERFEJS? (PYTANIE REKRUTACYJNE)
    // =================================================================================================
    //
    // Interfejs: wiele metod do jednego API, proste mapowanie „metoda Javy = zapytanie”, mało kodu, łatwy w testach
    //   serwisów (wstrzykujesz atrapę interfejsu). Spring sam tłumaczy argumenty i typ zwracany.
    // RestClient wprost: nietypowa obsługa odpowiedzi (exchange, strumieniowanie), dynamicznie składane zapytania,
    //   jedno-dwa wywołania. Oba działają na tym samym RestClient — możesz je mieszać.
    // (Ten sam mechanizm działa z WebClient — wtedy metody zwracają Mono/Flux; w tym module go nie ma.)

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @HttpExchange(url = "/supplier/api", accept = "application/json") na interfejsie; @GetExchange("/offers/{sku}"),
     *     @PostExchange(url = "/orders", contentType = "application/json") na metodach.
     *   • Parametry: @PathVariable, @RequestParam, @RequestBody, @RequestHeader — parametr bez adnotacji = błąd przy wywołaniu.
     *   • Typ zwracany: rekord, List<...> (bez ParameterizedTypeReference), ResponseEntity, Optional, void.
     *   • Ręcznie (Boot 3): HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build().createClient(Api.class).
     *   • Boot 4: @ImportHttpServices(group = "dostawca", types = Api.class) → bean „dostawca#pakiet.Api”.
     *   • Właściwości grupy: spring.http.serviceclient.dostawca.base-url / default-header.X / connect-timeout / read-timeout.
     *   • Kod grupy: bean RestClientHttpServiceGroupConfigurer → filterByName(...).forEachClient / forEachProxyFactory.
     *   • 404 domyślnie = wyjątek (też dla ResponseEntity i Optional); NotFoundRestClientAdapterDecorator → null/404/empty.
     *   • Testy: proxy na RestClient z MockRestServiceServer albo @SpringBootTest + @AutoConfigureMockRestServiceServer.
     *
     * PYTANIA KONTROLNE:
     *   1. Kto pisze implementację interfejsu SupplierApi i jak ona „wie”, jakie zapytanie wysłać?
     *   2. Co się stanie? Usuniesz @ImportHttpServices z Rc03Application, a CheapestOfferService nadal wstrzykuje SupplierApi.
     *   3. Co się stanie? Dostawca odpowie 404, a Ty wywołasz offerEntity("XXX-999") (metoda zwraca ResponseEntity).
     *      Dostaniesz ResponseEntity ze statusem 404?
     *   4. ZNAJDŹ BŁĄD: @GetExchange("/offers/{sku}") SupplierOffer offer(String sku); — co i KIEDY się stanie?
     *   5. Gdzie w Boot 4 ustawisz adres bazowy, nagłówek domyślny i limit czasu odczytu tylko dla grupy „dostawca”?
     *   6. Czym różni się @ImportHttpServices od metody @Bean z HttpServiceProxyFactory (styl Boot 3)?
     *   7. PYTANIE REKRUTACYJNE: kiedy wybrać interfejs HTTP, a kiedy RestClient wprost?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Rc03HttpInterfacesExercisesTest
    // =================================================================================================

    /**
     * Interfejs do ĆWICZEŃ 1–3. Metody mają na razie ciała {@code default}, które rzucają wyjątek „TODO” — proxy
     * wykonuje metody default zwyczajnie, więc testy padają, dopóki ich nie zamienisz w metody HTTP.
     * Dla każdej metody: USUŃ słowo {@code default} i ciało (zostaw sam średnik), DODAJ adnotacje.
     * Test tworzy proxy na RestClient z adresem bazowym http://dostawca.test.
     */
    public interface ExerciseSupplierApi {

        /**
         * ĆWICZENIE 1 (łatwe): GET /supplier/api/offers/{sku} → SupplierOffer.
         * Podpowiedź: @GetExchange("/supplier/api/offers/{sku}") nad metodą i @PathVariable przed parametrem.
         */
        default SupplierOffer offer(String sku) {
            // TODO: zamień na metodę HTTP (bez default i bez ciała) z adnotacjami
            throw new UnsupportedOperationException("TODO");
        }

        /**
         * ĆWICZENIE 2 (średnie): GET /supplier/api/offers?category={category} → lista ofert.
         * Podpowiedź: @GetExchange + @RequestParam; typ {@code List<SupplierOffer>} wystarczy w sygnaturze.
         */
        default List<SupplierOffer> offers(String category) {
            // TODO: zamień na metodę HTTP z adnotacjami
            throw new UnsupportedOperationException("TODO");
        }

        /**
         * ĆWICZENIE 3 (średnie): POST /supplier/api/orders z zamówieniem jako JSON w ciele i nagłówkiem
         * Idempotency-Key (klucz idempotencji — po co, zobaczysz w Rc04) → SupplierOrderConfirmation.
         * Podpowiedź: @PostExchange(url = "...", contentType = MediaType.APPLICATION_JSON_VALUE), @RequestBody,
         * @RequestHeader("Idempotency-Key").
         */
        default SupplierOrderConfirmation order(SupplierOrder order, String idempotencyKey) {
            // TODO: zamień na metodę HTTP z adnotacjami
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): utwórz proxy DOWOLNEGO interfejsu HTTP na podanym RestClient — bez Springa Boot,
     * samą fabryką. Test sprawdzi to na SupplierApi z tej lekcji: proxy musi być prawdziwym proxy JDK i wysyłać zapytania.
     * Podpowiedź: HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build().createClient(serviceType).
     */
    static <S> S exercise4(RestClient restClient, Class<S> serviceType) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Spring: HttpServiceProxyFactory tworzy proxy JDK. Przy wywołaniu przechwytywacz czyta adnotacje metody
     *      i interfejsu (metoda HTTP, ścieżka, accept), zamienia argumenty na części zapytania według ich adnotacji
     *      (@PathVariable, @RequestParam, @RequestBody...), wysyła zapytanie przez RestClient i zamienia odpowiedź
     *      na typ zwracany metody.
     *   2. Kontekst nie wstaje: UnsatisfiedDependencyException przy tworzeniu cheapestOfferService, w przyczynie
     *      „No qualifying bean of type 's13_rest_client.rc03_http_interfaces.SupplierApi' available” — Boot sam nie
     *      skanuje interfejsów @HttpExchange (test section3WithoutImport).
     *   3. Nie. Domyślnie 404 kończy się wyjątkiem HttpClientErrorException.NotFound także dla metod zwracających
     *      ResponseEntity i Optional (test section4Default). ResponseEntity 404 dostaniesz dopiero z
     *      NotFoundRestClientAdapterDecorator (ręcznie albo przez RestClientHttpServiceGroupConfigurer).
     *   4. Brak @PathVariable. Proxy powstanie bez błędu, ale przy WYWOŁANIU offer(...) poleci IllegalStateException
     *      „Could not resolve parameter [0] in public abstract ...SupplierOffer ...offer(java.lang.String): No suitable
     *      resolver” — zanim wyjdzie jakiekolwiek zapytanie (sprawdzone uruchomieniem). Poprawka: @PathVariable String sku.
     *   5. We właściwościach grupy: spring.http.serviceclient.dostawca.base-url,
     *      spring.http.serviceclient.dostawca.default-header.<Nazwa>, spring.http.serviceclient.dostawca.read-timeout
     *      (u nas w application-rc03.properties). Wartości grupy mają pierwszeństwo przed spring.http.clients.*.
     *   6. @Bean: piszesz sam RestClient, adapter, fabrykę i createClient — dla każdego interfejsu; ustawienia w kodzie.
     *      @ImportHttpServices: Spring rejestruje beany sam (także skanując pakiety), jeden RestClient na grupę,
     *      a Boot konfiguruje go z właściwości i dokłada swoje dostosowania (np. atrapę w testach).
     *   7. Interfejs — gdy API ma wiele zwykłych operacji „zapytanie → rekord”: mniej kodu, czytelna lista operacji,
     *      łatwe atrapy w testach serwisów. RestClient — gdy potrzebujesz pełnej kontroli nad odpowiedzią (exchange),
     *      dynamicznie składanych zapytań albo to tylko jedno wywołanie. Oba działają na tym samym RestClient.
     */
    // </editor-fold>
}
