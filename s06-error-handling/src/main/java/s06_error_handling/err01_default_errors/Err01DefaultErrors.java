package s06_error_handling.err01_default_errors;

import java.util.Map;
import java.util.Set;

/**
 * <pre>
 * TEMAT: Domyślna obsługa błędów w Spring Boot — co się dzieje, gdy kontroler rzuci wyjątek
 *        (error handling = obsługa błędów; exception = wyjątek; status code = kod statusu HTTP;
 *         whitelabel = „bezmarkowy”, domyślny; stack trace = ślad stosu wywołań)
 *
 * W SKRÓCIE:
 *   Gdy metoda kontrolera rzuci wyjątek, którego nikt nie złapał, serwer odpowiada 500 Internal Server Error
 *   (wewnętrzny błąd serwera). Spring Boot sam przygotowuje ciało odpowiedzi: JSON z polami timestamp, status, error
 *   i path. Robi to specjalny kontroler BasicErrorController pod adresem /error. Domyślnie NIE wysyła komunikatu
 *   wyjątku ani stack trace — i słusznie. UWAGA: ten mechanizm działa tylko na prawdziwym serwerze. MockMvc go nie ma.
 *
 * ANALOGIA: recepcja w hotelu.
 *   Gość (klient) prosi o coś, a w kuchni (kontrolerze) wybucha pożar (wyjątek). Recepcja (/error) nie opowiada gościowi,
 *   co się spaliło i jaki to piec — mówi tylko „przepraszamy, awaria, numer zgłoszenia taki a taki”. Szczegóły (stack
 *   trace) trafiają do dziennika hotelu (logu), a nie do gościa.
 *
 * JAK TO DZIAŁA:
 *   GET /api/boom
 *     → DispatcherServlet wywołuje Err01Controller.boom()  → IllegalStateException
 *     → nikt nie łapie (brak @ExceptionHandler)            → wyjątek wylatuje do kontenera serwletów (Tomcata)
 *     → Tomcat: status 500, a w konfiguracji błędów jest „strona błędu” /error (rejestruje ją Spring Boot)
 *     → Tomcat przekazuje żądanie WEWNĘTRZNIE (forward) na /error
 *     → BasicErrorController (bean z ErrorMvcAutoConfiguration) składa ciało z DefaultErrorAttributes
 *     → klient dostaje: 500 {"timestamp":"...","status":500,"error":"Internal Server Error","path":"/api/boom"}
 *   Format zależy od nagłówka Accept: przeglądarka (text/html) dostaje stronę „Whitelabel Error Page”, reszta — JSON.
 *
 * SŁÓWKA:
 *   timestamp = znacznik czasu; status = status (liczba); error = nazwa błędu (tekst statusu); path = ścieżka żądania;
 *   message = komunikat; trace = ślad stosu; include = dołącz; binding errors = błędy wiązania (walidacji);
 *   forward = przekazanie wewnętrzne żądania; error page = strona błędu; whitelabel = domyślna strona „bez marki”;
 *   leak = wyciek; propagate = propagować (wylecieć wyżej).
 *
 * ZOBACZ TEŻ: s00_start/Start04TestingIntro (MockMvc, @WebMvcTest), s00_start/Start05HttpFiles (RANDOM_PORT i pliki .http),
 *             s06_error_handling/Err02ExceptionHandler (jak przejąć kontrolę nad błędem),
 *             t10_exceptions/Exceptions01Basics (wyjątki w Javie).
 * </pre>
 */
public final class Err01DefaultErrors {

    private Err01DefaultErrors() {
    }

    // =================================================================================================
    // 1. DOMYŚLNY JSON BŁĘDU — BasicErrorController i /error (Err01DefaultErrorsTest)
    // =================================================================================================
    //
    // Pola domyślnego ciała w Boot 4.1:
    //   timestamp  — kiedy wystąpił błąd (nie sprawdzamy go w testach: zmienia się przy każdym zapytaniu),
    //   status     — liczba, np. 500,
    //   error      — tekst statusu, np. "Internal Server Error",
    //   path       — adres żądania, np. "/api/boom".
    // Pól message, exception, trace i errors domyślnie NIE MA. To samo ciało dostaniesz dla 404 (brak adresu)
    // i 400 (zły typ parametru) — wszystkie błędy przechodzą przez ten sam /error.
    //
    // Boot 4: klasy BasicErrorController i DefaultErrorAttributes leżą w modułach spring-boot-webmvc
    // (pakiety org.springframework.boot.webmvc.autoconfigure.error i org.springframework.boot.webmvc.error).
    // Importy z tutoriali do Boot 3 (org.springframework.boot.autoconfigure.web.servlet.error...) już nie zadziałają.
    //
    // PUŁAPKA: ciało błędu zależy od nagłówka Accept. Dlaczego? BasicErrorController ma DWIE metody pod /error:
    //   jedną dla text/html (Whitelabel Error Page) i drugą dla reszty (JSON). Klient przeglądarkowy i klient API
    //   dostają więc dwa różne kształty odpowiedzi na ten sam błąd.

    // =================================================================================================
    // 2. MockMvc NIE PRZEKAZUJE NA /error (Err01DefaultErrorsMockMvcTest)
    // =================================================================================================
    //
    // Przekazanie na /error robi KONTENER SERWLETÓW (Tomcat), a MockMvc go nie ma — wysyła żądanie prosto do
    // DispatcherServlet. Skutki, które sprawdza test:
    //   • niezłapany wyjątek wylatuje z mockMvc.perform(...) jako ServletException (przyczyna: IllegalStateException),
    //     zamiast zamienić się w odpowiedź 500;
    //   • dla 400 i 404 jest sam status — ciało puste, żadnego JSON-a z timestamp/status/error/path.
    // Chcesz zobaczyć PRAWDZIWE ciało błędu? Użyj @SpringBootTest(webEnvironment = RANDOM_PORT) i RestClient
    // (s00_start/Start05HttpFiles). Pamiętaj, że RestClient domyślnie rzuca wyjątek dla 4xx/5xx — żeby odczytać ciało,
    // dodaj {@code .onStatus(status -> true, (request, response) -> {})}.
    //
    // PUŁAPKA: test MockMvc na wyjątek jest zielony, a na produkcji klient widzi 500 z JSON-em — to dwa różne obrazy.
    //   Dlaczego? MockMvc pomija warstwę kontenera serwletów, więc „dobrze wyglądające” testy mogą nie odzwierciedlać
    //   tego, co zobaczy użytkownik. Testy własnej obsługi błędów (@ExceptionHandler) działają w MockMvc, bo należą
    //   do DispatcherServlet — o tym w Err02ExceptionHandler.

    // =================================================================================================
    // 3. CO WOLNO POKAZAĆ — spring.web.error.* (Err01DefaultErrorsLeakyTest)
    // =================================================================================================
    //
    //   spring.web.error.include-message=never        never (nigdy) | always (zawsze) | on-param (gdy ?message=true)
    //   spring.web.error.include-stacktrace=never     never | always | on-param (gdy ?trace=true)
    //   spring.web.error.include-binding-errors=never never | always | on-param (gdy ?errors=true)
    //   spring.web.error.include-exception=false      true → dodaje pole exception z nazwą klasy wyjątku
    //   spring.web.error.include-path=always          never → bez pola path
    //   spring.web.error.path=/error                  adres kontrolera błędów
    //   spring.web.error.whitelabel.enabled=true      false → wyłącza stronę HTML „Whitelabel”
    // Boot 4: te klucze zastąpiły {@code server.error.*} (w Boot 3). Stare klucze są w metadanych Boota 4.1.1 oznaczone
    // jako przestarzałe (deprecated) — w nowym kodzie pisz spring.web.error.*.
    // Sprawdzone testem Err01DefaultErrorsLeakyTest: z include-message=always w odpowiedzi jest adres bazy i nazwa
    // użytkownika z komunikatu wyjątku; z include-stacktrace=on_param pełny stack trace pojawia się po ?trace=true.
    //
    // PUŁAPKA: włączenie include-message/include-stacktrace „na chwilę, do debugowania” i zapomnienie o tym.
    //   Dlaczego? Komunikaty wyjątków zawierają adresy baz, nazwy tabel, fragmenty SQL; stack trace zdradza strukturę
    //   aplikacji i wersje bibliotek — to materiał dla atakującego (OWASP: „Information Exposure Through an Error Message”).
    //
    // DOBRA PRAKTYKA: na produkcji zostaw wszystko na never, a szczegóły zapisuj w logu serwera. Dlaczego? Klient
    //   potrzebuje wiedzieć CO poszło nie tak (kod, krótki opis), a programista — DLACZEGO (stack trace). Te dwie
    //   rzeczy mają różnych odbiorców i różne poziomy zaufania. Własny, bezpieczny komunikat dla klienta ustawisz
    //   w Err02ExceptionHandler i Err03ControllerAdvice.

    // =================================================================================================
    // 4. KODY STATUSU — kto jest winny?
    // =================================================================================================
    //
    //   4xx — błąd KLIENTA (zły adres 404, zły parametr 400, brak uprawnień 403) — klient ma poprawić zapytanie;
    //   5xx — błąd SERWERA (500 — nieobsłużony wyjątek w kodzie) — to my musimy poprawić kod.
    // Niezłapany wyjątek zawsze daje 500, nawet jeśli „winny” jest klient (np. nieistniejące id → NoSuchElementException
    // → 500 zamiast 404). To zła odpowiedź: klient myśli, że to awaria serwera. Dlatego uczymy się mapować wyjątki
    // na właściwe kody (Err02, Err03, Err05).
    //
    // PYTANIE REKRUTACYJNE: „Czym różni się 400 od 500 i kto powinien reagować?” — 4xx to wina zapytania (klient
    //   poprawia), 5xx to wina serwera (my poprawiamy); błędny kod w odpowiedzi psuje monitoring i ponawianie zapytań.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Niezłapany wyjątek w kontrolerze → 500; Boot przekazuje żądanie na /error (BasicErrorController).
     *   • Domyślny JSON: timestamp, status, error, path. Bez message, exception, trace (bezpieczne ustawienie).
     *   • Accept: text/html → strona Whitelabel Error Page; inne → JSON.
     *   • MockMvc nie ma kontenera serwletów: wyjątek wylatuje z perform(), a 400/404 mają puste ciało.
     *   • Prawdziwe ciało błędu: @SpringBootTest(RANDOM_PORT) + RestClient + onStatus(status -> true, ...).
     *   • Klucze Boot 4: spring.web.error.include-message / include-stacktrace / include-binding-errors /
     *     include-exception / include-path (dawniej server.error.*).
     *   • Na produkcji: never + szczegóły w logu.
     *
     * PYTANIA KONTROLNE:
     *   1. Co zwraca Spring Boot domyślnie, gdy kontroler rzuci niezłapany wyjątek? Jakie pola ma ciało?
     *   2. Co się stanie? W teście @WebMvcTest wywołasz mockMvc.perform(get("/api/boom")) — dostaniesz status 500 w wyniku
     *      czy wyjątek?
     *   3. Dlaczego domyślny JSON błędu nie ma pola message?
     *   4. ZNAJDŹ BŁĄD: w application.properties produkcyjnej aplikacji jest {@code spring.web.error.include-stacktrace=always}.
     *      Co jest nie tak?
     *   5. Co się stanie? Ustawisz include-stacktrace=on_param i wyślesz GET /api/boom BEZ ?trace=true — będzie pole trace?
     *   6. Dlaczego kod napisany na podstawie tutoriala z {@code server.error.include-message} może nie działać zgodnie
     *      z oczekiwaniem w Boot 4?
     *   7. ZNAJDŹ BŁĄD: klient dostał 500, bo wysłał nieistniejące id produktu (kod rzuca NoSuchElementException).
     *      Który kod HTTP byłby właściwy i dlaczego?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Err01DefaultErrorsExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć zbiór nazw pól, które domyślny JSON błędu w Boot 4 zawiera
     * (bez żadnych włączonych dodatków). Podpowiedź: patrz sekcja 1 tej lekcji; {@code Set.of(...)}.
     */
    static Set<String> exercise1() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć właściwości (klucz → wartość) bezpieczne dla produkcji: komunikat, stack trace
     * i błędy wiązania — nigdy; nazwa klasy wyjątku — wyłączona (false). Użyj kluczy {@code spring.web.error.include-message},
     * {@code ...include-stacktrace}, {@code ...include-binding-errors}, {@code ...include-exception}.
     * Podpowiedź: sekcja 3; {@code Map.of(...)}.
     */
    static Map<String, String> exercise2() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): z ciała błędu (mapa jak z RestClient) zbuduj jednolinijkowy opis
     * "404 Not Found: /api/nie-ma" (status, spacja, error, dwukropek, spacja, path). Gdy w mapie jest pole "message",
     * dopisz je na końcu w nawiasie: "500 Internal Server Error: /api/boom (Brak bazy)". Pole status to Integer.
     * Podpowiedź: {@code body.get("status")}, {@code body.containsKey("message")}.
     */
    static String exercise3(Map<String, Object> errorBody) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Status 500 i JSON z polami timestamp, status, error, path (przeglądarka — stronę Whitelabel). Przygotowuje go
     *      BasicErrorController pod /error. Sprawdzone testem Err01DefaultErrorsTest.
     *   2. Wyjątek: perform() rzuca jakarta.servlet.ServletException, a jej przyczyną jest IllegalStateException.
     *      Odpowiedzi 500 nie ma, bo MockMvc nie ma kontenera serwletów, który zamieniłby wyjątek w odpowiedź.
     *      Sprawdzone testem Err01DefaultErrorsMockMvcTest.
     *   3. Bo to zabezpieczenie: komunikat wyjątku może zawierać adres bazy, SQL, dane osobowe. Domyślnie
     *      include-message=never. Sprawdzone testem (noLeaksByDefault) — w ciele nie ma "jdbc" ani "shop_user".
     *   4. Stack trace na produkcji zdradza strukturę aplikacji, nazwy klas i wersje bibliotek — pomaga atakującemu.
     *      Ma być never (stack trace trafia do logu serwera).
     *   5. Nie. Przy on_param pole trace jest tylko wtedy, gdy w adresie jest ?trace=true. Sprawdzone testem
     *      Err01DefaultErrorsLeakyTest (stacktraceOnParam) — z ?trace=true pole trace jest; bez niego (messageAndExceptionLeak)
     *      go nie ma.
     *   6. Bo w Boot 4 klucze nazywają się spring.web.error.*; server.error.* są oznaczone jako przestarzałe
     *      (w metadanych konfiguracji Boota 4.1.1), więc nowy kod powinien używać nowych kluczy.
     *   7. 404 Not Found (nie ma zasobu) — to błąd klienta, nie serwera. 500 wprowadza w błąd klienta i alarmuje
     *      monitoring fałszywą awarią. Jak to naprawić — Err02ExceptionHandler.
     */
    // </editor-fold>
}
