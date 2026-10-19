package s02_configuration.cfg04_yaml;

/**
 * <pre>
 * TEMAT: application.yml — te same ustawienia w czytelniejszym, hierarchicznym zapisie
 *        (YAML = format zapisu danych oparty na wcięciach; indentation = wcięcie; document = dokument;
 *         list = lista; hierarchy = hierarchia, zagnieżdżenie; separator = separator, oddzielacz)
 *
 * W SKRÓCIE:
 *   Plik YAML (application.yml) niesie dokładnie te same klucze co application.properties, ale zamiast powtarzać
 *   prefiks w każdej linii, zapisujesz go raz — jako zagnieżdżone, wcięte bloki. Listy są pozycjami z myślnikiem,
 *   a w jednym pliku może być wiele dokumentów (oddzielonych linią ---), z których każdy ma własny warunek
 *   (np. tylko dla profilu). Boot po wczytaniu SPŁASZCZA YAML do zwykłych kluczy z kropkami, więc @Value,
 *   @ConfigurationProperties i priorytety działają tak samo jak dla .properties.
 *
 * ANALOGIA: spis treści książki i numeracja rozdziałów.
 *   .properties: każda linia ma pełny adres — "2.1.3 Zasady" (cfg04.shop.address.street=…).
 *   YAML: rozdział "2" zapisany raz, pod nim wcięty "2.1", pod nim "2.1.3" — czytelniej, ale WCIĘCIE jest znaczące:
 *   przesunięcie o jedną spację przenosi punkt do innego rozdziału.
 *
 * JAK TO DZIAŁA:
 *   cfg04.yml                              po wczytaniu (te same klucze co w .properties):
 *   cfg04:                                 cfg04.shop.name=Sklep YAML
 *     shop:                                cfg04.shop.opening-days[0]=pon
 *       name: Sklep YAML                   cfg04.shop.opening-days[1]=wt
 *       opening-days: [pon, wt]            ...
 *   1. YamlPropertySourceLoader czyta plik biblioteką SnakeYAML (jako UTF-8) i spłaszcza go do kluczy z kropkami.
 *   2. Każdy dokument (rozdzielony ---) staje się osobnym źródłem; dokument z spring.config.activate.on-profile
 *      jest użyty tylko wtedy, gdy ten profil jest aktywny.
 *   3. Reszta (priorytety, @Value, wiązanie) działa już na zwykłych kluczach — YAML jest tylko innym zapisem.
 *
 * SŁÓWKA:
 *   indentation = wcięcie; tab = tabulator; space = spacja; sequence/list = lista; mapping = mapa klucz–wartość;
 *   multi-document = wiele dokumentów w jednym pliku; flatten = spłaszczyć; boolean = wartość logiczna;
 *   quote = cudzysłów; to take precedence = mieć pierwszeństwo.
 *
 * ZOBACZ TEŻ: s02_configuration/Cfg01Properties (priorytety źródeł; pliki .properties w ISO-8859-1),
 *             s02_configuration/Cfg02ConfigurationProperties (wiązanie list, map i obiektów),
 *             s02_configuration/Cfg03Profiles (profile; tu ten sam efekt w jednym pliku YAML),
 *             t18_io_files/Io06Properties (klasa Properties w czystej Javie).
 * </pre>
 */
public final class Cfg04Yaml {

    private Cfg04Yaml() {
    }

    // =================================================================================================
    // 1. PROPERTIES vs YAML — ten sam klucz, inny zapis
    // =================================================================================================
    //
    //   application.properties                          application.yml
    //   cfg04.shop.name=Sklep YAML                      cfg04:
    //   cfg04.shop.address.street=Długa 1                 shop:
    //   cfg04.shop.address.zip=90-001                       name: Sklep YAML
    //                                                       address:
    //                                                         street: Długa 1
    //                                                         zip: "90-001"
    // Wcięcie to ZAWSZE spacje (zwykle dwie) — nigdy tabulatory. Po dwukropku musi być spacja ("klucz: wartość").
    // Test sekcji 1 pokazuje spłaszczanie: YAML staje się kluczami z kropkami.
    // Bonus: YAML jest czytany jako UTF-8, więc "Łódź" i "Długa" wpisujesz normalnie (w .properties — kody unicode).
    //
    // DOBRA PRAKTYKA: w jednym projekcie trzymaj JEDEN format (albo .properties, albo .yml). Dlaczego?
    //   Gdy ten sam klucz jest w obu, wygrywa .properties (sekcja 4) — zmieniasz wartość w .yml i nic się nie dzieje.
    //   Dwa formaty to zagadka dla każdego, kto przyjdzie po Tobie.
    //
    // UWAGA: rozszerzenie .yaml też działa. Adnotacja @PropertySource domyślnie NIE czyta YAML-a (to mechanizm
    // dla .properties) — YAML trafia do aplikacji wyłącznie jako pliki konfiguracji Boota.

    // =================================================================================================
    // 2. LISTY W YAML
    // =================================================================================================
    //
    //   opening-days:            albo w jednej linii:   opening-days: [pon, wt, śr]
    //     - pon
    //     - wt
    //     - śr
    // Po spłaszczeniu: cfg04.shop.opening-days[0]=pon, [1]=wt, [2]=śr — takie same indeksy jak ręcznie w .properties.
    // Wiąże się z polem List w rekordzie (YamlShopProperties.openingDays). Test sekcji 2.
    //
    // PUŁAPKA: lista w YAML w pliku profilu lub dokumentu NIE dokleja się do listy z innego pliku — ZASTĘPUJE ją.
    //   Dlaczego? Listy są wiązane jako całość: jeśli dokument profilu definiuje choć jeden element listy,
    //   Boot bierze listę z tego źródła, a elementów z niższego priorytetu nie łączy.
    //   (Zasada z dokumentacji Boota; w tej lekcji jej nie uruchamiamy.)

    // =================================================================================================
    // 3. WIELE DOKUMENTÓW W JEDNYM PLIKU — separator --- i on-profile
    // =================================================================================================
    //
    //   cfg04:                                    ← dokument 1: obowiązuje zawsze
    //     shop:
    //       name: Sklep YAML
    //   ---                                       ← separator dokumentów
    //   spring:
    //     config:
    //       activate:
    //         on-profile: cfg04-promo             ← dokument 2: tylko gdy aktywny jest profil cfg04-promo
    //   cfg04:
    //     shop:
    //       banner: Wyprzedaż do -30%!
    // To odpowiednik osobnego pliku application-cfg04-promo.properties z lekcji Cfg03Profiles — tyle że wszystko
    // leży w jednym pliku. Test sekcji 3: bez profilu baner to "brak", z profilem cfg04-promo — tekst z dokumentu 2.
    //
    // UWAGA (zmiana względem starych tutoriali): dawniej piszono spring.profiles: promo w dokumencie.
    // To zapis sprzed Boota 2.4. Dziś jest spring.config.activate.on-profile: promo. Stary zapis w Boot 4 NIE jest
    // po cichu pomijany — aplikacja nie wystartuje (sprawdzone testem sekcji 3):
    //   InvalidConfigDataPropertyException: Property 'spring.profiles' imported from location '…' is invalid
    //   and should be replaced with 'spring.config.activate.on-profile'
    //
    // PUŁAPKA: zapomniałeś o separatorze --- przed dokumentem profilu. Skutek zależy od reszty pliku
    //   (oba warianty sprawdzone testem sekcji 3):
    //   a) drugi „dokument” powtarza klucz główny pierwszego (u nas cfg04:) — w jednym dokumencie YAML ten sam klucz
    //      wystąpił dwa razy, więc aplikacja NIE wystartuje: DuplicateKeyException: found duplicate key cfg04;
    //   b) klucze się nie powtarzają — powstaje JEDEN dokument z warunkiem on-profile. Warunek dotyczy CAŁEGO
    //      dokumentu, więc bez profilu znikają także ustawienia „zwykłe”, które miały obowiązywać zawsze — bez błędu.
    //   Dlaczego? spring.config.activate.on-profile to warunek dla całego dokumentu, a nie dla linii, które stoją
    //   pod nim. Tylko separator --- dzieli plik na dokumenty.
    // PUŁAPKA: literówka w nazwie profilu w on-profile (cfg04-prmo). Dlaczego groźna? Dokument po cichu nigdy się
    //   nie włączy — Boot nie wie, że takiego profilu nikt nie zamierza włączać.

    // =================================================================================================
    // 4. KTO WYGRYWA, GDY ISTNIEJĄ OBA PLIKI
    // =================================================================================================
    //
    // W module leżą cfg04.yml (conflict: z-yml) i cfg04.properties (conflict=z-properties). Wynik w teście sekcji 4:
    // "z-properties". Reguła Boota: dla tej samej nazwy i tego samego miejsca .properties ma pierwszeństwo przed .yml.
    //
    // PUŁAPKA: edytujesz wartość w application.yml, uruchamiasz — bez zmian. Dlaczego? W tym samym katalogu
    //   leży application.properties z tym samym kluczem i to ono wygrywa. Sprawdzone testem (sekcja 4).
    //   Diagnoza: ctx.getEnvironment() — w IntelliJ w debuggerze zobaczysz, które źródło dało wartość.
    //
    // Dlaczego w lekcji pliki nazywają się cfg04.* a nie application.*? Wtedy czytane są tylko przez tę lekcję
    // (Cfg04Application ustawia spring.config.name=cfg04). Gdyby w module leżał wspólny application.yml,
    // dotyczyłby WSZYSTKICH lekcji, bo mają jedną ścieżkę klas.

    // =================================================================================================
    // 5. PUŁAPKI SKŁADNI YAML
    // =================================================================================================
    //
    // a) Tabulator we wcięciu → błąd składni. SnakeYAML zgłasza: "found character '\t(TAB)' that cannot start any
    //    token. (Do not use \t(TAB) for indentation)". Test sekcji 5.
    // b) Złe wcięcie NIE musi dać błędu — tylko zmienia klucz:
    //        cfg04:
    //          shop:
    //          name: X          ← name stoi na poziomie shop, więc klucz to cfg04.name, a nie cfg04.shop.name
    //    Aplikacja startuje, a wartość „nie dociera”. Test sekcji 5 pokazuje właśnie ten przypadek.
    // c) Wartości logiczne w starym stylu YAML 1.1: yes/no/on/off (oraz true/false) są BOOLEAN-ami — w trzech zapisach:
    //    małymi literami, z wielkiej litery albo WIELKIMI (no, No, NO; on, On, ON…). Zapis mieszany (nO, oFF) oraz
    //    pojedyncze litery y/n zostają tekstem.
    //        country: NO   → false (np. kod Norwegii!)     version: 1.10 → liczba 1.1 (traci zero)
    //    Test sekcji 6: pole country dostaje tekst "false". Cudzysłów ("NO", "1.10") zawsze daje tekst.
    // d) Dwukropek lub # w wartości: napis z ": " albo " #" ujmij w cudzysłów.
    //
    // DOBRA PRAKTYKA: wartości, które mają być TEKSTEM, a wyglądają na liczbę, datę lub wartość logiczną (kody
    //   pocztowe, kody krajów, numery wersji) zawsze w cudzysłowie. Dlaczego? Bez cudzysłowu parser YAML sam
    //   zgaduje typ i bywa to zgadywanie niezgodne z Twoim zamiarem — a błąd ujawnia się dopiero w danych.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • YAML = te same klucze co .properties, zapisane hierarchicznie (wcięcie 2 spacje, "klucz: wartość").
     *   • Tylko spacje, nigdy tabulatory; wcięcie jest znaczące — zła głębokość cicho zmienia klucz.
     *   • Lista: pozycje "- element" albo [a, b]; po spłaszczeniu klucz[0], klucz[1], …
     *   • Wiele dokumentów: separator ---; warunek: spring.config.activate.on-profile: profil — dotyczy CAŁEGO dokumentu.
     *   • Stary zapis spring.profiles: profil w Boot 4 = błąd startu (InvalidConfigDataPropertyException).
     *   • Oba pliki w tym samym miejscu: dla tego samego klucza wygrywa .properties, nie .yml.
     *   • yes/no/on/off (np. no, No, NO) to boolean w YAML 1.1; tekst, który wygląda na liczbę/boolean — w cudzysłów.
     *   • YAML czytany jako UTF-8 (polskie litery OK); .properties jako ISO-8859-1.
     *   • W projekcie trzymaj jeden format.
     *
     * PYTANIA KONTROLNE:
     *   1. Jak zapiszesz w YAML klucz a.b.c=1 i listę a.d[0]=x, a.d[1]=y?
     *   2. Co się stanie? W application.properties i application.yml jest ten sam klucz z różnymi wartościami.
     *   3. Co się stanie? W YAML wpisujesz we wcięciu tabulator.
     *   4. Co się stanie? W YAML masz country: NO i wiążesz do pola String.
     *   5. ZNAJDŹ BŁĄD: w dokumencie profilu napisano spring.profiles: promo — co jest nie tak w Bootcie 4?
     *   6. ZNAJDŹ BŁĄD: pod cfg04: stoją shop: i name: X z TYM SAMYM wcięciem (dwie spacje) — dlaczego
     *      @ConfigurationProperties(prefix = "cfg04.shop") nie widzi nazwy?
     *   7. Dlaczego ustawienia tej lekcji leżą w cfg04.yml, a nie w application.yml?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Cfg04YamlExercisesTest (obrabiają tekst YAML tą samą ładowarką, której używa Boot)
    // =================================================================================================

    /**
     * Zepsuty YAML do ćwiczenia 3: {@code user} i {@code pool} stoją o jeden poziom za płytko
     * (powinny być wewnątrz {@code db}).
     */
    static final String BROKEN_YAML = """
            cfg04:
              exercise:
                db:
                  url: jdbc:h2:mem:cfg04
                user: sa
                pool:
                  max: 5
            """;

    /**
     * ĆWICZENIE 1 (łatwe): zapisz w YAML-u (tekstem) te ustawienia z .properties:
     * {@code cfg04.exercise.name=Kawiarnia}, {@code cfg04.exercise.tables=12}, {@code cfg04.exercise.open=true},
     * {@code cfg04.exercise.menu[0]=kawa}, {@code cfg04.exercise.menu[1]=herbata}.
     * Podpowiedź: dwie spacje wcięcia, lista z myślnikami, bez tabulatorów.
     */
    static String exercise1() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): YAML z dwoma dokumentami. Pierwszy: {@code cfg04.exercise.greeting: Witaj}.
     * Drugi, tylko dla profilu {@code cfg04-promo}: {@code cfg04.exercise.greeting: Promocja!}.
     * Podpowiedź: separator {@code ---} i klucz {@code spring.config.activate.on-profile}.
     */
    static String exercise2() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): popraw {@link #BROKEN_YAML} tak, by klucze brzmiały
     * {@code cfg04.exercise.db.url}, {@code cfg04.exercise.db.user} i {@code cfg04.exercise.db.pool.max}.
     * Zwróć poprawiony tekst. Podpowiedź: zmień tylko wcięcia.
     */
    static String exercise3() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. a:\n  b:\n    c: 1\n  d:\n    - x\n    - y   (albo d: [x, y]). Sprawdź w sekcji 1 i 2, jak to się spłaszcza.
     *   2. Wygrywa wartość z .properties — plik .yml jest ignorowany dla tego klucza. Sprawdzone testem (sekcja 4).
     *   3. Błąd składni przy starcie — SnakeYAML: "found character '\t(TAB)' that cannot start any token.
     *      (Do not use \t(TAB) for indentation)", czyli znak TAB nie może być wcięciem. Sprawdzone testem (sekcja 5).
     *   4. Pole dostaje tekst "false": YAML 1.1 czyta NO jako wartość logiczną, a nie tekst.
     *      Sprawdzone testem (sekcja 6). Napraw: country: "NO".
     *   5. Zapis spring.profiles w dokumencie pochodzi sprzed Boota 2.4 i Boot 4 go odrzuca: aplikacja NIE wystartuje,
     *      błąd to InvalidConfigDataPropertyException: Property 'spring.profiles' imported from location '…'
     *      is invalid and should be replaced with 'spring.config.activate.on-profile'. Napraw:
     *      spring.config.activate.on-profile: promo. Sprawdzone testem (sekcja 3).
     *   6. name stoi na tym samym poziomie co shop, więc klucz to cfg04.name, a nie cfg04.shop.name. Aplikacja startuje
     *      bez błędu, a pole name zostaje null. Sprawdzone testem (sekcja 5). Napraw: wciąć name o dwie spacje bardziej.
     *   7. Pliki nazwane cfg04.* czyta tylko ta lekcja (spring.config.name=cfg04). Wspólny application.yml
     *      obowiązywałby wszystkie lekcje modułu naraz, bo mają jedną ścieżkę klas.
     */
    // </editor-fold>
}
