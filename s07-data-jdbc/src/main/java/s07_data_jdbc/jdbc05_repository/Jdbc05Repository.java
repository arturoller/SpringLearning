package s07_data_jdbc.jdbc05_repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * <pre>
 * TEMAT: Wzorzec repozytorium na JDBC — ProductRepository, wyjątki bazy, serwis i transakcja
 *        (repository = repozytorium, „magazyn” obiektów; exception translation = tłumaczenie wyjątków;
 *         transaction = transakcja; optimistic locking = optymistyczne blokowanie; version = wersja wiersza;
 *         service = serwis, warstwa logiki biznesowej)
 *
 * W SKRÓCIE:
 *   Repozytorium to jedyne miejsce, które zna SQL. Reszta aplikacji widzi tylko interfejs
 *   ({@code ProductRepository}: findById, findAll, findByCategory, save, deleteById) i operuje na rekordach Javy.
 *   Serwis (logika biznesowa) korzysta z repozytorium, a kontroler (s04) z serwisu. Dzięki temu SQL można zmienić
 *   (albo wymienić JDBC na JPA — s08) bez ruszania logiki, a logikę przetestować na atrapie bez bazy.
 *   Błędy bazy (złamany UNIQUE, NOT NULL, klucz obcy) wychodzą jako wyjątki z jednej hierarchii
 *   {@code DataAccessException}, niezależnej od rodzaju bazy. Gdy operacja to kilka zapisów — dodajemy transakcję.
 *
 * ANALOGIA: bibliotekarz przy okienku.
 *   Czytelnik (serwis) mówi: „poproszę książkę o numerze 7” albo „oddaję tę książkę”. Nie wie, czy katalog jest
 *   na kartkach, czy w komputerze, ani w której hali leżą regały — to wiedza bibliotekarza (repozytorium).
 *   Gdyby biblioteka zmieniła system, czytelnik niczego by nie zauważył. Transakcja to formularz „przeniesienie
 *   książek między działami”: albo bibliotekarz przeniesie obie, albo żadnej.
 *
 * JAK TO DZIAŁA:
 *   1. ProductRepository (interfejs) + JdbcProductRepository ({@code @Repository}, na JdbcClient) — Spring tworzy
 *      bean i wstrzykuje JdbcClient konstruktorem. Schemat tabeli product (z UNIQUE, FOREIGN KEY, CHECK, NOT NULL
 *      i kolumną version) tworzą migracje Flyway lekcji (db/migration/jdbc05; por. Jdbc04Flyway).
 *   2. Błąd SQL → SQLException z kodem bazy → JdbcClient przez SQLExceptionTranslator zamienia go na
 *      DataAccessException (RuntimeException, więc nie trzeba go deklarować ani łapać w każdej metodzie).
 *   3. {@code @Repository} dodatkowo włącza (w pełnej aplikacji Boot) PersistenceExceptionTranslationPostProcessor:
 *      wokół beana powstaje proxy, które tłumaczy wyjątki zgłaszane przez technologie bez własnego tłumaczenia
 *      (np. JPA w s08). Dla JdbcClient to pas bezpieczeństwa — wyjątek jest już przetłumaczony (sprawdzone testem).
 *   4. {@code @Transactional} na metodzie serwisu: Spring tworzy proxy, które przed metodą otwiera transakcję
 *      (na połączeniu z puli), a po niej robi commit albo — po wyjątku RuntimeException — rollback.
 *      Wszystkie wywołania repozytorium w środku używają TEGO SAMEGO połączenia.
 *
 * SŁÓWKA:
 *   constraint = ograniczenie bazy (UNIQUE, NOT NULL, FOREIGN KEY, CHECK); mock = atrapa (udawany obiekt);
 *   rollback = wycofanie zmian; commit = zatwierdzenie zmian; auto-commit = zatwierdzanie każdego polecenia osobno;
 *   stale (nieaktualny) = przestarzały odczyt; conflict = konflikt; DAO (Data Access Object) = starszy
 *   odpowiednik repozytorium.
 *
 * ZOBACZ TEŻ: t29_jdbc_databases/Jdbc05Dao (to samo w czystym JDBC: wzorzec DAO),
 *             t29_jdbc_databases/Jdbc04Transactions (transakcje w czystym JDBC: commit, rollback),
 *             s07_data_jdbc/Jdbc01JdbcClient (JdbcClient, @JdbcTest, rollback testów),
 *             s07_data_jdbc/Jdbc04Flyway (migracje, z których pochodzi schemat tej lekcji),
 *             s06_error_handling/Err05CustomExceptions (własne wyjątki biznesowe, jak ProductNotFoundException),
 *             s08_data_jpa/Jpa02Repositories (Spring Data JPA generuje repozytorium za Ciebie),
 *             s10_transactions/Tx01Transactional (pełny temat transakcji).
 * </pre>
 */
public final class Jdbc05Repository {

    private Jdbc05Repository() {
    }

    // =================================================================================================
    // 1. WZORZEC REPOZYTORIUM — interfejs + implementacja na JdbcClient
    // =================================================================================================
    //
    // Interfejs ProductRepository (CO):          Implementacja JdbcProductRepository (JAK):
    //   Optional<Product> findById(long id)        SELECT … WHERE id = :id            → query(Product.class).optional()
    //   List<Product> findAll()                    SELECT … ORDER BY id               → .list()
    //   List<Product> findByCategory(String c)     SELECT … WHERE category = :category ORDER BY id
    //   Product save(Product p)                    id == null → INSERT + klucz generowany; inaczej → UPDATE
    //   boolean deleteById(long id)                DELETE … WHERE id = :id            → update() == 1
    //
    // Konwencje, które warto zapamiętać (są takie same w Spring Data JPA, s08):
    //   • findById zwraca Optional, a NIE null i NIE wyjątek — „brak produktu” to normalna sytuacja (Jdbc01, sekcja 5);
    //   • lista wyników: zawsze ORDER BY, żeby kolejność była przewidywalna;
    //   • save robi INSERT albo UPDATE zależnie od tego, czy rekord ma już numer, i zwraca ZAPISANY obiekt
    //     (z numerem nadanym przez bazę) — bo rekord jest niezmienny, więc nie da się „dopisać” numeru do starego;
    //   • repozytorium NIE zawiera logiki biznesowej ani transakcji — to rola serwisu.
    //
    // PRZED (SQL rozsiany po aplikacji — w kontrolerze, serwisie, raportach):
    //   jdbc.sql("SELECT * FROM product WHERE id = ?")… w dziesięciu miejscach; zmiana nazwy kolumny = dziesięć poprawek.
    // PO (repozytorium): SQL tylko w JdbcProductRepository; reszta wywołuje repository.findById(7).
    //
    // Test sekcji 1: odczyty (w tym brak = Optional.empty()), INSERT z numerem z bazy, UPDATE, DELETE (true/false).
    //
    // PYTANIE REKRUTACYJNE: po co interfejs repozytorium, skoro implementacja jest jedna?
    //   Odpowiedź: serwis zależy od abstrakcji — w teście wstawiasz atrapę (sekcja 4), a przy zmianie technologii
    //   (JDBC → JPA) piszesz nową implementację bez zmian w serwisach.
    //
    // PUŁAPKA: SQL w serwisie albo kontrolerze „bo to tylko jedno zapytanie”. Dlaczego źle? Zapytań przybywa,
    //   logika miesza się z SQL-em i nie da się jej przetestować bez bazy.
    //
    // DOBRA PRAKTYKA: jedno repozytorium na jeden agregat (tu: produkt) i interfejs opisany językiem domeny
    //   (findByCategory), a nie SQL-a (selectWhereCategory). Dlaczego? Interfejs ma przetrwać zmianę technologii.

    // =================================================================================================
    // 2. @REPOSITORY I WYJĄTKI BAZY — hierarchia DataAccessException
    // =================================================================================================
    //
    // Wyjątki sprawdzone testami na H2 (migracja V1 tworzy ograniczenia UNIQUE, FOREIGN KEY, CHECK, NOT NULL):
    //
    //   złamane ograniczenie            wyjątek Springa                       kod H2
    //   UNIQUE (nazwa produktu)         DuplicateKeyException                 23505
    //   NOT NULL                        DataIntegrityViolationException       23502
    //   FOREIGN KEY (kategoria)         DataIntegrityViolationException       23506
    //   CHECK (stock >= 0)              DataIntegrityViolationException       23513
    //
    // Hierarchia: DuplicateKeyException → DataIntegrityViolationException → NonTransientDataAccessException
    //   → DataAccessException → RuntimeException. Czyli DuplicateKeyException JEST szczególnym przypadkiem
    //   DataIntegrityViolationException. Dlatego w catch zawsze najpierw podklasa (DuplicateKeyException),
    //   dopiero potem nadklasa — odwrotnie kompilator zgłosi, że druga gałąź jest nieosiągalna.
    //   Oryginalny SQLException zostaje jako przyczyna (getCause), a w komunikacie jest nazwa ograniczenia
    //   (np. UK_PRODUCT_NAME) — dlatego ograniczeniom warto nadawać nazwy (CONSTRAINT uk_product_name …).
    //   Konkretny podtyp zależy od bazy i sterownika: Spring tłumaczy kody błędów (SQLErrorCodes) i stan SQL
    //   (SQLState), a PostgreSQL czy MySQL mają własne kody — ale nazwy wyjątków Springa są te same. To właśnie
    //   znaczy „hierarchia niezależna od bazy”.
    //
    // CO ROBI @Repository? Dwie rzeczy: (1) oznacza bean jako element warstwy danych (stereotyp, jak @Service);
    // (2) w pełnej aplikacji Boot bean z tą adnotacją dostaje proxy tłumaczące wyjątki (test: AopUtils.isAopProxy).
    // Dla JdbcClient/JdbcTemplate tłumaczenie robią już one same, więc proxy niczego nie zmienia — typ wyjątku z testu
    // wyżej jest ten sam z @Repository i bez. Naprawdę przydaje się przy JPA (s08), gdzie wyjątki Hibernate
    // są dopiero przez nie zamieniane na DataAccessException. W wycinku @JdbcTest proxy NIE powstaje
    // (autokonfiguracji tłumaczenia tam nie ma) — a wyjątki i tak są te same, bo tłumaczy je JdbcClient.
    //
    // PUŁAPKA: łapanie ogólnego {@code catch (Exception e)} wokół zapisu. Dlaczego źle? Zjadasz też błędy
    //   programisty (NullPointerException) i nie odróżniasz „nazwa zajęta” od „baza nie działa”. Łap konkretny typ
    //   (DuplicateKeyException) i przetłumacz go na błąd biznesowy („produkt o takiej nazwie już istnieje”).
    //
    // DOBRA PRAKTYKA: ograniczenia bazy są ostatnią linią obrony, NIE jedyną. Walidację (s06) zrób wcześniej, żeby
    //   użytkownik dostał ładny komunikat; ograniczenie bazy i tak złapie wyścig dwóch żądań naraz (sprawdzenie
    //   „czy nazwa wolna” + zapis nie są atomowe). Dlaczego? Dwa żądania mogą jednocześnie przejść sprawdzenie.

    // =================================================================================================
    // 3. SERWIS NAD REPOZYTORIUM I @TRANSACTIONAL — rollback obu zapisów
    // =================================================================================================
    //
    // ProductService.transferStock(fromId, toId, quantity): przenosi towar między produktami — DWA zapisy:
    //   (1) zmniejsz stan produktu „skąd”,  (2) zwiększ stan produktu „dokąd”.
    // Jeśli (2) się nie uda (np. nie ma produktu „dokąd”), a (1) już poszło — towar „znika”: jest odjęty
    // i nigdzie nie dodany. Transakcja wiąże oba zapisy: albo oba, albo żaden.
    //
    // PRZED (bez transakcji): każde polecenie zatwierdza się osobno (auto-commit). Test: przeniesienie 30 sztuk
    //   z Kubka (100) do nieistniejącego produktu → ProductNotFoundException, a stan Kubka = 70. Zgubione 30 sztuk.
    // PO (@Transactional na metodzie serwisu): ten sam błąd → rollback → stan Kubka nadal 100.
    //
    // Warunek wycofania: wyjątek niesprawdzany (RuntimeException/Error). Wyjątek sprawdzany (Exception) domyślnie
    // NIE wycofuje transakcji — pułapki i opcje (rollbackFor, propagacja, proxy) to s10_transactions/Tx01Transactional.
    //
    // Dlaczego test sekcji 3 to zwykły JUnit na pełnej aplikacji, a nie @JdbcTest? Test z @JdbcTest sam jest jedną
    // transakcją, do której dołącza transakcja serwisu — commit serwisu nic wtedy nie znaczy, bo i tak wycofa go
    // test. Żeby zobaczyć PRAWDZIWY commit i rollback, nikt „z zewnątrz” nie może trzymać transakcji.
    //
    // Serwis w pełnej aplikacji jest proxy (test): to ono otwiera i zamyka transakcję. Dlatego @Transactional działa
    // tylko przy wywołaniu z ZEWNĄTRZ beana (self-invocation, czyli this.metoda(), omija proxy — Tx03ProxyPitfalls).
    //
    // PUŁAPKA: kilka zapisów w serwisie bez @Transactional „bo przecież każdy się udał”. Dlaczego źle? Awaria
    //   w środku (wyjątek, restart serwera) zostawia dane w stanie pośrednim, którego nie da się rozpoznać ani
    //   naprawić — a błąd wychodzi tygodnie później jako „niezgodność w magazynie”.
    //
    // DOBRA PRAKTYKA: @Transactional na metodach SERWISU (granica operacji biznesowej), nie repozytorium.
    //   Dlaczego? Jedna operacja biznesowa często woła kilka metod repozytorium i tylko serwis wie, co ma być atomowe.

    // =================================================================================================
    // 4. TESTOWANIE — @JdbcTest + @Import oraz atrapa repozytorium
    // =================================================================================================
    //
    // a) REPOZYTORIUM: @JdbcTest + @Import(JdbcProductRepository.class) + @ActiveProfiles("jdbc05").
    //    Wycinek @JdbcTest NIE skanuje komponentów (@Repository, @Service, @Component) — test pokazuje, że bez @Import
    //    bean nie istnieje (ObjectProvider.getIfAvailable() == null). Z @Import repozytorium dostaje JdbcClient z wycinka
    //    i pracuje na prawdziwej bazie H2 ze schematem z migracji Flyway (wycinek je uruchamia — Jdbc04Flyway).
    //    Każdy test jest wycofywany, więc dane się nie „brudzą” (Jdbc01, sekcja 1).
    //
    // b) SERWIS jednostkowo: @SpringJUnitConfig(ProductService.class) + @MockitoBean ProductRepository. Kontekst ma
    //    tylko serwis i atrapę; baza w ogóle się nie uruchamia. Sprawdzamy logikę: jakie rekordy trafiają do save()
    //    (verify), że przy braku towaru nie ma żadnego zapisu (never()), jaki wyjątek leci. Adnotacja
    //    {@code @MockitoBean} (Spring 6.2+/7; w Boot 4 jedyna — {@code @MockBean} został usunięty, patrz CLAUDE.md)
    //    podmienia bean w kontekście atrapą Mockito.
    //    Atrapa NIE ma transakcji — wycofanie sprawdza dopiero test na prawdziwej bazie (sekcja 3).
    //
    // Którą warstwę czym testować? SQL repozytorium → prawdziwa baza (literówka w SQL wyjdzie tylko tam).
    // Logikę serwisu → atrapa (szybko, deterministycznie). Transakcje → pełna aplikacja z prawdziwą bazą.
    //
    // PUŁAPKA: test repozytorium na atrapie JdbcClient. Dlaczego bezwartościowy? Sprawdza tylko, że wywołałeś metodę,
    //   a nie czy SQL jest poprawny i czy mapowanie kolumn działa (Jdbc01, sekcja 1).
    //
    // DOBRA PRAKTYKA: dla każdego ograniczenia bazy napisz test, który je łamie, i sprawdź typ wyjątku. Dlaczego?
    //   Kod serwisu będzie łapał ten typ — jeśli ktoś zmieni schemat, test od razu pokaże, że obsługa przestała działać.

    // =================================================================================================
    // 5. OPTYMISTYCZNE BLOKOWANIE — kolumna version
    // =================================================================================================
    //
    // Problem „zgubionej aktualizacji”: Anna i Jan wczytują ten sam produkt (stan 12). Anna zapisuje stan 11. Jan,
    // pracujący na starym odczycie, zapisuje swój wynik — i po cichu kasuje zmianę Anny.
    //
    // Rozwiązanie bez blokad bazy: kolumna version w wierszu.
    //   UPDATE product SET …, version = version + 1 WHERE id = :id AND version = :version
    // UPDATE trafia w wiersz tylko wtedy, gdy nadal ma wersję, którą wczytaliśmy. Liczba zmienionych wierszy 0 =
    // ktoś nas ubiegł (albo usunął wiersz) → repozytorium rzuca OptimisticLockingFailureException
    // (podklasa ConcurrencyFailureException; test). Wywołujący odczytuje produkt od nowa i próbuje jeszcze raz,
    // albo informuje użytkownika „dane zmieniły się w międzyczasie” (ćwiczenie 4).
    //
    // „Optymistyczne”, bo zakładamy, że konflikty są rzadkie, więc nikogo nie blokujemy z góry; płacimy tylko
    // wtedy, gdy konflikt naprawdę się zdarzy. Przeciwieństwo — pesymistyczne (SELECT … FOR UPDATE): blokada wiersza
    // na czas pracy, bezpieczna, ale wolniejsza i wymaga transakcji (więcej w dziale s10_transactions).
    //
    // To dokładnie to, co w JPA robi adnotacja {@code @Version} (s08/s10): Hibernate dopisuje warunek version do UPDATE
    // za Ciebie. Tu widzisz mechanizm odkryty — bez magii.
    //
    // PUŁAPKA: ignorowanie wyniku update() w kodzie zapisu (Jdbc01, sekcja 4). Dlaczego? Bez sprawdzenia liczby
    //   wierszy UPDATE z warunkiem version po cichu „nic nie zrobi” i aplikacja uzna zmianę za zapisaną.
    //
    // DOBRA PRAKTYKA: wersję podnoś WE WŁAŚCIWYM UPDATE (version = version + 1), nie osobnym poleceniem.
    //   Dlaczego? Dwa polecenia nie są atomowe — między nimi ktoś mógłby się wcisnąć.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Repozytorium = interfejs (CO) + implementacja (JAK); SQL tylko w implementacji; findById → Optional.
     *   • save: id == null → INSERT (klucz z bazy), inaczej UPDATE; zwraca zapisany obiekt (rekordy są niezmienne).
     *   • Błędy bazy → DataAccessException (RuntimeException): UNIQUE → DuplicateKeyException,
     *     NOT NULL / FOREIGN KEY / CHECK → DataIntegrityViolationException. Najpierw łap podklasę.
     *   • @Repository: stereotyp + proxy tłumaczące wyjątki (pełna aplikacja); dla JdbcClient tłumaczy on sam.
     *   • @Transactional na metodzie serwisu: wyjątek RuntimeException → rollback wszystkich zapisów; bez niej
     *     każde polecenie zatwierdza się osobno.
     *   • Testy: repozytorium → @JdbcTest + @Import + prawdziwa baza; serwis → @MockitoBean; transakcje → pełna aplikacja.
     *   • Optymistyczne blokowanie: UPDATE … WHERE id = ? AND version = ?, 0 wierszy → OptimisticLockingFailureException.
     *
     * PYTANIA KONTROLNE:
     *   1. Po co interfejs ProductRepository, skoro jest tylko jedna implementacja?
     *   2. Co się stanie? {@code repository.save(Product.create("Mysz", ...))}, gdy produkt „Mysz” już istnieje
     *      (kolumna name ma UNIQUE). Jaki dokładnie wyjątek poleci?
     *   3. Co się stanie? Zapisujesz produkt z kategorią, której nie ma w tabeli category (klucz obcy).
     *      Czy to ten sam wyjątek co przy UNIQUE?
     *   4. ZNAJDŹ BŁĄD: {@code try { repo.save(p); } catch (DataIntegrityViolationException e) { … }
     *      catch (DuplicateKeyException e) { … }} — co jest nie tak?
     *   5. Co się stanie? W metodzie z @Transactional pierwszy UPDATE się udał, drugi rzucił ProductNotFoundException
     *      (RuntimeException). A co, gdyby metoda NIE miała @Transactional?
     *   6. Dlaczego test transakcji serwisu nie może być zwykłym @JdbcTest?
     *   7. Dlaczego w @JdbcTest trzeba dodać @Import(JdbcProductRepository.class)?
     *   8. Co się stanie? Dwie osoby wczytały produkt w wersji 0; pierwsza zapisała zmianę, druga zapisuje swoją.
     *      Co zwróci UPDATE ... WHERE id = ? AND version = 0 i jaki wyjątek poleci?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jdbc05RepositoryExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć nazwy produktów (wszystkich kategorii), których cena jest NIE MNIEJSZA niż
     * {@code minPrice}, posortowane alfabetycznie. Użyj tylko metod interfejsu repozytorium.
     * Podpowiedź: {@code repository.findAll()}, strumień, {@code price().compareTo(minPrice) >= 0}
     * (BigDecimal porównujemy przez compareTo, nie equals — Jdbc01, sekcja 2), {@code sorted()}.
     */
    static List<String> exercise1NamesFromPrice(ProductRepository repository, BigDecimal minPrice) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): spróbuj zapisać produkt i zwróć tekst opisujący wynik: {@code "OK"} (zapisano),
     * {@code "DUPLICATE"} (złamany UNIQUE) albo {@code "INTEGRITY"} (inne złamane ograniczenie: NOT NULL, klucz obcy,
     * CHECK). Inne wyjątki mają wylecieć dalej.
     * Podpowiedź: sekcja 2 — dwa bloki catch, najpierw podklasa (DuplicateKeyException).
     */
    static String exercise2TryInsert(ProductRepository repository, Product product) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwiększ stan magazynu produktu o {@code amount} sztuk i zwróć zapisany produkt.
     * Gdy produktu nie ma — rzuć {@code ProductNotFoundException(id)}.
     * Podpowiedź: {@code findById(...).orElseThrow(...)}, {@code withStock(...)}, {@code save(...)}.
     */
    static Product exercise3Restock(ProductRepository repository, long id, int amount) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): odczytaj produkt, zastosuj do niego {@code change} i zapisz wynik. Jeśli zapis
     * zakończy się {@code OptimisticLockingFailureException} (ktoś był szybszy), odczytaj produkt OD NOWA i spróbuj
     * ponownie — łącznie najwyżej 3 próby; po trzeciej nieudanej wyjątek ma wylecieć dalej. Zwróć zapisany produkt.
     * Podpowiedź: sekcja 5 — pętla {@code for} z numerem próby; funkcję {@code change} wywołuj na ŚWIEŻO wczytanym
     * produkcie (stary odczyt ma starą wersję).
     */
    static Product exercise4UpdateWithRetry(ProductRepository repository, long id, UnaryOperator<Product> change) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Serwis zależy od abstrakcji, nie od JDBC: w teście wstawiasz atrapę (@MockitoBean), a przy zmianie
     *      technologii (np. JDBC → JPA) piszesz nową implementację bez zmian w serwisach.
     *   2. DuplicateKeyException (podklasa DataIntegrityViolationException); komunikat zawiera nazwę ograniczenia
     *      UK_PRODUCT_NAME, a przyczyną jest SQLException z kodem 23505. Sprawdzone testem (sekcja 2).
     *   3. Poleci DataIntegrityViolationException (dokładnie ta klasa, kod 23506, w komunikacie FK_PRODUCT_CATEGORY).
     *      To NIE jest DuplicateKeyException — ta dotyczy tylko UNIQUE/klucza głównego. Sprawdzone testem (sekcja 2).
     *   4. Kolejność catch: najpierw nadklasa, potem podklasa. Druga gałąź jest nieosiągalna (kompilator zgłosi błąd),
     *      bo DuplicateKeyException jest podklasą DataIntegrityViolationException. Najpierw łap DuplicateKeyException.
     *   5. Z @Transactional: rollback — pierwszy UPDATE zostaje wycofany (stan Kubka nadal 100). Bez @Transactional:
     *      każdy UPDATE jest zatwierdzony osobno, więc pierwszy zostaje (stan 70) i towar „zginął”.
     *      Sprawdzone testem (sekcja 3).
     *   6. @JdbcTest sam jest jedną transakcją wycofywaną na końcu testu, a transakcja serwisu dołącza do niej —
     *      nie widać prawdziwego commitu ani rollbacku serwisu. Test na pełnej aplikacji (bez zewnętrznej transakcji)
     *      pokazuje, co faktycznie trafiło do bazy.
     *   7. Wycinek @JdbcTest nie skanuje komponentów (@Repository, @Service…); zawiera tylko infrastrukturę JDBC.
     *      Bez @Import bean repozytorium nie istnieje. Sprawdzone testem (sekcja 4).
     *   8. UPDATE zwróci 0 (wiersz ma już wersję 1), a repozytorium rzuci OptimisticLockingFailureException;
     *      zmiana pierwszej osoby zostaje nienaruszona. Sprawdzone testem (sekcja 5).
     */
    // </editor-fold>
}
