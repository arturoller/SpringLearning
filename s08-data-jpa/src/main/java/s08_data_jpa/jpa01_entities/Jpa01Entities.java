package s08_data_jpa.jpa01_entities;

import jakarta.persistence.EntityManager;

/**
 * <pre>
 * TEMAT: Encje JPA — klasa Javy, której obiekty Hibernate zapisuje w tabeli
 *        (JPA = Jakarta Persistence API, standard mapowania obiektów na tabele; Hibernate = najpopularniejsza
 *         implementacja JPA, tu w wersji 7.4; entity = encja; mapping = mapowanie, odwzorowanie;
 *         ORM = object-relational mapping, mapowanie obiektowo-relacyjne)
 *
 * W SKRÓCIE:
 *   W s07_data_jdbc pisałeś SQL ręcznie i ręcznie zamieniałeś wiersze na obiekty. JPA odwraca kierunek: opisujesz
 *   KLASĘ adnotacjami (@Entity, @Id, @Column…), a Hibernate sam układa SQL: INSERT przy zapisie, SELECT przy
 *   odczycie, UPDATE, gdy zmienisz pole obiektu. W tej lekcji: jak zbudować encję, skąd bierze się id, jak
 *   Hibernate tworzy tabele (ddl-auto), jak podejrzeć generowany SQL i czym jest kontekst utrwalania.
 *
 * ANALOGIA: formularz w urzędzie i urzędnik.
 *   Klasa encji to wzór formularza (pola i ich opisy), a tabela to segregator z wypełnionymi formularzami.
 *   Hibernate to urzędnik: dostaje od ciebie wypełniony formularz (obiekt) i sam wpina go do segregatora.
 *   Biurko urzędnika to kontekst utrwalania: formularze, nad którymi właśnie pracuje. Jeśli poprawisz formularz
 *   leżący na biurku, urzędnik sam przepisze poprawkę do segregatora przy najbliższym porządkowaniu (flush).
 *
 * JAK TO DZIAŁA:
 *   Starter spring-boot-starter-data-jpa (Boot 4: moduły spring-boot-jpa, spring-boot-hibernate, spring-boot-data-jpa):
 *     1. Autokonfiguracja tworzy DataSource (pulę połączeń HikariCP) do bazy z spring.datasource.url.
 *     2. HibernateJpaConfiguration tworzy bean "entityManagerFactory" (LocalContainerEntityManagerFactoryBean):
 *        skanuje pakiet klasy @SpringBootApplication (i podpakiety) w poszukiwaniu klas @Entity
 *        i buduje z nich metamodel (metamodel = opis wszystkich encji, ich pól i kolumn).
 *     3. Zależnie od spring.jpa.hibernate.ddl-auto Hibernate tworzy tabele (create-drop) albo nic nie robi.
 *     4. Powstaje JpaTransactionManager — transakcje dla @Transactional (dział s10_transactions).
 *     5. EntityManager (zarządca encji) to API: persist (utrwal), find (znajdź), flush (wypchnij zmiany do bazy),
 *        clear (wyczyść kontekst). W kolejnych lekcjach schowa się za repozytoriami Spring Data.
 *
 * SŁÓWKA:
 *   entity = encja; table = tabela; column = kolumna; identifier (id) = identyfikator; primary key = klucz główny;
 *   generated value = wartość generowana; sequence = sekwencja (licznik w bazie); identity = kolumna tożsamości
 *   (numer nadaje baza); persist = utrwalić (zapisać nowy obiekt); flush = wypchnąć zmiany (wysłać SQL);
 *   persistence context = kontekst utrwalania; managed = zarządzany; detached = odłączony; dirty checking =
 *   wykrywanie zmian („brudnych” pól); schema = schemat bazy (tabele, kolumny); DDL = polecenia tworzące schemat
 *   (create/alter/drop); business key = klucz biznesowy (naturalny, np. SKU).
 *
 * ZOBACZ TEŻ: t29_jdbc_databases/Jdbc05Dao (ręczne mapowanie wierszy w czystym JDBC),
 *             s07_data_jdbc/Jdbc01JdbcClient (SQL pisany ręcznie w Springu),
 *             t12_collections/Collections11HashingInternals (jak HashSet używa hashCode),
 *             t09_records/Records01Basics (rekordy — dlaczego nie nadają się na encje),
 *             s08_data_jpa/Jpa02Repositories (repozytoria zamiast EntityManagera).
 * </pre>
 */
public final class Jpa01Entities {

    private Jpa01Entities() {
    }

    // =================================================================================================
    // 1. @Entity, @Table, @Id — minimalna encja i jej wymagania
    // =================================================================================================
    //
    //   @Entity                       ← „ta klasa ma tabelę”; Hibernate znajdzie ją skanując pakiet aplikacji
    //   @Table(name = "products")     ← nazwa tabeli (bez tego: z nazwy klasy, camelCase → snake_case:
    //   public class Product {           klasa NoCtorProduct dałaby tabelę no_ctor_product)
    //       @Id @GeneratedValue private Long id;      ← klucz główny
    //       protected Product() { }                   ← konstruktor bez argumentów DLA HIBERNATE
    //
    // Wymagania (Jakarta Persistence 3.2) i DLACZEGO:
    //   • konstruktor bez argumentów (public/protected) — przy odczycie Hibernate najpierw tworzy PUSTY obiekt,
    //     a potem wpisuje do pól wartości z wiersza;
    //   • klasa i jej metody nie final — Hibernate tworzy podklasę-pośrednika (proxy) do leniwego ładowania;
    //   • pola nie final — wpisuje do nich wartości po utworzeniu obiektu.
    // Dlatego encja to zwykła, ZMIENNA klasa z getterami, a nie rekord.
    //
    // PUŁAPKA: rekord albo klasa bez konstruktora bezargumentowego jako encja NIE zatrzymuje startu aplikacji.
    //   Hibernate 7.4 zapisze tylko INFO "HHH000182: No default (no-argument) constructor for class [...]",
    //   INSERT zadziała, a błąd pojawi się dopiero przy PIERWSZYM ODCZYCIE:
    //   org.hibernate.InstantiationException: No default constructor for entity '...RecordProduct'.
    //   Dlaczego tak późno? Do zapisu wystarczą gettery istniejącego obiektu; konstruktor jest potrzebny dopiero,
    //   gdy Hibernate musi STWORZYĆ obiekt z wiersza. Sprawdzone testem (sekcja 1, klasa RecordProduct).
    //   Klasa final z konstruktorem działa (sprawdzone uruchomieniem), ale bez proxy — getReference od razu
    //   wykonuje SELECT, a leniwe ładowanie (s09_jpa_relations) przestaje działać dla tej encji.
    //
    // PYTANIE REKRUTACYJNE: dlaczego encja JPA nie może być rekordem? (odpowiedź wyżej i w ODPOWIEDZIACH)

    // =================================================================================================
    // 2. @GeneratedValue — skąd bierze się id: SEQUENCE czy IDENTITY
    // =================================================================================================
    //
    //   Product  @GeneratedValue                                  (strategia AUTO — Hibernate wybiera sam)
    //   Customer @GeneratedValue(strategy = GenerationType.IDENTITY)
    //
    // DDL z prawdziwego logu (Hibernate 7.4 + H2 2.4):
    //   create sequence products_seq start with 1 increment by 50
    //   create table products (stock integer not null, unit_price numeric(10,2) not null, id bigint not null, ...)
    //   create table customers (vip boolean not null, id bigint generated by default as identity, ...)
    // Czyli AUTO dla pola Long na H2 = SEKWENCJA o nazwie <tabela>_seq z krokiem 50.
    //
    // Różnica widoczna w logu SQL:
    //   SEQUENCE (Product):  persist → "select next value for products_seq" → id już jest w obiekcie,
    //                        ale INSERT czeka do flush (np. końca transakcji). Hibernate rezerwuje numery
    //                        paczkami po 50 (allocationSize) — przy 5 zapisach pyta sekwencję najwyżej 2 razy.
    //   IDENTITY (Customer): persist → OD RAZU "insert into customers (...) values (?,?,?,?,default)", bo numer
    //                        nada dopiero baza przy wstawieniu wiersza — Hibernate nie może z tym czekać.
    // Sprawdzone testem (sekcja 2).
    //
    // DOBRA PRAKTYKA: gdy baza ma sekwencje (H2, PostgreSQL, Oracle), zostaw SEQUENCE (domyślne AUTO).
    //   Dlaczego? Hibernate odkłada wtedy INSERT-y do flush i może wysłać je paczką (batch — po ustawieniu
    //   spring.jpa.properties.hibernate.jdbc.batch_size); IDENTITY wymusza osobny INSERT przy każdym persist.
    //   IDENTITY wybierz, gdy baza nie ma sekwencji (np. MySQL) albo tabela już ma kolumnę AUTO_INCREMENT.

    // =================================================================================================
    // 3. @Column i @Enumerated — szczegóły kolumn
    // =================================================================================================
    //
    //   @Column(nullable = false, unique = true, length = 20)  → sku varchar(20) not null unique
    //   @Column(name = "unit_price", precision = 10, scale = 2) → unit_price numeric(10,2) (pole nazywa się price)
    //   int stock (bez @Column)                                 → stock integer not null (typ prosty nie ma null)
    //   String email (bez @Column)                              → email varchar(255) (domyślna długość 255)
    //   @Enumerated(EnumType.STRING) Category category          → na H2: category enum ('DOM','ELEKTRONIKA',...)
    // Atrybuty @Column (nullable, length, unique) przede wszystkim wpływają na wygenerowany DDL. Kto pilnuje
    // danych? Sprawdzone testem (sekcja 3) w tym module:
    //   • nullable = false + null      → Hibernate sam, już przy persist: PropertyValueException: not-null property
    //                                     references a null or transient value for entity ...Product.name
    //                                     (Hibernate robi to, bo w tym module nie ma Bean Validation — s05_validation;
    //                                     z walidacją na ścieżce klas zostawia to walidacji i bazie);
    //   • za długi tekst (length = 20) → dopiero baza przy flush: org.hibernate.exception.DataException;
    //   • powtórzone sku (unique)      → dopiero baza przy flush: org.hibernate.exception.ConstraintViolationException.
    // Do sprawdzania danych PRZED zapisem służy walidacja (dział s05_validation).
    //
    // PUŁAPKA: enum bez @Enumerated zapisuje się jako NUMER POZYCJI (EnumType.ORDINAL — domyślny!).
    //   Prawdziwy DDL dla pola OrderStatus bez adnotacji: status tinyint check ((status between 0 and 4)).
    //   Dlaczego to groźne? Gdy ktoś dopisze nową stałą w środku enuma albo zmieni kolejność, stare wiersze
    //   z numerem 2 nagle znaczą coś innego (WYSLANE zamienia się w inny status) — bez żadnego błędu.
    //   Zawsze pisz @Enumerated(EnumType.STRING). Sprawdzone testem (sekcja 3: w bazie leży tekst "ELEKTRONIKA").

    // =================================================================================================
    // 4. spring.jpa.hibernate.ddl-auto — kto tworzy tabele
    // =================================================================================================
    //
    //   none        — nic nie rób (schemat tworzy ktoś inny, np. Flyway)
    //   validate    — tylko sprawdź, czy tabele pasują do encji; niezgodność = błąd startu (Jpa06FlywayWithJpa)
    //   update      — dobuduj brakujące tabele i kolumny (Hibernate 7.4 potrafi też zmienić typ kolumny;
    //                 niczego nie usuwa) — NIE na produkcji (Jpa06FlywayWithJpa, sekcja 6)
    //   create      — usuń i utwórz tabele przy starcie
    //   create-drop — jak create + usuń tabele przy zamknięciu aplikacji
    //
    // Wartość DOMYŚLNA w Boot 4.1 (sprawdzone w kodzie HibernateDefaultDdlAutoProvider i testem sekcji 4):
    //   • baza wbudowana (H2/HSQLDB/Derby w pamięci) i schematem NIE zarządza Flyway/Liquibase → create-drop;
    //   • w każdym innym przypadku → none (Boot nie przekazuje wtedy Hibernate'owi żadnej wartości — Jpa06FlywayWithJpa).
    // Ta lekcja ma spring.flyway.enabled=false, więc dostaje create-drop i w logu widać "create table ...".
    //
    // PUŁAPKA: błąd w DDL przy create/create-drop NIE zatrzymuje startu aplikacji. Encja z
    //   @Table(name = "order") (ORDER to słowo zastrzeżone SQL) daje w logu tylko WARN
    //   "GenerationTarget encountered exception accepting command : Error executing DDL ..." — aplikacja startuje,
    //   a wybucha dopiero pierwszy INSERT (SQLGrammarException: Could not prepare statement ...).
    //   Dlaczego? Hibernate traktuje generowanie schematu jako „pomoc”, a nie warunek startu.
    //   Sprawdzone uruchomieniem. Wniosek: po każdej zmianie encji czytaj WARN-y w logu startu.

    // =================================================================================================
    // 5. Podgląd SQL: spring.jpa.show-sql kontra logger org.hibernate.SQL
    // =================================================================================================
    //
    //   spring.jpa.show-sql=true                          → System.out: "Hibernate: insert into products ..."
    //   logging.level.org.hibernate.SQL=debug             → log: "DEBUG ... org.hibernate.SQL : insert into ..."
    //   logging.level.org.hibernate.orm.jdbc.bind=trace   → wartości parametrów (zamiast znaków ?):
    //                                                        "binding parameter (4:VARCHAR) <- [ELE-001]"
    //   spring.jpa.properties.hibernate.format_sql=true   → SQL w wielu wierszach, z wcięciami
    // Ta lekcja w profilu jpa01 włącza logger; @DataJpaTest DODATKOWO włącza show-sql (atrybut showSql = true
    // domyślnie) — dlatego w testach każde polecenie widać dwa razy. Sprawdzone testem (sekcja 5).
    //
    // DOBRA PRAKTYKA: zamiast show-sql używaj loggera org.hibernate.SQL.
    //   Dlaczego? show-sql pisze prosto na System.out, z pominięciem systemu logowania: bez daty, wątku
    //   i poziomu, nie da się go skierować do pliku ani wyłączyć dla jednego pakietu. Logger podlega tym samym
    //   ustawieniom co cała aplikacja (logging.level..., plik logu).

    // =================================================================================================
    // 6. Kontekst utrwalania (persistence context) — „biurko” Hibernate'a w skrócie
    // =================================================================================================
    //
    // Każda transakcja ma swój kontekst utrwalania: mapę „id → obiekt” encji, które Hibernate właśnie śledzi.
    //   • persist(p) / find(...) → obiekt staje się ZARZĄDZANY (managed).
    //   • find tego samego id drugi raz → ten SAM obiekt z pamięci, bez SELECT (gwarancja tożsamości:
    //     jedna encja = jeden obiekt w kontekście; dlatego mówi się też „pamięć podręczna pierwszego poziomu”).
    //   • zmiana pola zarządzanego obiektu (setPrice) → przy flush Hibernate porównuje obiekt ze zdjęciem
    //     z chwili wczytania (dirty checking) i SAM wysyła UPDATE — bez wywołania żadnego „save”.
    //     Prawdziwy SQL: update products set category=?,name=?,unit_price=?,sku=?,stock=? where id=?
    //     (domyślnie Hibernate aktualizuje WSZYSTKIE kolumny, nie tylko zmienioną).
    //   • clear() → kontekst pusty, obiekty stają się ODŁĄCZONE (detached): zmiany w nich nikt już nie śledzi.
    // Sprawdzone testem (sekcja 6). Szczegóły transakcji — dział s10_transactions.
    //
    // PUŁAPKA: test „przechodzi”, a w bazie nic nie ma. Bez flush (i bez końca transakcji) INSERT/UPDATE
    //   jeszcze nie poszły do bazy; find zwraca obiekt z kontekstu, a nie z tabeli. W testach JPA rób
    //   flush() i clear() przed sprawdzeniem — wtedy kolejny find naprawdę wykona SELECT.

    // =================================================================================================
    // 7. equals i hashCode encji — PUŁAPKA z HashSet
    // =================================================================================================
    //
    // ŹLE (Employee): equals/hashCode z pola id generowanego przez bazę.
    //   • dwaj RÓŻNI nowi pracownicy mają id == null, więc są „równi” → w HashSet zostaje tylko jeden;
    //   • pracownik wrzucony do HashSet przed persist „znika” po persist: hashCode zmienił się z hash(null)
    //     na hash(1), a HashSet szuka go w innym kubełku → contains zwraca false.
    // DOBRZE (Product): equals/hashCode z klucza BIZNESOWEGO (sku) — unikalnego, niezmiennego, znanego od
    //   początku. DOBRZE (Customer): id, ale „nowe obiekty równe tylko sobie” + STAŁY hashCode klasy.
    // Sprawdzone testem (sekcja 7).
    //
    // DOBRA PRAKTYKA: w equals używaj instanceof i getterów (other.getSku()), nie getClass() i pól.
    //   Dlaczego? Hibernate może podsunąć proxy — podklasę encji z pustymi polami, która ładuje dane dopiero
    //   przy wywołaniu metody (s09_jpa_relations/Rel04LazyLoading). getClass() proxy to nie Product.class.
    // PUŁAPKA: equals/hashCode wygenerowane przez Lomboka (@Data) albo IDE ze WSZYSTKICH pól — zmiana ceny
    //   zmienia hashCode, a w encjach z relacjami (s09) dochodzi nieskończona rekurencja. Dlatego przy encjach
    //   nie używaj @Data (t20_lombok/Lombok03DataValueBuilder).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Encja: @Entity + @Id, konstruktor bez argumentów (protected), klasa/pola nie final. Nie rekord.
     *   • @GeneratedValue (AUTO) na H2 = sekwencja <tabela>_seq, krok 50, INSERT przy flush;
     *     IDENTITY = numer nadaje baza, INSERT natychmiast przy persist.
     *   • @Column(name, nullable, length, unique, precision, scale) — kształtuje DDL; null w nullable=false
     *     wyłapie Hibernate przy persist (gdy brak Bean Validation), długość i unikalność — baza przy flush.
     *   • Enum zawsze @Enumerated(EnumType.STRING) — domyślny ORDINAL zapisuje numer pozycji.
     *   • ddl-auto: none | validate | update | create | create-drop. Domyślnie create-drop dla bazy wbudowanej
     *     bez Flyway/Liquibase, w pozostałych przypadkach none. Błędy DDL to tylko WARN w logu.
     *   • SQL: logger org.hibernate.SQL=debug (+ org.hibernate.orm.jdbc.bind=trace dla wartości), nie show-sql.
     *   • Kontekst utrwalania: jeden obiekt na id, dirty checking (UPDATE bez save), flush wysyła SQL,
     *     clear odłącza obiekty.
     *   • equals/hashCode: klucz biznesowy albo „bezpieczne id + stały hashCode”. Nigdy surowe id ani wszystkie pola.
     *
     * PYTANIA KONTROLNE:
     *   1. Wymień wymagania, które musi spełnić klasa encji, i powiedz, po co Hibernate każde z nich.
     *   2. Co się stanie? Rekord RecordProduct z tej lekcji ({@code @Entity record RecordProduct(@Id String sku,
     *      String name)}): uruchamiasz aplikację, zapisujesz obiekt przez persist, robisz flush i clear,
     *      a potem wczytujesz go przez find.
     *   3. Czym różni się moment wysłania INSERT dla @GeneratedValue z sekwencją i z IDENTITY? Dlaczego?
     *   4. Co się stanie? Encja ma @Table(name = "order"), ddl-auto=create-drop, baza H2.
     *   5. ZNAJDŹ BŁĄD: pole {@code private OrderStatus status;} bez żadnej adnotacji, a po miesiącu ktoś dopisuje
     *      stałą ZWROCONE pomiędzy OPLACONE a WYSLANE.
     *   6. Jaką wartość ma domyślnie spring.jpa.hibernate.ddl-auto dla H2 w pamięci bez Flyway, a jaką dla
     *      PostgreSQL? Skąd ta różnica?
     *   7. ZNAJDŹ BŁĄD: encja ma {@code hashCode() { return Objects.hashCode(id); }}, id z @GeneratedValue.
     *      Zamówienia zbierasz w HashSet przed zapisem.
     *   8. Zmieniasz cenę obiektu wczytanego przez find i nie wywołujesz żadnego save. Czy zmiana trafi do bazy?
     *      Kiedy tak, a kiedy nie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jpa01EntitiesExercisesTest (uruchamiasz sam ▶; @Tag("cwiczenie"))
    // =================================================================================================
    //
    // ĆWICZENIE 1 (łatwe): zamień klasę ShopOrder (ten pakiet) w encję: tabela "orders", klucz główny = numer
    //   zamówienia nadawany przez sklep (String, BEZ @GeneratedValue), status zapisany jako tekst, kwota
    //   numeric(10,2) NOT NULL. Podpowiedź: @Entity, @Table, @Id, @Enumerated(EnumType.STRING),
    //   @Column(precision = ..., scale = ..., nullable = ...). Dlaczego nie tabela "order" — patrz sekcja 4.
    //
    // ĆWICZENIE 2 (średnie): dopisz w ShopOrder equals i hashCode po numerze zamówienia (klucz biznesowy).
    //   Test sprawdzi: dwa obiekty z tym samym numerem są równe, a zamówienie dodane do HashSet przed persist
    //   nadal jest w nim po persist. Podpowiedź: wzór w klasie Product.

    /**
     * ĆWICZENIE 3 (trudniejsze): zmień status zarządzanego zamówienia o podanym numerze na
     * {@link OrderStatus#WYSLANE} BEZ wywoływania persist ani merge — niech zrobi to dirty checking.
     * Test wywoła metodę w transakcji, potem zrobi flush i clear i sprawdzi status w bazie.
     * Podpowiedź: {@code entityManager.find(ShopOrder.class, number)} i setter. Wymaga ukończonego ćwiczenia 1.
     */
    static void exercise3(EntityManager entityManager, String orderNumber) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. @Entity i @Id (Hibernate musi wiedzieć, że klasa ma tabelę i który klucz identyfikuje wiersz);
     *      konstruktor bez argumentów public/protected (przy odczycie tworzy pusty obiekt i wpisuje pola);
     *      klasa i metody nie final (podklasa-proxy do leniwego ładowania); pola nie final (wpisywanie wartości
     *      po utworzeniu obiektu).
     *   2. Aplikacja wystartuje (w logu tylko INFO HHH000182: No default (no-argument) constructor for class ...),
     *      persist i flush (INSERT) zadziałają, a find po clear wykona SELECT i rzuci
     *      org.hibernate.InstantiationException: No default constructor for entity '...RecordProduct'.
     *      Rekord nie ma konstruktora bez argumentów. Sprawdzone testem (sekcja 1).
     *   3. Sekwencja: przy persist Hibernate pobiera numer (select next value for ..._seq, paczkami po 50), a INSERT
     *      wysyła dopiero przy flush. IDENTITY: INSERT natychmiast przy persist, bo numer nada dopiero baza
     *      w chwili wstawienia wiersza, a Hibernate musi znać id od razu (klucz w kontekście utrwalania).
     *   4. Aplikacja wystartuje — błąd DDL to tylko WARN "GenerationTarget encountered exception accepting command
     *      : Error executing DDL "create table order ..." (składnia: oczekiwano identyfikatora). Pierwszy zapis
     *      zakończy się SQLGrammarException: Could not prepare statement [... insert into [*]order ...].
     *      Napraw: @Table(name = "orders"). Sprawdzone uruchomieniem.
     *   5. Brak @Enumerated = ORDINAL: w bazie leży numer pozycji (tinyint 0..4). Po dopisaniu stałej w środku
     *      stare wiersze z numerem 2 (WYSLANE) czytają się jako ZWROCONE, z numerem 3 (DOSTARCZONE) jako WYSLANE,
     *      a 4 (ANULOWANE) jako DOSTARCZONE — dane po cichu zmieniają znaczenie. Napraw: @Enumerated(EnumType.STRING)
     *      (i migracja zamieniająca stare numery na nazwy).
     *   6. H2 w pamięci bez Flyway: create-drop; PostgreSQL: none. Boot uznaje bazę wbudowaną za „jednorazową”
     *      (znika z procesem), więc tworzy i usuwa tabele; prawdziwej bazy nie rusza (HibernateDefaultDdlAutoProvider).
     *      Gdy schematem zarządza Flyway/Liquibase — none także dla H2. Sprawdzone testem (sekcja 4) i w Jpa06.
     *   7. Przed zapisem id == null: wszystkie nowe zamówienia mają ten sam hashCode i są sobie „równe” — w HashSet
     *      zostanie jedno. Po zapisie hashCode się zmienia i contains przestaje je znajdować. Napraw: klucz
     *      biznesowy (numer zamówienia) albo bezpieczne id + stały hashCode (jak w Customer). Sprawdzone testem (sekcja 7).
     *   8. Tak, jeśli obiekt jest ZARZĄDZANY (wczytany w tej samej, wciąż otwartej transakcji): przy flush/commicie
     *      dirty checking wyśle UPDATE. Nie, jeśli obiekt jest odłączony (po clear, po zamknięciu transakcji) —
     *      wtedy trzeba go scalić (merge / save w Spring Data — lekcja Jpa02Repositories). Sprawdzone testem (sekcja 6).
     */
    // </editor-fold>
}
