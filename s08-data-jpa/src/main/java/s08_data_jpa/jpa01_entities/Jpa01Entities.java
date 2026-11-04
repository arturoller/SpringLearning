package s08_data_jpa.jpa01_entities;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.List;

/**
 * <pre>
 * TEMAT: Encje JPA i EntityManager — mapowanie klasy na tabelę, kontekst utrwalania, stany encji
 *        (ORM — Object-Relational Mapping = mapowanie obiektowo-relacyjne; entity = encja; persistence context =
 *         kontekst utrwalania; EntityManager = zarządca encji; dirty checking = wykrywanie zmian;
 *         flush = wypchnięcie zmian do bazy)
 *
 * W SKRÓCIE:
 *   W s07 pisałeś SQL ręcznie i sam zamieniałeś wiersze na rekordy (RowMapper). ORM robi to za Ciebie: opisujesz
 *   klasę adnotacjami ({@code @Entity}, {@code @Id}, {@code @Column}), a Hibernate generuje INSERT, SELECT,
 *   UPDATE i DELETE. Obiekty wczytane przez EntityManager są „zarządzane” — Hibernate pamięta je w kontekście
 *   utrwalania (pamięć podręczna 1. poziomu) i sam zapisze każdą ich zmianę przy flush/commit, bez wywołania „save”.
 *   To wygodne, ale wymaga zrozumienia, KIEDY Hibernate naprawdę rozmawia z bazą — o tym jest ta lekcja.
 *
 * ANALOGIA: szatnia w teatrze.
 *   Oddajesz płaszcz (persist) — dostajesz numerek (id). Szatniarz (kontekst utrwalania) pamięta, który płaszcz
 *   wisi pod którym numerem: poprosisz dwa razy o numer 7 — dostaniesz ten SAM płaszcz, szatniarz nie idzie
 *   do magazynu drugi raz (find, pamięć 1. poziomu). Jeśli przyszyjesz guzik do płaszcza, który wisi w szatni
 *   (encja zarządzana), szatniarz zauważy to przy inwentaryzacji (flush). Płaszcz zabrany do domu (detached)
 *   możesz przerabiać do woli — szatnia o tym nie wie, dopóki nie oddasz go ponownie (merge).
 *
 * JAK TO DZIAŁA:
 *   1. Starter spring-boot-starter-data-jpa wnosi Hibernate 7.4 i autokonfigurację HibernateJpaAutoConfiguration.
 *      Ta tworzy EntityManagerFactory (fabryka zarządców encji; w Hibernate to SessionFactory), menedżer transakcji
 *      JpaTransactionManager oraz współdzielone proxy EntityManager, które w każdej transakcji podaje „właściwy”
 *      EntityManager (z jego własnym kontekstem utrwalania).
 *   2. Encje Boot znajduje skanowaniem pakietu klasy {@code @SpringBootApplication} (tu: jpa01_entities) —
 *      tak samo jak komponenty. Z klas buduje metamodel (opis encji, pól, kolumn).
 *   3. Przy {@code ddl-auto=create-drop} (profil jpa01) Hibernate przy starcie wykonuje CREATE TABLE na podstawie
 *      metamodelu, a przy zamknięciu DROP. Od Jpa02 schemat robi Flyway (s08_data_jpa/Jpa06FlywayWithJpa).
 *   4. persist/find/remove działają na kontekście utrwalania; SQL leci do bazy przy flush (przed commitem, przed
 *      zapytaniem JPQL albo gdy wywołasz em.flush()) — wyjątkiem jest INSERT przy IDENTITY (sekcja 3).
 *
 * SŁÓWKA:
 *   persist = utrwal (zacznij zarządzać nowym obiektem); find = znajdź; remove = usuń; merge = scal (skopiuj stan
 *   obiektu odłączonego do zarządzanego); detach = odłącz; managed = zarządzany; transient = przejściowy (nowy,
 *   nieznany Hibernate); detached = odłączony; removed = usunięty; proxy = pośrednik (podklasa udająca encję);
 *   DDL (Data Definition Language) = polecenia tworzące schemat (CREATE TABLE…); sequence = sekwencja (licznik w bazie).
 *
 * ZOBACZ TEŻ: s07_data_jdbc/Jdbc05Repository (to samo ręcznie: SQL, RowMapper, repozytorium na JdbcClient),
 *             t29_jdbc_databases/Jdbc05Dao (czyste JDBC: DAO),
 *             s08_data_jpa/Jpa02Repositories (Spring Data JPA pisze repozytorium za Ciebie),
 *             s08_data_jpa/Jpa06FlywayWithJpa (schemat z Flyway, ddl-auto=validate),
 *             s10_transactions/Tx01Transactional (transakcje, w których żyje kontekst utrwalania).
 * </pre>
 */
public final class Jpa01Entities {

    private Jpa01Entities() {
    }

    // =================================================================================================
    // 1. ORM — OBIEKT ↔ WIERSZ; JPA, HIBERNATE I SPRING DATA JPA (TRZY WARSTWY)
    // =================================================================================================
    //
    // Obiekt Product  ⟷  wiersz tabeli product. Pole ⟷ kolumna. Referencja ⟷ klucz obcy (relacje: dział s09).
    // W s07_data_jdbc/Jdbc05Repository robiłeś to ręcznie: SQL w stringu + RowMapper (wiersz → rekord) + parametry.
    // ORM przejmuje tę „mechanikę”; Ty opisujesz tylko mapowanie.
    //
    //   warstwa             co to jest                                      pakiet / przykład
    //   JPA (Jakarta        SPECYFIKACJA: same interfejsy i adnotacje,      jakarta.persistence.*: @Entity, @Id,
    //   Persistence 3.2)    bez kodu, który coś robi                        EntityManager, EntityManagerFactory
    //   Hibernate ORM 7.4   IMPLEMENTACJA JPA: generuje SQL, zarządza       org.hibernate.*: SessionFactory,
    //                       kontekstem utrwalania, dialekty baz (H2Dialect) Session, Statistics
    //   Spring Data JPA     WARSTWA NAD JPA: repozytoria z interfejsów      org.springframework.data.jpa.*:
    //   (2026.0)            (JpaRepository), zapytania z nazw metod         JpaRepository (Jpa02, Jpa03)
    //
    // Piszesz kod pod JPA (jakarta.persistence), a Hibernate jest „silnikiem” pod spodem. Ta lekcja używa JPA
    // bezpośrednio (EntityManager), żeby było widać, co później Spring Data robi za Ciebie.
    //
    // Co tworzy Boot (test sekcji 1): EntityManagerFactory (rozpakowana — unwrap — to Hibernate SessionFactory),
    // JpaTransactionManager i współdzielony EntityManager. Metamodel zawiera DOKŁADNIE encje z pakietu lekcji
    // (Product, Customer) — encje innych lekcji modułu są w innych pakietach i tu nie trafiają.
    //
    // PUŁAPKA: {@code import javax.persistence.*} ze starych tutoriali (Boot 2 / Hibernate 5). Dlaczego? W Boot 3+
    //   JPA mieszka w pakiecie jakarta.persistence; javax.persistence nie ma na classpath (ścieżce klas), więc kod
    //   się nie skompiluje. Zamiana importów javax → jakarta to pierwszy krok migracji starego projektu.
    //
    // DOBRA PRAKTYKA: importuj adnotacje z jakarta.persistence, a nie ich odpowiedniki z org.hibernate.annotations,
    //   jeśli JPA ma własne. Dlaczego? Kod zależy od standardu; adnotacje Hibernate bierz tylko na rzeczy, których
    //   JPA nie ma (np. @CreationTimestamp).

    // =================================================================================================
    // 2. MAPOWANIE — @Entity, @Table, @Id, @GeneratedValue, @Column, @Enumerated, @Transient, daty
    // =================================================================================================
    //
    // Encja Product (przeczytaj komentarze w Product.java). Wygenerowany DDL — Hibernate wypisuje go w logu
    // (logger org.hibernate.SQL), a test odczytuje strukturę z INFORMATION_SCHEMA:
    //
    //   create table product (price numeric(10,2) not null, stock integer not null,
    //       created_at timestamp(6) with time zone not null, id bigint generated by default as identity,
    //       name varchar(100) not null unique, category enum ('COMPUTERS','HOME','OFFICE') not null, primary key (id))
    //
    //   adnotacja / typ pola                            efekt w tabeli (H2, sprawdzone testem)
    //   @Id + @GeneratedValue(strategy = IDENTITY)      id BIGINT ... GENERATED BY DEFAULT AS IDENTITY
    //   @Column(nullable = false, length = 100)         NAME CHARACTER VARYING(100) NOT NULL
    //   @Column(precision = 10, scale = 2) BigDecimal   PRICE NUMERIC(10,2)
    //   int (typ prosty)                                STOCK INTEGER NOT NULL (int nie może być null)
    //   @Enumerated(STRING)                             CATEGORY ENUM('COMPUTERS','HOME','OFFICE') — na H2 typ ENUM;
    //                                                   na innych bazach VARCHAR + CHECK; zawsze NAZWA stałej
    //   Instant                                         TIMESTAMP WITH TIME ZONE
    //   LocalDate (Customer.registeredOn)               DATE (LocalDateTime → TIMESTAMP)
    //   @Transient                                      brak kolumny
    //   pole bez adnotacji (Customer.email)             VARCHAR(255), dopuszcza NULL
    //
    // Nazwy kolumn: Boot ustawia strategię nazw PhysicalNamingStrategySnakeCaseImpl — pole registeredOn trafia
    // do kolumny registered_on (camelCase → snake_case). Jawną nazwę daje @Column(name = "created_at").
    //
    // STRATEGIE @GeneratedValue (H2, Hibernate 7 — sprawdzone testem):
    //   IDENTITY — numer nadaje baza przy INSERT. Skoro id znamy dopiero po INSERT, Hibernate wysyła go OD RAZU
    //              przy persist (nie czeka do flush) i nie może łączyć INSERT-ów w paczki (batch).
    //   SEQUENCE — numer z sekwencji bazy (select next value for …) PRZED INSERT; INSERT czeka do flush.
    //   AUTO (domyślna, Customer) — Hibernate 7 wybiera SEQUENCE: tworzy sekwencję customer_seq z krokiem 50.
    //              Krok 50 = Hibernate pobiera jedną wartość z bazy na 50 nowych obiektów (optymalizator „pooled”),
    //              dlatego numery mogą mieć dziury — to normalne, numer to identyfikator, nie licznik sztuk.
    //   UWAGA na stare tutoriale: w Hibernate 5 AUTO dawało tabelę hibernate_sequence wspólną dla wszystkich encji.
    //
    // Domyślne ddl-auto w Boot 4 (sprawdzone w HibernateDefaultDdlAutoProvider i testem): baza wbudowana (H2) i BRAK
    // narzędzia migracji (Flyway/Liquibase) → create-drop; w każdym innym przypadku → none. Dlatego lekcje ustawiają
    // tę wartość jawnie — nie polegaj na domyślnej, która zmienia się po dodaniu Flyway.
    //
    // Dlaczego create-drop tylko na naukę/prototyp? Hibernate tworzy schemat od zera przy każdym starcie i kasuje
    // go przy zamknięciu — wszystkie dane giną; nie ma historii zmian ani przeglądu SQL w code review; nie masz
    // kontroli nad nazwami ograniczeń (CONSTRAINT_1 zamiast uk_product_name). Na produkcji schemat zmieniają
    // migracje (Flyway, s07_data_jdbc/Jdbc04Flyway), a Hibernate go tylko sprawdza (validate — Jpa06).
    //
    // PUŁAPKA: {@code @Enumerated} pominięte albo ORDINAL (Customer.level). Dlaczego groźne? W bazie ląduje NUMER
    //   stałej (GOLD = 1, test). Ktoś dopisze PLATINUM przed GOLD — od tej chwili każde 1 w bazie czyta się jako
    //   PLATINUM. Dane po cichu zmieniają znaczenie, bez żadnego błędu. Zawsze @Enumerated(EnumType.STRING).
    //
    // DOBRA PRAKTYKA: pieniądze jako BigDecimal z @Column(precision, scale). Dlaczego? double nie zapisze dokładnie
    //   0.10 (t01_basics/Basics08FloatingPoint, t15_numbers/Numbers01BigDecimal), a bez precision/scale Hibernate wybierze domyślne
    //   (numeric(38,2)) — schemat powinien mówić wprost, jakich liczb się spodziewasz.

    // =================================================================================================
    // 3. ENTITYMANAGER — persist, find, remove, flush; kontekst utrwalania = pamięć 1. poziomu
    // =================================================================================================
    //
    //   metoda                   co robi                                         kiedy SQL (sprawdzone statystykami)
    //   em.persist(nowy)         nowy obiekt staje się zarządzany                IDENTITY: INSERT od razu;
    //                                                                            SEQUENCE: tylko next value, INSERT przy flush
    //   em.find(Product.class,   zwraca encję albo null (brak wiersza)           SELECT, ale tylko gdy encji o tym id
    //           id)                                                              nie ma jeszcze w kontekście
    //   em.remove(zarządzany)    oznacza do usunięcia                            DELETE przy flush
    //   em.flush()               wysyła zaległe INSERT/UPDATE/DELETE             teraz (transakcja nadal otwarta)
    //   em.clear()               zapomina wszystkie encje (odłącza je)           brak SQL
    //
    // Kontekst utrwalania (persistence context) to mapa „klasa + id → obiekt” w EntityManagerze. Dwa find tego samego
    // id w jednej transakcji → TEN SAM obiekt (==) i JEDEN SELECT (test). Gwarancja: w jednym kontekście jeden
    // wiersz = jeden obiekt Javy (PYTANIE REKRUTACYJNE: „co to jest first-level cache w Hibernate?”).
    // Kontekst żyje tyle, co transakcja — w Spring każde @Transactional (i każdy test @DataJpaTest) ma własny.
    //
    // flush ≠ commit. flush wysyła SQL do bazy w ramach transakcji (inne transakcje jeszcze go nie widzą);
    // commit zatwierdza. Hibernate robi flush sam: przed commitem i przed zapytaniem JPQL/SQL przez Hibernate,
    // które mogłoby zobaczyć nieaktualne dane (FlushMode AUTO).
    //
    // PUŁAPKA: em.find dla nieistniejącego id zwraca null, a NIE wyjątek (test). Dlaczego ważne? {@code
    //   em.find(...).getName()} kończy się NullPointerException. Spring Data opakowuje to w Optional (Jpa02).
    //
    // DOBRA PRAKTYKA: w testach wywołuj flush() i clear() przed sprawdzeniem wyniku (TestEntityManager:
    //   persistAndFlush, flush, clear). Dlaczego? Bez clear() find zwraca obiekt z PAMIĘCI kontekstu — test
    //   „zielony”, choć do bazy nic nie trafiło albo trafiło coś innego (sekcja 4 pokazuje to wprost).

    // =================================================================================================
    // 4. STANY ENCJI — transient → managed → detached → removed; dirty checking, detach i merge
    // =================================================================================================
    //
    //   new Product(...)  ── persist ──▶  MANAGED  ── remove ──▶  REMOVED  ── flush/commit ──▶ DELETE
    //   (TRANSIENT)                       │   ▲
    //                     detach/clear/   │   │  merge (zwraca KOPIĘ zarządzaną)
    //                     koniec transakcji▼   │
    //                                     DETACHED
    //
    //   TRANSIENT — zwykły obiekt Javy; Hibernate o nim nie wie (em.contains = false, id = null).
    //   MANAGED   — w kontekście utrwalania; każda zmiana pola trafi do bazy przy flush (em.contains = true).
    //   DETACHED  — ma id, odpowiada wierszowi, ale kontekst o nim zapomniał; zmiany NIE trafiają do bazy.
    //   REMOVED   — zaplanowany do usunięcia; DELETE przy flush.
    //
    // DIRTY CHECKING (wykrywanie zmian): przy wczytaniu Hibernate robi „zdjęcie” (snapshot) wartości pól. Przy flush
    // porównuje zdjęcie z obecnym stanem i dla zmienionych encji generuje UPDATE — bez żadnego save/update (test:
    // setPrice + flush = jeden UPDATE; drugi flush bez zmian = zero poleceń). Domyślnie UPDATE ustawia WSZYSTKIE
    // kolumny (jeden gotowy SQL na encję, łatwy do ponownego użycia); @DynamicUpdate z Hibernate to zmienia.
    //
    // merge(odłączony): Hibernate wczytuje (albo bierze z kontekstu) zarządzaną encję o tym id, KOPIUJE do niej stan
    // odłączonego obiektu i zwraca TĘ ZARZĄDZANĄ KOPIĘ. Obiekt przekazany do merge nadal jest odłączony (test).
    //
    // PUŁAPKA: {@code em.merge(product); product.setStock(5);} — zmiana po merge nie trafi do bazy. Dlaczego? merge
    //   zwraca inny obiekt; zarządzana jest kopia, a Ty zmieniasz oryginał. Zawsze: {@code product = em.merge(product);}.
    //
    // PUŁAPKA: asercja bez flush()/clear() (test). Zmieniasz cenę i od razu sprawdzasz em.find(...).getPrice() —
    //   widzisz nową cenę, choć w bazie (odczyt JdbcClient) nadal jest stara. Czytasz pamięć, nie bazę.
    //
    // DOBRA PRAKTYKA: zmieniaj encje przez metody z sensem biznesowym (product.restock(5)), a nie gołe settery
    //   wszędzie. Dlaczego? Dirty checking zapisze KAŻDĄ zmianę encji zarządzanej — także przypadkową; im mniej
    //   miejsc zmienia stan, tym łatwiej to kontrolować.

    // =================================================================================================
    // 5. PUŁAPKI ENCJI — konstruktor bez argumentów, final, equals/hashCode (testy: pakiet testowy jpa01_pitfalls)
    // =================================================================================================
    //
    // Błędne encje leżą w osobnym pakiecie TESTOWYM (s08_data_jpa.jpa01_pitfalls), bo wszystko pod jpa01_entities
    // zeskanowałaby klasa startowa lekcji (także klasy testowe — w testach są na tej samej ścieżce klas). Test buduje
    // dla nich osobną, małą EntityManagerFactory przez standardowe API JPA 3.2 (HibernatePersistenceConfiguration).
    //
    // a) BRAK KONSTRUKTORA BEZ ARGUMENTÓW. Hibernate tworzy obiekt pustym konstruktorem i wpisuje pola z wiersza.
    //    Co się dzieje (sprawdzone testem)? Start NIE zgłasza błędu (tylko log INFO HHH000182), persist działa,
    //    a wybucha dopiero pierwszy ODCZYT: org.hibernate.InstantiationException „No default constructor for entity”.
    //    Błąd wychodzi daleko od przyczyny, często dopiero na produkcji. Konstruktor może być protected.
    //
    // b) KLASA (albo metody) final. Leniwe proxy Hibernate (getReference, relacje LAZY w s09) to PODKLASA encji —
    //    klasy final nie da się rozszerzyć. Hibernate nie zgłasza błędu, tylko po cichu rezygnuje z proxy:
    //    getReference od razu wykonuje SELECT i zwraca prawdziwy obiekt (test: zwykła klasa → klasa ...$HibernateProxy
    //    i zero zapytań do pierwszego użycia; klasa final → zwykła klasa i SELECT od razu). Tracisz leniwe ładowanie.
    //
    // c) equals/hashCode NA WSZYSTKICH POLACH (np. Lombok @Data albo wygenerowane przez IDE). Encja trafia do HashSet
    //    jako nowa (id = null), persist nadaje id → hashCode się zmienia → {@code set.contains(encja)} zwraca false,
    //    choć obiekt NADAL jest w zbiorze (test). To samo po zmianie dowolnego pola. Do tego equals na polach
    //    uznaje za „równe” dwa różne wiersze o tych samych danych.
    //
    // DOBRA PRAKTYKA: dwie poprawne drogi (PYTANIE REKRUTACYJNE: „jak napisać equals/hashCode encji?”).
    //   1) Po id z obsługą null (Product): equals = ten sam typ i id != null i równe id; hashCode = STAŁA dla klasy
    //      (Product.class.hashCode()), bo id zmienia się z null na liczbę. instanceof zamiast getClass() — proxy
    //      to podklasa; other.getId() zamiast other.id — pola proxy są puste.
    //   2) Po kluczu naturalnym (business key), który jest unikalny i niezmienny, np. numer ISBN, e-mail jako login.
    //      Dlaczego nie domyślne z Object? Wczytany dwa razy w RÓŻNYCH kontekstach ten sam wiersz to dwa obiekty —
    //      Object.equals uzna je za różne.
    //
    // Lombok przy encjach (t20_lombok/Lombok03DataValueBuilder): @Getter/@Setter tak, @Data i @EqualsAndHashCode
    //   na wszystkich polach — nie (punkt c), @ToString — ostrożnie (relacje LAZY w s09 wywołają dodatkowe SELECT-y).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • JPA = specyfikacja (jakarta.persistence), Hibernate = implementacja, Spring Data JPA = repozytoria nad JPA.
     *   • Encja: @Entity + @Id, konstruktor bez argumentów (może być protected), klasa i metody nie-final,
     *     zmienny stan. Rekordy — na projekcje/DTO, nie na encje.
     *   • @GeneratedValue: IDENTITY → INSERT od razu przy persist; SEQUENCE/AUTO (H2: sekwencja klasa_seq, krok 50)
     *     → INSERT przy flush.
     *   • @Enumerated(EnumType.STRING) zawsze; ORDINAL zapisuje numer stałej.
     *   • @Column(nullable, length, precision, scale) opisuje kolumnę; @Transient = pole bez kolumny.
     *   • Kontekst utrwalania: jeden wiersz = jeden obiekt; find drugi raz = bez SELECT; find brak = null.
     *   • Encja zarządzana + zmiana pola = UPDATE przy flush/commit (dirty checking), bez save.
     *   • merge zwraca ZARZĄDZANĄ KOPIĘ — używaj wyniku. detach/clear = zmiany przestają trafiać do bazy.
     *   • W testach: flush() + clear() przed asercjami, inaczej czytasz pamięć, nie bazę.
     *   • equals po id (z null) + stały hashCode albo po kluczu naturalnym; nigdy po wszystkich polach.
     *   • ddl-auto=create-drop tylko do nauki; od Jpa02 Flyway + validate.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różnią się JPA, Hibernate i Spring Data JPA? Z którego pakietu importujesz @Entity w Boot 4?
     *   2. Dlaczego encja nie może być rekordem? Wymień dwa powody.
     *   3. Co się stanie? W jednej transakcji dwa razy wywołujesz em.find(Product.class, 1L). Ile poleceń SELECT
     *      trafi do bazy i czy oba wyniki są tym samym obiektem (==)?
     *   4. Co się stanie? Wczytujesz produkt przez find, zmieniasz mu cenę setterem i NIE wołasz żadnego save/merge.
     *      Transakcja się kończy commitem. Czy cena w bazie się zmieni?
     *   5. ZNAJDŹ BŁĄD: {@code em.merge(detachedProduct); detachedProduct.setStock(5); em.flush();} — stan magazynu
     *      w bazie nie zmienił się na 5. Dlaczego?
     *   6. Co się stanie? Encja nie ma konstruktora bez argumentów (jest tylko konstruktor z polami). Czy aplikacja
     *      wystartuje? Kiedy i jaki wyjątek się pojawi?
     *   7. Dlaczego equals/hashCode na wszystkich polach (Lombok @Data) jest groźne dla encji w HashSet?
     *   8. Jaką strategię wybiera @GeneratedValue bez parametrów w Hibernate 7 na H2 i dlaczego numery mogą mieć dziury?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jpa01EntitiesExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): utrwal nowy produkt i zwróć numer (id), który nadała mu baza.
     * Podpowiedź: sekcja 3 — {@code em.persist(product)}; przy IDENTITY id jest znane zaraz po persist.
     */
    static Long exercise1Persist(EntityManager em, Product product) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zmień cenę produktu o danym id na {@code newPrice} BEZ wywoływania persist ani merge.
     * Zwróć {@code true}, gdy produkt istniał, i {@code false}, gdy nie ma go w bazie.
     * Podpowiedź: sekcja 4 — dirty checking: wystarczy encja zarządzana z {@code em.find(...)} i setter;
     * pamiętaj, że find dla nieistniejącego id zwraca null.
     */
    static boolean exercise2ChangePrice(EntityManager em, long id, BigDecimal newPrice) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): dostajesz produkt ODŁĄCZONY (detached), już zmieniony przez kogoś poza transakcją.
     * Zapisz jego stan i zwróć obiekt, na którym da się dalej pracować (zarządzany).
     * Podpowiedź: sekcja 4 — {@code em.merge(...)} zwraca kopię zarządzaną; nie zwracaj argumentu.
     */
    static Product exercise3SaveDetached(EntityManager em, Product detached) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): z podanej listy numerów usuń te produkty, które istnieją i mają stan magazynu 0.
     * Nieistniejące numery pomiń. Zwróć liczbę usuniętych produktów.
     * Podpowiedź: sekcja 3 — {@code em.find} (może zwrócić null), {@code em.remove} przyjmuje tylko encję
     * ZARZĄDZANĄ; DELETE poleci przy flush (test robi go sam).
     */
    static int exercise4RemoveOutOfStock(EntityManager em, List<Long> ids) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. JPA to specyfikacja (interfejsy i adnotacje), Hibernate to jej implementacja (generuje SQL, zarządza
     *      kontekstem), Spring Data JPA to warstwa repozytoriów nad JPA. W Boot 4: jakarta.persistence.Entity
     *      (javax.persistence było w Boot 2).
     *   2. Rekord nie ma konstruktora bez argumentów, jest niezmienny (dirty checking wymaga zmiany pól w miejscu)
     *      i jest final (Hibernate nie zrobi z niego leniwego proxy — podklasy).
     *   3. Jeden SELECT; oba wyniki to ten sam obiekt (==) — drugi find bierze encję z kontekstu utrwalania.
     *      Sprawdzone testem (sekcja 3, statystyki Hibernate: 1 polecenie).
     *   4. Tak. Encja jest zarządzana, więc przy flush (przed commitem) dirty checking wykryje zmianę i wyśle UPDATE.
     *      Sprawdzone testem (sekcja 4).
     *   5. merge zwraca nowy, zarządzany obiekt; detachedProduct nadal jest odłączony, więc jego zmiana nie trafia
     *      do bazy. Poprawnie: {@code Product managed = em.merge(detachedProduct); managed.setStock(5);}.
     *      Sprawdzone testem (sekcja 4).
     *   6. Aplikacja wystartuje (Hibernate pisze tylko INFO HHH000182), persist też zadziała. Błąd pojawi się przy
     *      pierwszym odczycie encji z bazy: org.hibernate.InstantiationException „No default constructor for entity”.
     *      Sprawdzone testem (Jpa01PitfallsTest).
     *   7. hashCode zależy od pól, które się zmieniają (id nadawane przy persist, cena…). Po zmianie obiekt leży
     *      w zbiorze w „starym” kubełku i contains/remove go nie znajdują. Sprawdzone testem (Jpa01PitfallsTest).
     *   8. AUTO → SEQUENCE: sekwencja customer_seq z krokiem 50. Hibernate rezerwuje pulę 50 numerów jednym
     *      zapytaniem; numery niewykorzystane (np. po restarcie aplikacji) przepadają. Sprawdzone testem (sekcja 2).
     */
    // </editor-fold>
}
