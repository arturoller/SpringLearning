package s22_extras.ext05_webflux_overview;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * <pre>
 * TEMAT: Przegląd WebFlux — programowanie reaktywne (Mono, Flux, nieblokujące I/O), kontroler WebFlux, WebTestClient,
 *        blokowanie w potoku reaktywnym i kiedy WebFlux NIE jest potrzebny
 *        (reactive = reaktywny — reagujący na dane, gdy nadejdą; non-blocking I/O = nieblokujące wejście/wyjście)
 *
 * W SKRÓCIE:
 *   • Spring MVC: jedno zapytanie = jeden wątek, który CZEKA na bazę czy inną usługę. WebFlux: kilka wątków pętli
 *     zdarzeń obsługuje tysiące zapytań, bo nikt nie czeka — kod opisuje, CO zrobić, gdy dane nadejdą.
 *   • Mono = 0 lub 1 element „kiedyś”, Flux = 0..N elementów „kiedyś”. Nic się nie dzieje bez subskrypcji.
 *   • Kontroler WebFlux wygląda jak MVC (@RestController, @GetMapping), ale zwraca Mono/Flux. Testy: WebTestClient.
 *   • Najgroźniejszy błąd: blokujący kod (block(), JDBC, Thread.sleep) na wątku pętli zdarzeń.
 *   • Od Javy 21 są wątki wirtualne: zwykły, blokujący kod MVC też obsłuży tysiące zapytań. WebFlux jest potrzebny
 *     rzadziej, niż się wydaje.
 *
 * ANALOGIA: kelner w restauracji.
 *   MVC: kelner przyjmuje zamówienie, idzie do kuchni i STOI przy niej, aż danie będzie gotowe — na 100 stolików
 *   potrzeba 100 kelnerów. WebFlux: kelner zostawia zamówienie w kuchni z karteczką „zawołaj, jak gotowe” i obsługuje
 *   kolejne stoliki — wystarczy czterech kelnerów. Ale gdy jeden z nich usiądzie i zacznie czytać gazetę (blokowanie),
 *   jedna czwarta sali przestaje być obsługiwana. Wątki wirtualne: tanich kelnerów-praktykantów jest dowolnie wielu,
 *   więc każdy może sobie stać przy kuchni.
 *
 * JAK TO DZIAŁA:
 *   GET /api/reactive/products/SPO-001
 *     → Netty (wątek pętli zdarzeń reactor-http-nio-N) → filtry WebFlux (Spring Security: SecurityWebFilterChain)
 *     → DispatcherHandler (odpowiednik DispatcherServlet) → ReactiveProductController.one("SPO-001")
 *     → zwraca Mono — PRZEPIS, nie wynik → WebFlux subskrybuje Mono → element → Jackson → odpowiedź.
 *   Puste Mono → switchIfEmpty(Mono.error(ResponseStatusException 404)) → 404.
 *
 * SŁÓWKA:
 *   publisher = wydawca (źródło danych: Mono, Flux); subscriber = subskrybent; subscribe = zasubskrybować (uruchomić
 *   przepis); operator = operator (map, filter, flatMap — krok potoku); event loop = pętla zdarzeń; backpressure =
 *   przeciwciśnienie (odbiorca mówi, ile danych przyjmie); scheduler = planista (pula wątków); bounded elastic =
 *   ograniczona elastyczna pula (na kod blokujący); virtual thread = wątek wirtualny; functional endpoint = punkt
 *   końcowy w stylu funkcyjnym (RouterFunction).
 *
 * ZOBACZ TEŻ: s16_cache_scheduling_async/Async05VirtualThreads (wątki wirtualne w MVC),
 *             s16_cache_scheduling_async/Async04Async (@Async i pule wątków), t21_concurrency/Concurrency05CompletableFuture
 *             (wynik „kiedyś” w zwykłej Javie), t16_streams/Streams16Laziness (leniwość strumieni),
 *             s13_rest_client/Rc01RestClient (RestClient — blokujący odpowiednik WebClient).
 * </pre>
 */
public final class Ext05WebFluxOverview {

    private Ext05WebFluxOverview() {
    }

    // =================================================================================================
    // 1. Mono I Flux — PRZEPIS, A NIE WYNIK (Ext05WebFluxOverviewReactorTest)
    // =================================================================================================
    //
    //   Mono<Integer> mono = Mono.fromSupplier(licznik::incrementAndGet);   ← nic się nie wykonało (licznik = 0)
    //   StepVerifier.create(mono).expectNext(1).verifyComplete();             ← subskrypcja: wykonanie (1)
    //   StepVerifier.create(mono).expectNext(2).verifyComplete();             ← druga subskrypcja: OD NOWA (2)
    //
    // Operatory jak w Stream API: map, filter, flatMap (gdy krok sam zwraca Mono/Flux), zip (połącz dwa), reduce.
    // Błąd to też sygnał: kończy strumień (dalszych elementów już nie ma); onErrorReturn / onErrorResume go obsługują.
    // Puste Mono to NIE null — „brak wyniku” obsługujesz switchIfEmpty albo defaultIfEmpty.
    // Testy bez Springa: StepVerifier z reactor-test (expectNext, expectError, verifyComplete).
    //
    // PUŁAPKA: Mono.just(drogieWyliczenie()) liczy argument OD RAZU — przy budowaniu przepisu, nawet gdy nikt nie
    //   zasubskrybuje (sprawdzone testem justIsEager). Dlaczego? To zwykłe wywołanie Javy przed wywołaniem just().
    //   Leniwie: Mono.fromSupplier(() -> drogieWyliczenie()) albo Mono.fromCallable(...).

    // =================================================================================================
    // 2. WebFlux OBOK MVC W JEDNYM MODULE — TYP APLIKACJI (Ext05Application)
    // =================================================================================================
    //
    // Gdy na classpath są spring-boot-starter-webmvc i spring-boot-starter-webflux, Boot wybiera SERVLET (MVC, Tomcat)
    // — sprawdzone: new SpringApplication(Ext05Application.class).getWebApplicationType() == SERVLET. Dlatego:
    //   application.setWebApplicationType(WebApplicationType.REACTIVE);      ← w main (albo
    //   spring.main.web-application-type=reactive                             ← właściwość)
    // Wtedy: serwer Netty („Netty started on port 8080”), DispatcherHandler zamiast DispatcherServlet, a Spring Security
    // w wersji reaktywnej: SecurityWebFilterChain + ServerHttpSecurity (zamiast SecurityFilterChain + HttpSecurity).
    //
    // PUŁAPKA: test nie uruchamia main! @SpringBootTest bez spring.main.web-application-type=reactive zbuduje kontekst
    //   SERVLET i padnie (sprawdzone): „Parameter 0 of method ext05OpenChain in ...Ext05Application required a bean of
    //   type 'org.springframework.security.config.web.server.ServerHttpSecurity' that could not be found.” Dlaczego?
    //   Reaktywne Security nie startuje w aplikacji servletowej. Testy lekcji mają więc
    //   @SpringBootTest(properties = "spring.main.web-application-type=reactive").
    //
    // DOBRA PRAKTYKA: w prawdziwym projekcie wybierz JEDEN stos — webmvc ALBO webflux. Dlaczego? Sam WebClient (klient
    //   reaktywny) możesz używać w MVC, ale dwa serwery w jednej aplikacji to tylko zamieszanie (jak w tym module, gdzie
    //   wymusza to dydaktyka).

    // =================================================================================================
    // 3. KONTROLER WebFlux I WebTestClient (ReactiveProductController, Ext05WebFluxOverviewTest)
    // =================================================================================================
    //
    //   @GetMapping public Flux<ReactiveProduct> all()                    → tablica JSON
    //   @GetMapping("/{sku}") public Mono<ReactiveProduct> one(...)        → obiekt JSON albo 404 (switchIfEmpty)
    // Styl funkcyjny (Ext05Application.helloRoute): RouterFunctions.route().GET("/api/reactive/hello",
    // request -> ServerResponse.ok().bodyValue(...)).build() — trasa jako bean, bez adnotacji.
    //
    // WebTestClient (klient testowy WebFlux, odpowiednik MockMvc):
    //   @AutoConfigureWebTestClient                         ← bean połączony z kontekstem (atrapa serwera, bez sieci)
    //   WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build()   ← prawdziwy serwer (RANDOM_PORT)
    //   client.get().uri("/api/reactive/products").exchange().expectStatus().isOk().expectBodyList(...).hasSize(4);

    // =================================================================================================
    // 4. PUŁAPKA NR 1: BLOKOWANIE W POTOKU REAKTYWNYM
    // =================================================================================================
    //
    // Wątków pętli zdarzeń jest mało — w Reactor Netty domyślnie tyle, ile rdzeni procesora (co najmniej 4). Jeden
    // zablokowany wątek = zablokowane WSZYSTKIE zapytania, które na nim czekają. Blokuje: block(), JDBC/JPA, RestClient/RestTemplate, Thread.sleep, odczyt pliku.
    //
    // ŹLE (ReactiveProductController.blockingStock):
    //   Integer stock = catalog.findBySku(sku).delayElement(...).map(...).block();
    // Reactor wykrywa block() na wątku nieblokującym i rzuca (sprawdzone — ten sam kod, różne wątki):
    //   aplikacja:              IllegalStateException „block()/blockFirst()/blockLast() are blocking, which is not
    //                           supported in thread reactor-http-nio-4” → 500
    //   test RANDOM_PORT:       to samo, wątek webflux-http-nio-4
    //   test na atrapie (MOCK): to samo, wątek parallel-1 (WebTestClient wykonuje obsługę na puli parallel)
    // Uwaga: wykrywane jest TYLKO block(). JDBC czy Thread.sleep blokują po cichu — w testach pomaga narzędzie BlockHound.
    //
    // DOBRZE (wholesalePrice) — gdy MUSISZ użyć starego, blokującego API:
    //   Mono.fromCallable(() -> legacy.wholesalePrice(product)).subscribeOn(Schedulers.boundedElastic())
    // Kod wykonuje się na puli boundedElastic (test: wątek „boundedElastic-1”), a wątek pętli zdarzeń jest wolny.
    // Lepiej: biblioteki reaktywne do końca (WebClient, R2DBC, reaktywne sterowniki MongoDB/Redis).
    //
    // CIEKAWOSTKA (sprawdzona uruchomieniem): z DOMYŚLNYM łańcuchem Spring Security kontroler działał na wątkach
    // parallel-N, nie reactor-http-nio-N. Dlaczego? Domyślny łańcuch sięga po WebSession, a InMemoryWebSessionStore
    // tworzy sesję tak: Mono.fromSupplier(...).subscribeOn(boundedElastic()).publishOn(parallel()). Łańcuch tej lekcji
    // jest bezstanowy (NoOpServerSecurityContextRepository, NoOpServerRequestCache) — jak przystało na API z tokenami.

    // =================================================================================================
    // 5. KIEDY NIE POTRZEBUJESZ WebFlux — WĄTKI WIRTUALNE (s16_cache_scheduling_async/Async05VirtualThreads)
    // =================================================================================================
    //
    // Główny argument za WebFlux brzmiał: „wątki są drogie, więc nie można ich blokować”. Od Javy 21 wątki wirtualne są
    // tanie — w MVC wystarczy spring.threads.virtual.enabled=true i zwykły, czytelny, blokujący kod (JPA, RestClient)
    // obsłuży bardzo dużo równoczesnych zapytań.
    //   Wybierz MVC (+ wątki wirtualne), gdy: typowe CRUD-y, JPA/JDBC, zespół zna MVC, liczy się prostota debugowania.
    //   Rozważ WebFlux, gdy: strumienie danych (Server-Sent Events, WebSocket, długie połączenia), cały stos jest
    //   reaktywny (R2DBC, reaktywne sterowniki), potrzebujesz backpressure, bramka API (Spring Cloud Gateway).
    //
    // PUŁAPKA: „przepiszmy na WebFlux, będzie szybciej”. Dlaczego zwykle nie? Pojedyncze zapytanie NIE jest szybsze — WebFlux
    //   poprawia przepustowość przy dużej liczbie czekających zapytań. Za to stosy wywołań i debugowanie są trudniejsze,
    //   a jedno blokujące wywołanie w potoku psuje całość.
    //
    // CO CZYTAĆ DALEJ: dokumentacja Spring Framework — „Web on Reactive Stack” (WebFlux, WebClient, functional endpoints),
    // Project Reactor — Reference Guide (operatory, Schedulers, Context), Spring Data R2DBC, BlockHound.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Mono = 0..1, Flux = 0..N; to przepisy — wykonują się przy subskrypcji, każda subskrypcja od nowa.
     *   • Mono.just(x) liczy x od razu; Mono.fromSupplier/fromCallable — leniwie.
     *   • Puste Mono: switchIfEmpty / defaultIfEmpty; błąd: onErrorReturn / onErrorResume; testy: StepVerifier.
     *   • MVC + WebFlux na classpath → Boot wybiera SERVLET; reaktywnie: setWebApplicationType(REACTIVE) albo
     *     spring.main.web-application-type=reactive (także w @SpringBootTest(properties = ...)).
     *   • Security reaktywne: SecurityWebFilterChain + ServerHttpSecurity.
     *   • Nie blokuj wątków pętli zdarzeń; konieczny blokujący kod → fromCallable(...).subscribeOn(boundedElastic()).
     *   • Testy: @AutoConfigureWebTestClient (atrapa) albo WebTestClient.bindToServer() (RANDOM_PORT).
     *   • Najpierw rozważ MVC + wątki wirtualne (spring.threads.virtual.enabled=true).
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się Mono od Flux? Co zwraca findBySku dla nieznanego SKU — null?
     *   2. Co się stanie? Mono<Integer> m = Mono.fromSupplier(licznik::incrementAndGet); zasubskrybujesz m dwa razy.
     *      Ile wyniesie licznik? A dla Mono.just(licznik.incrementAndGet()) bez żadnej subskrypcji?
     *   3. Dlaczego w tym module aplikacja domyślnie byłaby servletowa i jak to zmienić?
     *   4. ZNAJDŹ BŁĄD: test @SpringBootTest bez właściwości spring.main.web-application-type=reactive nie startuje.
     *      Jaki jest komunikat i dlaczego?
     *   5. Co się stanie? Wywołasz GET /api/reactive/products/ELE-001/blocking-stock w działającej aplikacji.
     *   6. Jak poprawnie użyć starego, blokującego serwisu w potoku reaktywnym?
     *   7. Kiedy wybrałbyś MVC z wątkami wirtualnymi zamiast WebFlux?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Ext05WebFluxOverviewExercisesTest (StepVerifier, bez Springa)
    // =================================================================================================

    /** Pozycja zamówienia (ćwiczenie 3): sku, liczba sztuk, cena jednostkowa w złotych. */
    public record OrderLine(String sku, int quantity, BigDecimal unitPrice) {
    }

    /**
     * ĆWICZENIE 1 (łatwe): z Fluxa produktów zostaw tylko dostępne (stock większy od 0) i zwróć ich nazwy WIELKIMI
     * literami, w tej samej kolejności.
     * Podpowiedź: filter, map, toUpperCase().
     */
    static Flux<String> exercise1(Flux<ReactiveProduct> products) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): etykieta ceny produktu: "Słuchawki BT: 349.90 zł". Gdy Mono jest puste — "brak produktu".
     * Gdy Mono kończy się błędem — "cena niedostępna".
     * Podpowiedź: map, defaultIfEmpty, onErrorReturn — uwaga na kolejność operatorów.
     */
    static Mono<String> exercise2(Mono<ReactiveProduct> product) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): wartość zamówienia: suma quantity × unitPrice dla pozycji z quantity większym od 0.
     * Pusty Flux (albo same pozycje z zerem sztuk) → BigDecimal.ZERO. Wynik jako Mono.
     * Podpowiedź: filter, map, reduce(BigDecimal.ZERO, BigDecimal::add) — reduce na pustym Fluxie z wartością
     * początkową zwraca tę wartość (t16_streams/Streams07Reduce).
     */
    static Mono<BigDecimal> exercise3(Flux<OrderLine> lines) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Mono = co najwyżej jeden element, Flux = dowolnie wiele. findBySku dla nieznanego SKU zwraca PUSTE Mono
     *      (kończy się bez elementu) — nie null. Sprawdzone testem emptyMono.
     *   2. fromSupplier: 2 — każda subskrypcja wykonuje dostawcę od nowa (test lazyUntilSubscribe). Mono.just(...): 1,
     *      choć nikt nie subskrybował — argument policzono przy budowaniu (test justIsEager).
     *   3. Na classpath są i spring-boot-starter-webmvc, i spring-boot-starter-webflux — przy obu Boot wybiera SERVLET
     *      (sprawdzone: getWebApplicationType() == SERVLET). Zmiana: setWebApplicationType(WebApplicationType.REACTIVE)
     *      w main albo spring.main.web-application-type=reactive.
     *   4. „Parameter 0 of method ext05OpenChain in s22_extras.ext05_webflux_overview.Ext05Application required a bean
     *      of type 'org.springframework.security.config.web.server.ServerHttpSecurity' that could not be found.”
     *      Test nie woła main, więc kontekst jest SERVLET, a w nim nie ma reaktywnego Security. Poprawka:
     *      @SpringBootTest(properties = "spring.main.web-application-type=reactive"). Sprawdzone uruchomieniem.
     *   5. 500 Internal Server Error, w logu IllegalStateException „block()/blockFirst()/blockLast() are blocking, which
     *      is not supported in thread reactor-http-nio-4” (numer wątku może być inny). Sprawdzone uruchomieniem i testem.
     *   6. Mono.fromCallable(() -> stary.metoda(...)).subscribeOn(Schedulers.boundedElastic()) — wywołanie idzie na pulę
     *      przeznaczoną do blokowania, wątek pętli zdarzeń zostaje wolny (test: wątek boundedElastic-1).
     *   7. Typowe API z bazą przez JPA/JDBC, zespół zna MVC, liczy się prostota i czytelne stosy wywołań — wątki wirtualne
     *      (spring.threads.virtual.enabled=true) dają wysoką współbieżność bez przepisywania kodu na Mono/Flux.
     */
    // </editor-fold>
}
