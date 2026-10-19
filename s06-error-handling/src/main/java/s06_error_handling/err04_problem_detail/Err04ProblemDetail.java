package s06_error_handling.err04_problem_detail;

import java.util.Map;

/**
 * <pre>
 * TEMAT: ProblemDetail (RFC 9457) — standardowy kształt błędu w API
 *        (problem detail = szczegóły problemu; RFC = dokument standaryzacyjny internetu; extension member = pole rozszerzające)
 *
 * W SKRÓCIE:
 *   Każde API wymyślało własny kształt błędu ({"blad":...}, {"error_code":...}). RFC 9457 (następca RFC 7807) to
 *   wspólny standard: odpowiedź ma typ treści application/problem+json i pola type, title, status, detail, instance oraz
 *   dowolne własne pola. Spring ma na to klasę ProblemDetail, wyjątek niosący gotowy ProblemDetail (ErrorResponseException)
 *   i gotową klasę bazową porady ResponseEntityExceptionHandler, która zamienia na ProblemDetail wszystkie wyjątki
 *   Spring MVC. Dzięki temu klient (frontend, inny serwis) czyta błędy z całego API jednym kodem.
 *
 * ANALOGIA: formularz protokołu szkody w ubezpieczeniu.
 *   Każdy ubezpieczyciel mógłby przyjmować zgłoszenia w dowolnej formie. Ujednolicony formularz ma stałe rubryki:
 *   rodzaj szkody (type), krótki tytuł (title), kod (status), opis (detail), numer sprawy/miejsce (instance) — a na końcu
 *   wolne miejsce na dopiski (własne pola). Każdy wie, gdzie czego szukać.
 *
 * JAK TO DZIAŁA:
 *   Pola ProblemDetail (prawdziwa odpowiedź z uruchomienia, dla nieistniejącego produktu):
 *     {"detail":"Nie ma produktu o SKU XXX","instance":"/api/products/XXX","status":404,
 *      "title":"Produkt nie istnieje","type":"https://sklep.example.test/problemy/produkt-nie-istnieje","sku":"XXX"}
 *     type     — adres (URI) identyfikujący RODZAJ problemu; domyślnie "about:blank" (wtedy pole jest pomijane w JSON-ie);
 *     title    — krótki tytuł, ten sam dla danego rodzaju problemu;
 *     status   — kod HTTP (liczba), powtórzony w ciele;
 *     detail   — opis TEGO konkretnego zdarzenia;
 *     instance — adres zdarzenia; Spring sam wpisuje ścieżkę zapytania, jeśli go nie ustawisz;
 *     własne pola (setProperty) — trafiają na wierzch JSON-a (tak ustawia to mixin Jacksona ProblemDetailJacksonMixin).
 *   Ciało wysyłane jest z Content-Type: application/problem+json. Kolejność pól w JSON-ie jest alfabetyczna, a po nich
 *   idą własne — nie opieraj się na niej.
 *
 *   Trzy sposoby wytworzenia ProblemDetail:
 *     1. handler zwraca ProblemDetail (Spring ustawia status z pola status);
 *     2. wyjątek dziedziczy po ErrorResponseException i niesie własny ProblemDetail (OutOfStockException) — sam jest
 *        „odpowiedzią”; ResponseStatusException z Err02 to jego podklasa;
 *     3. porada rozszerza ResponseEntityExceptionHandler: zna wyjątki Spring MVC (405, 415, 404, 400 przy złym JSON-ie,
 *        brak parametru, walidacja...) i zamienia je na ProblemDetail; handlery można nadpisywać (handleMethodArgumentNotValid).
 *
 *   spring.mvc.problemdetails.enabled (sprawdzone w Boot 4.1.1): domyślnie false. Po ustawieniu true Boot rejestruje gotową poradę
 *   ProblemDetailsExceptionHandler (rozszerza ResponseEntityExceptionHandler, @Order(0)) — ale tylko, jeśli w kontekście nie ma
 *   już żadnego ResponseEntityExceptionHandler (@ConditionalOnMissingBean). Nazwa właściwości jest taka sama jak
 *   w Boot 3; w Boot 4 klasy trafiły do pakietu org.springframework.boot.webmvc.autoconfigure.
 *
 * SŁÓWKA:
 *   problem = problem, kłopot; detail = szczegół, opis; instance = egzemplarz (to konkretne zdarzenie); title = tytuł;
 *   extension = rozszerzenie; media type = typ treści (typ mediów); message source = źródło komunikatów (pliki
 *   .properties z tekstami, osobne na każdy język); locale = ustawienia językowe; flatten = „spłaszczyć” (na wierzch, bez zagnieżdżenia);
 *   fallback = wartość awaryjna.
 *
 * ZOBACZ TEŻ: s06_error_handling/Err01DefaultErrors (stary kształt błędu z /error), s06_error_handling/Err02ExceptionHandler
 *             (ResponseStatusException), s06_error_handling/Err03ControllerAdvice (porady i ich kolejność),
 *             s06_error_handling/Err05CustomExceptions (hierarchia wyjątków domeny), s04_web_rest/Rest04Json (Jackson 3),
 *             s05_validation/Val02ValidInControllers (walidacja w kontrolerach).
 * </pre>
 */
public final class Err04ProblemDetail {

    private Err04ProblemDetail() {
    }

    // =================================================================================================
    // 1. PROBLEMDETAIL Z HANDLERA (testy: Err04ProblemDetailTest #1, #2, #6)
    // =================================================================================================
    //
    // W ProblemAdvice: handler zwraca ProblemDetail, ustawiając title, type i własne pole sku:
    //     ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    //     problem.setTitle("Produkt nie istnieje");
    //     problem.setProperty("sku", e.getSku());
    //     return problem;
    // Spring odpowiada statusem 404 i typem application/problem+json — nie trzeba ResponseEntity ani @ResponseStatus.
    // Pole instance zostaje uzupełnione ścieżką zapytania (test #1), a własne pole sku jest na wierzchu JSON-a, nie w
    // obiekcie "properties" (test #2).
    //
    // DOBRA PRAKTYKA: title ma być STAŁY dla rodzaju problemu, a detail opisuje to konkretne zdarzenie. Dlaczego? Klient
    // (albo log, albo alert) może grupować błędy po type/title, a użytkownikowi pokazać detail. Gdy title zawiera numer
    // zamówienia, każdy błąd wygląda jak inny rodzaj problemu.
    //
    // DOBRA PRAKTYKA: nie wkładaj do detail ani do własnych pól wyjątku, stosu, SQL ani nazw tabel. Dlaczego? Ciało
    // ProblemDetail jest publiczne tak samo jak każda inna odpowiedź (lekcja Err01) — to samo ryzyko wycieku.

    // =================================================================================================
    // 2. ErrorResponseException — WYJĄTEK Z GOTOWYM PROBLEMDETAIL (testy: Err04ProblemDetailTest #3, #10)
    // =================================================================================================
    //
    // OutOfStockException dziedziczy po ErrorResponseException i w konstruktorze składa ProblemDetail (status 409, title,
    // type, pola sku i available). Rzucasz ją w kontrolerze i nie piszesz żadnego handlera:
    //     POST /api/products/ELE-001/reserve?quantity=10 → 409 application/problem+json z polami sku=ELE-001, available=7
    // Działa zarówno z naszą poradą, jak i z wbudowaną poradą Boota (test Err04BuiltInProblemDetailsTest #3). Bez żadnej
    // porady ten sam wyjątek zachowuje status, ale TRACI ciało — klient dostaje stary JSON z /error (Err04ProblemDetailsDisabledTest #2).
    //
    // PUŁAPKA: wyjątek domeny dziedziczący po ErrorResponseException wiąże domenę z HTTP (zna status i kształt odpowiedzi).
    //   Dlaczego to ważne? Taka klasa nie nadaje się do logiki, która nie jest wołana przez HTTP (zadanie w tle, import
    //   pliku). Dla małych API to wygodne; dla większych lepszy czysty wyjątek + jedna porada (ProductNotFoundException).

    // =================================================================================================
    // 3. ResponseEntityExceptionHandler JAKO BAZA PORADY (testy: Err04ProblemDetailTest #4, #5, #7)
    // =================================================================================================
    //
    // ProblemAdvice extends ResponseEntityExceptionHandler. Z klasy bazowej dostajemy za darmo (prawdziwe odpowiedzi):
    //     DELETE na adres tylko-GET → 405 {"detail":"Method 'DELETE' is not supported.","instance":...,"status":405,
    //                                       "title":"Method Not Allowed"}  + nagłówek Allow: GET
    //     brak wymaganego parametru → 400 {"detail":"Required parameter 'quantity' is not present.",...}
    //     Content-Type: text/plain na punkcie JSON → 415
    //     nieznany adres → 404 {"detail":"No static resource api/nic.",...}
    // Wszystkie jako application/problem+json. Dodajemy tylko to, co własne: handler wyjątku domeny, nadpisany
    // handleMethodArgumentNotValid (dopisuje mapę errors: pole → komunikat) i handler na Exception jako siatkę
    // bezpieczeństwa (500 bez szczegółów, szczegóły w logu).
    //
    // To rozwiązuje PUŁAPKĘ z Err03ControllerAdvice: handler na Exception w klasie dziedziczącej po
    // ResponseEntityExceptionHandler NIE zamienia 405 w 500. Dlaczego? Wewnątrz jednej klasy wygrywa najbardziej szczegółowy
    // handler, a klasa bazowa ma osobne handlery dla wyjątków Spring MVC — Exception.class dostaje tylko resztę (test #7).
    //
    // PUŁAPKA: dziedziczysz po ResponseEntityExceptionHandler, a w aplikacji jest jeszcze druga taka klasa (albo włączony
    //   wbudowany ProblemDetailsExceptionHandler). Dlaczego to kłopot? Dla wyjątku Springa odpowiada pierwsza porada
    //   w kolejności @Order (lekcja Err03) — druga bywa ignorowana, a wynik zależy od kolejności. Trzymaj JEDNĄ poradę
    //   dziedziczącą po tej klasie (Boot sam się wycofuje, gdy zobaczy twoją — Err04OwnAdviceWinsTest).

    // =================================================================================================
    // 4. spring.mvc.problemdetails.enabled (testy: Err04BuiltInProblemDetailsTest, Err04ProblemDetailsDisabledTest)
    // =================================================================================================
    //
    // Domyślnie (false) wyjątki Spring MVC kończą jako stary JSON z /error: {"timestamp","status","error","path"}
    // (test Err04ProblemDetailsDisabledTest #2). Po ustawieniu spring.mvc.problemdetails.enabled=true bez własnej porady
    // dostajesz problem+json dla wyjątków Spring MVC i dla ErrorResponseException (test BuiltIn #2, #3).
    //
    // PUŁAPKA: włączenie właściwości NIE zamienia w problem+json wszystkich błędów. Nasz zwykły wyjątek domeny
    //   (ProductNotFoundException) i każdy inny nieznany wyjątek nadal kończą jako 500 z JSON-em z /error — test BuiltIn #4.
    //   Dlaczego? Wbudowana porada zna tylko wyjątki Springa i ErrorResponse. Resztę musisz obsłużyć sam (własna porada).
    //
    // PUŁAPKA: komunikaty ProblemDetail dla wyjątków Springa są po angielsku („Method 'DELETE' is not supported.”). Chcesz
    //   polskie — użyj MessageSource (punkt 5), a nie nadpisywania handlerów jeden po drugim.

    // =================================================================================================
    // 5. KOMUNIKATY Z MessageSource (test: Err04ProblemDetailTest #8, #9)
    // =================================================================================================
    //
    // ResponseEntityExceptionHandler pyta MessageSource o tekst dla każdego wyjątku, który jest ErrorResponse. Klucze:
    //     problemDetail.<pełna nazwa klasy wyjątku>          → pole detail (z argumentami {0}, {1}...)
    //     problemDetail.title.<pełna nazwa klasy wyjątku>    → pole title
    // W pliku err04_messages.properties (UTF-8, ustawia to ProblemMessagesConfig):
    //     problemDetail.org.springframework.web.HttpRequestMethodNotSupportedException=Metoda {0} nie jest obsługiwana ... Dozwolone: {1}.
    // Dla DELETE na adres tylko-GET powstaje:  "Metoda DELETE nie jest obsługiwana dla tego adresu. Dozwolone: [GET]."
    // Język wybiera nagłówek Accept-Language (err04_messages_en.properties dla "en"). Klucz znaleziony w pliku NADPISUJE
    // tekst ustawiony w wyjątku — dotyczy to także title naszego OutOfStockException (test #9).
    //
    // PUŁAPKA: bez setFallbackToSystemLocale(false) dla języka, którego nie ma w plikach (np. "de"), Spring spróbuje
    //   najpierw języka SYSTEMU. Dlaczego to kłopot? Na Twoim komputerze (pl) dostaniesz polski plik, na serwerze (en) —
    //   angielski, a test raz przejdzie, raz nie. U nas fallback jest wyłączony, więc "de" daje plik domyślny (test #8).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • RFC 9457: Content-Type application/problem+json; pola type, title, status, detail, instance + własne (na wierzchu JSON-a).
     *   • Handler może zwrócić ProblemDetail (status z pola status); ProblemDetail.forStatusAndDetail(status, opis).
     *   • ErrorResponseException = wyjątek z wbudowanym ProblemDetail; ResponseStatusException to jej podklasa.
     *   • ResponseEntityExceptionHandler (klasa bazowa porady) obsługuje wyjątki Spring MVC; nadpisuj handleXxx, żeby dodać pola.
     *   • spring.mvc.problemdetails.enabled=true (domyślnie false) → Boot rejestruje ProblemDetailsExceptionHandler, o ile
     *     nie ma twojego ResponseEntityExceptionHandler. Nie obejmuje własnych wyjątków domeny.
     *   • Teksty: MessageSource, klucze problemDetail.<klasa> i problemDetail.title.<klasa>, język z Accept-Language.
     *   • Do ProblemDetail nie wkładaj stosu ani SQL-a.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie pola ma ProblemDetail według RFC 9457 i co znaczy każde z nich? Czym różni się title od detail?
     *   2. Co się stanie? Aplikacja ma włączone spring.mvc.problemdetails.enabled=true i nie ma własnej porady. Kontroler rzuca
     *      zwykły {@code ProductNotFoundException extends RuntimeException}. Jakiego typu treści użyje odpowiedź?
     *   3. Co się stanie? W aplikacji jest ProblemAdvice dziedziczący po ResponseEntityExceptionHandler i dodatkowo ustawiono
     *      spring.mvc.problemdetails.enabled=true. Ile porad typu ResponseEntityExceptionHandler będzie w kontekście?
     *   4. ZNAJDŹ BŁĄD: programista ustawia w handlerze {@code problem.setProperty("errors", errors)} i oczekuje w JSON-ie
     *      zagnieżdżenia {"properties":{"errors":...}}. Co zobaczy naprawdę?
     *   5. ZNAJDŹ BŁĄD: handler wyjątku domeny zwraca {@code ResponseEntity<ProblemDetail>} ze statusem 409, ale w polu status
     *      ProblemDetail zostało 500 (nie ustawiono go). Który status jest „prawdziwy” dla klienta, a który tylko w ciele?
     *   6. Dlaczego handler na {@code Exception.class} w klasie dziedziczącej po ResponseEntityExceptionHandler nie zamienia 405 w 500?
     *   7. Jak sprawić, żeby komunikat 405 był po polsku i po angielsku zależnie od zapytania?
     *   8. Dlaczego w testach tekstów MessageSource wyłączamy powrót do języka systemu (fallbackToSystemLocale)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Err04ProblemDetailExercisesTest (rozwiązania: solutions.err04_problem_detail)
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): czy podany typ treści to „problem”? Prawda dla "application/problem+json" i "application/problem+xml",
     * także z parametrem (np. "application/problem+json;charset=UTF-8") i niezależnie od wielkości liter. Fałsz dla
     * "application/json", "text/plain" i null.
     * Podpowiedź: toLowerCase(), startsWith (t04_strings/Strings01Basics).
     */
    static boolean isProblemMediaType(String contentType) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zbuduj ciało problemu jako mapę w kolejności: "status", "title", "detail", "instance" — pomijając
     * wartości null (status jest zawsze) — a po nich pola własne z {@code extensions} (w ich kolejności). Pole własne o nazwie
     * zarezerwowanej (type, title, status, detail, instance) ignoruj, standard nie pozwala ich nadpisać.
     * Przykład: (404, "Brak", null, "/x", {sku=ELE-001, title=zly}) → {status=404, title=Brak, instance=/x, sku=ELE-001}.
     * Podpowiedź: LinkedHashMap i sprawdzanie null przed put (t12_collections/Collections05Maps).
     */
    static Map<String, Object> problemBody(int status, String title, String detail, String instance, Map<String, Object> extensions) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): odtwórz wyszukiwanie komunikatu jak w MessageSource. {@code bundles} to mapa „język → (klucz →
     * tekst)”; język "" to plik domyślny. Szukaj klucza w pliku danego języka, a gdy go tam nie ma (albo nie ma takiego języka) —
     * w pliku domyślnym; gdy nigdzie — zwróć null. Znaleziony tekst sformatuj argumentami: zastąp {0}, {1}... kolejnymi
     * elementami {@code args} (tekst bez {0} zostaje bez zmian).
     * Przykład: bundles = {""→{k="Metoda {0}"}, "en"→{}}, język "en", klucz k, args "DELETE" → "Metoda DELETE" (z pliku domyślnego).
     * Podpowiedź: String.replace dla każdego indeksu albo MessageFormat (uwaga: apostrofy w MessageFormat są znakami specjalnymi).
     */
    static String resolveMessage(Map<String, Map<String, String>> bundles, String language, String key, Object... args) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. type — URI rodzaju problemu (domyślnie about:blank); title — krótki, stały tytuł rodzaju; status — kod HTTP;
     *      detail — opis tego konkretnego zdarzenia; instance — identyfikator/adres zdarzenia (Spring wpisuje ścieżkę
     *      zapytania). title jest taki sam dla wszystkich błędów danego rodzaju, detail różni się między zdarzeniami.
     *   2. Zwykły application/json (stary kształt z /error: timestamp, status, error, path) i status 500. Wbudowana porada
     *      obsługuje wyjątki Springa i ErrorResponse, a nie nasze wyjątki domeny. Sprawdzone: Err04BuiltInProblemDetailsTest #4.
     *   3. Jedna — tylko ProblemAdvice. Wbudowana porada ma @ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)
     *      i wycofuje się. Sprawdzone: Err04OwnAdviceWinsTest.
     *   4. Pole errors na wierzchu JSON-a: {"detail":...,"errors":{...},...}. Mixin Jacksona (ProblemDetailJacksonMixin)
     *      „spłaszcza” własne pola. Sprawdzone: Err04ProblemDetailTest #2 (jsonPath $.properties nie istnieje) i #4.
     *   5. Dla klienta liczy się status HTTP odpowiedzi (409) — to on trafia do linii statusu. Status w ciele to tylko
     *      kopia informacji; rozjazd między nimi wprowadza w błąd, więc ustaw oba tak samo albo zwracaj samego ProblemDetail
     *      z poprawnym polem status (Spring weźmie status z niego). Sprawdzone: Err04StatusMismatchTest.
     *   6. W jednej klasie wygrywa najbardziej szczegółowy handler, a ResponseEntityExceptionHandler ma własne handlery
     *      dla wyjątków Spring MVC; Exception.class dostaje tylko wyjątki, których klasa bazowa nie zna.
     *      Sprawdzone: Err04ProblemDetailTest #5 i #7.
     *   7. Pliki komunikatów (err04_messages.properties i err04_messages_en.properties) z kluczem
     *      problemDetail.org.springframework.web.HttpRequestMethodNotSupportedException i bean MessageSource; język wybiera
     *      nagłówek Accept-Language. Sprawdzone: Err04ProblemDetailTest #8.
     *   8. Bez tego dla nieobsługiwanego języka Spring sięga po język systemu, więc wynik zależałby od komputera,
     *      na którym biegnie test (polski na Twoim, angielski na serwerze CI).
     */
    // </editor-fold>
}
