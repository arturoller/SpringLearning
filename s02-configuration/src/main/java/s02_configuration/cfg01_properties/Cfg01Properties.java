package s02_configuration.cfg01_properties;

import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;

/**
 * <pre>
 * TEMAT: application.properties i @Value — ustawienia poza kodem
 *        (properties = właściwości, ustawienia w postaci klucz=wartość; configuration = konfiguracja;
 *         property source = źródło ustawień; placeholder = symbol zastępczy ${...})
 *
 * W SKRÓCIE:
 *   Adresy, limity i nazwy nie powinny siedzieć w kodzie — inne są na Twoim komputerze, inne w testach, inne na
 *   produkcji. Spring Boot czyta je z pliku application.properties (i kilku innych miejsc) i pozwala wstrzyknąć
 *   do beana adnotacją @Value("${klucz}"). Gdy ten sam klucz jest w kilku miejscach, wygrywa miejsce o WYŻSZYM
 *   priorytecie — dzięki temu na serwerze można nadpisać wartość z pliku bez przebudowy aplikacji.
 *
 * ANALOGIA: nastawy w pralce.
 *   Kod to pralka — mechanizm zawsze ten sam. Ustawienia to pokrętła: temperatura, obroty. Fabryka daje nastawy
 *   domyślne (plik w paczce), Ty możesz przekręcić pokrętło na chwilę (argument przy uruchomieniu), a nikt nie musi
 *   rozkręcać pralki, żeby zmienić temperaturę.
 *
 * JAK TO DZIAŁA:
 *   1. SpringApplication.run buduje Environment (środowisko = zbiór źródeł ustawień, uporządkowany wg priorytetu).
 *   2. Boot dokłada do niego źródła: argumenty, właściwości JVM, zmienne środowiska, pliki application*.properties.
 *   3. Przy tworzeniu beana Spring widzi @Value("${klucz}"), pyta Environment o wartość, zamienia tekst na typ
 *      parametru (int, boolean, Duration…) i wywołuje konstruktor.
 *   PRIORYTET (wyższy wygrywa; skrót wyboru z dokumentacji Boota):
 *      właściwości w teście (@SpringBootTest(properties=...))  →  argumenty wiersza poleceń (--klucz=wartość)
 *      →  właściwości JVM (java -Dklucz=wartość)  →  zmienne środowiska systemu operacyjnego
 *      →  pliki application-{profil}.properties  →  application.properties  →  wartość domyślna z kodu.
 *
 * SŁÓWKA:
 *   value = wartość; default value = wartość domyślna; placeholder = symbol zastępczy ${klucz};
 *   relaxed binding = luźne dopasowanie nazw (max-items = maxItems = max_items); command-line argument =
 *   argument wiersza poleceń; system property = właściwość systemowa JVM; environment variable = zmienna
 *   środowiska; override = nadpisać; precedence = pierwszeństwo, priorytet; conversion = konwersja typu.
 *
 * ZOBACZ TEŻ: t18_io_files/Io06Properties (klasa Properties z samej Javy — to samo, ale ręcznie),
 *             t30_build_modules/Build05ProcessesEnv (zmienne środowiska w Javie),
 *             s01_core_ioc/Ioc02ConstructorInjection (wstrzykiwanie przez konstruktor),
 *             s02_configuration/Cfg02ConfigurationProperties (następny krok: ustawienia w rekordzie),
 *             s02_configuration/Cfg05Environment (interfejs Environment).
 * </pre>
 */
public final class Cfg01Properties {

    private Cfg01Properties() {
    }

    // =================================================================================================
    // 1. PLIK application.properties — gdzie leży i jak wygląda
    // =================================================================================================
    //
    // Plik leży w src/main/resources (po zbudowaniu: w korzeniu ścieżki klas, czyli classpath). Format:
    //     cfg01.shop.name=Sklep Spring        ← klucz=wartość (znak # zaczyna komentarz)
    //     cfg01.shop.max-items=10             ← klucze pisze się małymi literami z myślnikami (kebab-case)
    // Kropki w kluczu tylko grupują nazwy — w lekcji Cfg02 zamienią się w zagnieżdżone obiekty.
    // W tym module ustawienia lekcji leżą w application-cfg01.properties: Boot wczytuje ten plik, gdy aktywny jest
    // profil "cfg01" (profile — lekcja Cfg03Profiles). Test sekcji 1 pokazuje, że wartości z pliku trafiają do beana.
    //
    // DOBRA PRAKTYKA: klucze z prefiksem własnej aplikacji (shop.…, cfg01.…) — nigdy gołe "name" czy "url".
    //   Dlaczego? Spring i biblioteki mają setki własnych kluczy (server.port, spring.datasource.url…). Własny
    //   prefiks chroni przed przypadkowym nadpisaniem cudzego ustawienia i od razu pokazuje, czyj to klucz.

    // PUŁAPKA: polskie litery w wartości w pliku .properties ("Łódź") wychodzą jako krzaczki.
    //   Dlaczego? Boot czyta pliki .properties w kodowaniu ISO-8859-1, a edytor zapisał je jako UTF-8: każda polska
    //   litera (2 bajty) staje się dwoma dziwnymi znakami. Sprawdzone testem (sekcja 1). Wyjścia: (1) wartości bez
    //   polskich liter, (2) kody unicode w pliku — ukośnik wsteczny, litera u i cztery cyfry szesnastkowe (np. ó
    //   to u00f3 po ukośniku; przykłady: plik application-cfg02.properties), (3) format YAML, który jest czytany
    //   jako UTF-8 (lekcja Cfg04Yaml).

    // =================================================================================================
    // 2. @Value("${klucz}") — wstrzyknięcie jednej wartości (ShopInfo)
    // =================================================================================================
    //
    // public ShopInfo(@Value("${cfg01.shop.name}") String name, ...)
    //   ${...} to placeholder (symbol zastępczy): Spring podmienia go na wartość z Environment, ZANIM zamieni tekst
    //   na typ parametru. Brak klucza i brak wartości domyślnej → aplikacja NIE wystartuje (błąd:
    //   "Could not resolve placeholder 'cfg01.shop.name'"). To dobrze: błąd widać od razu, a nie po tygodniu na produkcji.
    //
    // PUŁAPKA: @Value("cfg01.shop.name") BEZ ${ } wstrzyknie dosłowny tekst "cfg01.shop.name", a nie wartość z pliku.
    //   Dlaczego? Bez klamer Spring nie ma czego podmieniać — traktuje napis jak zwykły literał. Błędu nie będzie.
    //
    // PUŁAPKA: @Value na polu, którego używasz w konstruktorze — pole jest tam jeszcze null.
    //   Dlaczego? Kolejność: (1) Spring wywołuje konstruktor, (2) DOPIERO POTEM wstrzykuje pola. Zobacz sekcję 7.

    // =================================================================================================
    // 3. WARTOŚĆ DOMYŚLNA — ${klucz:domyślna}
    // =================================================================================================
    //
    // @Value("${cfg01.shop.slogan:Wszystko dla programisty}") — po dwukropku wartość używana, gdy klucza nie ma.
    // Pusta wartość domyślna też jest dozwolona: ${klucz:} daje pusty String.
    //
    // DOBRA PRAKTYKA: wartość domyślna tylko dla ustawień NAPRAWDĘ opcjonalnych (jak slogan, newsletter).
    //   Dla ustawień, bez których aplikacja nie ma sensu (adres bazy, klucz do usługi) NIE dawaj wartości domyślnej.
    //   Dlaczego? Ciche „domyślne” hasło albo adres na produkcji jest gorsze od głośnego błędu przy starcie.

    // =================================================================================================
    // 4. KONWERSJA TYPÓW — tekst z pliku zamienia się na int, boolean, Duration
    // =================================================================================================
    //
    // W pliku wszystko jest tekstem. Spring zamienia go na typ parametru:
    //   "10" → int,   "true" → boolean,   "30s" → java.time.Duration (30 sekund).
    // Duration rozumie krótkie zapisy: 500ms, 30s, 5m, 2h, 1d (a także ISO: PT30S).
    //
    // UWAGA (sprawdzone testem): ta wygoda pochodzi ze Spring BOOTA (ApplicationConversionService). W „gołym” Springu
    // (new AnnotationConfigApplicationContext, ApplicationContextRunner) wstrzyknięcie do Duration kończy się
    // błędem "no matching editors or conversion strategy found" — nawet dla zapisu ISO "PT7S".
    //
    // PUŁAPKA: tekst, którego nie da się zamienić ("dużo" na int), kończy start aplikacji wyjątkiem konwersji.
    //   Dlaczego? Konwersja dzieje się przy tworzeniu beana, więc błąd jest od razu, a nie przy pierwszym użyciu.
    //
    // PUŁAPKA: liczba BEZ jednostki jako Duration (cfg01.shop.reservation-time=30) to 30 MILISEKUND, nie 30 sekund.
    //   Dlaczego? Gdy w tekście nie ma jednostki, konwersja Boota przyjmuje milisekundy. Błędu nie ma — rezerwacja
    //   po prostu trwa ułamek sekundy. Sprawdzone testem (sekcja 4). Zawsze pisz jednostkę: 30s.

    // =================================================================================================
    // 5. LUŹNE DOPASOWANIE NAZW (relaxed binding) — podstawy
    // =================================================================================================
    //
    // W @Value zapisz klucz w formie KANONICZNEJ: małe litery i myślniki: ${cfg01.shop.max-items}.
    // Wtedy Boot znajdzie wartość także pod nazwami: cfg01.shop.maxItems, cfg01.shop.max_items
    // oraz (zmienna środowiska) CFG01_SHOP_MAXITEMS albo CFG01_SHOP_MAX_ITEMS (szczegóły: lekcja Cfg05Environment).
    //
    // PUŁAPKA: w @Value zapisujesz ${cfg01.shop.maxItems} (camelCase — zapis „wielbłądzi”: kolejne słowa sklejone,
    //   każde od wielkiej litery) — wtedy NIE znajdzie klucza max-items.
    //   Dlaczego? Luźne dopasowanie działa w jedną stronę: Boot przeszukuje źródła, porównując z formą kanoniczną
    //   zapisaną w adnotacji. Kanoniczny zapis (kebab-case) to jedyny, który obejmuje wszystkie warianty.
    //   Sprawdzone testem (sekcja 5): ${cfg01.shop.maxItems} nie widzi klucza zapisanego jako max-items.

    // =================================================================================================
    // 6. PRIORYTET ŹRÓDEŁ — kto wygrywa, gdy klucz jest w kilku miejscach
    // =================================================================================================
    //
    // Test sekcji 6 uruchamia tę samą aplikację trzy razy i czyta cfg01.shop.name:
    //   - sam plik                                   → "Sklep Spring"
    //   - plus właściwość JVM (-Dcfg01.shop.name=..)  → wartość z JVM (nadpisuje plik)
    //   - plus argument --cfg01.shop.name=..          → wartość z argumentu (nadpisuje i JVM, i plik)
    // A test klasy ma jeszcze @SpringBootTest(properties = ...), które wygrywa nad JVM i plikiem (cfg01.shop.vat-percent).
    // Zmienne środowiska systemu operacyjnego leżą POMIĘDZY właściwościami JVM a plikiem — nie ustawiamy ich w testach
    // (byłyby zależne od komputera), ale w lekcji Cfg05Environment symulujemy je bez dotykania prawdziwego systemu.
    //
    // PYTANIE REKRUTACYJNE: „Jak zmienić port/ustawienie na serwerze bez przebudowy aplikacji?” — argumentem
    //   --klucz=wartość, właściwością -Dklucz=wartość albo zmienną środowiska (typowo w kontenerze Dockera).
    //
    // DOBRA PRAKTYKA: w pliku trzymaj wartości DOMYŚLNE i bezpieczne (rozwojowe); wartości środowiska (adresy,
    //   hasła) podawaj z zewnątrz zmiennymi środowiska. Dlaczego? Ten sam plik JAR działa wszędzie, a sekrety
    //   nie trafiają do repozytorium.

    // =================================================================================================
    // 7. PUŁAPKI @Value — pole w konstruktorze i pole statyczne
    // =================================================================================================
    //
    // PRZED (błąd): FieldInjectionDemo — @Value na polu i odczyt tego pola w konstruktorze → null.
    // PO (poprawnie): wartość jako PARAMETR konstruktora (jak w ShopInfo).
    // Pole STATYCZNE z @Value: Spring go nie wstrzyknie (adnotacje wstrzykiwania działają na obiekcie, a pole statyczne
    // należy do klasy) — pole zostaje null. W logu nie ma ostrzeżenia (WARN), jest tylko łatwy do przeoczenia wpis
    // na poziomie INFO: "Autowired annotation is not supported on static fields: …". Sprawdzone testem (sekcja 7).

    /** PUŁAPKA w kodzie: pole wstrzykiwane po konstruktorze. Nie jest beanem — test rejestruje ją ręcznie. */
    static class FieldInjectionDemo {
        @Value("${cfg01.shop.name}")
        String name;                                  // wstrzyknięte PO konstruktorze

        final String nameSeenInConstructor;

        FieldInjectionDemo() {
            this.nameSeenInConstructor = name;        // tu name jest jeszcze null!
        }
    }

    /** PUŁAPKA w kodzie: @Value na polu statycznym nie działa. */
    static class StaticFieldDemo {
        @Value("${cfg01.shop.name}")
        static String name;                           // zostanie null
    }

    // =================================================================================================
    // 8. NIEPOPRAWNY KLUCZ — głośny błąd zamiast cichej pomyłki
    // =================================================================================================
    //
    // Bean z @Value("${cfg01.brak.takiego.klucza}") bez wartości domyślnej nie pozwoli kontekstowi wystartować.
    // Test sekcji 8 sprawdza, że komunikat wskazuje brakujący klucz.
    // UWAGA (sprawdzone testem): ta „twardość” pochodzi z autokonfiguracji Boota (PropertySourcesPlaceholderConfigurer).
    // W gołym Springu nierozwiązany placeholder jest po cichu zostawiany jako tekst "${klucz}" i kontekst startuje.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Pliki .properties są czytane jako ISO-8859-1 — polskie litery zapisuj kodami unicode albo użyj YAML.
     *   • application.properties (klucz=wartość) leży w src/main/resources; ustawienia lekcji: application-cfg01.properties.
     *   • @Value("${klucz}") — wartość z Environment; @Value("${klucz:domyślna}") — z wartością domyślną.
     *   • Wstrzykuj przez PARAMETR konstruktora (pola final); nie przez pole, jeśli użyjesz go w konstruktorze.
     *   • Konwersja tekstu na int/boolean/Duration ("30s") działa dzięki Spring Bootowi; w gołym Springu nie ma Duration.
     *   • Duration bez jednostki ("30") = milisekundy — zawsze pisz jednostkę (30s).
     *   • W @Value pisz klucz kanonicznie (kebab-case), wtedy luźne dopasowanie obejmuje camelCase i zmienne środowiska.
     *   • Priorytet: test properties > argumenty > właściwości JVM > zmienne środowiska > pliki > wartość domyślna.
     *   • Brak klucza bez wartości domyślnej = błąd przy starcie (i bardzo dobrze).
     *   • @Value na polu statycznym nie działa.
     *
     * PYTANIA KONTROLNE:
     *   1. Który ma wyższy priorytet: argument --klucz=wartość czy właściwość JVM -Dklucz=wartość?
     *   2. Co się stanie? Bean ma @Value("${cfg01.brak:}") String x — a klucza nie ma w żadnym źródle.
     *   3. Co się stanie? W konstruktorze odczytujesz pole oznaczone @Value (wstrzykiwane przez pole).
     *   4. ZNAJDŹ BŁĄD: @Value("cfg01.shop.name") String name — dlaczego name nie zawiera "Sklep Spring"?
     *   5. ZNAJDŹ BŁĄD: @Value("${cfg01.shop.maxItems}") przy kluczu max-items w pliku — dlaczego aplikacja nie wystartuje?
     *   6. Dlaczego ustawień nie wpisuje się na stałe w kodzie?
     *   7. Dlaczego dla adresu bazy danych nie warto dawać wartości domyślnej w ${klucz:domyślna}?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Cfg01PropertiesExercisesTest (klasy szkieletowe poniżej NIE są beanami — test rejestruje je ręcznie)
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): niech {@code Greeter} dostanie nazwę sklepu z klucza {@code cfg01.shop.name}, a metoda
     * {@code greet()} zwraca "Witaj w " + nazwa + "!". Dopisz tylko adnotację przy parametrze konstruktora.
     * Podpowiedź: {@code @Value("${...}")} stoi PRZED typem parametru.
     */
    static class Greeter {
        private final String shopName;

        Greeter(String shopName) {
            // TODO: twoje rozwiązanie (dopisz adnotację przy parametrze konstruktora)
            this.shopName = shopName;
        }

        String greet() {
            return "Witaj w " + shopName + "!";
        }
    }

    /**
     * ĆWICZENIE 2 (średnie): ustaw klucze {@code cfg01.exercise.retries} (int, domyślnie 3),
     * {@code cfg01.exercise.timeout} (Duration, domyślnie 5s) i {@code cfg01.exercise.enabled} (boolean, domyślnie true).
     * Podpowiedź: wartość domyślna po dwukropku; "5s" jako Duration zamieni Boot (test używa jego konwersji).
     */
    static class Limits {
        private final int retries;
        private final Duration timeout;
        private final boolean enabled;

        Limits(int retries, Duration timeout, boolean enabled) {
            // TODO: twoje rozwiązanie (dopisz adnotacje przy parametrach konstruktora)
            this.retries = retries;
            this.timeout = timeout;
            this.enabled = enabled;
        }

        int retries() {
            return retries;
        }

        Duration timeout() {
            return timeout;
        }

        boolean enabled() {
            return enabled;
        }
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): ZNAJDŹ BŁĄD. {@code Counter} ma odczytać limit z klucza {@code cfg01.exercise.limit}
     * i w konstruktorze obliczyć {@code doubled = limit * 2}, ale kontekst się nie uruchamia (albo liczy złe wartości).
     * Napraw klasę tak, by limit wchodził przez PARAMETR konstruktora. Podpowiedź: sekcja 7 lekcji.
     */
    static class Counter {
        @Value("${cfg01.exercise.limit}")
        Integer limit;                                // wstrzykiwane po konstruktorze

        final int doubled;

        Counter() {
            // TODO: twoje rozwiązanie (przenieś wartość do parametru konstruktora)
            this.doubled = limit * 2;
        }
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Argument --klucz=wartość. Sprawdzone testem (sekcja 6): argument wygrywa z właściwością JVM, a ta z plikiem.
     *   2. Nic złego: ${klucz:} ma pustą wartość domyślną, więc x to pusty String "". Sprawdzone testem (sekcja 3).
     *   3. Dostaniesz null (a przy użyciu go np. NullPointerException): Spring najpierw woła konstruktor, a pola
     *      wstrzykuje dopiero potem. Sprawdzone testem (sekcja 7).
     *   4. Brak ${ } — Spring wstrzyknął dosłowny tekst "cfg01.shop.name". Napraw: @Value("${cfg01.shop.name}").
     *      Sprawdzone testem (sekcja 2).
     *   5. W @Value jest camelCase, a luźne dopasowanie działa od formy kanonicznej (kebab-case) — klucz nie zostanie
     *      znaleziony, więc start kończy się błędem "Could not resolve placeholder". Pisz ${cfg01.shop.max-items}.
     *      Sprawdzone testem (sekcja 5).
     *   6. Bo inne wartości są w testach, na komputerze programisty i na produkcji; zmiana wartości nie powinna wymagać
     *      zmiany kodu i nowego wydania, a sekretów nie wolno trzymać w repozytorium.
     *   7. Bo cicha wartość domyślna (np. localhost) na produkcji podłączy aplikację do złej bazy bez żadnego błędu;
     *      lepiej, gdy aplikacja głośno odmówi startu.
     */
    // </editor-fold>
}
