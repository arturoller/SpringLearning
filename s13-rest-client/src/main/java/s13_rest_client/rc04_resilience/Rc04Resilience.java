package s13_rest_client.rc04_resilience;

import org.springframework.core.retry.RetryPolicy;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.function.Supplier;

/**
 * <pre>
 * TEMAT: Odporność wbudowana w Spring Framework 7 — {@code @Retryable}, {@code @ConcurrencyLimit}, {@code @EnableResilientMethods},
 *        RetryTemplate; ponawianie bez dublowania zamówień
 *        (resilience = odporność; retry = ponowienie; concurrency limit = limit współbieżności; idempotent = idempotentny)
 *
 * W SKRÓCIE:
 *   Zewnętrzne API miewa chwilowe kłopoty: 503 „przerwa”, zerwane połączenie, przekroczony czas. Często druga albo
 *   trzecia próba się udaje. Spring Framework 7 ma to w rdzeniu (wcześniej trzeba było dołączać bibliotekę Spring Retry):
 *   {@code @Retryable} ponawia metodę przy wskazanych wyjątkach (ile razy, z jakim odstępem), {@code @ConcurrencyLimit} pilnuje,
 *   żeby naraz działało najwyżej N wywołań, a RetryTemplate robi to samo w kodzie. Wszystko działa przez proxy —
 *   po włączeniu {@code @EnableResilientMethods}. Najważniejsza pułapka: ponawiać wolno tylko operacje IDEMPOTENTNE,
 *   inaczej jedno zamówienie zamieni się w dwa.
 *
 * ANALOGIA: dzwonienie do hurtowni, gdy linia jest zajęta.
 *   Zajęte (503)? Odkładasz słuchawkę, czekasz chwilę i dzwonisz znowu — najwyżej 3 razy, coraz dłużej czekając
 *   (opóźnienie z mnożnikiem), bo dzwonienie co sekundę tylko blokuje im linię. Ale gdy usłyszysz „nie mamy takiego
 *   towaru” (404), ponowny telefon nic nie da. A jeśli zamówienie podyktowałeś i rozmowa się urwała, nie dyktujesz go
 *   drugi raz od zera — podajesz numer zamówienia („to samo co przed chwilą, nr ZAM-0001”), żeby hurtownia nie wysłała
 *   towaru podwójnie (klucz idempotencji). Limit współbieżności to zasada „najwyżej 2 nasze telefony naraz”.
 *
 * JAK TO DZIAŁA:
 *   {@code @EnableResilientMethods} (Rc04Application)
 *     → RetryAnnotationBeanPostProcessor + ConcurrencyLimitBeanPostProcessor owijają beany w proxy (CGLIB)
 *   shopController → supplierClient (PROXY) → interceptor @Retryable → prawdziwy SupplierClient.price("ELE-001")
 *        próba 1: 503  → MethodRetryEvent → czekaj delay (200 ms)
 *        próba 2: 503  → MethodRetryEvent → czekaj delay × multiplier (400 ms)
 *        próba 3: 200  → wynik wraca do kontrolera; wołający nie wie o awariach
 *        (4 × 503 przy maxRetries = 3 → do wołającego leci OSTATNI wyjątek: HttpServerErrorException.ServiceUnavailable)
 *   supplierClient.basketPrices(...) → this.price(...) — wywołanie WEWNĄTRZ obiektu, z pominięciem proxy → 0 ponowień
 *
 * SŁÓWKA:
 *   retry = ponowienie; max retries = maksymalna liczba ponowień (BEZ pierwszej próby); delay = opóźnienie;
 *   multiplier = mnożnik (wykładnicze wydłużanie odstępów, exponential backoff); jitter = rozrzut (losowa poprawka
 *   odstępu); includes/excludes = uwzględnij/pomiń (typy wyjątków); transient = przejściowy; idempotent = idempotentny
 *   (powtórzenie daje ten sam efekt co jedno wykonanie); idempotency key = klucz idempotencji; throttle = dławik;
 *   reject = odrzuć; block = zablokuj (czekaj); flaky = zawodny, kapryśny; self-invocation = wywołanie z wnętrza obiektu.
 *
 * ZOBACZ TEŻ: s13_rest_client/Rc02ErrorsAndTimeouts (które wyjątki są przejściowe), s10_transactions/Tx03ProxyPitfalls
 *             (ta sama pułapka proxy przy @Transactional), s18_aop/Aop02Proxies (jak działa proxy),
 *             s16_cache_scheduling_async/Async05VirtualThreads (limit współbieżności a wątki wirtualne),
 *             s17_events/Evt01ApplicationEvents (zdarzenia), t21_concurrency/Concurrency07Synchronizers (zatrzaski w testach).
 * </pre>
 */
public final class Rc04Resilience {

    private Rc04Resilience() {
    }

    // =================================================================================================
    // 1. CO PONAWIAĆ, A CZEGO NIE
    // =================================================================================================
    //
    // Ponawiaj awarie PRZEJŚCIOWE — takie, które za chwilę mogą zniknąć:
    //   503 Service Unavailable, 502/504 (bramka), 429 Too Many Requests (z odczekaniem), zerwane połączenie,
    //   przekroczony limit czasu (ResourceAccessException).
    // NIE ponawiaj błędów „na stałe”: 400 (złe dane), 401/403 (brak uprawnień), 404 (nie ma), 409 (konflikt) — powtórka
    //   da ten sam wynik, a tylko obciąży dostawcę.
    // Ponawiaj tylko operacje IDEMPOTENTNE. W HTTP (RFC 9110) idempotentne są GET, HEAD, OPTIONS, PUT i DELETE;
    //   POST i PATCH — nie (każdy POST może utworzyć nowy zasób). Szczegóły — sekcja 5.
    //
    // DOBRA PRAKTYKA: zawsze podawaj includes (albo predicate). Dlaczego? @Retryable bez includes ponawia KAŻDY wyjątek
    //   (tak mówi dokumentacja adnotacji) — także 404 i błąd w Twoim kodzie (NullPointerException), który przy każdej
    //   próbie będzie taki sam. Test notIncludedExceptionIsNotRetried pokazuje, że wyjątek spoza includes kończy się
    //   po jednej próbie.

    // =================================================================================================
    // 2. @Retryable — liczba prób, opóźnienia, wyjątki (SupplierClient.price)
    // =================================================================================================
    //
    // org.springframework.resilience.annotation.Retryable (Spring Framework 7.0) — na metodzie albo na klasie:
    //   includes / value  — które wyjątki ponawiać (sprawdzane są też PRZYCZYNY — getCause() — wyjątku); excludes — których nie;
    //   predicate         — własna klasa MethodRetryPredicate, gdy typ wyjątku nie wystarcza (np. tylko 503 i 429);
    //   maxRetries        — ile PONOWIEŃ (domyślnie 3) → najwyżej 1 + maxRetries wywołań;
    //   delay             — odstęp przed pierwszym ponowieniem (domyślnie 1000 ms); multiplier — mnożnik kolejnych
    //                       odstępów (domyślnie 1.0 = stały; musi być ≥ 1); maxDelay — górna granica odstępu;
    //   jitter            — losowy rozrzut odstępu (żeby tysiąc klientów nie wróciło w tej samej milisekundzie);
    //   timeout           — łączny limit czasu wszystkich prób (od 7.0.2);
    //   …String           — wersje tekstowe z ${...}: maxRetriesString = "${rc04.retry.max-retries}",
    //                       delayString = "${rc04.retry.delay}" (np. "200ms") — wartości z application-rc04.properties.
    // Po wyczerpaniu prób do wołającego leci OSTATNI wyjątek metody (np. HttpServerErrorException.ServiceUnavailable),
    // a nie żaden „RetryException” — kod łapiący wyjątki nie musi się zmieniać.
    // Każda nieudana próba publikuje MethodRetryEvent (RetryLogger zapisuje go w logu: „Nieudana próba price: ...”),
    // a koniec prób — jeszcze jeden z isRetryAborted() == true.
    //
    // PUŁAPKA: tutoriale ze Spring Retry (osobna biblioteka, org.springframework.retry): @EnableRetry,
    //   @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 100)), @Recover. Dlaczego to pułapka? To INNA adnotacja
    //   o tej samej nazwie. W szczególności maxAttempts liczy WSZYSTKIE próby (3 = 1 + 2 ponowienia), a nowe maxRetries —
    //   tylko ponowienia (3 = 1 + 3). Sprawdzaj import: org.springframework.resilience.annotation.Retryable.
    // DOBRA PRAKTYKA: w testach ustaw opóźnienie na 0 (u nas rc04.retry.delay=0ms) i licz ZAPYTANIA (MockRestServiceServer
    //   + verify), a nie czas. Dlaczego? Testy są szybkie i nie zależą od obciążenia komputera.

    // =================================================================================================
    // 3. WŁĄCZENIE I PROXY — @EnableResilientMethods, wywołanie z tej samej klasy (PYTANIE REKRUTACYJNE)
    // =================================================================================================
    //
    // @EnableResilientMethods (na klasie @Configuration, u nas Rc04Application) rejestruje post-procesory, które
    // owijają beany z @Retryable/@ConcurrencyLimit w proxy (tu CGLIB — podklasa SupplierClient). Spring Boot 4.1 tego
    // NIE robi sam (żaden jar Boota 4.1.1 nie zawiera takiej autokonfiguracji) — bez adnotacji @Retryable jest po cichu
    // ignorowany: jedna próba, zero ostrzeżeń (test withoutEnableResilientMethods).
    // Proxy działa tylko na wywołania, które PRZEZ nie przechodzą:
    //   • kontroler → supplierClient.price(...) — przez proxy → ponowienia działają;
    //   • supplierClient.basketPrices(...) → this.price(...) — wywołanie wewnątrz obiektu docelowego, proxy go nie widzi
    //     → 0 ponowień (test section3SelfInvocation: dokładnie 1 zapytanie);
    //   • new FlakyOperation(...) — zwykły obiekt, nie bean → nikt go nie owinął → 0 ponowień.
    //
    // PUŁAPKA: wywołanie metody @Retryable z tej samej klasy. Dlaczego to błąd? this wskazuje na obiekt docelowy, nie
    //   na proxy. Rozwiązania: przenieś metodę do innego beana (najczystsze) albo wołaj przez wstrzyknięty bean.
    //   Ta sama zasada dotyczy @Transactional, @Async, @Cacheable (s10_transactions/Tx03ProxyPitfalls).
    // PUŁAPKA: czytanie POLA beana-proxy z zewnątrz (operation.attempts zamiast operation.attempts()). Dlaczego to błąd?
    //   Obiekt proxy CGLIB jest tworzony bez konstruktora — jego pola są puste (null), a prawdziwy stan ma obiekt
    //   docelowy. Sprawdzone testem proxyFieldIsEmpty: pole daje null, metoda daje 3.

    // =================================================================================================
    // 4. @ConcurrencyLimit — najwyżej N wywołań naraz (SupplierClient.stockReport, ExportJob)
    // =================================================================================================
    //
    // @ConcurrencyLimit(2) na metodzie: najwyżej 2 wątki naraz w tej metodzie tego beana. Polityka (policy, od 7.0.3):
    //   BLOCK (domyślna) — kolejne wywołanie CZEKA, aż zwolni się miejsce (test section4ConcurrencyLimit: trzeci wątek
    //     czeka, dostawca nigdy nie widzi więcej niż 2 zapytań naraz);
    //   REJECT — kolejne wywołanie od razu dostaje InvocationRejectedException (podklasa RejectedExecutionException):
    //     „Concurrency limit reached: 1 - not allowed to enter s13_rest_client.rc04_resilience.ExportJob.export”.
    // Adnotacja na KLASIE = jeden wspólny limit dla wszystkich jej metod; na metodzie — limit tylko tej metody.
    // Szczególnie przydatne z wątkami wirtualnymi: pula wątków przestaje być naturalnym ogranicznikiem, więc bez limitu
    // tysiąc żądań użytkowników = tysiąc równoczesnych zapytań do dostawcy.
    //
    // DOBRA PRAKTYKA: BLOCK, gdy zadanie i tak trzeba wykonać (raport), REJECT, gdy lepiej szybko odmówić niż czekać
    //   (drugi eksport w trakcie pierwszego). Dlaczego? BLOCK przy zawieszonym dostawcy trzyma czekające wątki — łącz go
    //   z limitem czasu klienta (Rc02), żeby kolejka nie rosła w nieskończoność.

    // =================================================================================================
    // 5. PONAWIANIE A IDEMPOTENTNOŚĆ — POST zamówienia (placeOrderUnsafe kontra placeOrder)
    // =================================================================================================
    //
    // Scenariusz (Rc04ResilienceServerTest, prawdziwy serwer): dostawca ZAPISAŁ zamówienie, ale odpowiedź się nie udała
    // (503, zerwane połączenie, nasz limit czasu). Z naszej strony to błąd → @Retryable wysyła POST jeszcze raz →
    // dostawca ma DWA zamówienia na SPO-001 (test unsafeOrderCreatesDuplicate).
    // Rozwiązanie: klucz idempotencji. Wołający tworzy go RAZ (ShopRestockController: UUID; w prawdziwym sklepie —
    // numer naszego zamówienia z bazy) i przekazuje do metody; każda próba wysyła nagłówek Idempotency-Key z tą samą
    // wartością, a dostawca rozpoznaje powtórkę i zwraca istniejące zamówienie (test idempotentOrderIsStoredOnce).
    // Nagłówek Idempotency-Key to popularna konwencja (m.in. w API płatności), a nie część podstawowego HTTP — dostawca
    // musi go obsługiwać. Gdy nie obsługuje: nie ponawiaj POST-a automatycznie, tylko sprawdź stan (GET) i zdecyduj.
    //
    // PUŁAPKA: @Retryable na POST bez klucza idempotencji. Dlaczego to błąd? Klient nie odróżni „nie dotarło” od
    //   „dotarło, ale odpowiedź zginęła” — ponowienie w drugim przypadku dubluje zamówienie, płatność, maila.
    // PUŁAPKA: klucz generowany WEWNĄTRZ ponawianej metody. Dlaczego to błąd? Każda próba wykona ciało metody od nowa
    //   i wyśle INNY klucz — dla dostawcy to różne zamówienia. Klucz musi powstać przed proxy, u wołającego.

    // =================================================================================================
    // 6. NASZE API I WIDOCZNOŚĆ PONOWIEŃ (ShopRestockController, RetryLogger)
    // =================================================================================================
    //
    // Kontroler woła supplierClient.price(...) — ponowienia są niewidoczne dla klienta naszego API (200 mimo dwóch 503).
    // Gdy próby się skończą, @ExceptionHandler zamienia wyjątek na 503 z ProblemDetail (jak w Rc02).
    // Ponowienia nie mogą być też niewidoczne dla NAS: RetryLogger słucha MethodRetryEvent i zapisuje każdą nieudaną
    // próbę (WARN) i koniec prób (ERROR). Częste ponowienia to sygnał, że dostawca ma kłopot — zanim całkiem padnie.
    //
    // DOBRA PRAKTYKA: ponawiaj w JEDNYM miejscu łańcucha (tu: klient dostawcy). Dlaczego? Gdy ponawia i klient, i serwis,
    //   i kontroler (po 3 razy), jedno zapytanie użytkownika zamienia się w 4 × 4 × 4 = 64 zapytania do dostawcy,
    //   który i tak już ma kłopoty.

    // =================================================================================================
    // 7. RetryTemplate — ponawianie w kodzie, bez adnotacji i proxy (org.springframework.core.retry)
    // =================================================================================================
    //
    //   RetryPolicy policy = RetryPolicy.builder().maxRetries(3).delay(Duration.ofMillis(200)).multiplier(2)
    //                                   .includes(HttpServerErrorException.class).build();
    //   RetryTemplate template = new RetryTemplate(policy);
    //   String result = template.execute(() -> client.call());   // Retryable<T> — lambda może rzucać wyjątki
    // execute(...) po wyczerpaniu prób rzuca RetryException (wyjątek SPRAWDZANY — checked): getRetryCount() (ile
    // ponowień), getExceptions() (wszystkie wyjątki po kolei), getCause() (ostatni), komunikat „Retry policy for
    // operation 'cena KSI-001' exhausted; aborting execution”. invoke(Supplier) — wygodniejszy: rzuca ostatni wyjątek
    // bez opakowania. RetryListener (setRetryListener) — powiadomienia o próbach, np. do logów i metryk.
    // Kiedy? Gdy ponawiasz fragment metody, decyzja zależy od danych albo nie masz beana/proxy (np. kod w main).
    //
    // Czego Spring Framework 7 NIE ma: wyłącznika awaryjnego (circuit breaker — „przestań dzwonić, gdy dostawca leży”)
    // i ogranicznika częstotliwości (rate limiter). Do tego służą biblioteki takie jak Resilience4j.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Włącz: @EnableResilientMethods na klasie @Configuration — Boot 4.1 nie robi tego sam.
     *   • @Retryable(includes = {HttpServerErrorException.class, ResourceAccessException.class}, maxRetries = 3,
     *     delay = 200, multiplier = 2, maxDelay = 2000) — najwyżej 1 + maxRetries wywołań; domyślnie: 3 ponowienia,
     *     1000 ms, KAŻDY wyjątek. Wersje …String przyjmują ${...}.
     *   • Po wyczerpaniu prób leci ostatni wyjątek metody; każda nieudana próba → MethodRetryEvent.
     *   • Działa tylko przez proxy: nie z tej samej klasy (this), nie na obiekcie z new, nie czytaj pól proxy.
     *   • @ConcurrencyLimit(2) — najwyżej 2 naraz; policy = REJECT → InvocationRejectedException od razu.
     *   • Ponawiaj tylko przejściowe błędy (5xx, sieć, limit czasu) i tylko operacje idempotentne (GET, PUT, DELETE...).
     *   • POST: klucz idempotencji (Idempotency-Key) tworzony RAZ przez wołającego, ten sam w każdej próbie.
     *   • RetryTemplate + RetryPolicy.builder(): execute → RetryException (checked), invoke → ostatni wyjątek.
     *   • Spring Retry (@EnableRetry, maxAttempts) to inna biblioteka: maxAttempts liczy wszystkie próby, maxRetries — ponowienia.
     *
     * PYTANIA KONTROLNE:
     *   1. Które z tych odpowiedzi dostawcy ponowisz: 503, 404, 400, przekroczony limit czasu, 502? Dlaczego?
     *   2. Co się stanie? Usuniesz @EnableResilientMethods z Rc04Application, a dostawca odpowie 503 przy pierwszej próbie price(...).
     *   3. Co się stanie? Wywołasz supplierClient.basketPrices(List.of("ELE-001")), a dostawca pierwszy raz odpowie 503.
     *   4. Ile NAJWYŻEJ razy wykona się metoda z @Retryable(maxRetries = 3)? A metoda z samym @Retryable — ile razy,
     *      z jakim odstępem i przy jakich wyjątkach?
     *   5. ZNAJDŹ BŁĄD: @Retryable(includes = HttpServerErrorException.class) public SupplierOrderConfirmation
     *      placeOrder(SupplierOrder order) { String key = UUID.randomUUID().toString(); ... .header("Idempotency-Key", key) ... }
     *   6. Czym różni się @ConcurrencyLimit z polityką BLOCK od REJECT? Kiedy którą wybrać?
     *   7. PYTANIE REKRUTACYJNE: dlaczego adnotacje oparte na proxy (@Retryable, @Transactional) nie działają przy
     *      wywołaniu metody z tej samej klasy?
     *   8. Czym różni się RetryTemplate.execute(...) od invoke(...) po wyczerpaniu prób?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Rc04ResilienceExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zbuduj RetryPolicy: najwyżej 2 ponowienia, bez opóźnienia, ponawiaj TYLKO
     * HttpServerErrorException (i jego podklasy). Test uruchomi ją w RetryTemplate: dla 503 oczekuje 3 prób, dla 404 — 1.
     * Podpowiedź: RetryPolicy.builder().maxRetries(...).delay(Duration.ZERO).includes(...).build().
     */
    static RetryPolicy exercise1() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): czy wolno AUTOMATYCZNIE ponowić zapytanie? Idempotentne metody HTTP (GET, HEAD, OPTIONS,
     * PUT, DELETE) — zawsze tak; POST i PATCH — tylko gdy zapytanie ma klucz idempotencji (hasIdempotencyKey).
     * Podpowiedź: HttpMethod to klasa ze stałymi (nie enum) — porównuj przez equals albo Set.of(...).contains(method).
     */
    static boolean exercise2(HttpMethod method, boolean hasIdempotencyKey) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): wykonaj call z ponawianiem (najwyżej maxRetries ponowień, bez opóźnienia, dowolny wyjątek)
     * i zwróć wynik. Po wyczerpaniu prób ma polecieć OSTATNI wyjątek z call — bez RetryException.
     * Podpowiedź: new RetryTemplate(RetryPolicy.builder()...build()).invoke(call).
     */
    static <T> T exercise3(Supplier<T> call, int maxRetries) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Bean do ĆWICZENIA 4 (średnie): wycena u dostawcy, który czasem rzuca IllegalStateException (awaria przejściowa).
     * Test rejestruje ten bean w kontekście z {@code @EnableResilientMethods}.
     */
    public static class ExerciseQuoteService {

        private final Supplier<String> supplierCall;

        public ExerciseQuoteService(Supplier<String> supplierCall) {
            this.supplierCall = supplierCall;
        }

        /**
         * ĆWICZENIE 4 (średnie): dodaj nad tą metodą adnotację tak, żeby przy IllegalStateException była ponawiana
         * najwyżej 2 razy (razem 3 próby), bez opóźnienia, a inne wyjątki (np. IllegalArgumentException) — nie.
         * Ciała nie zmieniaj. Bez adnotacji test padnie już na pierwszej awarii.
         * Podpowiedź: @Retryable z org.springframework.resilience.annotation — atrybuty includes, maxRetries, delay.
         */
        // TODO: dodaj adnotację nad metodą
        public String quote() {
            return supplierCall.get();
        }
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): wykonaj call przez RetryTemplate.execute (najwyżej maxRetries ponowień, bez opóźnienia).
     * Gdy się uda — zwróć pustą listę. Gdy próby się wyczerpią — zwróć komunikaty WSZYSTKICH wyjątków po kolei.
     * Przykład: 3 awarie przy maxRetries = 2 → ["awaria 1", "awaria 2", "awaria 3"].
     * Podpowiedź: execute rzuca RetryException (checked) — złap ją; getExceptions(), potem map(Throwable::getMessage).
     */
    static List<String> exercise5(Supplier<String> call, int maxRetries) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Ponawiamy 503, 502 i przekroczony limit czasu — to awarie przejściowe (dla GET; dla POST tylko z kluczem
     *      idempotencji). 404 i 400 — nie: produkt się nie pojawi, a złe dane nie staną się dobre od powtarzania.
     *   2. Żadnego ponowienia: bez @EnableResilientMethods nie powstaje proxy, adnotacja jest ignorowana bez ostrzeżenia.
     *      Pierwsze 503 od razu kończy się HttpServerErrorException.ServiceUnavailable u wołającego (to samo pokazuje
     *      test withoutEnableResilientMethods na FlakyOperation: jedna próba, wyjątek „awaria przejściowa nr 1”).
     *   3. HttpServerErrorException.ServiceUnavailable po JEDNYM zapytaniu: basketPrices woła this.price(...) z pominięciem
     *      proxy, więc @Retryable nie działa (test section3SelfInvocation).
     *   4. Najwyżej 4 razy (1 + 3 ponowienia). Samo @Retryable: maxRetries = 3 (do 4 wywołań), delay = 1000 ms,
     *      multiplier = 1.0 (stały odstęp) i ponawianie przy KAŻDYM wyjątku — dlatego zawsze podawaj includes.
     *   5. Klucz powstaje WEWNĄTRZ ponawianej metody — każda próba wykona ciało od nowa i wyśle nowy UUID. Dla dostawcy
     *      to różne zamówienia, więc ponowienie dalej dubluje zamówienie. Klucz musi przyjść od wołającego (parametr),
     *      jak w SupplierClient.placeOrder(order, idempotencyKey).
     *   6. BLOCK — nadmiarowe wywołanie czeka na wolne miejsce (dobre, gdy zadanie trzeba wykonać); REJECT — od razu
     *      InvocationRejectedException (dobre, gdy równoległe wykonanie nie ma sensu, np. drugi eksport w trakcie pierwszego).
     *   7. Spring dokłada zachowanie w obiekcie proxy, który stoi PRZED beanem. Wywołanie z zewnątrz idzie przez proxy,
     *      ale wewnątrz klasy this wskazuje na obiekt docelowy — wywołanie omija proxy i jego interceptory.
     *   8. execute(...) rzuca RetryException (checked) z historią: getRetryCount(), getExceptions(), getCause() = ostatni
     *      wyjątek. invoke(...) rzuca od razu ostatni wyjątek (RuntimeException bez opakowania) — wygodne w zwykłym kodzie.
     */
    // </editor-fold>
}
