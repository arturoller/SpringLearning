package s06_error_handling.err01_default_errors;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <pre>
 * TEMAT: Domyślna obsługa błędów w Spring Boocie — co się dzieje, gdy sami niczego nie obsłużymy
 *        (error = błąd; default = domyślny; whitelabel = „biała etykieta”, czyli strona bez żadnej szaty graficznej)
 *
 * W SKRÓCIE:
 *   Gdy w kontrolerze poleci wyjątek albo klient poprosi o nieistniejący adres, Spring Boot NIE zostawia go bez
 *   odpowiedzi. Zamienia błąd na odpowiedź HTTP: dla programu (curl, .http, frontend) — JSON z polami timestamp, status,
 *   error i path; dla przeglądarki — stronę „Whitelabel Error Page”. Za to odpowiada BasicErrorController (podstawowy
 *   kontroler błędów) pod adresem /error. Domyślnie Boot ukrywa szczegóły (komunikat wyjątku, stos wywołań), bo mogłyby
 *   zdradzić napastnikowi, jak zbudowana jest aplikacja. Ta lekcja pokazuje, jak to wygląda i jak to (ostrożnie) zmienić.
 *
 * ANALOGIA: recepcja w biurowcu.
 *   Pracownik (kontroler) ma kłopot i nie wie, co zrobić. Recepcja (Boot) nie wpuszcza klienta na zaplecze, tylko wręcza
 *   mu standardową kartkę: „Błąd 500, godzina, adres, którego dotyczył”. Nie podaje nazwiska pracownika ani opisu awarii
 *   instalacji — to wiedza wewnętrzna. Dopiero gdy sam poprosisz (i recepcja ci na to pozwoli), dostaniesz więcej szczegółów.
 *
 * JAK TO DZIAŁA:
 *   1. Metoda kontrolera rzuca wyjątek (np. IllegalStateException) albo adres nie pasuje do żadnego kontrolera.
 *   2. DispatcherServlet próbuje go obsłużyć swoimi rozwiązywaczami wyjątków (HandlerExceptionResolver). Znane wyjątki
 *      Springa (zły typ w adresie, niepoprawny JSON, brak adresu) kończą jako kod 400/404/405 przez response.sendError(...).
 *      Nieznany wyjątek (nasz własny, bez adnotacji) leci dalej i wychodzi z DispatcherServlet — Tomcat ustawia status 500.
 *   3. Tomcat widzi status błędu i robi wewnętrzne przekierowanie (forward) na adres /error
 *      (ustawienie: spring.web.error.path, domyślnie /error).
 *   4. Pod /error odpowiada BasicErrorController: dla zapytania z Accept: text/html — strona Whitelabel,
 *      dla pozostałych — JSON zbudowany przez DefaultErrorAttributes (atrybuty błędu).
 *   5. Domyślne ciało JSON:  {"timestamp":"...","status":404,"error":"Not Found","path":"/api/nic"}
 *      Pola message, trace, exception i errors są DOMYŚLNIE UKRYTE.
 *
 *   Ustawienia (Boot 4.1.1 — prefiks spring.web.error, wartości domyślne sprawdzone w metadanych Boota):
 *      spring.web.error.include-message         never   (never | always | on-param)
 *      spring.web.error.include-binding-errors  never   (tablica errors z błędami walidacji)
 *      spring.web.error.include-stacktrace      never   (pole trace — stos wywołań)
 *      spring.web.error.include-exception       false   (pole exception — nazwa klasy wyjątku)
 *      spring.web.error.include-path            always  (pole path)
 *      spring.web.error.whitelabel.enabled      true    (strona Whitelabel dla przeglądarki)
 *      spring.web.error.path                    /error
 *
 * SŁÓWKA:
 *   error page = strona błędu; stack trace = stos wywołań (lista metod, przez które przeszedł wyjątek); timestamp = znacznik
 *   czasu; binding errors = błędy wiązania (dane z zapytania, które nie pasują do rekordu/walidacji); forward = wewnętrzne
 *   przekazanie zapytania pod inny adres (klient o tym nie wie); on-param = „na parametr” (tylko gdy zapytanie go ma);
 *   internal server error = wewnętrzny błąd serwera (500); not found = nie znaleziono (404); method not allowed = metoda
 *   niedozwolona (405).
 *
 * ZOBACZ TEŻ: s05_validation/Val02ValidInControllers (skąd bierze się 400 przy złych danych),
 *             s06_error_handling/Err02ExceptionHandler (własna obsługa wyjątku zamiast domyślnej),
 *             s04_web_rest/Rest03Responses (kody statusu), t10_exceptions/Exceptions01Basics (wyjątki w Javie),
 *             s00_start/Start05HttpFiles (RANDOM_PORT i pliki .http).
 * </pre>
 */
public final class Err01DefaultErrors {

    private Err01DefaultErrors() {
    }

    // =================================================================================================
    // 1. DOMYŚLNA ODPOWIEDŹ — JSON z czterema polami (test: Err01DefaultErrorsTest #1, #2, #3, #4)
    // =================================================================================================
    //
    // Klasa ProductController (w tym pakiecie) celowo NIE obsługuje błędów. Co zwraca Boot (wynik prawdziwego uruchomienia):
    //
    //   GET /api/nie-ma-takiego      → 404  {"timestamp":"2026-10-19T07:49:21.359Z","status":404,"error":"Not Found",
    //                                        "path":"/api/nie-ma-takiego"}
    //   GET /api/products/KSI-999    → 500  {"timestamp":"...","status":500,"error":"Internal Server Error",
    //                                        "path":"/api/products/KSI-999"}      (kontroler rzucił IllegalStateException)
    //   GET /api/orders/abc          → 400  {... "error":"Bad Request" ...}        (abc nie jest liczbą long)
    //   POST /api/products z błędnymi danymi (@Valid) → 400, dokładnie ten sam kształt, BEZ listy błędów pól
    //   DELETE /api/products/ELE-001 → 405  nagłówek Allow: GET, ciało jak wyżej
    //
    // Zauważ: w ciele NIE MA tekstu wyjątku ani informacji, które pole jest złe. Klient dostaje tylko „że się nie udało”.
    // To bezpieczne, ale mało pomocne dla uczciwego klienta — dlatego w kolejnych lekcjach budujemy własne, bogatsze odpowiedzi.
    //
    // Co widać w LOGU serwera (prawdziwe uruchomienie):
    //   500 → poziom ERROR, logger Tomcata:  ERROR ... o.a.c.c.C.[.[.[/].[dispatcherServlet] : Servlet.service() for servlet
    //         [dispatcherServlet] in context with path [] threw exception [Request processing failed:
    //         java.lang.IllegalStateException: Brak produktu KSI-999 ...] with root cause   + cały stos wywołań
    //   400 → tylko WARN:  WARN ... .w.s.m.s.DefaultHandlerExceptionResolver : Resolved [org.springframework.web.bind.
    //         MethodArgumentNotValidException: Validation failed ...]   (bez stosu)
    //   404 → w logu na poziomie WARN nic nie ma.
    // Czyli: prawdziwa awaria (500) zostawia ślad ze stosem w logu — to tam szukasz przyczyny, nie w odpowiedzi dla klienta.
    //
    // DOBRA PRAKTYKA: szczegóły błędu zapisuj w logu serwera, a klientowi oddawaj krótką, bezpieczną odpowiedź.
    //   Dlaczego? Log czytasz Ty (zespół), a odpowiedź może przeczytać każdy, także ktoś, kto szuka luki w aplikacji.

    // =================================================================================================
    // 2. STRONA WHITELABEL DLA PRZEGLĄDARKI (test: Err01DefaultErrorsTest #5, #6)
    // =================================================================================================
    //
    // Przeglądarka wysyła Accept: text/html, więc BasicErrorController wybiera metodę errorHtml i zwraca (prawdziwa odpowiedź):
    //
    //   <html><body><h1>Whitelabel Error Page</h1><p>This application has no explicit mapping for /error, so you are seeing
    //   this as a fallback.</p><div id='created'>...</div><div>There was an unexpected error (type=Not Found, status=404).
    //   </div></body></html>
    //
    // Strona jest po angielsku i brzydka — to „fallback” (rozwiązanie awaryjne). Wyłączysz ją ustawieniem
    // spring.web.error.whitelabel.enabled=false (wtedy zostaje błąd z samego kontenera). Klient, który prosi
    // o coś innego niż JSON i HTML (np. Accept: application/xml), dostaje sam kod statusu i PUSTE ciało.
    //
    // PUŁAPKA: testujesz API przeglądarką i widzisz Whitelabel, a w teście (curl, RestClient) — JSON. Dlaczego? To ta sama
    //   aplikacja; o formie odpowiedzi decyduje nagłówek Accept zapytania. Nie szukaj „drugiego” kodu błędu.

    // =================================================================================================
    // 3. USTAWIENIA include-* (test: Err01DefaultErrorsExposedTest, Err01DefaultErrorsOnParamTest)
    // =================================================================================================
    //
    // Po ustawieniu (tylko na potrzeby nauki!)
    //   spring.web.error.include-message=always
    //   spring.web.error.include-stacktrace=always
    //   spring.web.error.include-exception=true
    //   spring.web.error.include-binding-errors=always
    // odpowiedź 500 zyskuje pola exception, trace i message (u nas: "Brak produktu KSI-999 w tabeli PRODUCTS
    // (jdbc:h2:mem:sklep, użytkownik sa)" — wyciekły nazwa tabeli, adres bazy i login), a odpowiedź 400 dla złego
    // POST-a — tablicę errors z elementami typu {"field":"name","code":"NotBlank","rejectedValue":"",...}.
    //
    // Wartość on-param (spring.web.error.include-message=on-param): pole pojawia się tylko, gdy zapytanie ma parametr
    // message — ?message=true, a także ?message (pusty) czy ?message=abc. Wyjątek: ?message=false.
    // Bez parametru pola nie ma. Wygodne na lokalnym środowisku programisty, zbędne na produkcji.
    //
    // PUŁAPKA: stos wywołań (include-stacktrace=always) w odpowiedzi na produkcji. Dlaczego? Zdradza klasy, biblioteki i ich
    //   wersje, układ pakietów, czasem fragmenty zapytań SQL — gotowa ściąga dla napastnika. Do tego message z wyjątku bywa
    //   wprost wpisaną daną wrażliwą (adres bazy, login, ścieżka pliku). Zasada: na produkcji zostaw domyślne never.
    //
    // PUŁAPKA: zmiana w Boot 4 — w starych tutorialach spotkasz server.error.include-message=always (Boot 2 i 3).
    //   W Boot 4 te klucze mają w metadanych poziom deprecation „error” i prefiks zmienił się na spring.web.error.*.
    //   Stary klucz NIE powoduje błędu startu — aplikacja startuje i go po cichu ignoruje (test Err01DefaultErrorsOldNamesTest).
    //   Dlaczego to groźne? Myślisz, że włączyłeś komunikaty, a ich nie ma — albo odwrotnie, myślisz, że coś zablokowałeś.

    // =================================================================================================
    // 4. MOCKMVC NIE PRZECHODZI PRZEZ /error (test: Err01MockMvcTest)
    // =================================================================================================
    //
    // Przekierowanie na /error robi KONTENER SERVLETÓW (Tomcat), a MockMvc kontenera nie uruchamia — woła
    // DispatcherServlet bezpośrednio. Skutki (sprawdzone testem):
    //   • 404 w MockMvc ma status 404, ale PUSTE ciało (nikt nie zbudował JSON-a z /error),
    //   • nieobsłużony wyjątek z kontrolera NIE staje się 500 z JSON-em — mockMvc.perform(...) rzuca go do testu
    //     (jako ServletException z przyczyną IllegalStateException),
    //   • jsonPath("$.status") na takiej odpowiedzi kończy się porażką asercji — pola nie ma.
    //
    // PUŁAPKA: test MockMvc „potwierdza”, że wyjątek jest rzucony, więc wygląda na zielony, a w prawdziwej aplikacji klient
    //   dostaje 500 z domyślnym JSON-em (albo Whitelabel). Dlaczego? MockMvc pomija całą ścieżkę przez Tomcata. Ciało błędu
    //   z /error sprawdzaj testem @SpringBootTest(webEnvironment = RANDOM_PORT) (prawdziwy serwer na losowym porcie).
    //   Gdy własna obsługa (@ExceptionHandler, @RestControllerAdvice — następne lekcje) sama buduje odpowiedź, MockMvc
    //   widzi ją normalnie, bo /error w ogóle nie jest potrzebne.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Nieobsłużony błąd → Tomcat przekierowuje na /error → BasicErrorController → JSON (lub Whitelabel dla Accept: text/html).
     *   • Domyślny JSON: timestamp, status, error, path. Ukryte: message, trace, exception, errors.
     *   • Boot 4.1.1: spring.web.error.include-message / include-binding-errors / include-stacktrace (never | always | on-param),
     *     include-exception (false), whitelabel.enabled (true). Stare server.error.* — po cichu ignorowane.
     *   • 500 loguje ERROR ze stosem (logger Tomcata), 400 loguje WARN z DefaultHandlerExceptionResolver, 404 — nic na WARN.
     *   • MockMvc nie zna /error: 404 bez ciała, wyjątek leci do testu. Ciało błędu testuj przez RANDOM_PORT.
     *   • Na produkcji: nie włączaj message ani stacktrace; szczegóły do logu.
     *
     * PYTANIA KONTROLNE:
     *   1. Którędy idzie nieobsłużony wyjątek z kontrolera, zanim klient dostanie JSON?
     *   2. Co się stanie? Aplikacja na Boot 4.1.1 ma ustawione server.error.include-message=always. Klient wywołuje
     *      GET /api/products/KSI-999. Czy odpowiedź zawiera pole message?
     *   3. Co się stanie? Ustawienie spring.web.error.include-message=on-param; zapytanie GET /api/products/KSI-999?message=true.
     *   4. ZNAJDŹ BŁĄD: test MockMvc mockMvc.perform(get("/api/nie-ma-takiego")).andExpect(jsonPath("$.status").value(404))
     *      ma sprawdzić domyślne ciało błędu Boota. Dlaczego pada i jak go naprawić?
     *   5. Co się stanie? Klient wysyła GET /api/nie-ma-takiego z nagłówkiem Accept: application/xml.
     *   6. Dlaczego include-stacktrace=always na produkcji to luka bezpieczeństwa?
     *   7. Gdzie szukasz przyczyny błędu 500: w odpowiedzi dla klienta czy w logu serwera i dlaczego?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Err01DefaultErrorsExercisesTest (rozwiązania: solutions.err01_default_errors)
    // =================================================================================================

    /** IncludeMode = kiedy dołączać pole do odpowiedzi błędu (odpowiednik wartości never / always / on-param). */
    public enum IncludeMode {
        NEVER, ALWAYS, ON_PARAM
    }

    /**
     * ĆWICZENIE 1 (łatwe): nazwa rodziny kodu statusu: 2xx → "sukces", 3xx → "przekierowanie", 4xx → "błąd klienta",
     * 5xx → "błąd serwera", inne → "nieznany".
     * Podpowiedź: dzielenie całkowite status / 100 i wyrażenie switch (t02_controlflow/Control02Switch).
     */
    static String statusFamily(int status) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zbuduj domyślne ciało błędu jak Boot: mapa z polami W TEJ KOLEJNOŚCI: "timestamp" (tekst
     * z {@code timestamp.toString()}), "status", "error" (powód, np. "Not Found"), "path".
     * Podpowiedź: LinkedHashMap pamięta kolejność wstawiania (t12_collections/Collections05Maps).
     */
    static Map<String, Object> defaultErrorBody(int status, String reason, String path, Instant timestamp) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): jak ćwiczenie 2, ale z polem "message" (między "error" a "path") — zależnie od trybu:
     * NEVER → bez pola; ALWAYS → z polem; ON_PARAM → z polem, gdy messageParam nie jest null i nie jest równy "false"
     * (wielkość liter bez znaczenia), czyli ?message, ?message=true, ?message=abc włączają, a brak parametru i ?message=false nie.
     * Podpowiedź: najpierw zbuduj mapę z ćwiczenia 2, a potem zbuduj nową z wstawionym polem — kolejność ma znaczenie.
     */
    static Map<String, Object> errorBody(int status, String reason, String path, Instant timestamp,
                                         IncludeMode mode, String messageParam, String message) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Kontroler rzuca wyjątek → rozwiązywacze wyjątków DispatcherServlet go nie obsługują (nasz wyjątek bez adnotacji)
     *      → wychodzi z DispatcherServlet, Tomcat ustawia status 500 → wewnętrzne przekierowanie (forward) na /error →
     *      BasicErrorController → JSON z DefaultErrorAttributes. (Znane wyjątki Springa kończą się przez sendError(400/404/405),
     *      ale potem też trafiają na /error.)
     *   2. NIE. W Boot 4 klucz server.error.* jest zastąpiony przez spring.web.error.*; stary klucz jest po cichu ignorowany,
     *      więc odpowiedź ma nadal tylko timestamp, status, error, path. Sprawdzone testem Err01DefaultErrorsOldNamesTest.
     *   3. Odpowiedź ma pole message z tekstem wyjątku. Bez ?message=true (albo z ?message=false) pola nie ma.
     *      Sprawdzone testem Err01DefaultErrorsOnParamTest i ręcznym uruchomieniem.
     *   4. MockMvc nie przechodzi przez /error, więc odpowiedź 404 ma puste ciało i pola status w JSON-ie nie ma — asercja
     *      pada (test Err01MockMvcTest). Naprawa: sprawdzić ciało przez @SpringBootTest(webEnvironment = RANDOM_PORT)
     *      i prawdziwego klienta HTTP, a w MockMvc sprawdzać tylko status().isNotFound().
     *   5. Status 404 i PUSTE ciało — BasicErrorController umie odpowiedzieć tylko JSON-em lub HTML-em, a klient nie
     *      akceptuje żadnego z nich. Sprawdzone testem Err01DefaultErrorsTest #6.
     *   6. Stos wywołań zdradza nazwy klas, biblioteki i ich wersje, układ aplikacji, czasem fragmenty SQL i dane
     *      z komunikatu — to gotowy materiał do przygotowania ataku. Domyślne never jest celowe.
     *   7. W logu serwera: 500 zapisuje się jako ERROR ze stosem wywołań i przyczyną. Odpowiedź dla klienta celowo zawiera tylko
     *      status i krótki opis, bo szczegóły są poufne.
     */
    // </editor-fold>
}
