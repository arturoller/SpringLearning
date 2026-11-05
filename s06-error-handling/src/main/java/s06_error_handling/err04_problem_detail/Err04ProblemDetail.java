package s06_error_handling.err04_problem_detail;

/**
 * <pre>
 * TEMAT: ProblemDetail (RFC 9457) — standardowy format błędu API i ResponseEntityExceptionHandler
 *        (problem detail = szczegóły problemu, gotowy, znormalizowany opis błędu; RFC = Request for Comments, numerowany
 *         dokument-standard internetu; response entity exception handler = klasa bazowa z gotową obsługą wyjątków Springa;
 *         extension = rozszerzenie, własna dodatkowa właściwość; instance = konkretne wystąpienie błędu)
 *
 * W SKRÓCIE:
 *   W Err03 wymyśliliśmy własny rekord {@code ApiError}. Każdy zespół wymyśla inny, więc klienci uczą się formatu każdego API od nowa.
 *   Standard RFC 9457 (następca RFC 7807) mówi, jak ma wyglądać odpowiedź błędu: obiekt JSON z polami {@code type}, {@code title},
 *   {@code status}, {@code detail}, {@code instance} oraz dowolnymi własnymi rozszerzeniami, z typem mediów
 *   {@code application/problem+json}. Spring ma na to gotową klasę {@code ProblemDetail}. Handler po prostu ją zwraca, a Spring
 *   ustawia status i nagłówek {@code Content-Type}. Wyjątki implementujące interfejs {@code ErrorResponse} (m.in.
 *   {@code ResponseStatusException} i {@code ErrorResponseException}) niosą ProblemDetail w sobie. Wszystkie wyjątki samego Springa
 *   (405, 415, 400, 404…) zamienia na ProblemDetail klasa {@code ResponseEntityExceptionHandler} — albo wbudowany w Boota handler
 *   po włączeniu {@code spring.mvc.problemdetails.enabled=true}. Dziedziczenie po {@code ResponseEntityExceptionHandler} to
 *   standardowa odpowiedź na pułapkę "łap wszystko" z Err03.
 *
 * ANALOGIA: formularz reklamacyjny ujednolicony przez urząd.
 *   W Err03 każda sieć sklepów miała własny druk reklamacji (ApiError): jedna z rubryką "kod", druga z "numer sprawy". Teraz urząd
 *   wydaje jeden obowiązkowy druk (RFC 9457) z rubrykami: rodzaj sprawy (type), nagłówek (title), numer statusu (status), opis
 *   (detail), numer konkretnej sprawy (instance). Sieć może dopisać na odwrocie własne rubryki (rozszerzenia, np. code).
 *   Każdy urzędnik (każdy klient API) czyta taki druk tak samo, a gotowe stemple (ResponseEntityExceptionHandler) wypełniają
 *   rubryki za sieć tam, gdzie błąd zgłosił sam urząd (wyjątki Springa).
 *
 * JAK TO DZIAŁA:
 *   1. Pola ProblemDetail: {@code type} (adres URI identyfikujący RODZAJ błędu; brak = "about:blank", czyli "nic ponad kod
 *      statusu"), {@code title} (krótki, stały opis rodzaju błędu), {@code status} (kod HTTP), {@code detail} (opis TEGO
 *      zdarzenia), {@code instance} (URI konkretnego wystąpienia; Spring uzupełnia go adresem żądania). Własne dodatki:
 *      {@code setProperty("code", "OUT_OF_STOCK")} — w JSON pojawiają się na tym samym poziomie co pola standardowe.
 *   2. Handler {@code @ExceptionHandler} zwraca {@code ProblemDetail}. Spring czyta z niego status (nie potrzeba
 *      {@code @ResponseStatus}), dopisuje {@code instance} i wysyła JSON z typem {@code application/problem+json}.
 *   3. Wyjątek z interfejsem {@code ErrorResponse} ma {@code getStatusCode()}, {@code getHeaders()} i {@code getBody()} (to jest
 *      ProblemDetail). Dzięki temu obsługa jest automatyczna — bez własnego handlera.
 *   4. Dziedzicząc po {@code ResponseEntityExceptionHandler}, dostajemy gotowe metody {@code handleXxx} (jedna na wyjątek Springa)
 *      i możemy nadpisać dowolną. Wszystkie schodzą do {@code handleExceptionInternal}, które zapisuje ProblemDetail do odpowiedzi.
 *   5. Teksty (title, detail, type) można wziąć z pliku komunikatów ({@code MessageSource}) po kluczach
 *      {@code problemDetail.title.<klasa wyjątku>} itd. — zob. sekcja 5.
 *
 * SŁÓWKA:
 *   problem detail = szczegóły problemu; media type = typ mediów (wartość nagłówka Content-Type); extension member = własna
 *   właściwość dodana do standardu; override = nadpisanie metody w klasie potomnej; message source = źródło komunikatów
 *   (pliki .properties z tekstami, także do tłumaczeń); locale = ustawienia językowe; built-in = wbudowany;
 *   conditional = warunkowy; ambiguous = niejednoznaczny.
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest03Responses (ResponseStatusException, @ResponseStatus),
 *             s05_validation/Val02ValidInControllers (dwa wyjątki walidacji),
 *             s06_error_handling/Err01DefaultErrors (domyślny /error i jego JSON),
 *             s06_error_handling/Err02ExceptionHandler (@ExceptionHandler),
 *             s06_error_handling/Err03ControllerAdvice (własny ApiError, pułapka "łap wszystko"),
 *             s06_error_handling/Err05CustomExceptions (hierarchia wyjątków sklepu mapowana na ProblemDetail).
 * </pre>
 */
public final class Err04ProblemDetail {

    private Err04ProblemDetail() {
    }

    // =================================================================================================
    // 1. ANATOMIA PROBLEMDETAIL (Err04ProblemDetailAnatomyTest, Err04ProblemDetailTest.problemDetailFromHandler)
    // =================================================================================================
    //
    // Gotowa odpowiedź z naszej lekcji (GET /err04/products/9):
    //
    //     HTTP/1.1 404
    //     Content-Type: application/problem+json
    //
    //     {
    //       "type": "https://sklep.example/errors/product-not-found",
    //       "title": "Produkt nie istnieje",
    //       "status": 404,
    //       "detail": "Nie ma produktu o id 9",
    //       "instance": "/err04/products/9",
    //       "code": "PRODUCT_NOT_FOUND"
    //     }
    //
    // Każde pole ma inną rolę (dlatego warto je odróżniać):
    //   • type     — RODZAJ błędu, stały dla wszystkich takich błędów. To adres URI; najlepiej prowadzący do strony z opisem
    //                błędu (nie musi istnieć na starcie, ale w prawdziwym API powinien). Dla klienta to "klucz" do rozpoznania
    //                błędu. Nie ustawiasz? Pole w ogóle nie trafia do JSON-a (standard: brak = about:blank). UWAGA: w Spring
    //                Framework 7 {@code getType()} zwraca wtedy {@code null}; w starszych wersjach zwracało about:blank.
    //   • title    — krótki opis rodzaju błędu, taki sam dla każdego takiego błędu ("Produkt nie istnieje"). Nie ustawiasz?
    //                Spring wstawia angielski opis statusu ("Not Found") — test problemDetailFields.
    //   • status   — kod HTTP powtórzony w ciele (przydaje się, gdy ciało leży w logu, a nagłówków już nie ma).
    //   • detail   — opis TEGO zdarzenia ("Nie ma produktu o id 9"). To jedyne pole, które zmienia się z żądania na żądanie.
    //   • instance — URI konkretnego zdarzenia. Spring uzupełnia je adresem żądania, jeśli sam go nie ustawisz.
    //   • własne właściwości — {@code problem.setProperty("code", "PRODUCT_NOT_FOUND")}. Standard nazywa je rozszerzeniami.
    //                Zasada: klient ma rozpoznawać błąd po {@code type} lub {@code code}, NIGDY po tekście z {@code title}/{@code detail}
    //                (tekst można przetłumaczyć lub poprawić literówkę).
    //
    // Jak to jest zbudowane? {@code ProblemDetail} to zwykła klasa Javy: {@code ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "…")}
    // tworzy obiekt, a settery uzupełniają resztę. Do JSON-a zamienia ją Jackson (w Boot 4 to Jackson 3) z gotową regułą ze
    // Springa ({@code ProblemDetailJacksonMixin}), która "rozpłaszcza" właściwości własne na poziom pól standardowych — dlatego
    // {@code code} stoi obok {@code title}, a nie w zagnieżdżonej mapie {@code properties}. Test problemDetailFromHandler sprawdza
    // dokładnie 6 pól.
    //
    // Typ mediów to {@code application/problem+json} (nie {@code application/json}) — klient wie z samego nagłówka, że dostał
    // opis błędu w standardowym formacie. Dotyczy to także klienta, który w nagłówku {@code Accept} prosi o zwykły
    // {@code application/json}: serwer i tak odpowie {@code application/problem+json} (test Err04ServerTest.problemJsonOnTheWire).
    //
    // DOBRA PRAKTYKA: stałe {@code type} i {@code title} dla rodzaju błędu, a zmienny tekst tylko w {@code detail}. Dlaczego?
    //   Klient i dokumentacja mogą wtedy opisać RODZAJ błędu raz, a zmienne dane (id produktu) nie mieszają się
    //   z tym, po czym błąd rozpoznajemy.
    //
    // DOBRA PRAKTYKA: dodaj stabilny, maszynowy kod w rozszerzeniu (tu {@code code}), jeśli {@code type} jest dla klientów zbyt
    //   "długi" do porównywania. Dlaczego? Kod w stylu {@code OUT_OF_STOCK} to wygodny klucz w {@code switch} po stronie
    //   aplikacji mobilnej czy frontu, a nie zmienia się razem z redakcją tekstów.
    //
    // PUŁAPKA: wstawianie do {@code detail} komunikatu technicznego wyjątku ({@code exception.getMessage()}) dla błędów 500.
    //   Dlaczego? Komunikaty zawierają SQL, nazwy hostów i dane użytkowników (Err01DefaultErrors). Opis ogólny idzie do klienta,
    //   szczegóły do logu. Dla wyjątków DOMENOWYCH (nasze własne, pisane dla człowieka teksty) {@code getMessage()} jest bezpieczne.

    // =================================================================================================
    // 2. HANDLER ZWRACAJĄCY PROBLEMDETAIL; ErrorResponse (Err04Advice, ShopController, ProductArchivedException)
    // =================================================================================================
    //
    // PRZED (Err03): handler zwracał własny rekord i musiał dopisywać {@code @ResponseStatus}:
    //
    //     @ExceptionHandler(ProductNotFoundException.class)
    //     @ResponseStatus(HttpStatus.NOT_FOUND)
    //     public ApiError onProductNotFound(ProductNotFoundException e, HttpServletRequest request) { … request.getRequestURI() … }
    //
    // PO (Err04Advice): status siedzi W obiekcie, a adresu żądania nie trzeba przekazywać:
    //
    //     @ExceptionHandler(ProductNotFoundException.class)
    //     public ProblemDetail onProductNotFound(ProductNotFoundException exception) {
    //         ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    //         problem.setType(URI.create("https://sklep.example/errors/product-not-found"));
    //         problem.setTitle("Produkt nie istnieje");
    //         problem.setProperty("code", "PRODUCT_NOT_FOUND");
    //         return problem;
    //     }
    //
    // Spring widzi zwracany typ {@code ProblemDetail} i sam: (1) ustawia status z pola {@code status}, (2) dopisuje {@code instance}
    // z adresu żądania, (3) wybiera typ {@code application/problem+json}. Dlatego samo {@code @ResponseStatus} jest zbędne
    // (test problemDetailFromHandler: 404 bez żadnej adnotacji ze statusem).
    //
    // Wyjątki, które SAME wiedzą, jak wygląda ich odpowiedź, implementują interfejs {@code ErrorResponse}. Najważniejsze dwa:
    //   • {@code ResponseStatusException} (znany z Rest03Responses): {@code throw new ResponseStatusException(HttpStatus.CONFLICT,
    //     "Produkt 1 jest już zarezerwowany")} — to status + detail. Zero własnych klas, dobre do szybkich przypadków.
    //   • {@code ErrorResponseException}: klasa bazowa do dziedziczenia. Nasz {@code ProductArchivedException} (410 Gone) ustawia
    //     type, detail i własną właściwość {@code code} w konstruktorze — wyjątek niesie komplet informacji, a handlera
    //     nie trzeba pisać. Test errorResponsesBecomeProblemDetail: 409 i 410 w formacie problem+json bez ani jednego
    //     handlera na te wyjątki w Err04Advice.
    //
    // Który sposób wybrać? Własny wyjątek domenowy + handler w advice (Err05CustomExceptions) trzyma warstwę serwisu wolną od
    // HTTP; {@code ErrorResponseException} wiąże wyjątek ze statusem HTTP, więc pasuje do warstwy web (kontrolery, filtry,
    // klasy "HTTP-owe"), a nie do serwisów.
    //
    // PUŁAPKA: rzucanie {@code ResponseStatusException} w serwisie. Dlaczego? Serwis zaczyna "znać HTTP" (status, nagłówki),
    //   a ta sama logika w zadaniu wsadowym czy kolejce komunikatów nie ma żadnego HTTP — kod nie nadaje się do ponownego użycia.
    //   W serwisie rzucaj wyjątek domenowy, a status wybieraj w advice.

    // =================================================================================================
    // 3. WBUDOWANE: spring.mvc.problemdetails.enabled (Err04BuiltInHandlerTest, Err04BuiltInOffTest)
    // =================================================================================================
    //
    // Co zrobi aplikacja BEZ naszego advice, gdy włączymy jedną właściwość?
    //
    //     spring.mvc.problemdetails.enabled=true
    //
    // Domyślnie jest {@code false} (sprawdzone w metadanych Boota 4.1.1). Po włączeniu Boot rejestruje bean
    // {@code problemDetailsExceptionHandler} — gotowe {@code @ControllerAdvice} dziedziczące po {@code ResponseEntityExceptionHandler}.
    // Wynik (Err04BuiltInHandlerTest): zła metoda HTTP (405, z nagłówkiem Allow), zły Content-Type (415), zepsuty JSON i zły typ
    // parametru (400), nieznana ścieżka (404), a także {@code ResponseStatusException} i {@code ErrorResponseException} dostają
    // {@code application/problem+json}. Bez właściwości (Err04BuiltInOffTest) status jest taki sam, ale ciało w MockMvc jest
    // puste, a na prawdziwym serwerze jest ogólny JSON z {@code /error} (Err01DefaultErrors).
    //
    // Czego wbudowany handler NIE robi:
    //   • wyjątków domenowych (ProductNotFoundException) nie zna — zostają niezłapane (w MockMvc: ServletException, na serwerze: 500
    //     z /error); test domainExceptionsAreNotHandled;
    //   • błąd walidacji ma tylko ogólne "Invalid request content." — bez listy pól (test validationWithoutFieldList).
    // Dlatego w realnej aplikacji i tak piszemy własny advice (sekcja 4). Właściwość jest dobra na start albo dla małego API
    // z samymi wyjątkami Springa.
    //
    // UWAGA (Boot 4 vs stare poradniki): stare właściwości {@code server.error.*} Boot 4 po cichu ignoruje (Err01DefaultErrors),
    // a właściwość ProblemDetail ma prefiks {@code spring.mvc.problemdetails.*} — nie {@code spring.web.error.*}.
    //
    // W tej lekcji NIE ustawiamy tej właściwości w plikach konfiguracji — tylko w jednym teście ({@code properties = …}), żeby nie
    // wpływała na inne lekcje. W swoim projekcie wpiszesz ją do {@code application.properties}.

    // =================================================================================================
    // 4. ResponseEntityExceptionHandler — ROZWIĄZANIE DOCELOWE (Err04Advice, Err04ProblemDetailTest)
    // =================================================================================================
    //
    // W Err03 pokazaliśmy pułapkę: handler {@code Exception.class} połyka wyjątki Springa (405 → 500) i musieliśmy ręcznie
    // wyłapywać {@code ErrorResponse}, {@code HttpMessageNotReadableException} i {@code TypeMismatchException}. Tu jest gotowy,
    // przetestowany przez zespół Springa zamiennik: klasa bazowa {@code ResponseEntityExceptionHandler}.
    //
    //     @RestControllerAdvice
    //     public class Err04Advice extends ResponseEntityExceptionHandler { … }
    //
    // Dostajesz za darmo: ProblemDetail dla 405, 415, 406, 400 (brak parametru, zepsuty JSON, zły typ, walidacja), 404 (brak
    // zasobu), 500 (błąd zapisu odpowiedzi) i innych — z poprawnymi nagłówkami (np. {@code Allow}). Test
    // springsOwnExceptionsAsProblemDetail: pięć różnych błędów Springa, wszystkie problem+json, 405 z {@code Allow: GET}.
    // Wszystkie metody klasy bazowej mają nazwę {@code handleXxx} i są {@code protected}, więc możesz nadpisać tylko to, co chcesz zmienić.
    //
    // NADPISYWANIE: błąd walidacji ma w Err04Advice listę pól:
    //
    //     @Override
    //     protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
    //             HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    //         … ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Dane są niepoprawne");
    //         … problem.setProperty("errors", lista obiektów {field, message});
    //         return handleExceptionInternal(exception, problem, headers, status, request);
    //     }
    //
    // Na końcu wołamy {@code handleExceptionInternal}: ta metoda zapisuje ciało do odpowiedzi tak samo jak dla wbudowanych
    // przypadków. Test validationErrorsList: tablica {@code errors} z polami {@code field} i {@code message}, posortowana po nazwie pola.
    //
    // HANDLER "ŁAP WSZYSTKO" Z Err03 — TERAZ BEZPIECZNY: w Err04Advice jest {@code @ExceptionHandler(Exception.class)} dla nieoczekiwanych
    // błędów (500, ogólny opis, szczegóły do logu). Nie połyka wyjątków Springa, bo klasa bazowa deklaruje je POSZCZEGÓLNIE,
    // a Spring wybiera handler dla najbliższego typu wyjątku (Err02ExceptionHandler, Err03ControllerAdvice). Test
    // catchAllKeepsSpringsStatuses: {@code DELETE} nadal daje 405, a nie 500. Nie potrzebujemy już ręcznego {@code instanceof ErrorResponse}.
    //
    // Co z wbudowanym handlerem Boota? Boot rejestruje swój tylko wtedy, gdy w aplikacji NIE MA żadnego
    // {@code ResponseEntityExceptionHandler} (warunek {@code @ConditionalOnMissingBean}). Nasz advice go wyłącza — nawet gdy
    // właściwość z sekcji 3 jest włączona (test Err04OwnAdviceDisablesBuiltInTest). Dzięki temu nie ma dwóch konkurujących handlerów.
    //
    // PUŁAPKA: własny {@code @ExceptionHandler(MethodArgumentNotValidException.class)} w klasie dziedziczącej po
    //   {@code ResponseEntityExceptionHandler}. Dlaczego? Klasa bazowa ma już handler dla tego wyjątku (jedną metodę
    //   {@code handleException} z listą typów), więc drugi daje błąd "Ambiguous @ExceptionHandler method mapped for …" PRZY STARCIE
    //   aplikacji (test Err04AmbiguousHandlerTest). Zmieniasz zachowanie przez NADPISANIE metody {@code handleXxx}, nie przez
    //   dopisanie drugiego handlera.
    //
    // PUŁAPKA: pominięcie {@code handleExceptionInternal} i zwrócenie własnego {@code ResponseEntity} z gołym tekstem. Dlaczego?
    //   Tracisz pracę, którą wykonuje klasa bazowa w tej metodzie: podstawienie tekstów z MessageSource dla wyjątków ErrorResponse
    //   (sekcja 5) i zapamiętanie wyjątku dla serwera przy statusie 500.
    //
    // DOBRA PRAKTYKA: jeden advice dziedziczący po {@code ResponseEntityExceptionHandler} jako domyślna obsługa błędów API. Dlaczego?
    //   Dostajesz spójny format dla błędów Springa i swoich, nie utrzymujesz listy wyjątków do wyłapania ręcznie, a nowe
    //   wersje Springa dokładają obsługę nowych wyjątków bez Twojej pracy.

    // =================================================================================================
    // 5. KOMUNIKATY Z MessageSource I type JAKO ADRES DOKUMENTACJI (Err04MessagesTest)
    // =================================================================================================
    //
    // Teksty błędów Springa są po angielsku ("Method 'DELETE' is not supported."). Klasa bazowa ResponseEntityExceptionHandler
    // pyta jednak {@code MessageSource} (źródło komunikatów, te same pliki co do tłumaczeń) o napisy dla każdego wyjątku
    // implementującego {@code ErrorResponse}. Klucze mają stałą postać (sprawdzone testem messageCodes):
    //
    //     problemDetail.type.<pełna nazwa klasy wyjątku>      →  pole type   (URI)
    //     problemDetail.title.<pełna nazwa klasy wyjątku>     →  pole title
    //     problemDetail.<pełna nazwa klasy wyjątku>           →  pole detail (może mieć argumenty {0}, {1}…)
    //
    // Plik {@code err04/messages.properties} tej lekcji, włączony właściwością {@code spring.messages.basename=err04/messages}
    // w profilu {@code err04} (plik application-err04.properties; w testach {@code @ActiveProfiles("err04")}):
    //
    //     problemDetail.title.org.springframework.web.HttpRequestMethodNotSupportedException=Niedozwolona metoda HTTP
    //     problemDetail.org.springframework.web.HttpRequestMethodNotSupportedException=Metoda {0} nie jest tu obsługiwana. Dozwolone metody: {1}
    //
    // Efekt (test messagesForSpringsException): 405 ma polski tytuł i opis z podstawioną metodą ({0} = DELETE) oraz listą
    // dozwolonych metod ({1} = [GET]). To samo działa dla NASZYCH wyjątków dziedziczących po {@code ErrorResponseException}
    // (test messagesForOwnException: ProductArchivedException, argument {0} to id produktu przekazany w konstruktorze).
    // Tekst wpisany w kodzie (setDetail…) jest wartością zapasową, a wpis w pliku ma pierwszeństwo, gdy istnieje.
    //
    // Czego nie obejmuje: ProblemDetail, który sami budujemy w handlerze (np. nasze onProductNotFound) — tam teksty są w kodzie,
    // plik komunikatów ich nie zmienia (test ownHandlersIgnoreMessageFile). Chcesz tłumaczyć także je? Użyj {@code MessageSource}
    // w handlerze samodzielnie ({@code messageSource.getMessage("shop.productNotFound", args, locale)}).
    // Język odpowiedzi wybiera nagłówek {@code Accept-Language} klienta (lub domyślny język serwera) — można wtedy dodać
    // {@code messages_en.properties} bez zmiany kodu. Kodowanie plików to UTF-8 (domyślnie w Boot), więc polskie litery wpisujemy normalnie.
    //
    // ADRES W type: URI w stylu {@code https://sklep.example/errors/out-of-stock} to "nazwa rodzaju błędu" — domena firmy,
    // stały fragment ścieżki, nazwa błędu kebab-case. Najlepiej, by pod tym adresem leżała strona z opisem: co znaczy błąd, kiedy
    // występuje, jak go naprawić. Klient nie musi jej pobierać; liczy się, że adres się NIE ZMIENIA.
    //
    // DOBRA PRAKTYKA: adresy {@code type} traktuj jak część publicznego kontraktu API: raz opublikowany adres nie zmienia się,
    //   nawet gdy zmienia się tytuł. Dlaczego? Klienci porównują {@code type} jako tekst, więc zmiana adresu psuje ich kod tak samo
    //   jak zmiana nazwy pola.
    //
    // PUŁAPKA: szukanie klucza po krótkiej nazwie klasy ({@code problemDetail.title.HttpRequestMethodNotSupportedException}).
    //   Dlaczego? Spring używa PEŁNEJ nazwy z pakietem; krótka nie zostanie znaleziona i po cichu zadziała tekst domyślny
    //   (nigdzie nie pojawi się błąd). Najpewniej: skopiuj nazwę z {@code ErrorResponse.getDefaultTitleMessageCode(Klasa.class)}.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • RFC 9457 (następca 7807): pola type, title, status, detail, instance + własne rozszerzenia (setProperty), typ mediów
     *     application/problem+json.
     *   • ProblemDetail.forStatusAndDetail(status, opis) → settery. Brak type → pole pominięte (getType() = null w Spring 7);
     *     brak title → angielski opis statusu; instance dopisuje Spring.
     *   • Handler zwracający ProblemDetail nie potrzebuje @ResponseStatus — status jest w obiekcie.
     *   • ErrorResponse (ResponseStatusException, ErrorResponseException i wyjątki Springa) niesie ProblemDetail; własny wyjątek
     *     może dziedziczyć po ErrorResponseException.
     *   • spring.mvc.problemdetails.enabled (domyślnie false) → wbudowany handler wyjątków Springa. Nie zna wyjątków domenowych i nie
     *     da listy pól walidacji.
     *   • Własny advice dziedziczący po ResponseEntityExceptionHandler wyłącza wbudowany (ConditionalOnMissingBean). Nadpisuj
     *     handleXxx, kończ na handleExceptionInternal. Nie dopisuj drugiego @ExceptionHandler dla tego samego wyjątku (Ambiguous).
     *   • Handler Exception.class w takiej klasie nie połyka 405/415/400: klasa bazowa deklaruje te wyjątki poszczególnie.
     *   • Klucze komunikatów: problemDetail.type.|title.|<pełna nazwa klasy>. Dotyczą wyjątków ErrorResponse, nie ProblemDetail
     *     zbudowanych ręcznie w handlerze.
     *   • type = stały adres URI rodzaju błędu; klient rozpoznaje błąd po type/code, nie po tekście.
     *
     * PYTANIA KONTROLNE:
     *   1. Które pola ma ProblemDetail według RFC 9457 i co się dzieje z polami, których nie ustawisz (type, title, instance)?
     *   2. Czym różnią się title i detail? Które z nich powinno być takie samo dla wszystkich błędów danego rodzaju?
     *   3. Co się stanie? W aplikacji nie ma żadnego advice ani właściwości spring.mvc.problemdetails.enabled. Kontroler rzuca
     *      {@code ResponseStatusException(CONFLICT, "…")}. Jaki status i jakie ciało dostanie klient w MockMvc?
     *   4. Co się stanie? Włączasz {@code spring.mvc.problemdetails.enabled=true} i dopisujesz własny advice dziedziczący po
     *      {@code ResponseEntityExceptionHandler}. Ile handlerów obsługuje wyjątki Springa i skąd to wiesz?
     *   5. ZNAJDŹ BŁĄD: advice dziedziczy po {@code ResponseEntityExceptionHandler} i ma
     *      {@code @ExceptionHandler(MethodArgumentNotValidException.class)}, bo "chcę własny format walidacji". Aplikacja
     *      nie startuje. Dlaczego i jak to naprawić?
     *   6. Co się stanie? Klient wysyła {@code Accept: application/json}, a serwer zwraca ProblemDetail. Jaki Content-Type
     *      dostanie w odpowiedzi?
     *   7. ZNAJDŹ BŁĄD: w pliku komunikatów jest {@code problemDetail.title.ProductNotFoundException=Produkt nie istnieje}, a odpowiedź
     *      dalej ma tytuł z kodu handlera. Podaj DWIE przyczyny.
     *   8. Dlaczego advice z handlerem {@code Exception.class} jest bezpieczny, gdy dziedziczy po {@code ResponseEntityExceptionHandler},
     *      a niebezpieczny (Err03), gdy jest zwykłą klasą?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielet: ExerciseController, ExerciseAdvice, testy: Err04ProblemDetailExercisesTest (@Tag("cwiczenie"))
    // =================================================================================================
    //
    // Rozwiązania wzorcowe: pakiet s06_error_handling.solutions.err04_problem_detail. Wszystkie adresy mają prefiks /err04/cw.
    // Test ćwiczeń buduje kontekst ręcznie (kontroler + ExerciseAdvice), więc Twoje handlery nie walczą z Err04Advice z lekcji.
    // ExerciseAdvice dziedziczy po ResponseEntityExceptionHandler.
    //
    // ĆWICZENIE 1 (łatwe): klient — w ExerciseController.customer dla id 1 zwróć "Anna", dla innych rzuć CustomerNotFoundException.
    //   W ExerciseAdvice dopisz handler zwracający ProblemDetail: 404, title "Klient nie istnieje", detail z wyjątku, code
    //   "CUSTOMER_NOT_FOUND". Podpowiedź: wzoruj się na Err04Advice.onProductNotFound.
    //
    // ĆWICZENIE 2 (łatwe): zamówienie — w ExerciseController.cancel rzuć ErrorResponseException ze statusem 409 (bez handlera!):
    //   setType("https://sklep.example/errors/order-locked"), setTitle("Zamówienie zablokowane"), detail "Zamówienie <id> jest
    //   zablokowane" i getBody().setProperty("code", "ORDER_LOCKED"). Podpowiedź: patrz ProductArchivedException.
    //
    // ĆWICZENIE 3 (średnie): brakujący parametr — search zwraca "Szukam: " + q. W ExerciseAdvice nadpisz
    //   handleMissingServletRequestParameter: 400, code "MISSING_PARAMETER", właściwość parameter = nazwa parametru (q).
    //   Podpowiedź: nazwę da exception.getParameterName(); odpowiedź zwróć przez handleExceptionInternal.
    //
    // ĆWICZENIE 4 (średnie): "łap wszystko" — crash rzuca IllegalStateException z "tajnym" tekstem. W ExerciseAdvice dopisz handler
    //   Exception.class: 500, detail "Wystąpił nieoczekiwany błąd", code "UNEXPECTED", wyjątek zaloguj (log.error z wyjątkiem
    //   jako ostatnim argumentem). Test sprawdza też, że DELETE /err04/cw/crash daje nadal 405.
    //
    // ĆWICZENIE 5 (trudniejsze): walidacja — create zwraca "Utworzono: " + imię. W ExerciseAdvice nadpisz
    //   handleMethodArgumentNotValid: 400, title "Błąd walidacji", code "VALIDATION_FAILED" i właściwość fields jako mapa
    //   pole → komunikat (posortowana, np. TreeMap). Podpowiedź: exception.getBindingResult().getFieldErrors().

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. type (URI rodzaju błędu), title (krótki, stały opis rodzaju), status (kod HTTP), detail (opis tego zdarzenia),
     *      instance (URI konkretnego zdarzenia) + własne właściwości. Nieustawione: type nie trafia do JSON-a (standard:
     *      about:blank; w Spring 7 getType() zwraca wtedy null), title to angielski opis statusu ("Not Found"), instance
     *      uzupełnia Spring adresem żądania (testy problemDetailFields i problemDetailFromHandler).
     *   2. title opisuje RODZAJ błędu i jest taki sam dla wszystkich błędów danego rodzaju ("Produkt nie istnieje"); detail opisuje
     *      TO zdarzenie ("Nie ma produktu o id 9") i zmienia się z żądania na żądanie. Stałe: type i title.
     *   3. Status 409, ale ciało PUSTE (test Err04BuiltInOffTest). MockMvc nie przechodzi przez /error; na prawdziwym serwerze
     *      klient dostałby ogólny JSON Boota z /error (Err01DefaultErrors), nie ProblemDetail. Dopiero advice albo
     *      spring.mvc.problemdetails.enabled=true daje application/problem+json.
     *   4. Jeden: Boot rejestruje swój handler tylko gdy nie ma ResponseEntityExceptionHandler (@ConditionalOnMissingBean),
     *      więc po dopisaniu naszego advice bean problemDetailsExceptionHandler nie powstaje (test
     *      Err04OwnAdviceDisablesBuiltInTest). Właściwość zostaje bez efektu.
     *   5. Klasa bazowa już ma handler dla MethodArgumentNotValidException, więc Spring zgłasza przy starcie "Ambiguous
     *      @ExceptionHandler method mapped for … MethodArgumentNotValidException" (test Err04AmbiguousHandlerTest). Naprawa:
     *      usuń dopisany handler i nadpisz protected metodę handleMethodArgumentNotValid (tak robi Err04Advice).
     *   6. application/problem+json — serwer nie dopasowuje się do Accept: ProblemDetail zawsze wychodzi z tym typem (test
     *      Err04ServerTest.problemJsonOnTheWire, Accept: application/json).
     *   7. Przyczyny: (a) klucz używa krótkiej nazwy klasy, a Spring szuka PEŁNEJ (z pakietem); (b) tytuł ustawia nasz handler ręcznie
     *      (ProblemDetail zbudowany w kodzie), a plik komunikatów dotyczy wyjątków ErrorResponse obsłużonych przez klasę bazową
     *      (test ownHandlersIgnoreMessageFile). Dodatkowo: ProductNotFoundException nie jest ErrorResponse, więc klucz
     *      i tak nigdy by nie zadziałał.
     *   8. Klasa bazowa deklaruje wyjątki Springa (405, 415, 400…) poszczególnie, a Spring wybiera handler dla najbliższego typu, więc
     *      Exception.class dostają tylko wyjątki nieznane (test catchAllKeepsSpringsStatuses). W Err03 zwykła klasa miała jedyny
     *      handler Exception.class, który pasował do wszystkiego, więc 405 zamieniał się w 500 (Err03NaiveCatchAllTest).
     */
    // </editor-fold>
}
