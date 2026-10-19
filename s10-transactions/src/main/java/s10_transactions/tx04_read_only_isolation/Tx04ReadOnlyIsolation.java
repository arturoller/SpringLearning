package s10_transactions.tx04_read_only_isolation;

/**
 * <pre>
 * TEMAT: readOnly, poziomy izolacji, utracona aktualizacja i timeout — atrybuty @Transactional poza propagacją
 *        (read only = tylko do odczytu; isolation = izolacja; timeout = limit czasu; lost update = utracona
 *         aktualizacja; optimistic/pessimistic locking = blokowanie optymistyczne/pesymistyczne)
 *
 * W SKRÓCIE:
 *   @Transactional ma jeszcze trzy ważne atrybuty. readOnly = true mówi „tylko czytam”: Hibernate przestaje śledzić
 *   zmiany encji i nie robi flush — zapis w takiej transakcji PRZEPADA po cichu (to nie jest zabezpieczenie przed
 *   zapisem!). isolation decyduje, ile widać z cudzych, równoległych transakcji: brudne odczyty, niepowtarzalne
 *   odczyty, fantomy. Najgroźniejsze zjawisko w praktyce to jednak utracona aktualizacja: dwie osoby czytają ten sam
 *   stan i obie go nadpisują. Bronią przed nią: atomowy UPDATE, @Version, blokada pesymistyczna. timeout ogranicza
 *   czas transakcji — ale niczego nie przerywa „w locie”, tylko sprawdza termin przed kolejnym zapytaniem.
 *   Wszystkie trzy atrybuty działają TYLKO, gdy metoda zaczyna NOWĄ transakcję.
 *
 * ANALOGIA: inwentaryzacja w sklepie.
 *   readOnly — kontroler z listą w ręku: tylko liczy, niczego nie przestawia (a jeśli coś przestawi, nikt tego nie
 *   zapisze w systemie). Izolacja — czy liczący widzi towar, który kasjer właśnie skanuje, ale klient jeszcze nie
 *   zapłacił (brudny odczyt)? Utracona aktualizacja — dwóch magazynierów spisuje ten sam regał z tej samej kartki
 *   „7 sztuk”, każdy wydaje 1 sztukę i obaj wpisują „6” — jedna sztuka „znika” z ewidencji. timeout — „inwentaryzacja
 *   ma trwać najwyżej godzinę”; nikt nie wyrwie liczącemu kartki o pełnej godzinie, ale następny wpis już nie przejdzie.
 *
 * JAK TO DZIAŁA:
 *   @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ, timeout = 5)
 *     proxy → JpaTransactionManager.getTransaction(...) — w logu DEBUG:
 *       Creating new transaction with name [...]: PROPAGATION_REQUIRED,ISOLATION_REPEATABLE_READ,timeout_5,readOnly
 *     1. HibernateJpaDialect: sesja Hibernate → FlushMode.MANUAL + setDefaultReadOnly(true)        (readOnly)
 *     2. połączenie JDBC: setReadOnly(true), setTransactionIsolation(4 = REPEATABLE READ)         (readOnly, isolation)
 *     3. termin = teraz + 5 s; przed każdym zapytaniem: setQueryTimeout(pozostały czas)             (timeout)
 *     4. Twoja metoda; commit/rollback
 *     5. połączenie wraca do puli z przywróconym poziomem izolacji i setReadOnly(false)
 *   Kroki 1–3 i 5 sprawdzone testami (oraz śledzeniem wywołań JDBC w H2: TRACE_LEVEL_SYSTEM_OUT=3).
 *
 * SŁÓWKA:
 *   read only = tylko do odczytu; flush = wypchnięcie zmian do bazy; isolation level = poziom izolacji;
 *   dirty read = brudny odczyt; non-repeatable read = niepowtarzalny odczyt; phantom read = odczyt fantomów;
 *   lost update = utracona aktualizacja; optimistic/pessimistic lock = blokada optymistyczna/pesymistyczna;
 *   version = wersja; deadline = termin; timed out = przekroczono czas; lock timeout = limit czasu czekania na blokadę;
 *   persistence context = kontekst utrwalania (pamięć encji w transakcji); refresh = odśwież.
 *
 * ZOBACZ TEŻ: s10_transactions/Tx01Transactional (podstawy), s10_transactions/Tx02Propagation (REQUIRED dołącza,
 *             REQUIRES_NEW), s10_transactions/Tx05ProgrammaticTransactions (te same atrybuty w TransactionTemplate),
 *             s08_data_jpa/Jpa01Entities (kontekst utrwalania, dirty checking),
 *             t21_concurrency/Concurrency02RaceConditions (wyścig o dane w pamięci — ten sam problem co utracona
 *             aktualizacja), t29_jdbc_databases/Jdbc04Transactions.
 * </pre>
 */
public final class Tx04ReadOnlyIsolation {

    private Tx04ReadOnlyIsolation() {
    }

    // =================================================================================================
    // 0. Jak testujemy współbieżność BEZ wątków — SecondUser i parametr „meanwhile”
    // =================================================================================================
    //
    // Zjawiska izolacji wymagają DWÓCH transakcji naraz. Dwa wątki i sleep dają wynik zależny od szczęścia, więc
    // w tej lekcji drugą transakcję prowadzi SecondUser — osobne połączenie JDBC z puli, z własną transakcją.
    // Jego kroki wstawiamy w ściśle określone miejsce: przed wywołaniem serwisu albo w parametr meanwhile
    // („w międzyczasie”), który serwis uruchamia między swoim odczytem a zapisem:
    //
    //   stockService.sellWithPause("ELE-001", 1, () -> bob.updateAndCommit("update ... stock = stock - 1 ..."));
    //      1. nasza transakcja czyta: stock = 7
    //      2. meanwhile: drugi użytkownik sprzedaje 1 szt. i zatwierdza → w bazie 6
    //      3. nasza transakcja: 7 - 1 = 6 → UPDATE ... set stock = 6 → COMMIT
    // Kolejność kroków jest zawsze ta sama — test jest deterministyczny. W prawdziwej aplikacji parametru meanwhile
    // nie ma: „w międzyczasie” to po prostu inne żądania obsługiwane równolegle.

    // =================================================================================================
    // 1. readOnly = true — co NAPRAWDĘ robi (Spring 7 + Hibernate 7.4 + H2)
    // =================================================================================================
    //
    // Sprawdzone testem sekcji 1 (ReportService.insideReadOnly / insideReadWrite):
    //   co                                         | readOnly = true | zwykła transakcja
    //   -------------------------------------------+-----------------+------------------
    //   TransactionSynchronizationManager          | readOnly: true  | false
    //     .isCurrentTransactionReadOnly()          |                 |
    //   sesja Hibernate: getHibernateFlushMode()   | MANUAL          | AUTO
    //   sesja Hibernate: isDefaultReadOnly()       | true            | false
    //
    //   • FlushMode.MANUAL (ręczny tryb wypychania) — Hibernate nie wysyła zmian do bazy ani przed zapytaniami, ani
    //     przy commicie.
    //   • setDefaultReadOnly(true) — encje wczytane w tej transakcji są tylko do odczytu: Hibernate nie porównuje ich
    //     stanu przy commicie (brak dirty checking), więc nie musi trzymać „zdjęcia” ich stanu — mniej pracy i pamięci.
    //   • Connection.setReadOnly(true) — to tylko PODPOWIEDŹ dla sterownika JDBC. H2 ją ignoruje: ręczny SQL UPDATE
    //     w transakcji readOnly przechodzi (test). Inne bazy mogą zapis odrzucić — zależy od bazy i sterownika.
    //   • Spring Data: SimpleJpaRepository (implementacja Twoich repozytoriów) ma @Transactional(readOnly = true) nad
    //     KLASĄ i zwykłe @Transactional nad save/delete — findById, findAll itd. działają w transakcji readOnly (test).
    //
    // DOBRA PRAKTYKA: metody, które tylko czytają (raporty, listy, szczegóły), oznaczaj @Transactional(readOnly = true);
    //   w serwisie „głównie do odczytu” możesz dać readOnly nad klasą, a zwykłe @Transactional nad metodami zapisu
    //   (tak robi SimpleJpaRepository). Dlaczego? Hibernate ma mniej pracy, a zamiar „tylko czytam” jest zapisany
    //   w kodzie — przypadkowa zmiana encji w raporcie nie trafi do bazy.

    // =================================================================================================
    // 2. PUŁAPKA: zapis w transakcji readOnly przepada PO CICHU
    // =================================================================================================
    //
    // Sprawdzone testem sekcji 2 (stan ELE-001 = 7):
    //   reports.sellInReadOnlyTransaction("ELE-001", 1)    → zwraca 6 (obiekt w pamięci), w bazie nadal 7
    //   reports.sellAndSaveInReadOnlyTransaction(...)      → jawne save() też nie pomaga — w bazie 7
    //   reports.reportThatSells("ELE-004", 1)              → raport readOnly woła StockService.sell (REQUIRED)
    //                                                        — sell DOŁĄCZA do transakcji tylko do odczytu → 4 zostaje 4
    // Ani wyjątku, ani ostrzeżenia w logu.
    //
    // PUŁAPKA: readOnly traktowane jak „zabezpieczenie przed zapisem” albo dopisane „na zapas” do metody, która woła
    //   inne serwisy. Dlaczego groźne? Zapisy wywołanych metod giną bez śladu, a ręczny SQL (JdbcClient) na H2 i tak
    //   przechodzi — więc nie chroni to ani przed zapisem, ani przed jego utratą. Raport, który MUSI coś zapisać
    //   (np. dziennik raportów), robi to w osobnej transakcji (REQUIRES_NEW — Tx02Propagation).

    // =================================================================================================
    // 3. Poziomy izolacji i zjawiska (PYTANIE REKRUTACYJNE)
    // =================================================================================================
    //
    // Zjawiska (gdy dwie transakcje pracują równolegle):
    //   brudny odczyt        — widzę zmianę, której inny jeszcze NIE zatwierdził (a może ją wycofać);
    //   niepowtarzalny odczyt — czytam ten sam wiersz dwa razy i dostaję różne wartości (ktoś zatwierdził zmianę);
    //   fantom               — to samo zapytanie o ZBIÓR (np. count w kategorii) daje więcej/mniej wierszy.
    //
    //   Isolation.*         | brudny odczyt | niepowtarzalny odczyt | fantom     ← standard SQL („może wystąpić”)
    //   --------------------+---------------+-----------------------+---------
    //   READ_UNCOMMITTED    | może          | może                  | może
    //   READ_COMMITTED      | nie           | może                  | może
    //   REPEATABLE_READ     | nie           | nie                   | może
    //   SERIALIZABLE        | nie           | nie                   | nie
    //   DEFAULT             | poziom domyślny BAZY — w H2: READ COMMITTED (test)
    //
    // Co pokazuje H2 2.4 (testy sekcji 3, stan ELE-001 = 7):
    //   • READ_UNCOMMITTED widzi niezatwierdzone „0” drugiego użytkownika; READ_COMMITTED widzi 7.
    //   • dwa odczyty z cudzym commitem pomiędzy: READ_COMMITTED → [7, 6]; REPEATABLE_READ → [6, 6].
    //   • count w kategorii DOM z cudzym INSERT pomiędzy: READ_COMMITTED → [2, 3]; REPEATABLE_READ → [3, 3] —
    //     H2 jest tu ŚCIŚLEJSZY niż standard (fantomów nie pokazuje). Inne bazy różnią się szczegółami — dlatego
    //     poziom izolacji zawsze sprawdzaj na tej bazie, której używasz na produkcji (s11_testing/Test06Testcontainers).
    //   • SERIALIZABLE w H2 dał w tych scenariuszach te same wyniki co REPEATABLE_READ (sprawdzone uruchomieniem).
    // Spring ustawia poziom na połączeniu (Connection.setTransactionIsolation) na czas transakcji i PRZYWRACA go,
    // zanim połączenie wróci do puli (test: po transakcji SERIALIZABLE kolejna znów ma READ COMMITTED).
    //
    // DOBRA PRAKTYKA: zostaw Isolation.DEFAULT (zwykle READ COMMITTED), a konkretne ryzykowne miejsca chroń celowo:
    //   atomowym UPDATE, @Version albo blokadą (sekcja 5). Dlaczego? Wyższa izolacja to więcej konfliktów: baza
    //   częściej wycofuje transakcje (patrz błąd 40001 w sekcji 5), a aplikacja musi je ponawiać.

    // =================================================================================================
    // 4. Kontekst utrwalania „udaje” REPEATABLE READ dla encji
    // =================================================================================================
    //
    // StockReader.readEntityTwice (READ_COMMITTED!), drugi użytkownik sprzedaje 1 szt. w międzyczasie:
    //   [encja 1. findById, encja 2. findById, SQL, encja po refresh] = [7, 7, 6, 6]   (test sekcji 4)
    // Drugi findById nie pyta bazy — zwraca TEN SAM obiekt z kontekstu utrwalania (pamięci encji transakcji), ze
    // starym stanem. Czysty SQL widzi już 6. entityManager.refresh(encja) czyta wiersz od nowa.
    //
    // PUŁAPKA: „sprawdzę jeszcze raz findById, czy stan się nie zmienił” w tej samej transakcji. Dlaczego? Dostaniesz
    //   ten sam obiekt z pamięci, niezależnie od poziomu izolacji. Świeże dane: refresh, zapytanie SQL albo nowa
    //   transakcja — a najlepiej obrona przed utratą aktualizacji z sekcji 5.

    // =================================================================================================
    // 5. Utracona aktualizacja (lost update) i cztery obrony (PYTANIE REKRUTACYJNE: blokowanie optymistyczne vs
    //    pesymistyczne)
    // =================================================================================================
    //
    // Problem — StockService.sellWithPause (sekcja 0): dwie sprzedaże po 1 szt. przy stanie 7, a w bazie 6, nie 5.
    // Żaden poziom izolacji READ_COMMITTED tego nie wykryje: każda transakcja zrobiła „poprawny” UPDATE. Test sekcji 5.
    //
    // OBRONA 1 — atomowy UPDATE (najlepszy dla liczników i stanów):
    //   update products set stock = stock - :pieces where sku = :sku and stock >= :pieces
    //   Baza odejmuje od AKTUALNEJ wartości i sama sprawdza warunek; 0 zmienionych wierszy = „za mało towaru”.
    //   Test: 7 → (drugi użytkownik) 6 → (my) 5; próba sprzedaży 99 szt. → false, stan bez zmian.
    //
    // OBRONA 2 — blokowanie optymistyczne: pole @Version w encji (VersionedProduct). Hibernate wysyła
    //   update versioned_products set name=?,stock=?,version=? where sku=? and version=?
    //   Gdy drugi użytkownik w międzyczasie zmienił wiersz (i podbił wersję), UPDATE trafia w 0 wierszy i przy
    //   COMMICIE (nie w linii sell!) leci ObjectOptimisticLockingFailureException:
    //     Unexpected row count (expected row count 1 but was 0) [update versioned_products set ... where sku=? and
    //     version=?] for entity [...VersionedProduct with id 'ELE-001']
    //   Nasza zmiana wycofana, cudza zostaje (stan/wersja = 6/1). Co dalej? Ponowić całą operację w NOWEJ transakcji
    //   (ćwiczenie 3) albo pokazać użytkownikowi „ktoś zmienił dane — odśwież”. „Optymistyczne”, bo zakłada, że
    //   konflikty są rzadkie: nikogo nie blokuje, tylko wykrywa konflikt na końcu.
    //
    // OBRONA 3 — blokowanie pesymistyczne: @Lock(LockModeType.PESSIMISTIC_WRITE) na metodzie repozytorium
    //   (ProductRepository.findForUpdate). Wiersz jest zablokowany od ODCZYTU do końca transakcji — inni, którzy chcą
    //   go zmienić, CZEKAJĄ. Test: drugi użytkownik próbuje sprzedać w międzyczasie i… czeka na naszą blokadę. Że to
    //   ten sam wątek, nie doczeka się — H2 przerywa czekanie po swoim limicie (2 s) błędem o kodzie 50200. W prawdziwej
    //   aplikacji drugi użytkownik to inny wątek: poczeka, aż zatwierdzimy, i sprzeda od ŚWIEŻEJ wartości.
    //   „Pesymistyczne”, bo zakłada konflikt z góry i blokuje — kosztem czekania innych.
    //
    // OBRONA 4 (zależna od bazy) — wyższa izolacja: w H2 REPEATABLE_READ nie pozwala nadpisać wiersza zmienionego
    //   i zatwierdzonego przez kogoś po naszym odczycie: nasza transakcja jest wycofywana (kod błędu H2 40001, komunikat
    //   „Deadlock detected. The current transaction was rolled back.” — na polskim Windowsie także po polsku:
    //   „Wykryto zakleszczenie…”), a Spring zgłasza CannotAcquireLockException. Test sekcji 5.
    //
    // DOBRA PRAKTYKA: liczniki i stany → atomowy UPDATE; edycja „formularza” encji (cena, opis) → @Version;
    //   krótkie, gorące sekcje, gdzie konflikt jest pewny → blokada pesymistyczna. Dlaczego nie zawsze pesymistycznie?
    //   Blokady to czekanie innych żądań (i ryzyko zakleszczeń), a @Version nic nie kosztuje, dopóki nie ma konfliktu.

    // =================================================================================================
    // 6. timeout — limit czasu transakcji (w SEKUNDACH)
    // =================================================================================================
    //
    // @Transactional(timeout = 5): Spring zapisuje termin (start + 5 s) i przed każdym zapytaniem przez
    // JdbcTemplate/JdbcClient i zapytaniami JPA ustawia instrukcji JDBC limit (setQueryTimeout) równy czasowi, który
    // JESZCZE ZOSTAŁ — test: StockReader.queryTimeoutWithTransactionTimeout() zwraca 5 (albo mniej, jeśli minęła
    // sekunda). Gdy termin już minął, NASTĘPNE zapytanie kończy się TransactionTimedOutException:
    //   „Transaction timed out: deadline was <data i godzina>”
    //
    // PUŁAPKA: timeout to nie stoper, który przerwie metodę. Test sekcji 6 (StockService.sellWithTimeout, timeout = 1):
    //   pierwszy UPDATE czeka na blokadę drugiego użytkownika — H2 przerywa to czekanie dopiero po SWOIM limicie
    //   (2 s), bo limit zapytania nie skraca czekania na blokadę w H2 (sprawdzone uruchomieniem); dopiero kolejne
    //   zapytanie zgłasza TransactionTimedOutException. Kod Javy między zapytaniami (pętla, wywołanie innego systemu)
    //   też nie zostanie przerwany. Dlaczego to ważne? timeout chroni przed „wiszącymi” zapytaniami, ale nie zastąpi
    //   limitów czasu przy wywołaniach zewnętrznych usług.
    //
    // Domyślny limit dla WSZYSTKICH transakcji: właściwość Boota spring.transaction.default-timeout (np. 7s) —
    // test Tx04ReadOnlyIsolationDefaultTimeoutTest: menedżer transakcji dostaje defaultTimeout = 7.

    // =================================================================================================
    // 7. PUŁAPKA: readOnly, isolation i timeout działają tylko dla NOWEJ transakcji
    // =================================================================================================
    //
    // Metoda z @Transactional(isolation = SERIALIZABLE) wywołana WEWNĄTRZ innej transakcji (REQUIRED → dołącza)
    // działa na poziomie transakcji zewnętrznej (test: READ COMMITTED). Tak samo readOnly: sellInReadOnlyTransaction
    // wywołane z transakcji do zapisu ZAPISUJE sprzedaż (test sekcji 7). Ustawienia bierze się z metody, która
    // transakcję ZACZĘŁA. Dlaczego to pułapka? Adnotacja na metodzie obiecuje coś, czego w tym wywołaniu nie ma.
    // Potrzebujesz innej izolacji na pewno? Nowa transakcja (REQUIRES_NEW — z kosztami z Tx02Propagation).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • readOnly = true: FlushMode.MANUAL + encje tylko do odczytu + Connection.setReadOnly(true) (podpowiedź —
     *     H2 ignoruje). Zmiany encji (także z save()) przepadają po cichu.
     *   • Repozytoria Spring Data: readOnly nad klasą, zwykła transakcja nad save/delete.
     *   • Izolacja: READ_UNCOMMITTED < READ_COMMITTED (domyślny w H2) < REPEATABLE_READ < SERIALIZABLE;
     *     zjawiska: brudny odczyt, niepowtarzalny odczyt, fantom. Isolation.DEFAULT = poziom bazy.
     *   • Kontekst utrwalania: drugi findById zwraca ten sam obiekt ze starym stanem; świeży stan → refresh / SQL.
     *   • Utracona aktualizacja: atomowy UPDATE · @Version (ObjectOptimisticLockingFailureException przy commicie,
     *     ponawiaj w NOWEJ transakcji) · @Lock(PESSIMISTIC_WRITE) · wyższa izolacja (zależnie od bazy).
     *   • timeout (sekundy): pozostały czas → setQueryTimeout; po terminie następne zapytanie →
     *     TransactionTimedOutException; nic nie jest przerywane „w locie”. Domyślnie: spring.transaction.default-timeout.
     *   • readOnly/isolation/timeout liczą się tylko w metodzie, która ZACZYNA transakcję.
     *
     * PYTANIA KONTROLNE:
     *   1. Co robi readOnly = true z sesją Hibernate i z połączeniem JDBC? Czy blokuje zapis w H2?
     *   2. Co się stanie? reports.sellInReadOnlyTransaction("ELE-001", 1) przy stanie 7 — co zwróci metoda i ile
     *      sztuk będzie w bazie?
     *   3. ZNAJDŹ BŁĄD: {@code @Transactional(readOnly = true) public Report monthly() { ... stockService.sell(sku, 1);
     *      ... }} — programista „przy okazji” rejestruje sprzedaż próbki w raporcie, a sprzedaż nigdy nie trafia do bazy.
     *   4. Co się stanie? reader.readStockTwiceReadCommitted("ELE-001", m) i readStockTwiceRepeatableRead("ELE-001", m),
     *      gdzie m to „drugi użytkownik sprzedaje 1 szt. i zatwierdza”, a stan początkowy to 7 — jakie listy wrócą?
     *   5. Co się stanie? stockService.sellWithPause("ELE-001", 1, m) przy stanie 7 (m jak wyżej) — ile sztuk będzie
     *      w bazie i jak się nazywa to zjawisko?
     *   6. Czym różni się blokowanie optymistyczne od pesymistycznego i kiedy wybrać które? (PYTANIE REKRUTACYJNE)
     *   7. Dlaczego @Transactional(isolation = Isolation.SERIALIZABLE) na metodzie wołanej z innej transakcji nic
     *      nie zmienia?
     *   8. Metoda ma timeout = 1, a pierwsze zapytanie czeka 2 s na blokadę. Kiedy i jakim wyjątkiem Spring zgłosi
     *      przekroczenie czasu?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — klasa Tx04Exercises (ten pakiet), testy: Tx04ReadOnlyIsolationExercisesTest
    // =================================================================================================
    //
    //   ĆWICZENIE 1 (łatwe): Tx04Exercises.readStockTwiceStable — dwa odczyty z tym samym wynikiem (izolacja).
    //   ĆWICZENIE 2 (średnie): Tx04Exercises.sellWithPauseSafely — sprzedaż bez utraconej aktualizacji.
    //   ĆWICZENIE 3 (trudniejsze): Tx04Exercises.sellVersionedWithRetry — ponowienie po konflikcie @Version.
    //   Rozwiązania: s10_transactions.solutions.tx04_read_only_isolation.Tx04ReadOnlyIsolationSolutions.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Sesja: FlushMode.MANUAL (brak flush) i setDefaultReadOnly(true) (encje bez dirty checking). Połączenie:
     *      setReadOnly(true) na czas transakcji. H2 tej podpowiedzi nie respektuje — ręczny SQL UPDATE przechodzi.
     *      Sprawdzone testem readOnlyUnderTheHood.
     *   2. Zwróci 6 (stan obiektu w pamięci), a w bazie zostanie 7 — zmiana encji w transakcji readOnly nie jest
     *      zapisywana. Sprawdzone testem writesInReadOnlyAreSilentlyLost.
     *   3. stockService.sell (REQUIRED) dołącza do transakcji raportu, która jest tylko do odczytu — zmiana encji
     *      przepada po cichu. Naprawa: sprzedaż poza raportem albo w osobnej transakcji (REQUIRES_NEW), a raport bez
     *      efektów ubocznych. Sprawdzone testem writesInReadOnlyAreSilentlyLost (reportThatSells).
     *   4. READ_COMMITTED → [7, 6] (niepowtarzalny odczyt), potem REPEATABLE_READ → [6, 6]. Sprawdzone testem
     *      nonRepeatableRead.
     *   5. 6 zamiast 5 — utracona aktualizacja (lost update): nasza transakcja zapisała 7 - 1, nadpisując sprzedaż
     *      drugiego użytkownika. Sprawdzone testem lostUpdateAndAtomicUpdate.
     *   6. Optymistyczne (@Version) nie blokuje — wykrywa konflikt przy zapisie i zgłasza wyjątek; dobre, gdy konflikty
     *      są rzadkie (edycja danych). Pesymistyczne (@Lock(PESSIMISTIC_WRITE)) blokuje wiersz od odczytu — inni
     *      czekają; dobre, gdy konflikt jest prawie pewny i sekcja krótka. Dla liczników najlepszy atomowy UPDATE.
     *   7. Metoda dołącza (REQUIRED) do istniejącej transakcji, a poziom izolacji ustawia się tylko przy STARCIE
     *      transakcji — obowiązuje poziom transakcji zewnętrznej. Sprawdzone testem attributesIgnoredWhenJoining.
     *   8. Dopiero przy NASTĘPNYM zapytaniu po upływie terminu: TransactionTimedOutException („Transaction timed out:
     *      deadline was …”). Samo czekanie na blokadę kończy limit H2 (2 s). Sprawdzone testem
     *      timeoutDetectedAtNextStatement.
     */
    // </editor-fold>
}
