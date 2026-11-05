package s06_error_handling.err01_default_errors;

/**
 * <pre>
 * TEMAT: Domyślna obsługa błędów w Spring Boot — kontroler błędów /error, kształt odpowiedzi i ustawienia spring.web.error.*
 *        (error = błąd; default = domyślny; exception = wyjątek; status code = kod statusu HTTP; response body = ciało
 *         odpowiedzi; whitelabel = "bez marki", neutralna strona awaryjna; stack trace = ślad stosu, lista wywołań metod
 *         w chwili wyjątku; leak = wyciek)
 *
 * W SKRÓCIE:
 *   Gdy z kontrolera wyleci wyjątek, którego nikt nie obsłużył, Spring Boot nie zostawia klienta z niczym. Odpowiedź
 *   dostaje kod 500, a ciało (JSON dla klienta API, stronę HTML dla przeglądarki) buduje wbudowany kontroler błędów
 *   {@code BasicErrorController} pod adresem {@code /error}. To samo dzieje się z nieznanym adresem (404) i z wyjątkami
 *   oznaczonymi kodem ({@code @ResponseStatus}, {@code ResponseStatusException}). Domyślnie ciało jest ubogie i
 *   bezpieczne: {@code timestamp}, {@code status}, {@code error}, {@code path} — bez komunikatu wyjątku i bez śladu stosu.
 *   Można to zmienić właściwościami {@code spring.web.error.*} (w Boot 4; w Boot 3 nazywały się {@code server.error.*}),
 *   ale rzadko należy — lepiej obsłużyć błędy samemu (Err02ExceptionHandler, Err03ControllerAdvice).
 *
 * ANALOGIA: dyżurny na bramie fabryki.
 *   Pracownik (metoda kontrolera) zgłasza awarię, ale nikt z hali jej nie obsłużył. Wtedy kończy się procedura "na hali" i
 *   wchodzi dyżurny z bramy ({@code /error}). Dyżurny nie wie, co dokładnie się zepsuło, i nie zdradza tego gościom.
 *   Wydaje standardową kartkę: "godzina, numer zdarzenia (status), rodzaj (error), miejsce (path)". Przeglądarce wręcza
 *   kolorową ulotkę (Whitelabel), a programowi — krótką kartkę w formacie JSON. Szczegóły techniczne (kto, gdzie, co
 *   się zepsuło) trzyma w swoim dzienniku (logi serwera), nie na kartce dla gości.
 *
 * JAK TO DZIAŁA:
 *   1. Kontroler rzuca wyjątek (np. {@code IllegalStateException}). Spring MVC szuka dla niego obsługi: {@code @ExceptionHandler}
 *      (Err02, Err03), {@code @ResponseStatus}, {@code ResponseStatusException}. Dla zwykłego wyjątku nie ma nic.
 *   2. Wyjątek wylatuje z {@code DispatcherServlet} do kontenera serwletów (Tomcat). Przy okazji {@code DefaultErrorAttributes}
 *      (bean typu {@code ErrorAttributes}, pełni też rolę {@code HandlerExceptionResolver}) zapamiętuje wyjątek w żądaniu.
 *   3. Dla wyjątków oznaczonych kodem ({@code @ResponseStatus}, {@code ResponseStatusException}) i dla błędów 4xx z samego
 *      Springa (np. 404 dla nieznanej ścieżki) Spring woła {@code response.sendError(status)}. Tomcat uznaje odpowiedź za błąd.
 *   4. Boot (klasa {@code ErrorMvcAutoConfiguration}) zarejestrował w Tomcacie "stronę błędu" pod adresem {@code /error}. Tomcat
 *      wykonuje wewnętrzne przekierowanie (dispatch typu ERROR) na ten adres — tym samym {@code DispatcherServlet}em.
 *   5. Obsługuje je {@code BasicErrorController}: metoda {@code error()} zwraca JSON (mapę), a {@code errorHtml()} stronę HTML.
 *      Wybór zależy od nagłówka {@code Accept} zapytania (przeglądarka wysyła {@code text/html}, klient API zwykle
 *      {@code application/json}).
 *   6. Mapę pól tworzy {@code ErrorAttributes} (domyślnie {@code DefaultErrorAttributes}); to, które pola dołączyć, ustalają
 *      właściwości {@code spring.web.error.include-*}. Kod statusu zostaje ten z kroku 3 (dla niezłapanego wyjątku: 500).
 *   7. Brakuje szablonu HTML o nazwie {@code error}? Boot pokazuje własną stronę "Whitelabel Error Page"
 *      (wyłączysz: {@code spring.web.error.whitelabel.enabled=false}).
 *
 * SŁÓWKA:
 *   BasicErrorController = podstawowy kontroler błędów; ErrorAttributes = atrybuty błędu (mapa pól); whitelabel = strona
 *   "bez marki"; include = dołącz; binding errors = błędy wiązania danych (np. walidacji); on_param = tylko na życzenie,
 *   gdy zapytanie ma odpowiedni parametr; conflict = konflikt (kod 409); dispatch = przekazanie żądania dalej wewnątrz
 *   serwera; fallback = rozwiązanie awaryjne, zapasowe.
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest03Responses (ResponseStatusException, @ResponseStatus, kody statusu),
 *             s05_validation/Val02ValidInControllers (błąd walidacji = 400 i ogólny JSON z /error),
 *             s06_error_handling/Err02ExceptionHandler (własna obsługa wyjątku w kontrolerze),
 *             s06_error_handling/Err03ControllerAdvice (jedna obsługa dla wszystkich kontrolerów),
 *             s06_error_handling/Err04ProblemDetail (standardowy format błędu, rozwiązanie docelowe),
 *             t10_exceptions/Exceptions01Basics (wyjątki w Javie: rzucanie, łapanie, propagacja).
 * </pre>
 */
public final class Err01DefaultErrors {

    private Err01DefaultErrors() {
    }

    // =================================================================================================
    // 1. WYJĄTEK BEZ OBSŁUGI = 500; NIEZNANA ŚCIEŻKA = 404 (ShopErrorsController.boom, Err01DefaultErrorsTest)
    // =================================================================================================
    //
    //     @GetMapping("/boom")
    //     public String boom() { throw new IllegalStateException("Błąd wewnętrzny: połączenie z bazą db-prod-01 odrzucone"); }
    //
    // Na prawdziwym serwerze klient dostaje (test unhandledExceptionIs500):
    //
    //     HTTP 500, Content-Type: application/json
    //     {"timestamp":"2026-11-04T10:15:26.840Z","status":500,"error":"Internal Server Error","path":"/err01/boom"}
    //
    // Cztery pola — i tylko one. Co znaczą:
    //   • {@code timestamp} — chwila błędu (tekst w formacie ISO 8601, strefa UTC);
    //   • {@code status}    — liczbowy kod HTTP (to samo, co kod odpowiedzi);
    //   • {@code error}     — nazwa kodu po angielsku ("Internal Server Error", "Not Found"…);
    //   • {@code path}      — adres, którego dotyczył błąd.
    // Nie ma ani komunikatu wyjątku ({@code message}), ani nazwy klasy wyjątku, ani śladu stosu. Test sprawdza nawet,
    // że "db-prod-01" nie ma w odpowiedzi — klient nie dowie się, jak nazywa się Twój serwer bazy.
    //
    // Nieznana ścieżka (test unknownPathIs404) przechodzi przez TĘ SAMĄ drogę: Spring MVC nie znajduje punktu końcowego
    // (w Spring 7 rzuca {@code NoResourceFoundException}), kończy to kodem 404, a /error buduje identyczny JSON
    // ({@code "status":404,"error":"Not Found"}). Dzięki temu klient API zawsze dostaje ten sam kształt błędu.
    //
    // Dwie rzeczy dzieją się po stronie SERWERA, nie klienta: wyjątek 500 jest zapisywany w logu razem ze śladem stosu
    // (znajdziesz go w konsoli po uruchomieniu testów) — to tam programista szuka przyczyny.
    //
    // Boot 4: klasy zmieniły pakiety. {@code ErrorAttributes} to teraz {@code org.springframework.boot.webmvc.error.ErrorAttributes},
    // a {@code BasicErrorController} to {@code org.springframework.boot.webmvc.autoconfigure.error.BasicErrorController}
    // (w Boot 3: {@code org.springframework.boot.web.servlet.error.*} i {@code org.springframework.boot.autoconfigure.web.servlet.error.*}).
    // Stare importy z tutoriali przestaną się kompilować.
    //
    // PUŁAPKA: uznanie, że 500 z domyślnej obsługi wystarcza w produkcji. Dlaczego? 500 mówi klientowi tylko "coś padło po
    //   stronie serwera" — nie odróżnia błędu danych od awarii, więc klient nie wie, czy ponawiać zapytanie. Kody 4xx
    //   (błąd klienta) i 5xx (błąd serwera) mają różne znaczenie, a wyjątek domenowy ("brak towaru") nie jest awarią.
    //   Własna obsługa daje właściwy kod (Err02, Err03, Err05).
    //
    // DOBRA PRAKTYKA: zostaw domyślną obsługę jako siatkę bezpieczeństwa i NIE polegaj na niej w zwykłych sytuacjach.
    //   Dlaczego? Zawsze zdarzy się wyjątek, którego nie przewidziałeś — wtedy chcesz, żeby klient dostał uprzejme 500
    //   bez szczegółów, a Ty znalazł pełny ślad w logu.

    // =================================================================================================
    // 2. SERWER NIE UJAWNIA SZCZEGÓŁÓW — spring.web.error.include-* (Err01IncludeTest, Err01OldNamesTest)
    // =================================================================================================
    //
    // Pola dodatkowe są domyślnie wyłączone. Włączasz je właściwościami (w teście: {@code @SpringBootTest(properties = …)}):
    //
    //     spring.web.error.include-message      = never (domyślnie) | always | on_param
    //     spring.web.error.include-binding-errors = never (domyślnie) | always | on_param   (lista błędów walidacji, pole "errors")
    //     spring.web.error.include-stacktrace   = never (domyślnie) | always | on_param   (pole "trace")
    //     spring.web.error.include-exception    = false (domyślnie) | true                (pole "exception": nazwa klasy wyjątku)
    //     spring.web.error.include-path         = always (domyślnie) | never | on_param
    //
    // Znaczenie wartości: {@code never} = nigdy; {@code always} = zawsze; {@code on_param} = tylko, gdy zapytanie ma parametr
    // o nazwie takiej jak pole ({@code ?message=true}, {@code ?trace=true}, {@code ?errors=true}).
    //
    // Co zobaczysz z {@code include-message=always, include-exception=true, include-stacktrace=on_param} (test Err01IncludeTest):
    //   • {@code message}   — dokładnie tekst z konstruktora wyjątku: "Błąd wewnętrzny: połączenie z bazą db-prod-01 odrzucone";
    //   • {@code exception} — "java.lang.IllegalStateException";
    //   • {@code trace}     — dopiero przy {@code /err01/boom?trace=true}: pełna lista metod, z nazwami Twoich klas i numerami linii.
    // Dla {@code ResponseStatusException} polem {@code message} jest jej opis (reason): "Nie ma produktu o id 9".
    //
    // ZMIANA W BOOT 4: te właściwości zmieniły przedrostek z {@code server.error.*} na {@code spring.web.error.*}
    // (w metadanych Boota stare nazwy mają poziom "error", zamiennik podany wprost). Co ciekawe, aplikacja z
    // starą nazwą NADAL SIĘ URUCHAMIA — właściwość jest po prostu ignorowana. Test Err01OldNamesTest pokazuje, że po
    // wpisaniu {@code server.error.include-message=always} w odpowiedzi dalej nie ma ani {@code message}, ani {@code trace}.
    // To zdradliwe: kopiujesz konfigurację z tutorialu, nic nie działa, a żaden błąd nie informuje, dlaczego.
    //
    // PUŁAPKA: {@code spring.web.error.include-stacktrace=always} (albo {@code include-message=always}) w konfiguracji
    //   produkcyjnej. Dlaczego? To wyciek informacji: ślad stosu zdradza nazwy pakietów i klas, wersje bibliotek, numery
    //   linii i ścieżki — atakującemu to gotowa mapa Twojej aplikacji. Komunikaty wyjątków potrafią zawierać nazwy
    //   serwerów, fragmenty SQL i dane użytkowników. Do pracy lokalnej wygodne, na serwerze niedopuszczalne.
    //
    // DOBRA PRAKTYKA: włączaj te opcje wyłącznie w profilu deweloperskim (Cfg03Profiles: {@code application-dev.properties}), a
    //   jeśli klient ma dostać komunikat, napisz go sam w obsłudze błędu (Err02/Err03), pod kontrolą, zamiast wystawiać
    //   komunikaty wyjątków. Dlaczego? Własny komunikat wybierasz świadomie ("Brak towaru: kawa"), a komunikat wyjątku
    //   napisał ktoś inny (biblioteka, sterownik bazy) i nie wiesz, co zawiera.

    // =================================================================================================
    // 3. KOD STATUSU Z WYJĄTKU: @ResponseStatus I ResponseStatusException (Rest03Responses)
    // =================================================================================================
    //
    // Dwa wbudowane sposoby, by wyjątek miał "swój" kod zamiast 500:
    //
    //   a) adnotacja na klasie wyjątku (OutOfStockException):
    //          @ResponseStatus(HttpStatus.CONFLICT)
    //          public class OutOfStockException extends RuntimeException { … }
    //      — wszędzie, gdzie ten wyjątek wyleci, klient dostanie 409;
    //   b) wyjątek rzucany w miejscu, z kodem i opisem (ShopErrorsController.product):
    //          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma produktu o id " + id);
    //
    // Oba są przekazywane przez {@code /error}, więc klient widzi taki sam JSON jak przy 500, tylko z innym statusem
    // (testy responseStatusOnException i responseStatusException): {@code "status":409,"error":"Conflict"} oraz
    // {@code "status":404,"error":"Not Found"}.
    //
    // Co ważne: opis wyjątku ("Brak towaru: kawa", "Nie ma produktu o id 9") NIE trafia do ciała — domyślnie
    // {@code include-message=never}. Klient widzi suchy "Conflict" bez wyjaśnienia. Dlatego te dwa mechanizmy to
    // "szybka" obsługa na start (lub dla prostych przypadków), a nie docelowa: nie pozwalają zbudować własnego ciała
    // (np. z kodem błędu biznesowego), a {@code @ResponseStatus} na klasie wiąże wyjątek domenowy z HTTP (sklepowa klasa
    // "brak towaru" nie powinna wiedzieć o kodzie 409!). Docelowo: Err02, Err03, Err05.
    //
    // PYTANIE REKRUTACYJNE: czym różni się {@code @ResponseStatus} na wyjątku od {@code ResponseStatusException}?
    //   ({@code @ResponseStatus} jest stałe dla klasy i nie da się go zmienić w locie; wyjątek domenowy musi znać HTTP.
    //   {@code ResponseStatusException} niesie kod i opis ustalane w miejscu rzucenia, jest klasą Springa — wygodna w
    //   kontrolerze, ale wiąże warstwę web z logiką, jeśli rzucisz ją z serwisu.)
    //
    // PUŁAPKA: oczekiwanie, że opis z {@code ResponseStatusException} sam trafi do klienta. Dlaczego? Domyślnie go nie ma w
    //   ciele (test sprawdza, że w odpowiedzi nie ma tekstu "Nie ma produktu"). Programista klienta widzi sam kod 404 i
    //   zgaduje, czego brakuje. Rozwiązanie: własny handler (Err02) albo ProblemDetail (Err04), nie {@code include-message=always}.

    // =================================================================================================
    // 4. MockMvc KONTRA PRAWDZIWY SERWER (Err01MockMvcTest)
    // =================================================================================================
    //
    // {@code /error} to część KONTENERA SERWLETÓW: robi go Tomcat, przekierowując odpowiedź błędu na adres {@code /error}.
    // MockMvc nie uruchamia Tomcata — woła {@code DispatcherServlet} bezpośrednio i nikt takiego przekierowania nie wykonuje.
    // Skutek (test Err01MockMvcTest):
    //   • wyjątek bez obsługi NIE staje się statusem 500, tylko wylatuje z {@code mockMvc.perform(...)} jako
    //     {@code jakarta.servlet.ServletException} (przyczyna: Twój wyjątek). Test, który oczekuje {@code status().isInternalServerError()},
    //     w ogóle nie dojdzie do asercji;
    //   • {@code ResponseStatusException} / {@code @ResponseStatus} / nieznana ścieżka dają właściwy STATUS (404, 409), ale
    //     PUSTE ciało. Opis błędu jest dostępny tylko w obiekcie odpowiedzi: {@code response.getErrorMessage()}.
    //
    // Z tego wynika podział testów:
    //   • Własną obsługę błędów (@ExceptionHandler, @RestControllerAdvice — Err02, Err03) testuj w MockMvc
    //     ({@code @WebMvcTest}): to szybkie testy, a Twój handler sam buduje ciało, więc MockMvc je zobaczy.
    //   • Domyślną obsługę (/error: kształt JSON, Whitelabel, ustawienia include-*) testuj tylko na prawdziwym serwerze
    //     ({@code @SpringBootTest(webEnvironment = RANDOM_PORT)} + {@code RestClient}, jak w Err01DefaultErrorsTest).
    //
    // PUŁAPKA: test w MockMvc, który sprawdza {@code jsonPath("$.error")} dla błędu z /error. Dlaczego? Przejdzie na
    //   zielono tylko jeśli ktoś naprawdę zbudował takie ciało w kontrolerze; dla domyślnego 404/500 ciało jest puste i
    //   test padnie ("No value at JSON path"). Zła diagnoza: "mój kod nie działa" — a to MockMvc nie zna /error.
    //
    // DOBRA PRAKTYKA: wybieraj najwęższy test, który widzi to, co chcesz sprawdzić. Dlaczego? @WebMvcTest startuje w
    //   ułamku sekundy i wystarcza dla własnych handlerów; test z serwerem (RANDOM_PORT) jest wolniejszy — zostaw go
    //   na kilka scenariuszy, które muszą przejść przez Tomcata.

    // =================================================================================================
    // 5. PRZEGLĄDARKA KONTRA KLIENT API: Accept, STRONA WHITELABEL (Err01DefaultErrorsTest, Err01WhitelabelOffTest)
    // =================================================================================================
    //
    // Ten sam błąd, inne ciało — zależnie od nagłówka {@code Accept}:
    //   • {@code Accept: application/json} (albo brak) → JSON z sekcji 1;
    //   • {@code Accept: text/html} (tak pytają przeglądarki) → HTML:
    //       <h1>Whitelabel Error Page</h1> … "There was an unexpected error (type=Internal Server Error, status=500)."
    //     (test browserGetsWhitelabelPage; komunikat wyjątku też nie trafia na stronę — ta sama zasada co w JSON-ie).
    //
    // Skąd się bierze ta strona? Boot sprawdza, czy aplikacja ma własny widok o nazwie {@code error} (np. szablon
    // {@code error.html} w {@code src/main/resources/templates/}). Nie ma? Wstawia stronę awaryjną "Whitelabel". W czysto
    // REST-owym API jej nie widać, dopóki ktoś nie otworzy adresu w przeglądarce.
    //
    // Wyłączenie: {@code spring.web.error.whitelabel.enabled=false} (test Err01WhitelabelOffTest). Efekt: przeglądarka dostaje
    // wtedy stronę błędu samego Tomcata ("HTTP Status 500 – Internal Server Error"), a klient JSON — niezmieniony JSON.
    // Uwaga: wyłączenie Whitelabel nie wyłącza {@code /error}, tylko ładną stronę HTML.
    //
    // Własny kształt ciała bez pisania handlerów: można podać własny bean typu {@code ErrorAttributes} (zwykle dziedziczy
    // po {@code DefaultErrorAttributes} i dokłada pola). To działa, ale tylko dla błędów przechodzących przez /error,
    // dlatego w tym kursie idziemy inną drogą: własny handler + ProblemDetail (Err03, Err04).
    //
    // DOBRA PRAKTYKA: w API, które ma tylko klientów programistycznych, nie przejmuj się stroną Whitelabel, ale zadbaj, by
    //   klienci wysyłali {@code Accept: application/json}. Dlaczego? Wtedy zawsze dostaną JSON, nawet jeśli ktoś doda do
    //   projektu szablony HTML.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Wyjątek bez obsługi → 500; nieznana ścieżka → 404; oba przez {@code /error} ({@code BasicErrorController}).
     *   • Domyślne ciało (JSON): {@code timestamp, status, error, path}. Bez message, exception i trace.
     *   • Pola opcjonalne: {@code spring.web.error.include-message | include-binding-errors | include-stacktrace}
     *     (never | always | on_param), {@code include-exception} (true/false). W Boot 3 przedrostek był {@code server.error.*} —
     *     w Boot 4 stara nazwa jest po cichu ignorowana!
     *   • Stack trace i komunikaty na produkcji = wyciek informacji. Włączaj tylko w profilu dev.
     *   • {@code @ResponseStatus} (na klasie wyjątku) i {@code ResponseStatusException} (kod + opis) zmieniają status,
     *     ale opis domyślnie nie trafia do ciała.
     *   • MockMvc nie zna {@code /error}: nieobsłużony wyjątek wylatuje jako ServletException, a 404/409 mają puste ciało.
     *   • Accept: text/html → strona Whitelabel; Accept: application/json → JSON. {@code spring.web.error.whitelabel.enabled=false}.
     *   • Własne ciało błędu: @ExceptionHandler (Err02), @RestControllerAdvice (Err03), ProblemDetail (Err04).
     *
     * PYTANIA KONTROLNE:
     *   1. Którędy trafia wyjątek z kontrolera do adresu {@code /error}? Kto buduje tam ciało odpowiedzi i skąd wie,
     *      czy ma zwrócić JSON, czy HTML?
     *   2. Jakie cztery pola ma domyślny JSON błędu w Spring Boot? Które pola dodają ustawienia include-*?
     *   3. Co się stanie? W aplikacji na Boot 4 wpisujesz {@code server.error.include-message=always}, a w odpowiedzi
     *      500 nadal nie ma pola {@code message}. Czy aplikacja się uruchomi i dlaczego nic nie widać?
     *   4. ZNAJDŹ BŁĄD: zespół ustawił w {@code application.properties} (użytym też na produkcji)
     *      {@code spring.web.error.include-stacktrace=always}, "żeby łatwiej szukać błędów". Co jest nie tak?
     *   5. Co się stanie? Test w {@code @WebMvcTest} wywołuje {@code get("/err01/boom")} i oczekuje
     *      {@code status().isInternalServerError()}. Jaki będzie wynik testu?
     *   6. Co się stanie? Kontroler rzuca {@code new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma produktu o id 9")}.
     *      Jaki status i jakie pola ma ciało na prawdziwym serwerze? Czy klient zobaczy tekst "Nie ma produktu o id 9"?
     *   7. ZNAJDŹ BŁĄD: test w MockMvc dla nieznanej ścieżki zawiera {@code .andExpect(jsonPath("$.error").value("Not Found"))}
     *      i pada. Dlaczego i jak go poprawić?
     *   8. Przeglądarka i skrypt {@code curl -H "Accept: application/json"} otwierają ten sam adres, który kończy się błędem 500.
     *      Co dostaje każdy z nich?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielet: ExerciseController, testy: Err01DefaultErrorsExercisesTest (@Tag("cwiczenie"))
    // =================================================================================================
    //
    // Rozwiązania wzorcowe: Err01SolutionsController (s06_error_handling.solutions.err01_default_errors).
    // Wszystkie punkty końcowe mają prefiks /err01/cw. Mapowania są gotowe.
    //
    // ĆWICZENIE 1 (łatwe): order — dla id 1 i 2 zwróć "Zamówienie #<id>"; dla innych rzuć ResponseStatusException z kodem 404
    //   i opisem "Nie ma zamówienia <id>". Podpowiedź: new ResponseStatusException(HttpStatus.NOT_FOUND, "…").
    //
    // ĆWICZENIE 2 (łatwe): stock — dodaj {@code @ResponseStatus(HttpStatus.CONFLICT)} do klasy NoStockException i rzuć ją dla
    //   sku "kawa"; dla innych zwróć "Jest: <sku>". Podpowiedź: adnotacja stoi nad klasą wyjątku, nie nad metodą kontrolera.
    //
    // ĆWICZENIE 3 (średnie): discount — procent od 0 do 100 → "Rabat: <percent>%"; poza zakresem → 400 z opisem
    //   "Rabat musi być w przedziale 0-100". Podpowiedź: sprawdź obie granice (≥ 0 i ≤ 100 włącznie).
    //
    // ĆWICZENIE 4 (średnie): summarize — zamień mapę z ciałem błędu na jedną linię "<status> <error> (<path>)", a gdy jest
    //   niepuste pole message — dopisz ": <message>". Podpowiedź: errorBody.get("path") zwraca Object; message może nie istnieć.
    //
    // ĆWICZENIE 5 (trudniejsze): leakedKeys — audyt wycieków: zwróć posortowaną listę kluczy trace, exception, errors, które
    //   występują w mapie, oraz message, ale tylko gdy jego tekst zawiera "db-" albo "SELECT". Podpowiedź: strumień po
    //   errorBody.entrySet(), sorted(), toList(); wartość message zamień na tekst przez String.valueOf.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Wyjątek wylatuje z DispatcherServleta do Tomcata (albo Spring woła {@code response.sendError(status)} dla
     *      {@code @ResponseStatus}, {@code ResponseStatusException}, 404). Boot zarejestrował w Tomcacie stronę błędu
     *      {@code /error}, więc Tomcat wykonuje wewnętrzny dispatch na ten adres. Ciało buduje {@code BasicErrorController}:
     *      {@code error()} daje JSON, {@code errorHtml()} stronę HTML — wybór zależy od nagłówka {@code Accept} zapytania.
     *   2. {@code timestamp, status, error, path} (test unhandledExceptionIs500). Ustawienia dokładają: {@code message},
     *      {@code exception}, {@code trace}, {@code errors} (błędy walidacji/wiązania).
     *   3. Aplikacja uruchomi się normalnie (test Err01OldNamesTest), bo stara nazwa po prostu jest ignorowana. W Boot 4
     *      przedrostek brzmi {@code spring.web.error.*}; z {@code spring.web.error.include-message=always} pole się pojawi
     *      (test Err01IncludeTest).
     *   4. Ślad stosu w odpowiedzi to wyciek informacji: nazwy pakietów, klas, numery linii i wersje bibliotek ułatwiają
     *      atak. Takie ustawienie wolno mieć tylko w profilu dev. Na produkcji szczegóły trzymamy w logach serwera.
     *   5. Test nie dojdzie do asercji: {@code mockMvc.perform} rzuci {@code ServletException} z przyczyną
     *      {@code IllegalStateException} (test unhandledExceptionReachesTheTest). MockMvc nie wykonuje przekierowania na
     *      {@code /error}, więc 500 w ogóle nie powstaje.
     *   6. Status 404, ciało {@code error: "Not Found"} (pola: timestamp, status, error, path). Tekstu "Nie ma produktu
     *      o id 9" klient NIE zobaczy, bo domyślnie {@code include-message=never} (test responseStatusException). Opis
     *      pojawi się dopiero po {@code spring.web.error.include-message=always} (test Err01IncludeTest) — albo przy
     *      własnej obsłudze błędu (Err02/Err03).
     *   7. MockMvc nie przechodzi przez {@code /error}, więc dla domyślnego 404 ciało jest puste i brak ścieżki JSON
     *      (test unknownPathHasNoBody). Popraw: w MockMvc sprawdzaj tylko {@code status().isNotFound()}; kształt JSON
     *      sprawdź na prawdziwym serwerze (RANDOM_PORT + RestClient) albo napisz własny handler, który zbuduje ciało.
     *   8. Przeglądarka ({@code Accept: text/html}) dostaje stronę "Whitelabel Error Page" (HTML, status 500), skrypt z
     *      {@code Accept: application/json} — JSON {@code {"timestamp":…,"status":500,"error":"Internal Server Error","path":…}}
     *      (testy browserGetsWhitelabelPage i unhandledExceptionIs500).
     */
    // </editor-fold>
}
