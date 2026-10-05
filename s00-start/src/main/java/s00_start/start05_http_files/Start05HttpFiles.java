package s00_start.start05_http_files;

/**
 * <pre>
 * TEMAT: Pliki .http — wysyłanie prawdziwych zapytań HTTP do działającej aplikacji z IntelliJ
 *        (HTTP client = klient HTTP; request = zapytanie; response = odpowiedź; header = nagłówek; body = ciało)
 *
 * W SKRÓCIE:
 *   Test sprawdza aplikację automatycznie, ale czasem chcesz sam „pogadać” z działającą aplikacją: wysłać GET, POST
 *   z JSON-em, zobaczyć status i nagłówki. Do tego służy plik .http — zwykły plik tekstowy z zapytaniami, który IntelliJ
 *   wysyła po kliknięciu ▶. Lekcje, które uruchamiają aplikację, mają gotowe pliki w katalogu http/ modułu.
 *   Test tej lekcji robi DOKŁADNIE to samo co plik .http: startuje serwer na prawdziwym porcie i wysyła zapytania siecią.
 *
 * ANALOGIA: formularz zamówienia w sklepie.
 *   Plik .http to wydrukowany formularz: „GET /api/products — poproszę listę”. Wypełniasz raz, wysyłasz tyle razy, ile
 *   chcesz, a odpowiedź sklepu (status 200 i lista) widzisz od razu. Przeglądarka umie wysłać tylko GET z paska adresu;
 *   formularz .http umie też POST, nagłówki i ciało.
 *
 * JAK TO DZIAŁA:
 *   ### 1. Wszystkie produkty                 ← separator i nazwa zapytania
 *   GET {{host}}/api/products                 ← linia zapytania: METODA adres ({{host}} = zmienna z @host = ...)
 *
 *   ### 4. Nowy produkt
 *   POST {{host}}/api/products                ← linia zapytania
 *   Content-Type: application/json            ← nagłówki: Nazwa: wartość
 *                                             ← PUSTA LINIA oddziela nagłówki od ciała
 *   {"name": "Ciastka", "priceGrosze": 1299}  ← ciało (body)
 *   Odpowiedź: HTTP/1.1 201 · Location: /api/products/4 · {"id":4,"name":"Ciastka","priceGrosze":1299}
 *
 * SŁÓWKA:
 *   request line = linia zapytania (metoda + adres); status code = kod statusu (200, 201, 404...); created = utworzono;
 *   not found = nie znaleziono; bad request = złe zapytanie; content type = typ zawartości (np. application/json);
 *   accept = akceptuję (jaki format odpowiedzi chcę); location = położenie (adres nowo utworzonego zasobu);
 *   random port = losowy port; endpoint = punkt końcowy (adres + metoda obsługiwane przez kontroler).
 *
 * ZOBACZ TEŻ: t28_networking_http/Http01UriUrl (budowa adresu), t28_networking_http/Http02HttpClient (klient HTTP w Javie),
 *             t28_networking_http/Http04JsonApi (JSON przez HTTP), s00_start/Start03FirstApplication (uruchamianie),
 *             s00_start/Start04TestingIntro (MockMvc — zapytania bez sieci).
 * </pre>
 */
public final class Start05HttpFiles {

    private Start05HttpFiles() {
    }

    // =================================================================================================
    // 1. PLIK .http — budowa
    // =================================================================================================
    //
    // Otwórz s00-start/http/start05_http_files.http. Każde zapytanie zaczyna się od ### z nazwą, potem:
    //   linia zapytania     GET {{host}}/api/products
    //   nagłówki            Content-Type: application/json   (opcjonalne)
    //   pusta linia + ciało {"name": "Ciastka", ...}          (tylko gdy wysyłasz dane, np. POST)
    // @host = http://localhost:8080 na górze pliku to zmienna — używasz jej jako {{host}}.
    //
    // DOBRA PRAKTYKA: adres serwera trzymaj w zmiennej (@host), a nie w każdym zapytaniu. Dlaczego? Gdy uruchomisz
    //   aplikację na innym porcie, zmieniasz jedną linię zamiast dziesięciu.

    // =================================================================================================
    // 2. GET — odczyt (zapytania 1–3 w pliku)
    // =================================================================================================
    //
    // GET /api/products → 200 OK i tablica JSON. GET /api/products/2 → jeden produkt. GET /api/products/999 → 404.
    // Kod statusu mówi, CO się stało: 2xx — sukces, 4xx — błąd po stronie klienta (złe zapytanie, brak zasobu),
    // 5xx — błąd po stronie serwera (wyjątek w aplikacji).
    //
    // PUŁAPKA: „wysłałem zapytanie i nic” — najczęściej aplikacja nie działa (Connection refused — połączenie
    //   odrzucone). Dlaczego? Plik .http nie uruchamia aplikacji; najpierw ▶ przy main, potem zapytanie.

    // =================================================================================================
    // 3. POST — tworzenie (zapytania 4–5 w pliku)
    // =================================================================================================
    //
    // POST wysyła dane w CIELE. Nagłówek Content-Type: application/json mówi serwerowi, jak je czytać — Spring wybiera
    // na tej podstawie konwerter (Jackson) i zamienia JSON na rekord NewProduct (@RequestBody).
    // Odpowiedź 201 Created ma nagłówek Location z adresem nowego produktu — tak REST mówi „utworzyłem, jest tutaj”.
    //
    // PUŁAPKA: brak pustej linii między nagłówkami a ciałem albo brak Content-Type. Dlaczego to błąd? Bez pustej linii
    //   JSON zostanie potraktowany jak kolejne nagłówki; bez Content-Type serwer nie wie, że to JSON, i odpowie
    //   415 Unsupported Media Type (nieobsługiwany typ zawartości).
    //
    // PUŁAPKA: ten sam POST wysłany drugi raz utworzy DRUGI produkt (id 5, 6...). Dlaczego? POST nie jest idempotentny
    //   (idempotentny = wielokrotne wykonanie daje ten sam efekt co jednokrotne). GET jest — możesz go powtarzać bez obaw.

    // =================================================================================================
    // 4. TEST NA PRAWDZIWYM PORCIE (Start05HttpFilesTest)
    // =================================================================================================
    //
    // @SpringBootTest(webEnvironment = RANDOM_PORT) startuje PRAWDZIWY serwer Tomcat na losowym wolnym porcie
    // (RANDOM_PORT = losowy port), a @LocalServerPort wstrzykuje jego numer. RestClient (klient REST ze Springa) wysyła
    // zapytania siecią — dokładnie jak plik .http, tylko automatycznie i z asercjami.
    //
    // DOBRA PRAKTYKA: w testach nigdy nie używaj na sztywno portu 8080. Dlaczego? Port może być zajęty (np. przez
    //   aplikację uruchomioną z IntelliJ), a testy uruchomione równolegle zderzyłyby się ze sobą. Losowy port tego unika.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Plik .http: ### nazwa, linia „METODA adres”, nagłówki „Nazwa: wartość”, pusta linia, ciało. ▶ wysyła zapytanie.
     *   • Zmienne: @host = http://localhost:8080 i {{host}} w zapytaniach.
     *   • POST z JSON-em: nagłówek Content-Type: application/json + pusta linia + JSON.
     *   • Kody: 200 OK, 201 Created (+ Location), 400 Bad Request, 404 Not Found, 415 Unsupported Media Type, 500 błąd serwera.
     *   • GET jest idempotentny, POST — nie (każde wysłanie tworzy nowy zasób).
     *   • Testy: @SpringBootTest(webEnvironment = RANDOM_PORT) + @LocalServerPort + RestClient — nigdy port 8080.
     *
     * PYTANIA KONTROLNE:
     *   1. Z jakich części składa się zapytanie POST w pliku .http?
     *   2. Co się stanie? Wyślesz zapytanie 4 (POST Ciastka) trzy razy po starcie aplikacji — jakie id dostanie ostatni produkt?
     *   3. Co się stanie? GET /api/products/999 — jaki status i dlaczego nie 500?
     *   4. ZNAJDŹ BŁĄD: zapytanie POST bez linii „Content-Type: application/json”, z JSON-em w ciele — co odpowie serwer?
     *   5. Czym różni się MockMvc (Start04) od RestClient na RANDOM_PORT (ta lekcja)?
     *   6. Dlaczego test nie używa portu 8080?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Start05HttpFilesExercisesTest
    // =================================================================================================

    /** HttpRequestLine = linia zapytania HTTP rozbita na metodę i adres (używana w ćwiczeniu 3). */
    public record HttpRequestLine(String method, String url) {
    }

    /**
     * ĆWICZENIE 1 (łatwe): zbuduj linię zapytania do lokalnej aplikacji: metoda + spacja + "http://localhost:8080" + ścieżka.
     * Przykład: ("GET", "/api/products") → "GET http://localhost:8080/api/products".
     */
    static String exercise1(String method, String path) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): nazwa kodu statusu: 200 → "OK", 201 → "Created", 400 → "Bad Request", 404 → "Not Found",
     * 415 → "Unsupported Media Type", 500 → "Internal Server Error", inny → "Inny kod: " + kod.
     * Podpowiedź: wyrażenie switch (t02_controlflow/Control02Switch).
     */
    static String exercise2(int statusCode) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): rozbierz linię zapytania z pliku .http na HttpRequestLine. Spacje na brzegach pomiń,
     * metodę i adres oddziela jedna lub więcej spacji. Metoda musi być jedną z: GET, POST, PUT, PATCH, DELETE, a adres
     * zaczynać się od "http://" albo "https://" — inaczej IllegalArgumentException.
     * Przykład: "  POST   http://localhost:8080/api/products " → HttpRequestLine("POST", "http://localhost:8080/api/products").
     * Podpowiedź: strip(), split("\\s+") i sprawdzenie długości tablicy (t04_strings/Strings05Regex).
     */
    static HttpRequestLine exercise3(String line) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Linia ### z nazwą; linia zapytania „POST adres”; nagłówki (tu Content-Type: application/json); PUSTA linia;
     *      ciało z JSON-em.
     *   2. id 6. Na starcie są produkty 1–3, więc kolejne POST-y dostają 4, 5 i 6 — POST nie jest idempotentny.
     *      Sprawdzone uruchomieniem (trzy razy POST po starcie aplikacji → id 4, 5, 6).
     *   3. 404 Not Found. Kontroler sam zwraca ResponseEntity.notFound() dla pustego Optional — nie ma wyjątku, więc nie
     *      ma 500. Sprawdzone testem.
     *   4. 415 Unsupported Media Type — bez Content-Type serwer nie wie, że ciało to JSON, i nie wybierze konwertera
     *      Jacksona. Sprawdzone testem (Start05HttpFilesTest wysyła ciało jako text/plain).
     *   5. MockMvc woła DispatcherServlet bezpośrednio, bez serwera i sieci — szybciej. RANDOM_PORT startuje prawdziwy
     *      Tomcat, a RestClient łączy się siecią — sprawdza też to, czego MockMvc nie widzi (prawdziwy serwer, port).
     *   6. Port 8080 może być zajęty (np. przez aplikację uruchomioną w IntelliJ), a równoległe testy kłóciłyby się o niego.
     */
    // </editor-fold>
}
