package s10_transactions.tx04_read_only_isolation;

/**
 * <pre>
 * TEMAT: {@code readOnly}, poziomy izolacji i blokady — co widzi i co może zepsuć druga transakcja, która działa
 *        w tym samym czasie: dirty read, non-repeatable read, phantom read, lost update; {@code @Version}
 *        (blokowanie optymistyczne) i {@code @Lock(PESSIMISTIC_WRITE)} (blokowanie pesymistyczne)
 *        (read-only = tylko do odczytu; isolation = izolacja, odgrodzenie; lock = blokada;
 *         optimistic = optymistyczne; pessimistic = pesymistyczne)
 *
 * W SKRÓCIE:
 *   {@code @Transactional(readOnly = true)} w aplikacji JPA wyłącza zapisywanie zmian encji (Hibernate: FlushMode
 *   MANUAL i sesja read-only) i daje bazie wskazówkę {@code Connection.setReadOnly(true)} — ale NIE jest zabezpieczeniem:
 *   jawny UPDATE przez JDBC w H2 przechodzi. Izolacja mówi, ile cudzych, równoległych zmian widzi transakcja; domyślnie
 *   (H2, PostgreSQL) READ COMMITTED — widać tylko dane zatwierdzone, ale ten sam odczyt powtórzony może dać inny wynik,
 *   a schemat „przeczytaj — zmień — zapisz” gubi cudze zmiany (lost update). Na lost update są dwa leki:
 *   {@code @Version} (wykryj konflikt przy zapisie) albo {@code select … for update} (zablokuj wiersz przy odczycie).
 *
 * ANALOGIA: wspólny arkusz z budżetem domowym.
 *   READ COMMITTED — widzisz tylko to, co ktoś już zapisał (Ctrl+S), ale gdy spojrzysz drugi raz, liczby mogą być
 *   inne. REPEATABLE READ — robisz sobie zrzut ekranu i do końca pracy patrzysz na zrzut. Lost update — Ty i siostra
 *   otwieracie arkusz, widzicie 100 zł, Ty wpisujesz 130, ona 150; kto zapisze później, ten „wygrywa”, a druga zmiana
 *   znika. {@code @Version} — arkusz ma numer wersji: zapis się nie uda, jeśli ktoś zapisał w międzyczasie (odśwież
 *   i spróbuj jeszcze raz). {@code for update} — bierzesz arkusz „na wyłączność”: siostra czeka, aż skończysz.
 *
 * JAK TO DZIAŁA:
 *   1. Proxy {@code @Transactional} przekazuje menedżerowi definicję transakcji: propagację, izolację, readOnly, timeout.
 *   2. {@code JpaTransactionManager} → {@code HibernateJpaDialect.beginTransaction}: przy readOnly ustawia sesji
 *      {@code FlushMode.MANUAL} i {@code setDefaultReadOnly(true)}, a połączeniu — przez
 *      {@code DataSourceUtils.prepareConnectionForTransaction} — {@code setReadOnly(true)} i ewentualnie
 *      {@code setTransactionIsolation(…)}. Timeout trafia do transakcji Hibernate ({@code Transaction.setTimeout}).
 *   3. Po commicie/rollbacku menedżer przywraca połączeniu poprzednie ustawienia — zanim wróci ono do puli.
 *   4. Izolację realizuje BAZA (H2: MVCC — wiele wersji wiersza; blokady wierszy przy zapisie). Spring tylko ją ustawia.
 *   5. {@code @Version} i {@code @Lock} realizuje Hibernate: dopisuje do SQL warunek na wersję albo {@code for update}.
 *
 * SŁÓWKA:
 *   read-only = tylko do odczytu; flush = wypchnięcie zmian do bazy; dirty checking = wykrywanie zmian w encjach;
 *   isolation level = poziom izolacji; dirty read = brudny odczyt (cudzych NIEzatwierdzonych danych);
 *   non-repeatable read = niepowtarzalny odczyt; phantom read = odczyt fantomów (nowych wierszy); lost update = utracona
 *   aktualizacja; snapshot = migawka; optimistic locking = blokowanie optymistyczne; pessimistic locking = blokowanie
 *   pesymistyczne; row lock = blokada wiersza; lock timeout = limit czasu czekania na blokadę; nowait = nie czekaj;
 *   deadlock = zakleszczenie; retry = ponowienie próby; replica = replika (kopia bazy tylko do odczytu).
 *
 * ZOBACZ TEŻ: s10_transactions/Tx01Transactional (podstawy, setRollbackOnly), s10_transactions/Tx02Propagation
 *             (REQUIRES_NEW — drugie połączenie; zakleszczenie na tym samym wierszu), s10_transactions/Tx05ProgrammaticTransactions
 *             (TransactionTemplate z własną izolacją), s07_data_jdbc/Jdbc05Repository (kolumna version „ręcznie”),
 *             s08_data_jpa/Jpa03QueryMethods (zapytania pochodne), t29_jdbc_databases/Jdbc04Transactions
 *             (setTransactionIsolation w czystym JDBC), t21_concurrency/Concurrency02RaceConditions (wyścig w pamięci
 *             — lost update to ten sam problem, tylko w bazie).
 * </pre>
 */
public final class Tx04ReadOnlyIsolation {

    private Tx04ReadOnlyIsolation() {
    }

    // =================================================================================================
    // 1. readOnly = true — co robi w JPA, w JDBC i czego NIE robi (ReadOnlyService)
    // =================================================================================================
    //
    // ReadOnlyService.renameInReadOnly: {@code @Transactional(readOnly = true)}, wczytanie portfela, zmiana właściciela.
    // Zdjęcie stanu z wnętrza transakcji (sprawdzone testem):
    //   isCurrentTransactionReadOnly() = true    — Spring wie, że to transakcja tylko do odczytu
    //   session.getHibernateFlushMode() = MANUAL — Hibernate NIE robi flush przy commicie (ani przed zapytaniami)
    //   session.isDefaultReadOnly() = true       — encje są wczytywane jako read-only…
    //   session.isReadOnly(wallet) = true        — …więc Hibernate nie trzyma ich kopii do porównania (mniej pamięci)
    //   connection.isReadOnly() = true           — Spring wywołał Connection.setReadOnly(true)
    // W obiekcie właściciel jest zmieniony, w bazie — NIE (test). Nawet jawne {@code em.flush()} nic nie zapisze:
    // encja read-only nie podlega dirty checking (test renameAndFlushInReadOnly).
    //
    // Po co więc readOnly?
    //   • wydajność: brak flush i kopii stanu encji — przy dużych odczytach (raporty, listy) to realna oszczędność;
    //   • czytelność: „ta metoda tylko czyta” widać w sygnaturze;
    //   • wskazówka dla bazy/sterownika ({@code Connection.setReadOnly}) — niektóre bazy optymalizują takie transakcje,
    //     a własny routing DataSource (np. {@code AbstractRoutingDataSource} — przełączanie źródeł danych) może kierować
    //     je do REPLIKI bazy tylko do odczytu (wzmianka — to architektura, nie domyślne zachowanie Boota).
    // Spring Data robi to samo za Ciebie: metody odczytu {@code SimpleJpaRepository} (findById, findAll…) mają
    // {@code @Transactional(readOnly = true)} — w logu: „Creating new transaction with name
    // [org.springframework.data.jpa.repository.support.SimpleJpaRepository.findById]: PROPAGATION_REQUIRED,
    // ISOLATION_DEFAULT,readOnly” (test).
    //
    // PUŁAPKA: readOnly to NIE jest zabezpieczenie przed zapisem. Jawny UPDATE przez JdbcClient w transakcji read-only
    //   (ReadOnlyService.renameWithJdbcInReadOnly) w H2 PRZECHODZI i zostaje zatwierdzony (test) — H2 ignoruje
    //   {@code setReadOnly(true)}. PostgreSQL by go odrzucił („cannot execute UPDATE in a read-only transaction”).
    //   Dlaczego ważne? Zachowanie zależy od bazy i sterownika; prawdziwą ochroną są uprawnienia użytkownika bazy.
    //
    // DOBRA PRAKTYKA: metody serwisu, które tylko czytają, oznaczaj {@code @Transactional(readOnly = true)} (albo całą
    //   klasę readOnly, a metody piszące zwykłym {@code @Transactional}). Dlaczego? Leniwe relacje działają w obrębie
    //   metody (s09_jpa_relations/Rel04LazyLoading), Hibernate oszczędza pracę, a intencja jest jasna.

    // =================================================================================================
    // 2. Zjawiska współbieżności i poziomy izolacji (IsolationShowcase, OtherUser)
    // =================================================================================================
    //
    //   Zjawisko               co się dzieje                                                 przykład
    //   dirty read             widzę cudze NIEzatwierdzone zmiany (które mogą zostać wycofane)  widzę 999.00, którego nie było
    //   non-repeatable read    ten sam wiersz czytany dwa razy daje różne wartości             100.00, potem 150.00
    //   phantom read           to samo zapytanie WHERE zwraca nowe/zniknięte wiersze           COUNT(*) = 2, potem 3
    //   lost update            dwie transakcje „przeczytaj — zmień — zapisz”; jedna zmiana ginie 100 + 50 + 30 = 130 (!)
    //
    //   Poziom (Isolation.…)   dirty read   non-repeatable read   phantom read   (wg standardu SQL)
    //   READ_UNCOMMITTED       możliwy      możliwy               możliwy
    //   READ_COMMITTED         —            możliwy               możliwy        ← domyślny w H2 i PostgreSQL
    //   REPEATABLE_READ        —            —                     możliwy        ← domyślny w MySQL (InnoDB)
    //   SERIALIZABLE           —            —                     —              jak gdyby transakcje szły po kolei
    //   Lost update standard SQL opisuje inaczej; w praktyce READ_COMMITTED go dopuszcza, a wyższe poziomy w H2
    //   i PostgreSQL go WYKRYWAJĄ (błąd przy zapisie — sekcja 3). {@code Isolation.DEFAULT} = „nie zmieniaj, użyj
    //   ustawienia bazy”.
    //
    // Sprawdzone testami (H2 2.4; poziom czytany z bazy: {@code INFORMATION_SCHEMA.SESSIONS.ISOLATION_LEVEL}):
    //   • {@code @Transactional} → „READ COMMITTED”; {@code @Transactional(isolation = Isolation.REPEATABLE_READ)} →
    //     „REPEATABLE READ” — Spring naprawdę przestawia połączenie;
    //   • non-repeatable read (readBalanceTwice): pierwszy odczyt 100.00, w międzyczasie OtherUser (REQUIRES_NEW —
    //     osobna transakcja na drugim połączeniu, zatwierdzona od razu) dodaje 50, drugi odczyt 150.00;
    //     z REPEATABLE_READ — oba odczyty 100.00 (H2 czyta z migawki — snapshot);
    //   • dirty read (dirtyRead): transakcja ustawia saldo 999.00 bez commitu; drugi użytkownik z READ_COMMITTED widzi
    //     100.00, z READ_UNCOMMITTED widzi 999.00 — po wycofaniu w bazie jest 100.00, czyli tamten widział „widmo”.
    //
    // Jak to możliwe w JEDNYM wątku? Transakcja A woła w połowie {@code meanwhile.run()} (meanwhile = w międzyczasie),
    // a ten uruchamia metodę OtherUser z REQUIRES_NEW: A jest zawieszona, B dostaje własne połączenie i kończy się
    // commitem, potem A wraca. Dla bazy to dwie zwykłe, równoległe transakcje — a kolejność jest zawsze ta sama.
    // DOBRA PRAKTYKA: tak testuj współbieżność — ręczne przeplatanie zamiast wątków i {@code Thread.sleep}.
    // Dlaczego? Test z prawdziwymi wątkami i sleepami raz przechodzi, raz nie (zależy od obciążenia maszyny).
    //
    // PUŁAPKA: przez JPA ten sam scenariusz (readBalanceTwiceWithJpa) daje 100.00 i 100.00 nawet przy READ COMMITTED.
    //   Dlaczego? Drugie {@code em.find} nie pyta bazy — zwraca obiekt z kontekstu utrwalania (pamięci podręcznej
    //   pierwszego poziomu). To nie izolacja bazy, tylko pamięć Hibernate — i do tego NIEAKTUALNA (w bazie jest 150.00).
    //
    // PYTANIE REKRUTACYJNE: „Wymień poziomy izolacji i zjawiska, którym zapobiegają. Jaki jest domyślny?”

    // =================================================================================================
    // 3. Lost update — jedna zmiana ginie; @Version (blokowanie optymistyczne) (TopUpService)
    // =================================================================================================
    //
    //   PRZED (TopUpService.topUp, portfel BEZ wersji, saldo 100.00):
    //     A: SELECT … (100.00)
    //        B (OtherUser.topUp): SELECT (100.00) → UPDATE wallet SET balance = 150.00 → COMMIT
    //     A: 100.00 + 30 → UPDATE wallet SET balance = 130.00 … WHERE id = ? → COMMIT
    //   Wynik: 130.00 zamiast 180.00 — doładowanie B zniknęło, NIKT nie dostał błędu (test). Hibernate wysyła
    //   {@code update wallet set balance=?,owner=? where id=?} — wartość policzoną w pamięci, nie „balance + 30”.
    //
    //   PO (TopUpService.topUpVersioned, encja VersionedWallet z polem {@code @Version long version}):
    //     B: {@code update versioned_wallet set balance=?,owner=?,version=? where id=? and version=?} (0 → 1) → COMMIT
    //     A: ten sam UPDATE z warunkiem {@code version = 0} → 0 wierszy → wyjątek przy commicie:
    //     {@code org.springframework.orm.ObjectOptimisticLockingFailureException: Unexpected row count (expected row}
    //     {@code count 1 but was 0) [update versioned_wallet …] for entity [… VersionedWallet with id '1']},
    //     przyczyna: {@code org.hibernate.StaleStateException} (stale = nieświeży). Saldo 150.00, wersja 1 (test).
    //   Konflikt jest WYKRYTY, a transakcja A wycofana. Co dalej? Zwykle: ponowić całą operację (nowa transakcja,
    //   świeży odczyt — ćwiczenie 4) albo pokazać użytkownikowi „ktoś zmienił dane, odśwież”.
    //   Porównaj: s07_data_jdbc/Jdbc05Repository — ta sama kolumna version, ale UPDATE z warunkiem pisany ręcznie.
    //
    //   Dla porównania (TopUpService.topUpRepeatableRead): portfel BEZ wersji, transakcja A z REPEATABLE_READ.
    //   H2 wykrywa, że A chce zmienić wiersz zmieniony i zatwierdzony po zrobieniu jej migawki:
    //   {@code org.springframework.dao.CannotAcquireLockException: could not execute statement [Deadlock detected.}
    //   {@code The current transaction was rolled back. …]} — saldo 150.00 (test). PostgreSQL w tej sytuacji zgłasza
    //   „could not serialize access due to concurrent update”. Działa, ale zależy od bazy — {@code @Version} działa
    //   tak samo wszędzie.
    //
    // DOBRA PRAKTYKA: {@code @Version} dla encji edytowanych przez wielu użytkowników (formularze, koszyki, stany).
    //   Dlaczego? Nie blokuje nikogo (zero czekania), konflikt jest rzadki, a gdy się zdarzy — jest wykryty.
    // PUŁAPKA: ponawianie po ObjectOptimisticLockingFailureException WEWNĄTRZ tej samej transakcji nie działa —
    //   transakcja jest już wycofana, a kontekst utrwalania trzyma stare dane. Dlaczego? Ponowienie musi być POZA
    //   metodą transakcyjną (osobny bean / pętla wokół wywołania przez proxy).
    // PYTANIE REKRUTACYJNE: „Czym różni się blokowanie optymistyczne od pesymistycznego? Kiedy które?”

    // =================================================================================================
    // 4. Blokowanie pesymistyczne — @Lock(PESSIMISTIC_WRITE) → SELECT … FOR UPDATE (WalletRepository, LockingService)
    // =================================================================================================
    //
    //   {@code @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<Wallet> findForUpdateById(Long id);}
    //   SQL (sprawdzone testem): {@code select w1_0.id,w1_0.balance,w1_0.owner from wallet w1_0 where w1_0.id=? for update}
    //   Wiersz jest zablokowany od odczytu do końca transakcji: inna transakcja, która chce go zmienić albo też
    //   zablokować, CZEKA. Lost update nie może się zdarzyć, bo nikt nie zmieni wiersza między naszym odczytem a zapisem.
    //
    //   Limit czasu czekania: podpowiedź {@code @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout",}
    //   {@code value = "0"))} → w H2: {@code … for update nowait} (sprawdzone testem SQL). Test: A blokuje portfel
    //   (withdrawWithLock), w międzyczasie B (OtherUser.topUpWithLockNoWait) chce go zablokować bez czekania →
    //   {@code org.springframework.dao.CannotAcquireLockException: JDBC exception executing SQL [Timeout trying to lock}
    //   {@code table "WALLET" …]}; wyjątek wylatuje też z A, więc A jest wycofana — saldo 100.00.
    //   Zwykły UPDATE od B (bez podpowiedzi) czeka tyle, ile pozwala baza: H2 domyślnie 2 sekundy (ustawienie
    //   {@code LOCK_TIMEOUT}). Test z {@code ;LOCK_TIMEOUT=200} w adresie bazy (Tx04SpecialSetupTest):
    //   {@code CannotAcquireLockException: could not execute statement [Timeout trying to lock table "WALLET" …]}.
    //   W „prawdziwym” życiu B jest w innym wątku i po prostu czeka na commit A, a potem czyta świeże saldo.
    //
    // PUŁAPKA: {@code findForUpdateById} wywołane BEZ transakcji → {@code InvalidDataAccessApiUsageException: No active}
    //   {@code transaction} (przyczyna: {@code jakarta.persistence.TransactionRequiredException}) — test. Dlaczego?
    //   Blokada żyje do końca transakcji, więc bez transakcji nie ma sensu; a metody zapytań pochodnych (w przeciwieństwie
    //   do findById z SimpleJpaRepository) nie mają własnej transakcji. Blokujące odczyty — tylko w serwisie
    //   z {@code @Transactional}.
    // PUŁAPKA: blokady pesymistyczne = czekanie i ryzyko zakleszczenia (dwie transakcje blokują wiersze w odwrotnej
    //   kolejności). Dlaczego groźne? Długa transakcja trzymająca blokadę zatrzymuje wszystkich, którzy chcą tych wierszy.
    //   Blokuj zawsze w tej samej kolejności (np. rosnąco po id) i krótko.
    // DOBRA PRAKTYKA: pesymistycznie tam, gdzie konflikt jest CZĘSTY i ponawianie drogie (np. ostatnia sztuka towaru,
    //   saldo konta przy wielu równoczesnych wypłatach); optymistycznie ({@code @Version}) — w pozostałych przypadkach.

    // =================================================================================================
    // 5. @Transactional(isolation = …, timeout = …) i dołączanie do trwającej transakcji (WalletWriter)
    // =================================================================================================
    //
    //   {@code @Transactional(isolation = Isolation.SERIALIZABLE)} — WalletWriter.isolationSeenBySerializableMethod:
    //   wywołana bez transakcji → baza pokazuje „SERIALIZABLE”.
    //   {@code @Transactional(timeout = 5)} — limit w SEKUNDACH; Spring przekazuje go transakcji Hibernate
    //   ({@code Transaction.getTimeout()} = 5 — test), a Hibernate ustawia poleceniom JDBC {@code setQueryTimeout}.
    //   UWAGA (sprawdzone w H2): limit zapytania nie skraca czekania na blokadę — o nim decyduje {@code LOCK_TIMEOUT}
    //   bazy (sekcja 4). Globalny domyślny limit: {@code spring.transaction.default-timeout}.
    //
    // PUŁAPKA: izolacja i readOnly działają tylko dla transakcji, która się ZACZYNA. Metoda z SERIALIZABLE wywołana
    //   z trwającej transakcji (IsolationShowcase.isolationSeenByJoiningSerializableMethod, REQUIRED → dołącza) widzi
    //   „READ COMMITTED” — atrybut jest po cichu IGNOROWANY (test). Tak samo readOnly: metoda read-only, która woła
    //   zwykłą metodę „piszącą” innego beana (ReadOnlyService.renameThroughWriter → WalletWriter.rename), wciąga ją
    //   do swojej transakcji read-only — zmiana PRZEPADA bez żadnego wyjątku (test). Dlaczego? Połączenie i sesja
    //   zostały ustawione na początku przez tego, kto transakcję otworzył; uczestnik nic już nie zmienia.
    //   Obrona: {@code JpaTransactionManager.setValidateExistingTransaction(true)} (validate existing transaction =
    //   sprawdzaj trwającą transakcję) — wtedy dołączenie z niezgodnymi ustawieniami rzuca
    //   {@code IllegalTransactionStateException} (Tx04SpecialSetupTest):
    //   {@code Participating transaction with definition [PROPAGATION_REQUIRED,ISOLATION_SERIALIZABLE] specifies}
    //   {@code isolation level which is incompatible with existing transaction: (unknown)} („(unknown)” — zewnętrzna
    //   ma ISOLATION_DEFAULT) oraz {@code Participating transaction with definition [PROPAGATION_REQUIRED,}
    //   {@code ISOLATION_DEFAULT] is not marked as read-only but existing transaction is}.
    //   Boot nie ma na to właściwości ({@code spring.transaction.*} to tylko default-timeout i
    //   rollback-on-commit-failure) — włącza się to beanem {@code TransactionManagerCustomizer} (dostrajacz menedżera;
    //   w Boot 4 pakiet {@code org.springframework.boot.transaction.autoconfigure}):
    //   {@code @Bean TransactionManagerCustomizer<AbstractPlatformTransactionManager> v() { return tm -> tm.setValidateExistingTransaction(true); }}
    //   (test: ValidatingTransactionsConfig). Jeśli metoda NAPRAWDĘ potrzebuje własnej izolacji, daj jej
    //   REQUIRES_NEW — ale pamiętaj o kosztach (s10_transactions/Tx02Propagation, sekcja 3).
    //
    // DOBRA PRAKTYKA: transakcje KRÓTKIE — tylko praca na bazie, żadnych wywołań HTTP, wysyłania e-maili, czekania na
    //   użytkownika w środku. Dlaczego? Transakcja trzyma połączenie z puli i blokady wierszy przez cały czas trwania:
    //   wolna usługa zewnętrzna wydłuża blokady i wyczerpuje pulę dla całej aplikacji. Wywołania zewnętrzne rób przed
    //   transakcją albo po commicie (s10_transactions/Tx05ProgrammaticTransactions, sekcja 4 — afterCommit).
    // DOBRA PRAKTYKA: zostaw domyślną izolację, dopóki nie masz konkretnego problemu. Dlaczego? Wyższe poziomy to więcej
    //   konfliktów i wycofań; lost update rozwiązuje się zwykle {@code @Version} albo {@code for update}, nie SERIALIZABLE.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • readOnly = true (JPA): FlushMode MANUAL + encje read-only (brak zapisu zmian, mniej pamięci) +
     *     Connection.setReadOnly(true) (wskazówka; H2 ją ignoruje — jawny UPDATE przechodzi). Nie zabezpieczenie.
     *   • Spring Data: findById/findAll mają readOnly = true; zapytania pochodne nie mają własnej transakcji.
     *   • Zjawiska: dirty read < non-repeatable read < phantom read; lost update — osobny, najczęstszy w praktyce.
     *   • Domyślnie READ COMMITTED (H2, PostgreSQL); MySQL InnoDB: REPEATABLE READ. Isolation.DEFAULT = ustawienie bazy.
     *   • Lost update: @Version → ObjectOptimisticLockingFailureException (ponów w NOWEJ transakcji);
     *     @Lock(PESSIMISTIC_WRITE) → select … for update (inni czekają); hint jakarta.persistence.lock.timeout = 0
     *     → for update nowait → CannotAcquireLockException.
     *   • JPA cache pierwszego poziomu „powtarza” odczyty niezależnie od izolacji — i może pokazywać stare dane.
     *   • isolation/readOnly/timeout działają tylko dla transakcji, która się ZACZYNA; uczestnik (REQUIRED) je ignoruje
     *     (validateExistingTransaction = true → IllegalTransactionStateException).
     *   • timeout w sekundach; transakcje krótkie, bez HTTP w środku.
     *
     * PYTANIA KONTROLNE:
     *   1. Co się stanie? W {@code @Transactional(readOnly = true)} wczytujesz encję, zmieniasz jej pole i wywołujesz
     *      {@code em.flush()}. Co będzie w bazie po zakończeniu metody?
     *   2. Co się stanie? W tej samej transakcji read-only wykonujesz {@code jdbc.sql("UPDATE …").update()} na H2.
     *      Czy zmiana zostanie zapisana? Czy możesz na tym polegać?
     *   3. Wymień zjawiska współbieżności i poziomy izolacji, które im zapobiegają. Jaki poziom jest domyślny w H2?
     *   4. Co się stanie? Dwie transakcje czytają saldo 100.00; jedna dodaje 50 i zatwierdza, potem druga dodaje 30
     *      i zatwierdza (encja bez {@code @Version}). Jakie saldo? A z {@code @Version}?
     *   5. Kiedy wybierzesz blokowanie optymistyczne, a kiedy pesymistyczne? Jaki SQL generuje
     *      {@code @Lock(LockModeType.PESSIMISTIC_WRITE)} w H2?
     *   6. ZNAJDŹ BŁĄD: {@code @Transactional(readOnly = true) void importAll() { for (…) writer.save(item); }}, gdzie
     *      {@code writer.save} ma zwykłe {@code @Transactional} — „import kończy się bez błędu, ale w bazie nic nie ma”.
     *   7. ZNAJDŹ BŁĄD: {@code @Transactional void transfer() { … reportService.monthlyReport(); … }}, a
     *      {@code monthlyReport} ma {@code @Transactional(isolation = SERIALIZABLE)} — „raport miał mieć SERIALIZABLE”.
     *   8. Dlaczego w środku transakcji nie należy wywoływać usług HTTP?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w pliku ReadOnlyIsolationExercises.java, testy: Tx04ReadOnlyIsolationExercisesTest
    // (pełna aplikacja, bez transakcji testu).
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Nic się nie zmieni: encja wczytana w sesji read-only nie podlega dirty checking, więc nawet jawny flush nie
     *      wysyła UPDATE; przy commicie flush w ogóle się nie odbywa (FlushMode MANUAL) — test sekcji 1.
     *   2. W H2 tak — UPDATE przechodzi i zostaje zatwierdzony, bo H2 ignoruje Connection.setReadOnly(true) (test).
     *      Nie wolno na tym polegać: inna baza (np. PostgreSQL) odrzuci zapis w transakcji read-only. readOnly to
     *      wskazówka i optymalizacja, nie zabezpieczenie.
     *   3. Dirty read (zapobiega READ_COMMITTED i wyżej), non-repeatable read (REPEATABLE_READ i wyżej), phantom read
     *      (SERIALIZABLE). H2 (i PostgreSQL) domyślnie: READ COMMITTED (test: INFORMATION_SCHEMA.SESSIONS).
     *   4. Bez @Version: 130.00 — lost update, doładowanie o 50 zginęło bez błędu. Z @Version: druga transakcja dostaje
     *      ObjectOptimisticLockingFailureException przy commicie i jest wycofana; saldo 150.00, wersja 1 (test sekcji 3);
     *      operację trzeba ponowić w nowej transakcji (ćwiczenie 4).
     *   5. Optymistyczne (@Version) — konflikty rzadkie, nie chcesz nikogo blokować, ponowienie jest tanie.
     *      Pesymistyczne — konflikty częste/kosztowne (ostatnia sztuka, saldo przy wielu wypłatach), transakcja krótka.
     *      SQL: {@code select … from wallet w1_0 where w1_0.id=? for update} (z lock.timeout = 0: {@code for update nowait}).
     *   6. writer.save dołącza (REQUIRED) do transakcji read-only importAll — sesja ma FlushMode MANUAL, więc zmiany nie
     *      są zapisywane i nikt nie rzuca wyjątku (test sekcji 5). Naprawa: importAll bez readOnly (to metoda pisząca);
     *      validateExistingTransaction = true zamieniłby cichą utratę danych w IllegalTransactionStateException.
     *   7. monthlyReport dołącza do trwającej transakcji transfer (READ COMMITTED), więc jego SERIALIZABLE jest ignorowane
     *      (test: baza pokazuje READ COMMITTED). Własna izolacja wymaga własnej transakcji: wywołanie spoza transakcji
     *      albo REQUIRES_NEW (z kosztem drugiego połączenia).
     *   8. Transakcja trzyma połączenie z puli i blokady wierszy przez cały czas — wolna odpowiedź HTTP wydłuża blokady
     *      (inni czekają) i może wyczerpać pulę; do tego wywołania HTTP nie da się wycofać rollbackiem. Wywołuj je przed
     *      transakcją albo po commicie (Tx05, afterCommit).
     */
    // </editor-fold>
}
