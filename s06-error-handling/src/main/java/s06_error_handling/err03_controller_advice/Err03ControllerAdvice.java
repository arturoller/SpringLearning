package s06_error_handling.err03_controller_advice;

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * TEMAT: Globalna obsługa błędów — @RestControllerAdvice, zakres, kolejność (@Order) i pierwszeństwo lokalnych handlerów
 *        (controller advice = „porada dla kontrolerów”, czyli wspólna klasa z logiką dla wielu kontrolerów;
 *         scope = zakres; order = kolejność; precedence = pierwszeństwo)
 *
 * W SKRÓCIE:
 *   Zamiast powtarzać @ExceptionHandler w każdym kontrolerze, piszesz JEDNĄ klasę z @RestControllerAdvice. Jej metody
 *   @ExceptionHandler obsługują wyjątki ze WSZYSTKICH kontrolerów (albo tylko z wybranych — zakres). Gdy advice jest
 *   kilka, o wyniku decyduje @Order; handler z samego kontrolera zawsze wygrywa z advice.
 *
 * ANALOGIA: dział reklamacji w sieci sklepów.
 *   Każdy sklep (kontroler) może rozpatrzyć reklamację u siebie na miejscu (handler lokalny). Jeśli nie umie, sprawa
 *   idzie do centrali (advice). W centrali jest kilka działów w ustalonej kolejności (@Order): dział płatności, dział
 *   zamówień (tylko dla sklepów internetowych — zakres), a na końcu ogólny „biuro obsługi klienta” (catch-all).
 *   Sprawę przejmuje pierwszy dział, który się nią zajmuje.
 *
 * JAK TO DZIAŁA:
 *   wyjątek w kontrolerze → ExceptionHandlerExceptionResolver szuka handlera:
 *     1) w klasie kontrolera                           ← wygrywa zawsze, jeśli pasuje
 *     2) w beanach @ControllerAdvice, w kolejności @Order (mniejsza liczba = wcześniej); bez @Order = na końcu
 *        – advice z ograniczonym zakresem jest pomijany, jeśli kontroler nie należy do zakresu
 *        – w ramach jednego advice: najbliższy typ wyjątku; między advice: pierwszy, który MA pasujący handler
 *   nic nie pasuje → dalej ResponseStatusExceptionResolver, Default... (Err02ExceptionHandler) → /error (Err01DefaultErrors)
 *
 * SŁÓWKA:
 *   advice = porada (tu: wspólna klasa pomocnicza kontrolerów); assignableTypes = typy przypisywalne (konkretne klasy
 *   kontrolerów); basePackageClasses = pakiety wskazane przez klasy; basePackages = pakiety (teksty); annotations =
 *   kontrolery oznaczone daną adnotacją; precedence = pierwszeństwo; catch-all = „łap wszystko”; log = dziennik;
 *   fallback = rozwiązanie awaryjne; override = przesłonić; logger = obiekt zapisujący do dziennika.
 *
 * ZOBACZ TEŻ: s06_error_handling/Err02ExceptionHandler (handler lokalny), s06_error_handling/Err04ProblemDetail
 *             (standardowy format odpowiedzi błędu), s06_error_handling/Err05CustomExceptions (hierarchia wyjątków),
 *             s00_start/Start04TestingIntro (@WebMvcTest), t10_exceptions/Exceptions07BestPractices.
 * </pre>
 */
public final class Err03ControllerAdvice {

    private Err03ControllerAdvice() {
    }

    // =================================================================================================
    // 1. GLOBALNY ADVICE (Err03ControllerAdviceTest)
    // =================================================================================================
    //
    // GlobalExceptionHandler ma {@code @RestControllerAdvice}: to {@code @ControllerAdvice} + {@code @ResponseBody},
    // więc wartości zwracane z metod trafiają do ciała odpowiedzi jako JSON. ProductController nie ma żadnego
    // @ExceptionHandler, a ProductNotFoundException zamienia się na 404 z naszym JSON-em. Klasa jest zwykłym beanem —
    // znajduje ją skanowanie komponentów w pakiecie aplikacji lekcji.
    // Mamy też "siatkę bezpieczeństwa" {@code @ExceptionHandler(Exception.class)}: 500, stały komunikat dla klienta,
    // szczegóły (stack trace) tylko do dziennika przez logger.error(..., e). Test sprawdza, że fragment "PRICE_V3"
    // z komunikatu wyjątku NIE trafia do odpowiedzi.
    //
    // DOBRA PRAKTYKA: zawsze miej w advice catch-all na Exception, który LOGUJE wyjątek i odsyła stały komunikat.
    //   Dlaczego? Bez niego nieobsłużone wyjątki trafiają do /error w domyślnym formacie Boota (inny kształt niż reszta
    //   błędów), a bez logowania nikt się nie dowie, co się zepsuło — klient dostaje 500, a w logach pusto.
    //
    // PUŁAPKA: catch-all {@code Exception.class} łapie także wyjątki, które Spring MVC sam zamieniłby na 4xx
    //   (brak parametru, zły typ, nieobsługiwana metoda HTTP). Dlaczego to groźne? Takie błędy klienta zamienią się na
    //   500 i zaciemnią monitoring. Rozwiązanie: Err04ProblemDetail (ResponseEntityExceptionHandler obsługuje je poprawnie).

    // =================================================================================================
    // 2. LOKALNY HANDLER WYGRYWA Z GLOBALNYM (Err03ControllerAdviceTest)
    // =================================================================================================
    //
    // LocalOverrideController ma własny handler ProductNotFoundException, a GlobalExceptionHandler — też.
    // Odpowiedź ma "handledBy": "local". Handler z kontrolera jest sprawdzany PRZED advice. Przydatne, gdy jeden
    // endpoint ma odpowiadać wyjątkowo. Sprawdzone testem localBeatsGlobal.

    // =================================================================================================
    // 3. KOLEJNOŚĆ ADVICE — @Order (Err03ControllerAdviceTest)
    // =================================================================================================
    //
    // PaymentAdvice (@Order(2)) i GlobalExceptionHandler (@Order(3)) mają handler dla PaymentDeclinedException.
    // Odpowiedź pochodzi z PaymentAdvice: 402 i "handledBy": "payment-advice". MNIEJSZA liczba = WYŻSZY priorytet.
    // Advice bez @Order ma najniższy priorytet (Ordered.LOWEST_PRECEDENCE) — a przy tym samym priorytecie kolejność
    // jest przypadkowa, więc nigdy nie polegaj na niej.
    //
    // PUŁAPKA: dwa advice bez @Order obsługujące ten sam wyjątek. Dlaczego? O wygranym decyduje nieokreślona
    //   kolejność rejestracji — aplikacja zadziała inaczej po drobnej zmianie (np. dodaniu innego beana).
    //   Zawsze nadawaj @Order, gdy dwa advice mogą pasować do tego samego wyjątku.

    // =================================================================================================
    // 4. ZAKRES — assignableTypes, basePackageClasses, annotations (Err03ControllerAdviceTest)
    // =================================================================================================
    //
    // @RestControllerAdvice ma atrybuty ograniczające zakres (sprawdzone w Spring Framework 7.0.9):
    //   assignableTypes    = konkretne klasy kontrolerów        ← użyte w OrderAdvice
    //   basePackages       = pakiety jako teksty
    //   basePackageClasses = pakiety wskazane przez klasy (odporne na zmianę nazwy pakietu)
    //   annotations        = kontrolery oznaczone daną adnotacją
    // OrderAdvice ma {@code assignableTypes = OrderController.class}: IllegalStateException w OrderController → 409,
    // ten sam wyjątek w ProductController → 500 z GlobalExceptionHandler (advice poza zakresem jest pomijany).
    //
    // DOBRA PRAKTYKA: ograniczaj zakres advice, który ma obsługiwać tylko jeden obszar (np. kontrolery admina).
    //   Dlaczego? Advice bez zakresu działa dla WSZYSTKICH kontrolerów aplikacji, więc cudze wyjątki mogą trafić
    //   do niewłaściwej obsługi i odpowiedzi staną się nieprzewidywalne.

    // =================================================================================================
    // 5. PUŁAPKA: PIERWSZY PASUJĄCY ADVICE, NIE „NAJBARDZIEJ SZCZEGÓŁOWY” (Err03ControllerAdviceTest)
    // =================================================================================================
    //
    // W obrębie JEDNEGO advice (i kontrolera) wygrywa najbliższy typ wyjątku (Err02ExceptionHandler). Ale MIĘDZY advice
    // decyduje kolejność: Spring bierze pierwszy advice (wg @Order), który ma jakikolwiek pasujący handler.
    // OrderAdvice (@Order(1)) ma handler RuntimeException, więc w OrderController przesłania on SZCZEGÓŁOWY handler
    // ProductNotFoundException z GlobalExceptionHandler: zamiast 404 klient dostaje 503. Sprawdzone testem
    // firstMatchingAdviceWinsEvenIfBroad (to samo dotyczy PaymentDeclinedException, mimo PaymentAdvice).
    //
    // PUŁAPKA: szeroki handler (RuntimeException, Exception) w advice o wysokim priorytecie. Dlaczego? Przesłania wszystkie
    //   węższe handlery z advice o niższym priorytecie. Szerokie handlery daj do advice o NAJNIŻSZYM priorytecie (na
    //   końcu), a wąskie (konkretne wyjątki) wyżej.

    // =================================================================================================
    // 6. ADVICE JEST ZWYKŁYM BEANEM LEKCJI (Err03AdviceIsolationTest)
    // =================================================================================================
    //
    // Advice działa tylko w kontekście, który go zeskanował. Err03Application skanuje pakiet tej lekcji, więc advice
    // z Err03 nie wpływa na Err02 i odwrotnie (test sprawdza pakiety beanów i brak kontrolerów z Err02).
    // Ta sama zasada w prawdziwym projekcie: advice w pakiecie głównym aplikacji obejmuje wszystkie kontrolery.
    //
    // PYTANIE REKRUTACYJNE: „Czym różni się @ControllerAdvice od @RestControllerAdvice?” — @RestControllerAdvice =
    //   @ControllerAdvice + @ResponseBody, więc zwrócone obiekty idą do ciała odpowiedzi (JSON), a nie są traktowane
    //   jako nazwy widoków. W aplikacji REST używaj @RestControllerAdvice.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @RestControllerAdvice = globalne @ExceptionHandler dla wielu kontrolerów (+ @ResponseBody).
     *   • Kolejność szukania: handler w kontrolerze → advice wg @Order (mniejsza liczba = ważniejszy) → dalej Spring/Boot.
     *   • W jednym advice wygrywa najbliższy typ wyjątku; między advice — pierwszy z pasującym handlerem.
     *   • Zakres: assignableTypes / basePackageClasses / basePackages / annotations.
     *   • Catch-all na Exception: loguj wyjątek (logger.error z wyjątkiem), klientowi daj stały komunikat.
     *   • Szerokie handlery — do advice o najniższym priorytecie; zawsze nadawaj @Order, gdy advice się pokrywają.
     *   • Advice działa w kontekście, który go zeskanował (w kursie: w aplikacji swojej lekcji).
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się @RestControllerAdvice od @ExceptionHandler w kontrolerze? Kiedy które?
     *   2. Co się stanie? Kontroler ma handler ProductNotFoundException, a globalny advice też. Który wygra i dlaczego?
     *   3. Co się stanie? Dwa advice: @Order(1) z handlerem RuntimeException i @Order(2) z handlerem
     *      ProductNotFoundException (podklasa RuntimeException). Który odpowie na ProductNotFoundException?
     *   4. ZNAJDŹ BŁĄD: dwa advice bez @Order obsługują ten sam wyjątek, a po dodaniu nowego beana odpowiedzi
     *      w aplikacji się zmieniły. Dlaczego i jak naprawić?
     *   5. Do czego służy assignableTypes i dlaczego ogranicza ryzyko?
     *   6. ZNAJDŹ BŁĄD: {@code logger.error("Błąd: " + e.getMessage())} w catch-all. Co przepada i dlaczego to wada?
     *   7. Co się stanie? W advice jest {@code @ExceptionHandler(Exception.class)}, a klient wysyła POST na adres, który
     *      przyjmuje tylko GET. Jaki kod dostanie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Err03ControllerAdviceExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): dostajesz mapę "nazwa advice → wartość z @Order". Zwróć listę nazw w kolejności, w jakiej
     * Spring będzie ich używać (najmniejsza liczba pierwsza; przy równych liczbach alfabetycznie).
     * Podpowiedź: {@code entrySet().stream().sorted(Map.Entry.comparingByValue().thenComparing(Map.Entry.comparingByKey()))}.
     */
    static List<String> exercise1(Map<String, Integer> orderByAdvice) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Kontroler do ćwiczenia 2: endpoint zawsze rzuca PaymentDeclinedException. */
    @RestController
    static class Exercise2Controller {

        @GetMapping("/api/ex/pay")
        String pay() {
            throw new PaymentDeclinedException("odrzucono");
        }
    }

    /**
     * ĆWICZENIE 2 (średnie): handler w klasie advice. Metoda ma obsłużyć PaymentDeclinedException i zwrócić
     * 402 z ciałem {"error": "Płatność odrzucona"}. Adnotację klasy dostajesz gotową: zakres jest zawężony do
     * Exercise2Controller, więc advice nie wpływa na resztę aplikacji lekcji (standaloneSetup w teście wymaga adnotacji
     * {@code @RestControllerAdvice} na klasie). Podpowiedź: dodaj {@code @ExceptionHandler(PaymentDeclinedException.class)};
     * zwracaj ResponseEntity.
     */
    @RestControllerAdvice(assignableTypes = Exercise2Controller.class)
    static class Exercise2Advice {

        // TODO: dodaj adnotację @ExceptionHandler i wypełnij metodę
        ResponseEntity<Map<String, String>> declined(PaymentDeclinedException e) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    /** Kontroler do ćwiczenia 3 (jest w zakresie advice). Aplikacja lekcji go zeskanuje — to bez znaczenia dla lekcji. */
    @RestController
    static class Exercise3Controller {

        @GetMapping("/api/ex/in")
        String in() {
            throw new IllegalStateException("Stan niedozwolony");
        }
    }

    /** Drugi kontroler do ćwiczenia 3 (POZA zakresem advice). */
    @RestController
    static class Exercise3OtherController {

        @GetMapping("/api/ex/out")
        String out() {
            throw new IllegalStateException("Stan niedozwolony");
        }
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): advice z zakresem i kolejnością. Dodaj nad klasą
     * {@code @RestControllerAdvice(assignableTypes = Exercise3Controller.class)} oraz {@code @Order(10)} i napisz handler
     * IllegalStateException → 409 z ciałem {"error": "<komunikat wyjątku>"}. Efekt: wyjątek z Exercise3Controller zostanie
     * obsłużony, a ten sam wyjątek z Exercise3OtherController — nie. Podpowiedź: {@code @ExceptionHandler(IllegalStateException.class)}.
     */
    // TODO: dodaj @RestControllerAdvice(assignableTypes = Exercise3Controller.class) i @Order(10)
    static class Exercise3Advice {

        // TODO: dodaj adnotację @ExceptionHandler i wypełnij metodę
        ResponseEntity<Map<String, String>> conflict(IllegalStateException e) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. @ExceptionHandler w kontrolerze działa tylko dla niego; @RestControllerAdvice — dla wielu kontrolerów (domyślnie
     *      wszystkich) w jednym miejscu. Wspólną obsługę (mapowanie wyjątków domenowych) daj do advice, wyjątki jednego
     *      endpointu zostaw w kontrolerze.
     *   2. Wygra handler z kontrolera: handler lokalny jest sprawdzany przed advice. Sprawdzone testem localBeatsGlobal.
     *   3. Advice z @Order(1) — jego handler RuntimeException pasuje, a Spring bierze pierwszy advice z pasującym
     *      handlerem, nie najbardziej szczegółowy. Sprawdzone testem firstMatchingAdviceWinsEvenIfBroad (OrderAdvice
     *      przesłania szczegółowy handler z GlobalExceptionHandler).
     *   4. Przy braku @Order kolejność advice jest nieokreślona (ten sam, najniższy priorytet), więc zależy od rejestracji
     *      beanów i może się zmienić po dodaniu klasy. Napraw: nadaj @Order każdemu z advice.
     *   5. assignableTypes ogranicza advice do wskazanych klas kontrolerów. Wąski advice nie wpływa na resztę
     *      aplikacji, więc nie przechwytuje cudzych wyjątków. Sprawdzone testem scopeLimitsAdvice.
     *   6. Przepada stack trace: ślad stosu i przyczyna wyjątku. Bez nich w logu widać tylko tekst komunikatu, a nie
     *      miejsce, gdzie błąd powstał. Poprawnie: {@code logger.error("Błąd", e)} (wyjątek jako ostatni argument).
     *   7. 500 Internal Server Error (z naszym JSON-em "handledBy": "global"), a powinno być 405 Method Not Allowed.
     *      Catch-all {@code Exception.class} z advice ma pierwszeństwo przed domyślną obsługą wyjątków Springa MVC,
     *      więc łapie też HttpRequestMethodNotSupportedException. Sprawdzone testem catchAllSwallowsMvcErrors.
     *      Poprawka: Err04ProblemDetail (ResponseEntityExceptionHandler).
     */
    // </editor-fold>
}
