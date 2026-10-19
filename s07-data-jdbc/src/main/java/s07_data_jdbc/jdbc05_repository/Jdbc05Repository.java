package s07_data_jdbc.jdbc05_repository;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.util.List;

/**
 * <pre>
 * TEMAT: Repozytorium na JdbcClient i test wycinkowy @JdbcTest
 *        (repository = repozytorium, „magazyn danych”; slice test = test wycinkowy, ładujący tylko część aplikacji;
 *         constraint = ograniczenie w bazie; rollback = wycofanie zmian; domain exception = wyjątek domenowy)
 *
 * W SKRÓCIE:
 *   Repozytorium to klasa, która jako JEDYNA zna SQL i tabele. Reszta aplikacji (serwisy, kontrolery) mówi do niej
 *   językiem biznesu: {@code findBySku("ELE-001")}, {@code save(nowyProdukt)}. Dzięki temu SQL nie rozlewa się po
 *   całym kodzie, a zmiana bazy czy zapytania dotyka jednego miejsca. Repozytorium testujemy na prawdziwej bazie (H2)
 *   testem wycinkowym {@code @JdbcTest}: startuje tylko to, co potrzebne do JDBC — szybko, bez serwera WWW i bez
 *   reszty aplikacji.
 *
 * ANALOGIA: bibliotekarz.
 *   Czytelnik (serwis) nie chodzi po magazynie i nie zna systemu półek (SQL). Mówi „poproszę Czysty kod” i dostaje
 *   książkę albo „nie mamy takiej”. Bibliotekarz (repozytorium) wie, gdzie co leży; gdy ktoś próbuje wpisać dwie
 *   książki pod tym samym numerem katalogowym, zgłasza to po ludzku („taka pozycja już istnieje”), a nie żargonem
 *   magazynu.
 *
 * JAK TO DZIAŁA:
 *   1. ProductRepository dostaje JdbcClient (konstruktor) i ma po jednej metodzie na każdą operację biznesową.
 *   2. Ograniczenia (UNIQUE na sku, CHECK na cenie, NOT NULL) leżą w BAZIE — migracja V1 w db/migration/jdbc05.
 *   3. Błąd bazy wraca jako DataAccessException (lekcja Jdbc02JdbcTemplate); repozytorium zamienia wybrany na wyjątek
 *      domenowy (DuplicateKeyException → DuplicateSkuException).
 *   4. @JdbcTest: Spring uruchamia kontekst z DataSource, JdbcClient, JdbcTemplate, menedżerem transakcji i Flyway;
 *      NIE skanuje beanów @Component/@Repository/@Service — dlatego repozytorium dołączamy przez {@code @Import}.
 *      Każdy test działa w transakcji wycofywanej po teście (rollback), więc testy nie psują sobie nawzajem danych.
 *   5. Domyślnie @JdbcTest PODMIENIA bazę z konfiguracji na świeżą wbudowaną (H2 z losową nazwą);
 *      {@code @AutoConfigureTestDatabase(replace = NONE)} zostawia bazę z konfiguracji aplikacji.
 *
 * SŁÓWKA:
 *   repository = repozytorium; slice = wycinek; replace = podmienić; rollback = wycofanie transakcji;
 *   constraint = ograniczenie; unique = unikalny; duplicate = duplikat; domain = domena (dziedzina biznesowa);
 *   import = dołączyć klasę do kontekstu; real = prawdziwy.
 *
 * ZOBACZ TEŻ: t29_jdbc_databases/Jdbc05Dao (wzorzec DAO w czystym JDBC),
 *             s07_data_jdbc/Jdbc01JdbcClient (JdbcClient), s07_data_jdbc/Jdbc02JdbcTemplate (hierarchia wyjątków),
 *             s07_data_jdbc/Jdbc04Flyway (migracje tej lekcji),
 *             s01_core_ioc/Ioc03Stereotypes (@Repository jako stereotyp),
 *             s10_transactions/Tx01Transactional (transakcje w serwisach).
 * </pre>
 */
public final class Jdbc05Repository {

    private Jdbc05Repository() {
    }

    // =================================================================================================
    // 1. REPOZYTORIUM: JEDYNE MIEJSCE Z SQL
    // =================================================================================================
    //
    //   @Repository
    //   class ProductRepository {
    //       List<Product> findAll() / findByCategory(...)
    //       Optional<Product> findBySku(String sku)
    //       Product save(NewProduct product)           ← zwraca produkt z id nadanym przez bazę
    //       boolean updatePrice(...) / deleteBySku(...) ← true = coś zmieniono
    //       int count()
    //   }
    //
    // Nazwy metod mówią o biznesie, nie o SQL; zwracane typy mówią prawdę: Optional (może nie być), List (może być
    // pusta), boolean (udało się lub nie). Zapis przyjmuje osobny rekord NewProduct (bez id), a zwraca Product (z id)
    // — kompilator nie pozwoli pomylić zapisanego z niezapisanym. Test sekcji 1: pełny obieg zapis → odczyt → zmiana → usunięcie.
    //
    // DOBRA PRAKTYKA: kolumny w SELECT wypisuj jawnie (zamiast *). Dlaczego? Rekord Product mapuje po
    //   nazwach kolumn; jawna lista chroni przed niespodzianką, gdy ktoś doda kolumnę do tabeli.

    // =================================================================================================
    // 2. Optional I WYNIK update(): PRAWDZIWE TYPY ZAMIAST null
    // =================================================================================================
    //
    // findBySku zwraca Optional: brak produktu to zwykła sytuacja, więc wywołujący musi zdecydować, co z nią
    // zrobić (np. zgłosić 404). updatePrice i deleteBySku zwracają boolean wyliczony z liczby wierszy
    // (update() == 1); false znaczy „nie było takiego produktu” — a nie błąd. Test sekcji 2.
    //
    // PUŁAPKA: repozytorium zwraca null dla braku wyniku (albo rzuca ogólny wyjątek).
    //   Dlaczego? null jest niewidoczny w typie — pierwsze wywołanie metody na wyniku daje
    //   NullPointerException daleko od przyczyny. Optional wymusza obsługę przypadku „brak”.

    // =================================================================================================
    // 3. BŁĘDY BAZY → WYJĄTEK DOMENOWY; OGRANICZENIA PILNUJE BAZA
    // =================================================================================================
    //
    //   try { jdbc.sql("INSERT ...").update(...); }
    //   catch (DuplicateKeyException e) { throw new DuplicateSkuException(sku, e); }
    //
    // Drugi produkt z tym samym SKU łamie ograniczenie UNIQUE: baza zgłasza błąd, Spring zamienia go na
    // DuplicateKeyException, a repozytorium na DuplicateSkuException („Produkt o SKU ELE-001 już istnieje”).
    // Serwis i kontroler łapią wyjątek domenowy — nie znają JDBC. Oryginalny wyjątek zostaje jako przyczyna (cause).
    // Inne naruszenia (ujemna cena — CHECK, brak nazwy — NOT NULL) to DataIntegrityViolationException, ale NIE
    // DuplicateKeyException — dzięki temu odróżnisz „duplikat” od innych złych danych. Test sekcji 3.
    //
    // PUŁAPKA: „sprawdź, potem wstaw”: if (!existsBySku(sku)) save(...).
    //   Dlaczego? Między sprawdzeniem a zapisem inny użytkownik może wstawić ten sam SKU (wyścig, race condition) —
    //   dwa żądania przejdą sprawdzenie, a drugi zapis i tak zderzy się z ograniczeniem. Skoro baza i tak musi pilnować
    //   unikalności, polegaj na ograniczeniu UNIQUE i łap wyjątek; sprawdzanie przed zapisem dodaje tylko zapytanie
    //   (może służyć wyłącznie do ładniejszego komunikatu).
    //
    // DOBRA PRAKTYKA: ograniczenia (UNIQUE, NOT NULL, CHECK, klucz obcy) definiuj w bazie, a nie tylko w Javie.
    //   Dlaczego? Z bazy korzysta nie tylko Twoja aplikacja (inne serwisy, ręczne skrypty, migracje danych) —
    //   walidacja w Javie ich nie obejmie. Baza jest ostatnią linią obrony.

    // =================================================================================================
    // 4. @JdbcTest — TEST WYCINKOWY
    // =================================================================================================
    //
    //   @JdbcTest                              // pakiet: org.springframework.boot.jdbc.test.autoconfigure (Boot 4)
    //   @Import(ProductRepository.class)       // repozytorium dołączamy ręcznie
    //   @ActiveProfiles("jdbc05")              // ustawienia lekcji (Flyway: katalog migracji)
    //   class Jdbc05RepositoryJdbcTest { @Autowired ProductRepository repository; ... }
    //
    // Co ładuje @JdbcTest (sprawdzone w teście sekcji 4): DataSource, JdbcClient, JdbcTemplate, menedżer transakcji,
    // Flyway (migracje wykonują się na bazie testowej). Czego NIE ładuje: beanów ze skanowania (@Repository, @Service,
    // @Component) — stąd @Import. Baza: DOMYŚLNIE podmieniona na świeżą wbudowaną H2 o losowej nazwie
    // (adres z application-jdbc05.properties jest wtedy ignorowany). Każdy test działa w transakcji, którą Spring
    // wycofuje po teście — dlatego zapisy z jednego testu nie są widoczne w drugim i kolejność testów nie ma znaczenia.
    //
    // PUŁAPKA: pakiet adnotacji. W starszych tutorialach (Boot 2/3) @JdbcTest importuje się z
    //   org.springframework.boot.test.autoconfigure.jdbc. W Boot 4 klasa leży w
    //   org.springframework.boot.jdbc.test.autoconfigure (moduł spring-boot-jdbc-test, starter
    //   spring-boot-starter-jdbc-test). Dlaczego? Boot 4 podzielił testy na moduły — każda technologia ma własny.
    //   Zły import = błąd kompilacji „cannot find symbol: class JdbcTest” (stary pakiet nadal istnieje, ale tej klasy już w nim nie ma).
    //
    // PUŁAPKA: zapomniany @Import. Test z @Autowired ProductRepository kończy się
    //   UnsatisfiedDependencyException / NoSuchBeanDefinitionException. Dlaczego? @JdbcTest celowo nie skanuje
    //   komponentów, żeby był szybki i izolowany. Sprawdzone testem (sekcja 4): w kontekście wycinkowym
    //   typu ProductRepository po prostu nie ma.

    // =================================================================================================
    // 5. @AutoConfigureTestDatabase(replace = NONE) — PRAWDZIWA BAZA Z KONFIGURACJI
    // =================================================================================================
    //
    // Domyślnie (replace = ANY) test dostaje własną, tymczasową bazę wbudowaną — bezpieczne, ale to NIE jest baza
    // z ustawień aplikacji. Gdy chcesz testować na bazie z konfiguracji (np. PostgreSQL w kontenerze —
    // Testcontainers z @ServiceConnection; poza tą lekcją), dodaj
    // @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE). W tej lekcji pokazujemy
    // różnicę na adresie połączenia: domyślnie jdbc:h2:mem:<losowa nazwa>, z NONE — jdbc:h2:mem:jdbc05
    // (jak w application-jdbc05.properties). Test sekcji 5.
    //
    // PUŁAPKA: replace = NONE przy zwykłej bazie lokalnej lub testowej, której dane zostają. Transakcja testu jest
    //   wycofywana, ale zmiany DDL i dane zatwierdzone przez coś innego (np. migracje, inna aplikacja) zostają —
    //   wynik testu może zależeć od stanu bazy. Dlaczego? Domyślna podmiana na świeżą bazę gwarantuje czyste
    //   środowisko; wyłączając ją, bierzesz tę gwarancję na siebie.

    // =================================================================================================
    // 6. @Repository W PEŁNYM KONTEKŚCIE: PROXY TŁUMACZĄCE WYJĄTKI
    // =================================================================================================
    //
    // W pełnej aplikacji (@SpringBootTest, zwykły start) Boot rejestruje postprocesor tłumaczenia wyjątków,
    // który obejmuje beany z @Repository. Test sekcji 6 pokazuje skutek: bean ProductRepository jest wtedy proxy
    // (klasa wygenerowana przez CGLIB: ProductRepository$$SpringCGLIB$$0), a w kontekście @JdbcTest — zwykłym obiektem (sprawdza to test sekcji 4).
    // Dla JdbcClient to proxy niczego nie zmienia (wyjątki są już zamienione na DataAccessException), ale warto
    // wiedzieć, że @Repository to nie tylko „etykieta”. Szczegóły stereotypów: lekcja Ioc03Stereotypes.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Repozytorium = jedyne miejsce z SQL; metody w języku biznesu; Optional zamiast null; boolean z update() == 1.
     *   • Unikalność i inne ograniczenia pilnuje BAZA; DuplicateKeyException → wyjątek domenowy (przyczyna zachowana).
     *   • DataIntegrityViolationException to szersza rodzina (NOT NULL, CHECK, klucz obcy); DuplicateKeyException — jej
     *     podklasa dla UNIQUE.
     *   • @JdbcTest (Boot 4: org.springframework.boot.jdbc.test.autoconfigure): DataSource, JdbcClient, Flyway,
     *     transakcja z rollbackiem; bez @Repository/@Service — dołącz @Import(MojeRepozytorium.class).
     *   • Domyślnie baza testowa jest podmieniana na świeżą wbudowaną; @AutoConfigureTestDatabase(replace = NONE)
     *     zostawia bazę z konfiguracji.
     *   • Starter testów: spring-boot-starter-jdbc-test.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego repozytorium zwraca Optional<Product> z findBySku, a nie null?
     *   2. Co się stanie? Wywołujesz save(...) z SKU, które już jest w tabeli product.
     *   3. Co się stanie? W teście @JdbcTest wstrzykujesz @Autowired ProductRepository, ale zapomniałeś
     *      o @Import.
     *   4. ZNAJDŹ BŁĄD: if (!existsBySku(sku)) { save(product); } — czemu to nie chroni przed duplikatami
     *      i co lepiej zrobić?
     *   5. Czy zapis zrobiony w jednym teście @JdbcTest jest widoczny w następnym? Dlaczego?
     *   6. Jak zmienić test, żeby korzystał z bazy z application-jdbc05.properties, a nie z podmienionej?
     *   7. ZNAJDŹ BŁĄD: w nowym projekcie Boot 4 importujesz org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
     *      z poradnika — co zobaczysz i dlaczego?
     *   8. Po co zamieniać DuplicateKeyException na wyjątek domenowy?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jdbc05RepositoryExercisesTest (baza H2 ze schematem migracji V1 i V2 lekcji)
    // =================================================================================================

    /** Rekord wyniku ćwiczenia 4: kategoria i liczba produktów w niej. */
    public record CategoryCount(String category, long productCount) {
    }

    /**
     * ĆWICZENIE 1 (łatwe): zwróć produkty o cenie NIE WYŻSZEJ niż maxPrice, od najtańszego; produkty o tej
     * samej cenie — po SKU. Podpowiedź: {@code WHERE price <= :maxPrice ORDER BY price, sku}.
     */
    static List<Product> exercise1(JdbcClient jdbc, BigDecimal maxPrice) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): usuń z tabeli wszystkie produkty, których nie ma na stanie (stock = 0),
     * i zwróć liczbę usuniętych wierszy. Podpowiedź: DELETE ... WHERE ... i update().
     */
    static int exercise2(JdbcClient jdbc) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zapisz produkt i zwróć go z nadanym id; gdy SKU już istnieje — rzuć
     * {@link DuplicateSkuException} (z oryginalnym wyjątkiem jako przyczyną). Podpowiedź: GeneratedKeyHolder,
     * update(keyHolder, "id"), catch (DuplicateKeyException e).
     */
    static Product exercise3(JdbcClient jdbc, NewProduct product) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć liczbę produktów w każdej kategorii, posortowane alfabetycznie po kategorii.
     * Podpowiedź: GROUP BY category, alias kolumny product_count (Spring zamieni go na pole
     * productCount rekordu) i query(CategoryCount.class).
     */
    static List<CategoryCount> exercise4(JdbcClient jdbc) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Produktu może nie być — to normalna sytuacja. Optional zapisuje to w typie i zmusza wywołującego do decyzji;
     *      null jest niewidoczny i kończy się NullPointerException daleko od przyczyny.
     *   2. Baza zgłosi naruszenie UNIQUE, Spring zamieni je na DuplicateKeyException, a repozytorium rzuci
     *      DuplicateSkuException („Produkt o SKU ... już istnieje”). Sprawdzone testem (sekcja 3).
     *   3. Kontekst wycinkowy nie skanuje @Repository, więc beana ProductRepository nie ma: błąd wstrzykiwania
     *      (UnsatisfiedDependencyException — brak beana tego typu). Sprawdzone testem (sekcja 4).
     *   4. Między SELECT a INSERT inny użytkownik może wstawić ten sam SKU (wyścig), a sprawdzenie nic nie gwarantuje.
     *      Lepiej: ograniczenie UNIQUE w bazie i złapanie DuplicateKeyException przy zapisie.
     *   5. Nie. Każdy test @JdbcTest działa w transakcji wycofywanej po teście (rollback), więc zmiany znikają.
     *      Dzięki temu kolejność testów nie ma znaczenia.
     *   6. Dodać @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE). Sprawdzone testem (sekcja 5).
     *   7. Błąd kompilacji „cannot find symbol” dla klasy JdbcTest (w starym pakiecie jej już nie ma). W Boot 4 adnotacja jest w org.springframework.boot.jdbc.test.autoconfigure
     *      (testy rozdzielono na moduły technologii; moduł spring-boot-jdbc-test).
     *   8. Warstwy wyżej (serwis, kontroler) nie muszą znać JDBC ani kodów błędów bazy; wyjątek domenowy mówi, co się
     *      stało w języku biznesu („SKU zajęte”), i łatwo go zamienić np. na odpowiedź HTTP 409.
     */
    // </editor-fold>
}
