package s10_transactions.tx03_proxy_pitfalls;

/**
 * <pre>
 * TEMAT: Pułapki proxy transakcyjnego — kiedy @Transactional po cichu NIE działa
 *        (proxy = pośrednik; self-invocation = wywołanie samego siebie; target = obiekt docelowy;
 *         final = ostateczny, „nie do nadpisania”; rollback-only = „tylko do wycofania”)
 *
 * W SKRÓCIE:
 *   @Transactional to nie magia wpisana w metodę, tylko PROXY — podklasa Twojego serwisu wygenerowana przez CGLIB,
 *   która „stoi przed” obiektem (Tx01Transactional, sekcja 6). Proxy otwiera transakcję tylko wtedy, gdy wywołanie
 *   PRZECHODZI PRZEZ NIE i gdy może NADPISAĆ metodę. Dlatego transakcji NIE ma przy: this.metoda() z tej samej klasy,
 *   metodzie private, metodzie final (a klasa final w ogóle nie wystartuje). Do tego proxy decyduje o commit/rollback
 *   tylko na podstawie wyjątku, który WYLATUJE z metody — wyjątek złapany w środku albo zamieniony na kontrolowany
 *   oznacza COMMIT. Kod się kompiluje, testy „szczęśliwej ścieżki” przechodzą, a dane psują się po cichu.
 *
 * ANALOGIA: sekretariat prezesa.
 *   Sekretarka (proxy) przed każdą rozmową z prezesem włącza nagrywanie (transakcję), a po rozmowie decyduje:
 *   „archiwizuj” (commit) albo „skasuj” (rollback) — patrząc WYŁĄCZNIE na minę rozmówcy, który wychodzi z gabinetu.
 *   Prezes, który dzwoni sam do siebie (this.metoda()), omija sekretariat — nic się nie nagrywa. Sprawy prywatne
 *   (private) w ogóle do sekretariatu nie trafiają. Drzwi zamurowane (final) — sekretarka nie wejdzie. A rozmówca,
 *   który ukrył problem i wyszedł uśmiechnięty (złapany wyjątek), dostaje „archiwizuj”.
 *
 * JAK TO DZIAŁA:
 *   test → stockService.restockViaThis("SPO-001", 10)
 *            [proxy StockService$$SpringCGLIB$$0] — restockViaThis nie ma @Transactional → bez transakcji, dalej:
 *            [obiekt StockService].restockViaThis(...)
 *               this.restock(...)       ← this = OBIEKT, nie proxy → @Transactional nad restock nikt nie czyta
 *                  findById            → transakcja repozytorium (tylko na czas odczytu), potem encja ODŁĄCZONA
 *                  item.add(10)        → zmiana w odłączonej encji — nikt jej nie zapisze
 *   Wynik: metoda „działa”, a dostawa znika. Sprawdzone testem sekcji 1.
 *
 * SŁÓWKA:
 *   self-invocation = wywołanie samego siebie; target object = obiekt docelowy; visible = widoczny;
 *   package-private = widoczny tylko w pakiecie (bez modyfikatora); subclass = podklasa; override = nadpisać;
 *   swallow an exception = połknąć wyjątek; aspect support = wsparcie aspektu; current = bieżący;
 *   lazy = leniwy (utworzony dopiero przy pierwszym użyciu); object provider = dostawca obiektu;
 *   warm-up = rozgrzewka; application ready = aplikacja gotowa; weaving = wplatanie (kodu do klas).
 *
 * ZOBACZ TEŻ: s10_transactions/Tx01Transactional (proxy CGLIB, rollbackFor),
 *             s10_transactions/Tx02Propagation (UnexpectedRollbackException — złapany wyjątek z INNEGO beana),
 *             s10_transactions/Tx05ProgrammaticTransactions (TransactionTemplate), s01_core_ioc/Ioc07Lifecycle
 *             (@PostConstruct), s18_aop (te same reguły proxy dla własnych aspektów),
 *             t19_annotations_reflection/Annotations06DynamicProxy (jak działa proxy).
 * </pre>
 */
public final class Tx03ProxyPitfalls {

    private Tx03ProxyPitfalls() {
    }

    // =================================================================================================
    // 0. JEDNA ZASADA, z której wynika cała lekcja
    // =================================================================================================
    //
    // Spring wstrzykuje innym beanom nie Twój obiekt, tylko PROXY: podklasę wygenerowaną przez CGLIB, która nadpisuje
    // metody i w nadpisanej wersji robi: „otwórz transakcję → wywołaj oryginał na obiekcie → commit albo rollback”.
    // Wynika z tego wprost:
    //   • transakcja jest tylko przy wywołaniu PRZEZ proxy — czyli z innego beana (sekcja 1);
    //   • proxy może przechwycić tylko metodę, którą umie NADPISAĆ — nie private i nie final (sekcja 2);
    //   • proxy widzi tylko to, co WYLATUJE z metody — nie to, co się w niej działo (sekcja 3).
    // Te same reguły dotyczą każdej adnotacji działającej przez proxy: @Cacheable, @Async, własne aspekty (s18_aop).

    // =================================================================================================
    // 1. this.metoda() — wywołanie samego siebie omija proxy (PYTANIE REKRUTACYJNE)
    // =================================================================================================
    //
    // public boolean restockViaThis(String sku, int pieces) {      // bez @Transactional
    //     return this.restock(sku, pieces);                       // restock MA @Transactional — i co z tego?
    // }
    //
    // Sprawdzone testem sekcji 1:
    //   • AopUtils.isAopProxy(stockService) → true (wstrzyknięty bean to proxy),
    //     ale wewnątrz metody AopUtils.isAopProxy(this) → false — this to „goły” obiekt;
    //   • stockService.restock("SPO-001", 10) z testu → transakcja, stan 120 → 130;
    //   • stockService.restockViaThis("SPO-001", 10) → isActualTransactionActive() = false, a stan ZOSTAJE 130.
    //     Encja z findById jest odłączona (transakcja repozytorium już się skończyła), więc zmiana pola nie trafia
    //     do bazy. Ani wyjątku, ani ostrzeżenia w logu.
    //
    // PUŁAPKA: „dodam @Transactional na metodę pomocniczą i zawołam ją z innej metody tej samej klasy”. Dlaczego to
    //   groźne? Adnotacja jest w kodzie, więc każdy czytelnik (i recenzent) zakłada, że transakcja jest. A jej nie ma:
    //   zmiany bez save() przepadają, a operacje z save() wykonują się w osobnych transakcjach (bez „wszystko albo nic”).

    // =================================================================================================
    // 2. Widoczność metody i final — co proxy CGLIB może nadpisać
    // =================================================================================================
    //
    //   metoda z @Transactional     | transakcja przy wywołaniu z innego beana? (Spring Framework 7, proxy CGLIB)
    //   ----------------------------+------------------------------------------------------------------------
    //   public                      | TAK
    //   protected                   | TAK (od Spring Framework 6.0 — wcześniej adnotacja była ignorowana)
    //   package-private             | TAK (od 6.0; wołać ją mogą tylko klasy z tego samego pakietu)
    //   private                     | NIE — po cichu ignorowana (prywatnej metody nie da się nadpisać)
    //   public final                | NIE — ostrzeżenie WARN przy starcie (niżej)
    //   cała klasa final            | aplikacja NIE STARTUJE
    // Sprawdzone testami sekcji 2 (test leży w tym samym pakiecie, więc może wołać protected i package-private).
    //
    // Metoda final: CGLIB nie może jej nadpisać, więc wywołanie trafia prosto do kodu z Twojej klasy — ale wykonuje
    // się na OBIEKCIE PROXY, nie na prawdziwym beanie. Obiekt proxy ma puste pola (Spring tworzy go bez konstruktora),
    // więc metoda final używająca pola kończy się tak (test):
    //   NullPointerException: Cannot invoke "s10_transactions.tx03_proxy_pitfalls.StockItemRepository.count()"
    //   because "this.stock" is null
    // Spring 7 przynajmniej ostrzega przy starcie (log WARN z CglibAopProxy — zobaczysz go w wyniku testów):
    //   Public final method [public final boolean s10_transactions.tx03_proxy_pitfalls.StockService.finalProbe()]
    //   cannot get proxied via CGLIB, consider removing the final marker or using interface-based JDK proxies.
    //
    // Klasa final jako bean z @Transactional (FinalPriceService zarejestrowana w teście) — start kończy się błędem:
    //   BeanCreationException: Error creating bean with name 'finalPriceService': Could not generate CGLIB subclass
    //   of class s10_transactions.tx03_proxy_pitfalls.FinalPriceService: Common causes of this problem include using
    //   a final class or a non-visible class
    //   (przyczyna na dnie: IllegalArgumentException: Cannot subclass final class ...FinalPriceService)
    //
    // DOBRA PRAKTYKA: @Transactional tylko na PUBLICZNYCH, nie-final metodach serwisu, wołanych z INNYCH beanów.
    //   Dlaczego? Wtedy zachowanie nie zależy od szczegółów proxy, a kod czyta się tak, jak działa. To, że Spring 6+
    //   obsługuje protected i package-private, jest przydatne, ale tylko przy proxy klasowym (CGLIB, domyślne w Boot);
    //   proxy interfejsowe (JDK) przechwytuje wyłącznie metody interfejsu.

    // =================================================================================================
    // 3. Wyjątki: proxy widzi tylko to, co WYLATUJE z metody (ReservationService)
    // =================================================================================================
    //
    // 3a. Wyjątek połknięty w środku — reserveIgnoringErrors(Kawa 2, Oliwa 1, Czekolada 5), Oliwy brak na stanie:
    //   try { take(...); } catch (IllegalStateException notEnough) { /* tylko logujemy */ }
    //   → metoda kończy się normalnie → COMMIT. Kawa i czekolada zdjęte ze stanu, oliwa nie — „pół zamówienia”.
    //
    // 3b. Złap niekontrolowany, rzuć KONTROLOWANY — reserveOrThrowChecked (ta sama lista):
    //   catch (IllegalStateException e) { throw new ReservationException("Rezerwacja nieudana: ...", e); }
    //   → wołający dostaje wyjątek, ALE ReservationException jest kontrolowany → domyślnie COMMIT (Tx01, sekcja 3).
    //   Kawa (przed błędem) zdjęta, czekolada nie (pętla do niej nie doszła). Sprawdzone testem sekcji 3.
    //
    // Naprawy (test sekcji 3):
    //   • rollbackFor = ReservationException.class — wyjątek kontrolowany też wycofuje;
    //   • chcesz ZWRÓCIĆ wynik (false) zamiast rzucać? Oznacz transakcję do wycofania:
    //       TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    //       return false;
    //     Metoda kończy się normalnie, a proxy robi ROLLBACK. W logu DEBUG: „Transactional code has requested
    //     rollback”. Uwaga: currentTransactionStatus() działa tylko w metodzie wywołanej PRZEZ proxy @Transactional;
    //     poza nią rzuca NoTransactionException: „No transaction aspect-managed TransactionStatus in scope” (test).
    //   • najprościej: nie łap — niech wyjątek wyleci, a obsłuż go WYŻEJ (np. w kontrolerze, s06_error_handling).
    // Gdy łapiesz wyjątek rzucony przez INNY bean transakcyjny (np. repozytorium), dochodzi jeszcze znacznik
    // rollback-only i UnexpectedRollbackException — to przypadek z Tx02Propagation, sekcja 3.
    //
    // PUŁAPKA: catch (Exception e) { log.warn(...); } w metodzie @Transactional. Dlaczego groźne? Wyjątek nie dociera
    //   do proxy, więc zatwierdzasz dokładnie ten niespójny stan, przed którym transakcja miała chronić.

    // =================================================================================================
    // 4. @PostConstruct i start aplikacji (StockWarmup)
    // =================================================================================================
    //
    // @PostConstruct void init() {
    //     transactionInPostConstruct = warmUp();                    // this.warmUp() — false
    //     viaOtherBean = restocker.restock("SPO-001", 0);           // INNY bean — true
    // }
    // @EventListener(ApplicationReadyEvent.class) void onReady() { … restocker.restock(…) … }   // true
    // Sprawdzone testem sekcji 4.
    //
    // Dlaczego this.warmUp() nie działa? To zwykłe wywołanie na this (sekcja 1). Do tego WŁASNE proxy beana powstaje
    // dopiero po metodach inicjalizacyjnych (BeanPostProcessor „po inicjalizacji” — s01_core_ioc/Ioc07Lifecycle), więc
    // w @PostConstruct nawet nie istnieje. Proxy INNYCH beanów są już gotowe (zależności wstrzyknięto w pełni
    // utworzone), dlatego restocker.restock(...) ma transakcję.
    //
    // DOBRA PRAKTYKA: pracę „przy starcie” na bazie (rozgrzewka, dane startowe) rób w metodzie wywoływanej po starcie:
    //   @EventListener(ApplicationReadyEvent.class) albo ApplicationRunner, i przez INNY bean transakcyjny.
    //   Dlaczego? Wtedy cały kontekst (ze wszystkimi proxy) jest gotowy i transakcje działają jak w każdym innym
    //   wywołaniu — nie musisz pamiętać, który bean jest już „owinięty”, a który jeszcze nie.

    // =================================================================================================
    // 5. Jak naprawić this.metoda() (StockService.restockVia…)
    // =================================================================================================
    //
    //   1. Przenieś metodę do INNEGO beana (StockRestocker) i wstrzyknij go. ← NAJLEPSZE: jasny podział, zero sztuczek.
    //   2a. Wstrzyknij „samego siebie” z @Lazy:  StockService(..., @Lazy StockService self, ...)
    //       @Lazy (leniwy) wstrzykuje pośrednika, który dopiero przy pierwszym wywołaniu pobiera z kontenera gotowy
    //       bean — czyli proxy. Bez @Lazy wstrzyknięcie samego siebie w konstruktorze kończy się błędem startu:
    //       „Requested bean is currently in creation: Is there an unresolvable circular reference or an asynchronous
    //       initialization dependency?” (sprawdzone uruchomieniem).
    //   2b. ObjectProvider<StockService> (dostawca obiektu) i selfProvider.getObject().restock(...) — to samo bez @Lazy.
    //   3. TransactionTemplate — transakcja otwierana w kodzie, bez proxy (Tx05ProgrammaticTransactions).
    //   4. Tryb AspectJ (@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)) — kod transakcji WPLATANY w klasę
    //      podczas kompilacji/ładowania, więc działa też this.metoda(). Wymaga dodatkowej konfiguracji; w kursie
    //      go nie używamy.
    // Naprawy 1–3 sprawdzone testem sekcji 5 (wszystkie dostawy zapisane, transakcja aktywna).
    //
    // DOBRA PRAKTYKA: gdy kusi Cię „ja sam przez proxy” (2a/2b), to zwykle znak, że klasa robi za dużo. Dlaczego?
    //   Dwie różne odpowiedzialności (np. „przyjmij dostawę” i „rozlicz partię dostaw”) w jednej klasie — wydzielenie
    //   jednej z nich do osobnego beana (naprawa 1) usuwa problem u źródła.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @Transactional działa przez proxy: tylko wywołania z INNEGO beana i tylko metody, które proxy może nadpisać.
     *   • this.metoda() → bez transakcji (zmiany bez save() przepadają po cichu).
     *   • private → ignorowane; protected/package-private → działa od Spring 6.0 (proxy CGLIB); final → nie działa
     *     (WARN przy starcie, NPE na polach); klasa final → aplikacja nie startuje.
     *   • Proxy decyduje po wyjątku, który WYLATUJE: złapany → COMMIT; zamieniony na kontrolowany → COMMIT.
     *   • Wynik zamiast wyjątku + rollback: TransactionAspectSupport.currentTransactionStatus().setRollbackOnly().
     *   • @PostConstruct: this.metoda() bez transakcji; pracę po starcie → @EventListener(ApplicationReadyEvent.class).
     *   • Naprawy: inny bean (najlepiej) · @Lazy self / ObjectProvider · TransactionTemplate · (AspectJ).
     *   • W testach: sonda isActualTransactionActive() + test BEZ transakcji testu + sprawdzenie stanu bazy.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego this.restock(...) wewnątrz StockService nie otwiera transakcji, choć restock ma @Transactional?
     *      (PYTANIE REKRUTACYJNE)
     *   2. Co się stanie? Stan SPO-001 to 120. Wywołujesz stockService.restockViaThis("SPO-001", 10) — co zwróci
     *      metoda i ile sztuk będzie w bazie?
     *   3. Co się stanie? Do klasy {@code public final class FinalPriceService} z metodą @Transactional dopisujesz
     *      {@code @Service} i uruchamiasz Tx03Application.
     *   4. ZNAJDŹ BŁĄD: {@code @Transactional public int reserve(Map<String, Integer> items) { for (...) { try {
     *      take(sku, n); } catch (IllegalStateException e) { log.warn("brak towaru"); } } return items.size(); }}
     *      — programista chciał „wszystko albo nic”.
     *   5. Co się stanie? reservations.reserveOrThrowChecked(Kawa 2, Oliwa 1, Czekolada 5) przy stanach 120/0/300 —
     *      czy poleci wyjątek i ile kawy zostanie w magazynie?
     *   6. Czym w Spring 7 (proxy CGLIB) różni się @Transactional na metodzie protected od tej na metodzie private?
     *   7. Jak zwrócić false z metody @Transactional i jednocześnie wycofać jej zmiany?
     *   8. Dlaczego w @PostConstruct wywołanie restocker.restock(...) ma transakcję, a this.warmUp() — nie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — klasa Tx03Exercises (ten pakiet), testy: Tx03ProxyPitfallsExercisesTest
    // =================================================================================================
    //
    //   ĆWICZENIE 1 (łatwe): Tx03Exercises.restockAll — kilka dostaw „wszystkie albo żadna”.
    //   ĆWICZENIE 2 (średnie): Tx03Exercises.reserveAllOrNothing — wynik false zamiast wyjątku, ale z wycofaniem.
    //   ĆWICZENIE 3 (trudniejsze): Tx03Exercises.restockEachSeparately — każda dostawa w osobnej transakcji, bez
    //     pułapki this.metoda().
    //   Rozwiązania: s10_transactions.solutions.tx03_proxy_pitfalls.Tx03ProxyPitfallsSolutions.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Transakcję otwiera proxy, a this wskazuje na obiekt docelowy, nie na proxy. Wywołanie this.restock(...)
     *      nie przechodzi przez proxy, więc nikt nie czyta adnotacji. Sprawdzone testem selfInvocationSkipsTheProxy.
     *   2. Zwróci false (brak transakcji), a w bazie zostanie 120 — encja po findById jest odłączona i zmiana nie
     *      trafia do bazy. Sprawdzone testem selfInvocationSkipsTheProxy.
     *   3. Aplikacja nie wystartuje: BeanCreationException „Could not generate CGLIB subclass of class
     *      ...FinalPriceService: Common causes of this problem include using a final class or a non-visible class”.
     *      Sprawdzone testem finalClassCannotBeProxied.
     *   4. Wyjątek jest łapany w środku, więc proxy widzi normalny powrót i ZATWIERDZA pozycje, które się udały
     *      („pół zamówienia”), a metoda zwraca liczbę wszystkich pozycji. Naprawa: nie łap (niech wyleci) albo w catch
     *      wywołaj TransactionAspectSupport.currentTransactionStatus().setRollbackOnly() i zwróć informację o błędzie.
     *      Sprawdzone testem caughtOrCheckedExceptionCommits (reserveIgnoringErrors).
     *   5. Poleci ReservationException („Rezerwacja nieudana: Za mało SPO-003: jest 0, potrzeba 1”), ale transakcja
     *      zostanie ZATWIERDZONA (wyjątek kontrolowany): kawy zostanie 118, czekolady 300 (pętla do niej nie doszła).
     *      Sprawdzone testem caughtOrCheckedExceptionCommits.
     *   6. protected: proxy CGLIB nadpisuje metodę i otwiera transakcję (od Spring 6.0). private: metody nie da się
     *      nadpisać, adnotacja jest po cichu ignorowana. Sprawdzone testem visibilityAndFinal.
     *   7. W catch: TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); return false; — proxy przy
     *      wyjściu zrobi ROLLBACK, a wołający dostanie false bez wyjątku. Sprawdzone testem fixesForExceptions.
     *   8. restocker to INNY, w pełni utworzony bean — wywołanie idzie przez jego gotowe proxy. this.warmUp() to
     *      wywołanie na obiekcie (omija proxy), a własne proxy StockWarmup powstaje dopiero po @PostConstruct.
     *      Sprawdzone testem postConstruct.
     */
    // </editor-fold>
}
