package s10_transactions.tx02_propagation;

/**
 * <pre>
 * TEMAT: Propagacja transakcji — co się dzieje, gdy metoda transakcyjna woła inną metodę transakcyjną:
 *        REQUIRED, REQUIRES_NEW, NESTED, SUPPORTS, NOT_SUPPORTED, MANDATORY, NEVER; rollback-only
 *        i {@code UnexpectedRollbackException}
 *        (propagation = propagacja, rozprzestrzenianie; required = wymagana; requires new = wymaga nowej;
 *         nested = zagnieżdżona; mandatory = obowiązkowa; rollback-only = „tylko do wycofania”)
 *
 * W SKRÓCIE:
 *   Atrybut {@code propagation} mówi proxy, co zrobić, gdy w chwili wywołania transakcja JUŻ trwa (albo nie trwa).
 *   Domyślna REQUIRED dołącza do trwającej transakcji — wszystkie metody dzielą jeden los: wyjątek w dowolnej z nich
 *   wycofuje całość, nawet jeśli wywołujący go złapie (wtedy commit kończy się {@code UnexpectedRollbackException}).
 *   REQUIRES_NEW zawiesza trwającą transakcję i otwiera własną, z DRUGIM połączeniem — idealne do audytu, który ma
 *   przetrwać wycofanie. NESTED to savepoint wewnątrz tej samej transakcji — ale NIE działa z JpaTransactionManager.
 *   Propagacja (jak cała {@code @Transactional}) działa tylko przy wywołaniu PRZEZ proxy.
 *
 * ANALOGIA: wspólny rachunek w restauracji.
 *   REQUIRED — dosiadasz się do stolika znajomych i dopisujesz do ich rachunku. Jeśli ktokolwiek przy stoliku
 *   zrobi awanturę i kelner unieważni rachunek, przepada cały — także Twoja część. REQUIRES_NEW — prosisz o osobny
 *   rachunek i płacisz od razu przy barze: cokolwiek stanie się przy stoliku, Twoja kawa jest zapłacona (ale barman
 *   musi obsłużyć Cię osobno — drugi „pracownik” = drugie połączenie). NESTED — kelner robi kreskę w notesie
 *   („od tego miejsca deser”): jeśli deseru nie ma, skreśla tylko to, co po kresce, a obiad zostaje.
 *
 * JAK TO DZIAŁA:
 *   1. Proxy wołanej metody pyta menedżera: {@code transactionManager.getTransaction(definicja)}. Menedżer patrzy, czy
 *      do wątku jest przypięta transakcja (TransactionSynchronizationManager) i co mówi propagacja.
 *   2. REQUIRED + trwa transakcja → „Participating in existing transaction” (dołączam): żadnego nowego połączenia,
 *      commit/rollback robi tylko ten, kto transakcję ZACZĄŁ. Uczestnik, z którego wyleciał wyjątek, może jedynie
 *      oznaczyć całą transakcję jako rollback-only.
 *   3. REQUIRES_NEW + trwa transakcja → „Suspending current transaction, creating new transaction” (zawieszam): bieżąca
 *      transakcja zostaje odpięta od wątku i odłożona, menedżer bierze NOWE połączenie z puli, po metodzie robi
 *      commit/rollback NOWEJ i przywraca starą.
 *   4. NESTED + trwa transakcja → {@code Connection.setSavepoint()}; wyjątek = {@code rollback(savepoint)} — tylko jeśli
 *      menedżer na to pozwala ({@code nestedTransactionAllowed}) i umie robić savepointy (JDBC tak, JPA nie).
 *   Log ({@code logging.level.org.springframework.orm.jpa.JpaTransactionManager=debug}) pokazuje każdą z tych decyzji.
 *
 * SŁÓWKA:
 *   propagation = propagacja; participate = uczestniczyć, dołączyć; suspend = zawiesić; resume = wznowić;
 *   savepoint = punkt zapisu (miejsce, do którego można się wycofać w ramach transakcji); rollback-only = oznaczona
 *   „tylko do wycofania”; unexpected rollback = nieoczekiwane wycofanie; illegal transaction state = niedozwolony stan
 *   transakcji; connection pool = pula połączeń; lock = blokada; deadlock = zakleszczenie; audit log = dziennik audytu;
 *   stock = stan magazynu; reserve = zarezerwować.
 *
 * ZOBACZ TEŻ: s10_transactions/Tx01Transactional (podstawy, rollback i wyjątki checked, setRollbackOnly),
 *             s10_transactions/Tx03ProxyPitfalls (dlaczego this.metoda() omija proxy),
 *             s10_transactions/Tx04ReadOnlyIsolation (blokady, izolacja — co widzi druga transakcja),
 *             s10_transactions/Tx05ProgrammaticTransactions (TransactionTemplate — propagacja w kodzie),
 *             t29_jdbc_databases/Jdbc04Transactions (savepointy w czystym JDBC).
 * </pre>
 */
public final class Tx02Propagation {

    private Tx02Propagation() {
    }

    // =================================================================================================
    // 1. REQUIRED (domyślna) — metoda wewnętrzna dołącza do zewnętrznej: wspólny los (OrderService.placeOrder)
    // =================================================================================================
    //
    //   OrderService.placeOrder  @Transactional (REQUIRED)  ── otwiera transakcję T1
    //     ├─ INSERT shop_order 'NEW'                          (w T1)
    //     ├─ StockService.reserve  @Transactional (REQUIRED)  ── T1 trwa → dołącza do T1
    //     │     UPDATE stock … WHERE quantity >= ?  → 0 wierszy → OutOfStockException
    //     └─ wyjątek wylatuje z placeOrder → proxy placeOrder (właściciel T1) robi ROLLBACK całej T1
    //
    // Sprawdzone testami: zamówienie 2 myszy (są 2) → {@code ZAM-1:RESERVED}, mysz = 0; zamówienie 3 myszy →
    // OutOfStockException „Brak towaru: Mysz ×3”, w bazie NIE MA zamówienia (INSERT z T1 wycofany), mysz = 2.
    // REQUIRED bez trwającej transakcji (np. reserve wywołane prosto z testu) po prostu otwiera nową.
    //
    // DOBRA PRAKTYKA: REQUIRED jest domyślna i w 90% przypadków właściwa — nie pisz {@code propagation = REQUIRED}
    //   „na wszelki wypadek”. Inną propagację wybieraj świadomie i opisz w komentarzu, DLACZEGO.

    // =================================================================================================
    // 2. PUŁAPKA rollback-only: wewnętrzna rzuca, zewnętrzna łapie → UnexpectedRollbackException
    // =================================================================================================
    //
    //   PRZED (OrderService.placeOrderIgnoringStockError):
    //     {@code insertOrder(...); try { stock.reserve(...); } catch (OutOfStockException e) { updateStatus(..., "WAITING"); }}
    //   Wydaje się, że obsłużyliśmy błąd. Ale wyjątek przeszedł przez proxy {@code StockService.reserve}. To proxy jest
    //   tylko UCZESTNIKIEM T1, więc nie może jej wycofać — robi jedyne, co może: oznacza T1 jako rollback-only
    //   (log DEBUG: „Participating transaction failed - marking existing transaction as rollback-only” — test).
    //   Metoda zewnętrzna kończy się normalnie, jej proxy próbuje commitu, widzi flagę i… wycofuje, rzucając:
    //     {@code org.springframework.transaction.UnexpectedRollbackException:}
    //     {@code Transaction silently rolled back because it has been marked as rollback-only}   (sprawdzone testem)
    //   W bazie nie ma zamówienia — ani NEW, ani WAITING.
    //   PYTANIE REKRUTACYJNE: „Skąd się bierze UnexpectedRollbackException?”
    //
    // Porównaj z s10_transactions/Tx01Transactional, sekcja 4: tam wyjątek rzucał NASZ kod w tej samej metodzie (nie
    // przechodził przez żadne proxy) — złapanie go dawało commit. Tu decyduje to, że po drodze było proxy uczestnika.
    //
    //   PO (OrderService.placeOrderOrWait): metoda wewnętrzna {@code tryReserve} ZWRACA {@code boolean} zamiast rzucać —
    //   żaden wyjątek nie przechodzi przez proxy, nikt nie ustawia flagi, commit się udaje (test: ZAM-1:WAITING,
    //   ZAM-2:RESERVED). Inne wyjścia: sprawdzić warunek przed wywołaniem; dać metodzie wewnętrznej REQUIRES_NEW (gdy
    //   jej skutki MAJĄ być niezależne — sekcja 3) albo NESTED (gdy menedżer umie — sekcja 4).
    //
    // PUŁAPKA: tak samo działa wyjątek z repozytorium Spring Data — jego metody też są transakcyjne (REQUIRED).
    //   Dlaczego ważne? {@code try { repo.save(...) } catch (DataIntegrityViolationException e) { … }} w metodzie
    //   transakcyjnej zwykle kończy się UnexpectedRollbackException przy commicie.

    // =================================================================================================
    // 3. REQUIRES_NEW — audyt zapisany mimo wycofania głównej transakcji; drugie połączenie; zakleszczenie (AuditLog)
    // =================================================================================================
    //
    //   OrderService.placeOrder (T1) ──► AuditLog.record @Transactional(propagation = REQUIRES_NEW)
    //        T1 zawieszona ── T2: nowe połączenie, INSERT audit_log, COMMIT T2 ── T1 wznowiona ──► … OutOfStockException
    //        → ROLLBACK T1 (zamówienie znika), a wpis audytu „Próba złożenia zamówienia ZAM-1” ZOSTAJE (test).
    //
    // Co jeszcze pokazują testy (OrderService.insertOrderAndLookFromNewTransaction):
    //   • {@code SELECT SESSION_ID()} w T2 jest INNY niż w T1 — to drugie połączenie (inna sesja H2);
    //   • T2 NIE widzi zamówienia wstawionego w T1 ({@code COUNT(*) = 0}) — dla bazy to dwie niezależne transakcje,
    //     a T1 jeszcze nie zatwierdziła (izolacja READ COMMITTED — s10_transactions/Tx04ReadOnlyIsolation);
    //   • nazwa transakcji w T2: {@code s10_transactions.tx02_propagation.AuditLog.inspectFromNewTransaction}.
    //
    // PUŁAPKA: REQUIRES_NEW potrzebuje DRUGIEGO połączenia, a pierwsze jest ciągle zajęte przez zawieszoną T1.
    //   Dlaczego groźne? Przy N równoległych żądaniach każde trzyma 1 połączenie i czeka na drugie — pula o rozmiarze N
    //   się wyczerpuje i wszyscy czekają na siebie nawzajem. Test z pulą o rozmiarze 1
    //   ({@code spring.datasource.hikari.maximum-pool-size=1}, {@code connection-timeout=250}):
    //   {@code CannotCreateTransactionException: Could not open JPA EntityManager for transaction}, a przyczyna:
    //   {@code SQLTransientConnectionException: HikariPool-… - Connection is not available, request timed out after …ms}.
    //
    // PUŁAPKA: REQUIRES_NEW zmienia wiersz, który zawieszona T1 już zmieniła (OrderService.reserveAndCorrectInNewTransaction:
    //   T1 robi UPDATE stock 'Mysz', T2 robi UPDATE stock 'Mysz'). T2 czeka na blokadę wiersza trzymaną przez T1,
    //   a T1 nie może się skończyć, bo czeka (w tym samym wątku!) na powrót z T2 — zakleszczenie aplikacyjne, którego
    //   baza nie wykryje. Test ustawia w H2 {@code ;LOCK_TIMEOUT=200} (ms) i dostaje
    //   {@code QueryTimeoutException … Timeout trying to lock table "STOCK"}; bez limitu (np. PostgreSQL ma domyślnie
    //   {@code lock_timeout = 0} — czekaj bez końca) wątek wisiałby na zawsze.
    //
    // DOBRA PRAKTYKA: REQUIRES_NEW tylko dla rzeczy naprawdę NIEZALEŻNYCH od głównej operacji (audyt, licznik prób,
    //   log błędów), które dotykają INNYCH tabel/wierszy. Dlaczego? Każde użycie to drugie połączenie i ryzyko
    //   blokady; a skutki REQUIRES_NEW zostają, nawet gdy główna operacja się wycofa — upewnij się, że tego chcesz
    //   (rezerwacja magazynu w REQUIRES_NEW „wyciekłaby” przy wycofanym zamówieniu).
    //   PYTANIE REKRUTACYJNE: „Czym różni się REQUIRED od REQUIRES_NEW? Podaj przykład użycia REQUIRES_NEW.”

    // =================================================================================================
    // 4. Pozostałe: SUPPORTS, NOT_SUPPORTED, MANDATORY, NEVER, NESTED (PropagationShowcase, NestedOrderService)
    // =================================================================================================
    //
    //   Propagacja      gdy transakcja TRWA                              gdy transakcji NIE MA
    //   REQUIRED        dołącz                                           otwórz nową
    //   REQUIRES_NEW    zawieś ją, otwórz nową (drugie połączenie)       otwórz nową
    //   NESTED          savepoint w tej samej transakcji                 otwórz nową (jak REQUIRED)
    //   SUPPORTS        dołącz                                           działaj bez transakcji
    //   NOT_SUPPORTED   zawieś ją, działaj bez transakcji                działaj bez transakcji
    //   MANDATORY       dołącz                                           IllegalTransactionStateException
    //   NEVER           IllegalTransactionStateException                 działaj bez transakcji
    //
    // Komunikaty (sprawdzone testami; transakcję „z zewnątrz” otwiera w teście TransactionTemplate):
    //   MANDATORY bez transakcji: {@code No existing transaction found for transaction marked with propagation 'mandatory'}
    //   NEVER w transakcji:       {@code Existing transaction found for transaction marked with propagation 'never'}
    //   NOT_SUPPORTED w transakcji: {@code isActualTransactionActive()} = false — zewnętrzna jest zawieszona.
    // Do czego? MANDATORY — metoda, która MA być częścią większej operacji (np. zapis do księgi) i nie może przypadkiem
    // zatwierdzić się sama; NOT_SUPPORTED — długie, nietransakcyjne prace (np. generowanie raportu, wywołanie obcej
    // usługi) wewnątrz kodu transakcyjnego; SUPPORTS — rzadko, dla odczytów „wszystko jedno”.
    //
    // NESTED a JPA (sprawdzone testami):
    //   • z domyślnym {@code JpaTransactionManager}: {@code isNestedTransactionAllowed()} = false i
    //     {@code NestedTransactionNotSupportedException: Transaction manager does not allow nested transactions by default -}
    //     {@code specify 'nestedTransactionAllowed' property with value 'true'};
    //   • po {@code setNestedTransactionAllowed(true)}: {@code NestedTransactionNotSupportedException: JpaDialect does not}
    //     {@code support savepoints - check your JPA provider's capabilities} — integracja Springa z Hibernate
    //     (HibernateJpaDialect) nie udostępnia savepointów, a sama JPA i tak nie umiałaby cofnąć kontekstu utrwalania
    //     (encji w pamięci) do savepointu.
    //   • z {@code JdbcTransactionManager} (drugi bean w Tx02Application, {@code @Bean(defaultCandidate = false)},
    //     wskazywany {@code @Transactional(transactionManager = "jdbcTransactionManager")}) NESTED działa: zamówienie
    //     z prezentem, gdy prezentu brak → ROLLBACK TO SAVEPOINT tylko rezerwacji prezentu; zamówienie i rezerwacja
    //     produktu zostają (test: 3 zamówienia RESERVED, klawiatur 2, myszy 0). Tu łapanie wyjątku jest bezpieczne —
    //     NESTED nie oznacza całości jako rollback-only.
    //
    // PUŁAPKA: dwa menedżery transakcji na JEDNYM DataSource w jednym wywołaniu. Transakcja menedżera JDBC woła metodę
    //   z {@code @Transactional} domyślnego (JPA) menedżera (NestedOrderService.placeOrderMixingManagers) →
    //   {@code CannotCreateTransactionException: Could not open JPA EntityManager for transaction}, przyczyna:
    //   {@code IllegalStateException: Already value [ConnectionHolder…] for key [HikariDataSource (…)] bound to thread}.
    //   Dlaczego? JpaTransactionManager nie rozpoznaje transakcji JDBC jako „swojej”, chce otworzyć własną i trafia na
    //   połączenie już przypięte do wątku. DOBRA PRAKTYKA: jeden menedżer transakcji na bazę; drugi tylko wyjątkowo
    //   i w odizolowanym kawałku kodu (jak tu, dla pokazania savepointów). Zamiast NESTED w JPA zwykle wystarcza
    //   „sprawdź, zanim zrobisz” albo wynik zamiast wyjątku (sekcja 2).

    // =================================================================================================
    // 5. Propagacja działa TYLKO przez proxy — this.metoda() z REQUIRES_NEW nic nie robi (zapowiedź Tx03)
    // =================================================================================================
    //
    //   PRZED (OrderService.placeOrderWithInternalAudit): {@code recordInternally(...)} — metoda TEJ SAMEJ klasy
    //   z {@code @Transactional(propagation = REQUIRES_NEW)}. Wywołanie {@code this.recordInternally} idzie prosto do
    //   obiektu, z pominięciem proxy, więc adnotacja jest ignorowana: wpis audytu powstaje w T1 i znika przy rollbacku
    //   (test: audit_log pusty). Dowód drugi: {@code currentTransactionNameInNewTransaction()} wywołana przez this zwraca
    //   nazwę transakcji ZEWNĘTRZNEJ ({@code …OrderService.transactionNameSeenBySelfInvokedRequiresNew}), a wywołana
    //   z zewnątrz (przez proxy) — własną ({@code …OrderService.currentTransactionNameInNewTransaction}).
    //   PO (OrderService.placeOrder): audyt w OSOBNYM beanie (AuditLog) — wywołanie przez proxy, REQUIRES_NEW działa.
    //   Szczegóły i inne obejścia: s10_transactions/Tx03ProxyPitfalls, sekcja 2.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • REQUIRED (domyślna): dołącz albo otwórz; wspólny los — wyjątek u uczestnika = rollback-only całości.
     *   • Złapany wyjątek z wewnętrznej REQUIRED → {@code UnexpectedRollbackException} („Transaction silently rolled
     *     back because it has been marked as rollback-only”). Lek: wynik zamiast wyjątku / sprawdź wcześniej.
     *   • REQUIRES_NEW: zawieś, nowa transakcja, DRUGIE połączenie, commit od razu → audyt, liczniki prób.
     *     Ryzyka: wyczerpanie puli, zakleszczenie przy tym samym wierszu, „wyciek” skutków przy wycofaniu głównej.
     *   • NESTED = savepoint; JpaTransactionManager go NIE obsługuje (NestedTransactionNotSupportedException);
     *     JdbcTransactionManager — tak.
     *   • SUPPORTS (dołącz / bez), NOT_SUPPORTED (zawieś / bez), MANDATORY (dołącz / wyjątek), NEVER (wyjątek / bez).
     *   • Nie mieszaj dwóch menedżerów na jednym DataSource w jednym wywołaniu.
     *   • Propagacja działa tylko przez proxy — this.metoda() ją ignoruje.
     *
     * PYTANIA KONTROLNE:
     *   1. Co się stanie? {@code @Transactional placeOrder()} wstawia zamówienie i woła {@code stock.reserve()}
     *      (REQUIRED), które rzuca OutOfStockException; placeOrder go nie łapie. Co jest w bazie?
     *   2. Co się stanie? Ten sam placeOrder łapie OutOfStockException z {@code stock.reserve()} i ustawia status WAITING.
     *      Jaki wyjątek dostanie wywołujący i co będzie w bazie? Dlaczego?
     *   3. ZNAJDŹ BŁĄD: rezerwację magazynu przeniesiono do REQUIRES_NEW, „żeby błąd magazynu nie psuł zamówienia”.
     *      Co się stanie z towarem, gdy po rezerwacji zamówienie wycofa się z innego powodu?
     *   4. Dlaczego REQUIRES_NEW może wyczerpać pulę połączeń albo zakleszczyć wątek? Kiedy jest naprawdę potrzebne?
     *   5. Czym różni się NESTED od REQUIRES_NEW? Co się stanie przy NESTED z domyślnym menedżerem Boota w aplikacji JPA?
     *   6. Kiedy MANDATORY, a kiedy NOT_SUPPORTED? Jaki wyjątek rzuca MANDATORY bez transakcji?
     *   7. ZNAJDŹ BŁĄD: {@code @Transactional void placeOrder() { … this.audit("…"); … }} oraz
     *      {@code @Transactional(propagation = REQUIRES_NEW) public void audit(String m)} w tej samej klasie —
     *      „audyt znika przy rollbacku zamówienia”.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w plikach ExerciseAuditLog.java (1, 3), PropagationExercises.java (2, 4) i ExerciseOrderWriter.java (4),
    // testy: Tx02PropagationExercisesTest (pełna aplikacja, bez transakcji testu).
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Nic: reserve dołączył do transakcji placeOrder (REQUIRED), wyjątek wyleciał z placeOrder, jej proxy zrobiło
     *      rollback całości — zamówienia nie ma, stan magazynu bez zmian (test sekcji 1).
     *   2. {@code UnexpectedRollbackException: Transaction silently rolled back because it has been marked as
     *      rollback-only}; w bazie nie ma zamówienia (ani NEW, ani WAITING). Wyjątek przeszedł przez proxy reserve —
     *      uczestnika transakcji — które oznaczyło wspólną transakcję jako rollback-only; commit zewnętrznej musiał się
     *      zamienić w rollback (test sekcji 2).
     *   3. Rezerwacja zatwierdziła się we własnej transakcji, więc po wycofaniu zamówienia towar zostaje
     *      „zarezerwowany” dla nieistniejącego zamówienia — wyciek. REQUIRES_NEW tylko dla rzeczy niezależnych (audyt);
     *      tu lepiej tryReserve (wynik zamiast wyjątku) albo sprawdzenie przed rezerwacją.
     *   4. Zawieszona transakcja trzyma pierwsze połączenie, nowa bierze drugie — przy wielu równoległych wywołaniach
     *      pula się kończy (test: pula 1 → CannotCreateTransactionException). Gdy nowa transakcja zmienia wiersz
     *      zablokowany przez zawieszoną, czeka na nią, a ona czeka na powrót z nowej — zakleszczenie (test: Timeout
     *      trying to lock table "STOCK"). Potrzebne, gdy skutek ma przetrwać wycofanie głównej operacji (audyt).
     *   5. NESTED działa w TEJ SAMEJ transakcji i połączeniu (savepoint) — commit całości robi zewnętrzna, wycofanie
     *      zagnieżdżonej cofa tylko do savepointu. REQUIRES_NEW to osobna transakcja z osobnym połączeniem i własnym
     *      commitem. Z JpaTransactionManager NESTED rzuca NestedTransactionNotSupportedException (domyślnie
     *      nestedTransactionAllowed = false; po włączeniu — „JpaDialect does not support savepoints”).
     *   6. MANDATORY — gdy metoda musi być częścią większej transakcji i nie może zatwierdzić się sama; bez transakcji
     *      rzuca IllegalTransactionStateException „No existing transaction found for transaction marked with
     *      propagation 'mandatory'”. NOT_SUPPORTED — gdy kawałek pracy ma iść bez transakcji (zawiesza trwającą).
     *   7. this.audit(...) omija proxy, więc REQUIRES_NEW jest ignorowane i wpis powstaje w transakcji zamówienia —
     *      znika przy jej rollbacku (test sekcji 5). Naprawa: audyt w osobnym beanie wywoływanym przez proxy
     *      (albo inne obejścia z s10_transactions/Tx03ProxyPitfalls).
     */
    // </editor-fold>
}
