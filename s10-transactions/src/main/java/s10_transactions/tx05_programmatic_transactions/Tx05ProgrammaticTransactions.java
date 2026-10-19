package s10_transactions.tx05_programmatic_transactions;

/**
 * <pre>
 * TEMAT: Transakcje programowe — TransactionTemplate i PlatformTransactionManager
 *        (programmatic = programowy, „zapisany w kodzie”; template = szablon; callback = wywołanie zwrotne, tu: lambda;
 *         transaction status = stan transakcji; rollback-only = „tylko do wycofania”)
 *
 * W SKRÓCIE:
 *   @Transactional to transakcja DEKLARATYWNA: granice wyznacza metoda, a pilnuje ich proxy. Czasem potrzebujesz
 *   transakcji na KAWAŁEK metody: osobnej dla każdego elementu w pętli, z wolnym wywołaniem innego systemu POZA
 *   transakcją, z ustawieniami wybranymi w trakcie działania. Wtedy otwierasz ją w kodzie:
 *   transactionTemplate.execute(status -> { ... }) — przed lambdą start, po niej commit, po wyjątku rollback.
 *   Pod spodem pracuje ten sam PlatformTransactionManager (tu JpaTransactionManager) co przy @Transactional, więc
 *   propagacja, readOnly, izolacja i timeout działają tak samo. Bez proxy nie ma też pułapek proxy (Tx03).
 *
 * ANALOGIA: zakupy z koszykiem vs zakupy przy kasie samoobsługowej.
 *   @Transactional to sklep z obsługą: wchodzisz (metoda), kasjer (proxy) otwiera rachunek i zamyka go, gdy
 *   wychodzisz. TransactionTemplate to kasa samoobsługowa: SAM decydujesz, które produkty skanujesz na jeden paragon,
 *   kiedy płacisz i kiedy anulujesz — także kilka paragonów jeden po drugim (transakcja na element).
 *   PlatformTransactionManager to zaplecze kasy: możesz otworzyć i zamknąć paragon „ręcznie”, ale jeśli zapomnisz
 *   go zamknąć, kasa zostaje zablokowana dla następnych klientów.
 *
 * JAK TO DZIAŁA:
 *   transactionTemplate.execute(status -> priceService…)        (TransactionTemplate — bez proxy, bez adnotacji)
 *     1. transactionManager.getTransaction(ustawienia szablonu) → JpaTransactionManager: nowa transakcja albo
 *        dołączenie (REQUIRED), w logu: „Creating new transaction with name [null]: PROPAGATION_REQUIRED,…”
 *        (szablon bez setName nie ma nazwy — stąd [null])
 *     2. Twoja lambda (dostaje TransactionStatus: setRollbackOnly(), isNewTransaction()…)
 *     3a. normalny powrót → commit (tu Hibernate robi flush — błędy z bazy wychodzą DOPIERO TERAZ, z execute)
 *     3b. RuntimeException / Error w lambdzie → rollback, wyjątek leci dalej
 *     3c. status.setRollbackOnly() → rollback bez wyjątku, execute zwraca wynik lambdy
 *   Bean transactionTemplate tworzy Spring Boot (autokonfiguracja transakcji) — sprawdzone testem sekcji 5.
 *
 * SŁÓWKA:
 *   execute = wykonaj; without result = bez wyniku; callback = wywołanie zwrotne; status = stan;
 *   rollback only = tylko do wycofania; dry run = próba „na sucho”; chunk = paczka; gateway = bramka;
 *   compensation = kompensacja (cofnięcie skutku innym działaniem); definition = definicja (ustawienia transakcji);
 *   completed = zakończona; bound to thread = przypięta do wątku; phase = faza.
 *
 * ZOBACZ TEŻ: s10_transactions/Tx01Transactional (reguły wycofania w @Transactional), s10_transactions/Tx02Propagation
 *             (propagacja, połączenia z puli), s10_transactions/Tx03ProxyPitfalls (TransactionTemplate jako naprawa
 *             this.metoda()), s10_transactions/Tx04ReadOnlyIsolation (readOnly, izolacja, timeout),
 *             s17_events (@TransactionalEventListener), s11_testing/Test04DataJpaTest (transakcja w testach),
 *             t13_lambdas/Lambda02FunctionalInterfaces (lambda jako implementacja interfejsu).
 * </pre>
 */
public final class Tx05ProgrammaticTransactions {

    private Tx05ProgrammaticTransactions() {
    }

    // =================================================================================================
    // 1. execute i executeWithoutResult (PriceService.changePrice / changePriceWithoutResult)
    // =================================================================================================
    //
    // public BigDecimal changePrice(String sku, BigDecimal newPrice) {
    //     return transactionTemplate.execute(status -> {          // execute = wykonaj w transakcji, zwróć wynik
    //         Product product = products.findById(sku).orElseThrow();
    //         BigDecimal oldPrice = product.getPrice();
    //         product.changePrice(newPrice);                      // dirty checking działa — to prawdziwa transakcja
    //         jdbc.sql("insert into price_changes …").update();   // ta sama transakcja, to samo połączenie
    //         return oldPrice;
    //     });
    // }
    // executeWithoutResult(status -> …) — to samo, gdy wynik nie jest potrzebny (lambda typu Consumer).
    //
    // Sprawdzone testami sekcji 1:
    //   • changePrice("KSI-001", 89.00) zwraca 79.00 (starą cenę), w bazie 89.00, w dzienniku wpis;
    //   • changePrice("SPO-001", 0.00) → DataIntegrityViolationException (CHECK price > 0 łamany przy flush, czyli
    //     przy commicie na końcu execute) i rollback CAŁOŚCI: cena 64.99, a INSERT do dziennika też wycofany.
    //
    // Wyjątki KONTROLOWANE: TransactionCallback.doInTransaction nie ma „throws” (test sprawdza to refleksją), więc
    // lambda nie może rzucić np. IOException — kompilator każe go złapać albo opakować w wyjątek niekontrolowany
    // (np. UncheckedIOException). A niekontrolowany = rollback. Inaczej niż w @Transactional, gdzie wyjątek
    // kontrolowany domyślnie oznacza COMMIT (Tx01Transactional, sekcja 3).
    //
    // DOBRA PRAKTYKA: w lambdzie tylko praca z bazą; wynik zwróć z execute i dalej pracuj na nim POZA transakcją.
    //   Dlaczego? Transakcja (i połączenie z puli) trwa dokładnie tyle, ile lambda.

    // =================================================================================================
    // 2. status.setRollbackOnly() — próba „na sucho” (PriceService.previewCatalogValue)
    // =================================================================================================
    //
    // „Jaka byłaby wartość cennika, gdyby kawa kosztowała 74.99?” — zmieniamy cenę naprawdę, wypychamy UPDATE
    // (products.flush()), pytamy bazę o sumę, a na końcu status.setRollbackOnly(). Test: wynik 6132.88 (o 10 zł więcej),
    // w bazie nadal 64.99. W logu DEBUG: „Transactional code has requested rollback”.
    // Bez flush() zapytanie SQL (JdbcClient) nie zobaczyłoby zmiany encji — Hibernate wysyła UPDATE dopiero przy flush.

    // =================================================================================================
    // 3. Transakcja na ELEMENT w pętli (PriceService.importPrices)
    // =================================================================================================
    //
    // newPrices.forEach((sku, price) -> {
    //     try {
    //         transactionTemplate.executeWithoutResult(status -> changeInCurrentTransaction(sku, price));
    //         imported.add(sku);
    //     } catch (RuntimeException importFailed) {           // łapiemy POZA execute!
    //         failed.put(sku, importFailed.getClass().getSimpleName());
    //     }
    // });
    // Test: ELE-001 → 5299.99 i KSI-001 → 69.00 zapisane; SPO-001 → 0.00 (DataIntegrityViolationException) i XXX-999
    // (NoSuchElementException) wycofane — każdy ze swoim wpisem w dzienniku. Ta sama lista w JEDNEJ transakcji
    // (importPricesAllOrNothing z @Transactional) — jedna zła cena wycofuje wszystko.
    //
    // Dlaczego catch POZA execute? Błąd z bazy pojawia się przy commicie, który robi execute na końcu — catch wewnątrz
    // lambdy by go nie złapał. A gdyby lambda złapała wyjątek rzucony przez INNY bean transakcyjny, zostałby znacznik
    // rollback-only i UnexpectedRollbackException (Tx02Propagation, sekcja 3).
    //
    // DOBRA PRAKTYKA: wielkość transakcji wybieraj świadomie: „wszystko albo nic” (jedna transakcja), „każdy element
    //   osobno” (import, wysyłka maili z zapisem statusu) albo „paczkami po N” (ćwiczenie 3). Dlaczego? Jedna wielka
    //   transakcja na 100 000 wierszy długo trzyma blokady i połączenie, a jeden błąd na końcu cofa godzinę pracy.

    // =================================================================================================
    // 4. Wolne wywołanie innego systemu POZA transakcją (CheckoutService)
    // =================================================================================================
    //
    // PRZED — @Transactional na całej metodzie checkoutInsideTransaction: bramka płatności wywoływana W TRANSAKCJI
    //   (test: wasTransactionActiveDuringLastCharge() = true). Połączenie z bazą jest zajęte przez cały czas
    //   rozmowy z bramką, choć baza nic nie robi (a każda transakcja trzyma połączenie — Tx02Propagation, sekcja 2).
    // PO — checkoutWithShortTransaction: najpierw paymentGateway.charge(...) bez transakcji (test: false), potem
    //   transactionTemplate.executeWithoutResult(status -> saveOrder(...)) — transakcja tylko na czas INSERT.
    //
    // PUŁAPKA: wywołania HTTP, wysyłka maili, długie obliczenia wewnątrz @Transactional. Dlaczego groźne? Przy
    //   wolnym zewnętrznym systemie wątki trzymają połączenia (i blokady wierszy) i pula się wyczerpuje — „staje” cała
    //   aplikacja, nie tylko płatności. Cena rozdzielenia: gdy zapis po płatności się nie uda, płatność trzeba wycofać
    //   osobnym działaniem (kompensacja) — o tym trzeba pomyśleć świadomie.

    // =================================================================================================
    // 5. Własne ustawienia: new TransactionTemplate(menedżer) (PriceService.insideOwnReadOnlyTemplate)
    // =================================================================================================
    //
    // TransactionTemplate reportTemplate = new TransactionTemplate(transactionManager);
    // reportTemplate.setName("raport-cen");            // nazwa w logu i w getCurrentTransactionName()
    // reportTemplate.setReadOnly(true);                // jak @Transactional(readOnly = true) — Tx04
    // reportTemplate.setTimeout(10);                   // sekundy
    // reportTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    // (jest też setIsolationLevel(TransactionDefinition.ISOLATION_…))
    // W logu: „Creating new transaction with name [raport-cen]: PROPAGATION_REQUIRES_NEW,ISOLATION_DEFAULT,
    // timeout_10,readOnly”. Test: wewnątrz nazwa „raport-cen”, readOnly = true, isNewTransaction() = true.
    //
    // PUŁAPKA: transactionTemplate.setReadOnly(true) na WSTRZYKNIĘTYM beanie. Dlaczego groźne? To jeden wspólny bean
    //   (singleton „transactionTemplate” z Boota — test), więc zmiana ustawień dotyka WSZYSTKICH serwisów, które go
    //   używają — nagle ich zapisy przepadają po cichu (Tx04, sekcja 2). Potrzebujesz innych ustawień → własna
    //   instancja (może być polem serwisu, utworzonym raz w konstruktorze).

    // =================================================================================================
    // 6. PlatformTransactionManager niskopoziomowo — i dlaczego zwykle wystarcza TransactionTemplate
    // =================================================================================================
    //
    // TransactionTemplate.execute to w środku: getTransaction → Twój kod → commit albo rollback. Można to napisać
    // samemu (PriceService.changePriceManually) — ale trzeba pamiętać o dwóch rzeczach:
    //
    //   DefaultTransactionDefinition definition = new DefaultTransactionDefinition();   // REQUIRED, DEFAULT…
    //   definition.setName("zmiana-ceny-recznie");
    //   TransactionStatus status = transactionManager.getTransaction(definition);
    //   BigDecimal oldPrice;
    //   try {
    //       oldPrice = changeInCurrentTransaction(sku, newPrice);
    //   } catch (RuntimeException | Error e) {
    //       transactionManager.rollback(status);       // rollback tylko, gdy zawiódł NASZ kod
    //       throw e;
    //   }
    //   transactionManager.commit(status);             // commit POZA try
    //
    // 1. commit POZA try. Gdy sam commit się nie uda (flush → CHECK), menedżer SAM wycofuje i kończy transakcję
    //    (w logu: „Initiating transaction rollback after commit exception”) i rzuca wyjątek z prawdziwą przyczyną
    //    (test: DataIntegrityViolationException). Wersja z commit W try (changePriceNaively) woła w catch drugi
    //    rollback i dostaje IllegalTransactionStateException: „Transaction is already completed - do not call commit
    //    or rollback more than once per transaction” — który ZASŁANIA prawdziwy błąd (test).
    // 2. ZAWSZE zakończ transakcję. Niezakończona zostaje przypięta do wątku (z połączeniem z puli): test
    //    beginAndForget → po powrocie isActualTransactionActive() = true, a następne priceService.changePrice(...)
    //    po cichu do niej DOŁĄCZA — i znika przy jej rollbacku.
    // Drobiazg: TransactionDefinition.withDefaults() działa, ale w logu wygląda jak
    // „org.springframework.transaction.StaticTransactionDefinition@…” — DefaultTransactionDefinition z setName czyta
    // się lepiej.
    //
    // DOBRA PRAKTYKA: zamiast getTransaction/commit/rollback używaj TransactionTemplate. Dlaczego? Szablon ma oba
    //   powyższe punkty zrobione dobrze — a ręczny kod łatwo zepsuć przy pierwszej zmianie.

    // =================================================================================================
    // 7. Kiedy programowo, a kiedy adnotacja + zapowiedź @TransactionalEventListener
    // =================================================================================================
    //
    //   sytuacja                                                  | narzędzie
    //   ----------------------------------------------------------+-------------------------------------------
    //   zwykła metoda serwisu = jedna operacja biznesowa           | @Transactional (domyślny wybór)
    //   transakcja na element / paczkę w pętli                     | TransactionTemplate
    //   wolne wywołanie zewnętrzne poza transakcją                 | TransactionTemplate na sam zapis
    //   metoda transakcyjna wołana z tej samej klasy (Tx03)        | TransactionTemplate albo osobny bean
    //   ustawienia transakcji znane dopiero w czasie działania     | własny TransactionTemplate
    //   transakcja zaczynana i kończona w RÓŻNYCH miejscach kodu   | PlatformTransactionManager (rzadkość)
    //
    // Zapowiedź (s17_events): @TransactionalEventListener — słuchacz zdarzenia, który Spring wywołuje dopiero
    // w określonej fazie transakcji; domyślnie phase = TransactionPhase.AFTER_COMMIT, czyli PO udanym commicie
    // (test sprawdza wartość domyślną). Typowe użycie: wyślij e-mail „zamówienie przyjęte” dopiero, gdy zamówienie
    // NAPRAWDĘ jest w bazie.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • transactionTemplate.execute(status -> wynik) / executeWithoutResult(status -> { … }).
     *   • Lambda bez wyjątku → commit (na końcu execute — wtedy wychodzą błędy z bazy); RuntimeException/Error →
     *     rollback; status.setRollbackOnly() → rollback bez wyjątku. Wyjątku kontrolowanego lambda nie rzuci.
     *   • Transakcja na element: execute w pętli, catch POZA execute.
     *   • Wolne wywołania zewnętrzne — poza transakcją; transakcja tylko na zapis.
     *   • Własne ustawienia → new TransactionTemplate(transactionManager) + setName/setReadOnly/setTimeout/
     *     setPropagationBehavior/setIsolationLevel. Wspólnego beana transactionTemplate NIE zmieniaj.
     *   • Ręcznie: getTransaction → try { kod } catch { rollback; throw } → commit POZA try; zawsze zakończ.
     *   • @TransactionalEventListener: domyślnie AFTER_COMMIT (s17_events).
     *
     * PYTANIA KONTROLNE:
     *   1. Co mają wspólnego @Transactional i TransactionTemplate, a czym się różnią?
     *   2. Co się stanie? priceService.changePrice("SPO-001", new BigDecimal("0.00")) przy cenie 64.99 — czy poleci
     *      wyjątek (jaki?), jaka będzie cena i ile wpisów przybędzie w dzienniku price_changes?
     *   3. Dlaczego w lambdzie TransactionTemplate nie rzucisz wprost IOException i co z tego wynika dla rollbacku
     *      (porównaj z regułami @Transactional z Tx01)?
     *   4. ZNAJDŹ BŁĄD: {@code TransactionStatus s = tm.getTransaction(def); try { work(); tm.commit(s); }
     *      catch (RuntimeException e) { tm.rollback(s); throw e; }} — przy błędzie CHECK w bazie w logach widać tylko
     *      IllegalTransactionStateException.
     *   5. Co się stanie? priceService.importPrices z mapą ELE-001 → 5299.99, SPO-001 → 0.00, KSI-001 → 69.00 —
     *      które ceny zostaną zapisane i co zwróci raport?
     *   6. Po co wywołanie bramki płatności przenosić poza transakcję i jaka jest cena tej zmiany?
     *   7. Dlaczego nie wolno wołać setReadOnly(true) na wstrzykniętym beanie transactionTemplate?
     *   8. Co się stanie, gdy po transactionManager.getTransaction(...) zapomnisz o commit i rollback?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — klasa Tx05Exercises (ten pakiet), testy: Tx05ProgrammaticTransactionsExercisesTest
    // =================================================================================================
    //
    //   ĆWICZENIE 1 (łatwe): Tx05Exercises.raisePriceByPercent — podwyżka w TransactionTemplate.execute.
    //   ĆWICZENIE 2 (średnie): Tx05Exercises.previewCatalogValueAfterDiscount — „na sucho” z setRollbackOnly.
    //   ĆWICZENIE 3 (średnie): Tx05Exercises.importInChunks — transakcja na paczkę.
    //   ĆWICZENIE 4 (trudniejsze): Tx05Exercises.changePricesManually — PlatformTransactionManager bez pułapek.
    //   Rozwiązania: s10_transactions.solutions.tx05_programmatic_transactions.Tx05ProgrammaticTransactionsSolutions.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Oba korzystają z tego samego PlatformTransactionManager (JpaTransactionManager) i tych samych ustawień
     *      (propagacja, izolacja, readOnly, timeout). @Transactional: granice = metoda, pilnuje proxy (pułapki z Tx03).
     *      TransactionTemplate: granice = lambda, w dowolnym miejscu kodu, bez proxy; inne reguły dla wyjątków
     *      kontrolowanych (nie da się ich rzucić z lambdy).
     *   2. DataIntegrityViolationException (CHECK price > 0 łamany przy flush w commicie na końcu execute). Rollback
     *      całości: cena 64.99, w dzienniku 0 nowych wpisów (INSERT też wycofany). Sprawdzone testem
     *      exceptionRollsBackEverything.
     *   3. TransactionCallback.doInTransaction nie deklaruje wyjątków, więc kompilator każe IOException złapać albo
     *      opakować (np. w UncheckedIOException). Opakowany = niekontrolowany = ROLLBACK. W @Transactional sam
     *      IOException oznaczałby domyślnie COMMIT.
     *   4. commit jest w try: gdy commit się nie uda, menedżer już wycofał i zakończył transakcję, a catch woła
     *      rollback drugi raz → IllegalTransactionStateException („Transaction is already completed - do not call
     *      commit or rollback more than once per transaction”) zasłania prawdziwy błąd. Naprawa: commit poza try
     *      (albo TransactionTemplate). Sprawdzone testem manualTransactionManager.
     *   5. Zapisane ELE-001 = 5299.99 i KSI-001 = 69.00; SPO-001 zostaje 64.99. Raport: imported = [ELE-001, KSI-001],
     *      failed = {SPO-001: DataIntegrityViolationException}. Sprawdzone testem transactionPerItem (ta sama
     *      sytuacja z dodatkowym nieznanym sku).
     *   6. Żeby połączenie z bazy (i blokady) nie były zajęte przez czas rozmowy z wolnym systemem — inaczej pula
     *      połączeń się wyczerpuje. Cena: zapis i płatność nie są już jedną transakcją; gdy zapis się nie uda,
     *      płatność trzeba wycofać osobno (kompensacja). Sprawdzone testem externalCallOutsideTransaction.
     *   7. Bo to jeden wspólny bean (singleton) — zmiana dotknęłaby wszystkich jego użytkowników i ich zapisy
     *      przepadałyby po cichu. Własne ustawienia = new TransactionTemplate(transactionManager).
     *   8. Transakcja (z połączeniem z puli) zostaje przypięta do wątku; kolejny kod transakcyjny w tym wątku po cichu
     *      do niej dołącza, a jego „zatwierdzone” zmiany giną razem z nią przy jej rollbacku. Sprawdzone testem
     *      forgottenTransactionStaysBoundToThread.
     */
    // </editor-fold>
}
