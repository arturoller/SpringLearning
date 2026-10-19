package s08_data_jpa.jpa06_flyway_with_jpa;

/**
 * <pre>
 * TEMAT: Flyway + JPA — schemat z migracji, Hibernate tylko sprawdza (ddl-auto=validate)
 *        (migration = migracja, wersjonowany skrypt zmieniający bazę; validate = sprawdź, zweryfikuj;
 *         schema = schemat bazy: tabele, kolumny, sekwencje; drift = rozjazd, tu: baza różni się od kodu)
 *
 * W SKRÓCIE:
 *   W Jpa01–Jpa05 tabele tworzył Hibernate z encji (create-drop) — wygodne w nauce, nie do przyjęcia na produkcji:
 *   brak historii zmian, brak przeglądu, ryzyko utraty danych. W prawdziwym projekcie schemat rośnie migracjami
 *   Flyway (V1__..., V2__... — poznałeś je w s07_data_jdbc/Jdbc04Flyway), a Hibernate dostaje
 *   spring.jpa.hibernate.ddl-auto=validate: przy starcie porównuje encje z tabelami i, gdy coś się nie zgadza,
 *   ZATRZYMUJE start aplikacji z dokładnym komunikatem. Błąd wychodzi przy uruchomieniu, a nie przy pierwszym
 *   zamówieniu klienta.
 *
 * ANALOGIA: budowa domu.
 *   Migracje Flyway to kolejne wpisy w dzienniku budowy: „V1 fundamenty, V2 piętro, V3 meble” — każdy wpis raz,
 *   po kolei, nic nie znika z dziennika. Hibernate z validate to inspektor nadzoru: nie buduje, tylko sprawdza,
 *   czy dom zgadza się z projektem (encjami). ddl-auto=update to majster, który w nocy sam dobudowuje ścianę
 *   „bo w projekcie była” — a rano ekipa z dziennikiem (Flyway) próbuje postawić ją drugi raz.
 *
 * JAK TO DZIAŁA:
 *   1. Bean entityManagerFactory ZALEŻY od beana flywayInitializer (FlywayAutoConfiguration) — dlatego migracje
 *      zawsze wykonują się przed startem Hibernate'a (widać to w komunikacie błędu z sekcji 6:
 *      "Failed to initialize dependency 'flywayInitializer' of LoadTimeWeaverAware bean 'entityManagerFactory'").
 *   2. Flyway czyta spring.flyway.locations (tu: classpath:db/migration/jpa06), wykonuje brakujące migracje
 *      i zapisuje je w tabeli flyway_schema_history.
 *   3. Hibernate z ddl-auto=validate dla KAŻDEJ encji sprawdza: czy tabela istnieje, czy każde pole ma kolumnę,
 *      czy typ kolumny pasuje, czy istnieje sekwencja dla @GeneratedValue. Pierwsza niezgodność =
 *      SchemaManagementException i koniec startu.
 *   4. Gdy Flyway (albo Liquibase) jest w projekcie, a ddl-auto nie ustawisz, Boot nie każe Hibernate'owi robić
 *      ze schematem NIC (Jpa01Entities, sekcja 4) — dlatego validate warto wpisać jawnie.
 *
 * SŁÓWKA:
 *   migration = migracja; version = wersja; history = historia; location = lokalizacja (katalog migracji);
 *   validate = sprawdź; update = zaktualizuj; missing = brakujący; wrong = zły; column type = typ kolumny;
 *   target = cel (wersja, do której migrować); offenders = „winowajcy” (pliki, które powodują błąd).
 *
 * ZOBACZ TEŻ: s07_data_jdbc/Jdbc04Flyway (nazwy migracji, suma kontrolna, flyway_schema_history),
 *             s08_data_jpa/Jpa01Entities (ddl-auto i jego wartość domyślna),
 *             s09_jpa_relations/Rel01ManyToOne (następny krok: relacje między encjami, np. zamówienie → klient),
 *             s11_testing/Test06Testcontainers (testy na prawdziwej bazie zamiast H2).
 * </pre>
 */
public final class Jpa06FlywayWithJpa {

    private Jpa06FlywayWithJpa() {
    }

    // =================================================================================================
    // 1. Najpierw Flyway, potem Hibernate — schemat i dane z migracji
    // =================================================================================================
    //
    // Profil jpa06 (application-jpa06.properties):
    //   spring.flyway.locations=classpath:db/migration/jpa06      ← WŁASNY katalog lekcji (sekcja 3)
    //   spring.jpa.hibernate.ddl-auto=validate                     ← Hibernate tylko sprawdza
    // Migracje (src/main/resources/db/migration/jpa06):
    //   V1__create_tables.sql      create sequence products_seq start with 1001 increment by 50;
    //                              create table products (...), create table customers (...)
    //   V2__add_stock.sql          alter table products add column stock integer not null default 0
    //   V3__insert_shop_data.sql   14 produktów sklepu (jawne id 1–14) i 7 klientów
    // Log startu (prawdziwy, skrócony): Migrating schema "PUBLIC" to version "1 - create tables" ...
    //   Successfully applied 3 migrations to schema "PUBLIC", now at version v3.
    // Flyway (bean Flyway) pokazuje historię przez flyway.info().applied(). Sprawdzone testem (sekcja 1).
    //
    // Sekwencja z migracji musi pasować do mapowania: @GeneratedValue szuka products_seq z krokiem 50.
    // Hibernate traktuje wartość z sekwencji jako GÓRNĄ granicę paczki 50 numerów — z sekwencją od 1001 pierwszy
    // nowy produkt dostał w uruchomieniu id 952 (= 1001 − 49). Dlatego sekwencja startuje z zapasem nad id 1–14
    // wstawionymi w V3. Sprawdzone testem (sekcja 1: nowe id większe niż 14).
    //
    // PUŁAPKA: w logu startu zobaczysz WARN Flyway: "Using H2 2.4.240 which is newer than the version Flyway has been
    //   verified with. The latest verified version of H2 is 2.3.232." To tylko ostrzeżenie — Boot 4.1 dobiera H2
    //   nowsze niż przetestowane przez Flyway. Migracje działają (sprawdzone testami). Nie każdy WARN to błąd —
    //   ale każdy trzeba przeczytać.

    // =================================================================================================
    // 2. Co sprawdza validate, a czego nie
    // =================================================================================================
    //
    // Sprawdza (dla każdej encji): tabelę, kolumnę dla każdego pola, typ kolumny, sekwencję.
    // NIE sprawdza: kolumn i tabel, których żadna encja nie używa. W tej lekcji:
    //   • kolumna notes z V1 nie ma pola w Product — start bez błędu;
    //   • tabela customers z V1 nie ma encji (dopóki nie zrobisz ćwiczenia 2) — start bez błędu;
    //   • kategoria: w migracji varchar(20), w encji @Enumerated(EnumType.STRING) — validate akceptuje
    //     (choć sam Hibernate na H2 tworzyłby typ enum — Jpa01Entities, sekcja 3).
    // Sprawdzone testem (sekcja 2).
    //
    // DOBRA PRAKTYKA: ustaw ddl-auto=validate jawnie we wszystkich środowiskach, w których schematem zarządza Flyway.
    //   Dlaczego? Domyślnie (sekcja 3) Hibernate niczego nie sprawdza — rozjazd encji i bazy wyjdzie dopiero
    //   wyjątkiem SQL w trakcie działania, przy pierwszym zapytaniu, które trafi na brakującą kolumnę.

    // =================================================================================================
    // 3. Domyślne ddl-auto przy Flyway i własny katalog migracji
    // =================================================================================================
    //
    // Bez profilu jpa06 (bez validate), ale z migracjami lekcji: hibernate.hbm2ddl.auto = null — Boot widzi, że
    // schematem zarządza Flyway, i nie każe Hibernate'owi nic robić (ani tworzyć, ani sprawdzać).
    // Sprawdzone testem (sekcja 3).
    //
    // Co się stanie bez spring.flyway.locations? Flyway bierze domyślne classpath:db/migration i przeszukuje je
    // RAZEM Z PODKATALOGAMI. W testach tego modułu na ścieżce klas są dwa katalogi z wersją 1 (jpa06 i testowy
    // jpa06-wrong-type), więc start kończy się błędem:
    //   Found more than one migration with version 1
    //   Offenders:
    //   -> ...\target\test-classes\db\migration\jpa06-wrong-type\V1__price_as_text.sql (SQL)
    //   -> ...\target\classes\db\migration\jpa06\V1__create_tables.sql (SQL)
    // (offenders = winowajcy). Sprawdzone testem (sekcja 3). Dlatego każda lekcja tego modułu albo ma własny
    // katalog (spring.flyway.locations), albo wyłączony Flyway (spring.flyway.enabled=false).

    // =================================================================================================
    // 4. Co się stanie, gdy brakuje migracji? (spring.flyway.target=1)
    // =================================================================================================
    //
    // spring.flyway.target=1 = „migruj tylko do wersji 1” — symuluje bazę, na którą ktoś zapomniał wgrać V2.
    // Encja Product ma pole stock, tabela — nie. Prawdziwy łańcuch wyjątków:
    //   BeanCreationException: Error creating bean with name 'entityManagerFactory' ...: Unable to build Hibernate
    //     SessionFactory  [persistence unit: default] ; nested exception is ...
    //   └ jakarta.persistence.PersistenceException: Unable to build Hibernate SessionFactory ...
    //     └ org.hibernate.tool.schema.spi.SchemaManagementException:
    //       Schema validation: missing column [stock] in table [products]
    // Aplikacja nie wstaje — i to jest DOBRA wiadomość: lepiej nie wystartować niż działać na złym schemacie.
    // Sprawdzone testem (sekcja 4).

    // =================================================================================================
    // 5. Zły typ kolumny
    // =================================================================================================
    //
    // Testowa migracja db/migration/jpa06-wrong-type (tylko w src/test/resources) tworzy price jako varchar(20),
    // a encja ma BigDecimal (numeric(10,2)). Prawdziwy komunikat:
    //   Schema validation: wrong column type encountered in column [price] in table [products];
    //   found [character varying (Types#VARCHAR)], but expecting [numeric(10,2) (Types#NUMERIC)]
    // Komunikat mówi wszystko: która kolumna, co jest w bazie, czego oczekuje encja. Sprawdzone testem (sekcja 5).

    // =================================================================================================
    // 6. Dlaczego NIE ddl-auto=update na produkcji (PYTANIE REKRUTACYJNE)
    // =================================================================================================
    //
    // Prawdziwy przebieg (test sekcji 6, jedna baza, dwa uruchomienia):
    //   A. baza z samym V1 + ddl-auto=update → Hibernate „naprawia” schemat po swojemu:
    //        alter table if exists products add column stock integer not null
    //   B. kolejne uruchomienie z pełnymi migracjami → Flyway wykonuje V2 (add column stock) i:
    //        Migration of schema "PUBLIC" to version "2 - add stock" failed! Please restore backups and roll back
    //        database and code!   SQL State : 42S21   (zduplikowana nazwa kolumny "STOCK")
    //      → FlywayMigrateException: Failed to execute script V2__add_stock.sql → aplikacja nie wstaje.
    // Do tego update w Hibernate 7.4 potrafi ZMIENIĆ TYP istniejącej kolumny (sprawdzone uruchomieniem na bazie
    // z price varchar: "alter table if exists products alter column price set data type numeric(10,2)") — na
    // tabeli z milionem wierszy to długa blokada albo błąd konwersji. Kolumn, których encja nie zna, nie usuwa
    // (notes została), więc zmiana nazwy pola kończy się NOWĄ kolumną obok starej, z danymi w starej.
    // Wnioski:
    //   • update nie zostawia historii (co, kiedy, dlaczego) i nikt nie przegląda jego zmian przed wdrożeniem;
    //   • nie wie, że zmieniłeś nazwę, a nie dodałeś nowe pole;
    //   • miesza się z Flyway: dwóch „właścicieli” schematu = konflikty jak wyżej.
    // DOBRA PRAKTYKA: produkcja = Flyway + validate. create-drop/update zostaw do szybkich eksperymentów
    //   na bazie w pamięci (jak Jpa01–Jpa05). Dlaczego? Schemat bazy z danymi klientów to kod jak każdy inny:
    //   wersjonowany, przeglądany i odtwarzalny — a nie efekt uboczny startu aplikacji.

    // =================================================================================================
    // 7. Co dalej
    // =================================================================================================
    //
    // Encje tego działu żyją osobno. Prawdziwy sklep łączy je: zamówienie należy do klienta, ma pozycje z produktami.
    // Relacje (@ManyToOne, @OneToMany), leniwe ładowanie i problem N+1 — dział s09_jpa_relations, zaczynając od
    // s09_jpa_relations/Rel01ManyToOne. Schemat dla nich będzie rósł kolejnymi migracjami — tak jak tutaj.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Produkcja: spring.flyway.locations=... (migracje V1__, V2__...) + spring.jpa.hibernate.ddl-auto=validate.
     *   • Kolejność startu: flywayInitializer → entityManagerFactory (migracje zawsze przed Hibernate'em).
     *   • validate sprawdza tabele, kolumny, typy i sekwencje encji; ignoruje kolumny/tabele bez encji.
     *     Błąd: SchemaManagementException "Schema validation: missing column [...]" / "wrong column type ...".
     *   • Flyway w projekcie + brak ddl-auto = Hibernate nic nie robi ze schematem (ustaw validate jawnie).
     *   • Domyślne classpath:db/migration skanuje podkatalogi — dwa pliki V1 = "Found more than one migration with version 1".
     *   • Zmiana schematu = NOWA migracja (V4__...), nigdy edycja wykonanej.
     *   • update: dobudowuje kolumny, potrafi zmienić typ, nic nie usuwa, nie zna zmian nazw, gryzie się z Flyway.
     *   • Sekwencja z migracji: nazwa <tabela>_seq, increment by 50, start powyżej id wstawionych ręcznie.
     *
     * PYTANIA KONTROLNE:
     *   1. Skąd pewność, że migracje Flyway wykonają się, zanim Hibernate zacznie sprawdzać schemat?
     *   2. Co się stanie? Uruchamiasz aplikację lekcji z profilem jpa06 i --spring.flyway.target=1.
     *   3. Co się stanie? Usuwasz z application-jpa06.properties linię spring.flyway.locations i uruchamiasz testy
     *      tej lekcji.
     *   4. ZNAJDŹ BŁĄD: zespół dodał pole weight do encji Product i wdrożył aplikację z ddl-auto=validate, ale bez
     *      nowej migracji. Co zobaczy w logu i jak to naprawić?
     *   5. Tabela ma kolumnę notes, której encja nie zna. Czy validate zgłosi błąd? Dlaczego?
     *   6. Wymień trzy powody, dla których ddl-auto=update nie nadaje się na produkcję.
     *   7. Jaką wartość ma hibernate.hbm2ddl.auto, gdy w projekcie jest Flyway, a ty nie ustawiłeś ddl-auto?
     *      Co z tego wynika?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Jpa06FlywayWithJpaExercisesTest (uruchamiasz sam ▶; @Tag("cwiczenie"))
    // =================================================================================================
    //
    // ĆWICZENIE 1 (łatwe): napisz migrację V4__add_weight_column.sql, która dodaje do tabeli products kolumnę
    //   weight_grams typu integer (może być pusta — istniejące produkty nie mają jeszcze wagi). Umieść ją w NOWYM
    //   katalogu src/main/resources/db/migration/jpa06-exercises (nie w jpa06 — tam zmieniłaby schemat testów lekcji).
    //   Test uruchomi Flyway z katalogami jpa06 i jpa06-exercises na pustej bazie i sprawdzi historię oraz kolumnę.
    //   Podpowiedź: nazwa pliku = V + numer + DWA podkreślniki + opis; treść — jak V2__add_stock.sql.
    //
    // ĆWICZENIE 2 (średnie): zamień klasę Customer (ten pakiet) w encję pasującą DOKŁADNIE do tabeli customers
    //   z migracji V1 — tak, żeby validate ją przepuścił. Pole name ↔ kolumna full_name, id nadaje baza (identity).
    //   Podpowiedź: @Entity, @Table, @Id + @GeneratedValue(strategy = GenerationType.IDENTITY),
    //   @Column(name = "full_name", ...). Błąd w mapowaniu zatrzyma start — przeczytaj komunikat „Schema validation”.

    /**
     * ĆWICZENIE 3 (trudniejsze): diagnoza startu. Uruchom aplikację lekcji (Jpa06Application, bez serwera WWW)
     * z podanymi argumentami. Gdy start się uda — zamknij kontekst i zwróć {@code null}. Gdy się nie uda — zwróć
     * komunikat NAJGŁĘBSZEJ przyczyny wyjątku (tej na samym dole łańcucha), np. dla {@code --spring.flyway.target=1}:
     * {@code "Schema validation: missing column [stock] in table [products]"}.
     * Podpowiedź: try-with-resources na ConfigurableApplicationContext, catch, pętla po getCause()
     * (albo gotowe NestedExceptionUtils.getMostSpecificCause z pakietu org.springframework.core).
     */
    static String exercise3(String... args) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bean entityManagerFactory zależy od beana flywayInitializer, więc Spring tworzy go (i wykonuje migracje)
     *      wcześniej. Widać to w komunikacie z sekcji 6: "Failed to initialize dependency 'flywayInitializer' of
     *      LoadTimeWeaverAware bean 'entityManagerFactory'". Sprawdzone testem (sekcje 1 i 6).
     *   2. Flyway wykona tylko V1, a validate zatrzyma start: SchemaManagementException: Schema validation:
     *      missing column [stock] in table [products] (opakowany w PersistenceException i BeanCreationException).
     *      Sprawdzone testem (sekcja 4).
     *   3. Flyway użyje domyślnego classpath:db/migration razem z podkatalogami. W testach są tam dwa pliki z wersją 1,
     *      więc start (i każdy test z kontekstem) kończy się: Found more than one migration with version 1 (plus lista
     *      Offenders). Sprawdzone testem (sekcja 3).
     *   4. Start zakończy się SchemaManagementException: Schema validation: missing column [weight] in table [products]
     *      (validate działa — błąd przy wdrożeniu, a nie u klienta). Naprawa: NOWA migracja (np. V4__add_weight.sql)
     *      w tym samym wdrożeniu co zmiana encji; nigdy edycja już wykonanej migracji.
     *   5. Nie. validate sprawdza tylko to, czego potrzebują encje — dodatkowe kolumny i tabele są dozwolone
     *      (to normalne np. przy kilku aplikacjach na jednej bazie). Sprawdzone testem (sekcja 2).
     *   6. Np.: brak historii i przeglądu zmian; nie odróżnia zmiany nazwy od nowego pola (nowa kolumna, dane w starej);
     *      potrafi zmieniać typy kolumn na żywej bazie; gryzie się z Flyway (V2 pada na kolumnie dodanej przez update —
     *      sekcja 6); nic nie usuwa, więc śmieci zostają na zawsze.
     *   7. null (Boot nie ustawia jej wcale) — Hibernate ani nie tworzy, ani nie sprawdza schematu. Rozjazd encji i
     *      tabel wyjdzie dopiero przy zapytaniu w trakcie działania, dlatego validate warto ustawić jawnie.
     *      Sprawdzone testem (sekcja 3).
     */
    // </editor-fold>
}
