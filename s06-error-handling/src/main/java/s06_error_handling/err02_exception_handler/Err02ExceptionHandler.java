package s06_error_handling.err02_exception_handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * <pre>
 * TEMAT: Lokalna obsługa błędów w kontrolerze — @ExceptionHandler, @ResponseStatus i ResponseStatusException
 *        (exception handler = obsługa wyjątku; response status = status odpowiedzi; reason = powód)
 *
 * W SKRÓCIE:
 *   Są trzy proste sposoby, by zamienić wyjątek na właściwy kod HTTP zamiast domyślnego 500:
 *   (1) @ResponseStatus na klasie wyjątku — „ten wyjątek zawsze znaczy 404”;
 *   (2) ResponseStatusException — rzucasz wyjątek ze statusem i powodem w miejscu błędu, bez nowej klasy;
 *   (3) @ExceptionHandler — metoda w kontrolerze, która łapie wyjątek i buduje własną odpowiedź (kod + ciało JSON).
 *   Metody @ExceptionHandler z kontrolera działają TYLKO dla tego kontrolera. Globalnie — Err03ControllerAdvice.
 *
 * ANALOGIA: reklamacje w sklepie.
 *   (1) to pieczątka na wzorze reklamacji „towar niedostępny → zwrot pieniędzy”. (2) to ręczna notatka sprzedawcy na
 *   konkretnej reklamacji. (3) to pracownik działu reklamacji, który przejmuje sprawę i sam pisze odpowiedź klientowi.
 *   Pracownik działu elektroniki nie rozpatruje reklamacji z działu odzieży — tak działa lokalność handlera.
 *
 * JAK TO DZIAŁA:
 *   kontroler rzuca wyjątek → DispatcherServlet pyta po kolei trzy „rozwiązywacze wyjątków” (HandlerExceptionResolver):
 *     1) ExceptionHandlerExceptionResolver — szuka metody @ExceptionHandler (najpierw w kontrolerze, potem w @ControllerAdvice)
 *     2) ResponseStatusExceptionResolver   — czyta @ResponseStatus z klasy wyjątku i obsługuje ResponseStatusException
 *     3) DefaultHandlerExceptionResolver   — standardowe wyjątki Springa MVC (np. brak parametru → 400)
 *   Pierwszy, który „załatwi” wyjątek, wygrywa. Nikt nie załatwił → wyjątek leci do Tomcata → /error → 500 (Err01DefaultErrors).
 *   Dlatego @ExceptionHandler (krok 1) wygrywa z @ResponseStatus na klasie wyjątku (krok 2).
 *   Wybór metody: ze wszystkich handlerów w kontrolerze Spring bierze ten o NAJBLIŻSZYM typie wyjątku.
 *
 * SŁÓWKA:
 *   exception handler = metoda obsługi wyjątku; resolver = rozwiązywacz; reason = powód; conflict = konflikt (409);
 *   gone = zniknął na stałe (410); bad request = złe zapytanie (400); most specific = najbardziej szczegółowy;
 *   swallow = połknąć (obsłużyć tak szeroko, że ginie informacja); local = lokalny; handler = obsługujący.
 *
 * ZOBACZ TEŻ: s06_error_handling/Err01DefaultErrors (co jest bez obsługi), s06_error_handling/Err03ControllerAdvice
 *             (obsługa globalna), s00_start/Start04TestingIntro (PriceController z @ExceptionHandler),
 *             t10_exceptions/Exceptions02CheckedUnchecked (hierarchia wyjątków).
 * </pre>
 */
public final class Err02ExceptionHandler {

    private Err02ExceptionHandler() {
    }

    // =================================================================================================
    // 1. @ResponseStatus NA KLASIE WYJĄTKU (Err02ExceptionHandlerTest, Err02ExceptionHandlerRealServerTest)
    // =================================================================================================
    //
    // ProductNotFoundException ma {@code @ResponseStatus(value = NOT_FOUND, reason = "...")}. Gdy wyleci z kontrolera,
    // Spring ustawia 404 i wywołuje response.sendError(404, powód). Co dalej:
    //   • MockMvc: status 404, powód w getErrorMessage(), ciało PUSTE;
    //   • prawdziwy serwer: sendError uruchamia /error (Err01DefaultErrors), więc ciało to domyślny JSON Boota
    //     — a powodu w nim NIE ma, bo include-message=never.
    // Wygoda: zero kodu obsługi. Ograniczenie: nie ustawisz własnego ciała JSON.
    //
    // PUŁAPKA: @ResponseStatus na klasie wyjątku z biblioteki, której nie możesz zmienić, jest niemożliwy.
    //   Dlaczego? Adnotację dopisuje się do deklaracji klasy. Dla cudzych wyjątków użyj @ExceptionHandler (punkt 3).

    // =================================================================================================
    // 2. ResponseStatusException (Err02ExceptionHandlerTest)
    // =================================================================================================
    //
    //   throw new ResponseStatusException(HttpStatus.CONFLICT, "Towar chwilowo niedostępny: " + id);
    // Jedna linijka, bez osobnej klasy. Obsługuje ją ten sam ResponseStatusExceptionResolver, więc skutek jest taki jak
    // w punkcie 1 (sendError: ciało robi /error). Sprawdzone testem: status 409, powód w getErrorMessage().
    //
    // DOBRA PRAKTYKA: ResponseStatusException na szybko, w małych kontrolerach; w większej aplikacji — własne wyjątki
    //   domenowe + jedna globalna obsługa (Err03, Err05). Dlaczego? Warstwa serwisu nie powinna znać HTTP: serwis rzuca
    //   ProductNotFoundException, a dopiero warstwa web decyduje, że to 404. ResponseStatusException w serwisie wiąże
    //   logikę biznesową z protokołem HTTP i utrudnia jej ponowne użycie (np. z zadania wsadowego).

    // =================================================================================================
    // 3. LOKALNY @ExceptionHandler (Err02ExceptionHandlerTest)
    // =================================================================================================
    //
    // W CatalogController:
    //   @ExceptionHandler(IllegalArgumentException.class)          ← „łap ten typ wyjątku (i jego podklasy)”
    //   @ResponseStatus(HttpStatus.BAD_REQUEST)                    ← „odpowiedz kodem 400” (tym razem na METODZIE)
    //   public Map<String,String> badArgument(IllegalArgumentException e) { return Map.of("error", "..."); }
    // Wartość zwrócona z metody trafia do ciała odpowiedzi tak jak w zwykłym @RestController (JSON). Dostajesz pełną
    // kontrolę nad kodem i ciałem. Sprawdzone testem: 400 i {"error": "Niepoprawne dane: ..."}; ta odpowiedź NIE przechodzi
    // przez /error (test na prawdziwym serwerze: ciało ma tylko pole error).
    // Metoda może przyjąć wyjątek, a także np. HttpServletRequest, i zwrócić ResponseEntity (kod + nagłówki + ciało).
    //
    // PUŁAPKA: komunikat wyjątku wklejony do odpowiedzi ({@code e.getMessage()}) może wyciec dane wewnętrzne.
    //   Dlaczego? Komunikat pisał programista „dla siebie” (nazwa tabeli, SQL). Do klienta wysyłaj tylko komunikaty
    //   napisane świadomie dla niego (jak "Rabat musi być w zakresie 0..100") albo stałe teksty (Err05CustomExceptions).

    // =================================================================================================
    // 4. KTÓRY HANDLER WYGRYWA? (Err02ExceptionHandlerTest)
    // =================================================================================================
    //
    // InventoryController ma dwa handlery: IllegalArgumentException (wąski) i RuntimeException (szeroki).
    //   NumberFormatException (podklasa IAE)  → handler IllegalArgumentException (400)   ← bliższy typ
    //   IllegalStateException                 → handler RuntimeException (500)          ← jedyny pasujący
    // Zasada: Spring wybiera handler o najmniejszej „odległości” w hierarchii klas wyjątku. Sprawdzone testem.
    // Drugi wniosek: @ExceptionHandler z kontrolera wygrywa z @ResponseStatus na klasie wyjątku (OutdatedProductException:
    // adnotacja mówi 410, handler mówi 404 → klient dostaje 404). Sprawdzone testem.
    //
    // PUŁAPKA: szeroki handler {@code @ExceptionHandler(RuntimeException.class)} połyka też wyjątki z @ResponseStatus
    //   i ResponseStatusException. Dlaczego? Handler z kontrolera działa PRZED ResponseStatusExceptionResolver, a
    //   ProductNotFoundException jest RuntimeException — więc zamiast zamierzonego 404 klient dostaje 500
    //   (test InventoryController /api/inventory/missing). Szeroki handler dopisuj ostrożnie albo dodaj osobne handlery
    //   dla wyjątków, które mają mieć inny kod.

    // =================================================================================================
    // 5. HANDLER JEST LOKALNY (Err02ExceptionHandlerTest)
    // =================================================================================================
    //
    // OrderController ma handler OrderNotFoundException i działa. IllegalArgumentException rzucone w OrderController NIE jest
    // obsłużone, mimo że CatalogController ma na nie handler: w MockMvc wyjątek wylatuje z perform() (Err01DefaultErrors),
    // na serwerze byłoby 500. Chcesz jedną obsługę dla wszystkich kontrolerów? → Err03ControllerAdvice.
    //
    // PYTANIE REKRUTACYJNE: „Jak obsłużyć wyjątki w Spring MVC?” — @ExceptionHandler w kontrolerze (lokalnie),
    //   @RestControllerAdvice (globalnie), @ResponseStatus / ResponseStatusException (sam kod statusu), a dla standardu
    //   odpowiedzi błędu — ProblemDetail (Err04ProblemDetail).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Domyślnie niezłapany wyjątek = 500. Mapuj wyjątki na właściwe kody.
     *   • @ResponseStatus(NOT_FOUND) na klasie wyjątku — zero kodu, ale bez własnego ciała; nie dla cudzych klas.
     *   • throw new ResponseStatusException(status, powód) — szybko, ale wiąże kod z HTTP; w serwisach unikaj.
     *   • @ExceptionHandler(Typ.class) w kontrolerze + @ResponseStatus na metodzie — pełna kontrola nad kodem i ciałem.
     *   • Wygrywa handler o najbliższym typie wyjątku; @ExceptionHandler wygrywa z @ResponseStatus z klasy wyjątku.
     *   • Handler z kontrolera działa tylko dla niego. Zbyt szeroki (RuntimeException) połyka resztę.
     *   • Nie wklejaj e.getMessage() do odpowiedzi bez zastanowienia.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie trzy rozwiązywacze wyjątków sprawdza DispatcherServlet i w jakiej kolejności?
     *   2. Co się stanie? Kontroler ma handlery dla IllegalArgumentException i RuntimeException, a metoda rzuca
     *      NumberFormatException. Który handler się wykona?
     *   3. Co się stanie? Wyjątek ma @ResponseStatus(GONE), a kontroler ma @ExceptionHandler dla tego wyjątku
     *      zwracający 404. Jaki kod dostanie klient?
     *   4. ZNAJDŹ BŁĄD: kontroler ma {@code @ExceptionHandler(RuntimeException.class)} i zwraca 500, a testy pokazują,
     *      że zamiast zamierzonego 404 (wyjątek z @ResponseStatus(NOT_FOUND)) klient dostaje 500. Dlaczego?
     *   5. Dlaczego w serwisie lepiej rzucić własny wyjątek domenowy niż ResponseStatusException?
     *   6. Co się stanie? CatalogController obsługuje IllegalArgumentException, a OrderController rzuca ten wyjątek.
     *      Jaką odpowiedź dostanie klient?
     *   7. ZNAJDŹ BŁĄD: {@code @ExceptionHandler} bez parametru wyjątku i bez wartości w adnotacji, np.
     *      {@code @ExceptionHandler public String h() {...}} — co jest z tym nie tak? (zajrzyj do odpowiedzi)
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Err02ExceptionHandlerExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć kod HTTP dla wyjątku: IllegalArgumentException (także podklasy) → 400,
     * NoSuchElementException → 404, każdy inny → 500. Podpowiedź: {@code instanceof}; pamiętaj o NumberFormatException.
     */
    static HttpStatus exercise1(Throwable e) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): kontroler (zagnieżdżona klasa z {@code @RestController} — aplikacja lekcji ją
     * zeskanuje, ale tylko jako dodatkowy adres /api/ex/...; test tworzy ją przez new i sprawdza BEZ Springa). Endpoint {@code GET /api/ex/items/{id}}: id mniejsze od 1 →
     * {@code ResponseStatusException} 400 (powód "Id musi być dodatnie"), id większe od 100 → 404 (powód
     * "Brak przedmiotu"), inaczej JSON {"id": id}. Podpowiedź: {@code new ResponseStatusException(HttpStatus.X, "powód")}.
     */
    @RestController
    static class Exercise2Controller {

        @GetMapping("/api/ex/items/{id}")
        Map<String, Object> item(@PathVariable long id) {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): kontroler z endpointem {@code GET /api/ex/orders/{id}}, który rzuca
     * OrderNotFoundException dla id 404. Napisz metodę {@code @ExceptionHandler}, która zamieni ten wyjątek na
     * 404 i ciało {"error": "<komunikat wyjątku>"}. Podpowiedź: ResponseEntity.status(...).body(Map.of(...))
     * albo {@code @ResponseStatus} na metodzie; nie zapomnij o adnotacji {@code @ExceptionHandler} nad metodą!
     */
    @RestController
    static class Exercise3Controller {

        @GetMapping("/api/ex/orders/{id}")
        Map<String, Object> order(@PathVariable long id) {
            if (id == 404) {
                throw new OrderNotFoundException(id);
            }
            return Map.of("id", id);
        }

        // TODO: dodaj adnotację @ExceptionHandler(OrderNotFoundException.class) i wypełnij metodę
        ResponseEntity<Map<String, String>> notFound(OrderNotFoundException e) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. (1) ExceptionHandlerExceptionResolver (metody @ExceptionHandler), (2) ResponseStatusExceptionResolver
     *      (@ResponseStatus i ResponseStatusException), (3) DefaultHandlerExceptionResolver (standardowe wyjątki MVC).
     *      Pierwszy, który obsłuży wyjątek, wygrywa. Nikt → Tomcat → /error → 500.
     *   2. Handler IllegalArgumentException (400): NumberFormatException jest jego podklasą, więc ma bliższy typ niż
     *      RuntimeException. Sprawdzone testem mostSpecificHandlerWins (/api/inventory/parse?value=abc → 400).
     *   3. 404. @ExceptionHandler (resolver 1) działa przed @ResponseStatus z klasy (resolver 2), więc adnotacja
     *      zostaje zignorowana. Sprawdzone testem handlerBeatsResponseStatusAnnotation.
     *   4. Handler z kontrolera działa przed ResponseStatusExceptionResolver, a wyjątek z adnotacją jest
     *      RuntimeException — szeroki handler go przechwytuje i odpowiada 500. Sprawdzone testem
     *      broadHandlerSwallowsResponseStatusException. Poprawka: osobny handler dla tego wyjątku albo węższy typ.
     *   5. Serwis nie powinien znać HTTP. Wyjątek domenowy opisuje problem biznesowy i da się go użyć w każdym kontekście
     *      (REST, zadanie wsadowe, test); zamianę na 404 robi warstwa web w jednym miejscu.
     *   6. Błąd 500 (domyślne ciało Boota) — handler z CatalogController nie działa w OrderController. W MockMvc wyjątek
     *      wylatuje z perform(). Sprawdzone testem handlersAreLocalToController. Rozwiązanie: Err03ControllerAdvice.
     *   7. Bez wartości w adnotacji Spring bierze typy wyjątków z PARAMETRÓW metody; gdy nie ma ani jednego, ani drugiego,
     *      nie wie, co łapać. Przy obsłudze wyjątku leci IllegalStateException "No exception types mapped to ..." i
     *      klient dostaje błąd zamiast Twojej odpowiedzi. Zawsze podawaj typ w adnotacji albo jako parametr.
     *      Sprawdzone uruchomieniem (próbny test z MockMvc).
     */
    // </editor-fold>
}
