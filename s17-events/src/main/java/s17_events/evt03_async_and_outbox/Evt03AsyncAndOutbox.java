package s17_events.evt03_async_and_outbox;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Set;
import java.util.function.Consumer;

/**
 * <pre>
 * TEMAT: Zdarzenia asynchroniczne i wzorzec outbox
 *        (async = asynchroniczny, „w innym wątku, bez czekania”; outbox = skrzynka nadawcza; relay = przekaźnik;
 *         at-least-once = „co najmniej raz”; idempotent = idempotentny, czyli bezpieczny przy powtórzeniu)
 *
 * W SKRÓCIE:
 *   Słuchacz może działać w osobnym wątku ({@code @Async}) — wtedy publikujący nie czeka, ale zyskuje nowe kłopoty: słuchacz
 *   nie ma transakcji publikującego, a jego wyjątek ginie w logu. Głębszy problem: zdarzenia Springa żyją w pamięci. Jeśli
 *   zamówienie zostanie zatwierdzone, a powiadomienie nie zdąży wyjść (serwer poczty padł, proces zabito), zdarzenie
 *   przepada i nic o nim nie wie. Lekarstwem jest wzorzec OUTBOX: wiadomość do wysłania zapisujesz w TEJ SAMEJ transakcji bazy co
 *   zamówienie, a wysyła ją osobny przekaźnik — i ponawia, aż się uda.
 *
 * ANALOGIA: zamówienie przez telefon i notatnik.
 *   Zamiast „zapamiętam i zaraz zadzwonię do magazynu” (a jak mnie ktoś zawoła — zapomnę), zapisujesz zamówienie ORAZ
 *   zlecenie „zadzwonić do magazynu” w tym samym zeszycie, jednym ruchem pióra. Potem co jakiś czas przeglądasz
 *   zeszyt: niewykonane zlecenia realizujesz i odhaczasz. Magazyn może dostać telefon dwa razy (jeśli zadzwoniłeś,
 *   a zanim zdążyłeś odhaczyć, ktoś Cię zawołał) — dlatego magazyn musi umieć powiedzieć „to zamówienie już mam”.
 *
 * JAK TO DZIAŁA:
 *   1. {@code @EnableAsync} włącza obsługę {@code @Async}; Spring Boot dostarcza pulę wątków, której wątki nazywają się
 *      {@code task-1}, {@code task-2}… (szczegóły: s16_cache_scheduling_async/Async04Async).
 *   2. Metoda {@code @Async @EventListener} (albo {@code @Async @TransactionalEventListener}) wykonuje się w wątku z puli.
 *   3. Zwykły słuchacz po commicie (poprzednia lekcja) jest w pamięci: gdy zawiedzie lub proces padnie, zdarzenie znika.
 *   4. Outbox: w transakcji zapisujesz zamówienie ORAZ wiersz w tabeli {@code outbox} (co wysłać). Transakcja jest atomowa,
 *      więc oba zapisy mają ten sam los: razem się zatwierdzą albo razem wycofają.
 *   5. Przekaźnik (relay) czyta niewysłane wiersze, wysyła je i DOPIERO po sukcesie oznacza jako wysłane; przy błędzie zwiększa
 *      licznik prób i zostawia wiersz na następny przebieg. Wołasz go z harmonogramu (@Scheduled — dział s16) albo ręcznie.
 *   6. Skutek: gwarancja „co najmniej raz” (at-least-once): wiadomość dojdzie, ale może dojść dwa razy → odbiorca
 *      rozpoznaje duplikaty po identyfikatorze (idempotentny odbiorca).
 *
 * SŁÓWKA:
 *   async = asynchronicznie; thread = wątek; pool = pula; outbox = skrzynka nadawcza; relay = przekaźnik; retry = ponowienie;
 *   attempts = próby; delivery = dostarczenie; duplicate = duplikat; idempotent = idempotentny (wielokrotne wykonanie
 *   daje ten sam skutek co jedno); atomic = atomowy, „wszystko albo nic”.
 *
 * ZOBACZ TEŻ: s17_events/Evt01ApplicationEvents (zdarzenia synchroniczne), s17_events/Evt02TransactionalEvents (fazy transakcji),
 *             s16_cache_scheduling_async/Async04Async (@Async od środka), s16_cache_scheduling_async/Sched03Scheduled (@Scheduled),
 *             s10_transactions/Tx01Transactional (atomowość transakcji),
 *             s20_modulith/Mod02ModuleEvents (rejestr publikacji zdarzeń w Spring Modulith — outbox „z pudełka”),
 *             s22_extras/Ext02Messaging (kolejki: Kafka, RabbitMQ),
 *             t21_concurrency/Concurrency01Threads (wątki w czystej Javie).
 * </pre>
 */
public final class Evt03AsyncAndOutbox {

    private Evt03AsyncAndOutbox() {
    }

    // =================================================================================================
    // 1. @Async NA SŁUCHACZU: PUBLIKUJĄCY NIE CZEKA, SŁUCHACZ DZIAŁA W INNYM WĄTKU I BEZ TRANSAKCJI
    // =================================================================================================
    //
    //   @Async                         // async = w osobnym wątku
    //   @EventListener
    //   public void onGated(GatedEvent event) { ... }
    //
    // Test sekcji 1 jest deterministyczny dzięki „bramce” (CountDownLatch): słuchacz czeka na niej, a test sprawdza,
    // że publishEvent już wrócił, a dziennik jest pusty. Potem otwiera bramkę i czeka (Awaitility) na wpis:
    //   async:pierwsze (transakcja aktywna=false)     — wątek: task-N (inny niż wątek testu)
    //
    // PUŁAPKA: @Async bez @EnableAsync. Dlaczego? Adnotacja nie robi nic sama z siebie — obsługuje ją dopiero
    //   postprocesor włączany przez @EnableAsync. Test sekcji 1b pokazuje, że bez niej metoda wykonuje się
    //   zwyczajnie, w wątku wywołującego, bez żadnego błędu (nawet gdy Boot dostarcza pulę wątków: Boot daje pulę, nie włącza @Async).
    //
    // PUŁAPKA: oczekiwanie, że asynchroniczny słuchacz „dziedziczy” transakcję i kontekst publikującego.
    //   Dlaczego? Transakcja jest związana z wątkiem (ThreadLocal), a słuchacz biegnie w innym — test pokazuje
    //   „transakcja aktywna=false”. Podobnie nie przechodzi inny kontekst powiązany z wątkiem (MDC w logach, kontekst
    //   bezpieczeństwa) — szczegóły w lekcji Async04Async. Słuchacz musi sam otwierać transakcje, jeśli ich potrzebuje.

    // =================================================================================================
    // 2. WYJĄTEK W ASYNCHRONICZNYM SŁUCHACZU: TYLKO LOG
    // =================================================================================================
    //
    // Test sekcji 2: publikujący nie dostaje żadnego wyjątku, a w logu (ERROR, wątek task-N) pojawia się komunikat
    // z prawdziwego uruchomienia:
    //   SimpleAsyncUncaughtExceptionHandler : Unexpected exception occurred invoking async method:
    //   public void s17_events.evt03_async_and_outbox.AsyncListeners.onFailing(...FailingAsyncEvent)
    //   (wraz z wyjątkiem „Asynchroniczny słuchacz zawiódł: test”)
    // To zachowanie dla metod zwracających void. Własną obsługę podpinasz przez AsyncUncaughtExceptionHandler
    // (lekcja Async04Async).
    //
    // PUŁAPKA: uznanie, że „skoro nic nie spadło na wywołującego, to się udało”.
    //   Dlaczego? Błąd jest tylko w logu; operacja po cichu nie została wykonana. Przy ważnych skutkach (płatność, wysyłka
    //   zamówienia) nie polegaj na samym @Async — zapisz wiadomość trwale (outbox, sekcje 5–7).

    // =================================================================================================
    // 3. @Async + @TransactionalEventListener: PO COMMICIE, W OSOBNYM WĄTKU
    // =================================================================================================
    //
    //   @Async
    //   @TransactionalEventListener            // faza AFTER_COMMIT
    //   public void afterCommitAsync(OrderPlacedAsync event) { ... }
    //
    // Połączenie obu adnotacji daje najczęstszy wariant „wyślij powiadomienie po zatwierdzeniu, ale nie blokuj
    // żądania”. Test sekcji 3 wypisuje z osobnego wątku:
    //   async-po-commicie:ZAM-001 (zamówień w bazie=1, transakcja aktywna=false)
    // — zamówienie jest już widoczne w bazie (commit zaszedł), a słuchacz nie ma transakcji i sam odczytuje dane.
    // Uwaga: słuchacz zapisujący do bazy musi mieć własną transakcję (Evt02TransactionalEvents, sekcja 6).
    //
    // DOBRA PRAKTYKA: do powiadomień na zewnątrz łącz @Async z @TransactionalEventListener (po commicie).
    //   Dlaczego? Powiadomienie nie opóźnia odpowiedzi i nie wychodzi dla wycofanego zamówienia. Ale pamiętaj — to wciąż
    //   zdarzenie „w pamięci”: patrz następna sekcja.

    // =================================================================================================
    // 4. PROBLEM: ZDARZENIE PO COMMICIE MOŻE PRZEPAŚĆ
    // =================================================================================================
    //
    // Test sekcji 4: słuchacz po commicie wysyła powiadomienie, a „serwer poczty” jest wyłączony. Wynik:
    //   • zamówienie jest w bazie (transakcja zatwierdzona),
    //   • powiadomienie nie wyszło, wyjątek został tylko w logu (lekcja Evt02TransactionalEvents, sekcja 4b),
    //   • po „naprawie serwera” NIKT do tego nie wraca — zdarzenie przepadło, bo nigdzie go nie zapisano.
    // To samo stanie się, gdy proces zostanie zabity między commitem a wysyłką (restart, awaria, OOM). Zdarzenia
    // Springa nie są trwałe i nie mają ponowień.
    //
    // PUŁAPKA: zakładanie, że @TransactionalEventListener(AFTER_COMMIT) gwarantuje dostarczenie.
    //   Dlaczego? Gwarantuje tylko, że uruchomi się PO commicie — nie, że się uda ani że w ogóle się wykona (awaria procesu).
    //   Dwa zapisy w różnych systemach (baza + serwer poczty/kolejka) nie mogą być atomowe — to problem „podwójnego zapisu”.

    // =================================================================================================
    // 5. OUTBOX: WIADOMOŚĆ ZAPISANA W TEJ SAMEJ TRANSAKCJI CO ZAMÓWIENIE
    // =================================================================================================
    //
    //   @Transactional
    //   public void placeOrder(String orderId, String customer, BigDecimal totalPln) {
    //       repository.insertOrder(orderId, customer, totalPln);
    //       repository.insertOutbox(orderId, "OrderPlaced", "orderId=" + orderId + ";customer=" + customer);
    //   }
    //
    // Obie operacje to zapisy w jednej bazie, więc są atomowe. Test sekcji 5: po udanym złożeniu jest 1 zamówienie i 1
    // niewysłana wiadomość (attempts=0); gdy metoda rzuca wyjątek po zapisach, wycofane zostają OBA — zamówienie i wiadomość.
    // Nie istnieje stan „zamówienie jest, a wiadomości nie ma” (ani odwrotnie). Tabela outbox (migracja V1 lekcji):
    //   id, order_id, type, payload, sent (czy wysłana), attempts (liczba nieudanych prób).
    //
    // DOBRA PRAKTYKA: zapisuj wiadomość do outboxa w tej samej transakcji i tym samym źródle danych co zmianę biznesową.
    //   Dlaczego? Tylko wtedy baza gwarantuje „wszystko albo nic”. Zapis outboxa w osobnej transakcji (REQUIRES_NEW) lub
    //   w innej bazie przywraca problem podwójnego zapisu.

    // =================================================================================================
    // 6. PRZEKAŹNIK: WYSYŁA, ODHACZA PO SUKCESIE, PONAWIA PRZY BŁĘDZIE
    // =================================================================================================
    //
    //   for (OutboxMessage message : repository.findUnsent()) {
    //       try { sender.send(message); repository.markSent(message.id()); }
    //       catch (RuntimeException e) { repository.incrementAttempts(message.id()); }
    //   }
    //
    // Test sekcji 6: serwer wyłączony → wysłano 0, wiadomość zostaje w skrzynce z attempts=1; po naprawie
    // następny przebieg wysyła 1 wiadomość, skrzynka pustoszeje, kolejny przebieg nie wysyła nic. Przekaźnik w tej lekcji
    // wołamy jawnie; w aplikacji wywołuje go harmonogram (@Scheduled — lekcja Sched03Scheduled) co kilka sekund.
    //
    // PUŁAPKA: oznaczanie wiadomości jako wysłanej PRZED wysyłką.
    //   Dlaczego? Jeśli proces padnie między oznaczeniem a wysyłką, wiadomość jest „odhaczona”, a nigdy nie wyszła — wracamy
    //   do tracenia zdarzeń. Odhaczaj dopiero po udanej wysyłce.

    // =================================================================================================
    // 7. CO NAJMNIEJ RAZ I IDEMPOTENTNY ODBIORCA
    // =================================================================================================
    //
    // Skoro odhaczamy po wysyłce, możliwy jest odwrotny kłopot: wiadomość wyszła, proces padł przed odhaczeniem.
    // Po restarcie przekaźnik wyśle ją ponownie. Test sekcji 7 to odtwarza: ręczne send(...) bez odhaczenia, potem
    // przebieg przekaźnika — odbiorca ma dwa wywołania (calls=2), ale jedną unikalną wiadomość (delivered=1), bo rozpoznaje
    // duplikaty po id. Gwarancji „dokładnie raz” między dwoma systemami w praktyce nie ma — jest „co najmniej raz” plus
    // odbiorca, który poradzi sobie z powtórką (idempotentny: zapamiętuje przetworzone id albo robi operację w sposób,
    // który powtórzony nic nie psuje).
    //
    // PYTANIE REKRUTACYJNE: czy da się dostarczyć wiadomość „dokładnie raz”? Odpowiedź: między niezależnymi systemami
    //   praktycznie nie — wybierasz „co najwyżej raz” (możesz zgubić) albo „co najmniej raz” (możesz zdublować) i projektujesz
    //   odbiorcę jako idempotentnego. Outbox daje „co najmniej raz”.
    //
    // DOBRA PRAKTYKA: nadawaj wiadomościom stabilny identyfikator i sprawdzaj go po stronie odbiorcy.
    //   Dlaczego? Ponowienia i duplikaty są normalne, a nie wyjątkowe. Bez identyfikatora klient dostanie dwa
    //   powiadomienia albo dwa razy naliczone punkty.

    // =================================================================================================
    // 8. KIEDY CO
    // =================================================================================================
    //
    // • Zwykłe @EventListener: reakcje wewnątrz transakcji i procesu, ważna spójność z bazą (Evt01ApplicationEvents).
    // • @TransactionalEventListener (AFTER_COMMIT): skutki poboczne, które mogą się czasem nie udać (np. odświeżenie
    //   pamięci podręcznej, mało ważne powiadomienie).
    // • @Async dodaj, gdy skutek jest wolny, a wynik niepotrzebny od razu.
    // • Outbox: skutek MUSI w końcu zajść (potwierdzenie zamówienia, zlecenie do magazynu, zdarzenie dla innego serwisu).
    //   Koszt: tabela, przekaźnik, duplikaty, sprzątanie starych wierszy. Gdy działa kilka instancji aplikacji, każda może
    //   chcieć wysłać ten sam wiersz — kolejny powód, by odbiorca był idempotentny.
    // • Gotowe rozwiązania: rejestr publikacji zdarzeń w Spring Modulith (dział s20_modulith) robi outbox za Ciebie;
    //   kolejki i brokery (Kafka, RabbitMQ — dział s22_extras) przejmują trwałość i ponowienia.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @Async na słuchaczu = osobny wątek (task-N), publikujący nie czeka. Wymaga @EnableAsync.
     *   • Słuchacz asynchroniczny nie ma transakcji publikującego; jego wyjątek idzie tylko do logu (dla void).
     *   • @Async + @TransactionalEventListener: osobny wątek PO commicie — wygodne, ale wciąż zdarzenie „w pamięci”.
     *   • Zdarzenie po commicie może przepaść (błąd wysyłki, awaria procesu) — nic go nie ponowi.
     *   • Outbox: wiadomość do tabeli w TEJ SAMEJ transakcji co zmiana; przekaźnik wysyła, odhacza PO sukcesie, ponawia.
     *   • Gwarancja outboxa: co najmniej raz → odbiorca idempotentny (rozpoznaje duplikaty po id).
     *   • Testy bez sleep: bramka (CountDownLatch) i Awaitility; przekaźnik wołaj jawnie.
     *
     * PYTANIA KONTROLNE:
     *   1. Co się stanie? Dodajesz @Async do metody słuchacza, ale w aplikacji nie ma @EnableAsync.
     *   2. Co się stanie? Asynchroniczny słuchacz (metoda void) rzuca wyjątek — czy zobaczy go kod, który opublikował zdarzenie?
     *   3. Co się stanie? Słuchacz po commicie wysyła powiadomienie, a serwer poczty jest wyłączony — co z zamówieniem,
     *      a co z powiadomieniem po naprawie serwera?
     *   4. ZNAJDŹ BŁĄD: przekaźnik najpierw wykonuje markSent(id), a potem sender.send(message). Co może pójść źle?
     *   5. Co się stanie? Wiadomość została wysłana, ale aplikacja padła, zanim przekaźnik zdążył ją odhaczyć. Co zrobi
     *      przekaźnik po restarcie i jak powinien zareagować odbiorca?
     *   6. Dlaczego wiadomość outbox zapisujemy w tej samej transakcji co zamówienie?
     *   7. ZNAJDŹ BŁĄD: serwis zapisuje zamówienie w transakcji, a wiadomość do outboxa w metodzie z
     *      REQUIRES_NEW po niej. Czym to grozi?
     *   8. Kiedy wystarczy zwykły @TransactionalEventListener, a kiedy potrzebujesz outboxa?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Evt03AsyncAndOutboxExercisesTest (baza H2 ze schematem migracji V1 lekcji)
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): w JEDNEJ transakcji ({@code transactions.execute...}) zapisz zamówienie do tabeli orders
     * (id, customer, total — użyj kwoty 349.90) oraz wiadomość do tabeli outbox (order_id, type = 'OrderPlaced', payload =
     * "orderId=" + orderId + ";customer=" + customer). Gdy {@code failAfterInserts} jest true, po zapisach rzuć
     * {@code new IllegalStateException("Płatność odrzucona")} — wtedy w bazie nie ma nic. Podpowiedź:
     * {@code transactions.executeWithoutResult(status -> { ... })}, a {@code jdbc.sql("INSERT ...").param(...).update()}.
     */
    static void exercise1(TransactionTemplate transactions, JdbcClient jdbc, String orderId, String customer,
                          boolean failAfterInserts) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): przekaźnik. Dla każdej niewysłanej wiadomości (sent = FALSE, w kolejności id) wywołaj
     * {@code sender.accept(message)}; po sukcesie ustaw sent = TRUE, a gdy sender rzuci {@code RuntimeException} — zwiększ
     * attempts o 1 i idź do następnej wiadomości. Zwróć liczbę wysłanych. Podpowiedź:
     * {@code jdbc.sql("SELECT id, order_id, type, payload, attempts FROM outbox WHERE sent = FALSE ORDER BY id")
     * .query(OutboxMessage.class).list()} (nazwy kolumn z podkreślnikami Spring mapuje na pola rekordu).
     */
    static int exercise2(JdbcClient jdbc, Consumer<OutboxMessage> sender) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): idempotentny odbiorca. Jeśli identyfikator wiadomości jest już w {@code processedIds} — nie
     * rób nic i zwróć {@code false}. W przeciwnym razie zapamiętaj id, wywołaj {@code handler.accept(message)} i zwróć
     * {@code true}. Podpowiedź: {@code Set.add} zwraca false, gdy element już był.
     */
    static boolean exercise3(Set<Long> processedIds, OutboxMessage message, Consumer<OutboxMessage> handler) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): przekaźnik z limitem prób. Jak w ćwiczeniu 2, ale wiadomości, które mają już
     * {@code attempts >= maxAttempts}, POMIJAJ (nie wysyłaj ich i nie zmieniaj) — to „zatrute” wiadomości, którymi zajmie
     * się człowiek. Zwróć liczbę wysłanych. Podpowiedź: warunek w SQL ({@code attempts < :maxAttempts}) albo w Javie.
     */
    static int exercise4(JdbcClient jdbc, Consumer<OutboxMessage> sender, int maxAttempts) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Metoda wykona się zwyczajnie, synchronicznie, w wątku wywołującego — bez błędu i bez ostrzeżenia. Sprawdzone
     *      testem (sekcja 1b: bez @EnableAsync wynik jest gotowy od razu i w wątku testu; z @EnableAsync — w wątku task-N).
     *   2. Nie — publikujący nie dostaje wyjątku; w logu jest ERROR „Unexpected exception occurred invoking async method:
     *      public void ...AsyncListeners.onFailing(...)” (SimpleAsyncUncaughtExceptionHandler). Sprawdzone testem (sekcja 2).
     *   3. Zamówienie zostaje w bazie (transakcja była zatwierdzona), powiadomienie nie wychodzi, a po naprawie serwera
     *      nikt go nie ponawia — zdarzenie przepadło; wyjątek jest tylko w logu. Sprawdzone testem (sekcja 4).
     *   4. Jeśli proces padnie (albo send rzuci wyjątek) po markSent, a przed wysłaniem, wiadomość jest oznaczona jako
     *      wysłana, choć nigdy nie wyszła — i przepada. Odhaczać trzeba PO udanej wysyłce (sekcja 6). To wniosek z
     *      kolejności operacji, a nie osobny test; odwrotna kolejność (poprawna) jest sprawdzona w sekcjach 6 i 7.
     *   5. Po restarcie przekaźnik znajdzie wiadomość jako niewysłaną i wyśle ją drugi raz (co najmniej raz). Odbiorca
     *      powinien rozpoznać duplikat po id i go zignorować (idempotentność). Sprawdzone testem (sekcja 7).
     *   6. Bo wtedy baza gwarantuje atomowość: zamówienie i wiadomość zatwierdzają się albo wycofują razem — nie ma stanu
     *      „zamówienie jest, a wiadomości nie ma” ani odwrotnie. Sprawdzone testem (sekcja 5).
     *   7. Gdy aplikacja padnie albo druga transakcja zawiedzie między zapisami, zamówienie jest zatwierdzone, a wiadomości
     *      nie ma (albo odwrotnie) — wracamy do problemu podwójnego zapisu. Outbox musi być w tej samej transakcji.
     *      (Wniosek z zasad transakcji — Tx02Propagation; sekcja 5 pokazuje poprawną, jedną transakcję.)
     *   8. Wystarczy zwykły @TransactionalEventListener, gdy skutek może się czasem nie udać i nic wielkiego się nie stanie
     *      (odświeżenie pamięci podręcznej, mało ważne powiadomienie). Outbox — gdy skutek musi w końcu zajść
     *      (potwierdzenie zamówienia, zlecenie do magazynu, zdarzenie dla innego serwisu).
     */
    // </editor-fold>
}
