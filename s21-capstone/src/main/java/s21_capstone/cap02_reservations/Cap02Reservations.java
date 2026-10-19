package s21_capstone.cap02_reservations;

/**
 * <pre>
 * TEMAT: Projekt końcowy 2 — system rezerwacji stanowisk serwisowych: sloty czasowe, brak nakładania się
 *        (reguła w serwisie + ograniczenie w bazie), współbieżność, Clock, anulowanie z terminem i zdarzenie
 *        obsłużone po zatwierdzeniu transakcji
 *        (slot = przedział czasu do zarezerwowania; overlap = nakładanie się; concurrency = współbieżność)
 *
 * W SKRÓCIE:
 *   Klient rezerwuje stanowisko serwisowe sklepu na 1–3 pełne godziny (9:00–17:00). Dwie rezerwacje tego samego
 *   stanowiska nie mogą się nakładać — pilnuje tego serwis (czytelny błąd 409) ORAZ baza (klucz główny tabeli
 *   zajętych godzin), bo samo sprawdzenie w serwisie przegrywa wyścig dwóch równoczesnych żądań. Wszystkie reguły
 *   czasu („nie w przeszłości”, „anulowanie do 24 h przed”) biorą „teraz” z beana Clock, więc testy ustawiają czas
 *   co do minuty. Po zatwierdzeniu rezerwacji słuchacz zdarzenia zapisuje potwierdzenie — tylko wtedy, gdy rezerwacja
 *   NAPRAWDĘ trafiła do bazy.
 *
 * ANALOGIA: zeszyt zapisów u fryzjera.
 *   Recepcjonistka patrzy w zeszyt, czy godzina wolna (reguła w serwisie). Gdy dwie osoby dzwonią naraz, obie słyszą
 *   „wolne”, ale wpisać się może tylko jedna — w zeszycie jest JEDNA kratka na godzinę (klucz główny w bazie). Druga
 *   osoba słyszy „przepraszam, ktoś był szybszy” (409). SMS z potwierdzeniem wysyła się dopiero po wpisaniu do zeszytu,
 *   a nie w trakcie rozmowy (AFTER_COMMIT). Odwołać wizytę można najpóźniej dzień wcześniej — według zegara salonu.
 *
 * JAK TO DZIAŁA:
 *   POST /api/reservations → ReservationController (@Valid) → ReservationService.reserve [transakcja]
 *     → DeskRepository.findByCode (404) → BookingPolicy.checkNewReservation (Clock; 422)
 *     → ReservationRepository.existsOverlapping (409 SLOT_TAKEN) → new Reservation(...) = wiersz reservations
 *       + po wierszu occupied_slots na każdą godzinę → saveAndFlush (naruszenie PK_OCCUPIED_SLOTS = ten sam 409)
 *     → publishEvent(ReservationConfirmed) → [commit] → ConfirmationNotifier.onConfirmed (AFTER_COMMIT,
 *       REQUIRES_NEW) → wiersz notifications → 201 + Location.
 *
 * SŁÓWKA:
 *   reservation = rezerwacja; desk = stanowisko; slot = przedział (tu: jedna godzina); availability = dostępność;
 *   overlap = nakładanie się; half-open interval = przedział półotwarty [od, do); race condition = wyścig;
 *   optimistic locking = blokowanie optymistyczne (@Version); event = zdarzenie; listener = słuchacz;
 *   commit = zatwierdzenie transakcji; rollback = wycofanie; waitlist = lista oczekujących; reminder = przypomnienie;
 *   unprocessable content = treść niemożliwa do przetworzenia (422); element collection = kolekcja wartości.
 *
 * ZOBACZ TEŻ: s21_capstone/Cap01ShopApi (architektura, ProblemDetail, testy na poziomach), s10_transactions/Tx02Propagation
 *             (REQUIRES_NEW), s10_transactions/Tx04ReadOnlyIsolation (@Version, utracona aktualizacja),
 *             s17_events/Evt01ApplicationEvents i s17_events/Evt02TransactionalEvents (zdarzenia i AFTER_COMMIT),
 *             s16_cache_scheduling_async/Sched03Scheduled (@Scheduled — ćwiczenie 2), s11_testing/Test02MockitoBeans
 *             (@MockitoSpyBean), t17_datetime/DateTime04ZonesInstant (Clock, strefy), t21_concurrency/Concurrency02RaceConditions.
 * </pre>
 */
public final class Cap02Reservations {

    private Cap02Reservations() {
    }

    // =================================================================================================
    // 1. SPECYFIKACJA — wymagania, przypadki użycia, punkty końcowe, model danych
    //    (testy: Cap02ReservationsTest.reservationLifecycle, Cap02ReservationsWebSliceTest #1)
    // =================================================================================================
    //
    // Wymagania:
    //   W1. Stanowiska SERWIS-1 (laptopy i smartfony) i SERWIS-2 (ekspresy do kawy), czynne 9:00–17:00.
    //   W2. Rezerwacja: stanowisko, początek o PEŁNEJ godzinie, 1–3 godziny, email klienta. Nie w przeszłości.
    //   W3. Rezerwacje jednego stanowiska nie mogą się nakładać — także przy dwóch żądaniach w tej samej chwili.
    //   W4. Anulowanie najpóźniej 24 h przed początkiem; anulowana rezerwacja zostaje w historii, a godziny się zwalniają.
    //   W5. Po potwierdzeniu klient dostaje wiadomość — ale nigdy dla rezerwacji, która się nie zapisała.
    //   W6. Plan dnia stanowiska: każda godzina WOLNY / ZAJETY / MINIONY — bez danych innych klientów.
    //
    // Przypadek użycia „Rezerwacja”: 1. Klient wybiera stanowisko, dzień i godziny (plan dnia).  2. Wysyła rezerwację.
    //   3. System sprawdza format (400), stanowisko (404), reguły czasu (422), wolne godziny (409).  4. Zapisuje rezerwację
    //   i zajęte godziny.  5. Po zatwierdzeniu wysyła potwierdzenie.  6. Odpowiedź 201 + Location + „anulować można do”.
    //
    // Punkty końcowe:
    //   GET  /api/desks                                      200 lista stanowisk
    //   GET  /api/desks/{code}/availability?date=RRRR-MM-DD  200 plan dnia | 400 zła data | 404
    //   POST /api/reservations                               201 | 400 | 404 | 409 SLOT_TAKEN | 422 NOT_FULL_HOUR,
    //                                                              OUTSIDE_OPENING_HOURS, IN_THE_PAST
    //   GET  /api/reservations/{id}                          200 | 404
    //   POST /api/reservations/{id}/cancel                   200 ANULOWANA | 404 | 409 TOO_LATE_TO_CANCEL, ALREADY_CANCELLED
    //   GET  /api/notifications                              200 „wysłane” wiadomości (podgląd do nauki)
    //
    // Model danych (V1–V3): desks 1 ── * reservations 1 ── * occupied_slots (PK: desk_id + slot_start);
    //   notifications (reservation_id, recipient, message); waitlist_entries (ćwiczenie 3).
    //
    // DOBRA PRAKTYKA: anulowanie jako POST /api/reservations/{id}/cancel, a nie DELETE. Dlaczego? DELETE obiecuje,
    //   że zasobu już nie będzie (kolejny GET → 404), a anulowana rezerwacja ISTNIEJE dalej — zmienia tylko stan
    //   (status ANULOWANA, data anulowania). Akcja na pod-zasobie mówi prawdę o tym, co się dzieje.

    // =================================================================================================
    // 2. ARCHITEKTURA — pakiety i kierunek zależności
    // =================================================================================================
    //
    //   cap02_reservations/   Cap02Application, Cap02Config (Clock + Security permitAll), Cap02Exercises
    //   ├── common/           BookingException (sealed): NotFound 404, Conflict 409, RuleViolation 422; BookingProblemAdvice
    //   ├── desk/             Desk, DeskRepository
    //   ├── reservation/      Reservation, OccupiedSlot, ReservationStatus, BookingPolicy, ReservationRepository,
    //   │                     ReservationService, ReservationController, ReservationDtos, ReservationConfirmed (zdarzenie)
    //   ├── notification/     Notification, NotificationRepository, ConfirmationNotifier (słuchacz), NotificationController
    //   └── waitlist/         WaitlistEntry, WaitlistRepository (pod ćwiczenie 3)
    //
    // notification zależy od reservation (zna zdarzenie ReservationConfirmed), ale reservation NIE zna notification.
    // Serwis rezerwacji tylko ogłasza „potwierdzono” — nie wie, czy słucha go email, SMS, audyt czy nikt. Dorzucenie
    // nowego słuchacza nie zmienia ani linijki w ReservationService (s17_events/Evt01ApplicationEvents).
    //
    // 422 a 409 (oba to błędy klienta „z sensem”, nie formatu):
    //   422 RuleViolation — żądanie NIGDY się nie uda (termin w przeszłości, 18:00, 10:30) — klient musi zmienić dane;
    //   409 Conflict      — żądanie kłóci się z OBECNYM stanem (ktoś ma tę godzinę) — może udać się później.
    //
    // PUŁAPKA: własne pole ProblemDetail o nazwie pola standardowego (status, title, type, detail, instance). Dlaczego?
    //   Pierwsza wersja ALREADY_CANCELLED dodawała setProperty("status", "ANULOWANA") — Jackson wypisał DWA klucze:
    //   "status":409,...,"status":"ANULOWANA" (złapane przy sprawdzaniu pliku .http). JSON.parse w przeglądarce
    //   bierze ostatnią wartość, więc klient „gubi” kod HTTP z ciała. Teraz pole nazywa się reservationStatus.

    // =================================================================================================
    // 3. CZAS — Clock, godziny pracy, anulowanie z terminem (testy: Cap02ReservationsUnitTest, Cap02ReservationsTest #3)
    // =================================================================================================
    //
    // Każde „teraz” pochodzi z beana Clock (Cap02Config): BookingPolicy.now(), anulowanie (cancel(policy.now())),
    // data utworzenia, powiadomienia. Profil cap02 ustawia cap02.clock.fixed=2026-10-19T10:00 — aplikacja i testy
    // „żyją” w tej chwili, więc dane startowe i plik .http działają tak samo dziś i za rok. Testy jednostkowe tworzą
    // BookingPolicy z Clock.fixed(...) i sprawdzają granice: 15:00 + 2 h kończy się o 17:00 = OK; 16:00 + 2 h = 422;
    // 10:00, gdy „teraz” jest 10:00 = 422 IN_THE_PAST (teraźniejszość to już nie przyszłość).
    //
    // Anulowanie: do startsAt − 24 h WŁĄCZNIE (now.isAfter(granica) → 409). Rezerwacja na jutro 10:00 przy „teraz”
    // 10:00 — jeszcze wolno; minutę później — nie. Dzisiejsza rezerwacja Zofii (15:00) → 409 TOO_LATE_TO_CANCEL
    // z polem "cancellableUntil":"2026-10-18T15:00".
    //
    // Czas lokalny: LocalDateTime w strefie sklepu (Europe/Warsaw). Godziny pracy to czas POLSKI — Clock.system(ZONE)
    // daje go także na serwerze ustawionym na UTC. (Przy klientach z wielu stref zapisalibyśmy Instant + strefę.)
    //
    // PUŁAPKA: @Future na polu DTO (s05_validation/Val01BeanValidation). Dlaczego? Bean Validation porównuje z zegarem
    //   SYSTEMOWYM, a nie z naszym beanem Clock — Boot nie łączy ich sam. Test futureAnnotationIgnoresOurClock: dla
    //   aplikacji 2026-10-19 12:00 to przyszłość (jest 10:00), a @Future zgłasza błąd, bo komputer ma już późniejszą datę.
    //   W testach i w aplikacji z zatrzymanym zegarem dostaniesz sprzeczne wyniki. Dlatego regułę „nie w przeszłości”
    //   sprawdza BookingPolicy — tym samym zegarem co reszta.
    //
    // PUŁAPKA: zegar demonstracyjny (Clock.fixed) w konfiguracji produkcyjnej. Dlaczego groźne? Wszystkie rezerwacje
    //   miałyby tę samą datę utworzenia, a „przeszłość” nigdy by nie nadeszła. Dlatego to osobna właściwość profilu
    //   demonstracyjnego, opisana jako „NIE w produkcji”, a nie stała w kodzie.

    // =================================================================================================
    // 4. BRAK NAKŁADANIA SIĘ I WSPÓŁBIEŻNOŚĆ (testy: Cap02ReservationsRepositoryTest, Cap02ReservationsTest #4)
    // =================================================================================================
    //
    // Warunek nakładania przedziałów półotwartych [s1, e1) i [s2, e2) (PYTANIE REKRUTACYJNE):  s1 < e2  AND  s2 < e1.
    // Styk (10–12 i 12–13) to NIE konflikt. W JPQL: r.startsAt < :end and :start < r.endsAt (existsOverlapping).
    //
    // Sama reguła w serwisie nie wystarczy — to „sprawdź, potem zrób” (check-then-act). Dwa żądania naraz:
    //     T1: existsOverlapping → false        T2: existsOverlapping → false
    //     T1: INSERT ... commit                T2: INSERT ... commit      → dwie rezerwacje na tę samą godzinę!
    // Druga linia obrony — baza. Przedziałów nie da się zapisać jako UNIQUE (PostgreSQL ma EXCLUDE USING gist, H2 nie),
    // więc dzielimy rezerwację na GODZINY: tabela occupied_slots, klucz główny (desk_id, slot_start). Rezerwacja
    // 10–12 to dwa wiersze: 10:00 i 11:00. Dwie transakcje nie zapiszą tej samej godziny — druga dostaje od H2:
    //     Unique index or primary key violation: "PUBLIC.PK_OCCUPIED_SLOTS ... VALUES (..., TIMESTAMP '2026-10-27 11:00:00')"
    // (na polskim systemie H2 pisze w logu: „Naruszenie ograniczenia Klucza Głównego lub Indeksu Unikalnego”). Spring
    // tłumaczy to na DataIntegrityViolationException, a serwis — przy saveAndFlush — na ten sam 409 SLOT_TAKEN.
    // Anulowanie czyści zbiór occupiedSlots → Hibernate: delete from occupied_slots where reservation_id=? → godziny wolne.
    //
    // Test DETERMINISTYCZNY (bez wątków i bez sleep): szpieg (@MockitoSpyBean) na ReservationRepository. Gdy NASZA
    // rezerwacja (10–12) sprawdzi „wolne”, w środku tego wywołania drugi klient rezerwuje 11–12 w transakcji
    // REQUIRES_NEW i ją zatwierdza. Nasza transakcja wraca, robi INSERT 11:00 i odbija się od klucza → 409, w bazie jest
    // tylko rezerwacja drugiego klienta i tylko on dostał potwierdzenie. Sprawdzone na H2 2.4: odczyt (count) nie
    // zakłada blokad, więc druga transakcja nie czeka na pierwszą — kolejność zdarzeń jest zawsze ta sama.
    //
    // PUŁAPKA: (PYTANIE REKRUTACYJNE: „czy @Version rozwiąże podwójną rezerwację?”) — NIE w tym modelu. Dlaczego?
    //   @Version chroni ISTNIEJĄCY wiersz przed dwiema równoczesnymi ZMIANAMI (UPDATE ... where version = ?,
    //   s10_transactions/Tx04ReadOnlyIsolation). Dwie NOWE rezerwacje to dwa różne INSERT-y — nie ma wspólnego wiersza,
    //   którego wersję można porównać. @Version zadziałałby, gdyby sloty były gotowymi wierszami (slot 11:00 → UPDATE
    //   „zajęty przez rezerwację X”); przy rezerwacjach wstawianych od zera potrzebne jest ograniczenie unikalności.
    //
    // PUŁAPKA: szpieg na repozytorium Spring Data i invocation.callRealMethod(). Dlaczego nie działa? Repozytorium to
    //   proxy INTERFEJSU — metoda jest abstrakcyjna i Mockito rzuca „Cannot call abstract real method on java object!”
    //   (sprawdzone). Niepodmienione metody szpieg przekazuje do prawdziwego repozytorium, więc test liczy „prawdziwą”
    //   odpowiedź przez findOverlapping (ten sam warunek).

    // =================================================================================================
    // 5. ZDARZENIE „POTWIERDZONO” I SŁUCHACZ PO COMMICIE (test: Cap02ReservationsEventsTest)
    // =================================================================================================
    //
    // ReservationService publikuje ReservationConfirmed (rekord — nie encję) w transakcji. ConfirmationNotifier:
    //     @TransactionalEventListener(phase = AFTER_COMMIT)  +  @Transactional(propagation = REQUIRES_NEW)
    // Co pokazują testy (wszystko na prawdziwej bazie, z prawdziwymi commitami):
    //   • po udanej rezerwacji jest wiadomość „Potwierdzamy rezerwację stanowiska SERWIS-1: 2026-11-09 10:00–11:00”;
    //   • rezerwacja w transakcji wycofanej (setRollbackOnly): w bazie brak rezerwacji i brak wiadomości — ale zwykły
    //     @EventListener dostał zdarzenie (wysłałby maila o rezerwacji, której nie ma);
    //   • słuchacz AFTER_COMMIT BEZ REQUIRES_NEW, który też robi notifications.save(...): wiersza NIE MA, a błędu też nie.
    //     Po commicie zasoby transakcji są jeszcze przypięte do wątku, więc save() dołącza do transakcji, która już się
    //     skończyła — i nikt jej drugi raz nie zatwierdzi (tak jest tu, na JPA; na czystym JDBC wiersz bywa zapisany
    //     „przypadkiem” poza kontrolą transakcji — porównaj s17_events/Evt02TransactionalEvents, sekcja 5);
    //   • zdarzenie opublikowane BEZ aktywnej transakcji: @TransactionalEventListener w ogóle nie rusza (chyba że ma
    //     fallbackExecution = true), zwykły @EventListener — tak.
    //
    // PUŁAPKA: @Transactional (domyślne REQUIRED) na słuchaczu @TransactionalEventListener. Co się stanie? Spring 6.1+
    //   nie wystartuje aplikacji (sprawdzone): „@TransactionalEventListener method must not be annotated with
    //   @Transactional unless when declared as REQUIRES_NEW or NOT_SUPPORTED”. Dobrze — inaczej zapis by przepadał.
    //
    // DOBRA PRAKTYKA: zdarzenie niesie DANE (id, email, kod stanowiska, godziny), a nie encję. Dlaczego? Słuchacz po
    //   commicie działa poza transakcją rezerwacji — leniwe relacje encji byłyby już niedostępne, a słuchacz
    //   asynchroniczny (s17_events/Evt03AsyncAndOutbox) działałby nawet w innym wątku.
    //
    // DOBRA PRAKTYKA: gdy potwierdzenie MUSI dojść (płatność, umowa), zapis „do wysłania” robi się W TEJ SAMEJ transakcji
    //   co rezerwacja (wzorzec outbox), a wysyłkę — osobno. AFTER_COMMIT nie przeżyje awarii serwera między commitem
    //   a wysłaniem; outbox — tak (s17_events/Evt03AsyncAndOutbox).

    // =================================================================================================
    // 6. TESTY NA KAŻDYM POZIOMIE
    // =================================================================================================
    //
    //   Cap02ReservationsUnitTest        JUnit + Clock.fixed     BookingPolicy (8 przypadków granicznych), anulowanie
    //   Cap02ReservationsWebSliceTest    @WebMvcTest + atrapa     400 / 422 / 409 / 201 + Location, format dat ISO
    //   Cap02ReservationsRepositoryTest  @DataJpaTest             warunek nakładania, PK_OCCUPIED_SLOTS, zwalnianie godzin
    //   Cap02ReservationsTest            @SpringBootTest+MockMvc  scenariusz, plan dnia, Clock, @Future, współbieżność
    //   Cap02ReservationsEventsTest      @SpringBootTest          AFTER_COMMIT, rollback, REQUIRES_NEW, brak transakcji
    // Testy pełnego kontekstu NIE są transakcyjne (inaczej AFTER_COMMIT nigdy by nie ruszył) — każdy rezerwuje inny dzień.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Przedziały półotwarte [od, do); nakładanie: s1 < e2 AND s2 < e1; styk to nie konflikt.
     *   • Reguła w serwisie (czytelny 409) + ograniczenie w bazie (PK occupied_slots) — saveAndFlush, by złapać naruszenie.
     *   • @Version chroni istniejący wiersz przed równoczesną zmianą; na dwa INSERT-y potrzeba UNIQUE/PK.
     *   • Test współbieżności bez wątków: @MockitoSpyBean + konkurent w REQUIRES_NEW w środku wywołania.
     *   • Każde „teraz” z beana Clock; @Future używa zegara systemowego — nie mieszaj; zegar demo tylko w profilu demo.
     *   • 422 = reguła, która nigdy się nie spełni; 409 = konflikt ze stanem; 400 = format.
     *   • @TransactionalEventListener(AFTER_COMMIT) + @Transactional(REQUIRES_NEW) na zapisach w słuchaczu.
     *   • Bez transakcji słuchacz transakcyjny nie rusza (fallbackExecution); zdarzenie = rekord z danymi, nie encja.
     *   • Anulowanie = akcja POST /cancel (zasób zostaje), a nie DELETE.
     *
     * PYTANIA KONTROLNE:
     *   1. Napisz warunek nakładania się rezerwacji [s1, e1) i [s2, e2). Czy 10–12 i 12–13 się nakładają?
     *   2. Dlaczego samo existsOverlapping w serwisie nie chroni przed podwójną rezerwacją?
     *   3. Co się stanie? Dwie transakcje równocześnie rezerwują SERWIS-2 na 27.10: T1 10–12, T2 11–12; T2 zatwierdza
     *      między sprawdzeniem a zapisem T1. Co dostanie klient T1 i co będzie w bazie?
     *   4. ZNAJDŹ BŁĄD: encja Reservation dostaje pole @Version, „żeby dwie osoby nie zarezerwowały tej samej godziny”.
     *   5. Co się stanie? Do NewReservationRequest dopiszesz @Future na startsAt, a aplikacja działa z zegarem
     *      zatrzymanym na 2026-10-19 10:00. Klient wyśle startsAt = 2026-10-19T12:00, a zegar komputera pokazuje
     *      już późniejszą chwilę (np. 2026-10-19 15:00).
     *   6. ZNAJDŹ BŁĄD: słuchacz @TransactionalEventListener public void on(ReservationConfirmed e)
     *      { notifications.save(...); } — w logu brak błędów, a powiadomień w tabeli nie ma.
     *   7. Dlaczego anulowanie to POST /api/reservations/{id}/cancel, a nie DELETE /api/reservations/{id}?
     *   8. Czym różni się 409 od 422 w tym API? Podaj po jednym przykładzie.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA (rozszerzenia systemu) — w klasie Cap02Exercises, testy: Cap02ReservationsExercisesTest (▶ sam),
    // rozwiązania: s21_capstone.solutions.cap02_reservations.Cap02ReservationsSolutions.
    //   ĆWICZENIE 1 (łatwe): limit — najwyżej 2 aktywne, przyszłe rezerwacje na klienta (409 RESERVATION_LIMIT).
    //   ĆWICZENIE 2 (średnie): przypomnienia na najbliższe 24 h — metoda dla @Scheduled, idempotentna (reminder_sent_at).
    //   ĆWICZENIE 3 (trudniejsze): lista oczekujących — zapis tylko na zajętą godzinę, anulowanie awansuje pierwszego
    //   z kolejki w tej samej transakcji (z potwierdzeniem po commicie).
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. s1 < e2 AND s2 < e1. 10–12 i 12–13: 10 < 13 tak, 12 < 12 NIE → nie nakładają się (koniec nie należy do
     *      przedziału). Test overlapQuery: „zaczyna się, gdy tamta się kończy” → false.
     *   2. Bo to dwa kroki (check-then-act): dwa żądania mogą jednocześnie usłyszeć „wolne” i oba zapisać rezerwację.
     *      Sprawdzenie daje czytelny błąd w typowym przypadku; gwarancję daje dopiero ograniczenie w bazie.
     *   3. T1 sprawdził „wolne”, ale jego INSERT godziny 11:00 narusza PK_OCCUPIED_SLOTS (T2 już ją zatwierdził) →
     *      DataIntegrityViolationException przy saveAndFlush → 409 SLOT_TAKEN; transakcja T1 wycofana w całości (także
     *      godzina 10:00). W bazie: tylko rezerwacja T2 i tylko jej potwierdzenie (test concurrentReservationOfTheSameSlot).
     *   4. @Version porównuje wersję ISTNIEJĄCEGO wiersza przy UPDATE. Dwie nowe rezerwacje to dwa INSERT-y do różnych
     *      wierszy — nie ma czego porównać, obie się zapiszą. Potrzebne ograniczenie unikalności (u nas PK occupied_slots)
     *      albo model z gotowymi wierszami slotów, które się aktualizuje (wtedy @Version zadziała).
     *   5. 400 z "errors":{"startsAt":"must be a future date"} (sprawdzone uruchomieniem), choć według zegara
     *      aplikacji (10:00) 12:00 to przyszłość. @Future używa zegara systemowego, a nie beana Clock
     *      (test futureAnnotationIgnoresOurClock). Reguły czasu sprawdzaj jednym zegarem — w BookingPolicy.
     *   6. Słuchacz AFTER_COMMIT działa, gdy transakcja jest już zatwierdzona, ale jej zasoby są jeszcze przypięte do
     *      wątku: save() dołącza do niej i nikt już tego nie zatwierdzi — zapis po cichu przepada (test
     *      saveWithoutRequiresNewIsLost). Poprawka: @Transactional(propagation = REQUIRES_NEW) na słuchaczu.
     *   7. Anulowana rezerwacja nie znika: zostaje w historii ze statusem ANULOWANA i datą. DELETE sugerowałby, że
     *      kolejny GET zwróci 404. Akcja POST .../cancel to zmiana stanu z regułami (termin 24 h, 409 przy drugim razie).
     *   8. 409 — konflikt z obecnym stanem, później może się udać: SLOT_TAKEN (ktoś ma tę godzinę), TOO_LATE_TO_CANCEL.
     *      422 — dane łamią regułę niezależnie od stanu, ponowienie nic nie da: IN_THE_PAST, OUTSIDE_OPENING_HOURS,
     *      NOT_FULL_HOUR.
     */
    // </editor-fold>
}
