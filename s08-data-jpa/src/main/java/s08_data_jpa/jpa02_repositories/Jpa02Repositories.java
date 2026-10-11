package s08_data_jpa.jpa02_repositories;

/**
 * <pre>
 * TEMAT: Repozytoria Spring Data JPA — CRUD bez pisania implementacji
 *        (repository = repozytorium, obiekt do zapisu i odczytu encji jednego typu; Spring Data = projekt Springa,
 *         który generuje repozytoria; CRUD = create, read, update, delete — utwórz, czytaj, zmień, usuń)
 *
 * W SKRÓCIE:
 *   W lekcji Jpa01Entities wołaliśmy EntityManager (menedżer encji) ręcznie. Spring Data idzie dalej: piszesz TYLKO
 *   interfejs {@code interface CustomerRepository extends JpaRepository<Customer, Long>}, a Spring przy starcie tworzy
 *   obiekt, który go implementuje (SimpleJpaRepository opakowany w proxy — pośrednika). Dostajesz od razu save,
 *   findById, findAll, count, deleteById i kilkanaście innych metod. Testujesz to wycinkiem @DataJpaTest.
 *
 * ANALOGIA: magazynier w hurtowni.
 *   Nie musisz wiedzieć, na której półce leży towar i jak działa wózek widłowy (EntityManager, SQL). Podajesz
 *   magazynierowi kartkę z numerem (findById) albo paczkę do odłożenia (save). Magazynier jest „z agencji” — nie
 *   zatrudniasz go sam, agencja (Spring Data) przysyła gotowego, przeszkolonego według Twojego opisu stanowiska
 *   (interfejsu).
 *
 * JAK TO DZIAŁA:
 *   start aplikacji:
 *     1. DataJpaRepositoriesAutoConfiguration włącza skanowanie repozytoriów w pakiecie klasy @SpringBootApplication
 *     2. dla każdego interfejsu rozszerzającego Repository Spring Data tworzy bean (proxy JDK) — w środku
 *        SimpleJpaRepository, który woła EntityManager
 *     3. metody SimpleJpaRepository są @Transactional (zapisy — transakcja do zapisu, odczyty — readOnly)
 *   customers.save(customer):
 *     id == null (encja NOWA)   → em.persist(customer), zwraca TEN SAM obiekt
 *     id != null (NIE nowa)     → em.merge(customer), zwraca ZARZĄDZANĄ KOPIĘ
 *
 * SŁÓWKA:
 *   repository = repozytorium; save = zapisz; find by id = znajdź po identyfikatorze; find all = znajdź wszystkie;
 *   exists = istnieje; delete = usuń; count = policz; flush = wypchnij zmiany do bazy; merge = scal; persist = utrwal;
 *   reference = odwołanie (tu: pośrednik bez danych); proxy = pośrednik; slice = wycinek (kontekstu w testach);
 *   rollback = wycofanie transakcji; data integrity violation = naruszenie integralności danych.
 *
 * ZOBACZ TEŻ: s08_data_jpa/Jpa01Entities (encje i EntityManager), s07_data_jdbc/Jdbc05Repository (repozytorium pisane
 *             ręcznie na JdbcClient), t11_generics/Generics07Repository (generyczne repozytorium w czystej Javie),
 *             s08_data_jpa/Jpa03QueryMethods (własne metody wyszukiwania).
 * </pre>
 */
public final class Jpa02Repositories {

    private Jpa02Repositories() {
    }

    // =================================================================================================
    // 1. RODZINA INTERFEJSÓW — od Repository do JpaRepository
    // =================================================================================================
    //
    //   Repository{@code <T, ID>}                      — „znacznik”: pusty interfejs, po nim Spring Data rozpoznaje repozytorium
    //    ├─ CrudRepository                     — save, findById, findAll (Iterable!), count, deleteById, existsById
    //    │   └─ ListCrudRepository             — to samo, ale findAll/saveAll zwracają List (od Spring Data 3)
    //    ├─ PagingAndSortingRepository         — findAll(Sort), findAll(Pageable) — lekcja Jpa05PagingSorting
    //    │   └─ ListPagingAndSortingRepository — findAll(Sort) zwraca List
    //    └─ JpaRepository                      — wszystko powyżej + JPA: flush, saveAndFlush, deleteAllInBatch,
    //                                            getReferenceById, findAll(Example)
    //
    // CustomerRepository rozszerza JpaRepository (pełen zestaw), ProductRepository — tylko ListCrudRepository.
    // Test sekcji 1 pokazuje też, CZYM jest bean repozytorium: pośrednikiem JDK (dynamic proxy), za którym stoi
    // SimpleJpaRepository. Pośrednik dokłada transakcje, tłumaczenie wyjątków i obsługę Twoich metod (Jpa03, Jpa04).
    //
    // PUŁAPKA: od Spring Data 3 PagingAndSortingRepository NIE rozszerza już CrudRepository. Dlaczego to ważne? Stare
    //   tutoriale piszą {@code extends PagingAndSortingRepository} i mają save/findById „za darmo” — dziś trzeba dopisać też
    //   CrudRepository (albo po prostu użyć JpaRepository, które ma oba).
    //
    // DOBRA PRAKTYKA: dla JPA najczęściej JpaRepository (albo ListCrudRepository, gdy chcesz mniej metod). Dlaczego?
    //   Metody zwracają List (a nie Iterable), są flush i saveAndFlush przydatne w testach. Repozytorium NIE potrzebuje
    //   adnotacji @Repository — Spring Data znajduje je po typie.

    // =================================================================================================
    // 2. CRUD — gotowe metody
    // =================================================================================================
    //
    //   customers.save(c) / saveAll(lista)       — zapis (INSERT albo UPDATE — sekcja 3)
    //   customers.findById(id)                   — Optional{@code <Customer>} (sekcja 4)
    //   customers.findAll() / findAllById(ids)   — lista
    //   customers.count() / existsById(id)       — select count(*) ...
    //   customers.deleteById(id) / delete(c) / deleteAll()
    //
    // Każda metoda modyfikująca ma własną transakcję (@Transactional w SimpleJpaRepository). Jeśli wołasz ją z metody,
    // która JUŻ ma transakcję (serwis z @Transactional albo test @DataJpaTest), repozytorium do niej dołącza.
    //
    // PUŁAPKA: findAll() bez warunków na dużej tabeli. Dlaczego? Wczytuje WSZYSTKIE wiersze do pamięci jako encje
    //   zarządzane — przy milionie zamówień aplikacja padnie. Do dużych zbiorów: stronicowanie (Jpa05PagingSorting).

    // =================================================================================================
    // 3. save — persist czy merge? (Customer z generowanym id kontra Product z ręcznym id)
    // =================================================================================================
    //
    // SimpleJpaRepository.save:   if (entityInformation.isNew(entity)) em.persist(entity) else em.merge(entity)
    // isNew: domyślnie „id == null” (dla id typu prostego, np. long — „id == 0”); jeśli encja ma pole @Version —
    //   „version == null”.
    //
    //   Customer nowy (id == null)      → persist → zwraca TEN SAM obiekt, który ma już id
    //   Customer odłączony (id != null) → merge → SELECT, skopiowanie stanu → zwraca INNY obiekt (zarządzany)
    //
    // Testy sekcji 3 sprawdzają obie ścieżki. Wniosek: ZAWSZE używaj wyniku save:
    //   customer = customers.save(customer);      // dobrze — dalej pracujesz na obiekcie zarządzanym
    //
    // PUŁAPKA: encja z ręcznie nadanym id (Product z kluczem SKU, bez @GeneratedValue) jest dla Spring Data ZAWSZE
    //   „nie nowa”. Dlaczego to problem? Każde save nowego produktu to merge: najpierw SELECT (czy taki wiersz jest?),
    //   potem INSERT — dwa zapytania zamiast jednego, a zwrócony obiekt to kopia. W logu (sprawdzone uruchomieniem):
    //     select p1_0.sku,p1_0.name,p1_0.price from products p1_0 where p1_0.sku=?
    //     insert into products (name,price,sku) values (?,?,?)
    //   Rozwiązania: generowane id + osobna kolumna unikalna na SKU, pole @Version, albo interfejs Persistable
    //   (persistable = dający się utrwalić) z własną metodą isNew().
    //
    // DOBRA PRAKTYKA: nie wołaj save na encji zarządzanej „dla pewności”. Dlaczego? W transakcji zmiana pola i tak
    //   trafi do bazy (dirty checking — Jpa01Entities, sekcja 8). save jest potrzebne dla NOWYCH i ODŁĄCZONYCH encji.

    // =================================================================================================
    // 4. findById → Optional; getReferenceById → pośrednik
    // =================================================================================================
    //
    //   Optional{@code <Customer>} found = customers.findById(id);   // SELECT od razu; brak wiersza → Optional.empty()
    //   String name = customers.findById(id).map(Customer::getName).orElse("nieznany");
    //   Customer c = customers.findById(id).orElseThrow();       // NoSuchElementException, gdy brak
    //
    //   Customer ref = customers.getReferenceById(id);   // BEZ SELECT — pusty pośrednik (proxy) z samym id
    //   ref.getName();                                    // dopiero teraz SELECT; brak wiersza → EntityNotFoundException
    //
    // getReferenceById przydaje się, gdy potrzebujesz tylko „wskaźnika” do encji, np. ustawić klienta w nowym
    // zamówieniu bez czytania klienta z bazy (relacje — kolejny dział).
    //
    // PUŁAPKA: getReferenceById dla nieistniejącego id NIE rzuca wyjątku od razu. Dlaczego? Hibernate tworzy pośrednika
    //   bez pytania bazy; wyjątek (jakarta.persistence.EntityNotFoundException: No row with the given identifier
    //   exists) leci dopiero przy pierwszym dotknięciu pola — często daleko od miejsca błędu. Test sekcji 4.
    //   Stare nazwy tej metody: getOne i getById — obie przestarzałe (deprecated).
    //
    // DOBRA PRAKTYKA: w serwisie zamieniaj pusty Optional na czytelny błąd domenowy:
    //   customers.findById(id).orElseThrow(() -> new CustomerNotFoundException(id)).
    //   Dlaczego? Komunikat mówi, CZEGO brakuje, a kontroler może go zamienić na odpowiedź 404.

    // =================================================================================================
    // 5. Usuwanie — deleteById na brakującym id
    // =================================================================================================
    //
    // SimpleJpaRepository.deleteById(id) w Spring Data JPA 4.1 (pociąg wydań 2026.0) robi:
    //   findById(id).ifPresent(this::delete)        // sprawdzone w kodzie (javap) i testem
    // Czyli: SELECT, a jeśli wiersz jest — DELETE (przy flush). Brak wiersza → NIC, żadnego wyjątku.
    //
    // PUŁAPKA: w Spring Data 2.x (Spring Boot 2) deleteById na brakującym id rzucał EmptyResultDataAccessException.
    //   Dlaczego to ważne? W starych projektach i tutorialach zobaczysz try/catch na ten wyjątek — dziś jest martwym
    //   kodem. Chcesz zgłosić „nie ma takiego klienta”? Sprawdź sam: if (!customers.existsById(id)) throw ...
    //
    // DOBRA PRAKTYKA: do masowego usuwania nie używaj deleteAll() na dużych tabelach. Dlaczego? deleteAll() najpierw
    //   wczytuje wszystkie encje i usuwa je po jednej (osobny DELETE dla każdej). Jest deleteAllInBatch() — jeden DELETE,
    //   ale z pominięciem kontekstu utrwalania (encje w pamięci nie wiedzą, że zniknęły).

    // =================================================================================================
    // 6. @DataJpaTest — test warstwy danych
    // =================================================================================================
    //
    // @DataJpaTest                                       // org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
    // @ActiveProfiles("jpa02")                           // w Boot 4 nowy pakiet (Boot 3: ...test.autoconfigure.orm.jpa)
    // class Jpa02RepositoriesTest { @Autowired CustomerRepository customers; ... }
    //
    // Co robi @DataJpaTest (sprawdzone w pliku adnotacji i testami):
    //   • kontekst tylko z JPA: encje, repozytoria, Hibernate, DataSource, TestEntityManager — bez serwisów, kontrolerów
    //   • @Transactional na klasie testu → KAŻDY test w transakcji wycofywanej na końcu (rollback)
    //   • @AutoConfigureTestDatabase(replace = NON_TEST) → DataSource podmieniony na wbudowaną bazę z losową nazwą
    //   • spring.jpa.show-sql=true (atrybut showSql adnotacji)
    // Test sekcji 6 pyta TestTransaction: transakcja aktywna i oznaczona do wycofania — dlatego testy nie widzą
    // nawzajem swoich danych i kolejność ich uruchamiania nie ma znaczenia.
    //
    // Tłumaczenie wyjątków: repozytorium zamienia błędy Hibernate/JDBC na wyjątki Springa (DataAccessException).
    // Duplikat e-maila: Hibernate rzuca ConstraintViolationException, a z repozytorium wychodzi
    // DataIntegrityViolationException — kod nie zależy od tego, czy pod spodem jest Hibernate, czy JDBC.
    //
    // PUŁAPKA: w @DataJpaTest zmiany często NIE docierają do bazy, bo transakcja się nie kończy (rollback, a nie commit),
    //   więc nie ma flush. Dlaczego to groźne? Test „przechodzi”, chociaż w prawdziwej aplikacji INSERT złamałby
    //   ograniczenie UNIQUE. Dlatego w teście duplikatu jest saveAndFlush — flush wymusza wysłanie SQL do bazy.
    //
    // DOBRA PRAKTYKA: w testach repozytoriów: dane przygotuj w teście (albo @BeforeEach), flush + clear przed
    //   sprawdzaniem odczytu. Dlaczego? Bez clear findById zwróci obiekt z „notesu” Hibernate, a nie z bazy.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • {@code interface XRepository extends JpaRepository<X, Long>} — Spring Data tworzy implementację (proxy + SimpleJpaRepository).
     *   • Rodzina: Repository → CrudRepository → ListCrudRepository; PagingAndSortingRepository osobno; JpaRepository = wszystko.
     *   • save: id == null → persist (ten sam obiekt); id != null → merge (kopia!). Zawsze używaj wyniku save.
     *   • Ręczne id = zawsze merge (SELECT + INSERT) → rozważ Persistable albo @Version.
     *   • findById → Optional; getReferenceById → pośrednik, wyjątek dopiero przy dostępie do pola.
     *   • deleteById brakującego id → cisza (od Spring Data 3).
     *   • @DataJpaTest: wycinek JPA, transakcja z rollbackiem, baza wbudowana, show-sql; flush przed sprawdzaniem.
     *   • Błędy bazy z repozytorium → DataAccessException (np. DataIntegrityViolationException).
     *
     * PYTANIA KONTROLNE:
     *   1. Kto pisze implementację interfejsu CustomerRepository i czym jest bean, który dostajesz przez @Autowired?
     *   2. Co się stanie? {@code Customer c = new Customer(...); customers.save(c);} — czy c ma potem id? A jeśli c było
     *      odłączone (miało id) — czy zwrócony obiekt to c?
     *   3. ZNAJDŹ BŁĄD: {@code customers.save(detached); detached.setName("X");} w transakcji — dlaczego nowa nazwa nie
     *      trafi do bazy?
     *   4. Co się stanie? customers.deleteById(-1L), gdy takiego klienta nie ma (Spring Boot 4).
     *   5. Czym różni się findById od getReferenceById? Kiedy getReferenceById rzuci wyjątek?
     *   6. Dlaczego encja z ręcznie nadanym id (bez @GeneratedValue) powoduje dodatkowy SELECT przy save?
     *   7. Co daje @DataJpaTest i dlaczego testy w nim nie widzą nawzajem swoich danych?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jpa02RepositoriesExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zarejestruj nowego klienta (email, name) przez repozytorium i zwróć jego id.
     * Podpowiedź: customers.save(new Customer(...)).getId().
     */
    static Long exercise1(CustomerRepository customers, String email, String name) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć imię klienta o podanym id albo tekst "nieznany klient", gdy go nie ma.
     * Bez if i bez get() na Optional. Podpowiedź: findById(id).map(...).orElse(...).
     */
    static String exercise2(CustomerRepository customers, Long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): usuń wszystkich NIEAKTYWNYCH klientów (active == false) i zwróć, ilu usunięto.
     * Podpowiedź: findAll() + stream().filter(...).toList(), potem deleteAll(lista) — metoda z CrudRepository
     * przyjmująca kolekcję encji.
     */
    static int exercise3(CustomerRepository customers) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Spring Data przy starcie: tworzy SimpleJpaRepository i owija go pośrednikiem JDK (proxy), który implementuje
     *      nasz interfejs. Sprawdzone testem repositoryIsProxyOverSimpleJpaRepository.
     *   2. Nowy obiekt (id == null): persist — save zwraca TEN SAM obiekt i c ma id. Odłączony: merge — zwraca INNĄ,
     *      zarządzaną kopię, a c zostaje odłączone. Sprawdzone testami saveNewEntityPersists i saveDetachedEntityMerges.
     *   3. save odłączonej encji robi merge i zwraca kopię — zarządzana jest KOPIA, a nie detached. Zmiana na detached
     *      po save nie jest śledzona. Poprawnie: {@code detached.setName("X"); Customer managed = customers.save(detached);}
     *      albo dalsze zmiany na zwróconym obiekcie.
     *   4. Nic — SimpleJpaRepository robi findById(id).ifPresent(this::delete), więc brak wiersza = brak akcji i brak
     *      wyjątku (w Spring Data 2.x było EmptyResultDataAccessException). Sprawdzone testem
     *      deleteByIdOnMissingIdIsSilent.
     *   5. findById od razu robi SELECT i zwraca Optional. getReferenceById zwraca pośrednika bez SELECT; wyjątek
     *      EntityNotFoundException leci przy pierwszym dostępie do pola, gdy wiersza nie ma. Sprawdzone testem
     *      getReferenceByIdFailsLazily.
     *   6. Spring Data uznaje encję za nową, gdy id == null. Ręczne id jest ustawione już w konstruktorze, więc save
     *      robi merge, a merge musi sprawdzić SELECT-em, czy wiersz istnieje. Sprawdzone uruchomieniem (log SQL testu
     *      assignedIdAlwaysMerges).
     *   7. Wycinek kontekstu tylko z JPA, bazę wbudowaną, TestEntityManager i show-sql; każdy test biegnie
     *      w transakcji wycofywanej na końcu, więc dane jednego testu znikają przed następnym. Sprawdzone testem
     *      testRunsInRolledBackTransaction.
     */
    // </editor-fold>
}
