package s10_transactions.tx01_transactional;

/**
 * <pre>
 * TEMAT: {@code @Transactional} — transakcja w Springu: commit, rollback i KTÓRE wyjątki wycofują zmiany
 *        (transaction = transakcja; transactional = transakcyjny; commit = zatwierdzenie; rollback = wycofanie)
 *
 * W SKRÓCIE:
 *   Transakcja to kilka operacji na bazie, które udają się WSZYSTKIE albo ŻADNA. W Springu nie piszesz
 *   {@code setAutoCommit(false)}, {@code commit()} ani {@code rollback()} ręcznie (jak w czystym JDBC —
 *   t29_jdbc_databases/Jdbc04Transactions). Stawiasz {@code @Transactional} na metodzie serwisu, a Spring owija bean
 *   w proxy (pośrednika), które przed metodą otwiera transakcję, a po niej robi commit albo rollback. Domyślna reguła:
 *   rollback dla {@code RuntimeException} i {@code Error}, COMMIT dla wyjątków sprawdzanych (checked) — to najczęstsza
 *   niespodzianka. Wyjątek złapany wewnątrz metody nie wycofuje niczego, bo proxy go nie widzi.
 *
 * ANALOGIA: przeprowadzka z jednego mieszkania do drugiego z ekipą, która ma kartę „cofnij”.
 *   Albo wszystkie meble lądują w nowym mieszkaniu (commit), albo — gdy w połowie okaże się, że nowe mieszkanie
 *   jest zamknięte — ekipa odwozi wszystko z powrotem (rollback). Bez ekipy (bez transakcji) kanapa stoi na ulicy:
 *   ze starego mieszkania zniknęła, do nowego nie dotarła. Kierownik ekipy (proxy) reaguje tylko na problem, który
 *   mu ZGŁOSISZ (wyjątek wychodzący z metody). Jeśli schowasz problem w kieszeni (catch), kierownik uzna, że wszystko
 *   poszło dobrze.
 *
 * JAK TO DZIAŁA:
 *   1. Przy starcie Boot widzi JPA na ścieżce klas i tworzy bean {@code transactionManager} typu
 *      {@code JpaTransactionManager} (menedżer transakcji JPA; interfejs: {@code PlatformTransactionManager}) —
 *      sprawdzone testem. Autokonfiguracja transakcji włącza też przetwarzanie {@code @Transactional}.
 *   2. Bean z {@code @Transactional} (np. TransferService) dostaje proxy CGLIB — podklasę wygenerowaną w czasie
 *      działania: {@code TransferService$$SpringCGLIB$$0}. W kontekście siedzi proxy, nie Twój obiekt.
 *   3. Wywołanie {@code transfers.transfer(...)} trafia do proxy → {@code TransactionInterceptor} (przechwytywacz)
 *      → {@code transactionManager.getTransaction(...)}: otwiera EntityManager i JEDNO połączenie JDBC, wyłącza
 *      autocommit i przypina je do bieżącego wątku. Repozytoria i JdbcClient w tym wątku używają tego połączenia.
 *   4. Metoda kończy się normalnie → commit. Wylatuje RuntimeException/Error → rollback, wyjątek leci dalej do
 *      wywołującego. Wylatuje wyjątek checked → COMMIT (domyślnie!), wyjątek też leci dalej.
 *   Log ({@code logging.level.org.springframework.orm.jpa.JpaTransactionManager=debug}, sprawdzone testem):
 *     {@code Creating new transaction with name [s10_transactions.tx01_transactional.TransferService.transfer]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT}
 *     {@code Opened new EntityManager [SessionImpl(…)] for JPA transaction}
 *     {@code Participating in existing transaction}        ← repozytorium dołącza do naszej transakcji
 *     {@code Initiating transaction commit} / {@code Initiating transaction rollback}
 *
 * SŁÓWKA:
 *   transaction = transakcja; commit = zatwierdzenie; rollback = wycofanie; transaction manager = menedżer transakcji;
 *   proxy = pośrednik; interceptor = przechwytywacz; checked exception = wyjątek sprawdzany (kompilator wymusza obsługę);
 *   unchecked exception = wyjątek niesprawdzany (RuntimeException); rollback-only = „tylko do wycofania” (flaga);
 *   rollback for = wycofaj dla; no rollback for = nie wycofuj dla; read only = tylko do odczytu;
 *   wallet = portfel; balance = saldo; fee = opłata; transfer = przelew; withdraw = wypłać; deposit = wpłać.
 *
 * ZOBACZ TEŻ: t29_jdbc_databases/Jdbc04Transactions (transakcje w czystym JDBC: setAutoCommit, commit, rollback),
 *             s07_data_jdbc/Jdbc05Repository (pierwszy {@code @Transactional} w kursie),
 *             s09_jpa_relations/Rel04LazyLoading (transakcja = życie sesji Hibernate; NOT_SUPPORTED w testach),
 *             s05_validation/Val05ServiceValidation (to samo proxy przy walidacji serwisów),
 *             s10_transactions/Tx02Propagation (co, gdy jedna metoda transakcyjna woła drugą),
 *             s10_transactions/Tx03ProxyPitfalls (kiedy proxy NIE działa),
 *             s10_transactions/Tx05ProgrammaticTransactions (to samo bez adnotacji — TransactionTemplate).
 * </pre>
 */
public final class Tx01Transactional {

    private Tx01Transactional() {
    }

    // =================================================================================================
    // 1. PROBLEM: przelew to dwa UPDATE-y. Awaria między nimi — bez transakcji pieniądze znikają (TransferService)
    // =================================================================================================
    //
    // Przelew 100.00 z portfela 1 (500.00) do portfela 99 (nie istnieje):
    //   wypłata z 1 → save → UPDATE wallet SET balance=400.00 WHERE id=1 → … → findById(99) → WalletNotFoundException
    //
    //   PRZED: {@code transferWithoutTransaction} — bez adnotacji. Każde {@code save()} repozytorium ma WŁASNĄ transakcję
    //     (SimpleJpaRepository jest {@code @Transactional}) i zatwierdza od razu. Wynik w bazie (test): portfel 1 = 400.00.
    //     Sto złotych wyparowało — nie ma ich ani w 1, ani w 99.
    //   PO: {@code transfer} — ten sam kod + {@code @Transactional}. Repozytorium DOŁĄCZA do transakcji serwisu
    //     („Participating in existing transaction”), więc nic nie jest zatwierdzane, dopóki metoda serwisu się nie
    //     skończy. Wyjątek → rollback → portfel 1 = 500.00 (test). Udany przelew 1 → 2: 400.00 i 200.00 (test).
    //
    // ACID — cztery obietnice transakcji (PYTANIE REKRUTACYJNE: „Co oznacza ACID?”):
    //   Litera  Angielski     Po polsku       Co znaczy na przykładzie przelewu
    //   A       Atomicity     niepodzielność  oba UPDATE-y albo żaden (ta sekcja)
    //   C       Consistency   spójność        po transakcji dane spełniają reguły (np. CHECK balance >= 0)
    //   I       Isolation     izolacja        inne transakcje nie widzą „połowy przelewu” (s10_transactions/Tx04ReadOnlyIsolation)
    //   D       Durability    trwałość        po commicie dane przetrwają awarię serwera (robi to baza)
    //
    // DOBRA PRAKTYKA: każdy przypadek użycia, który zmienia KILKA rzeczy (przelew, złożenie zamówienia), ma jedną
    //   transakcję na poziomie metody serwisu. Dlaczego? Tylko serwis wie, które zmiany muszą się udać razem —
    //   repozytorium widzi pojedynczy zapis.

    // =================================================================================================
    // 2. JAK TO DZIAŁA pod spodem: proxy → PlatformTransactionManager → jedno połączenie na transakcję
    // =================================================================================================
    //
    //   wywołujący ──► proxy TransferService$$SpringCGLIB$$0 ──► TransactionInterceptor
    //                     │ getTransaction(): nowy EntityManager + połączenie JDBC (autocommit = false), przypięte do wątku
    //                     ├──► prawdziwy TransferService.transfer(...)  (repozytoria używają TEGO połączenia)
    //                     │ metoda OK → commit();   RuntimeException/Error → rollback() i wyjątek leci dalej
    //                     ▼
    //   wywołujący dostaje wynik albo wyjątek
    //
    // Sprawdzone testami:
    //   • {@code AopUtils.isAopProxy(transfers)} → true, {@code AopUtils.isCglibProxy(transfers)} → true,
    //     {@code transfers.getClass().getName()} → {@code s10_transactions.tx01_transactional.TransferService$$SpringCGLIB$$0}
    //     (znacznik {@code $$SpringCGLIB$$} — tak w Framework 6 i 7; w starszych materiałach zobaczysz
    //     {@code $$EnhancerBySpringCGLIB$$} — to dawny Spring 5).
    //   • bean {@code PlatformTransactionManager} → {@code org.springframework.orm.jpa.JpaTransactionManager}.
    //     Bez JPA (sam JDBC) Boot utworzyłby {@code JdbcTransactionManager} (sprawdzone w kodzie autokonfiguracji Boot 4.1.1).
    //   • w metodzie z {@code @Transactional}: {@code TransactionSynchronizationManager.isActualTransactionActive()} → true,
    //     {@code getCurrentTransactionName()} → {@code s10_transactions.tx01_transactional.TransactionInspector.insideTransaction};
    //     w metodzie bez adnotacji: false i null (rekord TransactionInfo).
    //   • JPA (zapytanie natywne) i JdbcClient w JEDNEJ transakcji widzą tę samą sesję H2 ({@code SELECT SESSION_ID()}) —
    //     JpaTransactionManager udostępnia swoje połączenie kodowi JDBC („Exposing JPA transaction as JDBC” w logu).
    //     Dlatego JPA i JdbcClient można mieszać w jednej transakcji.
    //
    // PUŁAPKA: transakcja zajmuje połączenie z puli (HikariCP — pula połączeń) przez CAŁY czas trwania metody.
    //   Dlaczego ważne? Wolne wywołanie innej usługi (HTTP) w środku metody transakcyjnej trzyma połączenie bezczynnie;
    //   pod obciążeniem pula się wyczerpuje. Transakcje mają być KRÓTKIE (więcej: s10_transactions/Tx04ReadOnlyIsolation).

    // =================================================================================================
    // 3. KTÓRE wyjątki wycofują transakcję — domyślna reguła, rollbackFor, noRollbackFor (FeeService)
    // =================================================================================================
    //
    // Każda metoda FeeService najpierw pobiera opłatę 2.00 z portfela 2 (100.00), potem rzuca wyjątek. Wyniki w bazie (testy):
    //   Metoda / adnotacja                                           wyjątek                          saldo po    co się stało
    //   {@code withdrawWithFee} — {@code @Transactional}                             InsufficientFundsException (checked)  98.00      COMMIT!
    //   {@code withdrawWithFeeOrRollback} — {@code rollbackFor = InsufficientFundsException.class}  ta sama                         100.00     rollback
    //   {@code withdrawOrKeepFee} — {@code noRollbackFor = WithdrawalRejectedException.class}       WithdrawalRejectedException (runtime) 98.00   COMMIT (celowo)
    //   {@code withdrawWithFeeAndCrash} — {@code @Transactional}                     LedgerCorruptedError (Error)     100.00     rollback
    //   (saldo po = wynik pojedynczego wywołania na świeżych danych; w teście wywołania idą po kolei na tym samym portfelu)
    //
    // PUŁAPKA: wyjątek SPRAWDZANY (dziedziczy po {@code Exception}, nie po RuntimeException) domyślnie NIE wycofuje
    //   transakcji. Zmiany sprzed wyjątku zostają ZATWIERDZONE, a wywołujący i tak dostaje wyjątek — myśli, że nic się
    //   nie stało, a opłata poszła. Dlaczego tak? Reguła odziedziczona po EJB: checked = „przewidziany wynik biznesowy”,
    //   po którym aplikacja sama decyduje, co dalej; runtime = „błąd programu”, po którym trzeba wszystko cofnąć.
    //   PYTANIE REKRUTACYJNE: „Czy @Transactional wycofa transakcję po IOException / własnym wyjątku checked?” — NIE.
    //
    // DOBRA PRAKTYKA: wyjątki biznesowe pisz jako RuntimeException (jak WalletNotFoundException) albo — gdy musisz
    //   mieć checked — dopisz {@code rollbackFor = TwojWyjatek.class} (lub {@code rollbackFor = Exception.class}, żeby
    //   wycofywał KAŻDY wyjątek). Dlaczego? Żeby „metoda rzuciła wyjątek” zawsze znaczyło „nic nie zapisano”.
    //
    // {@code noRollbackFor} — odwrotny przypadek: wyjątek ma polecieć do wywołującego, ale zmiany mają zostać
    //   (tu: „próba wypłaty kosztuje 2.00 także przy odmowie”). Rzadkie — zwykle prościej nie rzucać wyjątku.
    //   Reguły dopasowują też PODKLASY wskazanego wyjątku. Wersje z nazwą w tekście: {@code rollbackForClassName}.

    // =================================================================================================
    // 4. Wyjątek złapany w środku metody → brak rollbacku; setRollbackOnly() (TransferService)
    // =================================================================================================
    //
    //   PRZED: {@code transferSwallowingError} — {@code try { moveMoney(...) } catch (WalletNotFoundException e) { return false; }}
    //     Proxy widzi zwykły powrót z metody → COMMIT. Wypłata z portfela 1 zapisana (400.00), wpłaty nie ma (test).
    //   PO: {@code transferOrRollbackQuietly} — w catch:
    //     {@code TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();}
    //     Proxy po powrocie sprawdza flagę rollback-only i zamiast commitu robi rollback, a wywołujący dostaje
    //     zwykłe {@code false} — bez wyjątku (test: saldo bez zmian).
    //
    // PUŁAPKA: {@code catch (Exception e) { log.error(...); }} w metodzie transakcyjnej = „zatwierdź to, co zdążyłem
    //   zrobić”. Dlaczego groźne? Dane zostają w połowie zmienione, a w logu tylko wpis o błędzie.
    //   Łap wyjątki w serwisie tylko wtedy, gdy wiesz, co z połową zmian zrobić (rzucić dalej, setRollbackOnly).
    //
    // UWAGA: tu wyjątek rzuca NASZ kod ({@code orElseThrow}), więc nie przechodzi przez żadne inne proxy. Gdy wyjątek
    //   wyleci z INNEJ metody transakcyjnej (np. innego serwisu z {@code @Transactional}) i złapiesz go dopiero u siebie,
    //   transakcja jest już oznaczona rollback-only i commit się nie uda — {@code UnexpectedRollbackException}
    //   (s10_transactions/Tx02Propagation, sekcja 2).
    //   Programowo, bez adnotacji: {@code TransactionTemplate} i {@code status.setRollbackOnly()}
    //   (s10_transactions/Tx05ProgrammaticTransactions).

    // =================================================================================================
    // 5. GDZIE stawiać @Transactional; klasa a metoda; jakarta.transaction.Transactional; PUŁAPKA testów
    // =================================================================================================
    //
    // Warstwa:
    //   • SERWIS (przypadek użycia: „przelej”, „złóż zamówienie”) — TAK. Tu wiadomo, co musi się udać razem.
    //   • Kontroler — NIE. Transakcja trwałaby też podczas zamiany na JSON i obsługi HTTP (długo trzyma połączenie),
    //     a logika biznesowa przecieka do warstwy webowej.
    //   • Repozytorium — zwykle nie trzeba: Spring Data ma już {@code @Transactional} na swoich metodach (zapisy)
    //     i {@code readOnly = true} (odczyty). Jedna metoda repozytorium = jedna mała transakcja (sekcja 1, PRZED).
    //
    // Klasa a metoda (WalletSettingsService, testy):
    //   {@code @Transactional(readOnly = true)} na klasie → każda publiczna metoda dostaje transakcję tylko do odczytu
    //   ({@code inspectRead}: active = true, readOnly = true). {@code @Transactional} na metodzie ZASTĘPUJE klasową w CAŁOŚCI
    //   ({@code rename}: readOnly = false — wartość domyślna; atrybuty się nie łączą). Co robi readOnly w JPA i JDBC:
    //   s10_transactions/Tx04ReadOnlyIsolation.
    //
    // Dwie adnotacje o tej samej nazwie (JakartaWalletService, testy):
    //   Spring {@code org.springframework.transaction.annotation.Transactional}  Jakarta {@code jakarta.transaction.Transactional}
    //   {@code propagation = Propagation.REQUIRES_NEW}                            {@code value = TxType.REQUIRES_NEW}
    //   {@code rollbackFor} / {@code noRollbackFor}                                {@code rollbackOn} / {@code dontRollbackOn}
    //   {@code readOnly}, {@code timeout}, {@code isolation}, {@code transactionManager}             brak
    //   Spring w Boot 4.1.1 obsługuje OBIE (sprawdzone testem: transakcja aktywna, checked → commit, rollbackOn → rollback).
    //
    // PUŁAPKA: IDE przy imporcie podpowiada obie adnotacje. Dlaczego groźne? Po wybraniu jakarta nie skompiluje się
    //   {@code readOnly = true} ani {@code rollbackFor} — a ktoś „naprawi” to, usuwając atrybut. DOBRA PRAKTYKA: w kodzie
    //   Springa używaj springowej {@code org.springframework.transaction.annotation.Transactional} — ma więcej opcji.
    //
    // PUŁAPKA: testy! {@code @DataJpaTest} oraz {@code @Transactional} na klasie testu uruchamiają test w JEDNEJ transakcji
    //   wycofywanej na końcu. Serwis z REQUIRED DOŁĄCZA do niej, więc test NIE widzi ani commitu, ani rollbacku serwisu
    //   (Tx01DataJpaTestPitfallTest, sprawdzone):
    //     • po „wycofanym” {@code transfer(1, 99, 100)} encja w pamięci Hibernate ma saldo 400.00 — test „pokazuje”, że
    //       rollbacku nie było; w rzeczywistości transakcja testu jest tylko oznaczona rollback-only;
    //     • {@code withdrawWithFee} i {@code withdrawWithFeeOrRollback} dają w teście TO SAMO (96.00 w pamięci) — nie da się
    //       sprawdzić, czy {@code rollbackFor} działa.
    //   Dlaczego? Granicą transakcji staje się metoda TESTU, a nie metoda serwisu. Stąd w tym dziale testy działają na
    //   pełnej aplikacji BEZ transakcji testu ({@code @SpringBootTest} + {@code @ActiveProfiles("tx01")}, Tx01TransactionalTest),
    //   czytają saldo JdbcClientem (widzi tylko dane zatwierdzone) i przywracają dane w {@code @BeforeEach}. Wycinek
    //   też da się „odczarować”: {@code @Transactional(propagation = Propagation.NOT_SUPPORTED)} na metodzie testu
    //   (to samo co w s09_jpa_relations/Rel04LazyLoading, sekcja 2) — wtedy widać prawdziwy rollback (test).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • {@code @Transactional} na metodzie serwisu = begin przed metodą, commit po niej, rollback po wyjątku.
     *   • Pod spodem: proxy CGLIB ({@code Klasa$$SpringCGLIB$$0}) → TransactionInterceptor → JpaTransactionManager.
     *   • Jedna transakcja = jedno połączenie przypięte do wątku; JPA i JdbcClient w niej dzielą połączenie.
     *   • Domyślnie rollback: RuntimeException i Error. COMMIT: wyjątki checked (!) → {@code rollbackFor = X.class}.
     *   • {@code noRollbackFor = X.class} — wyjątek leci dalej, zmiany zostają.
     *   • catch w metodzie = commit; cichy rollback: {@code TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()}.
     *   • Na klasie: wszystkie publiczne metody; na metodzie: zastępuje klasową w całości.
     *   • Spring: org.springframework…Transactional (readOnly, timeout, isolation); jakarta: rollbackOn, TxType.
     *   • Testy transakcji: bez transakcji testu (pełna aplikacja albo NOT_SUPPORTED) + reset danych.
     *   • Dowód: {@code TransactionSynchronizationManager.isActualTransactionActive()}, log JpaTransactionManager=debug.
     *
     * PYTANIA KONTROLNE:
     *   1. Co się stanie? Metoda BEZ {@code @Transactional} woła {@code wallets.save(from)}, a potem rzuca
     *      RuntimeException przed {@code wallets.save(to)}. Co jest w bazie?
     *   2. Kto naprawdę otwiera i zatwierdza transakcję, gdy wywołujesz metodę z {@code @Transactional}? Jaki bean
     *      menedżera transakcji tworzy Boot w aplikacji z JPA?
     *   3. Co się stanie? {@code @Transactional void pay() throws InsufficientFundsException} (checked) pobiera opłatę,
     *      a potem rzuca InsufficientFundsException. Czy opłata zostanie w bazie? Jak to zmienić?
     *   4. ZNAJDŹ BŁĄD: {@code @Transactional public void transfer(...) { try { moveMoney(...); } catch (Exception e)
     *      { log.error("Błąd przelewu", e); } }} — „przecież jest @Transactional, czemu pieniądze znikają?”.
     *   5. Czym różni się {@code @Transactional(readOnly = true)} na klasie od {@code @Transactional} na metodzie tej
     *      klasy? Jaki readOnly ma metoda z samą adnotacją {@code @Transactional}?
     *   6. ZNAJDŹ BŁĄD: test {@code @DataJpaTest} sprawdza, że po wyjątku w {@code transfer} saldo wynosi 500.00, i…
     *      nie przechodzi (jest 400.00), choć w produkcji przelew wycofuje się poprawnie. Dlaczego? Jak napisać test?
     *   7. Czym różni się {@code jakarta.transaction.Transactional} od springowego {@code @Transactional}?
     *   8. Dlaczego {@code @Transactional} stawiamy w serwisie, a nie w kontrolerze ani w repozytorium?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w pliku TransactionalExercises.java (metody z TODO), testy: Tx01TransactionalExercisesTest
    // (pełna aplikacja, bez transakcji testu).
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Zmiana {@code from} jest ZATWIERDZONA: bez transakcji serwisu każde save() ma własną transakcję repozytorium
     *      i commit od razu. Pieniądze zniknęły z pierwszego portfela, do drugiego nie dotarły (test sekcji 1: 400.00).
     *   2. Proxy CGLIB beana (TransactionInterceptor) woła PlatformTransactionManager: getTransaction przed metodą,
     *      commit/rollback po niej. W aplikacji z JPA Boot tworzy {@code JpaTransactionManager} (bean transactionManager).
     *   3. Opłata ZOSTANIE — wyjątki checked domyślnie powodują commit (test sekcji 3: 98.00). Zmiana:
     *      {@code @Transactional(rollbackFor = InsufficientFundsException.class)} albo wyjątek jako RuntimeException.
     *   4. catch połyka wyjątek, proxy widzi normalny powrót i robi COMMIT wypłaty bez wpłaty. Naprawa: nie łapać (niech
     *      wyjątek wyjdzie przez proxy), rzucić dalej albo w catch wywołać
     *      {@code TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()}.
     *   5. Na klasie obowiązuje wszystkie publiczne metody; adnotacja na metodzie zastępuje ją w całości (atrybuty się
     *      nie łączą). Sama {@code @Transactional} ma readOnly = false (test sekcji 5).
     *   6. @DataJpaTest działa w transakcji testu; serwis dołącza do niej, rollback serwisu tylko oznacza transakcję
     *      testu rollback-only, a encja w pamięci Hibernate dalej ma 400.00. Test trzeba uruchomić bez transakcji testu
     *      ({@code @Transactional(propagation = Propagation.NOT_SUPPORTED)} albo pełna aplikacja) i czytać saldo z bazy
     *      (JdbcClient), przywracając dane przed każdym testem.
     *   7. Inny pakiet i inne atrybuty: jakarta ma {@code value = TxType…}, {@code rollbackOn}, {@code dontRollbackOn};
     *      nie ma readOnly, timeout, isolation ani wyboru menedżera. Spring obsługuje obie, domyślna reguła (checked →
     *      commit) jest taka sama. W kodzie Springa wybieramy springową.
     *   8. Serwis zna cały przypadek użycia, więc wie, które zmiany muszą się udać razem. Transakcja w kontrolerze
     *      trzymałaby połączenie przez całe żądanie HTTP; repozytorium widzi tylko pojedynczy zapis (i ma już własne
     *      transakcje ze Spring Data).
     */
    // </editor-fold>
}
