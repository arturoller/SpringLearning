package s02_configuration.cfg05_environment;

import org.springframework.core.env.Environment;

import java.util.Map;

/**
 * <pre>
 * TEMAT: Environment, przełączniki funkcji i zmienne środowiska
 *        (environment = środowisko, obiekt Springa z ustawieniami i profilami; feature flag = przełącznik funkcji;
 *         environment variable = zmienna środowiska systemu operacyjnego; condition = warunek)
 *
 * W SKRÓCIE:
 *   Environment to „centrala” ustawień: łączy wszystkie źródła (argumenty, właściwości JVM, zmienne środowiska,
 *   pliki) w jedną uporządkowaną listę i zna aktywne profile. Możesz go zapytać wprost (getProperty), ale częściej
 *   korzystasz z niego pośrednio: @Value, @ConfigurationProperties i @ConditionalOnProperty pytają właśnie jego.
 *   @ConditionalOnProperty włącza lub wyłącza bean zależnie od wartości ustawienia — to prosty przełącznik funkcji:
 *   nową kasę wdrażasz „uśpioną” i włączasz zmienną środowiska, bez nowego wydania aplikacji.
 *
 * ANALOGIA: tablica rozdzielcza z wyłącznikami.
 *   Environment to tablica, na której zbiegają się wszystkie przewody: z pliku, z linii poleceń, z systemu.
 *   Wyłącznik @ConditionalOnProperty decyduje, czy dana „lampa” (bean) w ogóle się zapali.
 *   Zmienna środowiska to wyłącznik na zewnątrz budynku — administrator przestawia go bez wchodzenia do kodu.
 *
 * JAK TO DZIAŁA:
 *   1. Environment trzyma PropertySources w kolejności priorytetu (patrz lekcja Cfg01Properties).
 *   2. getProperty("cfg05.shop.discount-percent") przegląda je po kolei i zwraca pierwszą znalezioną wartość.
 *   3. Zmienne środowiska mają inną składnię nazw niż klucze (wielkie litery, podkreślniki zamiast kropek i myślników),
 *      więc źródło systemowe (SystemEnvironmentPropertySource) „tłumaczy” nazwy:
 *          cfg05.shop.discount-percent  ⇄  CFG05_SHOP_DISCOUNT_PERCENT
 *   4. @ConditionalOnProperty sprawdza wartość przy rejestracji beana: pasuje → bean powstaje, nie pasuje → nie ma go.
 *
 * SŁÓWKA:
 *   get property = pobierz ustawienie; required = wymagany; accepts profiles = czy pasuje do profili;
 *   conditional on property = warunkowo od ustawienia; having value = mający wartość; match if missing = pasuje,
 *   gdy brak; flag = flaga, przełącznik; relaxed binding = luźne dopasowanie nazw; legacy = stary, odziedziczony.
 *
 * ZOBACZ TEŻ: s02_configuration/Cfg01Properties (priorytety źródeł),
 *             s02_configuration/Cfg03Profiles (profile),
 *             s03_boot_basics/Boot01AutoConfiguration (autokonfiguracja Boota to setki takich warunków),
 *             t30_build_modules/Build05ProcessesEnv (zmienne środowiska w czystej Javie).
 * </pre>
 */
public final class Cfg05Environment {

    private Cfg05Environment() {
    }

    // =================================================================================================
    // 1. API ŚRODOWISKA — getProperty, getRequiredProperty, acceptsProfiles
    // =================================================================================================
    //
    //   environment.getProperty("klucz")                 → String albo null
    //   environment.getProperty("klucz", "domyślna")     → z wartością domyślną
    //   environment.getProperty("klucz", Integer.class)  → z konwersją typu (null, gdy brak)
    //   environment.getRequiredProperty("klucz")         → brak = IllegalStateException (nie null!)
    //   environment.containsProperty("klucz")            → czy istnieje
    //   environment.getActiveProfiles()                  → aktywne profile
    //   environment.acceptsProfiles(Profiles.of("a", "b"))→ czy aktywny jest a LUB b (też z ! i &)
    // DiscountService używa wszystkich tych metod. Test sekcji 1 sprawdza każdą.
    //
    // DOBRA PRAKTYKA: w zwykłej logice biznesowej wstrzykuj @Value albo rekord @ConfigurationProperties, a nie Environment.
    //   Dlaczego? Environment ukrywa, jakich ustawień klasa potrzebuje (nie widać ich w konstruktorze) i trudniej
    //   go testować. Environment jest dobry w kodzie infrastrukturalnym, gdzie nazwa klucza jest wyliczana.
    //
    // DOBRA PRAKTYKA: ustawienia bez których nie ma sensu działać pobieraj przez getRequiredProperty.
    //   Dlaczego? Brak klucza kończy się natychmiast czytelnym wyjątkiem ("Required key 'x' not found"),
    //   a nie NullPointerException w losowym miejscu.

    // =================================================================================================
    // 2. @ConditionalOnProperty — przełączniki funkcji
    // =================================================================================================
    //
    //   NewCheckout     @ConditionalOnProperty(prefix = "cfg05.feature", name = "new-checkout", havingValue = "true")
    //   LegacyCheckout  @ConditionalOnProperty(..., havingValue = "false", matchIfMissing = true)
    //   LoyaltyProgram  @ConditionalOnProperty("cfg05.feature.loyalty")   // bez havingValue
    //
    // Reguły (sprawdzone testem sekcji 2):
    //   • z havingValue: bean powstaje, gdy wartość równa się podanej;
    //   • bez havingValue: klucz musi istnieć i nie może mieć wartości "false";
    //   • matchIfMissing = true: bean powstaje także, gdy klucza w ogóle nie ma (domyślnie false: brak klucza = brak beana).
    // Układ NewCheckout + LegacyCheckout gwarantuje, że istnieje dokładnie jedna kasa niezależnie od konfiguracji
    // (stara jest „domyślna”, bo pasuje przy braku klucza).
    //
    // PUŁAPKA: oba beany z @ConditionalOnProperty z havingValue="true"/"false", ale BEZ matchIfMissing na tym „domyślnym”.
    //   Dlaczego to groźne? Gdy ktoś nie wpisze klucza, nie powstanie ŻADNA kasa i aplikacja się nie uruchomi
    //   (NoSuchBeanDefinitionException tam, gdzie wstrzykiwany jest Checkout).
    //
    // PUŁAPKA: literówka w nazwie flagi w pliku (cfg05.feature.new-chekout=true) — flaga „nie działa”, ale bez błędu.
    //   Dlaczego? Nieznany klucz nikomu nie przeszkadza (jak w Cfg02ConfigurationProperties). Przy matchIfMissing=true
    //   aplikacja po cichu wybiera wersję domyślną.
    //
    // DOBRA PRAKTYKA: flagi funkcji dawaj z prefiksem (feature.*) i myślnikami; dokumentuj domyślną wartość i usuń flagę
    //   (wraz ze starym kodem), gdy nowa funkcja jest już na stałe. Dlaczego? Flagi, które nigdy nie znikają, mnożą
    //   kombinacje konfiguracji do przetestowania.

    // =================================================================================================
    // 3. ZMIENNE ŚRODOWISKA — nazwa CFG05_SHOP_DISCOUNT_PERCENT ⇄ klucz cfg05.shop.discount-percent
    // =================================================================================================
    //
    // W systemie operacyjnym zmienne środowiska mają wielkie litery, cyfry i podkreślniki (nie wolno kropek ani
    // myślników). Reguła zamiany klucza na zmienną: kropki i myślniki → podkreślniki, wszystko wielkimi literami.
    //     cfg05.shop.discount-percent   →   CFG05_SHOP_DISCOUNT_PERCENT
    // Dotyczy to zarówno pytania Environmentu (getProperty), jak i wiązania do rekordów (relaxed binding).
    // Lista w zmiennej: wartości po przecinku:  CFG05_SHOP_DAYS=pon,wt.
    // (Zapis elementów listy z indeksem ma w zmiennych środowiska osobne reguły — dla prostych list użyj przecinków.)
    //
    // PUŁAPKA: zmienna CFG05_SHOP_DISCOUNTPERCENT (bez podkreślnika między discount a percent) NIE pasuje do klucza
    //   discount-percent. Dlaczego? Myślnik w kluczu odpowiada podkreślnikowi w zmiennej — sklejona nazwa to inna
    //   nazwa. Sprawdzone testem (sekcja 3): wiązanie zwraca „brak wartości”, a nie błąd.
    //
    // Testy NIE ustawiają prawdziwych zmiennych środowiska (zależałyby od komputera). Test sekcji 3 podstawia sztuczne
    // źródło SystemEnvironmentPropertySource, a test sekcji 4 — całe sztuczne "środowisko systemowe" w aplikacji.
    //
    // DOBRA PRAKTYKA: w kontenerze Dockera i w chmurze ustawienia podawaj zmiennymi środowiska (SPRING_PROFILES_ACTIVE,
    //   SHOP_DISCOUNT_PERCENT…), a w pliku trzymaj wartości domyślne. Dlaczego? Zmienne środowiska ustawia platforma
    //   uruchomieniowa bez dotykania obrazu aplikacji, a wartość z nich wygrywa z plikiem (sekcja 4).
    //
    // PUŁAPKA: zmienne środowiska nigdy nie trafiają do repozytorium, ale trafiają do logów i do narzędzi
    //   diagnostycznych. Sekrety (hasła) traktuj ostrożnie. Dlaczego? Wyciek z zrzutu środowiska jest realnym ryzykiem.

    // =================================================================================================
    // 4. PEŁNY PRIORYTET Z ZMIENNYMI ŚRODOWISKA
    // =================================================================================================
    //
    // Test sekcji 4 uruchamia aplikację z podstawionymi zmiennymi środowiska (CFG05_SHOP_DISCOUNT_PERCENT=15):
    //   plik: 10  →  zmienna środowiska: 15  →  właściwość JVM: 17  →  argument: 20. Każdy kolejny wygrywa z poprzednim.
    // Uzupełnia to lekcję Cfg01Properties, gdzie zmiennych środowiska nie dało się pokazać.

    // =================================================================================================
    // 5. PROFILE W KODZIE — acceptsProfiles
    // =================================================================================================
    //
    // DiscountService.mode() zwraca "WEEKEND", gdy aktywny jest profil cfg05-weekend. Test sekcji 5 włącza go i sprawdza.
    // PUŁAPKA: sprawdzanie profilu w kodzie (if prod) rozsiewa wiedzę o środowiskach po całej aplikacji.
    //   Dlaczego lepiej @Profile na beanach? Wybór implementacji w jednym miejscu (konfiguracja) jest łatwiejszy
    //   do przejrzenia i przetestowania niż rozproszone if-y.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Environment: getProperty(klucz[, domyślna|Typ.class]), getRequiredProperty (brak = IllegalStateException),
     *     containsProperty, getActiveProfiles, acceptsProfiles(Profiles.of(...)).
     *   • Zwykle wstrzykuj @Value / rekord @ConfigurationProperties; Environment — dla kodu infrastrukturalnego.
     *   • @ConditionalOnProperty(prefix, name, havingValue, matchIfMissing): bean powstaje zależnie od ustawienia.
     *   • Bez havingValue: klucz musi istnieć i nie być "false". Domyślny wariant: matchIfMissing = true.
     *   • Zmienna środowiska: wielkie litery, kropki i myślniki → _ (CFG05_SHOP_DISCOUNT_PERCENT); lista po przecinku.
     *   • Priorytet: plik < zmienna środowiska < właściwość JVM < argument wiersza poleceń.
     *   • Testy nie dotykają prawdziwych zmiennych środowiska — podstawiaj SystemEnvironmentPropertySource.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się getProperty od getRequiredProperty, gdy klucza brak?
     *   2. Co się stanie? {@code @ConditionalOnProperty("cfg05.feature.loyalty")} i w pliku jest {@code cfg05.feature.loyalty=false}.
     *   3. Co się stanie? Ta sama adnotacja, w pliku {@code cfg05.feature.loyalty=tak}.
     *   4. Co się stanie? Ustawiasz zmienną środowiska {@code CFG05_SHOP_DISCOUNTPERCENT=25} dla klucza {@code discount-percent}.
     *   5. ZNAJDŹ BŁĄD: NewCheckout ma havingValue="true", a LegacyCheckout havingValue="false" BEZ matchIfMissing,
     *      i nikt nie wpisał flagi do konfiguracji. Co się wydarzy przy starcie?
     *   6. ZNAJDŹ BŁĄD: w pliku {@code cfg05.feature.new-chekout=true} — dlaczego nowa kasa się nie włączyła?
     *   7. Jak zapiszesz klucz {@code app.max-retries} jako zmienną środowiska?
     *   8. Dlaczego Environment w logice biznesowej to zwykle gorszy wybór niż @Value?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Cfg05EnvironmentExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć liczbę prób z klucza {@code cfg05.exercise.retries}; gdy klucza brak — 3.
     * Podpowiedź: {@code environment.getProperty(klucz, Integer.class, wartośćDomyślna)}.
     */
    static int exercise1(Environment environment) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): wysyłka ekspresowa jako przełącznik funkcji. Bean {@code ExpressShipping} ma istnieć
     * TYLKO gdy {@code cfg05.exercise.express=true}. Dopisz adnotację nad klasą.
     * Podpowiedź: {@code @ConditionalOnProperty(name = ..., havingValue = ...)}; brak klucza i "false" = brak beana.
     */
    static class ExpressShipping {
        // TODO: twoje rozwiązanie (adnotacja nad klasą)
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): dostałeś mapę udającą zmienne środowiska systemu, np.
     * {@code {"CFG05_EXERCISE_MAX_DAYS": "4"}}. Zwróć wartość klucza {@code cfg05.exercise.max-days} jako Integer,
     * a gdy jej nie ma — -1. Podpowiedź: {@code new StandardEnvironment()}, dodaj
     * {@code new SystemEnvironmentPropertySource("nazwa", mapa)} do jego {@code getPropertySources()}, potem
     * {@code new Binder(ConfigurationPropertySources.get(env)).bind(klucz, Integer.class).orElse(-1)}.
     */
    static int exercise3(Map<String, Object> fakeOsEnvironment) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. getProperty zwraca null (albo podaną wartość domyślną); getRequiredProperty rzuca IllegalStateException
     *      z nazwą brakującego klucza. Sprawdzone testem (sekcja 1).
     *   2. Bean LoyaltyProgram nie powstanie — wartość "false" wyłącza bean (klucz istnieje, ale ma wartość false).
     *      Sprawdzone testem (sekcja 2).
     *   3. Bean powstanie: bez havingValue pasuje każda wartość inna niż "false", więc "tak" też włącza.
     *      Sprawdzone testem (sekcja 2).
     *   4. Nic się nie zwiąże: nazwa bez podkreślnika to inna nazwa niż CFG05_SHOP_DISCOUNT_PERCENT; wiązanie
     *      zwraca „brak wartości”, a nie błąd. Sprawdzone testem (sekcja 3).
     *   5. Żadna kasa nie powstanie (ani nowa, ani stara), więc wstrzyknięcie Checkout kończy się niepowodzeniem
     *      i aplikacja się nie uruchomi (brak beana wymaganego typu). Napraw: matchIfMissing = true na „domyślnym”.
     *      Sprawdzone testem (sekcja 2): bez klucza pasuje tylko wariant z matchIfMissing = true.
     *   6. Literówka w nazwie klucza — klucz nie istnieje w oczekiwanej formie, więc flaga jest „brakująca”,
     *      a matchIfMissing=true wybiera starą kasę. Nikt nie zgłasza błędu, bo nieznane klucze są ignorowane.
     *   7. APP_MAX_RETRIES (kropka i myślnik zamieniają się na podkreślniki, litery wielkie).
     *   8. Environment ukrywa potrzebne ustawienia (brak ich w konstruktorze) i trudniej go testować; @Value albo rekord
     *      pokazuje zależność wprost i da się go utworzyć zwykłym new.
     */
    // </editor-fold>
}
