package s10_transactions.tx02_propagation;

/**
 * <pre>
 * TEMAT: Propagacja transakcji — co się dzieje, gdy metoda transakcyjna woła inną metodę transakcyjną
 *        (propagation = propagacja, rozprzestrzenianie się; outer/inner transaction = transakcja zewnętrzna/wewnętrzna;
 *         suspend = wstrzymaj; join = dołącz; savepoint = punkt zapisu; rollback-only = „tylko do wycofania”)
 *
 * W SKRÓCIE:
 *   OrderService.placeOrder (z @Transactional) woła AuditService.log (też z @Transactional). Czy to jedna transakcja,
 *   czy dwie? Decyduje atrybut propagation metody WOŁANEJ. Domyślny REQUIRED: „dołącz do istniejącej, a jak nie ma —
 *   utwórz”. Wtedy obie metody dzielą jedną transakcję i jeden los: rollback jednej = rollback obu. REQUIRES_NEW:
 *   „zawsze własna” — np. dziennik audytu, który ma przetrwać nieudane zamówienie. Pozostałe wartości (MANDATORY,
 *   NEVER, SUPPORTS, NOT_SUPPORTED, NESTED) to kontrakty i przypadki szczególne.
 *
 * ANALOGIA: rachunek w restauracji.
 *   REQUIRED — dosiadasz się do stolika znajomych i dopisujesz do ICH rachunku: jeśli ktoś go nie zapłaci, nie
 *   zapłacony jest cały rachunek, także Twoja kawa. REQUIRES_NEW — prosisz o OSOBNY rachunek: płacisz swój niezależnie
 *   od tego, co zrobi reszta stolika. MANDATORY — „obsługujemy tylko gości przy stoliku z otwartym rachunkiem”.
 *   NEVER — „przy barze, bez rachunku; z rachunkiem nie wolno”.
 *
 * JAK TO DZIAŁA:
 *   orderService.placeOrderThenFail(...)                     [proxy] → JpaTransactionManager: NOWA transakcja T1
 *     orders.save(...)                                       → INSERT w T1 (niezatwierdzony)
 *     audit.logRequiresNew(...)                              [proxy] → REQUIRES_NEW:
 *        1. T1 WSTRZYMANA (odpięta od wątku), 2. NOWA transakcja T2 na DRUGIM połączeniu z puli,
 *        3. INSERT do audit_log w T2, 4. COMMIT T2, 5. T1 przypięta z powrotem
 *     throw IllegalStateException                            → ROLLBACK T1 (zamówienie znika, audyt zostaje)
 *   Przy REQUIRED punkt „audit” nie tworzy T2 — działa w T1, więc ROLLBACK T1 zabiera też wpis audytu.
 *   W logu (profil tx02, DEBUG JpaTransactionManager) widać: „Participating in existing transaction” (REQUIRED)
 *   i „Suspending current transaction, creating new transaction” (REQUIRES_NEW).
 *
 * SŁÓWKA:
 *   required = wymagana; requires new = wymaga nowej; mandatory = obowiązkowa; never = nigdy; supports = wspiera;
 *   not supported = nie wspiera; nested = zagnieżdżona; participate = uczestniczyć; existing = istniejąca;
 *   unexpected = nieoczekiwany; silently = po cichu; marked as = oznaczona jako; illegal state = niedozwolony stan;
 *   audit log = dziennik audytu.
 *
 * ZOBACZ TEŻ: s10_transactions/Tx01Transactional (podstawy, rollback), s10_transactions/Tx03ProxyPitfalls (dlaczego
 *             propagacja działa tylko między RÓŻNYMI beanami), s10_transactions/Tx05ProgrammaticTransactions
 *             (propagacja w TransactionTemplate), t29_jdbc_databases/Jdbc04Transactions (savepoint w JDBC).
 * </pre>
 */
public final class Tx02Propagation {

    private Tx02Propagation() {
    }

    // =================================================================================================
    // 0. SIEDEM WARTOŚCI propagation — ściągawka (pytanie: „jest transakcja wołającego?”)
    // =================================================================================================
    //
    //   propagation      | wołający MA transakcję                 | wołający NIE MA transakcji
    //   -----------------+----------------------------------------+-----------------------------------
    //   REQUIRED (dom.)  | dołącz do niej                          | utwórz nową
    //   REQUIRES_NEW     | wstrzymaj ją, utwórz NOWĄ               | utwórz nową
    //   MANDATORY        | dołącz do niej                          | IllegalTransactionStateException
    //   NEVER            | IllegalTransactionStateException        | działaj bez transakcji
    //   SUPPORTS         | dołącz do niej                          | działaj bez transakcji
    //   NOT_SUPPORTED    | wstrzymaj ją, działaj BEZ transakcji    | działaj bez transakcji
    //   NESTED           | punkt zapisu w niej (JDBC); JPA → błąd  | utwórz nową (jak REQUIRED)
    //
    // Wszystkie wiersze sprawdzone testami Tx02PropagationTest. Propagacja dotyczy metody WOŁANEJ i działa tylko,
    // gdy wywołanie idzie przez proxy (z innego beana) — dlatego audyt jest w osobnej klasie AuditService.

    // =================================================================================================
    // 1. REQUIRED — dołącz albo utwórz (domyślna)
    // =================================================================================================
    //
    // @Transactional                                    // to samo co propagation = Propagation.REQUIRED
    // public void logRequired(String message) { auditLog.save(new AuditEntry(message)); }
    //
    // Wołana z OrderService (który ma transakcję) — DOŁĄCZA: TransactionSynchronizationManager
    // .getCurrentTransactionName() w audycie zwraca nazwę transakcji zamówienia ("...OrderService.transactionNames").
    // Jeden los: rollback zamówienia wycofuje też wpis audytu. Sprawdzone testem sekcji 1.
    //
    // DOBRA PRAKTYKA: w 95% przypadków zostaw domyślne REQUIRED. Dlaczego? Jedna operacja biznesowa = jedna
    //   transakcja: wszystkie serwisy, które biorą w niej udział, mają wspólny commit albo wspólny rollback.

    // =================================================================================================
    // 2. REQUIRES_NEW — zawsze własna transakcja (dziennik audytu)
    // =================================================================================================
    //
    // @Transactional(propagation = Propagation.REQUIRES_NEW)
    // public void logRequiresNew(String message) { ... }
    //
    // Sprawdzone testem sekcji 2:
    //   • zamówienie wycofane (brak towaru) → wpis audytu „Próba zamówienia” ZOSTAJE (był zatwierdzony w T2);
    //   • audyt się wywrócił, a OrderService złapał wyjątek → zamówienie ZATWIERDZONE, audyt wycofany.
    // Typowe zastosowania: audyt i logi prób („kto próbował zapłacić”), liczniki, numeracja dokumentów — rzeczy, które
    // mają przetrwać niezależnie od losu głównej operacji.
    //
    // PUŁAPKA: REQUIRES_NEW bierze DRUGIE połączenie z puli, a pierwsze nadal trzyma (wstrzymana transakcja czeka).
    //   Test sekcji 2 liczy aktywne połączenia puli HikariCP: w transakcji REQUIRED — 1, w środku REQUIRES_NEW — 2.
    //   Dlaczego to groźne? Przy dużym ruchu wątki mogą czekać na wolne połączenie, którego nikt nie odda (każdy
    //   trzyma jedno i czeka na drugie) — aplikacja „staje”. Domyślna pula Boota (HikariCP) ma 10 połączeń, a na wolne
    //   czeka się do 30 s; potem CannotCreateTransactionException („Could not open JPA EntityManager for
    //   transaction”) — sprawdzone testem Tx02PropagationPoolTest na małej puli.
    //   Do tego: jeśli T2 zmienia TEN SAM wiersz, który T1 już zmieniła (i zablokowała), T2 czeka na koniec T1,
    //   a T1 czeka na koniec T2 — zakleszczenie w JEDNYM wątku.
    //   Baza nie rozpozna go jako zakleszczenia (po drugiej stronie nie czeka transakcja, tylko Twój kod), więc T2
    //   czeka do limitu czasu blokady (lock timeout; w H2 domyślnie 2 s), potem QueryTimeoutException i wycofanie
    //   obu transakcji — sprawdzone testem sekcji 2.
    //   Używaj REQUIRES_NEW do krótkich, niezależnych zapisów (np. tabela audit_log), nie „na wszelki wypadek”.

    // =================================================================================================
    // 3. UnexpectedRollbackException — PYTANIE REKRUTACYJNE
    // =================================================================================================
    //
    // @Transactional
    // public void placeOrderIgnoringAuditFailure(...) {
    //     orders.save(new ShopOrder(product, quantity));
    //     try {
    //         audit.logRequiredAndFail(message);      // REQUIRED → ta sama transakcja; rzuca IllegalStateException
    //     } catch (IllegalStateException auditFailed) {
    //         // „połykamy” — przecież zamówienie ma się zapisać…
    //     }
    // }                                               // ← tu Spring próbuje COMMIT i…
    //
    // Wynik (sprawdzone testem sekcji 3): UnexpectedRollbackException („Transaction silently rolled back because it
    // has been marked as rollback-only”), a w bazie NIC — ani zamówienia, ani audytu.
    // Dlaczego? Wyjątek przeleciał przez PROXY metody logRequiredAndFail. Proxy nie może wycofać transakcji, która nie
    // jest „jego” (tylko do niej dołączyło), więc OZNACZA ją jako rollback-only („nadaje się już tylko do wycofania”).
    // W logu DEBUG: „Participating transaction failed - marking existing transaction as rollback-only”.
    // Złapanie wyjątku w OrderService tego znacznika nie zdejmuje. Przy wyjściu z placeOrder… Spring widzi znacznik,
    // robi ROLLBACK i — żeby nikt nie myślał, że się udało — rzuca UnexpectedRollbackException.
    // To samo dzieje się, gdy wyjątek rzuci REPOZYTORIUM (np. save z null w kolumnie NOT NULL): jego metody też są
    // @Transactional (REQUIRED) — sprawdzone tym samym testem.
    //
    // Naprawa zależy od intencji:
    //   • błąd audytu ma NIE psuć zamówienia → audyt w REQUIRES_NEW (własna transakcja, jej rollback nas nie dotyczy);
    //   • błąd audytu ma psuć zamówienie → nie łap wyjątku (niech leci, będzie zwykły rollback z czytelnym błędem).
    //
    // PUŁAPKA: try/catch wokół wywołania INNEGO beana transakcyjnego (REQUIRED) w środku transakcji. Dlaczego? Kod
    //   wygląda na odporny na błędy, a w rzeczywistości cała operacja i tak zostanie wycofana — tylko później i z mniej
    //   czytelnym wyjątkiem.
    //   Ustawienie menedżera transakcji globalRollbackOnParticipationFailure = false (global rollback on participation
    //   failure = „globalne wycofanie po błędzie uczestnika”) wyłącza znacznik rollback-only — ale to rzadko dobry
    //   pomysł. Test sekcji 3: wyjątku nie ma, a w bazie zostaje zamówienie RAZEM z wpisem audytu, którego zapis
    //   „się nie udał” (metoda rzuciła wyjątek już po INSERT). Niespójne dane zatwierdzone po cichu.

    // =================================================================================================
    // 4. KONTRAKTY: MANDATORY, NEVER oraz „dopasowujące się” SUPPORTS, NOT_SUPPORTED
    // =================================================================================================
    //
    // MANDATORY — metoda jest „kawałkiem” większej operacji i nie ma sensu sama. Wywołana bez transakcji →
    //   IllegalTransactionStateException: „No existing transaction found for transaction marked with propagation
    //   'mandatory'”. Dzięki temu błąd projektu widać od razu, a nie po tygodniu w danych.
    // NEVER — odwrotnie: metoda NIE może działać w transakcji (np. długie wywołanie zewnętrznego systemu, które nie
    //   powinno trzymać połączenia z bazą). W transakcji → IllegalTransactionStateException: „Existing transaction
    //   found for transaction marked with propagation 'never'”.
    // SUPPORTS — „wszystko mi jedno”: w transakcji wołającego — dołączy, bez niej — działa bez transakcji.
    // NOT_SUPPORTED — wstrzymuje transakcję wołającego i działa bez niej (isActualTransactionActive() → false).
    // Wszystko sprawdzone testem sekcji 4.
    //
    // DOBRA PRAKTYKA: MANDATORY dla metod pomocniczych, które zmieniają dane tylko jako część większej całości
    //   (np. „zdejmij ze stanu” wywoływane z „złóż zamówienie”). Dlaczego? Kontrakt jest zapisany w kodzie i pilnowany
    //   przez Springa, a nie tylko w komentarzu.

    // =================================================================================================
    // 5. NESTED — punkt zapisu (savepoint): ZWERYFIKUJ, zanim użyjesz
    // =================================================================================================
    //
    // Idea: wewnątrz transakcji T1 Spring robi punkt zapisu (Connection.setSavepoint). Błąd w metodzie NESTED cofa
    // tylko do punktu zapisu, a T1 trwa dalej — jak „Ctrl+Z do ostatniego zapisu gry”.
    //
    // Co NAPRAWDĘ się dzieje w tym module (Spring 7.0.9 + Hibernate 7.4, sprawdzone testami sekcji 5):
    //   • JpaTransactionManager, NESTED wewnątrz transakcji → NestedTransactionNotSupportedException: „Transaction
    //     manager does not allow nested transactions by default - specify 'nestedTransactionAllowed' property with
    //     value 'true'” — cała operacja się wywraca.
    //   • Nawet po włączeniu setNestedTransactionAllowed(true) → NestedTransactionNotSupportedException: „JpaDialect does
    //     not support savepoints - check your JPA provider's capabilities” (HibernateJpaDialect — tłumacz Springa dla
    //     Hibernate — nie obsługuje punktów zapisu).
    //   • NESTED bez transakcji wołającego → działa jak REQUIRED (tworzy nową).
    //   • Z menedżerem JDBC (JdbcTransactionManager / DataSourceTransactionManager — s07_data_jdbc) NESTED działa:
    //     część zagnieżdżona cofnięta do punktu zapisu, zewnętrzna zatwierdzona.
    //
    // PUŁAPKA: przepisanie przykładu z tutoriala o NESTED do serwisu JPA. Dlaczego? Kompiluje się i startuje, a pada
    //   dopiero przy pierwszym wywołaniu w transakcji. W JPA zamiast NESTED użyj REQUIRES_NEW (osobna transakcja)
    //   albo przeprojektuj operację tak, żeby błąd części nie wymagał częściowego cofania.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • REQUIRED (domyślna): dołącz albo utwórz — wspólny commit/rollback z wołającym.
     *   • REQUIRES_NEW: zawsze nowa transakcja (wołająca wstrzymana, drugie połączenie) — audyt, logi prób.
     *   • MANDATORY: musi być transakcja (inaczej IllegalTransactionStateException); NEVER: nie może jej być.
     *   • SUPPORTS: z transakcją lub bez; NOT_SUPPORTED: wstrzymuje transakcję i działa bez niej.
     *   • NESTED: punkt zapisu — tylko z menedżerem JDBC; z JpaTransactionManager → NestedTransactionNotSupportedException.
     *   • Wyjątek przez proxy metody REQUIRED (także repozytorium!) = rollback-only dla CAŁEJ transakcji;
     *     złapanie go → UnexpectedRollbackException przy commicie.
     *   • Podgląd: TransactionSynchronizationManager.getCurrentTransactionName() / isActualTransactionActive().
     *   • Propagacja działa tylko przez proxy — wołana metoda musi być w INNYM beanie.
     *
     * PYTANIA KONTROLNE:
     *   1. Jaka jest domyślna propagacja @Transactional i co robi, gdy wołający ma transakcję, a co, gdy nie ma?
     *   2. Co się stanie? orderService.placeOrderThenFail("Kawa ziarnista 1kg", 1, Propagation.REQUIRES_NEW)
     *      przy pustych tabelach — ile wierszy będzie w orders, a ile w audit_log?
     *   3. Co się stanie? orderService.placeOrderIgnoringAuditFailure("Kawa ziarnista 1kg", 1, Propagation.REQUIRED)
     *      — czy metoda zakończy się normalnie i czy zamówienie będzie w bazie? (PYTANIE REKRUTACYJNE)
     *   4. ZNAJDŹ BŁĄD: w serwisie JPA programista oznaczył metodę „zapisz pozycję zamówienia” jako
     *      {@code @Transactional(propagation = Propagation.NESTED)}, żeby błąd jednej pozycji cofał tylko ją. Woła ją
     *      w pętli z metody {@code @Transactional placeOrder}.
     *   5. Co się stanie? auditService.logMandatory("x") wywołane z testu, który nie ma transakcji.
     *   6. Czym różni się NOT_SUPPORTED od NEVER?
     *   7. Dlaczego REQUIRES_NEW może „zawiesić” aplikację przy małej puli połączeń?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — klasa Tx02Exercises (ten pakiet), testy: Tx02PropagationExercisesTest
    // =================================================================================================
    //
    //   ĆWICZENIE 1 (łatwe): Tx02Exercises.logEvenIfCallerFails — audyt, który przetrwa rollback wołającego.
    //   ĆWICZENIE 2 (średnie): Tx02Exercises.cancelOrderInsideTransaction — metoda tylko do wołania w transakcji.
    //   ĆWICZENIE 3 (trudniejsze): Tx02Exercises.placeOrderWithSafeAudit — błąd audytu nie psuje zamówienia.
    //   Rozwiązania: s10_transactions.solutions.tx02_propagation.Tx02PropagationSolutions.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. REQUIRED. Gdy wołający ma transakcję — metoda do niej dołącza (wspólny los). Gdy nie ma — tworzy nową.
     *   2. orders: 0 (zamówienie wycofane razem z T1), audit_log: 1 (wpis zatwierdzony w osobnej transakcji T2).
     *      Sprawdzone testem requiresNewIsIndependent.
     *   3. Nie zakończy się normalnie: przy wyjściu poleci UnexpectedRollbackException („Transaction silently rolled
     *      back because it has been marked as rollback-only”), a zamówienia NIE będzie — wyjątek przeleciał przez proxy
     *      metody REQUIRED i oznaczył wspólną transakcję jako rollback-only. Sprawdzone testem unexpectedRollback.
     *   4. JpaTransactionManager nie obsługuje NESTED wewnątrz transakcji: pierwsze wywołanie rzuci
     *      NestedTransactionNotSupportedException i całe zamówienie się wywróci. W JPA: REQUIRES_NEW dla pozycji
     *      (jeśli każda może się zapisać niezależnie) albo walidacja pozycji PRZED zapisem. Sprawdzone testem nestedWithJpa.
     *   5. IllegalTransactionStateException („No existing transaction found for transaction marked with propagation
     *      'mandatory'”), nic się nie zapisze. Sprawdzone testem mandatoryNeverSupportsNotSupported.
     *   6. NOT_SUPPORTED po cichu WSTRZYMUJE transakcję wołającego i działa bez niej; NEVER w takiej sytuacji rzuca
     *      IllegalTransactionStateException. Bez transakcji wołającego obie po prostu działają bez transakcji.
     *   7. Wątek z REQUIRES_NEW trzyma jedno połączenie (wstrzymana T1) i potrzebuje drugiego (T2). Gdy wszystkie
     *      połączenia są zajęte przez wątki czekające na „drugie”, nikt nie może ruszyć — każdy czeka do limitu puli
     *      (HikariCP: domyślnie 30 s), a potem dostaje CannotCreateTransactionException. Sprawdzone testem
     *      Tx02PropagationPoolTest (pula 2, jedno połączenie zajęte, limit 250 ms).
     */
    // </editor-fold>
}
