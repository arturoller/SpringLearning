package s11_testing.test04_data_jpa_test;

/**
 * <pre>
 * TEMAT: @DataJpaTest dokładniej — transakcja z wycofaniem, TestEntityManager, flush()/clear(), @Sql, podmiana bazy
 *        (rollback = wycofanie transakcji; persistence context = kontekst utrwalania, „pamięć” Hibernate'a z encjami
 *         wczytanymi w tej transakcji; flush = wypchnięcie zaległych zmian do bazy jako SQL; clear = wyczyszczenie
 *         kontekstu utrwalania)
 *
 * W SKRÓCIE:
 *   {@code @DataJpaTest} uruchamia tylko warstwę JPA: encje, repozytoria, DataSource, Flyway. Każdy test działa
 *   w transakcji, którą Spring po teście WYCOFUJE — dlatego testy nie psują sobie danych i kolejność nie ma
 *   znaczenia. Haczyk: Hibernate odkłada zapisy do końca transakcji, a skoro transakcja jest wycofywana, część SQL
 *   w ogóle nie trafia do bazy. Test może więc „przejść”, choć prawdziwy zapis by się wywrócił. Lekarstwo:
 *   {@code flush()} (wyślij SQL teraz) i {@code clear()} (zapomnij obiekty, czytaj z bazy).
 *
 * ANALOGIA: brudnopis i ołówek.
 *   Test pisze na brudnopisie (transakcja), a po teście kartka idzie do niszczarki (wycofanie) — następny test
 *   dostaje czystą. Hibernate to sekretarka, która notuje zmiany w notesie (kontekst utrwalania) i przepisuje je na
 *   brudnopis dopiero, gdy musi (flush). Jeśli kartka trafi do niszczarki, zanim sekretarka cokolwiek przepisze,
 *   nikt nie zauważy, że w notesie był błąd, którego brudnopis by nie przyjął (np. puste pole „nazwa”).
 *
 * JAK TO DZIAŁA:
 *   1. {@code @DataJpaTest} (Boot 4: {@code org.springframework.boot.data.jpa.test.autoconfigure}) włącza
 *      autokonfiguracje: DataSource, JPA/Hibernate, repozytoria Spring Data, transakcje, inicjalizację bazy (Flyway),
 *      TestEntityManager; filtruje skanowanie do encji i repozytoriów.
 *   2. Zawiera {@code @Transactional} — SpringExtension otwiera transakcję przed każdą metodą testową i wycofuje ją
 *      po niej (chyba że {@code @Commit} / {@code @Rollback(false)}).
 *   3. Zawiera {@code @AutoConfigureTestDatabase}: w Boot 4 domyślnie {@code replace = NON_TEST} — DataSource
 *      z konfiguracji zostaje PODMIENIONY na świeżą bazę wbudowaną (H2 o losowej nazwie), chyba że pochodzi z testowego
 *      źródła ({@code @ServiceConnection} kontenera, {@code @DynamicPropertySource}, adres {@code jdbc:tc:}).
 *   4. {@code showSql = true} (domyślnie) — w logu widać każde polecenie: „Hibernate: select p1_0.id,… from product
 *      p1_0 where p1_0.sku=?”.
 *   5. Profil lekcji ({@code @ActiveProfiles("test04")}) włącza Flyway z {@code db/migration/test04}: tabela + 14
 *      produktów; {@code ddl-auto=validate} — Hibernate tylko sprawdza, czy encja pasuje do tabeli.
 *
 * SŁÓWKA:
 *   rollback = wycofanie; commit = zatwierdzenie; flush = wypchnięcie zmian do bazy; clear = wyczyszczenie;
 *   persistence context = kontekst utrwalania; entity manager = zarządca encji; replace = podmienić;
 *   embedded = wbudowana; script = skrypt; statement = instrukcja SQL; migration = migracja; seed = dane startowe.
 *
 * ZOBACZ TEŻ: s08_data_jpa/Jpa02Repositories (repozytoria i pierwszy @DataJpaTest), s07_data_jdbc/Jdbc04Flyway
 *             (migracje Flyway), s07_data_jdbc/Jdbc05Repository (@JdbcTest i podmiana bazy), s10_transactions/Tx01Transactional
 *             (transakcje), s11_testing/Test06Testcontainers (prawdziwy PostgreSQL zamiast H2).
 * </pre>
 */
public final class Test04DataJpaTest {

    private Test04DataJpaTest() {
    }

    // =================================================================================================
    // 1. TRANSAKCJA TESTU I WYCOFANIE PO TEŚCIE
    // =================================================================================================
    //
    //   @DataJpaTest @ActiveProfiles("test04")
    //   class ... {
    //       @Test void test() {
    //           TestTransaction.isActive();             // true — test działa w transakcji
    //           TestTransaction.isFlaggedForRollback(); // true — zostanie wycofana
    //           repository.save(odkurzacz);             // 15 produktów... ale tylko do końca testu
    //       }
    //   }
    //
    // TestTransaction (org.springframework.test.context.transaction) pozwala zajrzeć do transakcji testu, a nawet
    // zakończyć ją wcześniej: TestTransaction.end() robi to, co Spring robi po teście — wycofuje. Test sekcji 1
    // pokazuje to wprost: po end() liczba produktów wraca z 15 do 14. Dzięki temu dane startowe z migracji są
    // WSPÓLNE, a każdy test może je dowolnie zmieniać — następny i tak zobaczy 14 produktów.
    //
    // PUŁAPKA: @Commit (albo @Rollback(false)) „żeby zobaczyć dane w bazie”. Dlaczego? Zatwierdzone zmiany zostają
    //   w bazie współdzielonej przez kontekst z pamięci podręcznej — kolejne testy zaczynają od innych danych i zaczynają
    //   zależeć od kolejności. Jeśli musisz zatwierdzać, sprzątaj po sobie (np. @Sql z executionPhase = AFTER_TEST_METHOD).

    // =================================================================================================
    // 2. TestEntityManager — DANE PRZYGOTOWANE „OBOK” TESTOWANEGO REPOZYTORIUM
    // =================================================================================================
    //
    //   Long id = entityManager.persistAndGetId(czapka, Long.class);   // Boot 4: org.springframework.boot.jpa.test.autoconfigure
    //   entityManager.flush(); entityManager.clear();
    //   repository.findBySku("ODZ-003")                                // dopiero teraz testujemy repozytorium
    //
    // Po co, skoro jest repository.save? Bo testując metodę X repozytorium, nie chcesz przygotowywać danych tą samą
    // klasą — błąd w niej mógłby się „sam znosić”. TestEntityManager ma też persistFlushFind (zapisz, wypchnij, wczytaj
    // od nowa) i find. Test sekcji 2.

    // =================================================================================================
    // 3. flush() I clear(): ŻEBY TEST NAPRAWDĘ DOTKNĄŁ BAZY
    // =================================================================================================
    //
    // Hibernate trzyma wczytane encje w kontekście utrwalania (pierwszy poziom pamięci podręcznej):
    //   • findById tej samej encji drugi raz → TEN SAM obiekt, bez SELECT (test sekcji 3: isSameAs);
    //   • zmiana pola encji → UPDATE dopiero przy flush (ręcznym, przed zapytaniem JPQL albo przy zatwierdzeniu);
    //   • zwykłe SQL (JdbcClient) w tej samej transakcji widzi bazę, nie kontekst — bez flush: STARY stan lampki (18),
    //     po flush: nowy (0). Sprawdzone testem sekcji 3.
    //
    // PUŁAPKA: test zapisu bez flush(). Kolumna name ma w bazie NOT NULL; ustawiamy name = null i... nic się nie dzieje
    //   — findById zwraca obiekt z pamięci z nazwą null, test jest zielony. Dopiero repository.flush() wysyła UPDATE
    //   i baza go odrzuca: DataIntegrityViolationException (na dnie SQLException ze SQLState 23502 — naruszenie
    //   NOT NULL). Dlaczego to groźne? W produkcji flush nastąpi przy zatwierdzeniu transakcji — i tam błąd wyjdzie.
    //   W teście zatwierdzenia nie ma (wycofanie), więc bez flush() błąd NIGDY nie wyjdzie. Sprawdzone testem sekcji 3.
    //
    // Ciekawostka sprawdzona testem: zapytanie JPQL do tej samej tabeli (np. findBySku) wywołuje flush SAMO (tryb
    // FlushMode.AUTO) — Hibernate najpierw wysyła zaległy UPDATE, żeby wynik zapytania był aktualny. Dlatego ten sam
    // błąd NOT NULL wychodzi przy findBySku, ale nie przy findById (które czyta z pamięci, bez zapytania).
    //
    // PUŁAPKA: porównywanie treści komunikatu błędu bazy. Dlaczego? H2 tłumaczy komunikaty według Locale JVM — na
    //   polskim Windowsie test zobaczył „Pole nie może być NULL…”, na angielskim byłoby „NULL not allowed…”. Stały
    //   jest typ wyjątku i kod SQLState — to sprawdzaj.
    //
    // DOBRA PRAKTYKA: testując zapis, kończ go flush() (i clear() przed odczytem kontrolnym). Dlaczego? flush zmusza
    //   bazę do sprawdzenia ograniczeń, a clear zmusza Hibernate do prawdziwego SELECT — test sprawdza mapowanie
    //   i bazę, a nie tylko obiekt w pamięci.

    // =================================================================================================
    // 4. @Sql — DANE TESTOWE ZE SKRYPTU
    // =================================================================================================
    //
    //   @Test @Sql("/db/test04/clearance-sale.sql")                           // plik z classpath
    //   @Test @Sql(statements = "UPDATE product SET stock = 0 WHERE sku = 'KSI-001'")   // instrukcja wprost
    //
    // Skrypt wykonuje się PRZED metodą testową (domyślnie), w TEJ SAMEJ transakcji testu — więc po teście znika razem
    // z nią, a repozytorium widzi dane od razu. Kiedy @Sql, a kiedy TestEntityManager? @Sql — gdy danych jest dużo albo
    // chcesz je mieć jako czytelny SQL; TestEntityManager — gdy budujesz kilka obiektów w Javie. Testy sekcji 4.

    // =================================================================================================
    // 5. PODMIANA BAZY: @AutoConfigureTestDatabase
    // =================================================================================================
    //
    //   domyślnie (Boot 4: replace = NON_TEST)       → jdbc:h2:mem:<losowa nazwa>  (adres z profilu ignorowany)
    //   @AutoConfigureTestDatabase(replace = NONE)  → jdbc:h2:mem:test04          (baza z konfiguracji)
    //   @DynamicPropertySource z spring.datasource.url → ten adres, BEZ podmiany (źródło „testowe”)
    //
    // Pozostałe wartości: ANY (podmień zawsze), AUTO_CONFIGURED (podmień tylko DataSource z autokonfiguracji).
    // Starsze tutoriale (Boot 2, wczesne Boot 3) opisują domyślne ANY; w Boot 4 domyślne jest NON_TEST (sprawdzone
    // w jarze spring-boot-jdbc-test 4.1.1) — po to, żeby baza z kontenera Testcontainers
    // (s11_testing/Test06Testcontainers) nie była po cichu zamieniana na H2.
    // Testy sekcji 5: Test04DataJpaTestReplaceTest (adres URL połączenia w każdym wariancie).
    //
    // PUŁAPKA: przepis ze starego tutoriala „test na prawdziwej bazie” bez replace = NONE. Dlaczego? Przy ANY wycinek
    //   podmieniał każdą bazę na H2 — test przechodził, ale na zupełnie innej bazie, niż myślałeś. W Boot 4 baza
    //   z @ServiceConnection zostaje, a zwykły spring.datasource.url z pliku — nadal jest podmieniany.

    // =================================================================================================
    // 6. FLYWAY W WYCINKU I DANE NIEZALEŻNE OD KOLEJNOŚCI
    // =================================================================================================
    //
    // Inicjalizacja bazy (w tym Flyway) należy do autokonfiguracji wycinka, więc migracje lekcji wykonują się na
    // świeżej bazie testowej: w tabeli "flyway_schema_history" są wersje 1 i 2 (Flyway 12 dopisuje też wiersz
    // „<< Flyway Schema History table created >>” typu TABLE — sprawdzone uruchomieniem). Schemat z migracji +
    // ddl-auto=validate = test sprawdza też, czy encja pasuje do PRAWDZIWEGO schematu produkcyjnego. Test sekcji 6.
    //
    // DOBRA PRAKTYKA: wspólne dane startowe w migracji (albo skrypcie), zmiany per test — w transakcji testu.
    //   Dlaczego? Każdy test zaczyna od tego samego, znanego stanu; nie ma testów, które „przechodzą tylko po innym”.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @DataJpaTest = JPA + repozytoria + Flyway + TestEntityManager + transakcja z wycofaniem po każdym teście.
     *   • TestTransaction.isActive() / isFlaggedForRollback() / end(); @Commit = zatwierdź (ostrożnie!).
     *   • TestEntityManager: persist, persistAndGetId, persistFlushFind, find, flush, clear.
     *   • flush() — wyślij zaległy SQL (ograniczenia bazy!); clear() — zapomnij encje, następny odczyt to SELECT.
     *   • JPQL na tej samej tabeli robi flush sam (FlushMode.AUTO); findById z pamięci — nie.
     *   • @Sql("/plik.sql") / @Sql(statements = "...") — w transakcji testu, przed metodą.
     *   • Boot 4: @AutoConfigureTestDatabase(replace = NON_TEST) domyślnie; NONE = baza z konfiguracji.
     *   • Nie porównuj komunikatów bazy (zależą od Locale) — sprawdzaj typ wyjątku i SQLState.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego zapis zrobiony w jednym teście @DataJpaTest nie jest widoczny w następnym?
     *   2. Co się stanie? W teście ustawiasz encji pole, którego kolumna ma NOT NULL, na null, odczytujesz encję
     *      przez findById i kończysz test bez flush().
     *   3. Co się stanie? To samo, ale zamiast findById wołasz findBySku (zapytanie JPQL).
     *   4. Po co clear() przed odczytem kontrolnym, skoro flush() już wysłał SQL?
     *   5. ZNAJDŹ BŁĄD: profil testu ustawia spring.datasource.url=jdbc:h2:mem:sklep, a autor dziwi się, że
     *      w @DataJpaTest adres połączenia to jdbc:h2:mem:7f3c… — gdzie jest „błąd” i jak go zmienić?
     *   6. ZNAJDŹ BŁĄD: test sprawdza assertThat(e.getMessage()).contains("NULL not allowed"). U kolegi przechodzi,
     *      u Ciebie nie. Dlaczego?
     *   7. Kiedy @Sql, a kiedy TestEntityManager do przygotowania danych?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — uzupełniasz TESTY w pliku Test04DataJpaTestExercisesTest (src/test/java, ten sam pakiet).
    // =================================================================================================
    //
    // ĆWICZENIE 1 (łatwe): przygotuj dane przez TestEntityManager: produkt KSI-004 „Spring w akcji”, KSIAZKI,
    //   119.00 zł, stan 6. Podpowiedź: entityManager.persist(new Product(...)).
    //
    // ĆWICZENIE 2 (średnie): ustaw stan lampki DOM-002 na 0 tak, żeby zwykłe SQL (JdbcClient) zobaczyło zmianę.
    //   Podpowiedź: sekcja 3 — co wysyła zaległy UPDATE?
    //
    // ĆWICZENIE 3 (średnie): dodaj do metody testowej adnotację @Sql, która ustawia stan KSI-003 na 0 — tylko na czas
    //   tego testu. Podpowiedź: @Sql(statements = "UPDATE ...").
    //
    // ĆWICZENIE 4 (trudniejsze): po zapisie odkurzacza zakończ transakcję testu tak, jak Spring robi to po teście,
    //   i pokaż, że zapis zniknął. Podpowiedź: TestTransaction.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Każdy test działa w transakcji, którą Spring po teście wycofuje (rollback). Sprawdzone testem sekcji 1
     *      (TestTransaction.end() → liczba produktów wraca do 14).
     *   2. Nic — test jest zielony. findById zwraca obiekt z kontekstu utrwalania (z nazwą null), UPDATE nigdy nie
     *      idzie do bazy, bo transakcja zostaje wycofana. Błąd ukryty: w produkcji wyszedłby przy zatwierdzeniu.
     *      Dopiero flush() daje DataIntegrityViolationException (SQLState 23502). Sprawdzone testem sekcji 3.
     *   3. Wyjątek DataIntegrityViolationException już przy findBySku: zapytanie JPQL do tej samej tabeli wywołuje
     *      automatyczny flush (FlushMode.AUTO), więc UPDATE z null trafia do bazy. Sprawdzone testem sekcji 3.
     *   4. flush() wysyła SQL, ale encje zostają w kontekście utrwalania — następny odczyt zwróciłby ten sam obiekt
     *      z pamięci, bez SELECT. clear() wymusza prawdziwy odczyt z bazy, więc sprawdzasz, co naprawdę zapisano
     *      (i czy mapowanie działa w obie strony).
     *   5. To nie błąd, tylko domyślna podmiana bazy: @DataJpaTest (Boot 4: replace = NON_TEST) zastępuje DataSource
     *      z konfiguracji świeżą bazą wbudowaną o losowej nazwie. Bazę z profilu dostaniesz przez
     *      @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE). Sprawdzone testem sekcji 5.
     *   6. Komunikat H2 zależy od Locale JVM — po polsku brzmi „Pole nie może być NULL…”. Porównuj typ wyjątku
     *      i SQLState (23502), nie treść komunikatu.
     *   7. @Sql — dużo danych, czytelny SQL, dane wspólne dla kilku testów w pliku; TestEntityManager — kilka obiektów
     *      budowanych w Javie, gdy chcesz znać ich id i operować na encjach.
     */
    // </editor-fold>
}
