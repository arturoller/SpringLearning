package s11_testing.test04_data_jpa_test;

/**
 * <pre>
 * TEMAT: @DataJpaTest — testy repozytoriów i zapytań na prawdziwej bazie, TestEntityManager, transakcja testu
 *        i jej pułapki, @Sql
 *        (repository = repozytorium; persistence context = kontekst utrwalania, pamięć podręczna encji sesji;
 *         rollback = wycofanie transakcji; commit = zatwierdzenie transakcji; flush = wypchnięcie zmian do bazy;
 *         lazy = leniwie; projection = projekcja, rzut)
 *
 * W SKRÓCIE:
 *   {@code @DataJpaTest} buduje wycinek warstwy danych: encje, repozytoria Spring Data, EntityManager, bazę (domyślnie
 *   wbudowaną H2 z losową nazwą) i Flyway. Nie ma w nim kontrolerów ani serwisów. Każdy test działa w transakcji,
 *   którą Spring po teście WYCOFUJE — dzięki temu testy nie brudzą sobie nawzajem bazy. To wygodne, ale ma cenę: test
 *   wykonuje się w warunkach, w jakich kod aplikacji zwykle NIE działa (zawsze w jednej transakcji, bez commitu).
 *   Dlatego część błędów (leniwe ładowanie, naruszenia ograniczeń przy zapisie) w takim teście się nie pokazuje.
 *   Lekcja uczy używać wycinka świadomie: kiedy mu ufać, a kiedy wyłączyć transakcję testu.
 *
 * ANALOGIA: próba generalna w teatrze z kurtyną, która po spektaklu cofa scenę do stanu wyjściowego.
 *   Aktorzy (testy) mogą przewracać meble, bo po próbie wszystko wraca na miejsce (rollback). Ale w teatrze próbnym
 *   widzowie nigdy nie wejdą na scenę, więc nie sprawdzisz, czy kabel nie leży im pod nogami (LazyInitializationException)
 *   ani czy bilet jest ważny przy kasie (ograniczenie bazy sprawdzane przy zatwierdzeniu). Czasem trzeba zagrać
 *   spektakl "na żywo" (@Commit, NOT_SUPPORTED).
 *
 * JAK TO DZIAŁA:
 *   1. {@code @DataJpaTest} włącza autokonfiguracje JPA (EntityManager, Hibernate, repozytoria Spring Data),
 *      DataSource, transakcje, Flyway i {@code TestEntityManager}. Pozostałych beanów (@Service, @Controller) nie tworzy.
 *   2. Źródło danych jest podmieniane na wbudowaną bazę z LOSOWĄ nazwą (AutoConfigureTestDatabase), a Flyway lekcji
 *      (profil test04: {@code db/migration/test04}) wykonuje się na tej nowej bazie — schemat i dane startowe
 *      pochodzą więc z migracji, tak jak w aplikacji.
 *   3. Przed każdym testem Spring otwiera transakcję i oznacza ją do wycofania; po teście robi rollback.
 *      Repozytoria i serwisy dołączają do tej JEDNEJ transakcji.
 *   4. Zapisy trafiają do kontekstu utrwalania, a do bazy lecą przy flush (przy zapytaniu, commicie albo ręcznie).
 *      Przed asercją na danych z bazy robisz więc {@code flush()} i {@code clear()}.
 *   5. Hibernate z Flyway: Boot nie ustawia wtedy {@code ddl-auto} (domyślnie "none"), dlatego w profilu lekcji jest
 *      {@code ddl-auto=validate} — Hibernate sprawdza, czy encje pasują do tabel z migracji.
 *
 * SŁÓWKA:
 *   entity = encja; embedded database = baza wbudowana; entity manager = menedżer encji; detach = odłączyć encję od
 *   kontekstu; refresh = odświeżyć z bazy; constraint = ograniczenie (np. UNIQUE); dirty checking = wykrywanie zmian
 *   encji przy flush; propagation = propagacja (sposób dołączania do transakcji); script = skrypt.
 *
 * ZOBACZ TEŻ: s08_data_jpa/Jpa01Entities (kontekst utrwalania, flush, clear),
 *             s08_data_jpa/Jpa02Repositories (@DataJpaTest po raz pierwszy),
 *             s08_data_jpa/Jpa04CustomQueries (@Query i projekcje),
 *             s08_data_jpa/Jpa06FlywayWithJpa (Flyway + ddl-auto=validate),
 *             s09_jpa_relations/Rel04LazyLoading (LazyInitializationException),
 *             s10_transactions/Tx01Transactional (transakcje), s10_transactions/Tx02Propagation (propagacja),
 *             s11_testing/Test01TestSlices (wycinki), s11_testing/Test06Testcontainers (prawdziwy PostgreSQL).
 * </pre>
 */
public final class Test04DataJpaTest {

    private Test04DataJpaTest() {
    }

    // =================================================================================================
    // 1. CO ROBI @DataJpaTest (Test04SliceTest)
    // =================================================================================================
    //
    //   @DataJpaTest(showSql = false)        // pakiet: org.springframework.boot.data.jpa.test.autoconfigure
    //   @ActiveProfiles("test04")            // profil lekcji: Flyway, ddl-auto=validate
    //   class Test04SliceTest { @Autowired ProductRepository repository; ... }
    //
    // Test04SliceTest sprawdza, że w wycinku:
    //   • są repozytorium i encje Product i Category (metamodel Hibernate zawiera dokładnie te dwie), a serwisu
    //     CategoryService NIE ma (ObjectProvider.getIfAvailable() == null),
    //   • Flyway wykonał migracje V1 i V2 — dane (5 produktów, 2 kategorie) pochodzą z migracji, nie z kodu testu,
    //   • baza to wbudowana H2 z losową nazwą (adres nie zawiera "test04", choć profil go ustawia),
    //   • po każdym teście następuje rollback: dwa identyczne testy wstawiają produkt i oba widzą 6 (5 + własny)
    //     — przeszłyby tylko wtedy, gdy poprzedni test został wycofany, w dowolnej kolejności.
    //
    // Dlaczego {@code showSql = false}? Domyślnie wycinek drukuje każde zapytanie SQL na konsolę; w kursie wolimy
    // czytać wynik testów (SQL oglądamy tam, gdzie jest tematem lekcji — s08_data_jpa).
    //
    // PUŁAPKA: pomijanie Flyway i oparcie się na {@code ddl-auto=create-drop}. Dlaczego? Test działa na schemacie,
    //   który Hibernate wygenerował z encji, a w produkcji schemat tworzą migracje — błąd w migracji (zła kolumna,
    //   brak indeksu, brak ograniczenia UNIQUE) nigdy nie wyjdzie w teście. Dlatego uruchamiamy migracje i
    //   dajemy {@code validate}.
    //
    // PUŁAPKA: oczekiwanie, że wycinek sam ma serwisy. Dlaczego? Skanuje tylko encje i repozytoria; serwis dokładasz
    //   {@code @Import(CategoryService.class)} (Test04TransactionTest) — razem z jego zależnościami, jeśli ich nie ma.

    // =================================================================================================
    // 2. TestEntityManager (Test04EntityManagerTest)
    // =================================================================================================
    //
    // TestEntityManager (pakiet org.springframework.boot.jpa.test.autoconfigure) to cienka nakładka na EntityManager
    // dla testów. Najczęściej:
    //   tem.persistAndFlush(encja)   — zapisz i od razu wypchnij do bazy (INSERT); zwraca zapisaną encję,
    //   tem.find(Klasa.class, id)    — pobierz z kontekstu/bazy,
    //   tem.flush()                  — wypchnij oczekujące zmiany do bazy (UPDATE/INSERT),
    //   tem.clear()                  — wyrzuć wszystko z kontekstu (encje stają się odłączone),
    //   tem.refresh(encja)           — wczytaj stan encji ponownie z bazy, nadpisując zmiany w pamięci,
    //   tem.detach(encja)            — odłącz jedną encję.
    // Zwykle przygotowujesz dane przez tem (albo migracje/@Sql), a testujesz repozytorium.
    //
    // PUŁAPKA: asercja z pamięci kontekstu zamiast z bazy. Dlaczego? Hibernate trzyma wczytane encje w kontekście
    //   utrwalania. Test assertionFromMemoryTrap zmienia cenę Myszy wprost w bazie (JdbcClient), a
    //   {@code repository.findById(2L)} i tak zwraca STARĄ cenę (59,00) — z pamięci, bez pytania bazy. Dopiero po
    //   {@code tem.clear()} odczyt idzie do bazy i widać 1,00. Test, który "zapisuje i odczytuje" bez clear(), często
    //   sprawdza tylko pamięć — a nie to, co naprawdę zapisała baza.
    //
    // DOBRA PRAKTYKA: przed asercją na stanie bazy: {@code tem.flush(); tem.clear();}. Dlaczego? flush wymusza SQL
    //   (ujawnia błędy bazy, np. naruszone ograniczenia), a clear zmusza następny odczyt do pójścia do bazy.
    //   Test flushAndClearBeforeAssertion pokazuje: zmiana encji bez save jest w bazie dopiero po flush.

    // =================================================================================================
    // 3. TRANSAKCJA TESTU: ZALETY I PUŁAPKI (Test04TransactionTest, Test04NoTransactionTest, Test04CommitTest)
    // =================================================================================================
    //
    // Zalety wycofywanej transakcji: czystość (każdy test zaczyna od danych z migracji), szybkość (nie trzeba
    // sprzątać ani odtwarzać bazy), niezależność testów od kolejności.
    //
    // PUŁAPKA: LazyInitializationException jest UKRYTY (pierwsza z trzech pułapek transakcji testu). CategoryService.productNamesOf czyta leniwą kolekcję
    //   Category.products po zakończeniu transakcji repozytorium — w aplikacji to LazyInitializationException
    //   (s09_jpa_relations/Rel04LazyLoading). Dlaczego test tego nie wykrywa? Test działa w jednej transakcji, więc
    //   sesja Hibernate jest otwarta przez cały czas, a kolekcja ładuje się bez problemu (Test04TransactionTest).
    //   Zielony test nic nie mówi o aplikacji. Naprawa w serwisie: {@code @Transactional} (productNamesOfInTransaction)
    //   albo pobranie danych zapytaniem z JOIN FETCH.
    //
    // PUŁAPKA: błędy bazy zgłaszane dopiero przy flush/commit (druga z trzech). Zmiana nazwy produktu na już istniejącą (kolumna
    //   ma UNIQUE) NIE daje błędu w momencie setName — Hibernate odkłada UPDATE do flush. Test, który kończy się
    //   rollbackiem, nigdy nie zrobi flush ani commit, więc zielony test nie dowodzi, że zapis w ogóle przejdzie
    //   (test constraintViolationHiddenUntilFlush: bez flush brak błędu, z {@code repository.flush()} —
    //   DataIntegrityViolationException). Dla zapisu nowych encji z IDENTITY INSERT idzie od razu (tak jest u nas),
    //   ale przy innych strategiach generowania id (SEQUENCE) i przy UPDATE-ach — dopiero przy flush.
    //   Lekarstwo: {@code tem.flush()} (albo {@code repository.flush()}) po akcji, której skutek sprawdzasz.
    //
    // PUŁAPKA: brak commitu (trzecia z trzech). Rzeczy zależne od zatwierdzenia transakcji (np. zdarzenia
    //   {@code @TransactionalEventListener(AFTER_COMMIT)}, widoczność zmian dla innych połączeń, blokady) w teście z
    //   rollbackiem po prostu się nie wydarzają. To zadanie dla testów bez transakcji testu (poniżej).
    //
    // JAK WYŁĄCZYĆ / ZMIENIĆ transakcję testu:
    //   • {@code @Transactional(propagation = Propagation.NOT_SUPPORTED)} na klasie lub metodzie: test nie dostaje
    //     transakcji (Test04NoTransactionTest). Teraz serwis rzuca LazyInitializationException ("…Category.products…"),
    //     a metoda z {@code @Transactional} działa — test zachowuje się jak aplikacja. UWAGA: zapisy repozytorium są
    //     wtedy zatwierdzane od razu i NIKT ich nie wycofa — sprzątaj sam.
    //   • {@code @Commit} (to samo co {@code @Rollback(false)}): transakcja testu zostaje zatwierdzona po teście.
    //     Test04CommitTest: test 1 z @Commit zostawia produkt, test 2 go widzi; wpis z testu 2 (domyślny rollback)
    //     znika, a zapis z testu bez transakcji (NOT_SUPPORTED) przetrwa. Ta klasa ma własną konfigurację
    //     ({@code properties = "demo.commit=true"}), więc własny kontekst i własną losową bazę — zatwierdzone
    //     wiersze nie psują innych klas. Kolejność jest tam ustalona CELOWO (@Order), bo pokazujemy, co jeden
    //     test zostawia następnemu.
    //   • {@code TestTransaction} (isActive, isFlaggedForRollback, flagForCommit, end, start) pozwala sterować
    //     transakcją testu z kodu (np. zatwierdzić w środku testu i otworzyć nową).
    //
    // PUŁAPKA: {@code @Commit} / zapisy bez transakcji bez sprzątania. Dlaczego? Zatwierdzone wiersze zostają w bazie
    //   (a baza wycinka jest współdzielona przez klasy o tej samej konfiguracji — Test01TestSlices, sekcja 4), więc
    //   inne testy zaczynają widzieć dane, których nie wstawiały, i wynik zależy od kolejności. Używaj tylko tam,
    //   gdzie commit jest tematem testu, w osobnej konfiguracji i ze sprzątaniem.
    //
    // DOBRA PRAKTYKA: domyślny rollback dla testów repozytoriów; dla każdego zachowania zależnego od granic
    //   transakcji dodaj JEDEN test bez transakcji testu albo test całej aplikacji (@SpringBootTest, s10_transactions).

    // =================================================================================================
    // 4. @AutoConfigureTestDatabase(replace = NONE) — BAZA Z KONFIGURACJI (Test04RealDatabaseTest)
    // =================================================================================================
    //
    //   @DataJpaTest(showSql = false)
    //   @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)   // pakiet: org.springframework.boot.jdbc.test.autoconfigure
    //
    // Domyślnie ({@code replace = NON_TEST} w Boot 4; stare tutoriale piszą {@code ANY}) wycinek ODRZUCA bazę
    // z konfiguracji aplikacji i podstawia wbudowaną o losowej nazwie. Bazy dostarczonej przez sam test
    // ({@code @ServiceConnection}, {@code @DynamicPropertySource}) NIE podmienia — szczegóły w Test06Testcontainers.
    // Z {@code NONE} używa tej, którą skonfigurowałeś: u nas {@code jdbc:h2:mem:test04} z profilu (Test04RealDatabaseTest
    // sprawdza adres). Po co? Gdy testujesz na bazie TAKIEJ JAK PRODUKCYJNA: w Test06Testcontainers ten sam zapis
    // z {@code NONE} da prawdziwy PostgreSQL w kontenerze (adres podstawi {@code @ServiceConnection}). H2 w trybie
    // pamięci nie zna wszystkich rzeczy z PostgreSQL (typy, funkcje, ILIKE), więc zapytania natywne i migracje
    // warto sprawdzać na docelowej bazie.
    //
    // UWAGA: baza o stałej nazwie jest wspólna dla wszystkich kontekstów, które jej używają. Dane zatwierdzone przez
    // jeden test są widoczne dla pozostałych — rollback chroni tylko, gdy nikt nie robi commit.
    //
    // DOBRA PRAKTYKA: szybkie testy repozytoriów na H2 (domyślne ustawienie) plus kilka na prawdziwej bazie (NONE +
    //   Testcontainers). Dlaczego? H2 daje szybkość, prawdziwa baza — pewność, że SQL i migracje działają
    //   w docelowym silniku.

    // =================================================================================================
    // 5. @Sql, WŁASNE ZAPYTANIA I CZEGO NIE TESTOWAĆ (Test04QueriesTest)
    // =================================================================================================
    //
    // • {@code @Sql("/test04/extra-products.sql")} (pakiet org.springframework.test.context.jdbc) wykonuje skrypt
    //   PRZED testem — na klasie (dla wszystkich testów) albo na metodzie. Skrypt idzie w transakcji testu, więc po
    //   teście jest wycofany razem z resztą (test sqlDataVisible: 6 produktów; następny test: znów 5). Skrypty
    //   trzymaj w {@code src/test/resources}. Dla danych, które mają być WSPÓLNE dla wielu testów — migracje Flyway
    //   (u nas V2); dla danych potrzebnych jednemu testowi — @Sql albo {@code tem.persist(...)}.
    // • Testuj to, co NAPISAŁEŚ: własne {@code @Query} (findLowStock: filtr po stanie, kolejność, pusty wynik),
    //   metody z nazw dające nietypowy wynik, projekcje (findByCategoryNameOrderByName zwraca ProductSummary z nazwą
    //   i ceną), zapytania natywne, ograniczenia bazy (UNIQUE, NOT NULL — z flush!).
    // • NIE testuj metod {@code JpaRepository} (save, findById, count, deleteById…) "bo są" — to kod Spring Data,
    //   który ma własne tysiące testów. Taki test sprawdza framework, nie Twoją aplikację, a kosztuje tyle samo
    //   co każdy inny. Wyjątek: metoda, którą chcesz zobaczyć w działaniu dla własnej encji (np. odwzorowanie pól —
    //   Test04EntityManagerTest) — jeden test mapowania encji wystarczy.
    //
    // PUŁAPKA: test, który sprawdza tylko, że zapytanie "coś zwraca". Dlaczego? {@code assertThat(list).isNotEmpty()}
    //   przejdzie także dla zepsutego filtru. Zawsze sprawdzaj dokładną zawartość i kolejność (containsExactly)
    //   oraz przypadek pusty (findLowStock(-1) → pusta lista).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • {@code @DataJpaTest(showSql = false)} + {@code @ActiveProfiles("...")}: encje, repozytoria, EntityManager, baza
     *     wbudowana z losową nazwą, Flyway lekcji, rollback po każdym teście. Brak kontrolerów i serwisów
     *     ({@code @Import} dokłada serwis).
     *   • TestEntityManager: persistAndFlush, find, flush, clear, refresh, detach. Przed asercją na bazie: flush + clear.
     *   • Pułapki transakcji testu: ukrywa LazyInitializationException i błędy bazy zgłaszane przy flush/commit;
     *     brak commitu. Sterowanie: {@code @Transactional(propagation = NOT_SUPPORTED)}, {@code @Commit} /
     *     {@code @Rollback(false)}, {@code TestTransaction}.
     *   • Zatwierdzone dane przeciekają do innych testów — własna konfiguracja i sprzątanie.
     *   • {@code @AutoConfigureTestDatabase(replace = NONE)}: baza z konfiguracji (np. prawdziwy PostgreSQL).
     *   • {@code @Sql}: dane testowe z pliku, w transakcji testu (wycofywane). Testuj własne @Query i projekcje, nie
     *     metody JpaRepository.
     *   • Z Flyway: {@code ddl-auto=validate} (Boot nie ustawia ddl-auto sam).
     *
     * PYTANIA KONTROLNE:
     *   1. Co zawiera kontekst {@code @DataJpaTest}, a czego w nim nie ma? Jak dodasz do niego serwis?
     *   2. Co się stanie? Serwis czyta leniwą kolekcję encji po zakończeniu własnej transakcji (bez
     *      {@code @Transactional}), a test tego serwisu to zwykły test {@code @DataJpaTest}. A co, gdy dopiszesz do
     *      klasy testowej {@code @Transactional(propagation = Propagation.NOT_SUPPORTED)}?
     *   3. Co się stanie? W teście zmieniasz nazwę produktu na już istniejącą (kolumna ma UNIQUE) przez setter, bez
     *      save i bez flush. A po {@code repository.flush()}?
     *   4. ZNAJDŹ BŁĄD: test zmienia cenę przez JdbcClient ({@code UPDATE}), potem robi
     *      {@code repository.findById(2L)} i oczekuje nowej ceny — dostaje starą. Dlaczego i jak to naprawić?
     *   5. Co się stanie? Test 1 ma {@code @Commit} i wstawia produkt, test 2 (ta sama baza, domyślny rollback)
     *      zlicza produkty. Co zobaczy test 2? A jeśli test 1 nie ma {@code @Commit}?
     *   6. Po co jest {@code @AutoConfigureTestDatabase(replace = NONE)} i co się zmienia w adresie bazy?
     *   7. ZNAJDŹ BŁĄD: test {@code assertThat(repository.findAll()).isNotEmpty()} dla własnego zapytania z filtrem
     *      jest zielony, a filtr jest zepsuty. Co sprawdzić zamiast tego?
     *   8. Które metody repozytorium warto testować w {@code @DataJpaTest}, a których nie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — tym razem piszesz TESTY. Szkielet: Test04DataJpaExercisesTest (@Tag("cwiczenie")),
    // rozwiązania: s11_testing.solutions.test04_data_jpa_test.Test04DataJpaSolutionsTest (@Tag("wzorzec"))
    // =================================================================================================
    //
    // ĆWICZENIE 1 (łatwe): sprawdź projekcję: findByCategoryNameOrderByName("Dom") zwraca Kubek i Lampkę (w tej
    //   kolejności). Podpowiedź: sekcja 5 — extracting(ProductSummary::getName).containsExactly(...).
    //
    // ĆWICZENIE 2 (łatwe): zapisz produkt przez TestEntityManager.persistAndFlush i sprawdź wiersz w bazie przez
    //   JdbcClient. Podpowiedź: sekcja 2 — tem.find(Category.class, 2L) daje kategorię "Dom".
    //
    // ĆWICZENIE 3 (średnie): odtwórz pułapkę kontekstu: po UPDATE przez JdbcClient findById zwraca starą cenę,
    //   a po tem.clear() nową. Podpowiedź: sekcja 2 — kolejność: findById, UPDATE, findById, clear, findById.
    //
    // ĆWICZENIE 4 (średnie): użyj @Sql("/test04/extra-products.sql"): produktów ma być 6, a findLowStock(40) ma
    //   zawierać Zeszyt. Podpowiedź: sekcja 5.
    //
    // ĆWICZENIE 5 (trudniejsze): udowodnij, że bez transakcji testu CategoryService.productNamesOf rzuca
    //   LazyInitializationException, a productNamesOfInTransaction działa. Podpowiedź: sekcja 3 —
    //   @Import(CategoryService.class) i @Transactional(propagation = Propagation.NOT_SUPPORTED) na klasie.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Są: encje, repozytoria Spring Data, EntityManager i TestEntityManager, DataSource (baza wbudowana o
     *      losowej nazwie), transakcje, Flyway. Nie ma: kontrolerów, serwisów, MVC. Serwis dokładasz
     *      {@code @Import(CategoryService.class)}. Sprawdzone w Test04SliceTest i Test04TransactionTest.
     *   2. W zwykłym @DataJpaTest — nic: test działa w jednej transakcji, więc sesja jest otwarta i kolekcja się
     *      ładuje (błąd ukryty). Z {@code NOT_SUPPORTED}: LazyInitializationException (komunikat wskazuje
     *      Category.products), czyli to samo, co w aplikacji. Sprawdzone w Test04TransactionTest
     *      i Test04NoTransactionTest.
     *   3. Bez flush: nic, żadnego błędu (Hibernate odkłada UPDATE). Po {@code repository.flush()}:
     *      DataIntegrityViolationException (naruszone ograniczenie UNIQUE). Sprawdzone w Test04TransactionTest.
     *   4. findById zwraca encję z kontekstu utrwalania (wczytaną wcześniej), a UPDATE przez JdbcClient obszedł
     *      Hibernate. Naprawa: {@code tem.clear()} (albo {@code tem.refresh(encja)}) przed odczytem, żeby poszedł do
     *      bazy. Sprawdzone w Test04EntityManagerTest (assertionFromMemoryTrap, refresh).
     *   5. Test 2 zobaczy wiersz z testu 1 (commit go utrwalił), więc zlicza o jeden więcej. Bez {@code @Commit}
     *      transakcja testu 1 jest wycofana i test 2 widzi tylko dane z migracji. Sprawdzone w Test04CommitTest.
     *   6. Wycinek używa wtedy bazy z konfiguracji zamiast podstawiać wbudowaną o losowej nazwie. U nas adres to
     *      {@code jdbc:h2:mem:test04} (z profilu); z prawdziwym PostgreSQL będzie to adres kontenera
     *      (Test06Testcontainers). Sprawdzone w Test04RealDatabaseTest i Test04SliceTest.
     *   7. isNotEmpty przechodzi także dla błędnego filtru. Sprawdź dokładną zawartość i kolejność
     *      ({@code extracting(...).containsExactly("Lampka", "Monitor")}) oraz przypadek pusty
     *      ({@code findLowStock(-1)} → pusta lista). Sprawdzone w Test04QueriesTest (lowStock).
     *   8. Warto: własne @Query, metody z nazw o nietypowym wyniku, projekcje, zapytania natywne, ograniczenia bazy
     *      i mapowanie encji (jeden test). Nie warto: save, findById, count, deleteById z JpaRepository — to
     *      przetestowany kod Spring Data.
     */
    // </editor-fold>
}
