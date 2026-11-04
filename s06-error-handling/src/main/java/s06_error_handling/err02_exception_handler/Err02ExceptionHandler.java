package s06_error_handling.err02_exception_handler;

/**
 * <pre>
 * TEMAT: @ExceptionHandler — własna obsługa wyjątku w kontrolerze
 *        (exception handler = procedura obsługi wyjątku; hierarchy = hierarchia dziedziczenia klas; catch-all = "łap
 *         wszystko", handler dla najogólniejszego typu; response = odpowiedź; resolver = rozwiązywacz, element Springa,
 *         który próbuje zamienić wyjątek na odpowiedź)
 *
 * W SKRÓCIE:
 *   Metoda kontrolera oznaczona {@code @ExceptionHandler(TenWyjatek.class)} zostaje wywołana, gdy inna metoda TEGO
 *   SAMEGO kontrolera rzuci ten wyjątek (albo jego podklasę). Handler sam buduje odpowiedź: wybiera status, ciało i
 *   nagłówki. Dzięki temu klient dostaje przemyślany JSON z właściwym kodem (404, 409…) zamiast ogólnego 500 z
 *   Err01DefaultErrors. Wyjątek domenowy ("nie ma produktu") nie musi nic wiedzieć o HTTP — mapowanie na kod robi dopiero handler.
 *   Ograniczenie: handler w kontrolerze działa tylko dla tego kontrolera. Dlatego w praktyce prawie zawsze kończy się na
 *   Err03ControllerAdvice, ale mechanizm jest ten sam — warto go zrozumieć w małej skali.
 *
 * ANALOGIA: dział reklamacji w jednym sklepie.
 *   Sprzedawca (metoda kontrolera) nie umie rozwiązać problemu — "tego towaru nie ma". Zamiast wzruszyć ramionami
 *   (500), woła kierownika zmiany (handler) w TYM SAMYM sklepie. Kierownik wie, jak przeprosić klienta, wystawić
 *   protokół (ciało błędu) i wydać właściwy dokument (kod statusu). Kierownik z innego sklepu nie przyjdzie — jeśli w
 *   drugim sklepie (innym kontrolerze) zepsuje się to samo, nikt nie pomoże, aż zatrudnimy kierownika dla całej sieci
 *   (Err03ControllerAdvice).
 *
 * JAK TO DZIAŁA:
 *   1. Metoda kontrolera rzuca wyjątek (np. {@code ProductNotFoundException}).
 *   2. {@code DispatcherServlet} nie dostaje wyniku, więc pyta po kolei "rozwiązywaczy wyjątków"
 *      ({@code HandlerExceptionResolver}). Tu liczy się {@code ExceptionHandlerExceptionResolver}.
 *   3. Szuka metody z {@code @ExceptionHandler} najpierw w klasie kontrolera, który rzucił wyjątek (a potem w
 *      {@code @ControllerAdvice} — Err03). Wybiera handler z NAJBLIŻSZYM typem w hierarchii dziedziczenia.
 *   4. Buduje argumenty metody handlera: wyjątek, {@code HttpServletRequest}, {@code WebRequest} i inne.
 *   5. Wywołuje handler. Wartość zwrócona przechodzi tę samą drogę co w zwykłej metodzie kontrolera: {@code ResponseEntity}
 *      daje status i ciało, a zwykły obiekt jest zamieniany na JSON (Jackson) i dostaje status 200 albo ten z {@code @ResponseStatus}.
 *   6. Nie ma handlera albo handler sam rzucił wyjątek? Resolver oddaje sprawę dalej: {@code ResponseStatusExceptionResolver},
 *      {@code DefaultHandlerExceptionResolver} (m.in. 400/405/415), a na końcu Tomcat i {@code /error} (Err01).
 *
 * SŁÓWKA:
 *   handler = procedura obsługi; nearest = najbliższy; superclass/supertype = nadklasa/nadtyp; subclass = podklasa;
 *   swallow = połknąć (obsłużyć coś, czego obsłużyć nie powinniśmy); stable code = stały, maszynowy kod błędu;
 *   audit log = dziennik zdarzeń.
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest03Responses (ResponseEntity, @ResponseStatus),
 *             s05_validation/Val02ValidInControllers (MethodArgumentNotValidException, HandlerMethodValidationException),
 *             s06_error_handling/Err01DefaultErrors (co dzieje się, gdy handlera nie ma),
 *             s06_error_handling/Err03ControllerAdvice (ten sam handler, ale dla wszystkich kontrolerów),
 *             s06_error_handling/Err04ProblemDetail (standardowy format ciała błędu),
 *             s06_error_handling/Err05CustomExceptions (hierarchia własnych wyjątków sklepu),
 *             t10_exceptions/Exceptions05CustomExceptions (własne wyjątki w Javie),
 *             t10_exceptions/Exceptions02CheckedUnchecked (wyjątki sprawdzane i niesprawdzane).
 * </pre>
 */
public final class Err02ExceptionHandler {

    private Err02ExceptionHandler() {
    }

    // =================================================================================================
    // 1. @ExceptionHandler W KONTROLERZE: 404 Z WŁASNYM CIAŁEM (ShopController, ApiError)
    // =================================================================================================
    //
    // PRZED (Err01DefaultErrors): wyjątek bez obsługi → 500 i ogólny JSON z {@code /error} — nawet dla błędu klienta.
    //
    // PO: w kontrolerze dopisujemy metodę:
    //
    //     @ExceptionHandler(ProductNotFoundException.class)
    //     public ResponseEntity<ApiError> onProductNotFound(ProductNotFoundException exception, HttpServletRequest request) {
    //         ApiError body = new ApiError(404, "PRODUCT_NOT_FOUND", exception.getMessage(), request.getRequestURI());
    //         return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    //     }
    //
    // Wynik (test handlerBuildsOwnBody): status 404 i ciało
    // {@code {"status":404,"code":"PRODUCT_NOT_FOUND","message":"Nie ma produktu o id 9","path":"/err02/shop/products/9"}}.
    //
    // Klient dostał trzy rzeczy, których w domyślnym ciele nie było:
    //   • {@code message} — opis dla człowieka (tu napisany przez nas, więc świadomie bezpieczny);
    //   • {@code code} — STAŁY kod maszynowy. Klient (aplikacja mobilna, front) może na nim oprzeć warunek
    //     ({@code if (code == PRODUCT_NOT_FOUND)}), a komunikat wolno nam zmieniać i tłumaczyć;
    //   • właściwy status 404 zamiast 500.
    //
    // Samo metoda kontrolera {@code product(long id)} rzuca tylko {@code new ProductNotFoundException(id)}: nie zna HTTP.
    // Odpowiedzialności są rozdzielone — wyjątek opisuje, CO się stało w sklepie, handler decyduje, JAK to pokazać klientowi.
    // Dzięki temu ten sam wyjątek można rzucić z serwisu (s05_validation/Val05ServiceValidation), a serwis
    // nie ma importu z pakietu web.
    //
    // DOBRA PRAKTYKA: w ciele błędu zawsze podawaj stały kod maszynowy ({@code code}), nie tylko tekst. Dlaczego? Tekst jest
    //   dla człowieka i zmieni się przy poprawce literówki albo tłumaczeniu; klient, który porównuje tekst, wywróci
    //   się przy najmniejszej zmianie. Kod jest umową.
    //
    // DOBRA PRAKTYKA: wyjątek domenowy nie zna HTTP (nie importuje {@code HttpStatus}). Dlaczego? Warstwa biznesowa ma się
    //   dać użyć w kolejce komunikatów, w teście bez Springa czy w zadaniu wsadowym — tam żaden kod HTTP nie istnieje.

    // =================================================================================================
    // 2. PARAMETRY I TYP ZWRACANY HANDLERA (ShopController.onCardDeclined)
    // =================================================================================================
    //
    // Handler to prawie zwykła metoda kontrolera — Spring wstrzykuje do niej argumenty ze znanej listy:
    //   • wyjątek (pierwszy parametr; typ musi pasować do wyjątków z adnotacji);
    //   • {@code HttpServletRequest} / {@code HttpServletResponse} — żądanie i odpowiedź serwletowa (adres: {@code getRequestURI()});
    //   • {@code WebRequest} — to samo, ale niezależne od serwletów; {@code getDescription(false)} daje tekst "uri=/adres";
    //   • {@code Locale}, {@code Principal}, {@code HandlerMethod} (metoda kontrolera, która rzuciła wyjątek) i inne.
    //   Nie ma {@code @RequestBody} — ciała żądania nie czyta się w handlerze.
    //
    // Co może zwrócić:
    //   • {@code ResponseEntity<T>} — pełna kontrola: status, nagłówki, ciało (onProductNotFound);
    //   • zwykły obiekt (rekord {@code ApiError}, mapa) + {@code @ResponseStatus(HttpStatus.XYZ)} nad metodą (onCardDeclined) —
    //     krócej, gdy status jest stały; test returnTypesAndParameters (402);
    //   • {@code void} / {@code ProblemDetail} / {@code ModelAndView} — rzadziej (ProblemDetail → Err04).
    //
    // PUŁAPKA: handler zwracający zwykły obiekt BEZ {@code @ResponseStatus} (i bez {@code ResponseEntity}). Dlaczego? Handler
    //   zachowuje się jak zwykła metoda kontrolera, a jej domyślny status to 200 OK. Klient dostaje "200 OK" z opisem błędu
    //   w ciele (metoda onCouponExpiredForgotStatus; test forgottenStatusMeans200). Skrypty i proxy widzą sukces, a monitoring
    //   nie zlicza błędu. Status w ciele (pole {@code status}) NIE zmienia kodu odpowiedzi.
    //
    // DOBRA PRAKTYKA: dla handlera ze stałym statusem używaj {@code @ResponseStatus}, a dla zmiennego — {@code ResponseEntity};
    //   w teście zawsze sprawdzaj KOD odpowiedzi ({@code status().isNotFound()}), nie tylko ciało. Dlaczego? Kod jest
    //   tym, co czyta każdy klient HTTP, zanim zajrzy do ciała.

    // =================================================================================================
    // 3. KTÓRY HANDLER WYGRYWA: NAJBLIŻSZY TYP; KILKA TYPÓW W JEDNEJ ADNOTACJI
    // =================================================================================================
    //
    // Hierarchia w lekcji: CardDeclinedException dziedziczy po PaymentException, a wszystko po RuntimeException.
    // Handlery w kontrolerze: dla CardDeclinedException (402), dla PaymentException (400), dla RuntimeException (500).
    //
    //     CardDeclinedException  ─► handler dla CardDeclinedException   (najbliższy: ten sam typ)
    //     PaymentException       ─► handler dla PaymentException        (RuntimeException jest dalej)
    //     IllegalStateException  ─► handler dla RuntimeException        (nikt bliższy)
    //
    // Test nearestTypeWins: karta "0000" daje 402 CARD_DECLINED, karta "1111" daje 400 PAYMENT_ERROR. Reguła: Spring mierzy
    // "odległość" w hierarchii klas i wybiera najmniejszą. Kolejność metod w pliku nie ma znaczenia.
    //
    // Kilka wyjątków, jeden handler — typy w klamrach (onWarehouseProblem):
    //
    //     @ExceptionHandler({OutOfStockException.class, WarehouseClosedException.class})
    //     public ApiError onWarehouseProblem(RuntimeException exception, …)
    //
    // Typ parametru musi być wspólnym nadtypem wymienionych klas (tu {@code RuntimeException}). Gdy wpiszesz konkretny typ,
    // a przyjdzie drugi — Spring nie zbuduje argumentu i handler nie zadziała. Wtedy {@code instanceof} (lub
    // pattern matching w Javie 21) rozróżnia przypadki. Test severalTypesInOneAnnotation: obie sytuacje to 409, z kodami
    // OUT_OF_STOCK i WAREHOUSE_CLOSED. Bez listy typów w adnotacji, ale z parametrem, Spring bierze typ z parametru metody.
    //
    // Handler "ostatniej szansy" ({@code RuntimeException}, onUnexpected) zwraca ogólny komunikat "Wystąpił nieoczekiwany błąd",
    // a prawdziwy wyjątek zapisuje w logu ({@code log.error(…, exception)} — trzeci argument to wyjątek, żeby pojawił się
    // ślad stosu). Test catchAllForTheController: odpowiedź nie zawiera "hasło", choć wyjątek je miał w komunikacie.
    //
    // PUŁAPKA: handler dla {@code RuntimeException} (albo {@code Exception}) łapie też wyjątki, które Spring umiał obsłużyć
    //   sam. Dlaczego? Spring MVC zgłasza np. zepsuty JSON jako {@code HttpMessageNotReadableException} — a ona jest
    //   {@code RuntimeException}, więc Twój ogólny handler ją połyka. Efekt (test catchAllSwallowsSpringsOwn400): zepsuty
    //   JSON daje 500 UNEXPECTED zamiast 400. Błąd klienta wygląda jak awaria serwera, a w logu pojawia się fałszywy alarm.
    //   (405 Method Not Allowed handler {@code RuntimeException} w tym kontrolerze akurat nie złapie — to nie jest
    //   {@code RuntimeException} — ale handler dla {@code Exception} już tak, o tym w Err03.) Rozwiązanie: wąskie handlery albo
    //   dziedziczenie po {@code ResponseEntityExceptionHandler} (Err04).
    //
    // PYTANIE REKRUTACYJNE: który {@code @ExceptionHandler} zostanie wywołany, gdy pasuje kilka? (Ten z NAJBLIŻSZYM typem w
    //   hierarchii dziedziczenia względem rzuconego wyjątku; handler z kontrolera ma pierwszeństwo przed globalnym — Err03.)

    // =================================================================================================
    // 4. WALIDACJA W HANDLERZE: MAPA POLE → KOMUNIKAT (rozwinięcie zapowiedzi z Val02ValidInControllers)
    // =================================================================================================
    //
    //     @ExceptionHandler(MethodArgumentNotValidException.class)
    //     @ResponseStatus(HttpStatus.BAD_REQUEST)
    //     public Map<String, String> onInvalidBody(MethodArgumentNotValidException exception) {
    //         Map<String, String> messages = new TreeMap<>();
    //         for (FieldError error : exception.getBindingResult().getFieldErrors()) { messages.merge(error.getField(), …); }
    //         return messages;
    //     }
    //
    // Wynik (test validationBodyToSortedMap): 400 i {@code {"name":"Nazwa jest wymagana","quantity":"Ilość musi wynosić co najmniej 1"}}.
    //   • {@code TreeMap} sortuje klucze, więc odpowiedź ma zawsze tę samą kolejność (testy nie migoczą, klient ma powtarzalny wynik);
    //   • {@code merge(klucz, komunikat, (pierwszy, drugi) -> pierwszy + "; " + drugi)} łączy komunikaty, gdy jedno pole łamie kilka reguł
    //     (bez {@code merge} ostatni komunikat nadpisałby poprzednie).
    //
    // Druga strona (test validationOnParameterNeedsSecondHandler): metoda {@code replace} ma ograniczenie przy parametrze
    // ({@code @PathVariable @Min(1) long id}). Wtedy WSZYSTKIE błędy walidacji tej metody — także błędy ciała — przychodzą
    // jako {@code HandlerMethodValidationException}, a handler dla {@code MethodArgumentNotValidException} milczy.
    // Drugi handler zbiera {@code getParameterValidationResults()} → {@code getResolvableErrors()}: dla pola ciała jest to {@code FieldError}
    // (nazwa pola), dla parametru ścieżki — nazwa parametru ({@code getMethodParameter().getParameterName()}, np. "id").
    // Wynik: {@code {"id":"Id musi być dodatnie","name":"Nazwa jest wymagana"}}.
    //
    // Zepsuty JSON ({@code HttpMessageNotReadableException}) to jeszcze inny wyjątek i też zasługuje na własny handler
    // (Err04 pokaże, jak obsłużyć wszystkie standardowe wyjątki Springa jednym mechanizmem).
    //
    // DOBRA PRAKTYKA: błędy walidacji zwracaj w formie "pole → komunikat" (albo listy takich par), nigdy jako jeden długi
    //   tekst. Dlaczego? Formularz we froncie musi wiedzieć, które pole podświetlić na czerwono, a to wymaga struktury.

    // =================================================================================================
    // 5. PUŁAPKI: HANDLER DZIAŁA TYLKO W SWOIM KONTROLERZE; WYJĄTEK W SAMYM HANDLERZE
    // =================================================================================================
    //
    // 5a. Handler w kontrolerze ma zasięg jednego kontrolera. OtherController rzuca TEN SAM ProductNotFoundException, ale
    //     nie ma handlera (testy: Err02OtherControllerTest, Err02ServerTest):
    //        • w MockMvc wyjątek wylatuje z {@code perform(…)} jako {@code ServletException};
    //        • na prawdziwym serwerze klient dostaje 500 i ogólny JSON z /error — bez code i message.
    //     Kopiowanie handlera do każdego kontrolera to zła droga: powtórzenia rozjadą się z czasem. Docelowo jedno miejsce — Err03.
    //
    // 5b. Wyjątek rzucony w samym handlerze (onBrokenData rzuca IllegalStateException). Spring zapisuje ostrzeżenie
    //     "Failure in @ExceptionHandler …" w logu i... zapomina o handlerze: dalej leci ORYGINALNY wyjątek
    //     ({@code BrokenDataException}), a nie ten z handlera. Nikt inny go nie obsłuży, więc na serwerze klient dostaje 500 z /error,
    //     a w MockMvc test dostaje {@code ServletException} z przyczyną {@code BrokenDataException} (test exceptionInsideHandler).
    //     Wniosek: handler musi być prosty i niezawodny — żadnych zapytań do bazy, żadnego parsowania, żadnego {@code get(0)} na liście.
    //
    // PUŁAPKA: logika w handlerze, która sama może się wywrócić (zapytanie do bazy, odwołanie do pola, które bywa null).
    //   Dlaczego? Wyjątek w handlerze nie jest obsługiwany ponownie — tracisz zaplanowany kod i komunikat, klient widzi
    //   ogólne 500, a w logu są dwa ślady stosu do rozplątania. Handler ma tylko zamienić gotowe dane wyjątku na odpowiedź.
    //
    // DOBRA PRAKTYKA: w handlerze loguj wyjątki 5xx ze śladem stosu ({@code log.error("…", exception)}), a 4xx co najwyżej
    //   krótką linią. Dlaczego? 5xx to błąd naszego kodu — szczegóły są potrzebne do naprawy; 4xx to błąd klienta i
    //   pełne ślady stosu zalewałyby logi (i alarmy) przy każdej literówce w adresie.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • {@code @ExceptionHandler(X.class)} na metodzie kontrolera = obsługa wyjątku X (i podklas) rzuconego w TYM kontrolerze.
     *   • Handler buduje odpowiedź: {@code ResponseEntity<ApiError>} (status zmienny) albo rekord + {@code @ResponseStatus}.
     *   • Bez {@code @ResponseStatus} i {@code ResponseEntity} odpowiedź to 200 OK — nawet z opisem błędu w ciele!
     *   • Argumenty: wyjątek, {@code HttpServletRequest}, {@code WebRequest}, {@code Locale}, {@code HandlerMethod}… Nie ma {@code @RequestBody}.
     *   • Wygrywa NAJBLIŻSZY typ w hierarchii; kolejność metod w pliku nie ma znaczenia.
     *   • Kilka typów: {@code @ExceptionHandler({A.class, B.class})}, parametr = wspólny nadtyp.
     *   • Handler dla {@code RuntimeException}/{@code Exception} połyka też wyjątki Springa (zepsuty JSON 400 → 500).
     *   • Walidacja: {@code MethodArgumentNotValidException} (ciało) i {@code HandlerMethodValidationException} (parametry) — dwa handlery.
     *   • Handler działa tylko w swoim kontrolerze → globalnie: Err03. Wyjątek w handlerze → oryginał leci dalej (500).
     *   • Wyjątek domenowy nie zna HTTP; w ciele błędu stały {@code code} + czytelny {@code message}.
     *
     * PYTANIA KONTROLNE:
     *   1. Co robi {@code @ExceptionHandler} i dla jakich kontrolerów działa? Które pole w ciele błędu jest umową
     *      z klientem — {@code message} czy {@code code}?
     *   2. Co się stanie? Kontroler ma handler dla {@code RuntimeException} i dla {@code ProductNotFoundException}
     *      (podklasa {@code RuntimeException}). Metoda rzuca {@code ProductNotFoundException}. Który handler się wykona?
     *   3. Co się stanie? Handler dla {@code RuntimeException} istnieje w kontrolerze, a klient wysyła zepsuty JSON
     *      ({@code {zepsuty}) do metody z {@code @RequestBody}. Jaki status dostanie?
     *   4. ZNAJDŹ BŁĄD: handler zwraca {@code new ApiError(410, "COUPON_EXPIRED", …)} bez żadnych adnotacji nad metodą, a
     *      klient (i monitoring) widzą w logach same "200 OK". Dlaczego i jak to naprawić?
     *   5. ZNAJDŹ BŁĄD: istnieje handler dla {@code MethodArgumentNotValidException}, a PUT z {@code @PathVariable @Min(1) long id}
     *      i złym ciałem nadal nie zwraca mapy pól. Dlaczego?
     *   6. Co się stanie? {@code OtherController} rzuca {@code ProductNotFoundException}, a handler jest tylko w
     *      {@code ShopController}. Co zobaczy klient na prawdziwym serwerze, a co test w MockMvc?
     *   7. Co się stanie? Handler dla {@code BrokenDataException} sam rzuca {@code IllegalStateException}. Jaki wyjątek
     *      zobaczy test w MockMvc i jaki status klient na serwerze?
     *   8. Dlaczego wyjątek domenowy ({@code ProductNotFoundException}) nie powinien mieć w sobie kodu 404?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielet: ExerciseController, testy: Err02ExceptionHandlerExercisesTest (@Tag("cwiczenie"))
    // =================================================================================================
    //
    // Rozwiązania wzorcowe: Err02SolutionsController (s06_error_handling.solutions.err02_exception_handler).
    // Punkty końcowe (/err02/cw/…) i wyjątki są w szkielecie gotowe; w każdym ćwiczeniu uzupełniasz ciało metody
    // kontrolera (rzucenie wyjątku) ORAZ dopisujesz metodę-handler (komentarz na końcu klasy opisuje oczekiwany wynik).
    // Format ciała błędu: rekord ApiError z tej lekcji.
    //
    // ĆWICZENIE 1 (łatwe): klient — dla id 1 zwróć "Anna", dla innych rzuć CustomerNotFoundException. Dopisz handler zwracający
    //   ResponseEntity<ApiError> z kodem 404, code "CUSTOMER_NOT_FOUND", message z wyjątku i path z żądania.
    //   Podpowiedź: @ExceptionHandler(CustomerNotFoundException.class), HttpServletRequest.getRequestURI().
    //
    // ĆWICZENIE 2 (łatwe): zamówienie — ship zawsze rzuca OrderAlreadyShippedException. Handler: rekord ApiError ze statusem 409
    //   z @ResponseStatus, code "ALREADY_SHIPPED". Podpowiedź: sprawdź w teście KOD odpowiedzi, nie tylko ciało (pułapka sekcji 2).
    //
    // ĆWICZENIE 3 (średnie): faktury — "stare" → InvoiceExpiredException, "bledna" → InvoiceException("Faktura jest błędna").
    //   Dwa handlery: InvoiceException → 400 INVOICE_ERROR; InvoiceExpiredException (podklasa!) → 410 INVOICE_EXPIRED.
    //   Podpowiedź: nie musisz pilnować kolejności metod; wygrywa najbliższy typ.
    //
    // ĆWICZENIE 4 (średnie): klient z walidacją — dodaj @Valid przy ciele w create i handler MethodArgumentNotValidException →
    //   400 i TreeMap pole → komunikat (komunikaty jednego pola łącz tekstem "; "). Podpowiedź: Map.merge albo Collectors.toMap.
    //
    // ĆWICZENIE 5 (trudniejsze): magazyn — "closed" → WarehouseClosedException, "overloaded" → WarehouseOverloadedException.
    //   Jeden handler dla obu typów: 503, code = exception.getClass().getSimpleName(), message "Magazyn niedostępny".
    //   Podpowiedź: @ExceptionHandler({A.class, B.class}); parametr handlera to wspólny nadtyp (RuntimeException).

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Zamienia wyjątek rzucony w kontrolerze na zaplanowaną odpowiedź (status, ciało). Działa tylko dla kontrolera, w
     *      którym leży (dla wszystkich: @RestControllerAdvice, Err03). Umową z klientem jest {@code code} (stały, maszynowy);
     *      {@code message} jest dla człowieka i może się zmieniać.
     *   2. Handler dla {@code ProductNotFoundException} (404). Wygrywa najbliższy typ w hierarchii, a nie kolejność metod
     *      (test handlerBuildsOwnBody / catchAllForTheController).
     *   3. 500 z kodem UNEXPECTED zamiast 400. {@code HttpMessageNotReadableException} jest {@code RuntimeException}, więc ogólny
     *      handler ją połknął (test catchAllSwallowsSpringsOwn400).
     *   4. Handler zachowuje się jak zwykła metoda kontrolera, a ta domyślnie zwraca 200 OK; status w ciele nie zmienia kodu
     *      odpowiedzi (test forgottenStatusMeans200). Naprawa: dodaj {@code @ResponseStatus(HttpStatus.GONE)} nad metodą
     *      albo zwróć {@code ResponseEntity.status(HttpStatus.GONE).body(…)}.
     *   5. Ograniczenie na parametrze sprawia, że walidacja idzie drogą method validation i rzuca
     *      {@code HandlerMethodValidationException} — także dla błędów ciała. Potrzebny jest drugi handler (test
     *      validationOnParameterNeedsSecondHandler; zobacz Val02ValidInControllers, sekcja 4).
     *   6. Klient: 500 i ogólny JSON z /error, bez code i message. Test w MockMvc: {@code ServletException} z przyczyną
     *      {@code ProductNotFoundException} (testy Err02ServerTest i Err02OtherControllerTest).
     *   7. Test w MockMvc: {@code ServletException} z przyczyną {@code BrokenDataException} — czyli ORYGINALNY wyjątek, nie ten
     *      z handlera. Klient na serwerze dostaje 500 z /error (testy exceptionInsideHandler i brokenHandlerOnRealServer).
     *   8. Wiązałby warstwę biznesową z HTTP: serwis nie dałby się użyć w zadaniu wsadowym czy kolejce komunikatów, a ten sam
     *      wyjątek nie mógłby dawać innego kodu w innym kontekście. Kod wybiera warstwa web (handler).
     */
    // </editor-fold>
}
