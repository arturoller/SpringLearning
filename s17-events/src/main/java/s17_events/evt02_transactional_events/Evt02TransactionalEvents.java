package s17_events.evt02_transactional_events;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * TEMAT: Zdarzenia powiązane z transakcją: @TransactionalEventListener
 *        (transactional = transakcyjny; phase = faza; commit = zatwierdzenie transakcji; rollback = wycofanie
 *         transakcji; fallback = rozwiązanie awaryjne, „plan B”)
 *
 * W SKRÓCIE:
 *   Zwykły @EventListener reaguje OD RAZU, w środku transakcji — jeszcze nie wiadomo, czy transakcja się uda.
 *   Wysłałby e-mail „dziękujemy za zamówienie”, a chwilę później zamówienie by się wycofało. @TransactionalEventListener
 *   czeka na wynik transakcji i reaguje w wybranej fazie: przed zatwierdzeniem, po zatwierdzeniu (domyślnie), po wycofaniu
 *   albo po zakończeniu (tak czy inaczej). Dzięki temu „skutki uboczne na zewnątrz” (e-mail, wiadomość do innej
 *   usługi) wykonują się tylko dla zamówień, które naprawdę są w bazie.
 *
 * ANALOGIA: kelner i kuchnia.
 *   Kelner zapisuje zamówienie, ale woła „Kuchnia, start!” dopiero po tym, jak klient potwierdzi i zapłaci (commit).
 *   Gdyby klient zrezygnował (rollback), kuchnia nic nie ugotuje. Zwykły @EventListener to kelner, który
 *   woła do kuchni od razu po spisaniu zamówienia — a potem klient wychodzi bez płacenia.
 *
 * JAK TO DZIAŁA:
 *   1. Serwis jest @Transactional i publikuje zdarzenie (publishEvent) W TRAKCIE transakcji.
 *   2. Spring zauważa słuchacza @TransactionalEventListener, a transakcja jest aktywna — więc NIE wywołuje go
 *      od razu, tylko zapisuje go w synchronizacjach transakcji (ten sam mechanizm, którego używa sama transakcja).
 *   3. Gdy transakcja się kończy, Spring wywołuje zapisane metody w odpowiedniej fazie:
 *      BEFORE_COMMIT (tuż przed zatwierdzeniem, wyjątek wycofuje transakcję), AFTER_COMMIT (domyślna, po zatwierdzeniu),
 *      AFTER_ROLLBACK (po wycofaniu), AFTER_COMPLETION (po zakończeniu, tak czy inaczej).
 *   4. Gdy transakcji NIE ma, słuchacz jest pomijany — chyba że ma fallbackExecution = true.
 *   5. Słuchacz AFTER_COMMIT działa już po zatwierdzeniu: transakcji nie da się „cofnąć”, a pisanie do bazy wymaga
 *      osobnej transakcji (REQUIRES_NEW).
 *
 * SŁÓWKA:
 *   phase = faza; before = przed; after = po; completion = zakończenie; fallback = plan awaryjny; veto = sprzeciw
 *   (tu: wyjątek, który wstrzymuje zatwierdzenie); synchronization = synchronizacja (haczyki wywoływane przez
 *   transakcję); propagation = propagacja (jak transakcja zachowuje się wobec już trwającej).
 *
 * ZOBACZ TEŻ: s10_transactions/Tx01Transactional (@Transactional, commit i rollback),
 *             s10_transactions/Tx02Propagation (REQUIRES_NEW — nowa, niezależna transakcja),
 *             s10_transactions/Tx05ProgrammaticTransactions (TransactionTemplate — używamy go w ćwiczeniach),
 *             s17_events/Evt01ApplicationEvents (zwykłe zdarzenia: @EventListener, synchroniczność),
 *             s17_events/Evt03AsyncAndOutbox (co, gdy aplikacja padnie po commicie, a przed obsługą zdarzenia).
 * </pre>
 */
public final class Evt02TransactionalEvents {

    private Evt02TransactionalEvents() {
    }

    // =================================================================================================
    // 1. FAZY: KIEDY REAGUJE KTÓRY SŁUCHACZ
    // =================================================================================================
    //
    //   @Transactional
    //   public void placeOrder(...) {
    //       repository.insertOrder(...);
    //       publisher.publishEvent(new OrderPlaced(...));    // publikacja W TRANSAKCJI
    //       log.add("serwis:koniec-metody");
    //   }
    //
    // Dziennik z testu sekcji 1 (zatwierdzenie; wszystkie słuchacze mają @Order, żeby kolejność była jawna):
    //   zwykły(transakcja aktywna=true)      ← zwykły @EventListener: od razu, w środku transakcji
    //   serwis:koniec-metody                  ← serwis kończy metodę
    //   BEFORE_COMMIT                         ← tuż przed zatwierdzeniem
    //   AFTER_COMMIT(domyślna)                ← @TransactionalEventListener bez phase = AFTER_COMMIT
    //   AFTER_COMMIT+fallbackExecution
    //   AFTER_COMPLETION                      ← po zakończeniu, bez względu na wynik
    //
    // Co tu jest ważne: zwykły słuchacz zareagował PRZED końcem metody serwisu, czyli zanim zamówienie zostało
    // zatwierdzone; słuchacze „transakcyjni” czekali na wynik. Kolejność słuchaczy tej samej fazy (i faz AFTER_*
    // względem siebie) ustawiaj @Order: bez niego jest nieokreślona — w naszych uruchomieniach AFTER_COMPLETION
    // bywał przed AFTER_ROLLBACK.
    //
    // DOBRA PRAKTYKA: skutki uboczne na zewnątrz (e-mail, wiadomość do innego systemu, odświeżenie pamięci
    //   podręcznej) wykonuj w AFTER_COMMIT. Dlaczego? Wtedy masz pewność, że dane są zatwierdzone: odbiorca,
    //   który zapyta bazę o zamówienie, je znajdzie, a wycofane zamówienie nie wygeneruje fałszywego powiadomienia.

    // =================================================================================================
    // 2. WYCOFANIE TRANSAKCJI: AFTER_COMMIT MILCZY, A ZWYKŁY SŁUCHACZ JUŻ ZAREAGOWAŁ
    // =================================================================================================
    //
    // Test sekcji 2: serwis zapisuje zamówienie, publikuje zdarzenie i rzuca IllegalStateException("Płatność odrzucona").
    // Transakcja jest wycofana (w bazie 0 zamówień), a dziennik zawiera:
    //   zwykły(transakcja aktywna=true), AFTER_ROLLBACK, AFTER_COMPLETION
    // — czyli słuchacze AFTER_COMMIT (obaj) się NIE wykonali. Za to zwykły słuchacz zareagował na zamówienie, którego już nie ma.
    //
    // PUŁAPKA: zwykły @EventListener do wysyłania e-maili/powiadomień.
    //   Dlaczego? Reaguje, zanim transakcja się zakończy. Jeśli potem nastąpi rollback, klient dostaje
    //   „dziękujemy za zamówienie” o zamówieniu, którego nie ma. Efektu ubocznego na zewnątrz (e-mail) NIE da się
    //   wycofać razem z bazą.
    //
    // AFTER_ROLLBACK służy do sprzątania po nieudanej operacji (np. zwolnienie zarezerwowanych zasobów poza bazą,
    // zapis do dziennika „zamówienie odrzucone”); AFTER_COMPLETION — do czynności potrzebnych zawsze.

    // =================================================================================================
    // 3. PUBLIKACJA POZA TRANSAKCJĄ: SŁUCHACZ ZNIKA, CHYBA ŻE fallbackExecution
    // =================================================================================================
    //
    //   @TransactionalEventListener                          // brak transakcji → pominięty
    //   @TransactionalEventListener(fallbackExecution = true) // brak transakcji → wykonaj od razu
    //
    // Test sekcji 3: publishWithoutTransaction nie ma @Transactional. Zwykły słuchacz zareagował
    // (transakcja aktywna=false), słuchacz z fallbackExecution = true także (od razu, bez czekania na commit),
    // a słuchacz AFTER_COMMIT bez fallbackExecution — wcale i bez żadnego komunikatu.
    //
    // PUŁAPKA: zdarzenie opublikowane tam, gdzie nie ma transakcji (zapomniane @Transactional, wywołanie
    //   z metody this.coś() omijające proxy — lekcja s10_transactions/Tx03ProxyPitfalls, wątek bez transakcji).
    //   Dlaczego groźne? Słuchacz po prostu się nie wykonuje i NIC o tym nie mówi — „zniknęło powiadomienie”.
    //   fallbackExecution = true daje „plan B”, ale wtedy słuchacz może zareagować na dane, które nie są
    //   w żadnej transakcji; używaj go świadomie, a główną obroną jest pilnowanie, że publikujesz w transakcji.

    // =================================================================================================
    // 4. WYJĄTKI W FAZACH: BEFORE_COMMIT WYCOFUJE, AFTER_COMMIT TYLKO ZOSTAJE W LOGU
    // =================================================================================================
    //
    // 4a. Wyjątek w BEFORE_COMMIT: dociera do wywołującego serwis i transakcja zostaje WYCOFANA (test sekcji 4a:
    //     IllegalStateException z komunikatem „Audyt odrzucił zamówienie ZAM-004”, w bazie 0 zamówień).
    //     Dlatego BEFORE_COMMIT nadaje się do sprawdzeń „zatwierdź tylko, jeśli…” (weto).
    //
    // 4b. Wyjątek w AFTER_COMMIT: NIE dociera do wywołującego. Transakcja jest już zatwierdzona, więc Spring tylko go
    //     zapisuje w logu na poziomie ERROR (test sekcji 4b czyta log):
    //       ERROR o.s.t.s.TransactionSynchronizationUtils : TransactionSynchronization.afterCompletion threw exception
    //     a zamówienie zostaje w bazie. (W Springu 6.1+ słuchacze AFTER_COMMIT są wykonywani z wywołania
    //     „po zakończeniu” transakcji, dlatego komunikat mówi o afterCompletion.)
    //
    // PUŁAPKA: oczekiwanie, że błąd słuchacza AFTER_COMMIT „wywróci” operację albo da się go złapać try/catch wokół serwisu.
    //   Dlaczego? Operacja jest już zatwierdzona i zakończona; wywołujący nie zobaczy błędu, a skutek poboczny
    //   (np. nieudany e-mail) po cichu przepada, jeśli nie zadbasz o ponowienie (outbox — lekcja Evt03AsyncAndOutbox).

    // =================================================================================================
    // 5. PUŁAPKA: ZAPIS DO BAZY W SŁUCHACZU AFTER_COMMIT
    // =================================================================================================
    //
    //   @TransactionalEventListener
    //   public void naive(NaivePointsRequested e) { repository.insertPoints(...); }   // bez nowej transakcji
    //
    // Co pokazuje test sekcji 5 (JDBC + Hikari + H2, menedżer DataSourceTransactionManager):
    //   • w AFTER_COMMIT TransactionSynchronizationManager.isActualTransactionActive() zwraca true, a połączenie ma
    //     autoCommit=false — pozostało po zakończonej transakcji i wciąż jest do niego przypisane,
    //   • wiersz z punktami ostatecznie JEST w bazie, ale w trakcie słuchacza inna sesja jeszcze go NIE widzi (nie jest
    //     zatwierdzony); zatwierdza go dopiero sprzątanie połączenia po słuchaczu (prawdopodobnie przełączenie autoCommit
    //     z powrotem na true — w JDBC zmiana trybu w trakcie transakcji ją zatwierdza). Ten zapis nie podlega ŻADNEJ
    //     transakcji, nad którą masz kontrolę:
    //     gdy słuchacz rzuci wyjątek po zapisie, wiersz i tak zostaje, a wywołujący o niczym się nie dowie.
    //   Dokumentacja Springa ostrzega, że zmiany wykonane w tej fazie bez nowej transakcji mogą w ogóle nie zostać
    //   zatwierdzone — zależy to od menedżera transakcji i zasobu. To zachowanie „przypadkiem działa u mnie” jest
    //   dokładnie tym, czego nie chcesz w kodzie, na którym ktoś polega.
    //
    // PUŁAPKA: pisanie do bazy w AFTER_COMMIT „po prostu” (jak w zwykłym serwisie).
    //   Dlaczego? Transakcja zamówienia już się zakończyła; dopisek trafia do „resztek” tego połączenia i nie ma własnej
    //   transakcji: brak atomowości i brak wycofania. Efekt zależy od technologii dostępu do danych, więc kod
    //   działający na JDBC może zachować się inaczej po zmianie na JPA.

    // =================================================================================================
    // 6. POPRAWNIE: @Transactional(REQUIRES_NEW) NA SŁUCHACZU
    // =================================================================================================
    //
    //   @TransactionalEventListener
    //   @Transactional(propagation = Propagation.REQUIRES_NEW)   // nowa, niezależna transakcja
    //   public void correct(CorrectPointsRequested e) { repository.insertPoints(...); }
    //
    // Test sekcji 6a: zapis jest zatwierdzony przez własną transakcję, a gdy słuchacz rzuci wyjątek po zapisie, TYLKO jego
    // zapis jest wycofany (0 wierszy punktów), zamówienie (zatwierdzone wcześniej) zostaje. Wyjątek nadal nie wraca do
    // wywołującego (sekcja 4b), ale został wycofany i zalogowany.
    //
    // DOBRA PRAKTYKA: słuchacz AFTER_COMMIT, który zapisuje do bazy, dostaje własną transakcję: REQUIRES_NEW
    //   (albo NOT_SUPPORTED, gdy w ogóle nie chcesz transakcji). Dlaczego? Transakcja zamówienia jest zakończona —
    //   tylko nowa transakcja daje atomowość i wycofanie TEGO zapisu.
    //
    // PUŁAPKA: goły @Transactional (REQUIRED) na metodzie @TransactionalEventListener.
    //   Dlaczego? REQUIRED dołączyłoby do zakończonej transakcji. Spring tego nie dopuszcza — aplikacja nie
    //   uruchomi się (test sekcji 6b, komunikat z prawdziwego uruchomienia; opakowany w BeanInitializationException):
    //   java.lang.IllegalStateException: @TransactionalEventListener method must not be annotated with @Transactional
    //   unless when declared as REQUIRES_NEW or NOT_SUPPORTED: void ...BadListener.on(java.lang.String)

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @TransactionalEventListener czeka na wynik transakcji; fazy: BEFORE_COMMIT, AFTER_COMMIT (domyślna),
     *     AFTER_ROLLBACK, AFTER_COMPLETION.
     *   • Zwykły @EventListener reaguje od razu, jeszcze przed commitem — nie używaj go do e-maili i powiadomień.
     *   • Bez transakcji słuchacz transakcyjny jest pomijany po cichu, chyba że fallbackExecution = true.
     *   • BEFORE_COMMIT: wyjątek wycofuje transakcję (weto). AFTER_COMMIT: wyjątek tylko w logu (ERROR), wywołujący go nie widzi.
     *   • Zapis do bazy w AFTER_COMMIT: nowa transakcja — @Transactional(propagation = REQUIRES_NEW); goły @Transactional
     *     na takim słuchaczu Spring odrzuca przy starcie.
     *   • Kolejność słuchaczy ustawiaj @Order; bez niego jest nieokreślona.
     *   • Zdarzenie po commicie może zginąć (awaria aplikacji) — zapowiedź wzorca outbox w Evt03AsyncAndOutbox.
     *
     * PYTANIA KONTROLNE:
     *   1. Co się stanie? Serwis publikuje zdarzenie w metodzie BEZ @Transactional, a słuchacz ma samo
     *      @TransactionalEventListener (domyślna faza). Czy słuchacz się wykona i czy zobaczysz błąd?
     *   2. Co się stanie? Transakcja zostaje wycofana. Czy zareaguje zwykły @EventListener, a czy słuchacz AFTER_COMMIT?
     *   3. Co się stanie? Słuchacz AFTER_COMMIT rzuca wyjątek — czy wywołujący serwis go zobaczy i czy zamówienie
     *      zostanie w bazie?
     *   4. ZNAJDŹ BŁĄD: metoda ma @TransactionalEventListener oraz @Transactional (bez parametrów) i zapisuje do bazy.
     *      Co zobaczysz i kiedy?
     *   5. ZNAJDŹ BŁĄD: słuchacz AFTER_COMMIT zapisuje do bazy bez nowej transakcji i „działa u mnie”. Dlaczego to
     *      zły pomysł?
     *   6. Do czego służy faza BEFORE_COMMIT i co robi wyjątek z niej?
     *   7. Dlaczego do wysyłania e-maila wybierasz AFTER_COMMIT, a nie zwykły @EventListener?
     *   8. Która faza nadaje się do posprzątania po nieudanej transakcji, a która wykona się zawsze?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Evt02TransactionalEventsExercisesTest (mały kontekst Springa z prawdziwą transakcją na H2)
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): słuchacz ma zapisać identyfikator zamówienia do listy confirmed dopiero PO zatwierdzeniu
     * transakcji (nie w jej trakcie). Podpowiedź: @TransactionalEventListener — domyślna faza to AFTER_COMMIT.
     */
    public static class Exercise1ConfirmedListener {
        public final List<String> confirmed = new ArrayList<>();

        @TransactionalEventListener
        public void onOrder(OrderPlaced event) {
            // TODO: dodaj event.orderId() do confirmed
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 2 (średnie): słuchacz ma zapisać identyfikator do listy cancelled TYLKO gdy transakcja została
     * wycofana. Podpowiedź: zmień fazę w adnotacji na TransactionPhase.AFTER_ROLLBACK.
     */
    public static class Exercise2CancelledListener {
        public final List<String> cancelled = new ArrayList<>();

        @TransactionalEventListener
        public void onOrder(OrderPlaced event) {
            // TODO: ustaw fazę w adnotacji i dodaj event.orderId() do cancelled
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 3 (średnie): słuchacz ma zapisać identyfikator do listy received zarówno po zatwierdzeniu
     * transakcji, jak i wtedy, gdy zdarzenie opublikowano BEZ transakcji. Podpowiedź: fallbackExecution = true.
     */
    public static class Exercise3FallbackListener {
        public final List<String> received = new ArrayList<>();

        @TransactionalEventListener
        public void onOrder(OrderPlaced event) {
            // TODO: dodaj fallbackExecution do adnotacji i zapisz event.orderId() do received
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): słuchacz ma wstrzymać zatwierdzenie transakcji (weto), gdy kwota zamówienia przekracza
     * 10000 zł: rzuć IllegalStateException("Limit przekroczony") w fazie BEFORE_COMMIT; przy mniejszej kwocie nic
     * nie rób. Podpowiedź: phase = TransactionPhase.BEFORE_COMMIT, event.totalPln().intValue().
     */
    public static class Exercise4LimitListener {
        @TransactionalEventListener
        public void onOrder(OrderPlaced event) {
            // TODO: ustaw fazę BEFORE_COMMIT i rzuć wyjątek dla kwot powyżej 10000
            throw new UnsupportedOperationException("TODO");
        }
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Nie wykona się i nie zobaczysz żadnego błędu: bez aktywnej transakcji słuchacz transakcyjny jest pomijany po
     *      cichu (chyba że fallbackExecution = true). Sprawdzone testem (sekcja 3).
     *   2. Zwykły @EventListener zareagował (jeszcze w trakcie transakcji, zanim ją wycofano), a słuchacze AFTER_COMMIT nie.
     *      Wykona się AFTER_ROLLBACK. Sprawdzone testem (sekcja 2).
     *   3. Nie zobaczy — wyjątek ląduje tylko w logu (ERROR, „TransactionSynchronization.afterCompletion threw exception”),
     *      a zamówienie zostaje w bazie, bo transakcja była już zatwierdzona. Sprawdzone testem (sekcja 4b).
     *   4. Aplikacja się nie uruchomi: BeanInitializationException, a w przyczynie IllegalStateException „@TransactionalEventListener
     *      method must not be annotated with @Transactional unless when declared as REQUIRES_NEW or NOT_SUPPORTED”. Dzieje się
     *      to przy starcie kontekstu, nie przy publikacji. Sprawdzone testem (sekcja 6b). Naprawa:
     *      @Transactional(propagation = Propagation.REQUIRES_NEW).
     *   5. Transakcja zamówienia jest już zakończona, więc zapis nie ma własnej transakcji: brak atomowości i brak wycofania
     *      (sprawdzone testem sekcji 5: wiersz zostaje mimo wyjątku po zapisie, a wywołujący o nim nie wie); dokumentacja
     *      Springa ostrzega też, że zmiany mogą w ogóle nie zostać zatwierdzone, zależnie od menedżera transakcji.
     *   6. Wykonuje się tuż przed zatwierdzeniem, w tej samej transakcji; wyjątek stamtąd wycofuje transakcję i dociera do
     *      wywołującego (weto: „zatwierdź tylko, jeśli…”). Sprawdzone testem (sekcja 4a).
     *   7. Zwykły słuchacz reaguje, zanim wiadomo, czy transakcja się uda — e-mail mógłby dotyczyć wycofanego zamówienia
     *      (efektu ubocznego na zewnątrz nie cofniesz). AFTER_COMMIT daje pewność, że dane są zatwierdzone.
     *   8. Posprzątanie po nieudanej transakcji: AFTER_ROLLBACK. Zawsze (i po commicie, i po rollbacku): AFTER_COMPLETION.
     *      Sprawdzone testami sekcji 1 i 2.
     */
    // </editor-fold>
}
