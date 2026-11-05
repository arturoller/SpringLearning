package s08_data_jpa.jpa02_repositories;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * <pre>
 * TEMAT: Repozytoria Spring Data JPA — JpaRepository, save (persist kontra merge), findById, transakcje
 *        (repository = repozytorium; Spring Data = rodzina bibliotek Springa do baz danych; proxy = pośrednik;
 *         persist = utrwal nowy; merge = scal odłączony; reference = odwołanie (uchwyt bez danych))
 *
 * W SKRÓCIE:
 *   W s07_data_jdbc/Jdbc05Repository repozytorium pisałeś sam: interfejs + klasa z SQL-em. Spring Data JPA robi
 *   to za Ciebie: wystarczy {@code interface ProductRepository extends JpaRepository<Product, Long>}, a przy starcie
 *   powstaje bean z gotowymi metodami save, findById, findAll, count, deleteById… Pod spodem działa EntityManager
 *   z s08_data_jpa/Jpa01Entities, więc obowiązują te same zasady: kontekst utrwalania, dirty checking, stany encji.
 *   Najważniejsze haczyki: save to raz persist, raz merge; encja jest zarządzana tylko w transakcji.
 *
 * ANALOGIA: gotowy formularz w urzędzie zamiast pisania podania od zera.
 *   W s07 pisałeś każde podanie własnoręcznie (SQL). Teraz bierzesz druk „wniosek o…” (JpaRepository) — rubryki
 *   są już wydrukowane, wpisujesz tylko, czego dotyczy (Product, Long). Urzędnik (SimpleJpaRepository) wie, co
 *   z drukiem zrobić. Ale druk ma swoje zasady: wpiszesz numer sprawy w rubrykę „nowa sprawa” — urzędnik pójdzie
 *   szukać starej sprawy o tym numerze (merge), zamiast założyć nową.
 *
 * JAK TO DZIAŁA:
 *   1. Autokonfiguracja DataJpaRepositoriesAutoConfiguration (jak {@code @EnableJpaRepositories}) skanuje pakiet
 *      klasy startowej i szuka interfejsów rozszerzających {@code Repository}.
 *   2. Dla każdego tworzy fabrykę JpaRepositoryFactoryBean, a ta — proxy JDK implementujące Twój interfejs.
 *      Wywołanie metody z JpaRepository proxy przekazuje do obiektu SimpleJpaRepository (gotowa implementacja na
 *      EntityManagerze); metody z nazw (findByName) proxy zamienia na zapytania (s08_data_jpa/Jpa03QueryMethods).
 *   3. Proxy ma też porady (advice) AOP: transakcje (SimpleJpaRepository ma {@code @Transactional}) i tłumaczenie
 *      wyjątków JPA/Hibernate na DataAccessException (PersistenceExceptionTranslationInterceptor).
 *   4. Schemat robi Flyway (db/migration/jpa02), a Hibernate z {@code ddl-auto=validate} przy starcie sprawdza, czy
 *      encja pasuje do tabeli — nie zgadza się kolumna = aplikacja nie startuje (s08_data_jpa/Jpa06FlywayWithJpa).
 *
 * SŁÓWKA:
 *   CRUD (Create, Read, Update, Delete) = tworzenie, odczyt, zmiana, usuwanie; isNew = „czy nowa”; Optional = pudełko
 *   na wartość, które może być puste; read-only = tylko do odczytu; slice = wycinek (test tylko jednej warstwy);
 *   lazy = leniwie (dopiero przy pierwszym użyciu); flush = wypchnięcie zmian do bazy.
 *
 * ZOBACZ TEŻ: s07_data_jdbc/Jdbc05Repository (repozytorium pisane ręcznie na JdbcClient),
 *             s08_data_jpa/Jpa01Entities (EntityManager, stany encji, dirty checking — fundament tej lekcji),
 *             s08_data_jpa/Jpa03QueryMethods (zapytania z nazw metod w tym samym interfejsie),
 *             s08_data_jpa/Jpa06FlywayWithJpa (Flyway + ddl-auto=validate),
 *             s10_transactions/Tx01Transactional (transakcje od podstaw).
 * </pre>
 */
public final class Jpa02Repositories {

    private Jpa02Repositories() {
    }

    // =================================================================================================
    // 1. JpaRepository — implementację generuje Spring (proxy + SimpleJpaRepository)
    // =================================================================================================
    //
    // Cały kod repozytorium (ProductRepository.java):
    //   public interface ProductRepository extends JpaRepository<Product, Long> { }
    //
    // Test: bean to proxy JDK (Proxy.isProxyClass), a jego cel (target) to SimpleJpaRepository. Nigdzie nie piszesz
    // „new SimpleJpaRepository” — robi to Spring przy starcie dla każdego interfejsu repozytorium w pakiecie lekcji.
    //
    // Hierarchia interfejsów (od najmniejszego):
    //   interfejs                          co dodaje                                            findAll zwraca
    //   Repository<T, ID>                  nic (znacznik) — sam deklarujesz metody (Jpa03)      —
    //   CrudRepository<T, ID>              save, saveAll, findById, existsById, findAll,        Iterable
    //                                      count, deleteById, delete, deleteAll
    //   ListCrudRepository<T, ID>          to samo, ale zwraca List (Spring Data 3+)            List
    //   ListPagingAndSortingRepository     findAll(Sort), findAll(Pageable) (Jpa05)              List / Page
    //   JpaRepository<T, ID>               wszystko powyżej + flush, saveAndFlush,              List
    //                                      getReferenceById, deleteAllInBatch, findAll(Example)
    //
    // UWAGA na stare tutoriale: przed Spring Data 3 (Boot 2) CrudRepository.findAll zwracał tylko Iterable, a
    // getReferenceById nazywał się getOne/getById (obie nazwy są przestarzałe — deprecated).
    //
    // DOBRA PRAKTYKA: rozszerzaj tyle, ile potrzebujesz. Dlaczego? JpaRepository daje też deleteAll() czy
    //   deleteAllInBatch() — jeśli aplikacja nigdy nie powinna kasować wszystkiego, ListCrudRepository albo własny
    //   interfejs z wybranymi metodami (rozszerzający Repository) zamyka tę furtkę. Na naukę: JpaRepository.
    //
    // Izolacja lekcji (test): metamodel lekcji zawiera tylko jej Product — encja Product z Jpa01 (inny pakiet)
    // do niego nie trafia, choć obie leżą w tym samym module.

    // =================================================================================================
    // 2. save — nowa encja → persist, istniejąca → merge; używaj zwróconego obiektu
    // =================================================================================================
    //
    // SimpleJpaRepository.save robi (w uproszczeniu):
    //   if (entityInformation.isNew(entity)) { em.persist(entity); return entity; }
    //   else { return em.merge(entity); }
    //
    // isNew — skąd Spring wie, że encja jest nowa? Kolejno: encja implementuje Persistable → jej metoda isNew();
    //   ma pole @Version typu obiektowego → nowa, gdy wersja == null; w pozostałych przypadkach → nowa, gdy
    //   id == null (dla id typu prostego, np. long: gdy id == 0).
    //
    //   wywołanie                        co robi Hibernate (sprawdzone testem)                 wynik save
    //   save(nowy, id == null)           persist → INSERT (IDENTITY: od razu)                  TEN SAM obiekt z id
    //   save(odłączony, id = 2)          merge → SELECT wiersza 2, kopiowanie pól, UPDATE      INNY obiekt (kopia)
    //   save(nowy, ręcznie id = 777)     merge → SELECT-y szukające 777, wiersza nie ma →      wyjątek
    //                                    ObjectOptimisticLockingFailureException
    //   save(nowy, ręcznie id = 2)       merge → nadpisuje WSZYSTKIE kolumny wiersza 2         „Mysz” staje się „Zeszytem”
    //
    // PUŁAPKA: ręcznie ustawione id przy @GeneratedValue. Dlaczego? Spring uznaje encję za istniejącą i robi merge.
    //   Dla nieistniejącego numeru Hibernate 7 rzuca StaleObjectStateException „Row was already updated or deleted
    //   by another transaction”, który Spring tłumaczy na ObjectOptimisticLockingFailureException (podklasa
    //   OptimisticLockingFailureException — jak konflikt wersji w s07_data_jdbc/Jdbc05Repository). Mylący komunikat,
    //   a przyczyna banalna. Dla istniejącego numeru jest gorzej: brak błędu, cudzy wiersz po cichu nadpisany.
    //   UWAGA: Hibernate starszy niż 6.6 (Boot 2 i wczesne Boot 3) w tej sytuacji robił po cichu INSERT z nowym
    //   numerem — starsze tutoriale pokazują inne zachowanie niż Boot 4.
    //
    // PUŁAPKA: ignorowanie wyniku save. Dlaczego? Przy merge zarządzana jest zwrócona KOPIA (test: isNotSameAs);
    //   dalsze zmiany oryginału nie trafią do bazy (Jpa01, sekcja 4). Pisz zawsze {@code product = repository.save(product);}.
    //
    // DOBRA PRAKTYKA: w kodzie aplikacji zmieniaj istniejące encje przez findById + setter w transakcji serwisu
    //   (sekcja 4), a save wołaj dla NOWYCH encji. Dlaczego? merge kopiuje WSZYSTKIE pola odłączonego obiektu — także
    //   te, których klient wcale nie chciał zmieniać (np. puste pola z formularza nadpiszą dane w bazie).

    // =================================================================================================
    // 3. findById, getReferenceById, existsById, count, deleteById
    // =================================================================================================
    //
    //   metoda                      wynik / zachowanie (sprawdzone testem)
    //   findById(id)                Optional<Product>: wartość albo Optional.empty() (nigdy null, nigdy wyjątek)
    //   getReferenceById(id)        proxy (uchwyt) BEZ zapytania; SELECT przy pierwszym użyciu pola. Brak wiersza →
    //                               jakarta.persistence.EntityNotFoundException dopiero przy tym użyciu
    //   existsById(id)              boolean; jedno zapytanie, encja NIE jest wczytywana
    //   count()                     long; SELECT count(*)
    //   deleteById(id)              wczytuje encję i usuwa ją (DELETE przy flush). Brak wiersza → NIC (bez wyjątku)
    //
    // Po co getReferenceById? Gdy potrzebujesz tylko „wskazać” wiersz, np. ustawić klucz obcy w relacji
    // (order.setCustomer(customerRepository.getReferenceById(7))) — bez zbędnego SELECT. Relacje: s09_jpa_relations/Rel01ManyToOne.
    //
    // UWAGA na stare tutoriale: w Spring Data 2.x deleteById dla nieistniejącego id rzucał
    //   EmptyResultDataAccessException. Od Spring Data 3 (Boot 3) i w 2026.0 — po cichu nic nie robi (test).
    //   Jeśli „brak” ma być błędem, sprawdź sam: existsById albo findById(...).orElseThrow (ćwiczenie 2).
    //
    // PUŁAPKA: getReferenceById zamiast findById „bo szybciej”. Dlaczego? Nie wiesz, czy wiersz istnieje — wyjątek
    //   wyleci później, w innym miejscu kodu (np. przy serializacji do JSON), a poza transakcją dostaniesz
    //   LazyInitializationException (s09_jpa_relations/Rel04LazyLoading). Do odczytu danych — findById.

    // =================================================================================================
    // 4. @Transactional — repozytorium ma własne transakcje, serwis wyznacza granicę operacji
    // =================================================================================================
    //
    // SimpleJpaRepository ma na klasie {@code @Transactional(readOnly = true)}, a metody zapisu (save, deleteById…)
    // nadpisują to zwykłym {@code @Transactional} (test czyta adnotacje). Czyli każde wywołanie repozytorium BEZ
    // transakcji z zewnątrz to osobna, krótka transakcja — i osobny kontekst utrwalania.
    //
    // Skutek (test na pełnej aplikacji, ProductService — trzy metody z identycznym ciałem findById + setPrice):
    //   changePrice — {@code @Transactional}                → cena ZMIENIONA w bazie (dirty checking przy commicie)
    //   changePriceWithoutTransaction — bez adnotacji        → cena BEZ ZMIAN: transakcja skończyła się w findById,
    //                                                          encja jest odłączona, setter zmienia tylko pamięć
    //   changePriceInReadOnlyTransaction — readOnly = true   → cena BEZ ZMIAN: Spring ustawia sesji Hibernate
    //                                                          FlushMode.MANUAL, więc przy commicie nie ma flush
    //
    // Jeśli serwis ma {@code @Transactional}, transakcje repozytorium DOŁĄCZAJĄ do niej (propagacja REQUIRED):
    // jedna transakcja, jeden kontekst utrwalania na całą metodę serwisu. Więcej: s10_transactions/Tx01Transactional.
    //
    // PYTANIE REKRUTACYJNE: „czy po zmianie encji trzeba wołać save?” Odpowiedź: nie, jeśli encja jest zarządzana
    //   (wczytana w tej samej, wciąż otwartej transakcji) — dirty checking zapisze zmianę. Tak, jeśli jest odłączona
    //   (wtedy save = merge) albo nowa (save = persist).
    //
    // PUŁAPKA: {@code @Transactional(readOnly = true)} „dla wydajności” na metodzie, która jednak coś zmienia.
    //   Dlaczego groźne? Zmiana po cichu przepada — bez wyjątku (test). readOnly tylko na metodach czysto odczytowych.
    //
    // DOBRA PRAKTYKA: {@code @Transactional} na metodach SERWISU, który używa repozytorium; save wołaj jawnie tylko
    //   dla nowych encji. Dlaczego? Serwis wie, co jest jedną operacją biznesową; kilka wywołań repozytorium bez
    //   wspólnej transakcji = kilka osobnych commitów i encje odłączone między nimi.

    // =================================================================================================
    // 5. TESTOWANIE — @DataJpaTest, flush/clear, @Import serwisu
    // =================================================================================================
    //
    // {@code @DataJpaTest} (pakiet org.springframework.boot.data.jpa.test.autoconfigure w Boot 4 — w Boot 3 był
    // to org.springframework.boot.test.autoconfigure.orm.jpa) to wycinek: encje, repozytoria Spring Data,
    // EntityManager, TestEntityManager, JdbcClient, Flyway — bez @Service, @Component i weba (test: ObjectProvider
    // serwisu jest pusty). Dodatkowe beany dokładasz przez {@code @Import(ProductService.class)}.
    //
    // Co robi wycinek z bazą (sprawdzone testem): podmienia DataSource na wbudowaną H2 z losową nazwą
    // (spring.datasource.url z profilu jest pomijany), ale Flyway lekcji uruchamia się na niej — historia migracji ma
    // wersje 1 i 2 — a ddl-auto=validate sprawdza encję. Każdy test to jedna transakcja wycofywana na końcu.
    //
    // Konsekwencja „jednej transakcji na test”: serwis w teście dołącza do transakcji testu, więc zmiana nie trafia do
    // bazy, dopóki nie zrobisz flush (test: odczyt JDBC przed i po tem.flush()). A transakcja serwisu i commit — nie do
    // sprawdzenia w @DataJpaTest; do tego służy test pełnej aplikacji (sekcja 4).
    //
    // DOBRA PRAKTYKA: przed asercją na danych z bazy {@code tem.flush(); tem.clear();} (Jpa01, sekcja 3). Dlaczego?
    //   Bez tego findById zwraca obiekt z pamięci kontekstu — nawet gdy UPDATE nigdy by nie przeszedł (np. łamał
    //   ograniczenie bazy, którego błąd wyszedłby dopiero przy flush).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • {@code interface XRepository extends JpaRepository<Encja, TypId>} — implementację (proxy + SimpleJpaRepository)
     *     tworzy Spring. ListCrudRepository/JpaRepository zwracają List.
     *   • save: id == null → persist (ten sam obiekt); id != null → merge (zwraca KOPIĘ) — zawsze używaj wyniku.
     *   • Ręczne id przy @GeneratedValue: merge → ObjectOptimisticLockingFailureException (brak wiersza) albo
     *     nadpisanie cudzego wiersza (wiersz istnieje).
     *   • findById → Optional; getReferenceById → proxy bez SELECT, brak wiersza = EntityNotFoundException przy użyciu.
     *   • existsById — bez wczytywania encji; deleteById nieistniejącego — nic się nie dzieje (Spring Data 3+).
     *   • SimpleJpaRepository: @Transactional(readOnly = true) na klasie, zapis — @Transactional na metodach.
     *   • Serwis @Transactional: findById + setter wystarczy (dirty checking); bez transakcji encja jest odłączona;
     *     readOnly = true — zmiany przepadają.
     *   • @DataJpaTest: wycinek JPA + Flyway, podmieniona baza, rollback po teście; serwis przez @Import;
     *     flush/clear przed asercjami.
     *
     * PYTANIA KONTROLNE:
     *   1. Skąd bierze się implementacja interfejsu ProductRepository, skoro nigdzie jej nie napisałeś?
     *   2. Po czym Spring Data poznaje, czy save ma wywołać persist, czy merge?
     *   3. Co się stanie? {@code product.setId(777L); repository.save(product);} — encja ma @GeneratedValue,
     *      a w tabeli nie ma wiersza 777. Jaki wyjątek poleci?
     *   4. ZNAJDŹ BŁĄD: {@code repository.save(detachedProduct); detachedProduct.setStock(0);} w metodzie
     *      {@code @Transactional} — stan magazynu w bazie się nie zmienia. Dlaczego?
     *   5. Co się stanie? {@code repository.deleteById(999L)}, gdy produktu 999 nie ma.
     *   6. Co się stanie? Metoda serwisu BEZ @Transactional robi {@code repository.findById(2L).orElseThrow()
     *      .setPrice(...)} i nie woła save. Czy cena w bazie się zmieni? A z @Transactional(readOnly = true)?
     *   7. Czym różni się findById od getReferenceById? Kiedy getReferenceById ma sens?
     *   8. Dlaczego w teście @DataJpaTest zmiana zrobiona przez serwis nie jest widoczna przez JdbcClient,
     *      dopóki nie wywołasz flush?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jpa02RepositoriesExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć cenę produktu o danym numerze albo {@code Optional.empty()}, gdy go nie ma.
     * Podpowiedź: sekcja 3 — {@code findById(id).map(...)}; nie używaj {@code get()} bez sprawdzenia.
     */
    static Optional<BigDecimal> exercise1PriceOf(ProductRepository repository, long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): usuń produkt i zwróć {@code true}; gdy produktu nie ma — nic nie usuwaj i zwróć
     * {@code false}.
     * Podpowiedź: sekcja 3 — deleteById po cichu ignoruje brakujące id, więc najpierw {@code existsById}.
     */
    static boolean exercise2DeleteIfExists(ProductRepository repository, long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zapisz wszystkie nowe produkty jednym wywołaniem repozytorium i zwróć ich numery
     * w tej samej kolejności.
     * Podpowiedź: sekcja 1 — {@code saveAll(...)} zwraca {@code List} (ListCrudRepository); potem strumień
     * i {@code map(Product::getId)}.
     */
    static List<Long> exercise3AddAll(ProductRepository repository, List<Product> products) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): utwórz KOPIĘ produktu o numerze {@code id} pod nową nazwą (ta sama kategoria, cena,
     * stan i data utworzenia) i zwróć zapisaną kopię. Oryginał ma zostać bez zmian. Gdy produktu nie ma — rzuć
     * {@code ProductNotFoundException(id)}.
     * Podpowiedź: sekcja 2 — NIE zmieniaj nazwy wczytanej encji i nie ustawiaj jej {@code setId(null)}: wczytana
     * encja jest zarządzana, więc każda jej zmiana trafiłaby do bazy. Utwórz {@code new Product(...)} i zapisz save.
     */
    static Product exercise4Copy(ProductRepository repository, long id, String newName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Spring Data przy starcie znajduje interfejs (skanowanie pakietu klasy startowej) i tworzy proxy JDK, które
     *      wywołania metod CRUD przekazuje do SimpleJpaRepository (gotowa klasa na EntityManagerze). Sprawdzone testem
     *      (sekcja 1: Proxy.isProxyClass, target = SimpleJpaRepository).
     *   2. Metodą isNew: Persistable.isNew(), jeśli encja go implementuje; inaczej pole @Version (null = nowa); inaczej
     *      id (null, a dla typu prostego 0 = nowa). Nowa → persist, nie nowa → merge.
     *   3. Encja nie jest „nowa” (id != null), więc save robi merge; Hibernate szuka wiersza 777 (SELECT), nie znajduje
     *      i rzuca StaleObjectStateException, a Spring zamienia go na ObjectOptimisticLockingFailureException.
     *      Żadnego INSERT. Sprawdzone testem (sekcja 2).
     *   4. save odłączonej encji robi merge i zwraca INNY, zarządzany obiekt; zmiana detachedProduct po save nie jest
     *      śledzona. Poprawnie: {@code Product managed = repository.save(detachedProduct); managed.setStock(0);}.
     *      Sprawdzone testem (sekcja 2: isNotSameAs).
     *   5. Nic — bez wyjątku i bez zmian w bazie (Spring Data 3+; w 2.x było EmptyResultDataAccessException).
     *      Sprawdzone testem (sekcja 3).
     *   6. Bez @Transactional: nie zmieni się — transakcja findById już się skończyła, encja jest odłączona.
     *      Z readOnly = true: też nie — FlushMode.MANUAL, brak flush przy commicie. Zmieni się tylko ze zwykłym
     *      @Transactional. Sprawdzone testem (Jpa02ServiceTransactionTest).
     *   7. findById od razu wykonuje SELECT i zwraca Optional (pusty, gdy brak wiersza). getReferenceById zwraca proxy
     *      bez zapytania; SELECT (i ewentualny EntityNotFoundException) dopiero przy użyciu. Ma sens, gdy potrzebny
     *      jest tylko „wskaźnik” na wiersz, np. do ustawienia klucza obcego w relacji. Sprawdzone testem (sekcja 3).
     *   8. Test @DataJpaTest jest jedną transakcją; serwis do niej dołącza, a UPDATE czeka w kontekście utrwalania
     *      do flush (przed commitem, którego tu nie ma, bo test kończy się rollbackiem). JdbcClient czyta bazę
     *      bezpośrednio, więc widzi starą wartość. Sprawdzone testem (sekcja 5).
     */
    // </editor-fold>
}
