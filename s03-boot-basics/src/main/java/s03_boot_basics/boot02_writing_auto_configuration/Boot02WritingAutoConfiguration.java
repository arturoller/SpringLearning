package s03_boot_basics.boot02_writing_auto_configuration;

import java.math.BigDecimal;

/**
 * <pre>
 * TEMAT: Własna autokonfiguracja — piszemy mini-bibliotekę, którą Boot włącza sam
 *        (auto-configuration = autokonfiguracja; library = biblioteka; imports file = plik z listą klas do importu;
 *         ordering = kolejność; toggle / enabled flag = przełącznik „włączone”)
 *
 * W SKRÓCIE:
 *   Autokonfiguracja to zwykła klasa z metodami @Bean, oznaczona @AutoConfiguration, z warunkami @ConditionalOn…
 *   i wpisana do pliku META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports.
 *   Tak samo jak Boot robi DataSource (lekcja Boot01), Ty robisz PriceFormatter: aplikacja dodaje jar biblioteki
 *   i dostaje gotowy bean, który może skonfigurować ustawieniami (boot02.price.*) albo zastąpić własnym.
 *   Testujesz ją ApplicationContextRunnerem — w każdym scenariuszu osobno: wyłączona, domyślna, ustawiona, podmieniona.
 *
 * ANALOGIA: gniazdko z przełącznikiem w nowym mieszkaniu.
 *   Elektryk (autor biblioteki) montuje gniazdka w każdym pokoju (plik imports czyta każda aplikacja), ale każde ma
 *   wyłącznik (boot02.price.enabled). Lokator, który przyniósł własną lampę z wtyczką (własny bean), nie dostaje
 *   drugiej. A lampka nocna (PriceTagPrinter) świeci tylko, gdy prąd JUŻ płynie — dlatego podłącza się ją po
 *   włączeniu głównego obwodu (after = …).
 *
 * JAK TO DZIAŁA:
 *   1. Start aplikacji: @EnableAutoConfiguration czyta pliki …AutoConfiguration.imports ze WSZYSTKICH jarów
 *      (także z naszego modułu: src/main/resources/META-INF/spring/…).
 *   2. Boot ładuje beany aplikacji (skan + @Bean) i DOPIERO POTEM autokonfiguracje, posortowane według after/before.
 *   3. PriceAutoConfiguration: @ConditionalOnProperty(boot02.price.enabled=true) → @EnableConfigurationProperties
 *      wiąże rekord PriceProperties → metoda @Bean z @ConditionalOnMissingBean tworzy PriceFormatter (albo ustępuje).
 *   4. PriceTagAutoConfiguration (after = PriceAutoConfiguration): @ConditionalOnBean(PriceFormatter) → PriceTagPrinter.
 *   5. Serwis aplikacji ProductLabels wstrzykuje oba beany i nie wie, skąd przyszły.
 *
 * SŁÓWKA:
 *   auto-configuration = autokonfiguracja; imports = importy (lista klas); after/before = po/przed;
 *   enable configuration properties = włącz wiązanie ustawień; default value = wartość domyślna;
 *   having value = mający wartość; match if missing = pasuje, gdy brak; back-off = ustępowanie;
 *   candidate = kandydat; starter = zestaw zależności pod jedną nazwą; formatter = formater.
 *
 * ZOBACZ TEŻ: s03_boot_basics/Boot01AutoConfiguration (warunki, raport, ApplicationContextRunner),
 *             s02_configuration/Cfg02ConfigurationProperties (rekordy @ConfigurationProperties),
 *             s02_configuration/Cfg05Environment (@ConditionalOnProperty jako przełącznik funkcji),
 *             s03_boot_basics/Boot04Starters (starter = pom, który przynosi taką autokonfigurację).
 * </pre>
 */
public final class Boot02WritingAutoConfiguration {

    private Boot02WritingAutoConfiguration() {
    }

    // =================================================================================================
    // 1. KLASA @AutoConfiguration + USTAWIENIA (@EnableConfigurationProperties)
    // =================================================================================================
    //
    //   @AutoConfiguration
    //   @ConditionalOnProperty(name = "boot02.price.enabled", havingValue = "true")
    //   @EnableConfigurationProperties(PriceProperties.class)
    //   public class PriceAutoConfiguration {
    //       @Bean @ConditionalOnMissingBean
    //       PriceFormatter priceFormatter(PriceProperties properties) { … }
    //   }
    //
    //   @ConfigurationProperties("boot02.price")
    //   public record PriceProperties(@DefaultValue("false") boolean enabled,
    //                                 @DefaultValue("zł") String currency,
    //                                 @DefaultValue("2") int decimals) { }
    //
    // @AutoConfiguration (od Boot 2.7) to @Configuration(proxyBeanMethods = false) + możliwość podania after/before.
    // proxyBeanMethods = false: metody @Bean nie są „przechwytywane” przez pośrednika (proxy) CGLIB — szybszy start;
    // w autokonfiguracji zależności przekazujemy parametrami metod, nie wywołaniem innej metody @Bean.
    // @EnableConfigurationProperties rejestruje rekord jako bean — biblioteka NIE może liczyć na to, że aplikacja
    // ma @ConfigurationPropertiesScan w swoim pakiecie, więc sama mówi, które rekordy ustawień potrzebuje.
    // Test sekcji 1: domyślnie 12.5 → "12,50 zł"; z currency=EUR i decimals=0 → "13 EUR" (zaokrąglenie HALF_UP).
    //
    // DOBRA PRAKTYKA: klasa „biblioteczna” (PriceFormatter) jest zwykłą klasą bez adnotacji Springa, a cała wiedza
    //   o Springu siedzi w autokonfiguracji. Dlaczego? Bibliotekę da się użyć i przetestować bez kontenera (new),
    //   a autokonfiguracja jest cienką, wymienną warstwą.
    //
    // DOBRA PRAKTYKA: wszystkie klucze biblioteki pod jednym prefiksem (boot02.price.*) z wartościami domyślnymi.
    //   Dlaczego? Użytkownik widzi w jednym miejscu, co może ustawić, i nic nie musi ustawiać, żeby zacząć.

    // =================================================================================================
    // 2. REJESTRACJA — plik AutoConfiguration.imports (i dlaczego NIE skan komponentów)
    // =================================================================================================
    //
    // src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:
    //     s03_boot_basics.boot02_writing_auto_configuration.PriceAutoConfiguration
    //     s03_boot_basics.boot02_writing_auto_configuration.PriceTagAutoConfiguration
    // Jedna pełna nazwa klasy na linię; linia od # to komentarz.
    //
    // Dlaczego nie @ComponentScan / @Configuration „i tyle”?
    //   • Biblioteka nie wie, w jakim pakiecie jest aplikacja — skan aplikacji (com.mojsklep…) nigdy nie zajrzy do
    //     pakietu biblioteki (org.przyklad.ceny…). Plik imports działa niezależnie od pakietów.
    //   • Autokonfiguracje ładują się PO beanach aplikacji. Tylko dlatego @ConditionalOnMissingBean wiarygodnie widzi
    //     bean użytkownika (sekcja 3). Klasa znaleziona skanem byłaby przetwarzana RAZEM z beanami aplikacji,
    //     w kolejności, na którą nie masz wpływu.
    //   • Uwaga: nasza „biblioteka” leży w pakiecie lekcji, który skanuje Boot02Application. Boot pilnuje tego sam:
    //     filtr AutoConfigurationExcludeFilter (jest w @SpringBootApplication) pomija w skanie klasy, które są
    //     w pliku imports — test sekcji 5 sprawdza, że PriceAutoConfiguration jest zarejestrowana tylko raz.
    //
    // UWAGA — STARE TUTORIALE: w Boot 2 autokonfiguracje wpisywało się do META-INF/spring.factories pod kluczem
    //   org.springframework.boot.autoconfigure.EnableAutoConfiguration. Od Boot 3 ten wpis dla autokonfiguracji jest
    //   IGNOROWANY (spring.factories służy dziś tylko innym rzeczom, np. listenerom startu) — klasa po cichu się nie załaduje.
    //
    // IZOLACJA — PRZEŁĄCZNIK enabled:
    //   Plik imports czyta KAŻDA @SpringBootApplication z tym jarem na classpath — u nas także Boot01Application,
    //   Boot03…, Boot04… Dlatego PriceAutoConfiguration ma @ConditionalOnProperty(name = "boot02.price.enabled",
    //   havingValue = "true") BEZ matchIfMissing: brak klucza = biblioteka śpi. Test sekcji 2 sprawdza: brak klucza,
    //   "false" i "yes" → brak beanów; "true" i "TRUE" → są (wielkość liter w havingValue nie gra roli); aplikacja
    //   Boot01 nie ma PriceFormatter, a w jej raporcie warunków PriceAutoConfiguration jest w Negative matches.
    //
    // DOBRA PRAKTYKA: biblioteki, które robią coś „widocznego” (łączą się z siecią, startują wątki, wystawiają punkty
    //   końcowe), dają przełącznik xxx.enabled. Dlaczego? Użytkownik może wyłączyć funkcję jednym ustawieniem (np.
    //   w testach albo na jednym środowisku), bez exclude i bez znajomości nazw klas. Sam Boot ma takie przełączniki,
    //   np. spring.jmx.enabled. Domyślną wartość (włączone czy wyłączone) wybierz świadomie i opisz w dokumentacji.
    //
    // PUŁAPKA: dopisanie do pliku imports klasy BEZ żadnego warunku. Dlaczego groźne? Załaduje się w każdej aplikacji,
    //   która ma ten jar — także tam, gdzie nikt jej nie chciał (u nas: we wszystkich lekcjach modułu naraz).

    // =================================================================================================
    // 3. BACK-OFF — aplikacja z własnym PriceFormatter
    // =================================================================================================
    //
    // Aplikacja rejestruje bean shopPriceFormatter (waluta „złotych”, 1 miejsce po przecinku). Autokonfiguracja
    // przychodzi później, @ConditionalOnMissingBean widzi gotowy PriceFormatter → metoda priceFormatter() nie jest
    // wywoływana. Test sekcji 3: jeden PriceFormatter (nazwa shopPriceFormatter, brak beana „priceFormatter”),
    // a PriceTagPrinter z DRUGIEJ autokonfiguracji używa właśnie formatera aplikacji: "[Kubek | 12,5 złotych]".
    //
    // DOBRA PRAKTYKA: @ConditionalOnMissingBean na KAŻDEJ metodzie @Bean, którą użytkownik może chcieć podmienić.
    //   Dlaczego? Bez tego użytkownik dostanie dwa beany tego samego typu i błąd „expected single matching bean but
    //   found 2” w miejscu wstrzykiwania — a jedynym wyjściem będzie exclude całej autokonfiguracji.
    //
    // PYTANIE REKRUTACYJNE: jak napisać autokonfigurację, którą użytkownik może nadpisać własnym beanem?

    // =================================================================================================
    // 4. KOLEJNOŚĆ — @AutoConfiguration(after = …) / before i pułapka @ConditionalOnBean
    // =================================================================================================
    //
    //   @AutoConfiguration(after = PriceAutoConfiguration.class)   // oceniaj mnie PO formaterze
    //   @ConditionalOnBean(PriceFormatter.class)                   // „jest już formater?”
    //   public class PriceTagAutoConfiguration { @Bean PriceTagPrinter priceTagPrinter(PriceFormatter f) { … } }
    //
    // @ConditionalOnBean patrzy na beany ZAREJESTROWANE DO TEJ CHWILI. Wynik zależy więc od kolejności oceny.
    // Test sekcji 4 (sprawdzone uruchomieniem):
    //   • AutoConfigurations.of(PriceTagAutoConfiguration, PriceAutoConfiguration) — kolejność argumentów „zła”,
    //     ale after ją naprawia → PriceTagPrinter jest;
    //   • wariant z before = PriceAutoConfiguration → formater JEST w kontekście, a PriceTagPrinter NIE MA;
    //   • zwykła @Configuration UŻYTKOWNIKA z @ConditionalOnBean(PriceFormatter) → PriceTagPrinter NIE MA,
    //     bo konfiguracje użytkownika są oceniane przed wszystkimi autokonfiguracjami.
    //
    // PUŁAPKA: @ConditionalOnBean w zwykłej @Configuration aplikacji, zależny od beana z autokonfiguracji.
    //   Dlaczego zawodny? Warunek ocenia się, zanim autokonfiguracje w ogóle zaczną działać — bean z biblioteki
    //   jeszcze nie istnieje, więc warunek jest fałszywy, choć „na końcu” bean jest. Dokumentacja Boota zaleca
    //   @ConditionalOnBean/@ConditionalOnMissingBean tylko w klasach autokonfiguracji. W aplikacji zamiast warunku
    //   wstrzyknij ObjectProvider (dostawca obiektu — „daj, jeśli jest”) albo przenieś logikę do autokonfiguracji.
    //
    // DOBRA PRAKTYKA: after/before podawaj przez klasę (after = X.class), a gdy klasy może nie być na classpath —
    //   przez nazwę (afterName = "pełna.nazwa.X"). Dlaczego? Zależność kolejności jest wtedy jawna i sprawdzana
    //   przez kompilator, a nie „przypadkiem działa”.

    // =================================================================================================
    // 5. TESTOWANIE AUTOKONFIGURACJI — ApplicationContextRunner + jeden @SpringBootTest
    // =================================================================================================
    //
    // Wzorzec testów biblioteki (sekcje 1–4 już go używają; opis narzędzia — lekcja Boot01AutoConfiguration):
    //   ApplicationContextRunner runner = new ApplicationContextRunner()
    //           .withConfiguration(AutoConfigurations.of(PriceAutoConfiguration.class));
    //   runner.run(…)                                                   → wyłączona (brak klucza): brak beanów
    //   runner.withPropertyValues("boot02.price.enabled=true").run(…)   → włączona, ustawienia domyślne
    //   runner.withPropertyValues("boot02.price.enabled=true", "boot02.price.currency=EUR").run(…) → z ustawieniami
    //   runner.withPropertyValues(…).withBean("shopPriceFormatter", PriceFormatter.class, () -> mój).run(…) → back-off
    // Każdy scenariusz to osobny, świeży kontekst — scenariusze nie wpływają na siebie.
    //
    // Runner pomija plik imports (sam podajesz klasy), więc JEDEN test musi dowieść, że rejestracja naprawdę działa:
    // test sekcji 5 to @SpringBootTest na Boot02Application z profilem boot02 (application-boot02.properties:
    // enabled=true, currency=PLN). ProductLabels zwraca "19,90 PLN" i "[Kubek | 12,50 PLN]", a raport warunków ma
    // pełne dopasowanie obu klas.
    //
    // PUŁAPKA: withBean(PriceFormatter.class, …) bez nazwy nadaje beanowi nazwę wyliczoną z klasy — "priceFormatter",
    //   dokładnie taką jak metoda w autokonfiguracji. Dlaczego to myli? Test „brak beana priceFormatter” fałszywie
    //   pokaże, że Boot nie ustąpił. Nadawaj beanom testowym własne nazwy (sprawdzone uruchomieniem).
    //
    // DOBRA PRAKTYKA: każdą gałąź warunku testuj osobnym run(...). Dlaczego? Autokonfiguracja to w praktyce zestaw
    //   if-ów; nieprzetestowana gałąź (np. „wyłączona”) to dokładnie ta, która zepsuje cudzą aplikację.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Klasa: @AutoConfiguration(+ after/before) + warunki @ConditionalOn… + metody @Bean z @ConditionalOnMissingBean.
     *   • Ustawienia: rekord @ConfigurationProperties("prefiks") z @DefaultValue + @EnableConfigurationProperties(Rekord.class).
     *   • Rejestracja: META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports — pełna nazwa
     *     klasy na linię. NIE spring.factories (Boot 2) i NIE skan komponentów.
     *   • Autokonfiguracje ładują się PO beanach aplikacji → @ConditionalOnMissingBean widzi bean użytkownika.
     *   • Przełącznik: @ConditionalOnProperty(name = "x.enabled", havingValue = "true") — bez matchIfMissing = domyślnie wyłączona.
     *   • @ConditionalOnBean tylko w autokonfiguracjach i tylko z after = …, nigdy w @Configuration aplikacji.
     *   • Testy: ApplicationContextRunner + AutoConfigurations.of(...), scenariusze: wyłączona / domyślna / ustawiona /
     *     podmieniona; plus jeden @SpringBootTest dowodzący, że plik imports działa.
     *
     * PYTANIA KONTROLNE:
     *   1. Gdzie i w jakim formacie rejestruje się autokonfigurację w Boot 4? Co z wpisem w spring.factories?
     *   2. Dlaczego autokonfiguracja biblioteki nie powinna być znajdowana przez @ComponentScan?
     *   3. Co się stanie? W aplikacji jest {@code boot02.price.enabled=yes}.
     *   4. Co się stanie? {@code boot02.price.enabled=true}, {@code boot02.price.decimals=0}, formatujesz 12.5 zł w EUR.
     *   5. ZNAJDŹ BŁĄD: w {@code @Configuration} aplikacji jest {@code @ConditionalOnBean(PriceFormatter.class)},
     *      PriceFormatter pochodzi z autokonfiguracji, a bean z tej konfiguracji nigdy nie powstaje. Dlaczego?
     *   6. ZNAJDŹ BŁĄD: PriceTagAutoConfiguration ma {@code @AutoConfiguration(before = PriceAutoConfiguration.class)}.
     *      Co się stanie z PriceTagPrinter, choć PriceFormatter jest w kontekście?
     *   7. Po co bibliotece przełącznik {@code xxx.enabled} i dlaczego u nas nie ma matchIfMissing = true?
     *   8. Jakie scenariusze testujesz ApplicationContextRunnerem i czego runner NIE sprawdza?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Boot02WritingAutoConfigurationExercisesTest
    // =================================================================================================
    // Klasy ćwiczeń testujesz WYŁĄCZNIE ApplicationContextRunnerem. NIE dopisuj ich do pliku imports — każda
    // aplikacja modułu zaczęłaby je ładować. (Boot02Application może je znaleźć skanem, ale dzięki przełącznikom
    // boot02.exercise.* i kolejności opisanej w sekcji 4 nic tam nie zmienią.)

    /**
     * ĆWICZENIE 1 (łatwe): zwróć ścieżkę zasobu na classpath (bez ukośnika na początku), pod którą Boot 4 szuka listy
     * autokonfiguracji. Test sprawdza, że taki zasób istnieje i wymienia PriceAutoConfiguration.
     * Podpowiedź: sekcja 2 — katalog META-INF/spring/, nazwa pliku to pełna nazwa adnotacji + {@code .imports}.
     */
    static String exercise1ImportsFileLocation() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zamień tę klasę w autokonfigurację, która tworzy {@link ShippingCostCalculator} z progiem
     * darmowej wysyłki 200 — TYLKO gdy {@code boot02.exercise.shipping.enabled=true} — i ustępuje przed beanem
     * aplikacji tego samego typu.
     * Podpowiedź: {@code @AutoConfiguration}, {@code @ConditionalOnProperty(name = …, havingValue = "true")} nad klasą;
     * {@code @Bean} i {@code @ConditionalOnMissingBean} nad metodą.
     */
    static class Exercise2ShippingAutoConfiguration {
        // TODO: twoje rozwiązanie (adnotacje nad klasą i nad metodą)
        ShippingCostCalculator shippingCostCalculator() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 3 (średnie): ustawienia biblioteki. Rekord {@code DeliveryProperties} ma się wiązać z prefiksem
     * {@code boot02.exercise.delivery} (klucz {@code free-from}, domyślnie 150). Autokonfiguracja (włączana
     * {@code boot02.exercise.delivery.enabled=true}) rejestruje rekord i tworzy ShippingCostCalculator z jego progiem.
     * Podpowiedź: {@code @ConfigurationProperties} + {@code @DefaultValue("150")} na rekordzie;
     * {@code @EnableConfigurationProperties(DeliveryProperties.class)} na autokonfiguracji (wzór: PriceAutoConfiguration).
     */
    record DeliveryProperties(BigDecimal freeFrom) {
        // TODO: twoje rozwiązanie (adnotacje nad rekordem i przy komponencie)
    }

    /** ĆWICZENIE 3 — ciąg dalszy: autokonfiguracja korzystająca z {@link DeliveryProperties}. */
    static class Exercise3DeliveryAutoConfiguration {
        // TODO: twoje rozwiązanie (adnotacje nad klasą i nad metodą)
        ShippingCostCalculator shippingCostCalculator(DeliveryProperties properties) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): autokonfiguracja, która tworzy {@link PriceTagPrinter} tylko wtedy, gdy w kontekście
     * JEST PriceFormatter — i działa niezależnie od kolejności klas podanych w {@code AutoConfigurations.of(...)}.
     * Gdy formatera brak, kontekst ma wystartować bez błędu i bez PriceTagPrinter.
     * Podpowiedź: sekcja 4 — warunek na obecność beana i atrybut kolejności w {@code @AutoConfiguration}.
     */
    static class Exercise4PriceTagAutoConfiguration {
        // TODO: twoje rozwiązanie (adnotacje nad klasą i nad metodą)
        PriceTagPrinter priceTagPrinter(PriceFormatter formatter) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. W pliku META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports w jarze biblioteki —
     *      jedna pełna nazwa klasy na linię (# = komentarz). Wpis EnableAutoConfiguration w spring.factories to sposób
     *      z Boot 2; od Boot 3 Boot go dla autokonfiguracji ignoruje. Sprawdzone testem (sekcja 2: plik jest na classpath).
     *   2. Bo biblioteka nie zna pakietu aplikacji, a klasy ze skanu są przetwarzane razem z beanami aplikacji —
     *      @ConditionalOnMissingBean nie miałby gwarancji, że bean użytkownika już jest. Plik imports ładuje klasy
     *      PO beanach aplikacji i w ustalonej kolejności (after/before).
     *   3. Biblioteka się nie włączy: havingValue = "true" wymaga wartości true (bez względu na wielkość liter),
     *      a "yes" nią nie jest. Sprawdzone testem (sekcja 2).
     *   4. "13 EUR" — zero miejsc po przecinku i zaokrąglenie HALF_UP (12,5 → 13). Sprawdzone testem (sekcja 1).
     *   5. Konfiguracje aplikacji są oceniane przed autokonfiguracjami, więc w chwili sprawdzania warunku formatera
     *      jeszcze nie ma — warunek jest fałszywy. Sprawdzone testem (sekcja 4). Napraw: ObjectProvider albo logika
     *      w autokonfiguracji z after = PriceAutoConfiguration.
     *   6. PriceTagPrinter nie powstanie: klasa jest oceniana przed PriceAutoConfiguration, kiedy formatera jeszcze nie
     *      ma. Sprawdzone testem (sekcja 4). Poprawnie: after = PriceAutoConfiguration.class.
     *   7. Żeby jednym ustawieniem włączać/wyłączać funkcję bez exclude i bez znajomości nazw klas. U nas plik imports
     *      czytają wszystkie aplikacje modułu, więc domyślnie biblioteka MUSI spać — matchIfMissing = true włączyłoby
     *      ją we wszystkich lekcjach.
     *   8. Wyłączona (brak klucza / false), włączona z domyślnymi, włączona z ustawieniami, z beanem użytkownika
     *      (back-off), kolejność z innymi autokonfiguracjami. Runner NIE czyta pliku imports (sam podajesz klasy) —
     *      to sprawdza osobny @SpringBootTest na prawdziwej aplikacji.
     */
    // </editor-fold>
}
