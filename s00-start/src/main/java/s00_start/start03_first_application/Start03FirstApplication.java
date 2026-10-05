package s00_start.start03_first_application;

/**
 * <pre>
 * TEMAT: Pierwsza aplikacja Spring Boot — klasa startowa, skanowanie komponentów, kontroler, uruchamianie
 *        (application = aplikacja; component scan = skanowanie komponentów; controller = kontroler; run = uruchom)
 *
 * W SKRÓCIE:
 *   Aplikacja Spring Boot to klasa z @SpringBootApplication i metodą main wywołującą SpringApplication.run(...).
 *   Spring przeszukuje pakiet tej klasy (i podpakiety), tworzy beany z klas oznaczonych @Service, @RestController itd.,
 *   łączy je ze sobą (wstrzykuje zależności) i startuje wbudowany serwer Tomcat na porcie 8080.
 *   Ty piszesz tylko logikę (GreetingService) i mapowanie HTTP (GreetingController) — resztę robi framework.
 *
 * ANALOGIA: otwarcie restauracji.
 *   main to klucz w drzwiach. Spring jako kierownik sprawdza listę pracowników (skanowanie pakietu), przydziela im
 *   stanowiska (beany), mówi kelnerowi, który kucharz gotuje (wstrzykiwanie zależności), i otwiera drzwi dla gości
 *   (Tomcat na porcie 8080). Gość (przeglądarka, plik .http) składa zamówienie u kelnera (kontroler), kelner przekazuje je
 *   kucharzowi (serwis) i odnosi gotowe danie (JSON).
 *
 * JAK TO DZIAŁA:
 *   main → SpringApplication.run(Start03Application.class)
 *     1. skanowanie pakietu s00_start.start03_first_application → znaleziono GreetingService (@Service)
 *        i GreetingController (@RestController)
 *     2. tworzenie beanów: najpierw GreetingService, potem GreetingController(greetingService) — przez konstruktor
 *     3. autokonfiguracja: DispatcherServlet, Jackson (JSON), Tomcat
 *     4. log: „Tomcat started on port 8080” — aplikacja czeka na zapytania
 *   GET /api/hello?name=Ala → DispatcherServlet → GreetingController.hello("Ala") → GreetingService.greet("Ala")
 *     → Greeting("Cześć, Ala!") → Jackson → {"message":"Cześć, Ala!"}
 *
 * SŁÓWKA:
 *   service = serwis (logika biznesowa); rest controller = kontroler REST; mapping = mapowanie (adres → metoda);
 *   request parameter = parametr zapytania (?name=Ala); default value = wartość domyślna; port = port (numer „drzwi”
 *   serwera); embedded server = wbudowany serwer; log = dziennik zdarzeń aplikacji; dependency injection = wstrzykiwanie
 *   zależności; constructor injection = wstrzykiwanie przez konstruktor.
 *
 * ZOBACZ TEŻ: t34_toward_spring/Spring01IocContainer (własny kontener IoC — to samo „ręcznie”),
 *             t34_toward_spring/Spring02Layers (warstwy kontroler–serwis–repozytorium),
 *             t28_networking_http/Http03LocalServer (serwer HTTP w czystej Javie),
 *             s00_start/Start04TestingIntro (jak testować tę aplikację), s00_start/Start05HttpFiles (pliki .http).
 * </pre>
 */
public final class Start03FirstApplication {

    private Start03FirstApplication() {
    }

    // =================================================================================================
    // 1. KLASA STARTOWA — @SpringBootApplication i main (plik Start03Application)
    // =================================================================================================
    //
    // @SpringBootApplication łączy trzy adnotacje: @Configuration, @EnableAutoConfiguration i @ComponentScan.
    // Najważniejsze dla Ciebie: @ComponentScan skanuje pakiet klasy startowej i WSZYSTKIE jego podpakiety.
    //
    // PUŁAPKA: klasa leżąca POZA pakietem klasy startowej (np. wyżej, w s00_start) nie zostanie znaleziona. Dlaczego?
    //   Skanowanie idzie tylko w dół drzewa pakietów. Kontroler, który potrzebuje takiego serwisu, nie wystartuje:
    //   „required a bean of type ... that could not be found” (wymagany bean nie został znaleziony).
    //
    // DOBRA PRAKTYKA: klasę startową kładź w pakiecie głównym aplikacji, a resztę klas w nim lub w podpakietach.
    //   W tym kursie „pakietem głównym” każdej lekcji jest jej podpakiet — stąd osobna klasa startowa na lekcję.

    // =================================================================================================
    // 2. BEANY I WSTRZYKIWANIE ZALEŻNOŚCI (GreetingService, GreetingController)
    // =================================================================================================
    //
    // GreetingController potrzebuje GreetingService. NIE tworzy go przez new — dostaje go w konstruktorze od Springa.
    // Dlaczego to lepsze? Kontroler nie wie, skąd pochodzi serwis, więc w teście można podać inny (atrapę) —
    // zobaczysz to w Start04TestingIntro. Spring tworzy po JEDNYM obiekcie każdego beana (singleton) i współdzieli go.
    //
    // PUŁAPKA: beany innej lekcji (np. PriceCalculator z Start04TestingIntro) NIE istnieją w kontekście tej aplikacji.
    //   Dlaczego? Leżą w pakiecie-rodzeństwie, którego Start03Application nie skanuje. To celowa izolacja lekcji.

    // =================================================================================================
    // 3. URUCHAMIANIE I ZATRZYMYWANIE
    // =================================================================================================
    //
    // IntelliJ: ▶ obok main w Start03Application → okno Run → log kończy się linią w stylu:
    //   „Tomcat started on port 8080 (http) with context path '/'” — aplikacja działa i NIE kończy się sama.
    // Sprawdź: przeglądarka → http://localhost:8080/api/hello?name=Ala albo plik s00-start/http/start03_first_application.http.
    // Zatrzymanie: czerwony kwadrat ■ w oknie Run (albo Ctrl+F2).
    // Linia poleceń (z korzenia repozytorium):
    //   ./mvnw -pl s00-start spring-boot:run -Dspring-boot.run.main-class=s00_start.start03_first_application.Start03Application
    // (w module jest kilka klas startowych, więc trzeba wskazać, którą uruchomić).
    //
    // PUŁAPKA: drugie uruchomienie, gdy pierwsza aplikacja wciąż działa, kończy się błędem „Port 8080 was already in use”
    //   (port 8080 jest już zajęty). Dlaczego? Na jednym porcie może nasłuchiwać tylko jeden program. Zatrzymaj starą
    //   aplikację albo uruchom nową z innym portem: argument --server.port=8081 (Run → Edit Configurations → Program arguments).

    // =================================================================================================
    // 4. PARAMETRY ZAPYTANIA I WARTOŚĆ DOMYŚLNA
    // =================================================================================================
    //
    // @RequestParam(defaultValue = "świecie") String name:
    //   /api/hello?name=Ala → "Ala";  /api/hello → "świecie";  /api/hello?name= (pusty) → TEŻ "świecie".
    //
    // PUŁAPKA: wartość domyślna działa także dla PUSTEGO parametru, nie tylko dla brakującego. Dlaczego to ważne?
    //   Jeśli Twoja logika miała odróżniać „nie podano” od „podano pusty tekst”, defaultValue to ukryje.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan (pakiet klasy + podpakiety).
     *   • main: SpringApplication.run(KlasaStartowa.class, args) — startuje kontekst i serwer Tomcat (port 8080).
     *   • @Service — logika; @RestController + @GetMapping("/adres") — obsługa HTTP; wynik metody → JSON (Jackson).
     *   • Zależności przez konstruktor (bez new, jeden konstruktor nie potrzebuje @Autowired).
     *   • @RequestParam(defaultValue = "...") — wartość domyślna dla brakującego LUB pustego parametru.
     *   • Port zajęty → zatrzymaj starą aplikację albo --server.port=8081.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie trzy adnotacje zawiera @SpringBootApplication i co robi każda z nich?
     *   2. Co się stanie? Przeniesiesz GreetingService do pakietu s00_start (poziom wyżej) i uruchomisz Start03Application.
     *   3. Co się stanie? Zapytanie GET /api/hello?name= (pusty parametr) — jaki będzie JSON?
     *   4. ZNAJDŹ BŁĄD: w kontrolerze jest pole {@code private final GreetingService service = new GreetingService();}.
     *      Działa, ale co jest nie tak z punktu widzenia Springa i testów?
     *   5. Co się stanie? Uruchomisz Start03Application drugi raz, nie zatrzymując pierwszej.
     *   6. Dlaczego w module s00-start nie wystarczy samo ./mvnw -pl s00-start spring-boot:run?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Start03FirstApplicationExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): powitanie, które dla null, pustego i „białego” tekstu (same spacje) zwraca "Cześć, nieznajomy!",
     * a w pozostałych przypadkach "Cześć, " + imię bez spacji na brzegach + "!". Przykład: "  Ola " → "Cześć, Ola!".
     * Podpowiedź: String.isBlank() i String.strip().
     */
    static String exercise1(String name) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): wyciągnij numer portu z linii logu, np.
     * "Tomcat started on port 8080 (http) with context path '/'" → 8080. Gdy w linii nie ma „port liczba”, rzuć
     * IllegalArgumentException. Podpowiedź: Pattern.compile("port (\\d+)") i Matcher.find() (t04_strings/Strings05Regex).
     */
    static int exercise2(String logLine) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zbuduj polecenie uruchamiające klasę startową z linii poleceń.
     * Linux/macOS (windows = false): "./mvnw -pl s00-start spring-boot:run -Dspring-boot.run.main-class=" + klasa,
     * Windows (windows = true): to samo, ale zamiast "./mvnw" jest "mvnw.cmd".
     * Gdy nazwa klasy nie kończy się na "Application", rzuć IllegalArgumentException (to raczej nie klasa startowa).
     */
    static String exercise3(String module, String mainClass, boolean windows) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. @Configuration — klasa może definiować beany (metody @Bean); @EnableAutoConfiguration — Boot dokłada beany
     *      zależnie od classpath (np. DispatcherServlet, Tomcat); @ComponentScan — skanuje pakiet klasy i podpakiety
     *      w poszukiwaniu @Component/@Service/@Repository/@RestController.
     *   2. Aplikacja się nie uruchomi. GreetingService leży poza skanowanym pakietem, więc nie ma takiego beana, a
     *      GreetingController go wymaga: „Parameter 0 of constructor in ...GreetingController required a bean of type
     *      ...GreetingService that could not be found.” Sprawdzone uruchomieniem.
     *   3. {"message":"Cześć, świecie!"} — defaultValue działa też dla pustego parametru (test w Start03FirstApplicationTest).
     *   4. Serwis tworzony przez new nie jest beanem: Spring o nim nie wie, nie poda w nim swoich zależności, a w teście
     *      nie da się go podmienić na atrapę. Poprawnie: pole final + konstruktor z parametrem GreetingService.
     *   5. Druga aplikacja nie wystartuje — port 8080 zajmuje pierwsza. W logu: „Web server failed to start. Port 8080 was
     *      already in use.” Sprawdzone uruchomieniem.
     *   6. Bo w module jest kilka klas z main (po jednej na lekcję) i plugin Spring Boota nie wie, którą wybrać.
     *      Trzeba wskazać ją parametrem -Dspring-boot.run.main-class=pełna.nazwa.Klasy.
     */
    // </editor-fold>
}
