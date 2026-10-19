package s06_error_handling.err03_controller_advice;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <pre>
 * TEMAT: {@code @RestControllerAdvice} — jedno miejsce na obsługę błędów całej aplikacji
 *        (controller advice = porada dla kontrolerów; global = globalny, wspólny dla wszystkich)
 *
 * W SKRÓCIE:
 *   Handler {@code @ExceptionHandler} w kontrolerze (lekcja Err02) działa tylko dla tego kontrolera. Gdy ta sama obsługa
 *   ma obowiązywać wszędzie, przenosisz ją do osobnej klasy z adnotacją {@code @RestControllerAdvice}. Spring zbiera
 *   handlery z takich klas i stosuje je do wszystkich kontrolerów (albo tylko do wybranych: po pakiecie, klasie
 *   lub adnotacji). Kontrolery przestają zawierać try/catch i handlery — tylko rzucają wyjątki, a odpowiedź błędu
 *   powstaje w jednym, łatwym do przejrzenia miejscu. W tej samej klasie logujesz błąd — raz.
 *
 * ANALOGIA: centralny dział reklamacji sieci sklepów.
 *   Każdy sklep (kontroler) tylko przyjmuje reklamację i odsyła ją dalej. Centrala (porada) pisze jednolity list do klienta
 *   i raz zapisuje sprawę w rejestrze. Niektóre sklepy (np. rozliczenia) mają własny, specjalny dział, który sprawdzamy
 *   PRZED centralą — o kolejności sprawdzania decyduje numer priorytetu.
 *
 * JAK TO DZIAŁA:
 *   1. Spring znajduje wszystkie beany z {@code @ControllerAdvice}/{@code @RestControllerAdvice} i sortuje je według
 *      {@code @Order} (mniejsza liczba = ważniejsza; HIGHEST_PRECEDENCE to najmniejsza liczba, LOWEST_PRECEDENCE — największa;
 *      bez @Order porada ma najniższy priorytet).
 *   2. Gdy kontroler rzuci wyjątek, najpierw sprawdzane są handlery w samym kontrolerze (lekcja Err02),
 *      potem po kolei porady od najważniejszej.
 *   3. W każdej porze, która obejmuje ten kontroler (zawężenie niżej), szukany jest handler pasujący do wyjątku — wewnątrz
 *      jednej klasy wygrywa najbliższy typ. PIERWSZA porada, która ma jakikolwiek pasujący handler, kończy poszukiwania.
 *   4. Zawężanie porady: {@code @RestControllerAdvice(basePackages = "...")} (kontrolery z pakietu), {@code assignableTypes
 *      = X.class} (konkretne klasy kontrolerów), {@code annotations = ...} (kontrolery z daną adnotacją). Bez zawężenia
 *      porada obejmuje wszystko.
 *   5. {@code @RestControllerAdvice} = {@code @ControllerAdvice} + {@code @ResponseBody}: wynik handlera trafia do ciała
 *      (JSON); zwykłe {@code @ControllerAdvice} szukałoby widoku (strony HTML).
 *
 *   W tej lekcji: GlobalErrorAdvice (cała aplikacja, najniższy priorytet), BillingAdvice (tylko BillingController, najwyższy
 *   priorytet), AdminAdvice (tylko pakiet admin, @Order(1)). Kontrolery nie zawierają żadnej obsługi błędów.
 *
 * SŁÓWKA:
 *   advice = porada, rada; order = kolejność, priorytet; precedence = pierwszeństwo; narrow = zawężać; assignable types =
 *   typy przypisywalne (klasy, do których pasuje dany kontroler); base package = pakiet bazowy; catch-all = „łapiący
 *   wszystko”; fallback = rozwiązanie awaryjne, siatka bezpieczeństwa; field = pole; log = dziennik (zapis zdarzeń).
 *
 * ZOBACZ TEŻ: s05_validation/Val02ValidInControllers (skąd bierze się MethodArgumentNotValidException),
 *             s06_error_handling/Err02ExceptionHandler (handlery w kontrolerze), s06_error_handling/Err04ProblemDetail
 *             (standardowy kształt ciała błędu), s04_web_rest/Rest03Responses (statusy odpowiedzi),
 *             s01_core_ioc/Ioc03Stereotypes (porada jest beanem), t10_exceptions/Exceptions07BestPractices (logowanie wyjątków).
 * </pre>
 */
public final class Err03ControllerAdvice {

    private Err03ControllerAdvice() {
    }

    // =================================================================================================
    // 1. PORADA GLOBALNA — KONTROLERY BEZ OBSŁUGI BŁĘDÓW (testy: Err03ControllerAdviceTest #1, #3)
    // =================================================================================================
    //
    // ProductController rzuca ProductNotFoundException — czysty wyjątek domeny, bez statusu HTTP. GlobalErrorAdvice
    // zamienia go na odpowiedź (prawdziwe uruchomienie):
    //     GET /api/products/XXX → 404 {"code":"PRODUKT_NIE_ISTNIEJE","message":"Nie ma produktu o SKU XXX"}
    //     POST /api/orders z "{nie json" → 400 {"code":"NIEPOPRAWNY_JSON","message":"Ciało zapytania nie jest poprawnym JSON-em"}
    // Ten sam handler obsługuje wyjątek z każdego kontrolera, więc kształt błędów w całym API jest jednolity.
    //
    // DOBRA PRAKTYKA: jeden wspólny rekord błędu (tu ApiError: code + message) dla całego API. Dlaczego? Klient (frontend)
    //   pisze jedną obsługę błędów zamiast osobnej dla każdego punktu końcowego; pole code jest stabilne, a komunikat
    //   można zmienić bez psucia klientów. (Standardowy kształt: ProblemDetail — lekcja Err04.)

    // =================================================================================================
    // 2. WALIDACJA → MAPA POLE → KOMUNIKAT (test: Err03ControllerAdviceTest #2)
    // =================================================================================================
    //
    // MethodArgumentNotValidException (wyrzucany przy {@code @Valid @RequestBody}, patrz s05_validation/Val02ValidInControllers)
    // niesie BindingResult z listą błędów pól. Handler zamienia je na mapę. Prawdziwa odpowiedź dla zamówienia
    // z pustym klientem i błędną pierwszą pozycją:
    //   400 {"code":"BLAD_WALIDACJI","message":"Dane zamówienia są niepoprawne","fields":{
    //        "customer":"Podaj imię i nazwisko klienta","lines[0].quantity":"Ilość musi być dodatnia",
    //        "lines[0].sku":"SKU nie może być puste"}}
    // Zauważ nazwy pól zagnieżdżonych: lines[0].sku — indeks pozycji na liście. Poprawna pozycja o indeksie 1 nie ma wpisu.
    // Mapa jest TreeMap (posortowana po nazwie pola) — dlatego wynik jest powtarzalny, a test może go porównywać.
    //
    // PUŁAPKA: komunikaty domyślne z Hibernate Validatora zależą od języka (Locale) systemu — u Ciebie będą polskie,
    //   na serwerze CI angielskie. Dlaczego to kłopot? Test porównujący tekst raz zaświeci się na zielono, raz na czerwono.
    //   Ustaw własne {@code message = "..."} w adnotacjach (jak w NewOrder) albo testuj tylko nazwy pól.

    // =================================================================================================
    // 3. LOGOWANIE RAZ, SZCZEGÓŁY NIE DO KLIENTA (test: Err03ControllerAdviceTest #4)
    // =================================================================================================
    //
    // Handler IllegalStateException loguje błąd (log.error z wyjątkiem, więc ze stosem) i zwraca klientowi ogólny komunikat
    //     500 {"code":"BLAD_WEWNETRZNY","message":"Wystąpił błąd wewnętrzny. Spróbuj ponownie później."}
    // Test sprawdza trzy rzeczy: w odpowiedzi nie ma tekstu wyjątku („Baza nie odpowiada: jdbc:h2:mem:sklep...”), w logu
    // jest dokładnie jeden wpis [BLAD-WEWNETRZNY] ze szczegółami, a Tomcat NIE dopisuje drugiego błędu
    // („Servlet.service() ... threw exception”) — bo wyjątek został obsłużony.
    //
    // DOBRA PRAKTYKA: loguj wyjątek w jednym miejscu (w porze), nie w każdym kontrolerze/serwisie. Dlaczego? Ten sam błąd
    //   zapisany trzy razy w trzech warstwach zalewa log i utrudnia liczenie awarii. Loguj z wyjątkiem jako ostatnim argumentem
    //   (log.error("opis", e)) — wtedy w logu jest stos; samo e.getMessage() gubi stos i przyczynę.
    //
    // DOBRA PRAKTYKA: logi 4xx (błąd klienta) na poziomie WARN lub niżej, a 5xx (błąd serwera) na ERROR. Dlaczego? Zła wartość
    //   w formularzu to nie awaria — nie chcesz budzić dyżurnego o trzeciej w nocy przez literówkę klienta.

    // =================================================================================================
    // 4. ZAWĘŻANIE I KOLEJNOŚĆ (testy: Err03ControllerAdviceTest #5, #6, #7, #8)
    // =================================================================================================
    //
    // BillingController i AdminController rzucają TEN SAM typ wyjątku co ProductController (IllegalStateException),
    // a klient dostaje różne odpowiedzi:
    //     POST /api/billing/charge → 502 BRAMKA_PLATNOSCI          (BillingAdvice, assignableTypes)
    //     GET  /api/admin/report   → 503 RAPORT_NIEDOSTEPNY        (AdminAdvice, basePackages)
    //     GET  /api/products/ELE-001/stock → 500 BLAD_WEWNETRZNY   (GlobalErrorAdvice, bez zawężenia)
    // Kolejność porad w kontekście: BillingAdvice (HIGHEST_PRECEDENCE) → AdminAdvice (@Order(1)) → GlobalErrorAdvice
    // (LOWEST_PRECEDENCE). Dla Billing pasują dwie porady (Billing i Global) — wygrywa ważniejsza.
    //
    // Wyjątki Springa, dla których porada nie ma handlera (nieznany adres, zła metoda HTTP), idą dalej domyślną ścieżką
    // z lekcji Err01: 404 i 405 z domyślnym JSON-em Boota (test #8).
    //
    // PUŁAPKA: handler na {@code Exception.class} w poradzie „łapie wszystko” — także wyjątki samego Springa, które normalnie
    //   dają 404, 405, 400, 415. Test Err03AdvicePitfallsTest #1: zła metoda HTTP na istniejącym adresie kończy się
    //   500 zamiast 405. Dlaczego? Spring MVC rzuca dla tych sytuacji zwykłe wyjątki (HttpRequestMethodNotSupportedException
    //   itd.), a handler na Exception jest dla nich „pasującym” handlerem. Albo obsłuż wąskie typy (jak u nas), albo użyj
    //   ResponseEntityExceptionHandler, który zna wyjątki Springa (lekcja Err04ProblemDetail).
    //
    // PUŁAPKA: o zwycięstwie porady decyduje @Order, a NIE „szczegółowość” handlera. Test Err03AdvicePitfallsTest #3:
    //   porada @Order(1) z handlerem na Exception wygrywa z poradą @Order(2) z handlerem na IllegalArgumentException — klient
    //   dostaje 500 zamiast 400. Dlaczego? Pierwsza porada na liście, która ma JAKIKOLWIEK pasujący handler, kończy
    //   poszukiwania; szczegółowość liczy się dopiero wewnątrz jednej klasy. Handlery „na wszystko” daj do porady
    //   o najniższym priorytecie (test #4 pokazuje poprawną kolejność).
    //
    // PUŁAPKA: dwie porady bez @Order mają ten sam, najniższy priorytet — nie licz na żadną konkretną kolejność między nimi.
    //   Dlaczego? Brak @Order oznacza „obojętne”; wynik może się zmienić po dodaniu klasy albo zmianie nazwy beana.
    //   Jeśli porady mogą się nakładać, ZAWSZE ustaw @Order.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @RestControllerAdvice = globalne handlery (@ExceptionHandler) dla wielu kontrolerów + wynik do ciała (JSON).
     *   • Zawężenie: basePackages, assignableTypes, annotations. Bez zawężenia — cała aplikacja.
     *   • Kolejność: @Order, mniejsza liczba = ważniejsza porada; pierwsza z pasującym handlerem wygrywa.
     *   • Handler na Exception łapie też wyjątki Springa (405 → 500!) — nie dawaj go bez namysłu.
     *   • MethodArgumentNotValidException → getBindingResult().getFieldErrors() → mapa pole → komunikat.
     *   • Loguj błąd w jednym miejscu, z wyjątkiem (log.error("opis", e)); klientowi daj ogólny komunikat.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym {@code @RestControllerAdvice} różni się od handlera {@code @ExceptionHandler} wewnątrz kontrolera?
     *   2. Co się stanie? W aplikacji są dwie porady: A z {@code @Order(1)} i handlerem na Exception, B z {@code @Order(2)}
     *      i handlerem na IllegalArgumentException. Kontroler rzuca IllegalArgumentException. Która porada odpowie?
     *   3. Co się stanie? Dodajesz do porady handler {@code @ExceptionHandler(Exception.class)} zwracający 500. Klient wysyła
     *      POST na adres, który obsługuje tylko GET. Jaki status zobaczy?
     *   4. ZNAJDŹ BŁĄD: porada jest oznaczona {@code @ControllerAdvice} (bez Rest), a handler zwraca rekord ApiError. Programista
     *      oczekuje JSON-a. Co się stanie i jak to naprawić?
     *   5. ZNAJDŹ BŁĄD: handler w poradzie robi {@code log.error("Błąd: " + e.getMessage())}, a w kontrolerze ten sam wyjątek
     *      jest dodatkowo łapany i logowany. Co jest nie tak (dwie rzeczy)?
     *   6. Jak sprawić, żeby porada dotyczyła tylko kontrolerów z jednego pakietu?
     *   7. Dlaczego mapę pole → komunikat budujemy w {@code TreeMap}, a nie w {@code HashMap}?
     *   8. Dlaczego w testach porównujących komunikaty walidacji ustawiamy własne {@code message = "..."}?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Err03ControllerAdviceExercisesTest (rozwiązania: solutions.err03_controller_advice)
    // =================================================================================================

    /** FieldProblem = jeden błąd pola (nazwa pola + komunikat) — uproszczony odpowiednik FieldError. */
    public record FieldProblem(String field, String message) {
    }

    /**
     * AdviceDef = opis porady do ćwiczenia 3: nazwa, priorytet ({@code order}: mniejsza liczba = ważniejsza), kontrolery,
     * które obejmuje (pusty zbiór = wszystkie) i typy wyjątków, które obsługuje jej handlery.
     */
    public record AdviceDef(String name, int order, Set<Class<?>> controllers, Set<Class<? extends Throwable>> handles) {
    }

    /**
     * ĆWICZENIE 1 (łatwe): kod statusu HTTP dla wyjątku. ProductNotFoundException → 404, IllegalArgumentException → 400,
     * UnsupportedOperationException → 501, wszystko inne → 500.
     * Podpowiedź: {@code switch} z dopasowaniem typu (pattern matching, t07_inheritance_polymorphism/Inherit07SealedClasses).
     */
    static int statusFor(Throwable e) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zamień listę błędów pól na mapę pole → komunikat posortowaną po nazwie pola (TreeMap).
     * Gdy to samo pole ma kilka błędów, połącz ich komunikaty przez "; " w kolejności alfabetycznej komunikatów.
     * Przykład: [(sku,"puste"), (sku,"za krótkie"), (ilosc,"ujemna")] → {ilosc=ujemna, sku=puste; za krótkie}.
     * Podpowiedź: Map.merge (t12_collections/Collections05Maps) i sortowanie przed łączeniem.
     */
    static Map<String, String> toFieldMap(List<FieldProblem> problems) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): która porada odpowie? Zwróć nazwę pierwszej (wg rosnącego {@code order}) porady, która
     * (a) obejmuje dany kontroler (pusty zbiór controllers = obejmuje wszystkie) i (b) ma handler pasujący do wyjątku
     * (któryś z typów {@code handles} jest tym samym typem co {@code thrown} albo jego nadklasą). Gdy żadna — null.
     * Szczegółowość typu NIE ma znaczenia między poradami — liczy się tylko order. Wejściowej listy nie zmieniaj.
     * Podpowiedź: skopiuj listę, posortuj po order (Comparator.comparingInt) i znajdź pierwszą pasującą.
     */
    static String whichAdviceAnswers(List<AdviceDef> advices, Class<?> controller, Class<? extends Throwable> thrown) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Handler w kontrolerze obowiązuje tylko dla metod tego kontrolera; {@code @RestControllerAdvice} to osobna klasa
     *      (bean), której handlery obejmują wiele kontrolerów (wszystkie albo zawężone do pakietu/klas/adnotacji).
     *      Dzięki temu obsługa błędów jest w jednym miejscu, a kontrolery są czyste.
     *   2. Porada A (@Order(1)) — odpowie 500 (albo to, co zwraca jej handler na Exception). Pierwsza porada z jakimkolwiek
     *      pasującym handlerem kończy poszukiwania, a szczegółowość działa tylko w ramach jednej klasy. Sprawdzone:
     *      Err03AdvicePitfallsTest #3 (a w #4 — odwrotna kolejność daje 400).
     *   3. 500, a nie 405 — handler na Exception łapie też HttpRequestMethodNotSupportedException ze Springa. Sprawdzone:
     *      Err03AdvicePitfallsTest #1; bez porady ten sam błąd to 405 (test #2).
     *   4. Zwykłe @ControllerAdvice nie ma @ResponseBody, więc Spring NIE zapisze rekordu jako JSON, tylko potraktuje go jak
     *      atrybut modelu i spróbuje wyrenderować widok o nazwie wyprowadzonej z adresu. W teście ze standaloneSetup kończy się to
     *      ServletException „Circular view path [bad]...” (Err03AdvicePitfallsTest #5). Naprawa: @RestControllerAdvice albo
     *      @ResponseBody na handlerze.
     *   5. (a) Log bez wyjątku jako argumentu gubi stos i przyczynę — ma być log.error("Błąd", e). (b) Błąd jest
     *      zapisany dwa razy (w kontrolerze i w poradzie) — loguj w jednym miejscu, a w kontrolerze w ogóle nie łap wyjątku.
     *   6. {@code @RestControllerAdvice(basePackages = "nazwa.pakietu")} (albo basePackageClasses). Sprawdzone: AdminAdvice
     *      i Err03ControllerAdviceTest #6.
     *   7. HashMap nie gwarantuje kolejności, więc odpowiedź (i test) mogłyby się różnić między uruchomieniami; TreeMap sortuje po
     *      nazwie pola, więc wynik jest powtarzalny.
     *   8. Domyślne komunikaty Hibernate Validatora zależą od Locale JVM (polskie u Ciebie, angielskie na innym komputerze),
     *      więc test na treści domyślnych komunikatów byłby niedeterministyczny.
     */
    // </editor-fold>
}
