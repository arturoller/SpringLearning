package s01_core_ioc.ioc04_bean_methods;

import java.time.Clock;

/**
 * <pre>
 * TEMAT: Metody @Bean w klasie @Configuration — beany, których nie da się oznaczyć adnotacją
 *        (configuration = konfiguracja; bean method = metoda tworząca beana; Clock = zegar;
 *         proxy = pośrednik, obiekt udający inny obiekt; singleton = jeden wspólny egzemplarz na cały kontener)
 *
 * W SKRÓCIE:
 *   @Component stawiasz na SWOJEJ klasie. Ale co z klasami, których nie napisałeś: Clock, NumberFormat, DataSource
 *   z biblioteki? Nie dopiszesz im adnotacji. Wtedy piszesz metodę oznaczoną @Bean w klasie @Configuration:
 *   metoda tworzy obiekt (new ...), a Spring zapamiętuje go jako beana. Nazwa beana = nazwa metody (albo @Bean("nazwa")).
 *   Zależności metody przychodzą jako jej PARAMETRY. Ta sama technika służy, gdy tworzenie obiektu wymaga logiki
 *   (if, pętla, odczyt ustawień).
 *
 * ANALOGIA: sklep i zewnętrzni dostawcy.
 *   @Component to produkt z Twojej własnej wytwórni — sam naklejasz etykietę „do sprzedaży”. @Bean to produkt od obcego
 *   dostawcy: nie możesz kleić etykiet na ich towar, więc w księdze zamówień (klasa @Configuration) zapisujesz
 *   „kup od dostawcy X taki produkt i wystaw go pod nazwą Y”.
 *
 * JAK TO DZIAŁA:
 *   Start kontekstu:
 *     1. kontener skanuje pakiet i znajduje klasę z @Configuration (ShopConfig)
 *     2. dla każdej metody @Bean zapisuje definicję beana: nazwa = nazwa metody, typ = typ zwracany
 *     3. tworzy beany, wołając metody; parametry metod uzupełnia innymi beanami (invoiceNumberGenerator(Clock clock)
 *        dostaje bean "clock")
 *   Tryb PEŁNY (full): @Configuration (proxyBeanMethods = true, czyli „twórz pośrednika dla metod @Bean” — domyślnie)
 *     Spring generuje podklasę Twojej klasy (proxy CGLIB — biblioteka do generowania podklas w locie). Wywołanie
 *     jednej metody @Bean z drugiej trafia do proxy, które zwraca ISTNIEJĄCEGO singletona.
 *   Tryb LEKKI (lite): @Configuration(proxyBeanMethods = false) albo zwykła klasa z metodami @Bean
 *     Bez proxy metody są zwykłymi metodami Javy — wywołanie z drugiej metody tworzy NOWY obiekt, poza kontenerem.
 *
 * SŁÓWKA:
 *   configuration = konfiguracja; bean = ziarno (obiekt zarządzany przez Springa); clock = zegar; fixed = ustalony,
 *   zatrzymany; system default zone = domyślna strefa czasowa systemu; invoice = faktura; counter = licznik;
 *   generator = generator (twórca numerów); proxy = pośrednik; full/lite = pełny/lekki; candidate = kandydat.
 *
 * ZOBACZ TEŻ: s01_core_ioc/Ioc03Stereotypes (@Component i jego odmiany), s01_core_ioc/Ioc02ConstructorInjection
 *             (wstrzykiwanie zależności), s01_core_ioc/Ioc05MultipleCandidates (gdy beanów jednego typu jest kilka).
 * </pre>
 */
public final class Ioc04BeanMethods {

    private Ioc04BeanMethods() {
    }

    // =================================================================================================
    // 1. METODA @Bean — bean z klasy, której nie możemy oznaczyć (ShopConfig)
    // =================================================================================================
    //
    // @Configuration                                   // configuration = konfiguracja; klasa-księga zamówień
    // public class ShopConfig {
    //     @Bean                                        // bean = ziarno; „wynik tej metody jest beanem”
    //     public Clock clock() { return Clock.systemDefaultZone(); }
    // }
    //
    // Nazwa beana to nazwa METODY: "clock". Zmiana nazwy: @Bean("plnFormat") — wtedy nazwa metody (currencyFormat)
    // jest ignorowana i bean nazywa się "plnFormat". Typ beana to typ zwracany przez metodę.
    //
    // PUŁAPKA: zmiana nazwy metody @Bean zmienia nazwę beana. Dlaczego to groźne? Kod, który pobiera beana po nazwie
    //   (getBean("clock"), @Qualifier("clock")), przestaje działać po „niewinnym” przemianowaniu metody. Gdy nazwa ma
    //   znaczenie, ustaw ją jawnie w @Bean("...").

    // =================================================================================================
    // 2. ZALEŻNOŚCI METODY @Bean — jako parametry
    // =================================================================================================
    //
    // @Bean
    // public InvoiceNumberGenerator invoiceNumberGenerator(Clock clock) { return new InvoiceNumberGenerator(clock); }
    //
    // Parametr Clock kontener uzupełnia beanem "clock" — dokładnie jak parametr konstruktora w @Component.
    // Dlatego w ShopConfig nie wołamy clock() ręcznie: prosimy o gotowego beana.
    //
    // DOBRA PRAKTYKA: w metodzie @Bean bierz zależności jako parametry, a nie przez wywołanie innej metody @Bean.
    //   Dlaczego? Działa tak samo w trybie pełnym i lekkim, a kontener widzi zależność wprost (sekcja 5 pokazuje,
    //   co się dzieje, gdy wołasz metodę).

    // =================================================================================================
    // 3. @Bean czy @Component? — kiedy które
    // =================================================================================================
    //
    //   @Component (i @Service, @Repository): to TWOJA klasa, tworzona konstruktorem, bez specjalnej logiki.
    //   @Bean: klasa cudza (JDK: Clock, NumberFormat; biblioteki), albo tworzenie wymaga logiki (warunki,
    //          ustawienia, kilka beanów tego samego typu o różnych parametrach).
    //
    // NumberFormat.getCurrencyInstance(...) nie ma publicznego konstruktora do wywołania przez Springa — obiekt
    // powstaje fabryką, więc @Component nie wchodzi w grę. @Bean tak.
    //
    // PUŁAPKA: NumberFormat NIE jest bezpieczny wielowątkowo, a bean jest singletonem wspólnym dla wszystkich wątków.
    //   Dlaczego to problem? Równoległe wywołania format(...) mogą się wzajemnie psuć. W prawdziwej aplikacji
    //   tworzysz NumberFormat przy każdym użyciu (to tani obiekt); tu bean służy tylko do nauki mechanizmu.

    // =================================================================================================
    // 4. DOBRA PRAKTYKA: wstrzykuj Clock zamiast wołać LocalDate.now()
    // =================================================================================================
    //
    // PRZED:  LocalDate.now()                         — czas wzięty „z powietrza”, test zależy od dnia uruchomienia
    // PO:     LocalDate.now(clock)                    — czas z wstrzykniętego zegara; w teście: Clock.fixed(...)
    //
    // InvoiceNumberGenerator buduje numer "FV/2026/10/0001" z daty zegara. W produkcji bean "clock" to zegar
    // systemowy, w teście — zatrzymany na 2026-10-11. Test jest wtedy powtarzalny w każdy dzień roku.
    //
    // DOBRA PRAKTYKA: czas pobieraj z beana Clock, nigdy z LocalDate.now()/Instant.now() w logice. Dlaczego? Bez tego
    //   nie przetestujesz poprawnie końca miesiąca, roku przestępnego ani przejścia strefy czasowej, a testy bywają
    //   „zielone w poniedziałek i czerwone we wtorek”.

    // =================================================================================================
    // 5. TRYB PEŁNY (proxy) a LEKKI — wywołanie metody @Bean z innej metody
    // =================================================================================================
    //
    // FullModeConfig:   fullCalculator() { return new PriceCalculator(fullTaxTable()); }  → TA SAMA TaxTable co bean
    // LiteModeConfig:   liteCalculator() { return new PriceCalculator(liteTaxTable()); }  → NOWA TaxTable
    //
    // W trybie pełnym klasa konfiguracji jest podklasą wygenerowaną przez Springa (nazwa zawiera "$$SpringCGLIB$$"),
    // dlatego klasa @Configuration nie może być final, a metody @Bean nie mogą być private ani final.
    // Tryb lekki jest szybszy w starcie (brak generowania podklasy) i Boot używa go w swoich autokonfiguracjach.
    //
    // PUŁAPKA: w trybie lekkim wywołanie liteTaxTable() z innej metody tworzy DRUGI obiekt, o którym kontener nie
    //   wie. Dlaczego? Bez proxy nikt nie przechwytuje wywołania — to zwykłe wywołanie metody. Efekt: dwa „singletony”
    //   tego samego typu, które się rozjeżdżają (np. dwa liczniki). Rozwiązanie: zależność jako PARAMETR metody
    //   (liteCalculatorFromParameter) — kontener poda prawdziwego beana.
    //
    // DOBRA PRAKTYKA: domyślnie zostaw tryb pełny; przełącz na proxyBeanMethods = false dopiero, gdy żadna metoda
    //   @Bean nie woła innej i zależy Ci na szybszym starcie. Dlaczego? Tryb pełny chroni przed cichym
    //   utworzeniem drugiego obiektu.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @Bean na metodzie w klasie @Configuration: wynik metody jest beanem. Nazwa = nazwa metody lub @Bean("nazwa").
     *   • Zależności metody @Bean = jej parametry (kontener je uzupełnia, jak w konstruktorze).
     *   • @Component — Twoje klasy; @Bean — klasy cudze (JDK, biblioteki) lub tworzenie z logiką.
     *   • Clock jako bean: LocalDate.now(clock); w teście Clock.fixed(Instant, ZoneId) — czas zatrzymany.
     *   • Tryb pełny (domyślny): proxy CGLIB, wywołanie metody @Bean z innej metody daje TEGO SAMEGO singletona.
     *   • Tryb lekki (proxyBeanMethods = false lub zwykła klasa z @Bean): wywołanie daje NOWY obiekt.
     *   • Klasa @Configuration w trybie pełnym nie może być final; metody @Bean nie mogą być private/final.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego Clock musi być beanem z metody @Bean, a nie z @Component?
     *   2. Jak nazywa się bean z metody currencyFormat() oznaczonej @Bean("plnFormat")?
     *   3. Co się stanie? W LiteModeConfig kalkulator liteCalculator() woła liteTaxTable(). Czy jego TaxTable to ten sam
     *      obiekt co bean "liteTaxTable"?
     *   4. Co się stanie? W trybie pełnym wywołasz clock() na beanie ShopConfig pobranym z kontekstu. Czy dostaniesz
     *      tego samego Clock co getBean(Clock.class)?
     *   5. ZNAJDŹ BŁĄD: {@code return "FV/" + LocalDate.now().format(f) + "/0001";} w generatorze, który ma być
     *      testowany datą 2026-10-11. Co jest nie tak i jak to naprawić?
     *   6. ZNAJDŹ BŁĄD: w LiteModeConfig piszesz {@code new PriceCalculator(liteTaxTable())} i oczekujesz, że kalkulator
     *      dostał beana. Co poprawić?
     *   7. Kiedy wybierzesz @Bean, a kiedy @Component?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Ioc04BeanMethodsExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć prefiks numeru faktury w formacie "FV/yyyy/MM" według podanego zegara.
     * Dla zegara zatrzymanego na 2026-10-11 wynik to "FV/2026/10". Podpowiedź: LocalDate.now(clock) oraz
     * DateTimeFormatter.ofPattern("yyyy/MM") (albo String.format z getYear() i getMonthValue(), miesiąc dwucyfrowy: %02d).
     */
    static String exercise1(Clock clock) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // ĆWICZENIE 2 (średnie): uzupełnij klasę Ioc04Exercise2Config (w tym pakiecie): metoda warsawClock() z @Bean ma
    // zwracać zegar systemowy ze strefą Europe/Warsaw. Podpowiedź: Clock.system(ZoneId.of("Europe/Warsaw")).
    // Test rejestruje klasę przez ApplicationContextRunner i sprawdza strefę beana Clock.
    //
    // ĆWICZENIE 3 (trudniejsze): uzupełnij klasę Ioc04Exercise3Config: metoda invoiceNumberGenerator(Clock clock) ma
    // zwrócić nowy InvoiceNumberGenerator zbudowany z podanego zegara (zależność przychodzi jako parametr!).
    // Test dostarcza zegar zatrzymany na 2026-10-11 i oczekuje numeru "FV/2026/10/0001".

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo Clock jest klasą JDK — nie możemy dopisać jej adnotacji @Component. Metoda @Bean pozwala utworzyć obiekt
     *      samemu i oddać go kontenerowi.
     *   2. "plnFormat". Nazwa z @Bean("...") zastępuje nazwę metody; getBeanNamesForType(NumberFormat.class) nie zawiera
     *      "currencyFormat". Sprawdzone testem w Ioc04BeanMethodsTest.
     *   3. Nie — to inny obiekt (assertThat(...).isNotSameAs(...)). Wywołanie liteTaxTable() w trybie lekkim to zwykła
     *      metoda Javy, tworzy nową TaxTable. Sprawdzone testem w Ioc04BeanMethodsTest.
     *   4. Tak, to ten sam obiekt: bean ShopConfig jest proxy CGLIB, które przechwytuje wywołanie clock() i zwraca
     *      singletona z kontenera. Sprawdzone testem w Ioc04BeanMethodsTest.
     *   5. LocalDate.now() bierze datę z zegara systemu, więc wynik zależy od dnia uruchomienia, a test z datą 2026-10-11
     *      przejdzie tylko tego dnia. Naprawa: wstrzyknąć Clock i użyć LocalDate.now(clock) (w teście Clock.fixed).
     *   6. W trybie lekkim to wywołanie tworzy NOWĄ TaxTable, a nie beana. Poprawka: wziąć TaxTable jako parametr metody
     *      (jak w liteCalculatorFromParameter) albo przejść na tryb pełny. Sprawdzone testem w Ioc04BeanMethodsTest.
     *   7. @Component dla własnych klas z prostym tworzeniem; @Bean dla klas cudzych (JDK, biblioteki) i gdy tworzenie
     *      wymaga logiki.
     */
    // </editor-fold>
}
