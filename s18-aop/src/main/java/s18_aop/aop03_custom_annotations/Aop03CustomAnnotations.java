package s18_aop.aop03_custom_annotations;

/**
 * <pre>
 * TEMAT: Własne adnotacje + aspekty — {@code @AuditLog}, {@code @MeasureTime}, kolejność aspektów i pułapki
 *        (annotation = adnotacja; retention = przechowywanie; order = kolejność; audit = audyt;
 *         swallow an exception = połknąć wyjątek)
 *
 * W SKRÓCIE:
 *   Najczytelniejszy punkt przecięcia to WŁASNA ADNOTACJA: stawiasz {@code @AuditLog(action = "ZMIANA_CENY")} nad
 *   metodą, a aspekt z {@code @Around("@annotation(auditLog)")} robi resztę — i ma dostęp do atrybutów adnotacji.
 *   Kod mówi wprost, które metody są audytowane, a aspekt nie łapie niczego „przy okazji”. Gdy na jednej metodzie
 *   działa kilka aspektów, kolejność ustalasz {@code @Order} na KLASIE aspektu (mniejsza liczba = bardziej na
 *   zewnątrz). Dwie największe pułapki: adnotacja bez {@code @Retention(RUNTIME)} (aspekt jej nie widzi) i aspekt,
 *   który łapie wyjątki i zwraca null (błędy znikają).
 *
 * ANALOGIA: naklejki na paczkach w magazynie.
 *   Magazynier (metoda) pakuje paczki. Na niektórych ktoś nakleja naklejkę „OSTROŻNIE — szkło, kategoria 2”
 *   (adnotacja z atrybutem). Taśma (proxy) z czujnikiem (aspekt) czyta naklejki i robi swoje: waży, fotografuje,
 *   zapisuje w rejestrze. Naklejka musi być na WIERZCHU paczki (RUNTIME) — schowana w środku (CLASS) nie zadziała.
 *   Naklejka na katalogu wzorów (interfejsie) nie przykleja się sama do każdej paczki. Kolejność stanowisk na taśmie
 *   (@Order) decyduje, co jest pierwsze: waga czy rejestr.
 *
 * JAK TO DZIAŁA:
 *   {@code @AuditLog(action = "NOWE_ZAMOWIENIE") @MeasureTime(warnAboveMillis = 100)}
 *   public String placeOrder(String sku, int quantity)
 *     → proxy OrderService → łańcuch porad posortowany po @Order klas aspektów:
 *         AuditAspect (@Order(1))        „AUDYT start NOWE_ZAMOWIENIE [SPO-001, 2]”
 *           MeasureTimeAspect (@Order(2))  start = clock.millis()
 *             placeOrder(...)               „METODA placeOrder(SPO-001, 2)”
 *           MeasureTimeAspect             „POMIAR placeOrder: 40 ms (OK, próg 100 ms)”
 *         AuditAspect                     „AUDYT OK NOWE_ZAMOWIENIE”
 *   Atrybuty (action, warnAboveMillis) porada dostaje jako parametr: {@code AuditLog auditLog} → {@code auditLog.action()}.
 *
 * SŁÓWKA:
 *   annotation = adnotacja; attribute = atrybut; target = cel (gdzie wolno postawić adnotację);
 *   retention = przechowywanie (jak długo adnotacja „żyje”); RUNTIME = w czasie działania; documented = udokumentowana;
 *   audit log = dziennik audytu; measure = mierzyć; threshold = próg; order = kolejność; precedence = pierwszeństwo;
 *   swallow = połknąć; retry = ponowić; role = rola; permission = uprawnienie.
 *
 * ZOBACZ TEŻ: s18_aop/Aop01Aspects (porady, {@code @annotation} z pełną nazwą),
 *             s18_aop/Aop02Proxies (dlaczego this.metoda() omija aspekt),
 *             s10_transactions/Tx01Transactional i s10_transactions/Tx03ProxyPitfalls (transakcje — też aspekt),
 *             s13_rest_client/Rc04Resilience (wbudowane {@code @Retryable} ze Spring Framework 7),
 *             s15_actuator_logging/Obs03Metrics (pomiar czasu „na produkcji”: Micrometer),
 *             s12_security/Sec02Authorization (uprawnienia na metodach: {@code @PreAuthorize}),
 *             t19_annotations_reflection/Annotations02Custom (własne adnotacje w czystej Javie).
 * </pre>
 */
public final class Aop03CustomAnnotations {

    private Aop03CustomAnnotations() {
    }

    // =================================================================================================
    // 1. WŁASNA ADNOTACJA jako punkt przecięcia (AuditLog + AuditAspect)
    // =================================================================================================
    //
    //   @Target(ElementType.METHOD)            ← gdzie wolno ją postawić (tu: tylko na metodzie)
    //   @Retention(RetentionPolicy.RUNTIME)    ← MUSI być RUNTIME, żeby aspekt ją zobaczył
    //   public @interface AuditLog {
    //       String action();                   ← atrybut obowiązkowy (bez default)
    //   }
    //
    //   @Around("@annotation(auditLog)")                                   ← mała litera = NAZWA PARAMETRU
    //   public Object audit(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
    //       journal.add("AUDYT start " + auditLog.action() + ...);         ← atrybut adnotacji
    //       ...
    //   }
    //
    // Jak Spring to czyta: w wyrażeniu jest nazwa parametru „auditLog”, więc typ adnotacji bierze z TYPU parametru
    // porady (AuditLog). Ten typ rozwiązuje zwykła Java — więc tu import działa (rozwiązanie ćwiczenia 1 łapie
    // adnotację z innego pakietu). W formie BEZ parametru wpisujesz pełną nazwę typu w tekście:
    // {@code @annotation(s18_aop.aop03_custom_annotations.SafeCall)} — wtedy import nie pomaga (s18_aop/Aop01Aspects).
    //
    // Wynik z testu: priceService.changePrice("SPO-001", 59.99) →
    //   AUDYT start ZMIANA_CENY [SPO-001, 59.99] / METODA changePrice(SPO-001, 59.99) / AUDYT OK ZMIANA_CENY
    //
    // DOBRA PRAKTYKA: własna adnotacja zamiast wyrażenia „po nazwach” (execution(* *..*Service.save*(..))).
    //   Dlaczego? Patrząc na metodę, widzisz, że jest audytowana; zmiana nazwy metody niczego nie psuje; a aspekt nie
    //   złapie przypadkiem cudzych beanów — adnotacji używasz tylko Ty (por. pułapka z s18_aop/Aop01Aspects, sekcja 5).
    //
    // PUŁAPKA: adnotacja bez @Retention(RetentionPolicy.RUNTIME) (BrokenAuditLog na previewPrice). Dlaczego groźne?
    //   Domyślne przechowywanie to CLASS: adnotacja jest w pliku .class, ale działający program jej nie widzi.
    //   Porada {@code @Before("@annotation(brokenAuditLog)")} nie wykonuje się NIGDY — bez błędu i bez ostrzeżenia.
    //   (Ciekawostka z uruchomienia: wyrażenie bez parametru, z pełną nazwą, potrafi taką adnotację znaleźć, bo AspectJ
    //   czyta plik .class — ale atrybutów i tak nie odczytasz. Nie polegaj na tym: zawsze RUNTIME.)
    //
    // PUŁAPKA: adnotacja na metodzie INTERFEJSU (PriceArchive.archive). Dlaczego groźne? Adnotacje metod nie są
    //   dziedziczone przez metody klasy, a @annotation(...) w Spring AOP patrzy na metodę KLASY (PriceArchiveService).
    //   Audytu nie ma, a bean nawet nie dostaje proxy (test: AopUtils.isAopProxy = false). Stawiaj adnotację na klasie
    //   implementującej. (Uwaga: @Transactional Spring 7 znajduje także na metodzie interfejsu — sprawdzone
    //   uruchomieniem, proxy CGLIB — ale własny aspekt z @annotation tego nie robi. Nie przenoś przyzwyczajeń.)
    //
    // Test: Aop03CustomAnnotationsTest#auditReadsAnnotationAttribute, #invisibleAnnotations

    // =================================================================================================
    // 2. ADNOTACJA NA KLASIE i atrybut z wartością domyślną (MeasureTime + MeasureTimeAspect)
    // =================================================================================================
    //
    //   @Target({ElementType.METHOD, ElementType.TYPE})  ← metoda albo klasa
    //   public @interface MeasureTime { long warnAboveMillis() default 500; }
    //
    //   @annotation(measureTime) — adnotacja na METODZIE (nie widzi adnotacji klasy!)
    //   @within(measureTime)     — adnotacja na KLASIE: wszystkie metody tej klasy
    //   @within(measureTime) && !@annotation(s18_aop.aop03_custom_annotations.MeasureTime)
    //                            — klasa, ale bez metod z własną adnotacją (żeby nie mierzyć dwa razy)
    //
    // ReportService: @MeasureTime(warnAboveMillis = 1000) na klasie, a monthlyReport ma własne
    // @MeasureTime(warnAboveMillis = 10). Wynik z testu (zegar „pokazuje” 1000, 1050, 2000, 2050):
    //   POMIAR dailyReport: 50 ms (OK, próg 1000 ms)       ← próg z klasy
    //   POMIAR monthlyReport: 50 ms (WOLNO, próg 10 ms)    ← próg z metody wygrywa, jeden pomiar
    //
    // DOBRA PRAKTYKA: czas bierz z wstrzykniętego Clock (zegar), nie z System.currentTimeMillis().
    //   Dlaczego? Test podmienia zegar ({@code @MockitoBean Clock clock} + given(clock.millis()).willReturn(...)) i dostaje
    //   zawsze ten sam wynik — żadnych „czasem 39 ms, czasem 41 ms” ani usypiania wątku w teście.
    //   Do prawdziwych pomiarów na produkcji użyj metryk Micrometer (s15_actuator_logging/Obs03Metrics) — ten aspekt to
    //   model do nauki.
    //
    // Test: Aop03CustomAnnotationsTest#measureTimeOnClassAndMethod

    // =================================================================================================
    // 3. KOLEJNOŚĆ kilku aspektów (@Order) i kolejność względem transakcji
    // =================================================================================================
    //
    // Na placeOrder działają dwa aspekty. O kolejności decyduje @Order na KLASIE aspektu (albo interfejs Ordered):
    //   mniejsza liczba = wyższy priorytet = bardziej NA ZEWNĄTRZ: zaczyna pierwszy, kończy ostatni.
    //   AuditAspect @Order(1) → MeasureTimeAspect @Order(2) → metoda → MeasureTimeAspect → AuditAspect
    // Dlatego pomiar obejmuje samą metodę (bez audytu), a audyt widzi ostateczny wynik — także błąd. Wyniki z testów:
    //   sukces: AUDYT start ... / METODA placeOrder(SPO-001, 2) / POMIAR placeOrder: 40 ms (OK, próg 100 ms) / AUDYT OK ...
    //   błąd:   AUDYT start ... / METODA placeOrder(ELE-002, 1) / POMIAR placeOrder: 150 ms (WOLNO, próg 100 ms) /
    //           AUDYT BŁĄD NOWE_ZAMOWIENIE: Brak towaru: ELE-002 (stan 0, zamówiono 1)  — a wyjątek leci do wołającego
    // Bez @Order oba aspekty mają ten sam, najniższy priorytet (Ordered.LOWEST_PRECEDENCE) i kolejność jest
    // NIEOKREŚLONA — dziś może być dobra, po dodaniu klasy albo aktualizacji — inna.
    //
    // PUŁAPKA: @Order na METODZIE porady (OrderOnMethodAspect: @Order(-100) nad @Before). Dlaczego groźne? Spring
    //   bierze kolejność tylko z KLASY aspektu — adnotacja na metodzie jest ignorowana bez ostrzeżenia. Test: porada
    //   „z -100” biegnie PO AuditAspect z @Order(1). Chcesz różnej kolejności dla porad? Rozdziel je na osobne aspekty.
    //
    // Kolejność względem @Transactional (sprawdzone uruchomieniem z modułem transakcji Spring Boot 4.1.1; w tym dziale
    // nie ma bazy, więc bez testu — transakcje: s10_transactions/Tx01Transactional):
    //   • porada transakcji ma w Boot priorytet Ordered.LOWEST_PRECEDENCE (Integer.MAX_VALUE) — NAJNIŻSZY;
    //   • aspekt z @Order(1) jest więc NA ZEWNĄTRZ transakcji. Zapis z uruchomienia:
    //       order1 before tx=false, TX begin, METHOD, TX commit, order1 after
    //     — jego kod „przed” biegnie, zanim transakcja się zacznie, a „po” — gdy jest już zatwierdzona/wycofana;
    //   • aspekt BEZ @Order ma ten sam priorytet co transakcja → kolejność nieokreślona;
    //   • chcesz aspekt WEWNĄTRZ transakcji? Nadaj transakcji wyższy priorytet: @EnableTransactionManagement(order = 3)
    //     i aspekt z większą liczbą, np. @Order(5) (sprawdzone: TX begin → aspekt → metoda → aspekt → TX commit).
    //   Kiedy co? Ponawianie (retry) — NA ZEWNĄTRZ: każda próba dostaje nową, czystą transakcję (wewnątrz wszystkie
    //   próby dzieliłyby jedną transakcję razem ze zmianami z nieudanych prób). Pomiar „całości z commitem” — na
    //   zewnątrz. Zapis do bazy, który ma się wycofać razem z operacją — wewnątrz.
    //
    // Test: Aop03CustomAnnotationsTest#orderOfAspects, #orderOfAspectsOnError, #orderOnAdviceMethodIsIgnored

    // =================================================================================================
    // 4. PUŁAPKA: aspekt, który połyka wyjątki (SafeCall + SafeCallAspect)
    // =================================================================================================
    //
    //   @Around("@annotation(s18_aop.aop03_custom_annotations.SafeCall)")
    //   public Object swallow(ProceedingJoinPoint joinPoint) {
    //       try { return joinPoint.proceed(); }
    //       catch (Throwable error) { journal.add("POŁKNIĘTY ..."); return null; }      ← wyjątek znika
    //   }
    //
    // Wynik z testu: orderService.placeOrderSafely("ELE-002", 1) zwraca null (zamiast „Brak towaru…”), w dzienniku
    // jest „POŁKNIĘTY IllegalStateException: Brak towaru: ELE-002 (stan 0, zamówiono 1)”, a kod, który użyje wyniku
    // (result.length()), dostaje NullPointerException — daleko od prawdziwej przyczyny.
    //
    // PUŁAPKA: catch (Throwable) + return null w aspekcie. Dlaczego groźne? (1) Wołający myśli, że się udało, albo
    //   wywraca się w przypadkowym miejscu. (2) Dla typu prostego (int, boolean) zamiast błędu dostaniesz
    //   AopInvocationException (s18_aop/Aop01Aspects, sekcja 4). (3) Z transakcjami (sprawdzone uruchomieniem):
    //     aspekt NA ZEWNĄTRZ transakcji: TX begin, METHOD fail, TX rollback, swallowed — dane wycofane, a wołający
    //       dostaje null i nie wie, że nic się nie zapisało;
    //     aspekt WEWNĄTRZ transakcji: TX begin, METHOD fail, swallowed, TX commit — transakcja nie widzi wyjątku
    //       i ZATWIERDZA połowiczne zmiany (to samo, co złapany wyjątek w s10_transactions/Tx03ProxyPitfalls).
    //
    // DOBRA PRAKTYKA: aspekt może wyjątek zapisać, policzyć, przetłumaczyć na inny — ale ma go RZUCIĆ DALEJ.
    //   Dlaczego? O tym, co zrobić z błędem, decyduje kod biznesowy albo warstwa obsługi błędów
    //   (s06_error_handling/Err03ControllerAdvice), nie „niewidzialny” aspekt. Wzory: AuditAspect (catch → zapis →
    //   throw error) albo @AfterThrowing, który w ogóle nie potrafi połknąć wyjątku.
    //
    // Test: Aop03CustomAnnotationsTest#swallowingAspectHidesErrors (poprawny wzór: #orderOfAspectsOnError)

    // =================================================================================================
    // 5. KIEDY ASPEKT TO PRZESADA
    // =================================================================================================
    //
    // Aspekt ma koszt: kod „dzieje się” w miejscu, którego nie widać w metodzie; ślad stosu (stack trace) jest dłuższy
    // o ramki proxy; działa tylko przez proxy (s18_aop/Aop02Proxies); kolejność kilku aspektów trzeba pilnować.
    //
    //   aspekt ma sens, gdy …                          | jawne wywołanie jest lepsze, gdy …
    //   -----------------------------------------------+-----------------------------------------------------------
    //   sprawa TECHNICZNA (log, pomiar, audyt, retry)  | to logika BIZNESOWA (rabat, zmiana statusu, walidacja reguł)
    //   dotyczy wielu metod (kilkanaście i więcej)     | dotyczy jednej–dwóch metod
    //   nie zmienia wyniku ani argumentów             | zmienia wynik, argumenty albo przepływ sterowania
    //   jest gotowy w Springu? → użyj gotowego         | wymaga „sztuczek” (AopContext, this przez proxy)
    //
    // Najpierw sięgnij po GOTOWE aspekty Springa: @Transactional (s10_transactions/Tx01Transactional), @Cacheable
    // (s16_cache_scheduling_async/Cache01Cacheable), @Async (s16_cache_scheduling_async/Async04Async), @Retryable
    // (Spring Framework 7, s13_rest_client/Rc04Resilience), @PreAuthorize (s12_security/Sec02Authorization).
    // Własny aspekt pisz dla potrzeb, których nie ma w bibliotekach — jak audyt z {@code @AuditLog}.
    //
    // DOBRA PRAKTYKA: zanim napiszesz aspekt, zapytaj: „czy nowa osoba w zespole zrozumie, co robi ta metoda, czytając
    //   tylko jej kod?”. Dlaczego? Jeśli odpowiedź brzmi „nie, bo połowa zachowania jest w aspekcie” — a to zachowanie
    //   jest biznesowe — wywołaj je jawnie. Przewidywalny kod jest wart więcej niż kilka zaoszczędzonych linii.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Własna adnotacja: @Target (gdzie), @Retention(RUNTIME) (ZAWSZE), atrybuty z default albo bez (obowiązkowe).
     *   • Porada z atrybutami: {@code @Around("@annotation(auditLog)") Object audit(ProceedingJoinPoint jp, AuditLog auditLog)}.
     *   • @annotation = adnotacja na METODZIE klasy; @within = adnotacja na KLASIE; metody interfejsu się nie liczą.
     *   • Kolejność aspektów: @Order na KLASIE aspektu (mniejsza liczba = na zewnątrz). @Order na metodzie porady — ignorowane.
     *     Bez @Order — kolejność nieokreślona.
     *   • Transakcja w Boot ma najniższy priorytet: aspekt z @Order(n) działa NA ZEWNĄTRZ transakcji.
     *   • Aspekt nie połyka wyjątków: zapisz → throw. Połknięty wyjątek = null u wołającego albo COMMIT połowicznych zmian.
     *   • Czas w aspekcie z wstrzykniętego Clock — test podmienia zegar i ma stały wynik.
     *   • Aspekt dla spraw technicznych w wielu miejscach; logika biznesowa — jawnie. Najpierw gotowe aspekty Springa.
     *
     * PYTANIA KONTROLNE:
     *   1. Po co własna adnotacja, skoro to samo da się wybrać wyrażeniem execution(...)? Podaj dwie zalety.
     *   2. Co się stanie? Adnotacja {@code @Target(ElementType.METHOD) public @interface Audited { String value(); }}
     *      (bez @Retention) i porada {@code @Before("@annotation(audited)") void log(Audited audited)}.
     *      Wywołujesz metodę oznaczoną {@code @Audited("X")}.
     *   3. ZNAJDŹ BŁĄD: {@code @AuditLog} stoi na metodzie w interfejsie PriceArchive, a klasa PriceArchiveService tylko ją
     *      implementuje. Audytu nie ma. Dlaczego i jak to naprawić?
     *   4. Co się stanie? AuditAspect ma @Order(1), MeasureTimeAspect — @Order(2). W jakiej kolejności pojawią się wpisy
     *      dla placeOrder("SPO-001", 2)? Co się zmieni po zamianie liczb miejscami?
     *   5. ZNAJDŹ BŁĄD: autor chce, żeby porada biegła przed wszystkimi innymi, więc pisze {@code @Order(-100)} nad metodą
     *      {@code @Before} w aspekcie bez {@code @Order} na klasie. Dlaczego to nie działa?
     *   6. Aspekt ponawiający (retry) ma działać na metodzie z @Transactional. Czy powinien być na zewnątrz, czy wewnątrz
     *      transakcji? Jak to ustawić w Boot?
     *   7. Co się stanie? Aspekt łapie każdy wyjątek i zwraca null, a działa WEWNĄTRZ transakcji. Metoda zapisała dwa
     *      wiersze i rzuciła wyjątek przy trzecim.
     *   8. Podaj dwie sytuacje, w których jawne wywołanie jest lepsze niż aspekt.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Aop03CustomAnnotationsExercisesTest (bez Springa: AspectJProxyFactory)
    // =================================================================================================

    /*
     * ĆWICZENIE 1 (łatwe): otwórz Aop03Exercise1RoleAspect. Porada {@code @Before("@annotation(requiresRole)")} ma sprawdzić
     * rolę bieżącego użytkownika (CurrentUser) z rolą z adnotacji {@code @RequiresRole} (DiscountAdmin.grantDiscount).
     * Gdy się różnią — rzuć PermissionDeniedException z komunikatem
     * „Brak uprawnień: wymagana rola KIEROWNIK, a użytkownik ma rolę KLIENT”. Podpowiedź: requiresRole.value()
     * i currentUser.role(); porównuj equals, nie ==. Wyjątek z @Before zatrzymuje wywołanie metody.
     *
     * ĆWICZENIE 2 (średnie): otwórz Aop03Exercise2ErrorLogAspect — napisz POPRAWNĄ wersję SafeCallAspect.
     * Po sukcesie dopisz „OK placeOrder” i zwróć wynik; po wyjątku dopisz „BŁĄD placeOrder: IllegalStateException”
     * i rzuć DALEJ ten sam wyjątek. Podpowiedź: try { Object result = joinPoint.proceed(); ... return result; }
     * catch (Throwable error) { ...; throw error; } i error.getClass().getSimpleName().
     *
     * ĆWICZENIE 3 (trudniejsze): otwórz Aop03Exercise3RetryAspect i napisz ponawianie dla {@code @RetryOnFailure(attempts = 3)}:
     * najwyżej attempts wywołań, ponawiaj TYLKO po TemporaryFailureException, inne wyjątki rzucaj od razu, po ostatniej
     * nieudanej próbie rzuć ostatni TemporaryFailureException. Podpowiedź: pętla for z try/catch wokół
     * joinPoint.proceed() — w Spring AOP proceed() wolno wywołać kilka razy. (W prawdziwym projekcie: {@code @Retryable}
     * ze Spring Framework 7 — s13_rest_client/Rc04Resilience.)
     */

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Adnotacja nad metodą od razu mówi czytelnikowi, że metoda jest audytowana/mierzona; zmiana nazwy metody albo
     *      pakietu niczego nie psuje; aspekt nie złapie przypadkiem cudzych beanów; adnotacja niesie ustawienia
     *      (action, warnAboveMillis), które porada odczytuje.
     *   2. Nic — porada się nie wykona, bez błędu i bez ostrzeżenia. Bez @Retention przechowywanie to CLASS, więc w czasie
     *      działania adnotacji „nie ma”. Sprawdzone testem Aop03CustomAnnotationsTest#invisibleAnnotations (BrokenAuditLog).
     *      Naprawa: {@code @Retention(RetentionPolicy.RUNTIME)}.
     *   3. Adnotacje metod nie dziedziczą się z interfejsu, a {@code @annotation} patrzy na metodę klasy. Naprawa: postawić
     *      {@code @AuditLog} na metodzie w PriceArchiveService. Test: #invisibleAnnotations (bean nawet nie ma proxy).
     *   4. AUDYT start NOWE_ZAMOWIENIE [SPO-001, 2] → METODA placeOrder(SPO-001, 2) → POMIAR placeOrder: ... →
     *      AUDYT OK NOWE_ZAMOWIENIE (test #orderOfAspects). Po zamianie liczb pomiar jest na zewnątrz i kończy się
     *      OSTATNI: AUDYT start → METODA → AUDYT OK → POMIAR placeOrder (sprawdzone uruchomieniem), a zmierzony czas
     *      obejmuje też pracę aspektu audytu.
     *   5. Spring ustala kolejność aspektów tylko na podstawie @Order na KLASIE (albo Ordered); @Order na metodzie porady
     *      jest ignorowany. Test #orderOnAdviceMethodIsIgnored: porada „-100” biegnie po aspekcie z @Order(1).
     *      Naprawa: osobny aspekt z @Order(-100) na klasie.
     *   6. Na zewnątrz: każda próba dostaje nową, czystą transakcję, a zmiany nieudanej próby są wycofane. W Boot porada
     *      transakcji ma najniższy priorytet (Integer.MAX_VALUE), więc wystarczy aspekt z @Order, np. @Order(1)
     *      (sprawdzone uruchomieniem: order1 before tx=false, TX begin, ..., TX commit, order1 after).
     *   7. Transakcja nie zobaczy wyjątku i zrobi COMMIT — dwa wiersze zostaną zapisane mimo błędu (sprawdzone
     *      uruchomieniem: TX begin, METHOD fail, swallowed, TX commit). Wołający dostanie null.
     *   8. Np.: logika biznesowa (naliczenie rabatu, zmiana statusu zamówienia); zachowanie potrzebne w jednej–dwóch
     *      metodach; gdy aspekt musiałby zmieniać argumenty/wynik albo wymagałby AopContext.
     */
    // </editor-fold>
}
