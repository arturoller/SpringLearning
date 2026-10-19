package s10_transactions.tx01_transactional;

/**
 * <pre>
 * TEMAT: @Transactional — transakcje deklaratywne w Springu
 *        (transaction = transakcja; transactional = transakcyjny; commit = zatwierdzenie; rollback = wycofanie;
 *         declarative = deklaratywny, czyli „opisany adnotacją”, a nie zaprogramowany ręcznie)
 *
 * W SKRÓCIE:
 *   Transakcja to grupa operacji na bazie, która wykonuje się CAŁA albo WCALE. Przelew = obciąż konto A + uznaj konto B;
 *   jeśli druga część się nie uda, pierwsza musi zostać cofnięta. W czystym JDBC robiłeś to ręcznie:
 *   setAutoCommit(false), commit(), rollback() w catch (t29_jdbc_databases/Jdbc04Transactions). W Springu wystarczy
 *   adnotacja @Transactional nad metodą serwisu: Spring otworzy transakcję przed metodą, zatwierdzi ją po normalnym
 *   powrocie i wycofa, gdy metoda rzuci RuntimeException albo Error. UWAGA: wyjątek KONTROLOWANY (checked) domyślnie
 *   NIE wycofuje transakcji — to klasyczna pułapka.
 *
 * ANALOGIA: koszyk w sklepie internetowym i przycisk „Zapłać”.
 *   Wkładasz produkty do koszyka (operacje w transakcji) — nikt jeszcze nie widzi zmian w magazynie. „Zapłać” z
 *   sukcesem = commit: wszystko naraz staje się faktem. Karta odrzucona = rollback: koszyk znika, magazyn jak był.
 *   Nie ma stanu „zapłacono za połowę koszyka”.
 *
 * JAK TO DZIAŁA:
 *   transferService.transfer("PL-JAN", "PL-MARIA", 10000)       ← wywołujesz metodę na PROXY, nie na obiekcie
 *     1. proxy (TransferService$$SpringCGLIB$$0) → TransactionInterceptor (przechwytywacz transakcji)
 *     2. PlatformTransactionManager.getTransaction(...)  → tu: JpaTransactionManager
 *          - tworzy EntityManager, rozpoczyna transakcję w bazie (połączenie z autoCommit = false)
 *          - przypina EntityManager do bieżącego wątku (ThreadLocal) — repozytoria w tym wątku użyją TEGO SAMEGO
 *     3. wywołanie Twojej metody na prawdziwym obiekcie (find, debit, credit — zmiany w encjach)
 *     4a. normalny powrót  → flush (Hibernate wysyła UPDATE-y) + COMMIT
 *     4b. RuntimeException / Error → ROLLBACK, wyjątek leci dalej do wołającego
 *     4c. wyjątek kontrolowany → COMMIT (!), wyjątek leci dalej
 *   Kto to wszystko włączył? Spring Boot: TransactionAutoConfiguration (włącza obsługę @Transactional, proxy CGLIB;
 *   w Boot 4 w pakiecie org.springframework.boot.transaction.autoconfigure — w Boot 3 był w
 *   org.springframework.boot.autoconfigure.transaction, stąd inne importy w starszych poradnikach) i autokonfiguracja
 *   JPA (bean „transactionManager” typu JpaTransactionManager). Sprawdzone testem sekcji 6.
 *
 * SŁÓWKA:
 *   ACID — atomicity (niepodzielność), consistency (spójność), isolation (izolacja), durability (trwałość);
 *   commit = zatwierdź; rollback = wycofaj; rollback for = wycofaj dla; no rollback for = nie wycofuj dla;
 *   checked exception = wyjątek kontrolowany (musisz go zadeklarować w throws); unchecked = niekontrolowany
 *   (RuntimeException); proxy = pośrednik (obiekt „przed” Twoim obiektem); transaction manager = menedżer transakcji;
 *   interceptor = przechwytywacz; actual = rzeczywisty; active = aktywny; debit = obciążenie; credit = uznanie.
 *
 * ZOBACZ TEŻ: t29_jdbc_databases/Jdbc04Transactions (transakcje ręcznie w JDBC), t10_exceptions/Exceptions02CheckedUnchecked
 *             (wyjątki kontrolowane i niekontrolowane), t19_annotations_reflection/Annotations06DynamicProxy (jak działa
 *             proxy), s08_data_jpa/Jpa01Entities (dirty checking), s10_transactions/Tx02Propagation (co dalej).
 * </pre>
 */
public final class Tx01Transactional {

    private Tx01Transactional() {
    }

    // =================================================================================================
    // 0. ACID w pigułce — co obiecuje transakcja
    // =================================================================================================
    //
    //   A — atomicity (niepodzielność): wszystko albo nic. Obciążenie bez uznania nie może „zostać”.
    //   C — consistency (spójność): po transakcji dane spełniają reguły (np. CHECK (balance >= 0) w tabeli accounts).
    //   I — isolation (izolacja): równoległe transakcje nie widzą nawzajem swoich NIEZATWIERDZONYCH zmian (szczegóły
    //       i poziomy izolacji: Tx04ReadOnlyIsolation).
    //   D — durability (trwałość): po commicie dane przetrwają awarię (u nas H2 w pamięci — tylko do nauki).
    //
    // Spring NIE implementuje transakcji sam. Robi to BAZA DANYCH. Spring tylko w odpowiednim momencie mówi bazie
    // „zaczynamy”, „zatwierdź” albo „wycofaj” — tak jak Ty w JDBC, ale automatycznie.

    // =================================================================================================
    // 1. PRZED: serwis bez @Transactional (TransferService.transferWithoutTransaction)
    // =================================================================================================
    //
    // Każda metoda gotowego repozytorium Spring Data (findById, save…) jest SAMA transakcyjna, ale tylko „dla siebie”:
    //   accounts.save(source);   → transakcja nr 1: UPDATE accounts ... → COMMIT
    //   find(to)                 → AccountNotFoundException
    // Wynik: 200 zł zniknęło z konta Jana i nigdzie nie dotarło. Sprawdzone testem sekcji 1.
    //
    // PUŁAPKA: „Repozytorium jest transakcyjne, więc serwis jest bezpieczny” — NIE. Dlaczego? Transakcja repozytorium
    //   obejmuje JEDNO wywołanie. Granice transakcji muszą obejmować całą operację biznesową, czyli metodę SERWISU.

    // =================================================================================================
    // 2. PO: @Transactional nad metodą serwisu (TransferService.transfer)
    // =================================================================================================
    //
    // @Transactional
    // public void transfer(String from, String to, long amount) {
    //     find(from).debit(amount);      // zmiana salda w ZARZĄDZANEJ encji — bez save()
    //     find(to).credit(amount);       // brak konta → AccountNotFoundException → ROLLBACK całości
    // }
    //
    // Reguły wycofania (domyślne, sprawdzone testami sekcji 2 i 3):
    //   RuntimeException (np. IllegalStateException, AccountNotFoundException) → ROLLBACK
    //   Error (np. OutOfMemoryError)                                           → ROLLBACK
    //   wyjątek kontrolowany (Exception, który NIE jest RuntimeException)     → COMMIT
    //
    // Dlaczego nie ma save()? W transakcji encja z findById jest ZARZĄDZANA (managed): Hibernate zapamiętał jej stan
    // i przed commitem porównuje go z obecnym (dirty checking — wykrywanie zmian). Zmienione pole → UPDATE samo.
    //
    // DOBRA PRAKTYKA: @Transactional stawiaj na metodach SERWISU (warstwa logiki), nie kontrolera i nie encji.
    //   Dlaczego? Serwis wie, co jest jedną operacją biznesową („przelew”), kontroler nie powinien znać bazy, a encja
    //   nie jest beanem, więc Spring nie może jej owinąć proxy.

    // =================================================================================================
    // 3. WYJĄTEK KONTROLOWANY → domyślnie COMMIT (TransferService.transferAndConfirm)
    // =================================================================================================
    //
    // @Transactional
    // public void transferAndConfirm(...) throws PaymentGatewayException {    // checked
    //     transfer(from, to, amount);
    //     if (!bankOnline) throw new PaymentGatewayException("Bank nie potwierdził przelewu");
    // }
    // Wynik: wyjątek dociera do wołającego, a przelew JEST w bazie (commit). Sprawdzone testem sekcji 3.
    //
    // Skąd ta reguła? Przejęta z dawnych EJB: wyjątek kontrolowany traktowano jak „przewidziany wynik biznesowy”, po
    // którym dane mogą zostać. W praktyce prawie nikt się tego nie spodziewa.
    //
    // Naprawa: @Transactional(rollbackFor = PaymentGatewayException.class)   // rollback for = wycofaj dla
    //   (albo szerzej: rollbackFor = Exception.class). Od Spring Framework 6.2 można też zmienić regułę dla całej
    //   aplikacji: @EnableTransactionManagement(rollbackOn = RollbackOn.ALL_EXCEPTIONS) (enum RollbackOn:
    //   RUNTIME_EXCEPTIONS — domyślnie, ALL_EXCEPTIONS). Spring Boot 4.1 nie ma na to właściwości — w grupie
    //   spring.transaction.* są tylko default-timeout i rollback-on-commit-failure — więc trzeba dodać własną klasę
    //   konfiguracji z tą adnotacją. Wtedy Boot wycofuje swoją domyślną konfigurację transakcji i używa Twojej.
    //   Sprawdzone testem Tx01TransactionalRollbackOnTest: ta sama metoda transferAndConfirm zostaje WYCOFANA.
    //
    // PUŁAPKA: metoda z throws IOException / SQLException / własnym wyjątkiem z „extends Exception” i @Transactional
    //   bez rollbackFor. Dlaczego groźne? Test „szczęśliwej ścieżki” przechodzi, a przy błędzie w produkcji połowa
    //   zmian zostaje zatwierdzona — dokładnie to, przed czym transakcja miała chronić.
    //
    // DOBRA PRAKTYKA: własne wyjątki biznesowe rób niekontrolowane (extends RuntimeException), a jeśli musisz rzucać
    //   kontrolowany — dopisz rollbackFor. Dlaczego? Wtedy „wyjątek = rollback” działa bez wyjątków od reguły.

    // =================================================================================================
    // 4. noRollbackFor — wyjątek, który NIE ma cofać zmian (TransferService.chargeAndPrintReceipt)
    // =================================================================================================
    //
    // @Transactional(noRollbackFor = ReceiptPrinterException.class)          // no rollback for = nie wycofuj dla
    // Klient zapłacił, tylko paragon się nie wydrukował. Pieniądze mają zostać pobrane, a wyjątek i tak leci wyżej
    // (np. żeby pokazać komunikat „wydrukuj paragon ponownie”). Sprawdzone testem sekcji 4.
    // Istnieją też wersje „po nazwie”: rollbackForClassName / noRollbackForClassName — rzadko potrzebne.

    // =================================================================================================
    // 5. JAK ZAJRZEĆ, czy transakcja trwa — TransactionSynchronizationManager
    // =================================================================================================
    //
    // TransactionSynchronizationManager.isActualTransactionActive()   // czy rzeczywista transakcja jest aktywna?
    // To statyczna metoda, która sprawdza ThreadLocal bieżącego wątku. W metodzie z @Transactional → true,
    // w metodzie bez adnotacji (i w samym teście bez transakcji) → false. Sprawdzone testem sekcji 5.
    // Przyda się w kolejnych lekcjach do „podglądania” propagacji i pułapek proxy.
    //
    // PUŁAPKA: dwie adnotacje o tej samej nazwie. IntelliJ podpowie i
    //   org.springframework.transaction.annotation.Transactional (springowa), i jakarta.transaction.Transactional (JTA —
    //   Jakarta Transactions; jest na classpath razem z Hibernate). Spring rozumie OBIE (test sekcji 5), ale atrybuty
    //   mają inne: jakarta ma rollbackOn/dontRollbackOn i nie ma readOnly, timeout, isolation, propagation po
    //   springowemu. Dlaczego to pułapka? Kod się kompiluje, a „brakujące” atrybuty szuka się potem godzinami.
    //   W kursie zawsze importujemy org.springframework.transaction.annotation.Transactional.

    // =================================================================================================
    // 6. POD MASKĄ: proxy CGLIB + PlatformTransactionManager
    // =================================================================================================
    //
    // Bean wstrzyknięty jako TransferService to w rzeczywistości PODKLASA wygenerowana w locie przez CGLIB
    // (bibliotekę do generowania klas): TransferService$$SpringCGLIB$$0. Ta podklasa nadpisuje publiczne metody tak,
    // że najpierw pyta menedżera transakcji o transakcję, potem woła oryginał, na końcu commit/rollback.
    // To ten sam pomysł co dynamiczne proxy z t19_annotations_reflection/Annotations06DynamicProxy, tylko przez
    // dziedziczenie zamiast interfejsu.
    //
    // PlatformTransactionManager (menedżer transakcji platformy) — interfejs ze Springa: getTransaction, commit,
    // rollback. Implementacja zależy od technologii:
    //   JpaTransactionManager        — JPA/Hibernate (ten moduł; bean "transactionManager", sprawdzone testem)
    //   DataSourceTransactionManager / JdbcTransactionManager — czysty JDBC (s07_data_jdbc)
    //   JtaTransactionManager        — transakcje rozproszone (kilka baz naraz; rzadkość)
    // Twój kod tego nie widzi — @Transactional działa tak samo dla każdej implementacji.
    //
    // Konsekwencje „proxy” (szczegóły w Tx03ProxyPitfalls): transakcja działa TYLKO, gdy wywołanie przechodzi przez
    // proxy, czyli przychodzi z INNEGO beana. this.metoda() wewnątrz klasy omija proxy.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @Transactional nad metodą serwisu: start przed metodą, commit po powrocie, rollback po wyjątku.
     *   • Rollback domyślnie: RuntimeException i Error. Wyjątek kontrolowany → COMMIT (!).
     *   • rollbackFor = X.class — wycofaj także dla X; noRollbackFor = Y.class — nie wycofuj dla Y.
     *   • Spring 6.2+: @EnableTransactionManagement(rollbackOn = RollbackOn.ALL_EXCEPTIONS) — dla całej aplikacji.
     *   • W transakcji encje są zarządzane — zmiana pola = UPDATE przy commicie, bez save().
     *   • Repozytorium Spring Data jest transakcyjne „dla jednego wywołania” — granice wyznacza serwis.
     *   • Pod spodem: proxy CGLIB → TransactionInterceptor → PlatformTransactionManager (tu JpaTransactionManager).
     *   • Podgląd: TransactionSynchronizationManager.isActualTransactionActive().
     *   • Import: org.springframework.transaction.annotation.Transactional (nie jakarta.transaction).
     *   • Testy commit/rollback: @SpringBootTest bez transakcji testu + sprzątanie w @BeforeEach (nie @DataJpaTest).
     *
     * PYTANIA KONTROLNE:
     *   1. Co znaczy „A” w ACID i jak @Transactional go zapewnia?
     *   2. Co się stanie? transferAndConfirm("PL-JAN", "PL-MARIA", 10000, false) — jakie będzie saldo Jana
     *      (start: 100000) i czy metoda rzuci wyjątek?
     *   3. Co się stanie? transferWithoutTransaction("PL-JAN", "PL-NIEMA", 20000) — ile zostanie na koncie Jana?
     *   4. ZNAJDŹ BŁĄD: {@code @Transactional public void importOrders(Path file) throws IOException { saveAll(...);
     *      parseRest(file); }} — parseRest rzuca IOException w połowie pliku, a programista spodziewa się, że nic się
     *      nie zapisze.
     *   5. Dlaczego w metodzie transfer nie ma accounts.save(...), a zmiana salda i tak trafia do bazy?
     *   6. Dlaczego testy tej lekcji używają @SpringBootTest i sprzątania w @BeforeEach, a nie @DataJpaTest?
     *   7. Jaki bean zarządza transakcjami w tym module i kto go utworzył?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — klasa Tx01Exercises (ten pakiet), testy: Tx01TransactionalExercisesTest
    // =================================================================================================
    //
    //   ĆWICZENIE 1 (łatwe): Tx01Exercises.transferWithLimit — rollback dla wyjątku kontrolowanego (rollbackFor).
    //   ĆWICZENIE 2 (średnie): Tx01Exercises.chargeWithBrokenPrinter — wyjątek bez wycofania (noRollbackFor).
    //   ĆWICZENIE 3 (trudniejsze): Tx01Exercises.payAll — kilka płatności „wszystkie albo żadna”.
    //   Rozwiązania: s10_transactions.solutions.tx01_transactional.Tx01TransactionalSolutions.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Atomicity — niepodzielność: wszystkie operacje transakcji wykonują się albo żadna. @Transactional otwiera
     *      transakcję przed metodą i wycofuje ją (rollback), gdy metoda rzuci RuntimeException/Error — cofa się wtedy
     *      także to, co metoda zdążyła zmienić.
     *   2. Metoda rzuci PaymentGatewayException, a saldo Jana będzie 90000 — wyjątek kontrolowany NIE wycofuje
     *      transakcji, przelew został zatwierdzony. Sprawdzone testem checkedExceptionCommitsByDefault.
     *   3. 80000 — save(source) zatwierdził obciążenie w osobnej transakcji repozytorium, a potem poleciał
     *      AccountNotFoundException. Sprawdzone testem withoutTransactionMoneyDisappears.
     *   4. IOException jest kontrolowany, więc transakcja zostanie ZATWIERDZONA z częścią zamówień. Naprawa:
     *      {@code @Transactional(rollbackFor = IOException.class)} (albo rollbackFor = Exception.class).
     *   5. Encje pobrane w transakcji są zarządzane; przed commitem Hibernate porównuje ich stan ze „zdjęciem” z chwili
     *      odczytu (dirty checking) i sam wysyła UPDATE dla zmienionych pól.
     *   6. @DataJpaTest owija każdy test w transakcję wycofywaną na końcu; @Transactional serwisu dołączyłby do niej,
     *      więc nie byłoby prawdziwego commitu ani rollbacku w chwili wyjścia z metody serwisu. Bez transakcji testu
     *      dane są naprawdę zatwierdzane, dlatego trzeba je przywracać przed każdym testem.
     *   7. JpaTransactionManager o nazwie "transactionManager" — utworzyła go autokonfiguracja Spring Boot dla JPA
     *      (bo na classpath jest spring-boot-starter-data-jpa). Sprawdzone testem proxyAndTransactionManager.
     */
    // </editor-fold>
}
