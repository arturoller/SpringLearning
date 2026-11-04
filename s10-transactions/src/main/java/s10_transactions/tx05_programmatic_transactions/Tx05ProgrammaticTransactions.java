package s10_transactions.tx05_programmatic_transactions;

/**
 * <pre>
 * TEMAT: Transakcje programowe — {@code TransactionTemplate}, {@code PlatformTransactionManager} wprost
 *        i akcje po transakcji ({@code TransactionSynchronization}: {@code afterCommit}, {@code afterCompletion})
 *        (programmatic = programowe, w kodzie; template = szablon; synchronization = synchronizacja;
 *         after commit = po zatwierdzeniu; after completion = po zakończeniu)
 *
 * W SKRÓCIE:
 *   {@code @Transactional} (deklaratywnie) otacza transakcją CAŁĄ metodę i działa tylko przez proxy.
 *   {@code TransactionTemplate} otacza transakcją DOWOLNY kawałek kodu — lambdę w {@code execute}: dzięki temu
 *   jedna metoda może mieć wiele krótkich transakcji (np. jedną na element partii), a kod działa także bez proxy.
 *   Pod spodem i jedno, i drugie woła {@code PlatformTransactionManager}: getTransaction → commit/rollback.
 *   Akcje, które nie dają się wycofać (e-mail, wiadomość do innego systemu), rejestrujemy jako {@code afterCommit}
 *   — wykonają się dopiero po udanym commicie.
 *
 * ANALOGIA: przelew w aplikacji bankowej.
 *   {@code @Transactional} — automat: każda operacja „od kliknięcia do potwierdzenia” jest jedną transakcją, nie
 *   musisz o tym myśleć. {@code TransactionTemplate} — sam decydujesz, które kroki idą „pod jednym potwierdzeniem”:
 *   paczkę 100 przelewów możesz zatwierdzać po jednym, żeby jeden błędny numer konta nie zablokował reszty.
 *   {@code afterCommit} — SMS „przelew wykonany” bank wysyła dopiero PO zaksięgowaniu, nie w trakcie.
 *
 * JAK TO DZIAŁA:
 *   1. {@code transactionTemplate.execute(callback)} — kod Springa (uproszczony):
 *      {@code status = tm.getTransaction(this)} → {@code result = callback.doInTransaction(status)} →
 *      wyjątek RuntimeException/Error: {@code tm.rollback(status)} i dalej ten sam wyjątek; inny Throwable: rollback
 *      i {@code UndeclaredThrowableException}; bez wyjątku: {@code tm.commit(status)} (który zrobi rollback, jeśli
 *      ktoś ustawił {@code setRollbackOnly}) → {@code return result}.
 *   2. {@code TransactionTemplate} JEST definicją transakcji (dziedziczy po {@code DefaultTransactionDefinition}):
 *      jego settery (propagacja, izolacja, readOnly, timeout, nazwa) odpowiadają atrybutom {@code @Transactional}.
 *   3. Boot tworzy bean {@code transactionTemplate} (autokonfiguracja transakcji) na domyślnym menedżerze — tu
 *      {@code JpaTransactionManager}; ustawienia: PROPAGATION_REQUIRED, ISOLATION_DEFAULT, bez readOnly i timeoutu.
 *   4. {@code TransactionSynchronizationManager.registerSynchronization(…)} dopisuje „słuchacza” do listy
 *      trwającej transakcji (ThreadLocal); menedżer woła go przy commicie/rollbacku.
 *
 * SŁÓWKA:
 *   template = szablon; callback = wywołanie zwrotne (kod przekazany do wykonania); execute = wykonaj;
 *   without result = bez wyniku; status = stan (transakcji); rollback-only = tylko do wycofania;
 *   new transaction = nowa transakcja; definition = definicja; platform transaction manager = menedżer transakcji
 *   platformy; synchronization = synchronizacja; before commit = przed zatwierdzeniem; after commit = po zatwierdzeniu;
 *   after completion = po zakończeniu; batch = partia; dry run = próba na sucho; undeclared = niezadeklarowany;
 *   declarative = deklaratywny (opisany adnotacją); programmatic = programowy (zapisany w kodzie).
 *
 * ZOBACZ TEŻ: s10_transactions/Tx01Transactional (rollback i wyjątki, setRollbackOnly w @Transactional),
 *             s10_transactions/Tx02Propagation (REQUIRES_NEW, drugie połączenie), s10_transactions/Tx03ProxyPitfalls
 *             (kiedy proxy nie działa — tam szablon jest obejściem), s10_transactions/Tx04ReadOnlyIsolation
 *             (readOnly, izolacja, krótkie transakcje), t29_jdbc_databases/Jdbc04Transactions (to samo w czystym
 *             JDBC: setAutoCommit(false), commit, rollback), s17_events (@TransactionalEventListener — zdarzenia po
 *             commicie w stylu deklaratywnym).
 * </pre>
 */
public final class Tx05ProgrammaticTransactions {

    private Tx05ProgrammaticTransactions() {
    }

    // =================================================================================================
    // 1. TransactionTemplate.execute / executeWithoutResult — kiedy lepszy niż adnotacja (OrderImporter, PlainOrderWriter)
    // =================================================================================================
    //
    //   {@code Long count = tx.execute(status -> { …; return wynik; });}    // z wynikiem
    //   {@code tx.executeWithoutResult(status -> { … });}                     // bez wyniku
    //
    // Kiedy szablon zamiast {@code @Transactional}?
    //   a) Partia, w której KAŻDY element ma osobną transakcję (OrderImporter.importEach): 4 pozycje, z czego jedna
    //      ma kwotę ujemną (CHECK), a jedna powtarza numer (UNIQUE) → zapisane ZAM-1 i ZAM-3, odrzucone ZAM-2 i drugi
    //      ZAM-1 (test). Wyjątek wylatuje z {@code executeWithoutResult} (rollback tylko tej pozycji) i łapiemy go
    //      POZA transakcją. Dla porównania {@code @Transactional importAllOrNothing}: jedna zła pozycja → nic nie
    //      zostaje (test). PUŁAPKA: gdyby importEach miała {@code @Transactional}, szablony w środku DOŁĄCZYŁYBY do
    //      jednej transakcji (REQUIRED) i wrócilibyśmy do „wszystko albo nic” — z UnexpectedRollbackException
    //      na końcu (test importEachInsideOneTransaction; mechanizm: s10_transactions/Tx02Propagation, sekcja 2).
    //   b) Tylko CZĘŚĆ metody ma być w transakcji (OrderImporter.saveWithPreparation): przygotowanie danych bez
    //      transakcji ({@code isActualTransactionActive()} = false), zapis w {@code execute} (= true) — test.
    //      Dlaczego to ważne? Krótsza transakcja = krócej zajęte połączenie i blokady (Tx04, sekcja 5).
    //   c) Kod bez proxy (PlainOrderWriter — obiekt z {@code new}, metoda prywatna wołana przez this): szablon to
    //      zwykłe wywołanie metody, więc działa wszędzie (test: błąd drugiego INSERT-u wycofuje też pierwszy).
    //      Porównaj s10_transactions/Tx03ProxyPitfalls: tam {@code @Transactional} w tych miejscach milczał.
    //
    // Wyjątki w lambdzie: {@code TransactionCallback} nie deklaruje {@code throws}, więc wyjątku checked nie rzucisz
    // wprost. Gdy jednak wyleci (sztuczka „sneaky throw” w teście) — rollback i
    // {@code UndeclaredThrowableException: TransactionCallback threw undeclared checked exception} (test).
    // Inaczej niż w {@code @Transactional} (Tx01, sekcja 3): szablon wycofuje przy KAŻDYM wyjątku, a reguł
    // {@code rollbackFor}/{@code noRollbackFor} nie ma — chcesz commit mimo błędu? Złap wyjątek w lambdzie.
    //
    // DOBRA PRAKTYKA: wstrzykuj bean {@code TransactionTemplate} (albo buduj własny na {@code PlatformTransactionManager})
    //   w konstruktorze, jak każdą zależność. Dlaczego? Jest bezpieczny wątkowo i bezstanowy w trakcie
    //   {@code execute} — jedna instancja obsłuży całą aplikację.

    // =================================================================================================
    // 2. status.setRollbackOnly() i własne ustawienia szablonu (TemplateSettingsShowcase)
    // =================================================================================================
    //
    //   {@code status.setRollbackOnly()} — „na końcu wycofaj”: bez wyjątku, wynik lambdy wraca normalnie.
    //   TemplateSettingsShowcase.dryRunInsert: w środku {@code COUNT(*)} = 1, po wyjściu baza pusta (test).
    //   Odpowiednik w {@code @Transactional}: {@code TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()}
    //   (Tx01, sekcja 4) — tu status masz pod ręką jako parametr lambdy.
    //
    //   Własne ustawienia = NOWA instancja szablonu, tworzona raz (w konstruktorze):
    //     {@code reportTx = new TransactionTemplate(transactionManager);}
    //     {@code reportTx.setName("monthlyReport");}                                 // nazwa transakcji
    //     {@code reportTx.setReadOnly(true);}                                        // = readOnly = true
    //     {@code reportTx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);} // = isolation
    //     {@code reportTx.setTimeout(10);}                                           // = timeout = 10 (sekund)
    //     {@code requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);} // = propagation
    //   Sprawdzone testem: w środku nazwa „monthlyReport”, readOnly = true, baza pokazuje „REPEATABLE READ”, a log:
    //   {@code Creating new transaction with name [monthlyReport]: PROPAGATION_REQUIRED,ISOLATION_REPEATABLE_READ,}
    //   {@code timeout_10,readOnly}. (Nazwa domyślnego szablonu to null — {@code @Transactional} nadaje nazwę
    //   „pakiet.Klasa.metoda” sam.)
    //
    //   {@code status.isNewTransaction()} (TemplateSettingsShowcase.nesting, test): zewnętrzny szablon → true;
    //   wspólny szablon (REQUIRED) w środku → false (dołączył); szablon REQUIRES_NEW w środku → true i INNA sesja H2.
    //
    // PUŁAPKA: zmiana ustawień WSPÓLNEGO beana ({@code transactionTemplate.setReadOnly(true)} w jednym serwisie).
    //   Dlaczego groźne? To singleton — od tej chwili KAŻDY, kto go używa, dostaje transakcje read-only (a zapisy
    //   JPA przepadają po cichu — Tx04, sekcja 1). Testy sprawdzają, że wspólny bean ma ustawienia domyślne.
    //
    // DOBRA PRAKTYKA: jeden szablon na jeden zestaw ustawień, z nazwą mówiącą, do czego służy (reportTx,
    //   requiresNewTx). Dlaczego? Ustawienia są widoczne w jednym miejscu, a szablonów nie tworzy się w pętli.

    // =================================================================================================
    // 3. PlatformTransactionManager wprost — to robi proxy pod spodem (ManualOrderService)
    // =================================================================================================
    //
    //   {@code DefaultTransactionDefinition definition = new DefaultTransactionDefinition(); // REQUIRED, DEFAULT}
    //   {@code definition.setName("manualPlaceOrder");}
    //   {@code TransactionStatus status = transactionManager.getTransaction(definition);   // begin albo dołącz}
    //   {@code try { …praca… } catch (RuntimeException | Error e) { transactionManager.rollback(status); throw e; }}
    //   {@code transactionManager.commit(status);                                          // POZA try}
    //
    // Log (test): {@code Creating new transaction with name [manualPlaceOrder]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT},
    // potem {@code Initiating transaction commit}. Błąd drugiego zamówienia (kwota 0) → rollback zamówienia i wpisu
    // audytu (test). Tak samo działa {@code TransactionInterceptor} (proxy {@code @Transactional}) — z dodatkiem reguł
    // rollbackFor i odczytu adnotacji.
    // Dlaczego commit POZA try? Nieudany commit też KOŃCZY transakcję. Gdyby commit był w try, catch zawołałby
    // rollback na transakcji już zakończonej — test: commit rzuca UnexpectedRollbackException, a rollback po nim
    // {@code IllegalTransactionStateException: Transaction is already completed …}, który zasłoniłby pierwotny błąd.
    //
    // PUŁAPKA: drugi commit (albo commit po rollbacku) tej samej transakcji (ManualOrderService.commitTwice) →
    //   {@code IllegalTransactionStateException: Transaction is already completed - do not call commit or rollback more}
    //   {@code than once per transaction} (test). PUŁAPKA: zapomniany commit/rollback (np. return w środku bez finally)
    //   — połączenie zostaje przypięte do wątku i nie wraca do puli. Dlaczego to ważne? To dlatego zwykle NIE piszemy
    //   tego ręcznie: szablon i proxy zawsze kończą transakcję.
    // DOBRA PRAKTYKA: menedżera wprost używaj tylko, gdy początek i koniec transakcji są w RÓŻNYCH miejscach kodu
    //   (np. własna integracja z biblioteką, która ma osobne wywołania „start” i „koniec”). W pozostałych — szablon.

    // =================================================================================================
    // 4. Akcje po transakcji — TransactionSynchronization: afterCommit, afterCompletion (OrderNotifications)
    // =================================================================================================
    //
    //   PRZED (placeOrderSendingMailImmediately): zapis zamówienia, {@code mail.send(…)}, potem błąd → zamówienia nie ma,
    //   a klient dostał „Przyjęliśmy zamówienie ZAM-1” (test). E-maila nie wycofa żaden rollback.
    //
    //   PO (placeOrderSendingMailAfterCommit):
    //     {@code TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {}}
    //     {@code     public void afterCommit() { mail.send(…); }                  // tylko po udanym commicie}
    //     {@code     public void afterCompletion(int status) { … }               // zawsze: STATUS_COMMITTED / STATUS_ROLLED_BACK}
    //     {@code });}
    //   Test: ZAM-1 (sukces) → e-mail wysłany; ZAM-2 (błąd) → bez e-maila; afterCompletion: „ZAM-1: COMMITTED”,
    //   „ZAM-2: ROLLED_BACK”. Wszystkie metody interfejsu są {@code default} — nadpisujesz tylko potrzebne
    //   (jest też {@code beforeCommit(boolean readOnly)} i {@code beforeCompletion()}).
    //
    // PUŁAPKA: rejestracja bez trwającej transakcji → {@code IllegalStateException: Transaction synchronization is not}
    //   {@code active} (test). Dlaczego? Lista synchronizacji istnieje tylko w czasie transakcji.
    // PUŁAPKA: wyjątek z afterCommit (placeOrderWithFailingMail — „Serwer poczty nie odpowiada”) dociera do
    //   wywołującego, choć zamówienie JEST już zatwierdzone (test). Dlaczego groźne? Wywołujący myśli „nie udało się”
    //   i ponawia → duplikat. W afterCommit łap i loguj błędy (albo zapisz zadanie do ponowienia).
    // PUŁAPKA: praca z bazą w afterCommit (placeOrderAndAuditInAfterCommit). Transakcja jest zatwierdzona, ale zasoby
    //   są wciąż przypięte do wątku: {@code isActualTransactionActive()} = true (test). Dokumentacja Springa
    //   ({@code TransactionSynchronization.afterCommit}) ostrzega: taki kod wciąż „uczestniczy” w starej transakcji
    //   i commit już po nim NIE nastąpi — zapis wymaga REQUIRES_NEW. W tym module (JPA + H2) połączenie jest już
    //   w trybie auto-commit ({@code SELECT AUTOCOMMIT()} = true — test), więc INSERT zapisał się od razu — poza
    //   jakąkolwiek transakcją. To szczegół konkretnego menedżera i bazy, nie gwarancja: zapis w afterCommit rób
    //   w nowej transakcji (szablon z PROPAGATION_REQUIRES_NEW).
    //
    // Zapowiedź: s17_events — {@code @TransactionalEventListener} (słuchacz zdarzeń transakcyjnych) robi to samo
    // deklaratywnie: zdarzenie opublikowane w transakcji trafia do słuchacza domyślnie w fazie AFTER_COMMIT.
    // PYTANIE REKRUTACYJNE: „Jak wysłać e-mail dopiero po zatwierdzeniu transakcji?”

    // =================================================================================================
    // 5. Porównanie: deklaratywnie vs programowo
    // =================================================================================================
    //
    //   cecha                         @Transactional (deklaratywnie)        TransactionTemplate (programowo)
    //   zasięg                        cała metoda (nie private; przez proxy) dowolny blok kodu (lambda)
    //   wymaga proxy / beana          tak (Tx03)                             nie — zwykłe wywołanie metody
    //   kilka transakcji w metodzie   nie (tylko przez inne beany)          tak (np. pętla, transakcja na element)
    //   reguły rollbacku              rollbackFor / noRollbackFor            każdy wyjątek → rollback
    //   setRollbackOnly               TransactionAspectSupport…              status.setRollbackOnly()
    //   ustawienia                    atrybuty adnotacji                     settery szablonu (osobna instancja)
    //   czytelność logiki biznesowej  wysoka — transakcje „niewidoczne”      niższa — kod transakcji w środku
    //
    // DOBRA PRAKTYKA: domyślnie {@code @Transactional} na metodach serwisu; {@code TransactionTemplate} dla przypadków
    //   specjalnych (partie, krótki fragment metody, miejsca bez proxy, testy otwierające transakcję „z zewnątrz” —
    //   jak w testach Tx02). Dlaczego? Adnotacja oddziela logikę od infrastruktury i trudniej o niej zapomnieć;
    //   szablon daje precyzję tam, gdzie adnotacja nie sięga.
    // Wzmianka: w aplikacjach reaktywnych (WebFlux, R2DBC) odpowiednikiem szablonu jest {@code TransactionalOperator}
    //   (operator transakcyjny) — transakcja nie może tam wisieć na wątku (ThreadLocal), więc idzie w kontekście
    //   strumienia.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • {@code tx.execute(status -> wynik)} / {@code tx.executeWithoutResult(status -> …)}; bean z Boota: REQUIRED,
     *     DEFAULT; działa bez proxy.
     *   • Każdy wyjątek z lambdy → rollback (checked → UndeclaredThrowableException); {@code status.setRollbackOnly()}
     *     → rollback bez wyjątku.
     *   • Osobne ustawienia = osobna instancja: {@code new TransactionTemplate(tm)} + setName/setReadOnly/
     *     setIsolationLevel/setTimeout/setPropagationBehavior. Wspólnego beana nie zmieniaj.
     *   • Partia: transakcja na element, wyjątek łapany POZA szablonem, metoda zewnętrzna BEZ @Transactional.
     *   • {@code tm.getTransaction(def)} → praca → {@code tm.commit(status)} / {@code tm.rollback(status)} —
     *     raz; commit poza try.
     *   • {@code registerSynchronization}: afterCommit (tylko sukces), afterCompletion(STATUS_COMMITTED /
     *     STATUS_ROLLED_BACK); wymaga trwającej transakcji; wyjątek z afterCommit dociera do wywołującego po commicie;
     *     zapis do bazy w afterCommit — tylko w REQUIRES_NEW.
     *   • Deklaratywnie domyślnie, programowo dla przypadków specjalnych; reaktywnie: TransactionalOperator.
     *
     * PYTANIA KONTROLNE:
     *   1. Kiedy wybierzesz TransactionTemplate zamiast {@code @Transactional}? Podaj trzy sytuacje.
     *   2. Co się stanie? {@code tx.execute(status -> { insert(…); status.setRollbackOnly(); return 42; })} — co zwróci
     *      wywołanie i co będzie w bazie?
     *   3. ZNAJDŹ BŁĄD: {@code @Transactional public void importAll(List<…> lines) { for (…) try {}
     *      {@code tx.executeWithoutResult(s -> insert(line)); } catch (DataAccessException e) { log(…); } }} —
     *      „miała być transakcja na element, a jedna zła pozycja psuje cały import”.
     *   4. ZNAJDŹ BŁĄD: {@code transactionTemplate.setReadOnly(true)} w konstruktorze serwisu raportów (wstrzyknięty bean
     *      z Boota). Co się stanie w innych serwisach?
     *   5. Jak wygląda poprawna obsługa transakcji przez PlatformTransactionManager wprost? Dlaczego commit jest poza try?
     *   6. Co się stanie? W {@code @Transactional} metodzie rejestrujesz afterCommit wysyłający e-mail, a potem metoda
     *      rzuca RuntimeException. Czy e-mail wyjdzie? A co pokaże afterCompletion?
     *   7. Co się stanie, gdy afterCommit rzuci wyjątek? Czy dane są w bazie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w pliku ProgrammaticExercises.java, testy: Tx05ProgrammaticTransactionsExercisesTest
    // (pełna aplikacja, bez transakcji testu).
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. (a) partia z osobną transakcją na element — błąd jednego nie cofa reszty; (b) tylko fragment metody ma być
     *      w transakcji (krótsza transakcja, np. bez wywołań zewnętrznych); (c) kod bez proxy — obiekt z new, metoda
     *      prywatna, wywołanie this; także w testach, by otworzyć transakcję „z zewnątrz”.
     *   2. Zwróci 42 (wynik lambdy wraca normalnie), a w bazie nic nie będzie — szablon zamiast commitu zrobi rollback
     *      (test dryRunInsert: w środku COUNT = 1, po wyjściu 0).
     *   3. {@code @Transactional} na importAll: wszystkie szablony w pętli DOŁĄCZAJĄ (REQUIRED) do jednej transakcji.
     *      Błąd pozycji oznacza całość jako rollback-only, więc na końcu importAll dostaje UnexpectedRollbackException
     *      i nic nie zostaje. Naprawa: usuń {@code @Transactional} z importAll (albo szablon z REQUIRES_NEW).
     *   4. Bean {@code transactionTemplate} jest singletonem — wszystkie serwisy, które go używają, dostaną transakcje
     *      read-only (zapisy JPA po cichu przepadną). Naprawa: {@code new TransactionTemplate(transactionManager)}
     *      z własnymi ustawieniami.
     *   5. {@code status = tm.getTransaction(def); try { praca } catch (RuntimeException | Error e) { tm.rollback(status);}
     *      {@code throw e; } tm.commit(status);} — commit poza try, bo gdy sam commit zawiedzie, transakcja jest już
     *      zakończona i rollback w catch rzuciłby IllegalTransactionStateException, zasłaniając pierwotny błąd.
     *   6. Nie — afterCommit wywołuje się tylko po udanym commicie; afterCompletion dostanie STATUS_ROLLED_BACK
     *      (test: „ZAM-2: ROLLED_BACK”, brak e-maila).
     *   7. Wyjątek dociera do wywołującego, ale commit już się odbył — dane SĄ w bazie (test: zamówienie zapisane mimo
     *      „Serwer poczty nie odpowiada”). Dlatego w afterCommit łapiemy i logujemy błędy.
     */
    // </editor-fold>
}
