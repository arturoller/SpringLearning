package s04_web_rest.rest01_controllers;

import java.util.Map;
import java.util.Set;

/**
 * <pre>
 * TEMAT: Kontrolery REST — @RestController, @RequestMapping i adnotacje metod HTTP; jak DispatcherServlet wybiera
 *        metodę; 404 i 405
 *        (controller = kontroler; mapping = mapowanie, przypisanie adresu do metody; handler = metoda obsługująca;
 *        dispatcher = rozdzielacz; servlet = serwlet, klasa obsługująca zapytania HTTP w serwerze Javy)
 *
 * W SKRÓCIE:
 *   Kontroler REST to klasa, której metody obsługują zapytania HTTP. Adnotacje mówią Springowi, KTÓRA metoda
 *   obsługuje KTÓRY adres i KTÓRĄ metodę HTTP: @GetMapping (odczyt), @PostMapping (utworzenie), @PutMapping
 *   (zastąpienie), @PatchMapping (częściowa zmiana), @DeleteMapping (usunięcie). @RequestMapping na klasie daje
 *   wspólny początek adresu. @RestController sprawia, że wynik metody idzie do ciała odpowiedzi jako JSON.
 *   Gdy adresu nie zna żadna metoda — 404 Not Found; gdy adres jest znany, ale nie z tą metodą HTTP —
 *   405 Method Not Allowed (z nagłówkiem Allow, który wylicza dozwolone metody).
 *
 * ANALOGIA: recepcja w biurowcu.
 *   DispatcherServlet to recepcjonistka. Każdy gość (zapytanie) mówi, do kogo idzie (adres) i w jakiej sprawie
 *   (metoda HTTP). Recepcjonistka ma spis: „pokój 12 (/api/products) — odbiór paczek (GET) i nadawanie (POST)”.
 *   Nie ma takiego pokoju → „nie ma tu nikogo takiego” (404). Pokój jest, ale sprawy takiej tam nie załatwisz →
 *   „tu tylko odbiór i nadawanie” (405 + lista dozwolonych spraw w nagłówku Allow).
 *
 * JAK TO DZIAŁA:
 *   GET /api/products/7
 *     → Tomcat (serwer) przekazuje zapytanie do DispatcherServlet (jeden serwlet na całą aplikację)
 *     → RequestMappingHandlerMapping (mapowanie metod) szuka metody: adres /api/products + /{id}, metoda GET
 *       ✔ znalazł ProductController.one(long id)
 *     → RequestMappingHandlerAdapter (adapter) przygotowuje argumenty: "7" z adresu → long 7 (Rest02)
 *     → wywołuje one(7) → wynik Reply
 *     → HttpMessageConverter (konwerter komunikatów) — dla JSON-a: Jackson — zamienia Reply na JSON w ciele odpowiedzi
 *   Spis wszystkich mapowań powstaje RAZ, przy starcie aplikacji (skanowanie klas z @Controller/@RestController).
 *
 * SŁÓWKA:
 *   request = zapytanie; response = odpowiedź; body = ciało; handler = metoda obsługująca; mapping = mapowanie;
 *   path = ścieżka; view = widok (szablon strony HTML); forward = przekazanie dalej (wewnątrz serwera);
 *   method not allowed = metoda niedozwolona; not found = nie znaleziono; allow = zezwalaj (nagłówek z listą metod);
 *   message converter = konwerter komunikatów (Java ↔ JSON); slice = wycinek (aplikacji, w testach).
 *
 * ZOBACZ TEŻ: t34_toward_spring/Spring03RestConcepts (pojęcia REST bez Springa), t28_networking_http/Http03LocalServer
 *             (serwer HTTP „ręcznie” — zobacz, ile Spring robi za Ciebie), s00_start/Start03FirstApplication
 *             (pierwszy kontroler), s04_web_rest/Rest02RequestData (dane z zapytania), s04_web_rest/Rest03Responses.
 * </pre>
 */
public final class Rest01Controllers {

    private Rest01Controllers() {
    }

    // =================================================================================================
    // 1. @RestController — wynik metody = ciało odpowiedzi (Rest01ControllersTest, sekcja 1)
    // =================================================================================================
    //
    // Klasa ProductController ma dwie adnotacje:
    //   @RestController                      ← „to kontroler, a wyniki metod zamieniaj na JSON w ciele odpowiedzi”
    //   @RequestMapping("/api/products")     ← wspólny początek adresu dla wszystkich metod klasy
    // Metoda all() ma @GetMapping BEZ argumentu — obsługuje sam adres z klasy: GET /api/products.
    // Zwraca List<Reply>; Jackson zamienia ją na [{"handler":"ProductController.all","info":"lista produktów"}].
    // Content-Type odpowiedzi: application/json — Spring sam ustawił typ, bo wybrał konwerter JSON.
    //
    // DOBRA PRAKTYKA: wszystkie adresy API zaczynaj od /api/... i grupuj po zasobie (/api/products, /api/orders) —
    //   jeden zasób = jeden kontroler z @RequestMapping na klasie. Dlaczego? Adres zasobu jest w jednym miejscu
    //   (zmiana w jednej linii), a prefiks /api łatwo odróżnić od stron HTML czy plików statycznych
    //   (i np. objąć wspólną konfiguracją CORS — Rest06Cors).

    // =================================================================================================
    // 2. @Controller + @ResponseBody — to samo „na piechotę” (sekcja 2)
    // =================================================================================================
    //
    // @RestController to skrót: @Controller + @ResponseBody na każdej metodzie. Zwykły @Controller pochodzi z czasów,
    // gdy kontroler zwracał nazwę WIDOKU (strony HTML generowanej z szablonu, np. Thymeleaf).
    //   ShopInfoController.info()            ma @ResponseBody → JSON w ciele, jak w @RestController
    //   ShopInfoController.forgotten...()    NIE ma @ResponseBody → "shop-hours" to NAZWA WIDOKU, nie dane!
    //
    // PUŁAPKA: zapomniany @ResponseBody w zwykłym @Controller. Dlaczego to boli? Spring nie zgłasza błędu przy starcie:
    //   String zwrócony z metody uznaje za nazwę widoku i przekazuje (forward) zapytanie pod adres „shop-hours”.
    //   W MockMvc widać status 200, PUSTE ciało i forwardedUrl = shop-hours. Na prawdziwym serwerze taki adres nie
    //   istnieje, więc klient dostaje 404 Not Found z JSON-em błędu Boota — choć metoda kontrolera wykonała się
    //   poprawnie! Sprawdzone testem (Rest01ControllersTest i Rest01ControllersServerTest).
    //   PYTANIE REKRUTACYJNE: „Czym różni się @Controller od @RestController?”

    // =================================================================================================
    // 3. METODY HTTP — @GetMapping, @PostMapping, @PutMapping, @PatchMapping, @DeleteMapping (sekcja 3)
    // =================================================================================================
    //
    //   adnotacja        metoda HTTP   znaczenie w REST                      przykład
    //   @GetMapping      GET           odczyt (nic nie zmienia)              GET    /api/products/7
    //   @PostMapping     POST          utworzenie nowego zasobu              POST   /api/products
    //   @PutMapping      PUT           zastąpienie całego zasobu             PUT    /api/products/7
    //   @PatchMapping    PATCH         zmiana części zasobu                  PATCH  /api/products/7
    //   @DeleteMapping   DELETE        usunięcie                             DELETE /api/products/7
    // Każda z nich to skrót: @GetMapping("/x") = @RequestMapping(path = "/x", method = RequestMethod.GET).
    // Ten sam adres /api/products/{id} obsługują CZTERY różne metody Javy — rozróżnia je metoda HTTP.
    //
    // Gdy kilka wzorców pasuje do adresu, wygrywa NAJBARDZIEJ KONKRETNY: GET /api/products/bestsellers trafia do
    // bestsellers(), a nie do one({id}) — stała ścieżka jest „konkretniejsza” niż zmienna. Kolejność metod w klasie
    // nie ma znaczenia.
    //
    // PUŁAPKA: dwie metody z IDENTYCZNYM mapowaniem (ten sam adres i metoda HTTP) — aplikacja NIE wystartuje
    //   (IllegalStateException: Ambiguous mapping — niejednoznaczne mapowanie). Dlaczego to dobrze? Spring nie zgaduje,
    //   którą wybrać — błąd wychodzi przy starcie, a nie u klienta. Dlatego każda lekcja tego działu ma własną klasę
    //   startową: Rest01Application i Rest05Application mają kontrolery pod tym samym adresem /api/products, ale żadna
    //   aplikacja nie widzi kontrolerów drugiej.
    //
    // DOBRA PRAKTYKA: w adresie rzeczownik (zasób), czynność wyraża metoda HTTP: POST /api/products, a nie
    //   GET /api/createProduct. Dlaczego? Klient (i każdy programista) od razu wie, czego się spodziewać: GET niczego
    //   nie zmienia, więc przeglądarka czy serwer pośredniczący mogą go bezpiecznie powtórzyć lub zapamiętać.

    // =================================================================================================
    // 4. 405 METHOD NOT ALLOWED — adres znany, metoda HTTP nie (sekcja 4)
    // =================================================================================================
    //
    // POST /api/shop/info → istnieje tylko GET dla tego adresu → 405 i nagłówek Allow: GET.
    // POST /api/products/5 → dla /{id} są GET, PUT, PATCH, DELETE → 405 i Allow z tymi czterema metodami.
    // Robi to DefaultHandlerExceptionResolver (domyślna obsługa wyjątków MVC): HttpRequestMethodNotSupportedException
    // → 405. Twój kod nie musi nic robić.
    // Za darmo dostajesz też HEAD (jak GET, ale bez ciała) dla każdego GET-a i OPTIONS (pytanie „co tu wolno?”) —
    // OPTIONS /api/shop/info → 200 i Allow: GET,HEAD,OPTIONS. Sprawdzone testem.
    //
    // PUŁAPKA: kolejność metod w nagłówku Allow nie jest ustalona. Dlaczego to ważne? Test (albo klient), który
    //   porównuje cały nagłówek jako tekst, może raz przejść, a raz nie — sprawdzaj, czy ZAWIERA metody.

    // =================================================================================================
    // 5. 404 NOT FOUND — nikt nie obsługuje adresu (sekcja 5)
    // =================================================================================================
    //
    // GET /api/nothing → żadne mapowanie nie pasuje. W Spring Framework 6.1+ (więc i w 7) zapytanie trafia wtedy do
    // obsługi plików statycznych, która rzuca NoResourceFoundException („brak zasobu statycznego”) → 404.
    // Na prawdziwym serwerze Boot dokleja JSON błędu: {"timestamp":...,"status":404,"error":"Not Found","path":...}.
    // MockMvc tego JSON-a nie pokazuje — strona błędu to funkcja serwera (kontener serwletów), a MockMvc serwera nie ma.
    // Szczegóły i własne odpowiedzi błędów: dział s06_error_handling.
    //
    // PUŁAPKA: literówka w adresie (np. /api/product zamiast /api/products) też daje 404 — nie 500 i nie „pusty wynik”.
    //   Dlaczego warto to wiedzieć? Przy 404 najpierw porównaj adres z mapowaniem (i @RequestMapping klasy!), zanim
    //   zaczniesz szukać błędu w logice.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @RestController = @Controller + @ResponseBody — wynik metody → JSON w ciele odpowiedzi.
     *   • @Controller bez @ResponseBody → String to nazwa WIDOKU (forward), nie dane.
     *   • @RequestMapping("/api/x") na klasie = prefiks; @GetMapping/@PostMapping/@PutMapping/@PatchMapping/
     *     @DeleteMapping na metodach (= @RequestMapping z method = ...).
     *   • GET odczyt, POST tworzy, PUT zastępuje całość, PATCH zmienia część, DELETE usuwa.
     *   • Wybór metody: DispatcherServlet → HandlerMapping (adres + metoda HTTP) → HandlerAdapter (argumenty)
     *     → wywołanie → HttpMessageConverter (JSON). Mapowania zbierane raz, przy starcie.
     *   • Konkretna ścieżka wygrywa ze zmienną: /bestsellers przed /{id}.
     *   • Brak adresu → 404; adres jest, metoda HTTP nie → 405 + nagłówek Allow. HEAD i OPTIONS — za darmo.
     *   • Dwa identyczne mapowania → aplikacja nie startuje (Ambiguous mapping).
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się @Controller od @RestController? (PYTANIE REKRUTACYJNE)
     *   2. Co się stanie? Wyślesz DELETE /api/shop/info — jaki status i jaki nagłówek dostaniesz?
     *   3. Co się stanie? Wyślesz GET /api/products/bestsellers — która metoda go obsłuży: one(...) czy bestsellers()?
     *   4. ZNAJDŹ BŁĄD: zwykły @Controller, metoda @GetMapping("/api/hello") public String hello() { return "Cześć"; }.
     *      Klient na prawdziwym serwerze dostaje 404, choć metoda się wykonała. Dlaczego i jak to naprawić?
     *   5. ZNAJDŹ BŁĄD: dwie metody w różnych klasach mają @GetMapping("/api/products") i obie są skanowane przez tę
     *      samą aplikację. Co się stanie przy starcie?
     *   6. Kto zamienia rekord Reply na JSON i skąd wie, że ma to zrobić?
     *   7. Jaka jest różnica między 404 a 405 z punktu widzenia klienta?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — ćwiczenia 1–2: szkielet CustomerController; ćwiczenie 3 poniżej. Testy: Rest01ControllersExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 3 (trudniejsze): zasymuluj recepcjonistkę z ANALOGII, czyli decyzję DispatcherServlet o statusie.
     * routes = spis mapowań: ścieżka → zbiór metod HTTP (np. "/api/products" → {"GET", "POST"}).
     * Zwróć: 404, gdy ścieżki nie ma w spisie; 200, gdy metoda jest w zbiorze; 200 także dla HEAD, gdy w zbiorze jest
     * GET, i dla OPTIONS przy każdej znanej ścieżce (jak w Springu — sekcja 4); w pozostałych przypadkach 405.
     * Metodę porównuj bez względu na wielkość liter ("get" = "GET").
     * Przykład: (spis, "DELETE", "/api/products") → 405; (spis, "head", "/api/products") → 200; (spis, "GET", "/x") → 404.
     * Podpowiedź: method.toUpperCase(), routes.get(path) i kilka warunków if (t02_controlflow/Control01IfElse).
     */
    static int exercise3(Map<String, Set<String>> routes, String method, String path) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. @RestController = @Controller + @ResponseBody na wszystkich metodach: wynik metody to DANE w ciele
     *      odpowiedzi (JSON). W zwykłym @Controller wynik (String) to nazwa widoku HTML, chyba że metoda ma
     *      @ResponseBody. Do API używaj @RestController.
     *   2. 405 Method Not Allowed i nagłówek Allow: GET — adres jest znany, ale tylko z metodą GET.
     *      Sprawdzone testem (Rest01ControllersTest, sekcja 4 — ten sam mechanizm dla POST).
     *   3. bestsellers() — stała ścieżka jest bardziej konkretna niż wzorzec /{id}, więc wygrywa niezależnie od
     *      kolejności metod. Sprawdzone testem (sekcja 3).
     *   4. Brak @ResponseBody: "Cześć" to nazwa widoku, Spring robi forward pod adres „Cześć”, którego nie ma → 404.
     *      Naprawa: @RestController na klasie albo @ResponseBody na metodzie. Sprawdzone testem
     *      (Rest01ControllersServerTest — ten sam przypadek dla /api/shop/forgotten).
     *   5. Aplikacja nie wystartuje: IllegalStateException „Ambiguous mapping” — Spring nie wie, którą metodę wybrać.
     *      Sprawdzone uruchomieniem (druga metoda z identycznym mapowaniem dodana na próbę).
     *   6. HttpMessageConverter z Jacksonem (konwerter JSON). Wie, bo @RestController (@ResponseBody) każe zapisać
     *      wynik w ciele, a klient akceptuje JSON (domyślnie każdy typ) — Spring wybiera pierwszy pasujący konwerter.
     *   7. 404: „pod tym adresem nic nie ma” — popraw adres. 405: „adres dobry, ale ta metoda HTTP tu nie działa” —
     *      zmień metodę na jedną z nagłówka Allow.
     */
    // </editor-fold>
}
