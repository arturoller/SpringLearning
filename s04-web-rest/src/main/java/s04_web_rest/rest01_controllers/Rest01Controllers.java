package s04_web_rest.rest01_controllers;

/**
 * <pre>
 * TEMAT: Kontrolery REST w Spring MVC — {@code @RestController}, {@code @RequestMapping} i adnotacje metod HTTP
 *        (controller = kontroler; mapping = mapowanie, czyli przypisanie adresu do metody; handler = metoda obsługująca)
 *
 * W SKRÓCIE:
 *   Kontroler to klasa, której metody odpowiadają na zapytania HTTP. Adnotacja mówi Springowi: „zapytanie GET pod adres
 *   /api/products obsługuje TA metoda”. Spring sam wywołuje metodę, a to, co zwróci, zamienia na JSON i wysyła klientowi.
 *   {@code @RestController} = {@code @Controller} + {@code @ResponseBody}. Na klasie {@code @RequestMapping("/api/x")}
 *   ustawia wspólny początek adresu, a na metodach {@code @GetMapping}, {@code @PostMapping}, {@code @PutMapping},
 *   {@code @PatchMapping}, {@code @DeleteMapping} wybierają metodę HTTP i resztę adresu.
 *   Zła metoda HTTP → 405, nieznany adres → 404, dwie metody na ten sam adres i metodę HTTP → aplikacja NIE wystartuje.
 *
 * ANALOGIA: recepcja w biurowcu.
 *   Tomcat to drzwi wejściowe, a DispatcherServlet to recepcjonistka. Każdy gość (zapytanie) mówi, do kogo idzie
 *   („GET /api/products/ELE-001”). Recepcjonistka sprawdza w spisie pokoi (HandlerMapping), który pracownik (metoda
 *   kontrolera) się tym zajmuje, i prowadzi gościa do niego. Odpowiedź pracownika tłumacz (konwerter wiadomości)
 *   przekłada na język gościa (JSON). Gdy w spisie są dwa pokoje z tym samym numerem — biuro w ogóle się nie otwiera.
 *
 * JAK TO DZIAŁA: droga jednego zapytania GET /api/products/ELE-001
 *   1. Tomcat (wbudowany serwer ze spring-boot-starter-webmvc) przyjmuje połączenie i woła servlet DispatcherServlet.
 *   2. DispatcherServlet pyta beany HandlerMapping, kto obsłuży zapytanie. RequestMappingHandlerMapping zna wszystkie
 *      metody z {@code @RequestMapping} — zebrał je PRZY STARCIE aplikacji (stąd błąd startu przy duplikatach).
 *   3. RequestMappingHandlerAdapter (adapter) przygotowuje argumenty metody ({@code @PathVariable}, {@code @RequestBody}…)
 *      i ją wywołuje: ProductController.one("ELE-001").
 *   4. Metoda zwraca obiekt Product. Dzięki {@code @ResponseBody} trafia on do HttpMessageConverter (konwertera
 *      wiadomości HTTP) — dla JSON-a to JacksonJsonHttpMessageConverter (Jackson 3, Spring Framework 7).
 *   5. Odpowiedź: 200 OK, Content-Type: application/json, ciało {"sku":"ELE-001",...}.
 *
 * SŁÓWKA:
 *   request = zapytanie; response = odpowiedź; body = ciało; header = nagłówek; status code = kod statusu;
 *   endpoint = punkt końcowy (endpoint) — adres + metoda HTTP obsługiwane przez kontroler; handler method = metoda
 *   obsługująca; dispatcher = rozdzielacz; message converter = konwerter wiadomości; view = widok (strona HTML);
 *   method not allowed = metoda niedozwolona (405); not found = nie znaleziono (404); ambiguous = niejednoznaczny;
 *   singleton = jedyny egzemplarz (jeden obiekt na całą aplikację).
 *
 * ZOBACZ TEŻ: s01_core_ioc/Ioc03Stereotypes ({@code @Controller} jako stereotyp — znacznik dla skanowania komponentów),
 *             s00_start/Start04TestingIntro (MockMvc i {@code @WebMvcTest}), s00_start/Start05HttpFiles (pliki .http),
 *             t34_toward_spring/Spring03RestConcepts (REST bez Springa), s04_web_rest/Rest02RequestData (dane zapytania),
 *             s04_web_rest/Rest03Responses (statusy i ResponseEntity), s11_testing/Test03MockMvcTester (MockMvcTester).
 * </pre>
 */
public final class Rest01Controllers {

    private Rest01Controllers() {
    }

    // =================================================================================================
    // 1. @RestController = @Controller + @ResponseBody (GreetingControllers)
    // =================================================================================================
    //
    // @Controller to stereotyp (znacznik) — mówi skanowaniu komponentów „to jest bean i to jest kontroler webowy”
    // (s01_core_ioc/Ioc03Stereotypes). Sam w sobie NIE decyduje, co zrobić z wynikiem metody. W klasycznym MVC
    // (Model-View-Controller = model-widok-kontroler) wynik metody to NAZWA WIDOKU, czyli szablonu strony HTML.
    // @ResponseBody zmienia to zachowanie: wynik metody idzie prosto do ciała odpowiedzi (przez konwerter wiadomości).
    // @RestController to adnotacja złożona: ma na sobie @Controller i @ResponseBody — w REST API używasz tylko jej.
    //
    // PRZED (klasyczny zapis):              PO (to samo, krócej):
    //   @Controller                           @RestController
    //   @ResponseBody                         class RestGreetingController { ... }
    //   class ClassicGreetingController {...}
    //
    // PUŁAPKA: sam @Controller bez @ResponseBody w aplikacji REST (ViewNameController). Metoda zwraca "witaj", a Spring
    //   uznaje to za nazwę widoku i przekazuje zapytanie dalej (forward) pod adres „witaj”. Nie ma tam żadnej strony,
    //   więc prawdziwy serwer odpowiada 404 Not Found — a w MockMvc widać tylko status 200 i forwardedUrl = "witaj"
    //   (MockMvc nie wykonuje przekazania). Dlaczego to podstępne? Kod się kompiluje, aplikacja startuje, a błąd wychodzi
    //   dopiero przy pierwszym zapytaniu. W REST API zawsze @RestController.

    // =================================================================================================
    // 2. @RequestMapping NA KLASIE + @GetMapping/@PostMapping/... NA METODACH (ProductController)
    // =================================================================================================
    //
    //   @RequestMapping("/api/products")      ← wspólny początek adresu (prefix) dla całej klasy
    //   @GetMapping                 → GET    /api/products                lista
    //   @GetMapping("/{sku}")       → GET    /api/products/{sku}          jeden produkt
    //   @PostMapping                → POST   /api/products                dodanie
    //   @PutMapping("/{sku}")       → PUT    /api/products/{sku}          zastąpienie CAŁEGO produktu
    //   @PatchMapping("/{sku}/stock") → PATCH /api/products/{sku}/stock   zmiana CZĘŚCI (tylko stan magazynu)
    //   @DeleteMapping("/{sku}")    → DELETE /api/products/{sku}          usunięcie
    //
    // @GetMapping to skrót od @RequestMapping(method = RequestMethod.GET) — tak samo pozostałe. Samo @RequestMapping
    // na METODZIE bez atrybutu method przyjmuje KAŻDĄ metodę HTTP (GET, POST, DELETE...) — rzadko tego chcesz.
    //
    // Metoda HTTP mówi, CO robimy z zasobem (resource — „rzecz”, do której prowadzi adres):
    //   GET — odczyt (nic nie zmienia), POST — utworzenie, PUT — zastąpienie w całości, PATCH — zmiana części,
    //   DELETE — usunięcie. Adres to rzeczownik (/api/products), a nie czasownik (/api/getProducts, /api/deleteProduct).
    //
    // DOBRA PRAKTYKA: adresy w liczbie mnogiej i bez czasowników: GET /api/products/ELE-001, DELETE /api/products/ELE-001.
    //   Dlaczego? Metoda HTTP już mówi, co robisz; adres tylko wskazuje „na czym”. Klient zgadnie resztę API sam.
    //
    // Metoda void (DELETE) → status 200 i puste ciało. Wynik null, 201 Created i 204 No Content —
    // w s04_web_rest/Rest03Responses.

    // =================================================================================================
    // 3. POD MASKĄ: DispatcherServlet → HandlerMapping → HandlerAdapter → konwerter (test: underTheHood)
    // =================================================================================================
    //
    // Boot (autokonfiguracja WebMvcAutoConfiguration i DispatcherServletAutoConfiguration) rejestruje m.in. beany:
    //   dispatcherServlet                — jedyny servlet aplikacji; wszystkie zapytania przechodzą przez niego,
    //   requestMappingHandlerMapping     — mapa „warunki zapytania → metoda kontrolera” zbudowana przy starcie,
    //   requestMappingHandlerAdapter     — wywołuje metodę i ma listę konwerterów wiadomości (HttpMessageConverter).
    // Test underTheHood pyta RequestMappingHandlerMapping, którą metodę wybrałby dla GET /api/products/{sku}, i sprawdza,
    // że wśród konwerterów jest JacksonJsonHttpMessageConverter.
    //
    // PUŁAPKA: stare tutoriale. W Spring Framework 6 konwerter JSON nazywał się MappingJackson2HttpMessageConverter
    //   (Jackson 2). W Spring Framework 7 + Boot 4 domyślny jest JacksonJsonHttpMessageConverter (Jackson 3, pakiet
    //   tools.jackson). Dlaczego to ważne? Kod z internetu, który szuka starego konwertera, po prostu go nie znajdzie.
    //   Więcej o Jacksonie 3 — s04_web_rest/Rest04Json.

    // =================================================================================================
    // 4. BŁĘDY MAPOWANIA: 405, 404, duplikat (błąd startu), dwa wzorce na ten sam adres (500)
    // =================================================================================================
    //
    // a) Adres istnieje, ale nie dla tej metody HTTP: POST /api/greeting/rest → 405 Method Not Allowed. Spring rzuca
    //    HttpRequestMethodNotSupportedException („Request method 'POST' is not supported”) i dodaje nagłówek
    //    Allow: GET — lista metod, które ZADZIAŁAJĄ pod tym adresem.
    // b) Adres nie istnieje: GET /api/nothing → 404 Not Found. Żaden kontroler nie pasuje, więc zapytanie trafia do
    //    obsługi plików statycznych (static resources — np. obrazki z katalogu static/), która rzuca
    //    NoResourceFoundException („No static resource api/nothing.”). Stąd w logach mowa o „static resource”,
    //    choć nie prosiłeś o żaden plik.
    // c) Dwie metody z tym samym adresem I tą samą metodą HTTP (np. dwa razy @GetMapping("/{sku}")) — aplikacja NIE
    //    WYSTARTUJE: IllegalStateException: Ambiguous mapping. Cannot map '...' method ... to {GET [/api/products/{sku}]}:
    //    There is already '...' bean method ... mapped. Test: Rest01ControllersStartupTest.
    // d) Dwa RÓŻNE wzorce, które pasują do tego samego adresu: @GetMapping("/{sku}") i @GetMapping("/{id}"). Dla Springa to
    //    różne wzorce, więc start się udaje — a pierwsze zapytanie GET kończy się 500 i wyjątkiem
    //    IllegalStateException: Ambiguous handler methods mapped for '/api/x/1': {...}. Test: Rest01ControllersStartupTest.
    //
    // PUŁAPKA: 405 zamiast 404 myli początkujących („przecież adres jest dobry!”). Dlaczego 405? Adres jest dobry, ale
    //   metoda HTTP nie — sprawdź nagłówek Allow w odpowiedzi, podpowiada, jakiej metody użyć.
    //
    // DOBRA PRAKTYKA: wspólny prefiks na klasie (@RequestMapping("/api/products")) i JEDNA nazwa zmiennej ścieżki
    //   ({sku}) w całym kontrolerze. Dlaczego? Duplikaty i niejednoznaczne wzorce łatwiej wtedy zauważyć gołym okiem.

    // =================================================================================================
    // 5. KONTROLER JEST SINGLETONEM (test: controllerIsSingletonWithSharedState)
    // =================================================================================================
    //
    // Kontroler to zwykły bean o zasięgu singleton (s01_core_ioc/Ioc06Scopes): JEDEN obiekt obsługuje WSZYSTKIE zapytania,
    // równolegle, w wielu wątkach Tomcata. Pole products w ProductController widzą wszyscy klienci — produkt dodany
    // POST-em przez jednego klienta zobaczy w GET każdy inny.
    //
    // PUŁAPKA: (PYTANIE REKRUTACYJNE: „czy kontroler Springa jest bezpieczny wątkowo?”) pole z danymi JEDNEGO zapytania
    //   w kontrolerze, np. private String currentUser; ustawiane na początku metody. Dlaczego to błąd? Dwa zapytania naraz
    //   nadpisują sobie to samo pole (wyścig — t21_concurrency/Concurrency02RaceConditions). Dane zapytania trzymaj
    //   w parametrach i zmiennych lokalnych; wspólny stan — tylko bezpieczny wątkowo (ConcurrentHashMap) albo w bazie.

    // =================================================================================================
    // 6. TESTY KONTROLERÓW: @WebMvcTest + MockMvc (zapowiedź MockMvcTester)
    // =================================================================================================
    //
    // Rest01ControllersTest używa @WebMvcTest(controllers = ...) — wycinka (slice) kontekstu: tylko warstwa webowa
    // (kontrolery, konwertery, DispatcherServlet), bez serwera i sieci. MockMvc „udaje” zapytanie HTTP i woła
    // DispatcherServlet bezpośrednio. Rest01ControllersRealServerTest startuje PRAWDZIWY Tomcat na losowym porcie —
    // tylko tam widać to, co robi serwer, np. stronę błędu Boota (JSON z polami timestamp, status, error, path).
    // Test mockMvcTesterPreview pokazuje nowszy styl: MockMvcTester (od Spring Framework 6.2) z asercjami AssertJ —
    // całość w s11_testing/Test03MockMvcTester.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @RestController = @Controller + @ResponseBody → wynik metody idzie do ciała (JSON/tekst), nie do widoku.
     *   • @RequestMapping("/api/x") na klasie = wspólny prefiks; @GetMapping/@PostMapping/@PutMapping/@PatchMapping/
     *     @DeleteMapping na metodach = metoda HTTP + reszta adresu.
     *   • Droga zapytania: Tomcat → DispatcherServlet → RequestMappingHandlerMapping → RequestMappingHandlerAdapter →
     *     metoda kontrolera → HttpMessageConverter (JacksonJsonHttpMessageConverter) → odpowiedź.
     *   • Zła metoda HTTP → 405 + nagłówek Allow. Nieznany adres → 404 (NoResourceFoundException).
     *   • Ten sam adres + ta sama metoda HTTP w dwóch metodach → błąd STARTU „Ambiguous mapping”.
     *   • Różne wzorce pasujące do jednego adresu ({sku} i {id}) → 500 przy zapytaniu „Ambiguous handler methods”.
     *   • Metoda void → 200 i puste ciało. Sam @Controller zwracający String → nazwa widoku (w REST to błąd).
     *   • Kontroler = singleton → żadnych pól z danymi pojedynczego zapytania.
     *
     * PYTANIA KONTROLNE:
     *   1. Z jakich dwóch adnotacji składa się @RestController i co robi każda z nich?
     *   2. Co się stanie? Dopiszesz do ProductController drugą metodę z @GetMapping("/{sku}") (np. oneAgain) i uruchomisz
     *      Rest01Application.
     *   3. Co się stanie? Wyślesz POST /api/products/ELE-001 (adres istnieje, ale tylko dla GET, PUT i DELETE) — jaki status
     *      i jaki nagłówek dostaniesz?
     *   4. ZNAJDŹ BŁĄD: klasa z @Controller (bez @ResponseBody) i metodą @GetMapping("/api/hello") String hello()
     *      { return "Cześć"; } — klient dostaje 404 zamiast tekstu „Cześć”. Dlaczego?
     *   5. Który bean zbiera mapowania wszystkich metod kontrolerów i kiedy to robi?
     *   6. ZNAJDŹ BŁĄD: w kontrolerze jest pole private Long lastProductId; ustawiane w metodzie GET /{id} i czytane
     *      kilka linii niżej. Przy małym ruchu działa, w produkcji klienci czasem dostają cudze dane. Dlaczego?
     *   7. Czym różni się @GetMapping od @RequestMapping bez atrybutu method umieszczonego na metodzie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w klasie Rest01ExerciseController (adresy /api/cw01/...), testy: Rest01ControllersExercisesTest.
    // Rozwiązania wzorcowe: s04_web_rest.solutions.rest01_controllers (zajrzyj po własnej próbie).
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. @Controller — stereotyp: klasa staje się beanem i kontrolerem webowym (skanowanie komponentów ją znajdzie,
     *      RequestMappingHandlerMapping zbierze jej metody). @ResponseBody — wynik metody trafia do ciała odpowiedzi
     *      przez HttpMessageConverter, zamiast być traktowany jako nazwa widoku.
     *   2. Aplikacja nie wystartuje. Tworzenie beana requestMappingHandlerMapping kończy się BeanCreationException
     *      z przyczyną IllegalStateException: „Ambiguous mapping. Cannot map 'productController' method
     *      ...ProductController#oneAgain(String) to {GET [/api/products/{sku}]}: There is already 'productController'
     *      bean method ...ProductController#one(String) mapped.” (kolejność metod w komunikacie zależy od kolejności
     *      rejestracji). Sprawdzone uruchomieniem: Rest01ControllersStartupTest.duplicateMappingStopsStartup.
     *   3. 405 Method Not Allowed i nagłówek Allow z metodami dostępnymi pod tym adresem: GET, PUT i DELETE (raz
     *      widziałem „PUT, GET, DELETE”, raz „GET, DELETE, PUT” — kolejność nie jest gwarantowana). Sprawdzone testem
     *      methodNotAllowed i zapytaniem 11 z pliku rest01_controllers.http.
     *   4. Brak @ResponseBody (albo @Controller zamiast @RestController). Spring traktuje "Cześć" jako nazwę widoku
     *      i przekazuje zapytanie pod adres „Cześć”, którego nie ma → 404. Sprawdzone testami
     *      viewNameInsteadOfBody (MockMvc: forwardedUrl) i Rest01ControllersRealServerTest (prawdziwy serwer: 404).
     *   5. RequestMappingHandlerMapping — przy starcie aplikacji (podczas tworzenia beana, w afterPropertiesSet).
     *      Dlatego duplikat mapowania psuje start, a nie dopiero pierwsze zapytanie.
     *   6. Kontroler jest singletonem — jeden obiekt dla wszystkich zapytań, obsługiwanych równolegle w wielu wątkach.
     *      Pole lastProductId jest wspólne: zapytanie B nadpisuje je między zapisem a odczytem w zapytaniu A (wyścig).
     *      Poprawka: zmienna lokalna albo parametr metody.
     *   7. @GetMapping przyjmuje tylko GET. @RequestMapping("/x") na metodzie bez method przyjmuje KAŻDĄ metodę HTTP
     *      (GET, POST, PUT, DELETE...) — zwykle niezamierzenie.
     */
    // </editor-fold>
}
