package s04_web_rest.rest01_controllers;

/**
 * <pre>
 * TEMAT: Kontrolery REST — jak zapytanie HTTP trafia do metody Javy
 *        (controller = kontroler, klasa przyjmująca zapytania; mapping = mapowanie, przypisanie adresu do metody;
 *         REST — Representational State Transfer, styl budowania API przez HTTP; API — interfejs programistyczny)
 *
 * W SKRÓCIE:
 *   Kontroler REST to zwykła klasa z adnotacją @RestController. Jej metody oznaczasz @GetMapping, @PostMapping
 *   i podobnymi — każda mówi: „na takie zapytanie (metoda HTTP + ścieżka) odpowiadam ja”. To, co metoda zwróci,
 *   Spring sam zamienia na ciało odpowiedzi: String na tekst, rekord na JSON. Nie piszesz serwletów, nie parsujesz
 *   adresów, nie ustawiasz Content-Type ręcznie — adnotacje opisują CO ma się stać, a Spring robi resztę.
 *
 * ANALOGIA: recepcja w dużym biurowcu.
 *   Recepcjonistka (DispatcherServlet) nie załatwia spraw sama. Patrzy na kartkę gościa: „dostawa paczki” (metoda HTTP)
 *   do „księgowości, piętro 3” (ścieżka) i odsyła go do właściwego pokoju (metody kontrolera). Gdy takiego pokoju
 *   nie ma — „nie ma takiego biura” (404); gdy pokój jest, ale nie przyjmuje paczek — „tu tylko faktury” (405);
 *   gdy gość chce odpowiedź po chińsku, a biuro pisze tylko po polsku i angielsku — „nie dogadamy się” (406).
 *
 * JAK TO DZIAŁA:
 *   1. Przeglądarka lub klient (curl, plik .http, JavaScript) wysyła zapytanie: metoda HTTP + ścieżka + nagłówki + ciało.
 *   2. Wbudowany serwer (Tomcat) oddaje je jedynemu serwletowi Spring MVC: DispatcherServlet
 *      (dispatcher = dyspozytor; servlet = komponent serwera obsługujący zapytania).
 *   3. DispatcherServlet pyta HandlerMapping (mapowanie na obsługę): „która metoda obsługuje GET /rest01/hello?”.
 *      Odpowiedź zna z adnotacji @RequestMapping/@GetMapping, które zebrał przy starcie aplikacji.
 *   4. HandlerAdapter (adapter obsługi) wywołuje metodę, podając jej argumenty (od lekcji Rest02RequestData).
 *   5. Wartość zwrócona przez metodę trafia do HttpMessageConverter (konwerter wiadomości HTTP): String → tekst,
 *      rekord/obiekt → JSON (Jackson). Konwerter wybierany jest po typie wartości i nagłówku Accept zapytania.
 *   6. Serwer odsyła odpowiedź: status (domyślnie 200), nagłówki (m.in. Content-Type) i ciało.
 *
 * SŁÓWKA:
 *   request = zapytanie; response = odpowiedź; path = ścieżka; handler = obsługa (metoda, która obsługuje zapytanie);
 *   status code = kod statusu; header = nagłówek; body = ciało; content type = typ treści; consume = przyjmować,
 *   produce = wytwarzać, oddawać; media type = typ mediów (np. application/json); view = widok; prefix = przedrostek;
 *   ambiguous = niejednoznaczny; trailing slash = końcowy ukośnik.
 *
 * ZOBACZ TEŻ: s00_start/Start04TestingIntro (pierwszy kontroler i test MockMvc),
 *             s03_boot_basics/Boot01AutoConfiguration (skąd Boot bierze DispatcherServlet i konwertery),
 *             s04_web_rest/Rest02RequestData (dane z zapytania: ścieżka, parametry, nagłówki, ciało),
 *             s04_web_rest/Rest03Responses (statusy i nagłówki odpowiedzi),
 *             t09_records/Records01Basics (rekordy — z nich robimy JSON),
 *             t10_exceptions/Exceptions01Basics (wyjątki — przydadzą się przy błędach HTTP).
 * </pre>
 */
public final class Rest01Controllers {

    private Rest01Controllers() {
    }

    // =================================================================================================
    // 1. @RestController — NAJPROSTSZY KONTROLER (HelloController, test: restControllerIsControllerPlusResponseBody)
    // =================================================================================================
    //
    //   @RestController                      // „ta klasa przyjmuje zapytania HTTP i zwraca dane, nie widoki”
    //   @RequestMapping("/rest01")           // prefiks wszystkich ścieżek tej klasy
    //   public class HelloController {
    //       @GetMapping("/hello")            // GET /rest01/hello
    //       public String hello() { return "Witaj w sklepie: Sklep DP21!"; }
    //   }
    //
    // @RestController to SKRÓT: @Controller + @ResponseBody.
    //   • @Controller (kontroler) — klasa jest beanem (ziarnem) obsługującym zapytania. W klasycznych aplikacjach
    //     z szablonami HTML zwrócony String oznacza NAZWĘ WIDOKU (view): „pokaż stronę o takiej nazwie”.
    //   • @ResponseBody (ciało odpowiedzi) — „zwrócona wartość JEST treścią odpowiedzi, nie nazwą widoku”.
    // Test sekcji 1 sprawdza to uruchomieniem:
    //   a) HelloController ma obie adnotacje (przez @RestController), ClassicController nie ma @ResponseBody;
    //   b) /rest01/classic-body (@Controller + @ResponseBody na metodzie) zwraca tekst — jak @RestController;
    //   c) /rest01/classic-view (@Controller bez @ResponseBody) NIE zwraca napisu „nazwa-widoku” jako treści:
    //      ciało jest puste, a Spring próbuje przekazać sterowanie do widoku o tej nazwie (forward = przekazanie dalej).
    //
    // Skąd Spring zna kontroler? @SpringBootApplication zawiera @ComponentScan; @RestController jest „stereotypem”
    // (@Component), więc klasa w skanowanym pakiecie sama staje się beanem — nie rejestrujesz jej nigdzie ręcznie.
    // Ustawienie rest01.shop-name (w konstruktorze przez @Value) pokazuje, że kontroler jest zwykłym beanem: dostaje
    // wstrzyknięte zależności i ustawienia jak każdy inny (s01_core_ioc/Ioc02ConstructorInjection).
    //
    // DOBRA PRAKTYKA: kontroler ma być CIENKI — przyjmuje zapytanie, woła serwis, zwraca wynik. Dlaczego? Logika
    //   biznesowa w kontrolerze nie da się użyć z innego miejsca (zadanie cykliczne, kolejka) i trudno ją testować
    //   bez HTTP. Kontroler to „tłumacz” między HTTP a Twoim kodem.
    //
    // PUŁAPKA: @Controller zamiast @RestController w API zwracającym dane. Dlaczego? Zwrócony String czy obiekt
    //   zostanie potraktowany jako nazwa widoku — zamiast danych dostaniesz pustą odpowiedź albo błąd, i to bez
    //   żadnej wskazówki w kodzie. W API REST używaj @RestController.

    // =================================================================================================
    // 2. MAPOWANIA — @RequestMapping NA KLASIE I @GetMapping… NA METODACH (OrdersController)
    // =================================================================================================
    //
    //   @RequestMapping("/rest01/orders")   na klasie  → wspólny prefiks
    //   @GetMapping      GET     → pobierz (odczyt, nic nie zmienia)
    //   @PostMapping     POST    → utwórz coś nowego
    //   @PutMapping      PUT     → podmień w całości
    //   @PatchMapping    PATCH   → zmień część
    //   @DeleteMapping   DELETE  → usuń
    //
    // Skrótowe adnotacje to @RequestMapping(method = …) w krótszej formie: @GetMapping("/x") ≡
    // @RequestMapping(path = "/x", method = RequestMethod.GET). W starszym kodzie zobaczysz ten dłuższy zapis (metoda
    // legacy() w OrdersController).
    // Pod jedną ścieżką /rest01/orders żyje PIĘĆ metod Javy — wybiera między nimi metoda HTTP zapytania. To podstawa
    // REST: adres mówi „o czym” (zamówienia), metoda HTTP mówi „co z tym zrobić”.
    //
    // Test sekcji 2 (uruchomiony, nie policzony w głowie):
    //   a) GET/POST/PUT/PATCH/DELETE na /rest01/orders trafiają do pięciu różnych metod;
    //   b) POST na /rest01/hello (mapowanie TYLKO dla GET) → 405 Method Not Allowed, a nagłówek odpowiedzi
    //      Allow: GET podpowiada, co wolno (405 = „ścieżka jest, ale nie dla tej metody HTTP”);
    //   c) GET na /rest01/nie-ma-takiej → 404 Not Found („ścieżki nie ma wcale”).
    //
    // Spring 6.1+ (i dalej 7): nieznana ścieżka to wyjątek NoResourceFoundException (nie znaleziono zasobu) — bo
    // zapytanie „spada” do obsługi plików statycznych, która też nic nie znajduje. Dla klienta to nadal zwykłe 404.
    //
    // PUŁAPKA: pomylone metody HTTP: pobieranie danych przez POST albo usuwanie przez GET. Dlaczego źle?
    //   GET powinien być bezpieczny — przeglądarki, roboty indeksujące i pamięć podręczna wolno go wysyłać
    //   „na zapas”; kto usuwa dane przez GET, straci je, gdy robot odwiedzi link.
    //
    // DOBRA PRAKTYKA: prefiks wspólny dla klasy w @RequestMapping, a w metodach tylko końcówka. Dlaczego? Zmiana
    //   adresu (np. dodanie /api/v1) to jedna edycja, a nie szukanie po wszystkich metodach.

    // =================================================================================================
    // 3. POD SPODEM — HandlerMapping I HttpMessageConverter (HelloController, test: messageConvertersChooseContentType)
    // =================================================================================================
    //
    // Konwerter wybiera typ odpowiedzi po tym, CO zwraca metoda:
    //   • String        → StringHttpMessageConverter        → Content-Type: text/plain;charset=UTF-8
    //   • rekord/obiekt → JacksonJsonHttpMessageConverter   → Content-Type: application/json
    // Test sekcji 3 sprawdza oba nagłówki, a drugi test (underTheHood) zagląda do środka: kontekst ma
    // RequestMappingHandlerMapping, który zna wpis „GET [/rest01/hello]”, oraz RequestMappingHandlerAdapter z oboma
    // konwerterami. To nie magia — to zwykłe beany, które możesz wstrzyknąć i obejrzeć.
    //
    // BOOT 4 / FRAMEWORK 7 vs STARE TUTORIALE: konwerter JSON oparty o Jackson 3 nazywa się
    // JacksonJsonHttpMessageConverter. W tutorialach zobaczysz MappingJackson2HttpMessageConverter (Jackson 2) —
    // w nowych projektach to nie jest domyślny konwerter. Więcej: s04_web_rest/Rest04Json.
    //
    // Rekord HelloController.Greeting(String message, int version) zamienia się na {"message":"…","version":1}
    // — nazwy pól JSON biorą się z nazw składowych rekordu. Nie piszesz żadnego kodu zamiany obiektu na tekst.
    //
    // PUŁAPKA: Accept: application/json nie zmienia tego, CO zwraca metoda. Test stringWithJsonAcceptIsNotJson
    //   pokazuje: metoda zwracająca String, wywołana z Accept: application/json, odpowiada nagłówkiem
    //   Content-Type: application/json, a w ciele jest GOŁY tekst bez cudzysłowów — czyli niepoprawny JSON.
    //   Dlaczego groźne? Klient uwierzy w nagłówek i spróbuje sparsować tekst jako JSON. Jeśli API ma oddawać JSON,
    //   zwracaj obiekt (rekord, mapę, listę), a nie ręcznie zbudowany napis.
    //
    // PYTANIE REKRUTACYJNE: czym jest DispatcherServlet i co robi z przychodzącym zapytaniem?

    // =================================================================================================
    // 4. produces I consumes — NEGOCJACJA TREŚCI (ReportController)
    // =================================================================================================
    //
    // Nagłówki HTTP mówią, w jakim formacie klient i serwer się porozumiewają:
    //   • Accept (zapytanie)        — „potrafię odczytać taki format odpowiedzi” → porównywany z produces metody
    //   • Content-Type (zapytanie)  — „moje ciało jest w takim formacie”          → porównywany z consumes metody
    // Mapowanie z produces/consumes pasuje tylko wtedy, gdy nagłówek zgadza się z deklaracją metody:
    //
    //   @GetMapping(produces = "text/csv")                       → Accept: text/csv     → CSV
    //   @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE) → Accept: application/json → JSON
    //   @PostMapping(path = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    //
    // Dwie metody GET pod /rest01/report to NIE jest konflikt — różnią się produces, więc Accept wybiera właściwą
    // (test differentProducesIsNotAmbiguous: dwa wpisy z tą samą ścieżką w HandlerMapping).
    //
    // Test sekcji 4 sprawdza kody (uruchomieniem):
    //   • Accept: text/csv → 200 i CSV; Accept: application/json → 200 i JSON;
    //   • Accept: application/xml (żadna metoda nie umie XML) → 406 Not Acceptable („nie mogę odpowiedzieć w tym
    //     formacie”); odpowiedź w nagłówku Accept wylicza formaty, które serwer UMIE oddać;
    //   • POST /import z Content-Type: application/json → 200;
    //   • POST /import z Content-Type: text/plain → 415 Unsupported Media Type („nie przyjmuję takiego formatu”);
    //   • POST /import BEZ Content-Type → też 415.
    //   Zapamiętaj: 406 dotyczy ODPOWIEDZI (nagłówek Accept), 415 dotyczy CIAŁA ZAPYTANIA (nagłówek Content-Type).
    //   • Brak Accept w ogóle (curl bez nagłówka) → Spring wybiera pierwszą pasującą metodę; tu odpowiedź to JSON.
    //     Nie polegaj na tym — przy kilku formatach pisz Accept w zapytaniu.
    //
    // PUŁAPKA: pomylenie 406 z 415. Dlaczego łatwo? Oba kody dotyczą „formatu”. Pytanie pomocnicze:
    //   „Czy serwer nie zrozumiał tego, CO ZOSTAŁO WYSŁANE?” → 415. „Czy serwer nie umie odpowiedzieć tym, CZEGO KLIENT
    //   OCZEKUJE?” → 406.
    //
    // DOBRA PRAKTYKA: consumes dla metod przyjmujących ciało (POST/PUT/PATCH) ustawiaj świadomie, a produces tylko
    //   tam, gdzie obsługujesz kilka formatów. Dlaczego? Zbędne ograniczenia zamieniają drobną różnicę w nagłówku
    //   klienta w twardy błąd; ale tam, gdzie format ma znaczenie (CSV vs JSON), deklaracja robi za dokumentację.

    // =================================================================================================
    // 5. PUŁAPKI DOPASOWANIA — KOŃCOWY UKOŚNIK I DWIE METODY Z TYM SAMYM MAPOWANIEM
    // =================================================================================================
    //
    // a) Końcowy ukośnik (trailing slash). Od Spring Framework 6 mapowanie /rest01/hello NIE pasuje do
    //    /rest01/hello/ (z ukośnikiem na końcu). Sprawdzone w Framework 7: test trailingSlashDoesNotMatch — odpowiedź
    //    404. Podobnie /rest01/hello.json (rozszerzenie w ścieżce) już nie oznacza „daj JSON” → 404.
    //    W starych tutorialach (Spring 5) /hello/ działało — jeśli stary kod „sam działał”, a nowy daje 404, zacznij
    //    od sprawdzenia ukośnika. Dlaczego zmieniono? Jeden adres = jeden zasób; automatyczne dopasowania były
    //    źródłem niejasności (dwa adresy dla jednej strony — problem dla pamięci podręcznej i zabezpieczeń).
    //
    // b) Dwie metody z tym samym mapowaniem (ta sama ścieżka + metoda HTTP + produces/consumes). Aplikacja
    //    w ogóle się nie uruchomi: kontekst kończy się błędem „Ambiguous mapping. Cannot map … method” (ambiguous =
    //    niejednoznaczny). Test Rest01AmbiguousMappingTest buduje mały kontekst z takim kontrolerem i sprawdza błąd
    //    startu (WebApplicationContextRunner). Zaleta: błąd wychodzi przy STARCIE, nie w produkcji pod ruchem.
    //
    // PUŁAPKA: kopiowanie metody kontrolera i zapomnienie o zmianie ścieżki. Dlaczego? Błąd „Ambiguous mapping”
    //   w logu wymienia obie metody — czytaj go od końca do początku: nazwy klas i metod są w komunikacie.
    //
    // DOBRA PRAKTYKA: po dodaniu nowego mapowania uruchom test kontrolera od razu. Dlaczego? @WebMvcTest ładuje
    //   mapowania tak samo jak aplikacja, więc konflikt wychodzi w kilka sekund, a nie dopiero po wdrożeniu.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @RestController = @Controller + @ResponseBody: zwrócona wartość to ciało odpowiedzi.
     *   • Zwykły @Controller zwraca NAZWĘ WIDOKU (chyba że dasz @ResponseBody).
     *   • @RequestMapping na klasie = prefiks; @GetMapping/@PostMapping/@PutMapping/@PatchMapping/@DeleteMapping
     *     = skróty dla metod HTTP.
     *   • Ścieżka jest, metoda HTTP nie → 405 (nagłówek Allow); ścieżki nie ma → 404.
     *   • Droga zapytania: Tomcat → DispatcherServlet → HandlerMapping → metoda → HttpMessageConverter → odpowiedź.
     *   • String → text/plain; rekord/obiekt → application/json (Jackson 3: JacksonJsonHttpMessageConverter).
     *   • produces ↔ nagłówek Accept (zły → 406); consumes ↔ nagłówek Content-Type (zły albo brak → 415).
     *   • /hello/ nie pasuje do /hello (404). Dwie takie same mapowania → błąd „Ambiguous mapping” przy starcie.
     *   • Test: @WebMvcTest + MockMvc (wycinek warstwy web, bez Tomcata).
     *
     * PYTANIA KONTROLNE:
     *   1. Z jakich dwóch adnotacji składa się @RestController i co robi każda z nich?
     *   2. Który kod odpowiedzi dostaniesz, gdy wyślesz POST na adres, który ma mapowanie tylko dla GET? A gdy wyślesz
     *      GET na adres, którego nikt nie zmapował? Co mówi nagłówek Allow?
     *   3. Co się stanie? Metoda ma {@code @GetMapping("/hello")}, a klient wywołuje {@code GET /hello/}.
     *   4. Co się stanie? Klient wysyła {@code POST /rest01/report/import} z nagłówkiem
     *      {@code Content-Type: text/plain}, a metoda ma {@code consumes = "application/json"}.
     *   5. Co się stanie? Klient wysyła {@code GET /rest01/report} z nagłówkiem {@code Accept: application/xml}.
     *   6. ZNAJDŹ BŁĄD: kontroler jest oznaczony {@code @Controller}, metoda {@code @GetMapping("/price")} zwraca
     *      {@code new Price(100)}, a klient dostaje pustą odpowiedź zamiast JSON. Co poprawić?
     *   7. ZNAJDŹ BŁĄD: w klasie są dwie metody {@code @GetMapping("/products")} (jedna do listy, druga do wyszukiwania).
     *      Co zobaczysz i jak to naprawić?
     *   8. Jaka jest różnica między 406 a 415?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielet: ExerciseController, testy: Rest01ControllersExercisesTest (uruchom: @Tag("cwiczenie"))
    // =================================================================================================
    //
    // Szkielet to klasa ExerciseController (ten sam pakiet). Wzorcowe rozwiązania: Rest01SolutionsController
    // w pakiecie s04_web_rest.solutions.rest01_controllers.
    //
    // ĆWICZENIE 1 (łatwe): metoda greeting() ma zwracać napis „Witaj w sklepie!” (odpowiedź text/plain).
    //   Podpowiedź: wystarczy return z napisem — mapowanie GET jest już w szkielecie.
    //
    // ĆWICZENIE 2 (łatwe): metoda shop() ma zwracać {@code new ShopInfo("Sklep DP21", 6)}; test sprawdza JSON.
    //   Podpowiedź: nie zamieniaj rekordu na tekst ręcznie — Spring zrobi z niego JSON sam (sekcja 3).
    //
    // ĆWICZENIE 3 (średnie): pod ścieżką /rest01/cw/cart ma działać GET → „koszyk” oraz DELETE → „koszyk opróżniony”.
    //   Podpowiedź: dodaj DRUGĄ metodę z @DeleteMapping("/cart") — ta sama ścieżka, inna metoda HTTP (sekcja 2).
    //
    // ĆWICZENIE 4 (trudniejsze): GET /rest01/cw/report ma odpowiadać „id;name” tylko na Accept: text/csv; na
    //   Accept: application/json test oczekuje 406.
    //   Podpowiedź: atrybut produces w @GetMapping (sekcja 4); nie sprawdzaj nagłówka ifem — Spring zrobi to za Ciebie.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. @Controller (klasa jest beanem obsługującym zapytania) + @ResponseBody (zwrócona wartość jest ciałem
     *      odpowiedzi, a nie nazwą widoku). Sprawdzone testem (sekcja 1).
     *   2. POST na adres tylko-GET → 405 Method Not Allowed; GET na niezmapowany adres → 404 Not Found.
     *      Nagłówek Allow w odpowiedzi 405 wylicza metody dozwolone dla tej ścieżki (tu: Allow: GET).
     *      Sprawdzone testem (sekcja 2).
     *   3. 404 — od Spring Framework 6 końcowy ukośnik nie jest dopasowywany; sprawdzone w Framework 7 (sekcja 5).
     *   4. 415 Unsupported Media Type — metoda przyjmuje tylko JSON, a ciało zapytania zadeklarowano jako tekst.
     *      Sprawdzone testem (sekcja 4).
     *   5. 406 Not Acceptable — żadna z metod pod tą ścieżką nie umie oddać XML (umieją CSV i JSON). Sprawdzone testem.
     *   6. @Controller traktuje wynik jako nazwę widoku. Zamień na @RestController (albo dodaj @ResponseBody) —
     *      wtedy rekord Price zostanie zamieniony na JSON.
     *   7. Aplikacja nie wystartuje: „Ambiguous mapping. Cannot map … method” — obie metody mają identyczne
     *      mapowanie (GET /products). Napraw: zmień ścieżkę jednej (np. /products/search) albo rozróżnij je
     *      w inny sposób (w Rest02RequestData: parametry zapytania w jednej metodzie). Sprawdzone testem (sekcja 5).
     *   8. 406 dotyczy ODPOWIEDZI: klient (nagłówek Accept) chce formatu, którego serwer nie umie oddać.
     *      415 dotyczy CIAŁA ZAPYTANIA: klient (nagłówek Content-Type) wysłał format, którego metoda nie przyjmuje.
     */
    // </editor-fold>
}
