package s06_error_handling.err03_controller_advice;

/**
 * <pre>
 * TEMAT: @RestControllerAdvice — jedna, globalna obsługa błędów dla wszystkich kontrolerów
 *        (controller advice = "doradca kontrolerów", klasa z handlerami wspólnymi dla wielu kontrolerów; global = globalny,
 *         dla całej aplikacji; precedence = pierwszeństwo; selector = selektor, warunek wyboru kontrolerów; narrow =
 *         zawęzić, ograniczyć zasięg; fallback = rozwiązanie zapasowe, "ostatnia deska ratunku")
 *
 * W SKRÓCIE:
 *   Klasa z adnotacją {@code @RestControllerAdvice} zawiera metody {@code @ExceptionHandler} (znane z Err02ExceptionHandler), ale
 *   obowiązują one WSZYSTKIE kontrolery aplikacji (albo tylko wybrane — selektory). Jedno miejsce zamiast kopiowania handlerów
 *   do każdego kontrolera. {@code @RestControllerAdvice} to {@code @ControllerAdvice} + {@code @ResponseBody}, czyli wartości
 *   zwracane z handlerów są zamieniane na JSON. Reguły rozstrzygania: handler LOKALNY w kontrolerze wygrywa z globalnym; między
 *   różnymi advice rozstrzyga {@code @Order}; w obrębie jednego advice — najbliższy typ wyjątku. Handler "łap wszystko"
 *   ({@code Exception.class}) wymaga ostrożności, bo połyka też wyjątki, które Spring umiał obsłużyć sam (405, 415, 404…).
 *
 * ANALOGIA: centrala reklamacji sieci sklepów.
 *   W Err02 każdy sklep miał własnego kierownika od reklamacji (handler w kontrolerze). Teraz sieć otwiera jedną centralę
 *   (advice): każdy sklep przekazuje tam sprawę, a centrala wydaje klientowi dokument w jednym, ujednoliconym formacie.
 *   Centrala ma swoje oddziały specjalistyczne (advice zawężone do wybranych sklepów) i dział "wszystko inne" na końcu
 *   kolejki. Jeśli sklep ma własną, lokalną procedurę (handler lokalny), to ona ma pierwszeństwo przed centralą.
 *   Dział "wszystko inne" musi stać na końcu kolejki: gdyby stanął na początku, przejąłby sprawy specjalistów.
 *
 * JAK TO DZIAŁA:
 *   1. Metoda kontrolera rzuca wyjątek. {@code DispatcherServlet} przekazuje go {@code ExceptionHandlerExceptionResolver}.
 *   2. Resolver zna kontroler (tzw. typ handlera), który rzucił wyjątek, i szuka metody {@code @ExceptionHandler}:
 *      a) NAJPIERW w klasie samego kontrolera. Znalazł pasującą? Koniec — handler lokalny wygrywa;
 *      b) potem w beanach {@code @ControllerAdvice} — w kolejności {@code @Order} (mniejsza liczba = wcześniej). Bierze
 *         PIERWSZY advice, który ma handler pasujący do wyjątku, i w nim wybiera najbliższy typ w hierarchii.
 *   3. Advice z selektorem ({@code assignableTypes}, {@code annotations}, {@code basePackages}, {@code basePackageClasses}) jest
 *      pomijany, jeśli kontroler nie pasuje do selektora. Błędy powstające, zanim Spring wybierze kontroler (405, nieznana
 *      ścieżka) widzą tylko advice BEZ selektora.
 *   4. Wywołuje handler, a jego wynik trafia do klienta dokładnie tak, jak wynik zwykłej metody kontrolera (Err02).
 *   5. Żaden handler nie pasuje? Dalej działają wbudowane mechanizmy ({@code @ResponseStatus}, {@code DefaultHandlerExceptionResolver},
 *      a na końcu {@code /error} — Err01DefaultErrors).
 *
 * SŁÓWKA:
 *   advice = rada, wskazówka; selector = selektor; marker annotation = adnotacja-znacznik; assignable = przypisywalny
 *   (kontroler będący danym typem lub jego podklasą); precedence = pierwszeństwo; highest/lowest precedence = najwyższe/najniższe
 *   pierwszeństwo; greedy = zachłanny; consistent = spójny; contract = umowa.
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest01Controllers (kontrolery REST, 405 i 415 domyślnie),
 *             s05_validation/Val02ValidInControllers (dwa wyjątki walidacji),
 *             s06_error_handling/Err01DefaultErrors (domyślny /error),
 *             s06_error_handling/Err02ExceptionHandler (handler w jednym kontrolerze — to samo, mniejszy zasięg),
 *             s06_error_handling/Err04ProblemDetail (ResponseEntityExceptionHandler i standardowy format RFC 9457),
 *             s06_error_handling/Err05CustomExceptions (hierarchia wyjątków sklepu i jej mapowanie),
 *             s01_core_ioc/Ioc03Stereotypes (adnotacje-stereotypy: {@code @ControllerAdvice} jest beanem jak {@code @Component}),
 *             s11_testing/Test02MockitoBeans (testy warstwy web).
 * </pre>
 */
public final class Err03ControllerAdvice {

    private Err03ControllerAdvice() {
    }

    // =================================================================================================
    // 1. @RestControllerAdvice: JEDNO MIEJSCE DLA WIELU KONTROLERÓW (ShopAdvice, Err03ControllerAdviceTest)
    // =================================================================================================
    //
    // PRZED (Err02): każdy kontroler miał własny handler dla ProductNotFoundException. Dziesięć kontrolerów = dziesięć kopii,
    // które z czasem się rozjeżdżają (jeden zwraca 404 z kodem, inny 400 bez kodu).
    //
    // PO: jedna klasa, zero handlerów w kontrolerach (ProductController i OrderController nie mają ani jednej adnotacji
    // {@code @ExceptionHandler}):
    //
    //     @RestControllerAdvice
    //     @Order(100)
    //     public class ShopAdvice {
    //         @ExceptionHandler(ProductNotFoundException.class)
    //         @ResponseStatus(HttpStatus.NOT_FOUND)
    //         public ApiError onProductNotFound(ProductNotFoundException exception, HttpServletRequest request) { … }
    //         … OrderNotFoundException, OutOfStockException, MethodArgumentNotValidException …
    //     }
    //
    // Test oneAdviceForSeveralControllers: błędy z /err03/products i /err03/orders mają ten sam szkielet i te same zasady
    // (404 ORDER_NOT_FOUND, 409 OUT_OF_STOCK…). Test validationInAdvice: błąd walidacji to ten sam {@code ApiError} z dodatkowym
    // polem {@code fields} (mapa pole → komunikat, posortowana). Pole {@code fields} pojawia się tylko tu, bo rekord ma
    // {@code @JsonInclude(NON_NULL)}.
    //
    // Co daje {@code @RestControllerAdvice}? To skrót: {@code @ControllerAdvice} (bean, który "doradza" kontrolerom) +
    // {@code @ResponseBody} (wynik handlera → JSON). Samo {@code @ControllerAdvice} w aplikacji REST wymagałoby dopisania
    // {@code @ResponseBody} nad każdą metodą (albo zwracania {@code ResponseEntity}); inaczej Spring szukałby widoku HTML
    // o nazwie równej zwróconemu tekstowi.
    //
    // Advice to zwykły bean (znajduje go skanowanie komponentów — Ioc03Stereotypes), więc może mieć wstrzykiwane zależności
    // (np. serwis komunikatów, {@code MessageSource} do tłumaczeń).
    //
    // W TESTACH: {@code @WebMvcTest(ProductController.class)} ładuje wskazany kontroler ORAZ WSZYSTKIE beany
    // {@code @ControllerAdvice} z aplikacji (test Err03AdviceLoadedTest sprawdza, że w kontekście jest ShopAdvice i
    // FallbackAdvice, a OrderController nie). Dlatego handlery globalne działają w teście bez dodatkowej konfiguracji —
    // i dlatego błąd w jednym advice może zepsuć test zupełnie niezwiązanego kontrolera (sekcja 4).
    //
    // DOBRA PRAKTYKA: jedno {@code @RestControllerAdvice} na całą aplikację jako punkt wyjścia, z handlerami grupowanymi po
    //   rodzaju błędu (domenowe, walidacja, ogólny). Dlaczego? Programista, który szuka "skąd się wziął ten kod 409", ma
    //   jedno miejsce do przeszukania — nie musi przeglądać każdego kontrolera.
    //
    // PUŁAPKA: pozostawienie kopii handlera w kontrolerze "na wszelki wypadek". Dlaczego? Lokalny handler zawsze wygrywa z globalnym
    //   (sekcja 2), więc ta kopia po cichu przesłania advice. Zmieniasz format błędu w jednym miejscu, a jeden endpoint
    //   dalej zwraca stary — i nikt nie wie dlaczego.

    // =================================================================================================
    // 2. KOLEJNOŚĆ: LOKALNY → ADVICE; MIĘDZY ADVICE DECYDUJE @Order (LocalOverrideController, PrimaryAdvice)
    // =================================================================================================
    //
    //   wyjątek ─► [1] handler w klasie kontrolera? ──tak──► KONIEC
    //                    │ nie
    //                    ▼
    //              [2] advice po kolei wg @Order (1, 2, …, 100, …, LOWEST_PRECEDENCE)
    //                    │ pierwszy, który MA pasujący handler, wygrywa
    //                    ▼
    //              [3] nikt? → domyślne mechanizmy Springa i /error
    //
    // Handler lokalny (test localHandlerBeatsAdvice): {@code LocalOverrideController} ma własny handler dla
    // {@code ProductNotFoundException}, a {@code ShopAdvice} obsługuje ten sam wyjątek. Odpowiedź: 404 z kodem {@code LOCAL_NOT_FOUND}
    // (lokalny), podczas gdy ten sam wyjątek z {@code ProductController} daje {@code PRODUCT_NOT_FOUND} (globalny).
    // To dobry mechanizm na wyjątki od reguły — ale używaj go oszczędnie.
    //
    // Kilka advice (test orderDecidesBetweenAdvices): {@code PrimaryAdvice} ({@code @Order(1)}) i {@code AlternativeAdvice}
    // ({@code @Order(2)}) obsługują ten sam {@code PriorityDemoException}. Wygrywa Primary (kod PRIMARY) — mniejsza liczba
    // oznacza wyższe pierwszeństwo — choć alfabetycznie Alternative jest przed nim. Stałe: {@code Ordered.HIGHEST_PRECEDENCE}
    // (najmniejsza liczba całkowita, wygrywa zawsze) i {@code Ordered.LOWEST_PRECEDENCE} (największa, wygrywa na końcu).
    //
    // Bez {@code @Order} advice mają to samo, najniższe pierwszeństwo, a ich wzajemna kolejność nie jest gwarantowana — zadziała
    // "na razie", a potem zmieni ją inne skanowanie klas. Gdy kilka advice obsługuje te same wyjątki, ZAWSZE dawaj {@code @Order}.
    //
    // WAŻNE: kolejność dotyczy ADVICE jako całości, nie pojedynczych typów wyjątków. Spring bierze pierwszy advice, który ma
    // JAKIKOLWIEK pasujący handler. "Najbliższy typ" liczy się dopiero wewnątrz jednego advice.
    //
    // PYTANIE REKRUTACYJNE: co wygra — handler lokalny w kontrolerze czy globalny w {@code @ControllerAdvice}? (Lokalny; jeśli
    //   lokalnego nie ma, wybierany jest globalny według {@code @Order}.)

    // =================================================================================================
    // 3. ZAWĘŻANIE ZASIĘGU: assignableTypes, annotations, basePackages (AdminAdvice, ReportsAdvice, LegacyAdvice)
    // =================================================================================================
    //
    // Domyślnie advice dotyczy wszystkich kontrolerów. Adnotacja przyjmuje selektory (wystarczy, że kontroler pasuje do
    // CHOĆ JEDNEGO podanego):
    //
    //     @RestControllerAdvice(assignableTypes = AdminController.class)          // konkretne klasy (i ich podklasy)
    //     @RestControllerAdvice(annotations = ReportsApi.class)                   // kontrolery z daną adnotacją-znacznikiem
    //     @RestControllerAdvice(basePackages = "com.sklep.legacy")                // kontrolery z pakietu (i podpakietów)
    //     @RestControllerAdvice(basePackageClasses = LegacyController.class)      // jak wyżej, ale bezpieczne przy zmianie nazwy pakietu
    //
    // Lekcja ma trzy zawężone advice obsługujące TEN SAM {@code IllegalArgumentException} (test narrowedAdvices):
    //   • /err03/admin/discount  → AdminAdvice   (assignableTypes) → 400 ADMIN_BAD_INPUT;
    //   • /err03/reports/sales   → ReportsAdvice (annotations)     → 422 REPORT_BAD_INPUT;
    //   • /err03/legacy/export   → LegacyAdvice  (basePackageClasses) → 400 LEGACY_BAD_INPUT.
    // Każdy kontroler dostaje tylko "swój" advice — nic się nie nakłada.
    //
    // Kiedy zawężać? Gdy część API ma inny kontrakt błędów (stary klient z innym formatem, panel administracyjny, osobny
    // moduł z własnymi wyjątkami). Gdy format jest wspólny — zostaw jeden advice bez selektora.
    //
    // PUŁAPKA: oczekiwanie, że advice z selektorem obsłuży WSZYSTKIE błędy "swojego" kontrolera. Dlaczego? Selektor działa po
    //   znanym kontrolerze. Błędy, które powstają, zanim Spring wybierze kontroler — zła metoda HTTP (405), nieznana
    //   ścieżka (404), zły {@code Content-Type} — nie mają kontrolera, więc zawężony advice ich nie widzi. Test
    //   narrowedAdviceSkipsErrorsBeforeTheController: {@code DELETE /err03/admin/discount} dostaje domyślne 405 z pustym ciałem,
    //   mimo że {@code AdminAdvice} istnieje. Jednolity format dla takich błędów daje tylko advice BEZ selektora (sekcja 4).
    //
    // DOBRA PRAKTYKA: przy selektorach wolisz {@code basePackageClasses} i {@code assignableTypes} (klasy) od {@code basePackages}
    //   (tekst). Dlaczego? Klasa jest sprawdzana przez kompilator i narzędzia refaktoryzacji; tekst z nazwą pakietu po zmianie
    //   nazwy pakietu po cichu przestaje cokolwiek obejmować.

    // =================================================================================================
    // 4. HANDLER "ŁAP WSZYSTKO": Exception.class — PO CO I JAK GO NIE ZEPSUĆ (FallbackAdvice, katalog err03_pitfalls)
    // =================================================================================================
    //
    // Cel: nieprzewidziany wyjątek (NullPointerException w serwisie, błąd sterownika bazy) ma dać klientowi ogólne 500 w naszym
    // formacie i zaowocować wpisem w logu — bez wycieku szczegółów (test fallbackHidesDetails: odpowiedź nie zawiera "hasło",
    // choć wyjątek je miał w komunikacie). Wyjątek wolno zalogować dopiero w tym handlerze, bo to JEDYNE miejsce, gdzie
    // widzimy "zaskoczenie".
    //
    // PUŁAPKA: naiwny handler "łap wszystko" połyka wyjątki Springa (test Err03NaiveCatchAllTest). Dlaczego? Przykład:
    //
    //     @ExceptionHandler(Exception.class) @ResponseStatus(INTERNAL_SERVER_ERROR) public String onAnything(Exception e) { … }
    //
    // Taki handler łapie WSZYSTKO, więc zamienia też wyjątki, które Spring sam zna i zamieniłby na 4xx: zła metoda HTTP (405), nieobsługiwany
    // typ treści (415), nieznana ścieżka (404 — wyjątek {@code NoResourceFoundException}), zepsuty JSON i zły typ parametru
    // (400). Klient robi pomyłkę w adresie, a dostaje 500; monitoring zgłasza "awarię serwera", która nie jest awarią.
    //
    // Poprawka widoczna w {@code FallbackAdvice} (tym samym handlerem, trzema gałęziami):
    //   a) {@code exception instanceof ErrorResponse} — interfejs wyjątków Springa, które same znają status
    //      ({@code HttpRequestMethodNotSupportedException}, {@code HttpMediaTypeNotSupportedException}, {@code NoResourceFoundException},
    //      {@code MethodArgumentNotValidException}…). Oddajemy ich status, a nawet nagłówki (np. {@code Allow: GET} przy 405);
    //   b) {@code HttpMessageNotReadableException} i {@code TypeMismatchException} — też 4xx, ale NIE są {@code ErrorResponse};
    //   c) reszta: 500, ogólny komunikat, szczegóły do logu.
    // Test fallbackKeepsSpringsOwnStatuses sprawdza wszystkie pięć przypadków: 405 (z Allow), 415, 404, zepsuty JSON i zły typ
    // parametru (/products/abc) → 400.
    // To działa, ale wymaga znajomości tych wyjątków i pilnowania listy. Standardowe rozwiązanie — dziedziczenie po
    // {@code ResponseEntityExceptionHandler} z gotową obsługą wszystkich wyjątków Springa (Err04ProblemDetail).
    //
    // PUŁAPKA: kolejność advice (test Err03GreedyOrderTest). Dlaczego? Advice z {@code Exception.class} i wysokim pierwszeństwem przejmuje wszystko. W teście
    // {@code GreedyAdvice} ma {@code @Order(HIGHEST_PRECEDENCE)}, a {@code ShopAdvice} obsługuje {@code ProductNotFoundException} — mimo
    // to klient dostaje 500, bo wygrywa PIERWSZY advice z jakimkolwiek pasującym handlerem, a {@code Exception.class} pasuje do
    // wszystkiego. Dlatego {@code FallbackAdvice} ma {@code @Order(Ordered.LOWEST_PRECEDENCE)} — musi być ostatni.
    //
    // PUŁAPKA: handler ostatniej szansy zwracający {@code e.getMessage()} klientowi. Dlaczego? Komunikaty wyjątków potrafią
    //   zawierać SQL, nazwy hostów i dane użytkowników (Err01DefaultErrors, sekcja 2). Do klienta idzie ogólny komunikat,
    //   do logu — pełny wyjątek jako OSTATNI argument ({@code log.error("…", exception)}), żeby zapisał się ślad stosu.
    //
    // DOBRA PRAKTYKA: loguj wyjątek dokładnie raz — w handlerze 5xx. Dlaczego? Logowanie go także w serwisach i kontrolerze
    //   daje trzy ślady stosu tego samego zdarzenia, a przy szukaniu przyczyny trzeba je mozolnie scalać.

    // =================================================================================================
    // 5. JEDEN SPÓJNY FORMAT BŁĘDU W CAŁYM API (ApiError, Err03ServerTest)
    // =================================================================================================
    //
    // Test oneFormatEverywhere przechodzi przez osiem różnych błędów (domenowe, panelu admina, raportów, nieznana ścieżka,
    // zły typ) i dla każdego sprawdza ten sam szkielet: {@code status} (liczba), {@code code} (tekst), {@code message} (tekst),
    // {@code path} (adres żądania). Test Err03ServerTest robi to na prawdziwym serwerze: nawet nieznana ścieżka i niezłapany
    // wyjątek mają format {@code ApiError} — advice przejął je PRZED kontrolerem {@code /error}, więc domyślny JSON Boota
    // z Err01DefaultErrors (z polem {@code timestamp}) już się nie pojawia.
    //
    // Dlaczego to ważne? Klient (aplikacja mobilna, front, inny serwis) pisze JEDNĄ funkcję obsługi błędów: sprawdza {@code code},
    // pokazuje {@code message}. Nie musi zgadywać, jaki kształt ma tym razem błąd. Format błędu jest częścią umowy API — tak
    // samo ważną jak format poprawnej odpowiedzi.
    //
    // Gotowy, STANDARDOWY szkielet dla takiego formatu to {@code ProblemDetail} (RFC 9457, Err04ProblemDetail): pola {@code type},
    // {@code title}, {@code status}, {@code detail}, {@code instance}. Nasze {@code ApiError} to uproszczony odpowiednik, dobry do
    // nauki mechanizmu; w nowym projekcie użyj raczej ProblemDetail.
    //
    // DOBRA PRAKTYKA: jeden rekord błędu (tu {@code ApiError}) używany przez wszystkie handlery i advice. Dlaczego? Gdy każdy
    //   advice tworzy własny kształt, "jednolity format" jest fikcją — pierwszy nowy handler zepsuje spójność.
    //   Dodatkowe pola (jak {@code fields}) są opcjonalne, a szkielet stały.
    //
    // DOBRA PRAKTYKA: testuj format błędu jedną pętlą po różnych adresach (jak oneFormatEverywhere). Dlaczego? Nowy
    //   endpoint z własnym wyjątkiem wpadnie do listy jednym wpisem, a test powie, czy jego odpowiedź trzyma szkielet.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • {@code @RestControllerAdvice} = {@code @ControllerAdvice} + {@code @ResponseBody}; metody {@code @ExceptionHandler} obowiązują
     *     wiele kontrolerów. Advice to zwykły bean.
     *   • Kolejność: handler w kontrolerze → advice wg {@code @Order} (pierwszy z pasującym handlerem) → wbudowane mechanizmy i /error.
     *   • W obrębie jednego advice wygrywa najbliższy typ wyjątku; MIĘDZY advice — kolejność {@code @Order} (mniejsza liczba = wcześniej).
     *   • Gdy kilka advice obsługuje te same wyjątki — zawsze {@code @Order}; bez niego kolejność nie jest gwarantowana.
     *   • Selektory: {@code assignableTypes}, {@code annotations}, {@code basePackages}/{@code basePackageClasses}. Zawężony advice nie widzi
     *     błędów sprzed wyboru kontrolera (405, nieznana ścieżka).
     *   • {@code Exception.class} = "łap wszystko": ostatni w kolejce ({@code LOWEST_PRECEDENCE}), przepuszczaj {@code ErrorResponse}
     *     ze statusem Springa (405, 415, 404…), zwracaj ogólny komunikat, loguj ślad stosu. Lepiej: Err04.
     *   • {@code @WebMvcTest} ładuje wszystkie {@code @ControllerAdvice} aplikacji.
     *   • Jeden format błędu dla całego API (status, code, message, path) = umowa z klientem.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się {@code @RestControllerAdvice} od {@code @ControllerAdvice}? Gdzie w kodzie widać, że wynik
     *      handlera zamienia się na JSON?
     *   2. Co się stanie? Kontroler ma własny handler dla {@code ProductNotFoundException}, a w {@code @RestControllerAdvice} jest drugi
     *      handler dla tego samego wyjątku. Który odpowie klientowi?
     *   3. Co się stanie? Dwa advice obsługują ten sam wyjątek; jeden ma {@code @Order(1)}, drugi {@code @Order(2)}. Który wygra
     *      i czy ma znaczenie nazwa klasy?
     *   4. Co się stanie? Dodajesz advice z {@code @ExceptionHandler(Exception.class)}, zwracający zawsze 500, i klient
     *      robi {@code DELETE} na endpoint przyjmujący tylko {@code GET}. Jaki status dostanie?
     *   5. ZNAJDŹ BŁĄD: {@code @RestControllerAdvice @Order(Ordered.HIGHEST_PRECEDENCE)} z handlerem {@code Exception.class} obok szczegółowego
     *      advice dla {@code ProductNotFoundException}. Klienci narzekają, że zawsze dostają 500. Dlaczego i jak naprawić?
     *   6. ZNAJDŹ BŁĄD: {@code @RestControllerAdvice(assignableTypes = AdminController.class)} obsługuje {@code IllegalArgumentException}, ale
     *      {@code DELETE /admin/discount} (zła metoda) nadal zwraca 405 z pustym ciałem. Dlaczego advice go nie obsługuje?
     *   7. Co się stanie? W handlerze "łap wszystko" zwracasz klientowi {@code exception.getMessage()}. Dlaczego to ryzykowne?
     *   8. Jak sprawdzisz w {@code @WebMvcTest}, że wszystkie błędy API mają ten sam szkielet? Które advice załaduje
     *      {@code @WebMvcTest(ProductController.class)}?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielet: ExerciseController, ExerciseAdvice, ExerciseVipController, ExerciseUrgentAdvice, ExerciseAdvisoryAdvice;
    // testy: Err03ControllerAdviceExercisesTest (@Tag("cwiczenie"))
    // =================================================================================================
    //
    // Rozwiązania wzorcowe: pakiet s06_error_handling.solutions.err03_controller_advice. Wszystkie adresy mają prefiks /err03/cw.
    // Test ćwiczeń buduje kontekst ręcznie (bez advice z lekcji), więc Twoje handlery nie walczą z lekcją.
    // Format ciała błędu: rekord ApiError (pakiet lekcji).
    //
    // ĆWICZENIE 1 (łatwe): klient — w ExerciseController.customer dla id 1 zwróć "Anna", dla innych rzuć CustomerNotFoundException.
    //   W ExerciseAdvice dopisz handler → 404, code "CUSTOMER_NOT_FOUND", message z wyjątku, path z HttpServletRequest.
    //   Podpowiedź: wzoruj się na ShopAdvice.onProductNotFound.
    //
    // ĆWICZENIE 2 (łatwe): zamówienie — cancel zawsze rzuca OrderLockedException. Handler w ExerciseAdvice → 409 ORDER_LOCKED.
    //   Podpowiedź: @ResponseStatus(HttpStatus.CONFLICT) nad metodą handlera.
    //
    // ĆWICZENIE 3 (średnie): wyjątek od reguły — w ExerciseVipController.vip rzuć CustomerNotFoundException i dopisz w tym kontrolerze
    //   LOKALNY handler → 404, code "VIP_NOT_FOUND", message "Nie ma klienta VIP <id>". /cw/customers/9 ma nadal dawać
    //   CUSTOMER_NOT_FOUND z advice. Podpowiedź: id weź z adresu (request.getRequestURI()) albo dodaj getter do wyjątku.
    //
    // ĆWICZENIE 4 (średnie): "łap wszystko" — w ExerciseController.crash rzuć IllegalStateException z "tajnym" komunikatem. W ExerciseAdvice
    //   dopisz handler Exception.class → 500, code "UNEXPECTED", message "Wystąpił nieoczekiwany błąd"; wyjątek zaloguj
    //   (log.error z wyjątkiem jako ostatnim argumentem). Odpowiedź nie może zawierać tajnego tekstu.
    //
    // ĆWICZENIE 5 (trudniejsze): kolejność — ExerciseController.priority rzuca ExercisePriorityException, którą obsługują DWA advice:
    //   ExerciseUrgentAdvice (ma zwyciężyć, kod URGENT) i ExerciseAdvisoryAdvice (kod ADVISORY). Dodaj @Order do obu klas,
    //   a w ExerciseAdvice zostaw LOWEST_PRECEDENCE. Podpowiedź: pamiętaj, że mniejsza liczba = wyższe pierwszeństwo, a
    //   handler Exception.class z ćwiczenia 4 nie może wyprzedzić Urgent.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. {@code @RestControllerAdvice} to {@code @ControllerAdvice} z dodaną {@code @ResponseBody}: wartości zwracane z handlerów trafiają
     *      do ciała odpowiedzi jako JSON (Jackson), a nie są traktowane jako nazwa widoku. Widać to w definicji adnotacji
     *      (meta-adnotacje {@code @ControllerAdvice} i {@code @ResponseBody}).
     *   2. Handler lokalny z kontrolera (w teście localHandlerBeatsAdvice: LOCAL_NOT_FOUND). Globalny zadziała tylko dla
     *      kontrolerów, które sami nie obsłużyli tego wyjątku.
     *   3. Wygra advice z {@code @Order(1)} — mniejsza liczba = wyższe pierwszeństwo. Nazwa klasy nie ma znaczenia (test
     *      orderDecidesBetweenAdvices: PrimaryAdvice wygrywa, choć AlternativeAdvice jest alfabetycznie pierwszy).
     *   4. 500 zamiast 405. {@code HttpRequestMethodNotSupportedException} (405) też jest {@code Exception}, więc naiwny handler ją
     *      połyka i traci się nagłówek {@code Allow} (test methodNotAllowedBecomes500). Poprawka: {@code instanceof ErrorResponse}
     *      i oddanie statusu Springa (test fallbackKeepsSpringsOwnStatuses) albo {@code ResponseEntityExceptionHandler} (Err04).
     *   5. Spring bierze pierwszy advice (wg {@code @Order}), który ma jakikolwiek pasujący handler; {@code Exception.class} pasuje do
     *      wszystkiego, a przy {@code HIGHEST_PRECEDENCE} jest pierwszy, więc szczegółowy advice nigdy się nie wykonuje
     *      (test Err03GreedyOrderTest). Naprawa: handler ogólny na koniec — {@code @Order(Ordered.LOWEST_PRECEDENCE)}.
     *   6. Advice z selektorem {@code assignableTypes} dotyczy tylko błędów z konkretnego kontrolera. 405 powstaje, zanim Spring
     *      wybierze kontroler (nie ma jeszcze "kontrolera, który rzucił"), więc zawężony advice go nie widzi (test
     *      narrowedAdviceSkipsErrorsBeforeTheController). Jednolity format dla 405 daje advice bez selektora.
     *   7. Komunikaty wyjątków pochodzą z kodu i bibliotek (SQL, nazwy hostów, ścieżki plików, dane użytkowników) i trafiłyby
     *      do klienta — wyciek informacji. Klient dostaje ogólny tekst, szczegóły idą do logu (test fallbackHidesDetails).
     *   8. Pętlą po kilku adresach błędów, z asercją na status/code/message/path (test oneFormatEverywhere). {@code @WebMvcTest
     *      (ProductController.class)} załaduje ten kontroler i WSZYSTKIE advice z aplikacji (ShopAdvice, FallbackAdvice, Admin…),
     *      ale nie pozostałe kontrolery (test Err03AdviceLoadedTest).
     */
    // </editor-fold>
}
