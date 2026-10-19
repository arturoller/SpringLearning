package s03_boot_basics.boot02_writing_auto_configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <pre>
 * TEMAT: Własna autokonfiguracja — „biblioteka sklepu”, która konfiguruje się sama
 *        (auto-configuration = autokonfiguracja; library = biblioteka; starter = zestaw startowy zależności;
 *         ordering = kolejność; registration = rejestracja)
 *
 * W SKRÓCIE:
 *   Piszesz bibliotekę używaną przez wiele aplikacji (tu: ShopGreeter — powitanie klienta). Chcesz, żeby
 *   użytkownik dodał zależność i od razu dostał gotowy bean — tak jak dostaje JsonMapper od Boota.
 *   Potrzebujesz czterech rzeczy: klasy biblioteki (bez Springa), rekordu ustawień (@ConfigurationProperties),
 *   klasy @AutoConfiguration z warunkami i wpisu w pliku META-INF/spring/…AutoConfiguration.imports.
 *   Dobra autokonfiguracja jest „grzeczna”: wycofuje się przed beanem użytkownika, działa tylko wtedy, gdy ma sens,
 *   i nie zmienia zachowania aplikacji, która o niej nie wie.
 *
 * ANALOGIA: mebel z instrukcją montażu w paczce.
 *   Biblioteka to mebel, autokonfiguracja to instrukcja montażu włożona do paczki: „jeśli w pokoju nie ma już
 *   szafy — złóż tę”, „jeśli masz ściankę z płyt — użyj kołków z woreczka B”. Plik .imports to naklejka na
 *   kartonie „w środku instrukcja” — bez niej nikt jej nie znajdzie. A klient, który ma własną szafę,
 *   instrukcji po prostu nie wykonuje.
 *
 * JAK TO DZIAŁA:
 *   1. ShopGreeter — klasa biblioteki; ShopGreeterProperties — rekord z ustawieniami s03.greeter.*.
 *   2. ShopGreeterAutoConfiguration (@AutoConfiguration) — tworzy bean ShopGreeter, gdy spełnione są warunki:
 *      @ConditionalOnClass(ShopGreeter.class), @ConditionalOnBooleanProperty("s03.greeter.enabled"),
 *      a na metodzie @Bean — @ConditionalOnMissingBean.
 *   3. Plik src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 *      zawiera pełną nazwę tej klasy — tak Boot ją znajduje (lekcja Boot01AutoConfiguration, sekcja 2).
 *   4. Boot02Application włącza profil boot02, a plik application-boot02.properties — s03.greeter.enabled=true.
 *   5. Testy: ApplicationContextRunner + AutoConfigurations.of(ShopGreeterAutoConfiguration.class).
 *
 * SŁÓWKA:
 *   greeter = witający; farewell = pożegnanie; enabled = włączony; default value = wartość domyślna;
 *   after/before = po/przed; filtered class loader = ładowacz klas z filtrem (udaje brak klasy);
 *   context runner = uruchamiacz kontekstu; proxy bean methods = pośredniczenie w metodach @Bean;
 *   optional dependency = zależność opcjonalna; metadata = metadane (dane o danych).
 *
 * ZOBACZ TEŻ: s03_boot_basics/Boot01AutoConfiguration (jak Boot ładuje i ocenia autokonfiguracje),
 *             s02_configuration/Cfg02ConfigurationProperties (rekord @ConfigurationProperties),
 *             s02_configuration/Cfg05Environment (@ConditionalOnProperty i @ConditionalOnBooleanProperty),
 *             s01_core_ioc/Ioc04BeanMethods (metody @Bean),
 *             s03_boot_basics/Boot04Starters (startery — „paczka” z biblioteką i autokonfiguracją),
 *             t30_build_modules/Build03Modules (podział projektu na moduły).
 * </pre>
 */
public final class Boot02WritingAutoConfiguration {

    private Boot02WritingAutoConfiguration() {
    }

    // =================================================================================================
    // 1. KLASA @AutoConfiguration I JEJ WARUNKI — domyślnie wyłączona
    // =================================================================================================
    //
    //   @AutoConfiguration                                    ← „to autokonfiguracja” (od Boota 2.7)
    //   @ConditionalOnClass(ShopGreeter.class)                ← biblioteka jest na ścieżce klas
    //   @ConditionalOnBooleanProperty("s03.greeter.enabled")  ← s03.greeter.enabled=true; BRAK klucza = wyłączona
    //   @EnableConfigurationProperties(ShopGreeterProperties.class)   ← zarejestruj rekord ustawień
    //   public class ShopGreeterAutoConfiguration {
    //       @Bean
    //       @ConditionalOnMissingBean                          ← tylko gdy użytkownik nie dał własnego ShopGreeter
    //       ShopGreeter shopGreeter(ShopGreeterProperties properties) { … }
    //   }
    //
    // @AutoConfiguration = @Configuration(proxyBeanMethods = false) + miejsce na kolejność (before/after, sekcja 4).
    // proxyBeanMethods = false: Spring nie opakowuje klasy pośrednikiem (proxy), więc wywołanie jednej metody @Bean
    // z drugiej utworzyłoby NOWY obiekt. W autokonfiguracjach zależności bierz z parametrów metod @Bean (jak tu
    // ShopGreeterProperties), nigdy przez wywołanie innej metody @Bean.
    //
    // @ConditionalOnBooleanProperty (od Boota 3.5) ma domyślnie matchIfMissing = false: brak klucza = brak beana.
    // Wartość porównywana z "true" — "tak" NIE włącza (test sekcji 1). @EnableConfigurationProperties stoi na
    // klasie z warunkami, więc gdy autokonfiguracja jest wyłączona, nie powstaje nawet bean z ustawieniami.
    //
    // DOBRA PRAKTYKA: autokonfiguracja nie może zmieniać zachowania aplikacji, która o niej nie wie.
    //   Funkcje ryzykowne albo kosztowne (wysyłka maili, połączenia sieciowe, zadania w tle) włączaj dopiero jawnym
    //   ustawieniem — tak jak Boot robi z JMX (spring.jmx.enabled, lekcja Boot01). Dlaczego? Jar biblioteki może
    //   trafić na ścieżkę klas pośrednio (jako zależność innej zależności), a aplikacja nagle zacznie robić coś,
    //   czego nikt nie zamawiał. W tym kursie jest też drugi powód — sekcja 7.

    // =================================================================================================
    // 2. USTAWIENIA — rekord @ConfigurationProperties z @DefaultValue
    // =================================================================================================
    //
    //   @ConfigurationProperties(prefix = "s03.greeter")
    //   public record ShopGreeterProperties(@DefaultValue("false") boolean enabled,
    //                                       @DefaultValue("SpringLearning") String shopName,
    //                                       @DefaultValue("Dzień dobry") String greeting) { }
    //
    // Ustawienie s03.greeter.shop-name trafia do pola shopName (luźne dopasowanie nazw — relaxed binding).
    // @DefaultValue podaje wartość, gdy klucza brak — bez niej String byłby null, a int — 0.
    // Test sekcji 2: bez ustawień „Dzień dobry, Jan Kowalski! Witamy w sklepie SpringLearning.”,
    // z ustawieniami — wartości z pliku.
    //
    // PUŁAPKA: polskie znaki w wartościach w pliku .properties zapisanym w UTF-8 (shop-name=SpringLearning Kraków).
    //   Dlaczego? Boot czyta pliki .properties w kodowaniu ISO-8859-1, więc „ó” zamienia się w „Ã³” (sprawdzone
    //   uruchomieniem). Wartości z polskimi znakami trzymaj w YAML (czytany jako UTF-8) albo w kodzie (@DefaultValue).
    //
    // Dla wygody użytkownika biblioteki dodaje się zwykle procesor adnotacji spring-boot-configuration-processor:
    // przy kompilacji tworzy plik META-INF/spring-configuration-metadata.json (opis kluczy s03.greeter.*), z którego
    // IDE z obsługą Springa biorą podpowiedzi w application.properties. (Tu go nie dodajemy — to tylko wygoda,
    // nie warunek działania.)

    // =================================================================================================
    // 3. „GRZECZNOŚĆ” — bean użytkownika wygrywa, brak biblioteki = nic się nie dzieje
    // =================================================================================================
    //
    // Test sekcji 3:
    //   • użytkownik ma własny @Bean ShopGreeter myGreeter() → w kontekście jest tylko "myGreeter", choć
    //     s03.greeter.enabled=true i s03.greeter.greeting=Witaj. Ustawienia biblioteki jego beana nie dotyczą;
    //   • FilteredClassLoader ukrywa klasę ShopGreeter → @ConditionalOnClass nie pasuje → brak beana.
    //     W prawdziwej bibliotece moduł z autokonfiguracją ma bibliotekę jako zależność OPCJONALNĄ
    //     (<optional>true</optional>), więc ten warunek sprawdza, czy użytkownik naprawdę ją dołączył.
    //
    // PUŁAPKA: @ConditionalOnMissingBean na zwykłej klasie @Configuration użytkownika (nie autokonfiguracji).
    //   Dlaczego? Warunek widzi tylko beany zarejestrowane DO TEJ PORY; w zwykłej konfiguracji kolejność zależy
    //   od skanowania i może „nie zauważyć” beana z innej klasy. Dokumentacja tej adnotacji zaleca używać jej
    //   tylko w autokonfiguracjach (sekcja 5 pokazuje skutek).

    // =================================================================================================
    // 4. KOLEJNOŚĆ — @AutoConfiguration(after = …) / (before = …)
    // =================================================================================================
    //
    // Boot ustawia autokonfiguracje tak: najpierw ALFABETYCZNIE po pełnej nazwie klasy, potem według
    // @AutoConfigureOrder, a na końcu respektuje before/after (kod klasy AutoConfigurationSorter).
    // Kiedy kolejność ma znaczenie? Gdy Twój warunek pyta o bean z INNEJ autokonfiguracji
    // (@ConditionalOnBean / @ConditionalOnMissingBean). Warunek widzi tylko to, co już zarejestrowano.
    //
    // Test sekcji 4: autokonfiguracja „raportu powitań” z @ConditionalOnBean(ShopGreeter.class):
    //   • bez after: jej nazwa (…Boot02WritingAutoConfigurationTest$Report…) jest alfabetycznie PRZED
    //     …ShopGreeterAutoConfiguration, więc warunek liczy się, zanim ShopGreeter powstanie → raportu NIE ma;
    //   • z @AutoConfiguration(after = ShopGreeterAutoConfiguration.class) → raport jest.
    // Gdy klasy, po której chcesz się ustawić, może nie być na ścieżce klas — użyj afterName = "pełna.nazwa".
    //
    // PUŁAPKA: „u mnie działa” dzięki kolejności alfabetycznej. Dlaczego to groźne? Po zmianie nazwy klasy albo
    //   pakietu kolejność się odwraca i bean po cichu znika. Zależność od innej autokonfiguracji zawsze zapisuj
    //   jawnie (after/before).

    // =================================================================================================
    // 5. REJESTRACJA, A NIE SKANOWANIE — dlaczego?
    // =================================================================================================
    //
    // ShopGreeterAutoConfiguration leży w pakiecie skanowanym przez Boot02Application, a mimo to skanowanie jej
    // NIE bierze: @SpringBootApplication ma filtr AutoConfigurationExcludeFilter, który pomija klasy z adnotacją
    // @AutoConfiguration albo wpisane do pliku .imports (Boot01AutoConfiguration, sekcja 1). Powody:
    //   1) KOLEJNOŚĆ: autokonfiguracje muszą być przetwarzane PO konfiguracji użytkownika. Test sekcji 5 wczytuje
    //      ShopGreeterAutoConfiguration jak zwykłą konfigurację, przed konfiguracją użytkownika — i w kontekście są
    //      DWA beany ShopGreeter ("shopGreeter" i "myGreeter"): @ConditionalOnMissingBean policzył się za wcześnie.
    //      Ta sama para wczytana prawidłowo (AutoConfigurations.of) daje jeden bean — użytkownika.
    //   2) ZASIĘG: pakiety biblioteki (np. com.acme.shop.autoconfigure) i tak nie leżą pod pakietem aplikacji
    //      użytkownika — skanowanie „działałoby” tylko przypadkiem.
    //   3) KONTROLA: użytkownik może wyłączyć tylko kandydata z listy .imports (exclude — Boot01, sekcja 6).
    //
    // PUŁAPKA: w teście piszesz withUserConfiguration(MojaAutoKonfiguracja.class) zamiast
    //   withConfiguration(AutoConfigurations.of(MojaAutoKonfiguracja.class)).
    //   Dlaczego? Test traktuje wtedy autokonfigurację jak zwykłą konfigurację — bez sortowania i bez „po
    //   użytkowniku”, więc może pokazać wynik, którego w prawdziwej aplikacji nie będzie (test sekcji 5).

    // =================================================================================================
    // 6. PLIK .imports W PRAKTYCE
    // =================================================================================================
    //
    // src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:
    //     # komentarz (linia od #)
    //     s03_boot_basics.boot02_writing_auto_configuration.ShopGreeterAutoConfiguration
    //
    // Test sekcji 6 uruchamia Boot02Application z profilem boot02:
    //   • bean ShopGreeter jest i wita „Witaj, Zofia Krawczyk! Witamy w sklepie SpringLearning Warszawa.”;
    //   • bean klasy autokonfiguracji nazywa się jak PEŁNA nazwa klasy
    //     ("s03_boot_basics.boot02_writing_auto_configuration.ShopGreeterAutoConfiguration") — tak Spring nazywa
    //     klasy zaimportowane; klasa zeskanowana nazywałaby się "shopGreeterAutoConfiguration" (tej nazwy nie ma);
    //   • klasy z @AutoConfiguration zagnieżdżone w teście (ten sam pakiet) też nie trafiają do kontekstu.
    //
    // PUŁAPKA: literówka w pliku .imports. Inaczej niż literówka w exclude (Boot01 — cisza), tu start się NIE UDA:
    //       java.lang.IllegalStateException: Unable to read meta-data for class
    //           s03_boot_basics.boot02_writing_auto_configuration.ShopGreeterAutoConfigurtion
    //       Caused by: java.io.FileNotFoundException: class path resource […] cannot be opened because it does not exist
    //   Dlaczego? Każda linia pliku to obietnica „ta klasa istnieje” — Boot musi ją przeczytać, żeby ocenić warunki.
    //   Test sekcji 6 kładzie taki plik na ścieżce klas (w katalogu tymczasowym) i sprawdza ten komunikat.

    // =================================================================================================
    // 7. PLIK .imports DZIAŁA NA CAŁĄ ŚCIEŻKĘ KLAS — także na inne lekcje
    // =================================================================================================
    //
    // Wszystkie lekcje działu s03 dzielą jedną ścieżkę klas (jeden moduł Mavena), więc Boot01Application,
    // Boot03Application… też czytają nasz plik .imports i oceniają ShopGreeterAutoConfiguration. Dlatego jest
    // domyślnie wyłączona: test sekcji 7 uruchamia aplikację lekcji Boot01 — beana ShopGreeter nie ma, a raport
    // warunków mówi: „@ConditionalOnBooleanProperty (s03.greeter.enabled=true) did not find property
    // 's03.greeter.enabled'”. Tak samo zachowałaby się prawdziwa biblioteka w cudzej aplikacji.

    // =================================================================================================
    // 8. JAK TO WYGLĄDA W PRAWDZIWEJ BIBLIOTECE (tylko opis — bez testu)
    // =================================================================================================
    //
    // Firma „Acme” wydaje bibliotekę sklepu jako trzy artefakty Mavena:
    //   acme-shop                              kod biblioteki (ShopGreeter) — bez zależności od Springa
    //   acme-shop-spring-boot-autoconfigure    ShopGreeterAutoConfiguration + rekord ustawień + plik .imports;
    //                                          acme-shop jako zależność <optional>true</optional>
    //   acme-shop-spring-boot-starter          „pusty” artefakt (bez kodu) zależny od dwóch powyższych —
    //                                          użytkownik dodaje TYLKO ten jeden (Boot04Starters)
    // Prostą bibliotekę można połączyć w jeden moduł (kod autokonfiguracji + .imports w jednym jarze).
    // Opcjonalnie: spring-boot-autoconfigure-processor tworzy przy kompilacji plik
    // META-INF/spring-autoconfigure-metadata.properties (np. „…JacksonAutoConfiguration.ConditionalOnClass=…”
    // w jarze spring-boot-jackson) — Boot odrzuca wtedy kandydatów bez ładowania ich klas (szybszy start).
    //
    // DOBRA PRAKTYKA: nazwa startera firmy trzeciej to <projekt>-spring-boot-starter, NIE spring-boot-starter-<projekt>.
    //   Dlaczego? Przedrostek spring-boot jest zarezerwowany dla oficjalnych artefaktów Spring Boota — tak zaleca
    //   jego dokumentacja; po nazwie od razu widać, kto odpowiada za starter.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Biblioteka z autokonfiguracją = klasa biblioteki + rekord @ConfigurationProperties + klasa @AutoConfiguration
     *     z warunkami + wpis w META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports.
     *   • Warunki „grzecznej” autokonfiguracji: @ConditionalOnClass (biblioteka jest), @ConditionalOnBooleanProperty
     *     / @ConditionalOnProperty (włączona), @ConditionalOnMissingBean na @Bean (użytkownik wygrywa).
     *   • @AutoConfiguration = @Configuration(proxyBeanMethods = false) + before/after; zależności przez parametry @Bean.
     *   • Kolejność: alfabetycznie → @AutoConfigureOrder → before/after. Warunek o bean z innej autokonfiguracji
     *     wymaga after = … (afterName = "…", gdy klasy może nie być).
     *   • Autokonfiguracji NIE skanujemy (AutoConfigurationExcludeFilter); wchodzi przez plik .imports, zawsze po
     *     konfiguracji użytkownika. Bean klasy zaimportowanej nazywa się jak pełna nazwa klasy.
     *   • Literówka w .imports = błąd startu („Unable to read meta-data for class …”); literówka w exclude = cisza.
     *   • Test: new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(X.class)) +
     *     withPropertyValues / withUserConfiguration / withClassLoader(new FilteredClassLoader(Klasa.class)).
     *   • Pliki .properties: ISO-8859-1 — polskie znaki w wartościach w YAML.
     *   • Starter firmy trzeciej: <projekt>-spring-boot-starter (+ moduł <projekt>-spring-boot-autoconfigure).
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie cztery elementy trzeba przygotować, żeby biblioteka „konfigurowała się sama”?
     *   2. Co się stanie? W pliku jest s03.greeter.enabled=tak. Czy powstanie bean ShopGreeter?
     *   3. Co się stanie? Użytkownik definiuje @Bean ShopGreeter myGreeter() i ustawia s03.greeter.enabled=true oraz
     *      s03.greeter.greeting=Witaj. Ile beanów ShopGreeter będzie w kontekście i jakim słowem przywita klienta?
     *   4. ZNAJDŹ BŁĄD: autokonfiguracja raportu ma @ConditionalOnBean(ShopGreeter.class), nie ma after, a raport
     *      nie powstaje, choć ShopGreeter jest w kontekście. Co poprawić?
     *   5. Dlaczego @SpringBootApplication nie skanuje ShopGreeterAutoConfiguration, choć leży w skanowanym pakiecie?
     *      Co poszłoby źle, gdyby ją skanował?
     *   6. Co się stanie? W pliku .imports wpisano ShopGreeterAutoConfigurtion (literówka).
     *   7. Dlaczego w tym kursie ShopGreeterAutoConfiguration jest domyślnie wyłączona?
     *   8. ZNAJDŹ BŁĄD: firma nazwała swój starter spring-boot-starter-acme-shop.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Boot02WritingAutoConfigurationExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): ustawienia z prefiksem {@code s03.exercise.greeter}. Gdy w konfiguracji nie ma kluczy,
     * {@code shopName} ma być {@code "SpringLearning"}, a {@code maxGreetingsPerDay} — {@code 100}.
     * Dopisz adnotacje przy polach rekordu (bez startera walidacji — wystarczy wartość domyślna).
     * Podpowiedź: {@code @DefaultValue("...")} z pakietu {@code org.springframework.boot.context.properties.bind};
     * tekst „100” Boot sam zamieni na {@code int}.
     *
     * @param shopName           nazwa sklepu
     * @param maxGreetingsPerDay ile powitań dziennie wysyła sklep
     */
    @ConfigurationProperties(prefix = "s03.exercise.greeter")
    public record ExerciseGreeterProperties(
            String shopName,              // TODO: wartość domyślna "SpringLearning"
            int maxGreetingsPerDay) {     // TODO: wartość domyślna 100
    }

    /*
     * ĆWICZENIE 2 (średnie): piszesz TEST. W Boot02WritingAutoConfigurationExercisesTest uzupełnij metodę
     * exercise2(): ApplicationContextRunner z AutoConfigurations.of(ShopGreeterAutoConfiguration.class),
     * ustawieniami s03.greeter.enabled=true i s03.greeter.greeting=Siema oraz Twoją konfiguracją z beanem
     * ShopGreeter o nazwie "vipGreeter" (powitanie "Szanowny Panie"). Udowodnij, że:
     *   (a) w kontekście jest dokładnie jeden ShopGreeter i jest to "vipGreeter";
     *   (b) nie ma beana "shopGreeter";
     *   (c) powitanie NIE zaczyna się od "Siema" (ustawienia biblioteki nie dotyczą beana użytkownika).
     * Podpowiedź: test sekcji 3 (Boot02WritingAutoConfigurationTest); konfigurację użytkownika napisz jako
     * zagnieżdżoną klasę statyczną z metodą @Bean (bez @Configuration).
     */

    /**
     * ĆWICZENIE 3 (trudniejsze): druga autokonfiguracja biblioteki. Bean {@link ShopFarewell} ma powstać TYLKO wtedy,
     * gdy w kontekście jest już {@link ShopGreeter} (czyli gdy działa ShopGreeterAutoConfiguration), i tylko wtedy,
     * gdy użytkownik nie zdefiniował własnego {@code ShopFarewell}.
     * Uzupełnij klasę: metoda {@code @Bean} z dwoma warunkami i właściwa kolejność względem ShopGreeterAutoConfiguration.
     * Podpowiedź: {@code @ConditionalOnBean(ShopGreeter.class)} + {@code @ConditionalOnMissingBean}, a w adnotacji
     * klasy {@code after = ShopGreeterAutoConfiguration.class}. Bez {@code after} test nie przejdzie — sprawdź
     * dlaczego (sekcja 4: nazwa tej klasy jest alfabetycznie przed ShopGreeterAutoConfiguration).
     */
    @AutoConfiguration   // TODO: ustal kolejność względem ShopGreeterAutoConfiguration
    static class ExerciseFarewellAutoConfiguration {
        // TODO: twoje rozwiązanie — metoda @Bean zwracająca new ShopFarewell()
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Klasa biblioteki (bez Springa), rekord ustawień @ConfigurationProperties, klasa @AutoConfiguration
     *      z warunkami i wpis z pełną nazwą tej klasy w META-INF/spring/org.springframework.boot.autoconfigure.
     *      AutoConfiguration.imports. (W prawdziwej bibliotece dochodzi jeszcze starter — sekcja 8.)
     *   2. Nie. @ConditionalOnBooleanProperty porównuje wartość z "true" (bez względu na wielkość liter); "tak" nie
     *      pasuje. Sprawdzone testem (sekcja 1).
     *   3. Jeden — "myGreeter" użytkownika; "Cześć" (powitanie z jego konstruktora). @ConditionalOnMissingBean
     *      wycofał metodę biblioteki, a s03.greeter.greeting dotyczy tylko beana biblioteki. Sprawdzone testem
     *      (sekcja 3).
     *   4. Dopisz @AutoConfiguration(after = ShopGreeterAutoConfiguration.class). Bez tego kolejność jest
     *      alfabetyczna i warunek @ConditionalOnBean może być liczony, zanim ShopGreeter zostanie zarejestrowany.
     *      Sprawdzone testem (sekcja 4).
     *   5. Filtr AutoConfigurationExcludeFilter pomija klasy z @AutoConfiguration i te z pliku .imports.
     *      Zeskanowana byłaby przetwarzana razem z konfiguracją użytkownika, w niekontrolowanej kolejności —
     *      @ConditionalOnMissingBean mógłby nie zauważyć beana użytkownika i powstałyby dwa beany (test sekcji 5);
     *      nie dałoby się jej też wyłączyć przez exclude.
     *   6. Start aplikacji się nie uda: IllegalStateException „Unable to read meta-data for class
     *      s03_boot_basics.boot02_writing_auto_configuration.ShopGreeterAutoConfigurtion”, przyczyna:
     *      FileNotFoundException (nie ma takiego pliku .class). Sprawdzone testem (sekcja 6).
     *   7. Plik .imports działa na całą ścieżkę klas modułu, czyli na aplikacje wszystkich lekcji s03. Wyłączona
     *      domyślnie nie zmienia niczego w innych lekcjach (test sekcji 7) — włącza ją tylko profil boot02.
     *      To ta sama zasada co w prawdziwych bibliotekach: nie zmieniaj zachowania aplikacji, która o Tobie nie wie.
     *   8. Przedrostek spring-boot jest zarezerwowany dla oficjalnych artefaktów Spring Boota. Poprawna nazwa:
     *      acme-shop-spring-boot-starter (i moduł acme-shop-spring-boot-autoconfigure).
     */
    // </editor-fold>
}
