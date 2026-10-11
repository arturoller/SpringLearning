package s06_error_handling.err04_problem_detail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;

/**
 * <pre>
 * TEMAT: ProblemDetail (RFC 9457) — jeden, standardowy kształt odpowiedzi z błędem
 *        (problem detail = szczegóły problemu; RFC = dokument standaryzujący protokoły internetu;
 *         media type = typ zawartości, tu application/problem+json)
 *
 * W SKRÓCIE:
 *   Każde API odsyła błędy po swojemu: raz {"error": "..."}, raz {"message": "..."}, raz {"blad": ...}. Klient musi znać
 *   każdy format. RFC 9457 (następca RFC 7807) ustala wspólny: obiekt JSON z polami type, title, status, detail,
 *   instance oraz dowolnymi własnymi polami, z typem zawartości application/problem+json. Spring ma do tego klasę
 *   ProblemDetail, wyjątek ErrorResponseException i bazową klasę advice ResponseEntityExceptionHandler.
 *
 * ANALOGIA: formularz zgłoszenia awarii.
 *   Zamiast pisać do serwisu wolny tekst („nie działa”), każdy wypełnia ten sam formularz: rodzaj usterki (type),
 *   krótki tytuł (title), numer statusu (status), opis konkretnego przypadku (detail) i numer zgłoszenia (instance).
 *   Serwisant (klient API) zawsze wie, gdzie szukać czego, a dodatkowe rubryki (własne pola) niczego nie psują.
 *
 * JAK TO DZIAŁA:
 *   {
 *     "type": "https://shop.example.com/problems/product-not-found",   ← adres-identyfikator RODZAJU problemu (nie musi działać)
 *     "title": "Produkt nie istnieje",                                 ← krótki opis rodzaju; NIE zmienia się między wystąpieniami
 *     "status": 404,                                                   ← kod HTTP (powtórzony w ciele)
 *     "detail": "Nie znaleziono produktu o id 999",                    ← opis TEGO konkretnego wystąpienia
 *     "instance": "/api/products/999",                                 ← identyfikator wystąpienia (Spring wpisuje adres żądania)
 *     "productId": 999                                                 ← własne pole (setProperty) — na głównym poziomie JSON-a
 *   }
 *   Metoda @ExceptionHandler zwraca ProblemDetail → Spring wybiera konwerter JSON i Content-Type application/problem+json.
 *   ResponseEntityExceptionHandler to gotowa klasa bazowa: ma @ExceptionHandler dla wielu wyjątków Springa MVC
 *   (brak parametru, zła metoda HTTP, zły typ parametru...) i zamienia je na ProblemDetail.
 *
 * SŁÓWKA:
 *   problem detail = szczegóły problemu; type = rodzaj; title = tytuł; status = status; detail = szczegół;
 *   instance = wystąpienie (konkretny przypadek); property = własność (własne pole); extension = rozszerzenie;
 *   conditional = warunkowy; media type = typ zawartości; obsolete = zastąpić (RFC 9457 zastępuje RFC 7807).
 *
 * ZOBACZ TEŻ: s06_error_handling/Err03ControllerAdvice (advice, kolejność), s06_error_handling/Err02ExceptionHandler
 *             (ResponseStatusException), s06_error_handling/Err05CustomExceptions (walidacja jako ProblemDetail),
 *             s00_start/Start05HttpFiles (żądania HTTP).
 * </pre>
 */
public final class Err04ProblemDetail {

    private Err04ProblemDetail() {
    }

    // =================================================================================================
    // 1. OBIEKT ProblemDetail (Err04ProblemDetailTest)
    // =================================================================================================
    //
    //   ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Za mało towaru");   // status + detail
    //   p.setType(URI.create("https://shop.example.com/problems/out-of-stock"));                     // rodzaj problemu
    //   p.setTitle("Brak towaru");
    //   p.setInstance(URI.create("/api/orders/17"));                                                  // zwykle ustawia Spring
    //   p.setProperty("available", 2);                                                                // własne pole
    // Fabryki: forStatus(status), forStatusAndDetail(status, detail). W Spring Framework 7.0.9 nieustawiony type to null
    // (w JSON-ie pola nie ma — według RFC brak typu oznacza "about:blank", czyli "nic ponad status HTTP"), a getTitle()
    // bez setTitle() zwraca opis statusu (np. "Not Found"). Sprawdzone testem Err04ProblemDetailTest.
    //
    // PUŁAPKA: wstawianie do title/detail danych zależnych od wystąpienia i wrażliwych (SQL, ścieżki, stack trace).
    //   Dlaczego? title ma opisywać RODZAJ problemu i być stały (klienci mogą go porównywać), a detail trafia do człowieka —
    //   oba pola widzi każdy klient API, więc obowiązują te same zasady co przy include-message (Err01DefaultErrors).
    //
    // DOBRA PRAKTYKA: w type używaj stabilnego adresu-identyfikatora rodzaju problemu (np. https://shop.example.com/problems/out-of-stock),
    //   najlepiej z dokumentacją pod tym adresem. Dlaczego? Klient ma jeden niezmienny „klucz” do obsługi konkretnego błędu
    //   (switch po type), niezależny od języka komunikatu i od wersji tekstów.

    // =================================================================================================
    // 2. GOTOWY ADVICE BOOTA: spring.mvc.problemdetails.enabled (Err04ProblemDetailOffTest, Err04ProblemDetailBootTest)
    // =================================================================================================
    //
    // Domyślnie (Boot 4.1.1) właściwość ma wartość false: błędy Spring MVC mają sam status, bez ciała (MockMvc), a na
    // prawdziwym serwerze — domyślny JSON z /error (Err01DefaultErrors). Ustaw
    //   spring.mvc.problemdetails.enabled=true
    // — Boot zarejestruje advice (ProblemDetailsExceptionHandler, @Order(0)), dziedziczący po ResponseEntityExceptionHandler,
    // i wszystkie wyjątki Springa MVC oraz ResponseStatusException staną się application/problem+json:
    // brak parametru → 400, zła metoda → 405, ResponseStatusException(409) → 409. Sprawdzone testami. Komunikaty detail
    // z wyjątków Springa są po angielsku ("Required parameter 'q' is not present.").
    // Gotowy advice jest zarejestrowany warunkowo: tylko gdy w kontekście NIE MA innego beana
    // ResponseEntityExceptionHandler (@ConditionalOnMissingBean) — sprawdzone w kodzie klasy konfiguracyjnej Boota.
    // Wyjątki domenowe (ProductNotFoundException) gotowy advice zna? NIE — nadal wylatują (test domainExceptionStillUnhandled).
    //
    // Boot 4: ta sama właściwość co w Boot 3; w tutorialach spotkasz też stary sposób — własną klasę z
    // @ControllerAdvice dziedziczącą po ResponseEntityExceptionHandler (sekcja 3).
    //
    // PUŁAPKA: zakładanie, że problemdetails.enabled=true załatwia wszystkie błędy. Dlaczego? Obsługuje tylko wyjątki
    //   Springa; błędy domenowe i nieprzewidziane (NullPointerException) nadal idą do /error w domyślnym formacie —
    //   masz wtedy dwa formaty odpowiedzi w jednym API. Dopisz własny advice (sekcja 3).

    // =================================================================================================
    // 3. WŁASNY ADVICE Z ProblemDetail (Err04ProblemDetailAdviceTest)
    // =================================================================================================
    //
    // ProblemDetailAdvice dziedziczy po ResponseEntityExceptionHandler (dostajesz za darmo obsługę wyjątków Springa MVC
    // jako problem+json, bez ustawiania problemdetails.enabled) i dopisuje:
    //   @ExceptionHandler(ProductNotFoundException.class)
    //   public ProblemDetail productNotFound(ProductNotFoundException e) { ... return problem; }
    // Metoda zwraca ProblemDetail — bez ResponseEntity i bez @ResponseStatus: status bierze się z ProblemDetail.
    // Spring sam dopisuje instance (adres żądania). Własne pole productId trafia na główny poziom JSON-a.
    // Gdy w kontekście jest taki własny advice, Boot nie tworzy swojego (test onlyOneResponseEntityExceptionHandler).
    //
    // PUŁAPKA: advice z ResponseEntityExceptionHandler i dodatkowy osobny advice z {@code @ExceptionHandler(Exception.class)}
    //   bez @Order. Dlaczego? Catch-all może przechwycić wyjątki Springa przed klasą bazową i zamienić je na 500
    //   (Err03ControllerAdvice, sekcja 1). Catch-all trzymaj w TEJ SAMEJ klasie (wtedy wygrywa najbliższy typ)
    //   albo w advice o najniższym priorytecie.

    // =================================================================================================
    // 4. ErrorResponseException i ResponseStatusException (Err04ProblemDetailBootTest, Err04ProblemDetailAdviceTest)
    // =================================================================================================
    //
    // ErrorResponseException to wyjątek, który SAM niesie ProblemDetail (konstruktor: status, ProblemDetail, przyczyna).
    // Można go rzucić z dowolnego miejsca, a ResponseEntityExceptionHandler odeśle dokładnie ten ProblemDetail (z uzupełnionym
    // instance). ResponseStatusException (Err02ExceptionHandler) jest jego podklasą, więc przy włączonym advice z
    // ResponseEntityExceptionHandler dostaje kształt problem+json zamiast pustego ciała. Można też dziedziczyć po
    // ErrorResponseException i zrobić z własnego wyjątku domenowego od razu gotową odpowiedź — ale wtedy wyjątek
    // zna HTTP (por. Err02ExceptionHandler, DOBRA PRAKTYKA przy ResponseStatusException).
    //
    // PYTANIE REKRUTACYJNE: „Jak zwracać błędy w REST API w jednolitym formacie?” — ProblemDetail (RFC 9457,
    //   application/problem+json) wraz z @RestControllerAdvice dziedziczącym po ResponseEntityExceptionHandler, albo
    //   spring.mvc.problemdetails.enabled=true; własne rozszerzenia przez setProperty.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • RFC 9457: type, title, status, detail, instance + własne pola; Content-Type: application/problem+json.
     *   • ProblemDetail.forStatusAndDetail(status, detail); setType, setTitle, setInstance, setProperty(nazwa, wartość).
     *   • spring.mvc.problemdetails.enabled=true (domyślnie false) → gotowy advice Boota dla wyjątków Springa MVC.
     *   • Własny advice: @RestControllerAdvice + extends ResponseEntityExceptionHandler + @ExceptionHandler zwracający ProblemDetail.
     *   • Własny ResponseEntityExceptionHandler w kontekście wyłącza gotowy advice Boota.
     *   • ErrorResponseException = wyjątek z gotowym ProblemDetail; ResponseStatusException jest jego podklasą.
     *   • title stały dla rodzaju problemu, detail dla wystąpienia; nigdy dane wewnętrzne.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie pięć pól definiuje RFC 9457 i które z nich opisuje rodzaj problemu, a które konkretne wystąpienie?
     *   2. Co się stanie? Wywołasz setProperty("productId", 999) — gdzie w JSON-ie znajdzie się pole productId?
     *   3. Co się stanie? W Boot 4.1.1 nie ustawisz żadnej właściwości i nie napiszesz advice, a klient wyśle POST na
     *      adres obsługujący tylko GET. Jakie ciało zobaczy w teście MockMvc?
     *   4. ZNAJDŹ BŁĄD: w aplikacji jest {@code spring.mvc.problemdetails.enabled=true}, a rzucony ProductNotFoundException
     *      (zwykły wyjątek domenowy) nadal kończy się 500 w domyślnym formacie Boota. Dlaczego?
     *   5. Co się stanie? Dopiszesz własny advice dziedziczący po ResponseEntityExceptionHandler do aplikacji, w której
     *      jest problemdetails.enabled=true. Czy w kontekście będą dwa takie beany?
     *   6. Dlaczego klientowi lepiej sprawdzać pole type niż title albo detail?
     *   7. ZNAJDŹ BŁĄD: {@code problem.setDetail("SQL: " + e.getSql())} w handlerze — co jest nie tak?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Err04ProblemDetailExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zbuduj ProblemDetail dla nieistniejącego produktu: status 404, tytuł "Produkt nie istnieje",
     * detail "Nie znaleziono produktu o id <id>", typ https://shop.example.com/problems/product-not-found, własne pole
     * "productId" z id. Podpowiedź: sekcja 1 i klasa ProblemDetailAdvice.
     */
    static ProblemDetail exercise1(long productId) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zbuduj ErrorResponseException dla przekroczonego limitu zapytań: status 429, ProblemDetail
     * z tytułem "Zbyt wiele zapytań", detail "Limit zapytań przekroczony" i własnym polem "retryAfterSeconds"
     * (przekazana liczba sekund). Podpowiedź: {@code new ErrorResponseException(HttpStatus.TOO_MANY_REQUESTS, problem, null)}.
     */
    static ErrorResponseException exercise2(int retryAfterSeconds) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): advice dziedziczący po ResponseEntityExceptionHandler, który IllegalArgumentException
     * zamienia na ProblemDetail 400 z tytułem "Niepoprawne dane" i detail równym komunikatowi wyjątku. Resztę (np. 405)
     * dostajesz od klasy bazowej za darmo. Klasa jest włączana w aplikacji lekcji tylko właściwością
     * {@code err04.exercise3-advice=true} — dlatego nie koliduje z resztą lekcji. Podpowiedź: dodaj
     * {@code @ExceptionHandler(IllegalArgumentException.class)}; zwróć ProblemDetail.
     */
    @RestControllerAdvice
    @ConditionalOnProperty(prefix = "err04", name = "exercise3-advice", havingValue = "true")
    static class Exercise3Advice extends ResponseEntityExceptionHandler {

        // TODO: dodaj adnotację @ExceptionHandler i wypełnij metodę
        ProblemDetail illegalArgument(IllegalArgumentException e) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. type, title, status, detail, instance. type i title opisują RODZAJ problemu (stałe); detail i instance —
     *      konkretne wystąpienie (detail: co się stało tym razem; instance: identyfikator wystąpienia, np. adres żądania).
     *      status powtarza kod HTTP.
     *   2. Na głównym poziomie obiektu JSON, obok type/title/status (nie w zagnieżdżonym obiekcie).
     *      Sprawdzone testem Err04ProblemDetailAdviceTest (jsonPath "$.productId").
     *   3. Puste ciało i brak Content-Type — sam status 405. Sprawdzone testem Err04ProblemDetailOffTest.
     *   4. Gotowy advice Boota zna tylko wyjątki Springa MVC i ResponseStatusException/ErrorResponseException. Własny
     *      wyjątek domenowy wymaga własnego @ExceptionHandler. Sprawdzone testem domainExceptionStillUnhandled
     *      (wyjątek wylatuje z MockMvc; na serwerze byłoby 500).
     *   5. Nie, tylko jeden — Boot rejestruje swój advice z warunkiem @ConditionalOnMissingBean(ResponseEntityExceptionHandler),
     *      więc własny go wypiera. Sprawdzone testem onlyOneResponseEntityExceptionHandler (z własnym advice).
     *   6. title i detail to teksty dla człowieka — mogą się zmienić (poprawka literówki, tłumaczenie) i różnią się
     *      między wystąpieniami. type jest stałym identyfikatorem rodzaju problemu, więc kod klienta może na nim bezpiecznie
     *      rozgałęziać logikę.
     *   7. Do detail trafia SQL, czyli szczegół wewnętrzny — widzi go każdy klient API. To wyciek informacji
     *      (struktura bazy, nazwy tabel). Szczegóły loguj, klientowi daj ogólny opis.
     */
    // </editor-fold>
}
