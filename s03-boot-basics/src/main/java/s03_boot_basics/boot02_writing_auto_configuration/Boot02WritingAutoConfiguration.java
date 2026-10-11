package s03_boot_basics.boot02_writing_auto_configuration;

import org.springframework.context.ApplicationContext;

/**
 * <pre>
 * TEMAT: Własna autokonfiguracja — jak biblioteka „sama się konfiguruje” po dodaniu do projektu
 *        (auto-configuration = autokonfiguracja; imports = plik z listą klas; guard = bezpiecznik;
 *         application context runner = uruchamiacz kontekstu aplikacji, narzędzie testowe Boota)
 *
 * W SKRÓCIE:
 *   Autokonfiguracja to NIE magia — to zwykła klasa konfiguracyjna z metodami {@code @Bean}, oznaczona
 *   {@code @AutoConfiguration}, z warunkami {@code @ConditionalOn...} i wpisana do pliku
 *   META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports. Tak samo robią to twórcy
 *   bibliotek: dodajesz jar → jego plik .imports dołącza do listy kandydatów → beany pojawiają się same.
 *   W tej lekcji piszemy Boot02GreetingAutoConfiguration (domyślny GreetingService — usługa powitań) i testujemy ją
 *   ApplicationContextRunnerem: jest bean domyślny; bean użytkownika wygrywa; po wyłączeniu właściwością — brak beana.
 *
 * ANALOGIA: automat z kawą w biurze.
 *   Firma (biblioteka) stawia automat (bean domyślny), ale tylko jeśli biuro go zamówiło (właściwość enabled)
 *   i nie ma już własnego ekspresu (brak beana). Wpis na liście dostaw (plik .imports) sprawia, że kurier przyjeżdża
 *   do KAŻDEGO biura w budynku — dlatego automat ma warunek „tylko na zamówienie”.
 *
 * JAK TO DZIAŁA:
 *   src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 *     └── s03_boot_basics.boot02_writing_auto_configuration.Boot02GreetingAutoConfiguration
 *   start dowolnej aplikacji Boota w tym module (Boot01Application, Boot02Application, ...):
 *     1. kandydaci = 12 klas ze spring-boot-autoconfigure + NASZA klasa z pliku .imports tego modułu
 *     2. warunek {@code @ConditionalOnProperty(name = "boot02.greeting.enabled", havingValue = "true")}:
 *          brak/false → klasa pominięta (Negative match w raporcie)    true → idziemy dalej
 *     3. metoda greetingService: {@code @ConditionalOnMissingBean} → jest Twój GreetingService? tak → pomiń; nie → utwórz
 *
 * SŁÓWKA:
 *   greeting = powitanie; enabled = włączony; shop name = nazwa sklepu; user configuration = konfiguracja użytkownika;
 *   with property values = z wartościami właściwości; has bean / does not have bean = ma bean / nie ma beana;
 *   has single bean = ma dokładnie jeden bean (danego typu); starter = starter (zestaw zależności); proxy bean
 *   methods = pośredniczenie w metodach {@code @Bean}; before / after = przed / po.
 *
 * ZOBACZ TEŻ: s03_boot_basics/Boot01AutoConfiguration (warunki i raport), s03_boot_basics/Boot04Starters (startery),
 *             s02_configuration/Cfg02ConfigurationProperties ({@code @ConfigurationProperties}),
 *             s01_core_ioc/Ioc04BeanMethods (metody {@code @Bean}),
 *             t19_annotations_reflection/Annotations05MiniFramework (framework, który sam znajduje klasy).
 * </pre>
 */
public final class Boot02WritingAutoConfiguration {

    private Boot02WritingAutoConfiguration() {
    }

    // =================================================================================================
    // 1. KLASA AUTOKONFIGURACJI — Boot02GreetingAutoConfiguration
    // =================================================================================================
    //
    //   {@code @AutoConfiguration}                                                ← to jest autokonfiguracja
    //   {@code @ConditionalOnProperty(name = "boot02.greeting.enabled", havingValue = "true")}   ← bezpiecznik
    //   {@code @EnableConfigurationProperties(Boot02GreetingProperties.class)}    ← ustawienia boot02.greeting.*
    //   public class Boot02GreetingAutoConfiguration {
    //       {@code @Bean @ConditionalOnMissingBean}
    //       GreetingService greetingService(Boot02GreetingProperties properties) { ... }
    //   }
    //
    // {@code @AutoConfiguration} = {@code @Configuration(proxyBeanMethods = false)} + atrybuty before/after (kolejność
    // względem innych autokonfiguracji, np. after = JmxAutoConfiguration.class). proxyBeanMethods = false znaczy: Spring
    // nie tworzy podklasy-pośrednika (proxy) dla tej klasy — start jest szybszy, ale jedna metoda {@code @Bean} nie
    // może wołać drugiej i liczyć na ten sam obiekt. Zależności bierz z parametrów metody (jak properties wyżej).
    //
    // DOBRA PRAKTYKA: w autokonfiguracji każda metoda {@code @Bean} „publiczna dla użytkownika” ma
    //   {@code @ConditionalOnMissingBean}. Dlaczego? Użytkownik musi móc podmienić każdy element swoim beanem — inaczej
    //   dostanie dwa beany jednego typu i błąd „expected single matching bean but found 2” (oczekiwano jednego beana).

    // =================================================================================================
    // 2. REJESTRACJA — plik .imports
    // =================================================================================================
    //
    // Plik src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:
    //   # komentarze zaczynają się od #
    //   s03_boot_basics.boot02_writing_auto_configuration.Boot02GreetingAutoConfiguration
    // Maven kopiuje go do target/classes, a w bibliotece — do jara. Test sekcji 2 sprawdza, że ImportCandidates
    // (ten sam mechanizm, którego używa Boot) widzi naszą klasę.
    //
    // PUŁAPKA: nazwa pliku to PEŁNA nazwa adnotacji AutoConfiguration z rozszerzeniem .imports, a katalog to
    //   META-INF/spring (nie META-INF). Dlaczego to groźne? Literówka w nazwie pliku lub katalogu nie daje żadnego
    //   błędu — autokonfiguracja po prostu nie jest kandydatem i nigdy się nie włącza.
    //
    // Ciekawostka: klasa leży w pakiecie skanowanym przez Boot02Application, a mimo to nie jest rejestrowana jako
    //   zwykła {@code @Configuration}. {@code @SpringBootApplication} ma filtr AutoConfigurationExcludeFilter (filtr
    //   wykluczający autokonfiguracje), który przy skanowaniu pomija klasy wpisane do pliku .imports. W prawdziwej
    //   bibliotece i tak trzymamy autokonfiguracje w pakiecie, którego nikt nie skanuje.

    // =================================================================================================
    // 3. BEZPIECZNIK — dlaczego nasza autokonfiguracja jest domyślnie WYŁĄCZONA
    // =================================================================================================
    //
    // Plik .imports leży w src/main/resources modułu s03-boot-basics, więc jest na classpath KAŻDEJ aplikacji tego
    // modułu: Boot01Application, Boot03..., Boot04... — one też wczytają naszą klasę jako kandydata! To jest dokładnie
    // to, co robi prawdziwa biblioteka: dodajesz jar → jej autokonfiguracja działa w całej aplikacji.
    // Gdyby nie warunek {@code @ConditionalOnProperty(name = "boot02.greeting.enabled", havingValue = "true")}, bean
    // greetingService pojawiłby się w kontekstach innych lekcji i zmienił ich wyniki (np. liczbę beanów).
    // Test sekcji 3 uruchamia Boot01Application i sprawdza: bez właściwości — nie ma GreetingService, a raport warunków
    // pokazuje naszą klasę jako niespełnioną; z właściwością — bean pojawia się nawet tam.
    //
    // PUŁAPKA: „dodałem bibliotekę, a w mojej aplikacji pojawił się bean, którego nie chciałem”. Dlaczego? Każdy jar
    //   z plikiem .imports dokłada kandydatów do KAŻDEJ aplikacji, która ma go na classpath. Rozwiązania: właściwość
    //   wyłączająca (jeśli biblioteka ją ma), własny bean (back-off) albo exclude (Boot01AutoConfiguration, sekcja 6).
    //
    // DOBRA PRAKTYKA: funkcje „ciężkie” albo zmieniające zachowanie aplikacji włączaj właściwością ...enabled
    //   z jasnym matchIfMissing. Dlaczego? Sama obecność jara nie powinna niespodziewanie zmieniać działania aplikacji.

    // =================================================================================================
    // 4. TESTOWANIE — ApplicationContextRunner + AutoConfigurations.of(...)
    // =================================================================================================
    //
    //   new ApplicationContextRunner()
    //       .withConfiguration(AutoConfigurations.of(Boot02GreetingAutoConfiguration.class))  ← jak autokonfiguracja
    //       .withPropertyValues("boot02.greeting.enabled=true")                               ← właściwości
    //       .withUserConfiguration(Boot02CustomGreetingConfig.class)                          ← beany użytkownika
    //       .run(context -> assertThat(context).hasSingleBean(GreetingService.class));
    //
    // Runner tworzy MAŁY kontekst (bez skanowania, bez pliku .imports, bez application.properties) i sam go zamyka.
    // AutoConfigurations.of(...) rejestruje klasy PO konfiguracji użytkownika — dokładnie jak prawdziwy Boot, więc
    // test back-off jest wiarygodny. Trzy przypadki w teście sekcji 4:
    //   a) enabled=true, brak beana użytkownika → DefaultGreetingService
    //   b) enabled=true + Boot02CustomGreetingConfig → wygrywa bean użytkownika (vipGreetingService), domyślnego nie ma
    //   c) bez enabled (albo enabled=false) → brak GreetingService
    //
    // DOBRA PRAKTYKA: autokonfigurację testuj runnerem, a nie {@code @SpringBootTest}. Dlaczego? Runner startuje
    //   w milisekundach, każdy przypadek ma osobny, czysty kontekst, a kombinacje (właściwości × beany użytkownika ×
    //   brak klasy przez FilteredClassLoader) łatwo opisać w kilku linijkach.

    // =================================================================================================
    // 5. JAK TO WYGLĄDA W PRAWDZIWEJ BIBLIOTECE (teoria)
    // =================================================================================================
    //
    //   acme-greeting/                         kod biblioteki (GreetingService, DefaultGreetingService)
    //   acme-greeting-spring-boot-autoconfigure/  {@code @AutoConfiguration} + plik .imports (+ opcjonalnie
    //                                          spring-boot-configuration-processor — podpowiedzi właściwości w IDE)
    //   acme-greeting-spring-boot-starter/     sam pom.xml: zależy od dwóch powyższych (Boot04Starters)
    // Nazwa: własne startery nazywa się acme-spring-boot-starter (nazwa projektu NA POCZĄTKU). Przedrostek
    // spring-boot-starter-... jest zarezerwowany dla oficjalnych starterów Spring Boota.
    //
    // PUŁAPKA: wstrzykiwanie przez pola ({@code @Autowired}) i skanowanie pakietów w autokonfiguracji. Dlaczego źle?
    //   Autokonfiguracja ma być przewidywalna: wszystko, co tworzy, widać w metodach {@code @Bean} i ich warunkach.
    //   {@code @ComponentScan} w bibliotece wciągnąłby do aplikacji użytkownika klasy, o które nie prosił.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Autokonfiguracja = klasa {@code @AutoConfiguration} z metodami {@code @Bean} + warunki {@code @ConditionalOn...}.
     *   • Rejestracja: META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports (1 klasa w linii).
     *   • Każdy bean dla użytkownika: {@code @ConditionalOnMissingBean} — bean użytkownika wygrywa.
     *   • Plik .imports działa w KAŻDEJ aplikacji z tym jarem/modułem na classpath → bezpiecznik
     *     {@code @ConditionalOnProperty(..., havingValue = "true")} albo inny warunek.
     *   • Zależności przez parametry metod {@code @Bean} (proxyBeanMethods = false); żadnego {@code @ComponentScan}.
     *   • Test: ApplicationContextRunner + AutoConfigurations.of(...) + withPropertyValues + withUserConfiguration.
     *   • Własny starter: nazwa-spring-boot-starter (spring-boot-starter-* tylko dla oficjalnych).
     *
     * PYTANIA KONTROLNE:
     *   1. Z czego składa się autokonfiguracja i jak Spring Boot się o niej dowiaduje?
     *   2. Co się stanie? Uruchomisz Boot01Application (inna lekcja!) z właściwością boot02.greeting.enabled=true.
     *      Czy w jej kontekście będzie bean GreetingService?
     *   3. Co się stanie? W runnerze z enabled=true zarejestrujesz Boot02CustomGreetingConfig. Ile beanów
     *      GreetingService będzie w kontekście i który?
     *   4. ZNAJDŹ BŁĄD: autor biblioteki zapisał plik jako META-INF/spring/AutoConfiguration.imports. Testy z runnerem
     *      przechodzą, ale w aplikacji użytkownika autokonfiguracja nie działa. Dlaczego testy tego nie wykryły?
     *   5. ZNAJDŹ BŁĄD: metoda {@code @Bean GreetingService greetingService()} w autokonfiguracji nie ma
     *      {@code @ConditionalOnMissingBean}. Co zobaczy użytkownik, który ma własny GreetingService i wstrzykuje go
     *      przez konstruktor?
     *   6. Dlaczego nasza autokonfiguracja ma warunek havingValue = "true" zamiast matchIfMissing = true?
     *   7. Czym różni się ApplicationContextRunner od {@code @SpringBootTest} i dlaczego do autokonfiguracji wolimy runner?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Boot02WritingAutoConfigurationExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): pobierz z kontekstu bean GreetingService i zwróć powitanie dla klienta „Ania”.
     * W kontekście Boot02Application z profilem boot02 wynik to: „Witaj, Ania! Zapraszamy do sklepu Sklep pod Sprezyna.”
     * Podpowiedź: context.getBean(GreetingService.class).greet(...).
     */
    static String exercise1(ApplicationContext context) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // ĆWICZENIE 2 (średnie): uzupełnij klasę Boot02ExerciseGreetingConfig (opis w jej Javadoc).
    // ĆWICZENIE 3 (trudniejsze): uzupełnij klasę Boot02ExerciseLoyaltyAutoConfiguration (opis w jej Javadoc).

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Ze zwykłej klasy {@code @AutoConfiguration} z metodami {@code @Bean} i warunkami {@code @ConditionalOn...}.
     *      Boot dowiaduje się o niej z pliku META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
     *      (w jarze biblioteki albo w resources modułu), który czyta {@code @EnableAutoConfiguration}.
     *   2. Tak. Plik .imports jest na classpath całego modułu, więc Boot01Application też ma naszą klasę wśród
     *      kandydatów; właściwość spełnia warunek i bean powstaje. Bez właściwości — nie ma go, a raport warunków
     *      pokazuje Boot02GreetingAutoConfiguration jako niespełnioną. Sprawdzone testem w Boot02WritingAutoConfigurationTest
     *      (sekcja 3).
     *   3. Jeden — vipGreetingService użytkownika. Domyślny greetingService się wycofuje ({@code @ConditionalOnMissingBean}).
     *      Sprawdzone testem (sekcja 4).
     *   4. Runner z AutoConfigurations.of(...) w ogóle nie czyta plików .imports — sam rejestruje podane klasy. Test
     *      sprawdza więc logikę warunków, ale nie rejestrację. Dlatego warto mieć też test, który czyta kandydatów
     *      przez ImportCandidates.load(AutoConfiguration.class, ...) (jak test sekcji 2) albo startuje prawdziwą aplikację.
     *   5. Powstaną DWA beany typu GreetingService (użytkownika i domyślny), więc wstrzyknięcie przez konstruktor
     *      zakończy się błędem startu NoUniqueBeanDefinitionException — „expected single matching bean but found 2”
     *      (Spring nie wie, który wybrać). Sprawdzone testem (sekcja 4, Boot02NoBackOffAutoConfiguration).
     *      Wyjątek: gdy parametr konstruktora nazywa się tak jak jeden z beanów (tu: greetingService), Spring wybierze
     *      bean po nazwie i błędu nie będzie — to też sprawdza test. Nie licz na ten przypadek: to kruche „szczęście”.
     *   6. Bo plik .imports działa we wszystkich aplikacjach modułu. havingValue = "true" bez matchIfMissing znaczy
     *      „domyślnie wyłączona” — włącza ją tylko lekcja, która tego chce. matchIfMissing = true włączyłoby ją wszędzie.
     *   7. Runner tworzy mały kontekst tylko z tym, co mu podasz (bez skanowania, bez .imports, bez application.properties),
     *      startuje w milisekundach i sam się zamyka; łatwo w nim sprawdzić wiele kombinacji. {@code @SpringBootTest}
     *      startuje całą aplikację — wolniej i bez kontroli nad tym, co jeszcze jest w kontekście.
     */
    // </editor-fold>
}
