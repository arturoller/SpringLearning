package s06_error_handling.err05_custom_exceptions;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * TEMAT: Własne wyjątki domeny sklepu — hierarchia, kody statusu, bezpieczeństwo i identyfikator błędu
 *        (custom exception = własny wyjątek; domain = domena, czyli dziedzina problemu: tu sklep; error id = identyfikator błędu)
 *
 * W SKRÓCIE:
 *   Gdy w sklepie brakuje produktu albo zamówienie łamie regułę, serwis rzuca wyjątek, który mówi, CO się stało
 *   (ProductNotFoundException, OutOfStockException, InvalidOrderException) — i nic nie wie o HTTP. Jedna porada zamienia
 *   te wyjątki na odpowiedzi z dobrym kodem statusu (404, 409, 422). Błędy serwera (awaria bazy, błąd w kodzie) dostają
 *   inne traktowanie: klient widzi ogólny opis i identyfikator błędu, a szczegóły ze stosem zostają w logu pod tym samym
 *   identyfikatorem. Dzięki temu nic wrażliwego nie wycieka, a każdy zgłoszony problem da się odnaleźć w logach.
 *
 * ANALOGIA: reklamacja w sklepie z numerem sprawy.
 *   Klient, któremu zepsuł się zakup, dostaje kartkę: „Numer sprawy 6c27c166, odezwiemy się”. Nie dostaje schematu
 *   magazynu ani nazwiska pracownika. Obsługa po numerze znajduje całą historię w systemie. A gdy klient prosi o produkt,
 *   którego nie ma, mówi mu się wprost: „Nie ma takiego towaru” — to nie jest awaria sklepu.
 *
 * JAK TO DZIAŁA:
 *   Hierarchia (sealed = zapieczętowana, lista podklas zamknięta):
 *       RuntimeException
 *       └── ShopException (sealed, code(), details())           ← błędy KLIENTA / reguł biznesowych
 *           ├── ProductNotFoundException   → 404 Not Found
 *           ├── OutOfStockException        → 409 Conflict
 *           └── InvalidOrderException      → 422 Unprocessable Content
 *       StorageUnavailableException (z przyczyną cause)         ← awaria infrastruktury → 503
 *       (każdy inny wyjątek, np. NullPointerException)           ← błąd programisty → 500
 *   Przepływ: kontroler → serwis rzuca wyjątek domeny → ShopProblemAdvice → ProblemDetail (lekcja Err04) + wpis w logu.
 *   Switch po typie w poradzie jest wyczerpujący (kompilator pilnuje, by każda podklasa miała swój status).
 *
 *   Dobór kodu statusu (decyzje tej lekcji):
 *     400 Bad Request          — zapytania nie da się zrozumieć albo dane mają zły FORMAT (zły JSON, puste pole, ilość ≤ 0,
 *                                brak wymaganego pola) — wyłapuje Bean Validation i Spring, zanim wejdzie serwis;
 *     404 Not Found            — nie ma ZASOBU wskazanego w ADRESIE (GET /api/products/XXX);
 *     409 Conflict             — zapytanie jest poprawne, ale koliduje z AKTUALNYM STANEM (za mało towaru);
 *     422 Unprocessable Content— składnia poprawna, ale TREŚĆ łamie regułę biznesową (pusta lista pozycji, nieznany SKU
 *                                w pozycji zamówienia). Gdyby w tym API wszystko szło jako 400, też byłoby poprawnie, ale
 *                                wtedy klient nie odróżni „źle zapisałeś” od „dobrze zapisałeś, ale to nie ma sensu”;
 *     500 / 503                — wina serwera: 500 błąd w kodzie, 503 chwilowa niedostępność zależności (baza).
 *     Spójność jest ważniejsza niż wybór: zdecyduj raz i trzymaj się tego w całym API.
 *
 * SŁÓWKA:
 *   sealed = zapieczętowany; permits = zezwala (lista dozwolonych podklas); unchecked = niekontrolowany (nie trzeba go deklarować
 *   przez throws); cause = przyczyna (wyjątek, który wywołał ten wyjątek); wrap = opakować (złapać i rzucić nowy, z przyczyną);
 *   leak = wyciek (ujawnienie czegoś, co miało zostać wewnątrz); correlation id = identyfikator korelacji (ten sam numer
 *   w odpowiedzi i w logu); root cause = pierwotna przyczyna (najgłębszy wyjątek w łańcuchu); unprocessable = nie do przetworzenia.
 *
 * ZOBACZ TEŻ: s06_error_handling/Err04ProblemDetail (kształt odpowiedzi), s06_error_handling/Err03ControllerAdvice (porada i
 *             logowanie w jednym miejscu), s10_transactions/Tx01Transactional (wyjątki niekontrolowane a wycofanie transakcji),
 *             t10_exceptions/Exceptions02CheckedUnchecked (kontrolowane i niekontrolowane),
 *             t10_exceptions/Exceptions05CustomExceptions (własne wyjątki w Javie),
 *             t10_exceptions/Exceptions06ChainingWrapping (łańcuch przyczyn), t10_exceptions/Exceptions07BestPractices,
 *             t07_inheritance_polymorphism/Inherit07SealedClasses (klasy zapieczętowane).
 * </pre>
 */
public final class Err05CustomExceptions {

    private Err05CustomExceptions() {
    }

    // =================================================================================================
    // 1. HIERARCHIA WYJĄTKÓW DOMENY (testy: Err05HierarchyTest #1, #2, #3)
    // =================================================================================================
    //
    // ShopException to jeden wspólny korzeń z kodem (code()) i danymi (details()). Wspólny korzeń pozwala złapać wszystkie
    // błędy sklepu jednym handlerem, a mimo to rozróżnić je typem. Jest zapieczętowany: dopisanie czwartej podklasy bez
    // wpisania jej do permits kończy się błędem kompilacji:
    //     error: class is not allowed to extend sealed class: ShopException (as it is not listed in its 'permits' clause)
    // a brak nowego przypadku w switch w poradzie — kolejnym błędem kompilacji:
    //     error: the switch expression does not cover all possible input values
    // (oba komunikaty sprawdzone kompilatorem). Zapomnienie o statusie dla nowego wyjątku wychodzi więc na etapie
    // budowania, a nie w produkcji.
    //
    // Wyjątki są NIEKONTROLOWANE (dziedziczą po RuntimeException). Dlaczego? Wyjątek kontrolowany zmusza każdą warstwę
    // pośrednią do deklaracji throws albo try/catch, a obsłużyć go i tak może tylko porada na górze. Dodatkowo domyślnie
    // transakcje Springa wycofują się przy wyjątkach niekontrolowanych (s10_transactions/Tx01Transactional).
    //
    // DOBRA PRAKTYKA: wyjątek domeny opisuje, co się stało w sklepie (brak towaru), a nie jak to pokazać w HTTP. Dlaczego?
    //   Ten sam serwis może wołać REST, zadanie w tle albo konsola — status 409 ma sens tylko w HTTP. Zamianę na status robi
    //   jedno miejsce (porada), więc zmiana polityki („422 zamiast 400”) to jedna zmiana, a nie dwadzieścia.
    //
    // PUŁAPKA: wyjątek wyrzucany jako sposób na zwykłą kontrolę przepływu („nie znaleziono → wyjątek” w pętli, gdzie brak
    //   wyniku jest normalny). Dlaczego? Wyjątek jest drogi (zbiera stos) i ukrywa przebieg programu. Gdy brak wyniku jest
    //   zwykłym przypadkiem, zwróć Optional; wyjątek rzucaj, gdy w tym miejscu brak oznacza błąd — jak GET /api/products/XXX.

    // =================================================================================================
    // 2. KODY STATUSU 404 / 409 / 422 / 400 (testy: Err05CustomExceptionsTest #1–#5)
    // =================================================================================================
    //
    // Prawdziwe odpowiedzi (kopie z uruchomienia, skrócone):
    //   GET  /api/products/XXX                         → 404 {"code":"PRODUKT_NIE_ISTNIEJE","sku":"XXX",...}
    //   POST /api/orders, ELE-001 × 50 (jest 7)         → 409 {"code":"BRAK_TOWARU","available":7,"sku":"ELE-001",...}
    //   POST /api/orders, "lines": []                   → 422 {"code":"ZAMOWIENIE_NIEPOPRAWNE","detail":"Zamówienie nie ma żadnej pozycji"}
    //   POST /api/orders, SKU ZZZ-1 (nieznany)          → 422 (a NIE 404 — adres /api/orders istnieje, zły jest element treści)
    //   POST /api/orders, "customer":"" i quantity 0    → 400 {"errors":{"customer":"...","lines[0].quantity":"..."}}
    //   POST /api/orders, brak pola "lines"             → 400 {"errors":{"lines":"Brak listy pozycji"}}
    // Podział pracy: FORMAT sprawdza Bean Validation w kontrolerze (400), SENS BIZNESOWY — serwis (409/422).
    //
    // PUŁAPKA: 404 dla „nieznanego produktu w ciele zamówienia”. Dlaczego? 404 mówi „nie ma zasobu pod tym adresem”. Klient
    //   zobaczy 404 i pomyśli, że adres /api/orders nie istnieje (literówka w URL), podczas gdy zawinił jeden element JSON-a.
    //
    // PUŁAPKA: 500 na błąd klienta (brak sprawdzenia danych, NullPointerException z pustego pola). Dlaczego? Klient dostaje
    //   „błąd serwera” i ponawia zapytanie, które nigdy się nie uda, a Ty dostajesz alarm o awarii, której nie było.

    // =================================================================================================
    // 3. BŁĘDY SERWERA: NIE UJAWNIAJ, LOGUJ, DAJ IDENTYFIKATOR (testy: Err05CustomExceptionsTest #6, #7, #8)
    // =================================================================================================
    //
    // NullPointerException w serwisie albo awaria bazy to nie sprawa klienta. Porada:
    //   1) generuje errorId (tu przez ErrorIdGenerator — w teście podmieniony na stały „TEST-0001”),
    //   2) zapisuje do logu na poziomie ERROR wpis z errorId i PEŁNYM wyjątkiem (stos + „Caused by”),
    //   3) zwraca ProblemDetail ze statusem 500/503, ogólnym opisem i polem errorId.
    // Odpowiedź (prawdziwa):  {"detail":"Wystąpił błąd wewnętrzny. Podaj identyfikator błędu obsłudze sklepu.",
    //                          "instance":"/api/bug","status":500,"title":"Błąd wewnętrzny","errorId":"9a372cd7"}
    // W logu (prawdziwy wpis): ERROR ... ShopProblemAdvice : [BLAD-WEWNETRZNY] errorId=9a372cd7 — nieoczekiwany błąd
    //                          java.lang.NullPointerException: Cannot invoke "Object.toString()" because "nothing" is null
    // Dla awarii bazy w logu jest dodatkowo: Caused by: java.lang.IllegalStateException: Connection refused:
    // jdbc:h2:tcp://db-prod:9092/sklep (login: sa) — czyli adres i login bazy są w logu (dla zespołu), a NIE w odpowiedzi.
    // Błędy klienta (4xx) idą do logu krótkim wpisem INFO bez stosu: „Błąd klienta [PRODUKT_NIE_ISTNIEJE]: Nie ma produktu o SKU XXX”.
    //
    // DOBRA PRAKTYKA: błąd serwera ma w odpowiedzi i w logu TEN SAM identyfikator. Dlaczego? Użytkownik zgłasza „błąd 6c27c166”,
    //   a Ty jednym wyszukiwaniem w logach znajdujesz pełny stos — bez zgadywania po godzinie i treści.
    //
    // DOBRA PRAKTYKA: opakowując wyjątek niższej warstwy, zawsze przekaż przyczynę (new X("opis", e)). Dlaczego? Bez niej w logu
    //   zostaje tylko Twój opis, a prawdziwy powód (Connection refused, zły SQL) przepada — nie ma już czego szukać.
    //
    // PUŁAPKA: new StorageUnavailableException(e.getMessage(), null) albo samo log.error(e.getMessage()). Dlaczego?
    //   Gubisz przyczynę i stos; zostaje jeden tekst bez miejsca, w którym błąd powstał (test Err05HierarchyTest #6).
    //
    // PUŁAPKA: catch (Exception e) { return null; } albo e.printStackTrace() w serwisie. Dlaczego? Pierwsze połyka błąd i
    //   przesuwa awarię w inne miejsce (NullPointerException dalej), drugie pisze na standardowe wyjście poza systemem logów
    //   (bez poziomu, czasu i identyfikatora). Wyjątek łap tylko tam, gdzie wiesz, co z nim zrobić.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Wyjątki domeny: wspólny korzeń (ShopException, sealed, RuntimeException) + podklasy znaczące biznesowo.
     *   • Wyjątek nie zna HTTP; status i ciało ustala jedna porada (switch po typie).
     *   • 400 zły format; 404 brak zasobu z adresu; 409 konflikt ze stanem; 422 reguła biznesowa złamana w poprawnym zapytaniu;
     *     500 błąd w kodzie; 503 awaria zależności.
     *   • Błąd serwera: klientowi ogólny opis + errorId; do logu ERROR ze stosem i tym samym errorId.
     *   • Opakowując wyjątek — przekaż cause. Nie łap wyjątku, jeśli nie wiesz, co z nim zrobić.
     *   • Błędy 4xx loguj krótko (INFO/WARN), 5xx — ERROR ze stosem.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego zamówienie z pustą listą pozycji dostaje w tej lekcji 422, a nie 400 ani 404?
     *   2. Co się stanie? Klient wysyła POST /api/orders z JSON-em {"customer":"Jan"} (bez pola lines). Jaki status dostanie
     *      i skąd on pochodzi (serwis czy walidacja)?
     *   3. Co się stanie? Klient wysyła zamówienie z pozycją o SKU "ZZZ-1" (takiego produktu nie ma). 404 czy 422?
     *   4. Co się stanie? Dopisujesz czwartą klasę DiscountExpiredException extends ShopException, ale nie wpisujesz jej do permits.
     *      Co zobaczysz jako pierwsze — błąd kompilacji, błąd przy starcie aplikacji czy 500 w produkcji?
     *   5. ZNAJDŹ BŁĄD: {@code catch (IllegalStateException e) { throw new StorageUnavailableException(e.getMessage(), null); }}
     *      Co tracimy i jak to poprawić?
     *   6. ZNAJDŹ BŁĄD: handler dla Exception zwraca {@code ProblemDetail.forStatusAndDetail(500, e.getMessage())}. Dlaczego to dziura?
     *   7. Po co identyfikator błędu w odpowiedzi 500 i w logu?
     *   8. Dlaczego 4xx logujemy jednym krótkim wpisem INFO, a 5xx na poziomie ERROR ze stosem?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Err05CustomExceptionsExercisesTest (rozwiązania: solutions.err05_custom_exceptions)
    // =================================================================================================

    /** Rodzaj sytuacji błędu do ćwiczenia 1. */
    public enum ErrorKind {
        MALFORMED_JSON, FIELD_FORMAT, RESOURCE_MISSING_IN_URL, REFERENCE_MISSING_IN_BODY, STATE_CONFLICT,
        BUSINESS_RULE, DEPENDENCY_DOWN, BUG
    }

    /**
     * ĆWICZENIE 1 (łatwe): wybierz kod statusu wg zasad tej lekcji: MALFORMED_JSON → 400, FIELD_FORMAT → 400,
     * RESOURCE_MISSING_IN_URL → 404, REFERENCE_MISSING_IN_BODY → 422, STATE_CONFLICT → 409, BUSINESS_RULE → 422,
     * DEPENDENCY_DOWN → 503, BUG → 500.
     * Podpowiedź: wyrażenie switch po enum, z kilkoma etykietami w jednym przypadku (t02_controlflow/Control02Switch, t08_enums/Enums01Basics).
     */
    static int statusFor(ErrorKind kind) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zbuduj PUBLICZNE ciało błędu. Dla statusu 4xx: mapa {"detail": komunikat}. Dla 5xx (500 i więcej):
     * mapa {"detail": "Wystąpił błąd wewnętrzny.", "errorId": errorId} — komunikat wewnętrzny NIE może się w niej znaleźć.
     * Pola w kolejności detail, errorId. Inny status (np. 200) → IllegalArgumentException.
     * Podpowiedź: LinkedHashMap albo Map.of (dla dwóch pól kolejność nie jest ważna w teście, ale sprawdza się zawartość).
     */
    static Map<String, String> publicBody(int status, String internalMessage, String errorId) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): rozwiń łańcuch przyczyn wyjątku do listy tekstów "NazwaKlasy: komunikat" (prosta nazwa klasy,
     * getSimpleName; gdy komunikat jest null — sama nazwa klasy). Pierwszy element to sam wyjątek, ostatni — pierwotna przyczyna.
     * Łańcuch może mieć pętlę (wyjątek, który pośrednio wskazuje sam siebie jako przyczynę) — przerwij przy powtórzeniu tego
     * samego obiektu, by nie zapętlić się w nieskończoność.
     * Podpowiedź: pętla po getCause() i zbiór odwiedzonych (identyfikowanych po tożsamości — Collections.newSetFromMap(new IdentityHashMap<>())).
     */
    static List<String> causeChain(Throwable error) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. JSON jest poprawny składniowo i pola mają dobre typy (więc nie 400 — to byłby „nie rozumiem zapytania”), a adres
     *      /api/orders istnieje (więc nie 404). Zamówienie łamie regułę biznesową („musi mieć pozycję”) — to dokładnie przypadek
     *      422 Unprocessable Content. (Wariant „wszystko jako 400” też bywa stosowany — ważne, by trzymać się jednego.)
     *   2. 400. Pole lines ma @NotNull w rekordzie NewOrder, więc to Bean Validation w kontrolerze (MethodArgumentNotValidException),
     *      zanim zamówienie trafi do serwisu; odpowiedź ma errors.lines = „Brak listy pozycji”. Sprawdzone:
     *      Err05CustomExceptionsTest #4.
     *   3. 422 — element treści zamówienia jest błędny, adres istnieje. 404 oznaczałoby „nie ma zasobu pod adresem”. Sprawdzone:
     *      Err05CustomExceptionsTest #3.
     *   4. Błąd kompilacji: „class is not allowed to extend sealed class: ShopException (as it is not listed in its 'permits'
     *      clause)” — to pierwszy błąd, zanim aplikacja w ogóle powstanie. Po dopisaniu do permits kolejny błąd kompilacji:
     *      „the switch expression does not cover all possible input values”, dopóki porada nie dostanie nowego przypadku.
     *      Oba komunikaty sprawdzone kompilatorem javac.
     *   5. Tracimy przyczynę i jej stos: w logu zostaje tylko tekst, bez informacji, gdzie wyjątek powstał. Poprawka:
     *      throw new StorageUnavailableException("Nie udało się odczytać raportu", e) — przyczyna jako drugi argument.
     *      Sprawdzone: Err05HierarchyTest #4 (z przyczyną) i #6 (bez przyczyny).
     *   6. Treść wyjątku trafia do klienta: może zawierać adresy, loginy, fragmenty SQL (u nas: „Connection refused:
     *      jdbc:h2:tcp://db-prod...”). Klientowi daj ogólny opis i errorId, a e.getMessage() zostaw w logu.
     *      Sprawdzone: Err05CustomExceptionsTest #6 i #7 (odpowiedź nie zawiera tekstu z wnętrza).
     *   7. Użytkownik podaje identyfikator z odpowiedzi, a Ty jednym wyszukiwaniem w logach znajdujesz pełny wpis ze stosem.
     *      Bez identyfikatora trzeba zgadywać po czasie i adresie, a przy dużym ruchu to nie wystarcza.
     *   8. 4xx to normalny skutek błędu klienta (literówka, zły adres) — stos nic nie wnosi, a poziom ERROR budziłby alarmy.
     *      5xx to awaria po stronie serwera: potrzebny jest stos, przyczyna i identyfikator. Sprawdzone:
     *      Err05CustomExceptionsTest #6 i #8.
     */
    // </editor-fold>
}
