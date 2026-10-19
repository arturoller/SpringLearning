package s08_data_jpa.jpa02_repositories;

/**
 * <pre>
 * TEMAT: Repozytoria Spring Data JPA — interfejs zamiast klasy DAO
 *        (repository = repozytorium, obiekt do zapisu i odczytu encji jednego typu; Spring Data = projekt Springa,
 *         który pisze implementację repozytorium za ciebie; DAO = data access object, obiekt dostępu do danych)
 *
 * W SKRÓCIE:
 *   W Jpa01Entities zapisywałeś encje przez EntityManager. W prawdziwej aplikacji robisz to przez repozytorium:
 *   piszesz JEDNĄ linię — {@code interface ProductRepository extends JpaRepository<Product, Long> {}} — a Spring
 *   Data przy starcie dokłada implementację z kilkunastoma metodami: save, findById, findAll, count, existsById,
 *   deleteById... Ta lekcja pokazuje, co każda z nich robi POD SPODEM (jaki SQL i kiedy), bo tam czają się pułapki:
 *   save nie zawsze robi INSERT, a zwrócony obiekt nie zawsze jest tym, który przekazałeś.
 *
 * ANALOGIA: okienko w bibliotece.
 *   Nie chodzisz sam między regałami (SQL, EntityManager). Podchodzisz do okienka (repozytorium) i mówisz:
 *   „oddaję książkę” (save), „poproszę książkę nr 7” (findById), „ile macie książek?” (count). Bibliotekarz
 *   (SimpleJpaRepository) wie, jak to zrobić. Okienko „tylko wypożyczenia” (ProductCatalog) nie przyjmie zwrotów.
 *
 * JAK TO DZIAŁA:
 *   1. Autokonfiguracja DataJpaRepositoriesAutoConfiguration (moduł spring-boot-data-jpa) włącza skanowanie
 *      repozytoriów w pakiecie klasy @SpringBootApplication — szuka interfejsów rozszerzających Repository.
 *   2. Dla każdego interfejsu tworzy bean: obiekt-pośrednika (proxy JDK), który implementuje interfejs.
 *   3. Wywołanie metody z CrudRepository/JpaRepository proxy przekazuje do SimpleJpaRepository — zwykłej klasy
 *      Spring Data, która używa EntityManagera (persist, merge, find, remove) i ma adnotacje @Repository
 *      oraz @Transactional(readOnly = true) na klasie (metody zapisujące mają własne @Transactional).
 *   4. Wyjątki Hibernate'a są tłumaczone na hierarchię DataAccessException Springa
 *      (np. naruszenie unikalności → DataIntegrityViolationException).
 *
 * SŁÓWKA:
 *   repository = repozytorium; save = zapisz; find = znajdź; exists = istnieje; count = policz; delete = usuń;
 *   merge = scal (skopiuj stan odłączonego obiektu do zarządzanego); reference = odwołanie (pośrednik bez danych);
 *   batch = paczka (wiele wierszy jednym poleceniem); rollback = wycofanie transakcji; persistable = dający się
 *   utrwalić; detached = odłączony (spoza kontekstu utrwalania).
 *
 * ZOBACZ TEŻ: s08_data_jpa/Jpa01Entities (EntityManager, kontekst utrwalania),
 *             t29_jdbc_databases/Jdbc05Dao (DAO pisane ręcznie — porównaj ilość kodu),
 *             s07_data_jdbc/Jdbc05Repository (repozytorium na JdbcClient),
 *             t14_optional/Optional03BestPractices (jak używać Optional z findById),
 *             s11_testing/Test04DataJpaTest (więcej o testach JPA).
 * </pre>
 */
public final class Jpa02Repositories {

    private Jpa02Repositories() {
    }

    // =================================================================================================
    // 1. Repozytorium to interfejs — implementację tworzy Spring Data
    // =================================================================================================
    //
    // Hierarchia interfejsów (Spring Data 2026.0 — spring-data-commons 4.1 i spring-data-jpa 4.1):
    //
    //   Repository<T, ID>                   ← znacznik bez metod („to jest repozytorium”)
    //     └ CrudRepository                  ← save, findById, findAll (Iterable!), count, existsById, delete...
    //         └ ListCrudRepository          ← to samo, ale findAll/saveAll/findAllById zwracają List
    //             └ JpaRepository           ← + ListPagingAndSortingRepository (Jpa05PagingSorting),
    //                                          flush, saveAndFlush, deleteAllInBatch, getReferenceById...
    //
    // Bean ProductRepository to proxy JDK (klasa w rodzaju jdk.proxy2.$Proxy130), a „prawdziwa” robota dzieje się
    // w SimpleJpaRepository. Sprawdzone testem (sekcja 1).
    //
    // DOBRA PRAKTYKA: wybierz NAJWĘŻSZY interfejs, który wystarcza. ListCrudRepository, gdy potrzebujesz tylko CRUD,
    //   JpaRepository, gdy potrzebujesz stronicowania lub flush; goły Repository z wybranymi metodami (sekcja 7),
    //   gdy chcesz zabronić np. usuwania. Dlaczego? Interfejs to umowa: czego nie ma w interfejsie, tego nikt
    //   przez przypadek nie wywoła.

    // =================================================================================================
    // 2. save — persist dla nowej encji, merge dla istniejącej (PYTANIE REKRUTACYJNE)
    // =================================================================================================
    //
    // Kod SimpleJpaRepository.save (Spring Data JPA 4.1, w skrócie):
    //   if (entityInformation.isNew(entity)) { entityManager.persist(entity); return entity; }
    //   else                                 { return entityManager.merge(entity); }
    // „Nowa” = id == null (dla id typu prostego long: id == 0; z polem @Version typu Long: wersja == null).
    //
    //   Nowy produkt (id == null)  → persist → "select next value for products_seq", INSERT przy flush;
    //                                save zwraca TEN SAM obiekt (już zarządzany).
    //   Odłączony produkt (id = 1) → merge   → "select ... from products p1_0 where p1_0.id=?" i UPDATE przy flush;
    //                                save zwraca INNY obiekt — zarządzaną kopię. Przekazany obiekt dalej jest odłączony.
    // Sprawdzone testem (sekcja 2).
    //
    // PUŁAPKA: ignorowanie wyniku save.
    //   PRZED:  repository.save(detached);  detached.setStock(0);        // zmiana przepada!
    //   PO:     Product managed = repository.save(detached);  managed.setStock(0);   // UPDATE przy flush
    //   Dlaczego? merge kopiuje stan do obiektu z kontekstu utrwalania i TEN obiekt zwraca. Hibernate śledzi
    //   kopię, a nie to, co mu podałeś. Zasada: zawsze pracuj dalej na wyniku save.
    //
    // PUŁAPKA: save NIE jest potrzebny dla obiektu zarządzanego (wczytanego w tej samej transakcji) — zmiana pola
    //   wystarczy (dirty checking, Jpa01Entities sekcja 6). save w takiej sytuacji nic nie psuje, ale wiele osób
    //   myli go z „wyślij UPDATE teraz” — UPDATE i tak pójdzie dopiero przy flush.

    // =================================================================================================
    // 3. save z ręcznie nadanym id — zbędny SELECT przed INSERT
    // =================================================================================================
    //
    // DiscountCode ma @Id String code nadawany przez sklep ("LATO10") — bez @GeneratedValue. id nigdy nie jest null,
    // więc save uznaje KAŻDY nowy kod za istniejący i woła merge. Prawdziwy SQL:
    //   select dc1_0.code,dc1_0.percent from discount_codes dc1_0 where dc1_0.code=?   ← merge sprawdza, czy jest
    //   insert into discount_codes (percent,code) values (?,?)                          ← nie ma, więc wstawia
    // Sprawdzone testem (sekcja 3).
    // Przy imporcie 10 000 kodów to 10 000 zbędnych SELECT-ów. Lekarstwa:
    //   • interfejs Persistable z własnym isNew() (ćwiczenie 3) — Spring Data zapyta encję, czy jest nowa;
    //   • pole @Version typu Long (null = nowa) — przy okazji daje blokowanie optymistyczne (s10_transactions);
    //   • klucz generowany przez bazę + kod jako zwykła kolumna z unique (najprostsze).

    // =================================================================================================
    // 4. Odczyt: findById, findAll, existsById, count, getReferenceById
    // =================================================================================================
    //
    //   findById(id)          → Optional<Product>; brak wiersza = Optional.empty(), NIE null i NIE wyjątek
    //   findAll()             → List<Product> (z ListCrudRepository); findAllById(List.of(1L, 2L))
    //   existsById(id)        → select count(*) from products p1_0 where p1_0.id=?
    //   count()               → select count(*) from products p1_0
    //   getReferenceById(id)  → pośrednik (klasa Product$HibernateProxy) BEZ zapytania; SELECT dopiero przy
    //                           pierwszym getterze, a gdy wiersza brak — EntityNotFoundException: No row with the
    //                           given identifier exists for entity [...Product with id '12345']
    //   findById(null)        → InvalidDataAccessApiUsageException: The given id must not be null
    // Sprawdzone testem (sekcja 4).
    //
    // Dwa zachowania kontekstu utrwalania, które zaskakują:
    //   • findById zaraz po save (bez flush) NIE wysyła SELECT — zwraca obiekt z kontekstu (Jpa01Entities, sekcja 6);
    //   • count() (zapytanie JPQL) najpierw WYPYCHA oczekujące INSERT-y (flush automatyczny — FlushMode.AUTO),
    //     żeby policzyć także niezapisane jeszcze produkty. Sprawdzone testem (sekcja 4).
    //
    // DOBRA PRAKTYKA: findById(id).orElseThrow(() -> new ...("Brak produktu o id " + id)) zamiast .get().
    //   Dlaczego? Własny wyjątek z czytelnym komunikatem zamiast NoSuchElementException: No value present.
    //   getReferenceById używaj tylko wtedy, gdy potrzebujesz samego odwołania (np. ustawić relację w s09)
    //   i masz pewność, że wiersz istnieje.

    // =================================================================================================
    // 5. Usuwanie: deleteById, delete, deleteAllInBatch
    // =================================================================================================
    //
    //   deleteById(id)       → najpierw SELECT (findById), potem "delete from products where id=?"
    //   deleteById(999)      → tylko SELECT, i NIC — żadnego wyjątku, gdy wiersza nie ma
    //   deleteAllInBatch()   → jedno "delete from products p1_0", bez wczytywania encji
    // Sprawdzone testem (sekcja 5).
    // Starsze tutoriale (Spring Data 2.x, Boot 2): deleteById nieistniejącego id rzucało
    // EmptyResultDataAccessException. Od Spring Data 3.0 (Boot 3) brak wiersza jest po cichu ignorowany.
    //
    // PUŁAPKA: deleteAllInBatch omija kontekst utrwalania — encje wczytane wcześniej w tej transakcji dalej „żyją”
    //   w pamięci, choć w bazie ich już nie ma. Dlaczego? To jedno polecenie SQL, a nie remove() na każdej encji.
    //   Ten sam problem wraca przy @Modifying (Jpa04CustomQueries, clearAutomatically).

    // =================================================================================================
    // 6. @DataJpaTest i TestEntityManager — jak testować repozytoria
    // =================================================================================================
    //
    // @DataJpaTest (Boot 4: pakiet org.springframework.boot.data.jpa.test.autoconfigure, starter
    // spring-boot-starter-data-jpa-test):
    //   • buduje tylko warstwę JPA: DataSource, Hibernate, repozytoria, Flyway/Liquibase (bez kontrolerów i serwisów);
    //   • podmienia bazę na wbudowaną o losowej nazwie (@AutoConfigureTestDatabase, domyślnie replace = NON_TEST);
    //   • każdy test biegnie w transakcji, którą Spring WYCOFUJE po teście (TestTransaction.isFlaggedForRollback()
    //     == true) — dane jednego testu nie przeciekają do drugiego;
    //   • włącza show-sql (linie "Hibernate: ...").
    // TestEntityManager (pakiet org.springframework.boot.jpa.test.autoconfigure) — pomocnik do przygotowania danych
    // OBOK testowanego repozytorium: persist, persistAndFlush, persistFlushFind, find, flush, clear, getId.
    //
    // PUŁAPKA: test bez flush() i clear() nie sprawdza bazy. repository.save(p) + repository.findById(id) zwraca
    //   obiekt z kontekstu utrwalania — bez żadnego SQL. Błąd w mapowaniu (np. sku dłuższe niż 20 znaków) wyszedłby
    //   dopiero przy flush, a transakcja testu jest na końcu WYCOFYWANA bez flush — więc test przechodzi, choć
    //   w aplikacji zapis by padł. Test sekcji 6: save za długiego sku i findById — bez błędu; flush — DataException.
    //   Dlatego: zapisz → flush() → clear() → odczytaj.
    //
    // Wyjątki przez repozytorium są PRZETŁUMACZONE: powtórzone sku przy saveAndFlush daje
    // org.springframework.dao.DataIntegrityViolationException (przyczyna: Hibernate ConstraintViolationException).
    // W Jpa01Entities ten sam błąd z gołego EntityManagera był „surowym” wyjątkiem Hibernate'a.
    // Sprawdzone testem (sekcja 6).

    // =================================================================================================
    // 7. Repozytorium tylko do odczytu — goły Repository z wybranymi metodami
    // =================================================================================================
    //
    //   interface ProductCatalog extends Repository<Product, Long> {
    //       Optional<Product> findById(Long id);   List<Product> findAll();   long count();
    //   }
    // Metody mają te same sygnatury co w CrudRepository, więc Spring Data podpina gotową implementację
    // SimpleJpaRepository. Serwis, który dostaje ProductCatalog, nie ma jak wywołać save ani delete.
    // Sprawdzone testem (sekcja 7).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • {@code interface XRepository extends JpaRepository<X, Long> {}} — implementację tworzy Spring Data (proxy →
     *     SimpleJpaRepository). Szukane w pakiecie klasy @SpringBootApplication.
     *   • save: id == null → persist (zwraca ten sam obiekt); id != null → merge (SELECT, zwraca KOPIĘ). Pracuj na wyniku.
     *   • Ręcznie nadane id = merge przy każdym save (SELECT + INSERT). Lekarstwo: Persistable, @Version, klucz z bazy.
     *   • findById → Optional; existsById/count → select count(*); getReferenceById → proxy, wyjątek przy braku wiersza.
     *   • deleteById: SELECT + DELETE, brak wiersza = nic (od Spring Data 3); deleteAllInBatch: jedno DELETE.
     *   • Zapytanie (count, findAll) wypycha oczekujące zmiany (flush automatyczny).
     *   • @DataJpaTest: tylko JPA, baza testowa, transakcja wycofywana po teście, show-sql. TestEntityManager do danych.
     *     W teście: zapisz → flush() → clear() → odczytaj.
     *   • Repozytorium przekłada wyjątki na DataAccessException (DataIntegrityViolationException).
     *
     * PYTANIA KONTROLNE:
     *   1. Skąd bierze się implementacja interfejsu ProductRepository, skoro nikt jej nie napisał?
     *   2. Co się stanie? {@code Product p = repository.findById(1L).orElseThrow(); entityManager.clear();
     *      p.setStock(0); repository.save(p); p.setName("Nowa nazwa"); entityManager.flush();}
     *      Która zmiana trafi do bazy?
     *   3. Dlaczego save kodu rabatowego "LATO10" wysyła SELECT, choć kodu jeszcze nie ma w bazie? Jak to naprawić?
     *   4. Co się stanie? Wywołujesz repository.deleteById(999L), a produktu 999 nie ma w bazie.
     *   5. ZNAJDŹ BŁĄD: test {@code repository.save(p); assertThat(repository.findById(p.getId())).isPresent();}
     *      „sprawdza zapis do bazy”. Co naprawdę sprawdza i jak go poprawić?
     *   6. Czym różni się findById od getReferenceById? Kiedy każda z nich wysyła SELECT?
     *   7. Dlaczego dane zapisane w jednym teście @DataJpaTest nie są widoczne w drugim?
     *   8. Jaki wyjątek dostaniesz przy powtórzonym sku przez repozytorium, a jaki przez goły EntityManager? Skąd różnica?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jpa02RepositoriesExercisesTest (uruchamiasz sam ▶; @Tag("cwiczenie"))
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): przyjęcie dostawy. Zwiększ stan magazynowy produktu o podanym id o {@code quantity}
     * i zwróć nowy stan. Gdy produktu nie ma — rzuć {@code IllegalArgumentException} z komunikatem
     * {@code "Brak produktu o id " + id}. Podpowiedź: findById(...).orElseThrow(...), setter, save (albo nic —
     * w transakcji testu obiekt jest zarządzany; save jest potrzebny, gdy wywołujesz metodę poza transakcją).
     */
    static int exercise1(ProductRepository repository, Long id, int quantity) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): usuń wszystkie produkty z zerowym stanem magazynowym (stock == 0) i zwróć, ile ich było.
     * Użyj wyłącznie metod z JpaRepository (zapytania z nazw metod poznasz w Jpa03QueryMethods).
     * Podpowiedź: findAll(), filtr strumienia, deleteAll(lista).
     */
    static int exercise2(ProductRepository repository) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // ĆWICZENIE 3 (trudniejsze): klasa GiftCard (ten pakiet) — zaimplementuj Persistable<String>, żeby save nowej
    //   karty wysyłał TYLKO INSERT (bez SELECT), a save karty wczytanej z bazy i odłączonej — UPDATE (merge).
    //   Podpowiedź: pole @Transient boolean isNew = true; isNew() je zwraca; metoda z @PostPersist i @PostLoad
    //   ustawia false. Test liczy linie "Hibernate: select" w wyjściu. Uwaga: przy Persistable Spring Data pyta
    //   isNew() ZAMIAST sprawdzać id — źle napisane isNew() (zawsze true) skończy się błędem przy save odłączonej karty.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Spring Data przy starcie znajduje interfejsy rozszerzające Repository (w pakiecie klasy @SpringBootApplication),
     *      tworzy dla każdego proxy JDK i kieruje wywołania metod CRUD do SimpleJpaRepository, która używa EntityManagera.
     *   2. Tylko stock = 0. Po clear obiekt p jest odłączony, więc save robi merge: kopiuje stan p (stock 0) do NOWEGO
     *      obiektu zarządzanego i zwraca go. Kod ignoruje wynik, a setName zmienia odłączone p — nikt tego nie śledzi.
     *      Przy flush idzie UPDATE ze starą nazwą i stock = 0. Sprawdzone testem (sekcja 2).
     *   3. Klucz nadaje sklep, więc id nie jest null i SimpleJpaRepository.isNew zwraca false → merge → merge musi
     *      sprawdzić, czy wiersz istnieje (SELECT), a potem wstawia (INSERT). Naprawa: Persistable z własnym isNew(),
     *      pole @Version typu Long albo klucz generowany przez bazę. Sprawdzone testem (sekcja 3).
     *   4. Nic złego: deleteById robi findById (SELECT), a pusty Optional po prostu ignoruje — bez wyjątku
     *      (od Spring Data 3.0; w 2.x było EmptyResultDataAccessException). Sprawdzone testem (sekcja 5).
     *   5. Sprawdza tylko kontekst utrwalania: findById zwraca obiekt z pamięci, bez SELECT, a INSERT nawet nie poszedł.
     *      Poprawka: save → flush() → clear() → findById (albo TestEntityManager.persistFlushFind) — wtedy odczyt
     *      naprawdę idzie do bazy, a błędy mapowania ujawniają się w teście. Sprawdzone testem (sekcje 4 i 6).
     *   6. findById od razu wykonuje SELECT (chyba że encja jest już w kontekście) i zwraca Optional. getReferenceById
     *      zwraca pośrednika bez SELECT; zapytanie idzie przy pierwszym getterze, a brak wiersza kończy się
     *      EntityNotFoundException. Sprawdzone testem (sekcja 4).
     *   7. Każdy test @DataJpaTest działa w transakcji, którą Spring wycofuje po teście (rollback), więc nic nie zostaje
     *      w bazie. Sprawdzone testem (sekcja 6).
     *   8. Przez repozytorium: DataIntegrityViolationException (Spring), z przyczyną Hibernate ConstraintViolationException —
     *      Spring Data dokłada do proxy każdego repozytorium tłumaczenie wyjątków na DataAccessException.
     *      Przez goły EntityManager wstrzyknięty do testu nikt nie tłumaczy, więc leci
     *      org.hibernate.exception.ConstraintViolationException (Jpa01Entities, sekcja 3). Sprawdzone testem (sekcja 6).
     */
    // </editor-fold>
}
