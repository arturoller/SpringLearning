package s05_validation.val05_service_validation;

import jakarta.validation.ConstraintViolationException;

import java.util.Map;

/**
 * <pre>
 * TEMAT: Walidacja w warstwie serwisów — {@code @Validated} na klasie, ograniczenia parametrów i wyniku metod
 *        (service = serwis, klasa z logiką biznesową; proxy = obiekt-pośrednik owijający prawdziwy obiekt;
 *        method validation = walidacja metod; return value = wartość zwracana)
 *
 * W SKRÓCIE:
 *   Kontroler jest tylko jednym z wejść do aplikacji. Serwis wołają też zadania cykliczne, konsumenci kolejek, inne
 *   serwisy i testy — żaden z nich nie przechodzi przez {@code @Valid} kontrolera. Dlatego serwis może sam pilnować swoich
 *   reguł: wystarczy {@code @Validated} na klasie i ograniczenia przy parametrach ({@code @Positive BigDecimal price})
 *   oraz przy wyniku ({@code @PositiveOrZero int stockOf(...)}). Złamanie reguły to
 *   {@code ConstraintViolationException}, a ciało metody w ogóle się nie wykonuje.
 *
 * ANALOGIA: recepcja w magazynie hurtowni.
 *   Zamówienia przychodzą różnymi drzwiami: przez sklep internetowy (kontroler), telefonicznie, od dostawców.
 *   Recepcja magazynu (proxy walidacji) sprawdza KAŻDE zlecenie, zanim trafi do magazyniera — niezależnie od tego, którymi
 *   drzwiami przyszło. Magazynier pracuje już tylko na sprawdzonych zleceniach. Ale gdy magazynier woła kolegę przez ścianę
 *   (wywołanie {@code this.metoda()}), recepcja tego nie widzi.
 *
 * JAK TO DZIAŁA:
 *   Kontekst startuje → MethodValidationPostProcessor (BeanPostProcessor z ValidationAutoConfiguration Boota) szuka beanów
 *   z @Validated i owija je w proxy CGLIB (podklasa wygenerowana w locie: ProductService$$SpringCGLIB$$0).
 *
 *   inny bean ──▶ proxy.add(command) ──▶ [Validator: parametry] ──▶ prawdziwy ProductService.add(command)
 *                        │                                                  │
 *                        └─ naruszenie → ConstraintViolationException       └─ wynik ──▶ [Validator: wynik] ──▶ wywołujący
 *
 *   Ścieżki naruszeń: add.command.name (metoda.parametr.pole), stockOf.sku, stockOf.{@code <return value>}.
 *   Nazwy parametrów są prawdziwe dzięki fladze kompilatora -parameters (ustawia ją spring-boot-starter-parent);
 *   bez niej zobaczyłbyś arg0, arg1...
 *
 * SŁÓWKA:
 *   service = serwis; proxy = obiekt-pośrednik; post processor = „postprocesor” (obiekt, który po utworzeniu beana może go
 *   podmienić na proxy); self-invocation = wywołanie własnej metody przez this (omija proxy); return value = wartość
 *   zwracana; command = polecenie (obiekt z danymi operacji); fail fast = „zawiedź szybko” (zgłoś błąd od razu, przy
 *   pierwszym złym kroku); invariant = niezmiennik (reguła, która zawsze musi być prawdziwa).
 *
 * ZOBACZ TEŻ: s05_validation/Val02ValidInControllers (walidacja w kontrolerze, @Validated na klasie kontrolera to błąd),
 *             s05_validation/Val03NestedAndGroups (kaskada @Valid, którą tu stosujemy na parametrze),
 *             s06_error_handling/Err03ControllerAdvice (zamiana ConstraintViolationException na 400),
 *             s01_core_ioc/Ioc02ConstructorInjection (wstrzykiwanie serwisu do kontrolera),
 *             t19_annotations_reflection/Annotations06DynamicProxy (jak działa proxy).
 * </pre>
 */
public final class Val05ServiceValidation {

    private Val05ServiceValidation() {
    }

    // =================================================================================================
    // 1. @Validated NA KLASIE → PROXY (ProductService)
    // =================================================================================================
    //
    //   @Service @Validated
    //   public class ProductService {
    //       public Product add(@NotNull @Valid NewProduct command) { ... }
    //
    // Sama obecność ograniczeń przy parametrach nic nie robi. Potrzebna jest adnotacja @Validated na klasie
    // (org.springframework.validation.annotation.Validated) — dopiero ona sprawia, że MethodValidationPostProcessor owija
    // bean w proxy. Ten postprocesor rejestruje autokonfiguracja walidacji Boota (bean methodValidationPostProcessor);
    // w naszym module jest starter validation. W teście widać: bean to klasa
    // ProductService$$SpringCGLIB$$0, podklasa ProductService (AopUtils.isCglibProxy == true).
    //
    // PUŁAPKA: ograniczenie przy parametrze metody serwisu BEZ @Validated na klasie (UnvalidatedService). Dlaczego to
    //   groźne? Wygląda na zabezpieczone, ale bean nie jest proxy i ograniczenie jest tylko ozdobą: shout("")
    //   wykonuje się bez błędu. Brakuje też jakiegokolwiek ostrzeżenia w logu.
    //
    // Test: section1_proxy, section5_missingValidated.

    // =================================================================================================
    // 2. PARAMETRY: ConstraintViolationException zamiast wykonania metody
    // =================================================================================================
    //
    // add(new NewProduct(" ", "x", BigDecimal.ZERO)) → ConstraintViolationException; getConstraintViolations() zawiera
    // naruszenia ze ścieżkami add.command.name, add.command.price, add.command.sku (kaskada @Valid z Val03 działa
    // też tutaj). Metoda add NIE wykonała się ani razu — licznik się nie zmienił. add(null) łamie @NotNull
    // (ścieżka add.command).
    //
    // UWAGA (porównanie z kontrolerem, Val02): to INNY wyjątek niż w kontrolerze. @Valid @RequestBody daje
    // MethodArgumentNotValidException i MVC zamienia go na 400. ConstraintViolationException z serwisu MVC domyślnie
    // NIE zna — bez własnej obsługi (dział s06) klient dostaje 500.
    //
    // Test: section2_parameterViolations.

    // =================================================================================================
    // 3. WYNIK METODY: ograniczenie przy typie zwracanym
    // =================================================================================================
    //
    //   @PositiveOrZero
    //   public int stockOf(@Pattern(regexp = SKU_FORMAT) String sku)
    //
    // Ograniczenie stojące nad metodą dotyczy WYNIKU. Gdy dane w serwisie są uszkodzone (BAD-001 ma stan -3), wynik łamie
    // regułę, wyjątek ma ścieżkę stockOf.<return value>, a zły wynik nie wychodzi dalej. To zasada „zawiedź szybko”:
    // lepszy czytelny błąd niż ujemny stan magazynowy wędrujący po systemie.
    //
    // DOBRA PRAKTYKA: ograniczeń na wyniku używaj oszczędnie — do prawdziwych niezmienników. Dlaczego? Każde wywołanie
    //   płaci za walidację, a błąd na wyniku oznacza błąd w SAMYM serwisie lub jego danych (nie w danych klienta), więc
    //   rzadko jest czymś, co klient może naprawić.
    //
    // Test: section3_returnValue.

    // =================================================================================================
    // 4. PUŁAPKA: WYWOŁANIE Z TEJ SAMEJ KLASY OMIJA PROXY
    // =================================================================================================
    //
    //   public String describe(String sku) { return "SKU: " + this.normalize(sku); }   // this = prawdziwy obiekt, bez proxy
    //   public String normalize(@NotBlank String sku) { ... }
    //
    // Walidację wykonuje proxy, ale wewnątrz klasy this wskazuje na prawdziwy obiekt, nie na proxy. Dlatego
    // service.normalize(" ") z zewnątrz rzuca wyjątek, a service.describe(" ") (które woła normalize
    // przez this) — nie: zwraca "SKU: ", bez żadnego błędu. Dla null dopiero w ciele normalize poleci NullPointerException.
    //
    // PUŁAPKA: liczyć, że ograniczenie na metodzie pomocniczej chroni również wywołania wewnątrz klasy. Dlaczego nie?
    //   Spring AOP działa przez proxy, a wywołanie przez this proxy omija — ta sama zasada dotyczy @Transactional,
    //   @Cacheable i @Async. Rozwiązanie: stawiaj ograniczenia na metodach PUBLICZNYCH, wołanych z zewnątrz — na „granicy”
    //   serwisu (tak jak w ćwiczeniu 2) — albo przenieś metodę do osobnego beana.
    //
    // Test: section4_selfInvocation.

    // =================================================================================================
    // 5. KONTROLER CZY SERWIS? KIEDY CO
    // =================================================================================================
    //
    //   KONTROLER (@Valid, ograniczenia na parametrach):  kształt i format danych z HTTP — „czy to w ogóle poprawne
    //       zapytanie?”. Błąd to 400 dla klienta, dobrze obsługiwany centralnie (s06). Serwis nawet nie jest wywołany.
    //   SERWIS (@Validated):  reguły, których serwis nie może oddać w obce ręce — bo woła go też ktoś inny niż kontroler
    //       (zadanie, kolejka, inny serwis), albo to niezmienniki (wynik). Błąd to „programista wywołał mnie źle”.
    //   BAZA (UNIQUE, NOT NULL, CHECK):  ostatnia linia obrony — jedyna, która gwarantuje spójność przy równoległych zapisach.
    //   BIZNES (zwykły kod, wyjątki domenowe):  reguły zależne od stanu („za mało towaru na stanie”) — nie są walidacją danych.
    //
    // DOBRA PRAKTYKA: waliduj na granicach, a nie wszędzie po trochu. Dlaczego? Ta sama reguła zapisana w trzech warstwach
    //   rozjedzie się przy pierwszej zmianie. Dobry układ: format i kształt — w kontrolerze; niezmienniki publicznego API
    //   serwisu — w serwisie; spójność — w bazie. W teście widać skutek: zły formularz zatrzymuje kontroler (400), a SKU
    //   z adresu, którego kontroler nie sprawdził, zatrzymuje dopiero serwis (wyjątek → bez obsługi 500).
    //
    // Test: section6_controllerVersusService.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Walidacja metod serwisu: @Validated na KLASIE + ograniczenia przy parametrach/wyniku. Wyjątek:
     *     ConstraintViolationException (MVC nie zamienia go na 400 — bez obsługi 500).
     *   • Działa przez proxy CGLIB z MethodValidationPostProcessor (autokonfiguracja Boota). Bez @Validated — brak walidacji.
     *   • Ścieżki: metoda.parametr.pole (add.command.name), metoda.parametr, metoda.<return value>.
     *   • Kaskada: @NotNull @Valid NewProduct command; wynik: ograniczenie nad metodą.
     *   • Wywołanie przez this (z tej samej klasy) omija proxy — ograniczenia stawiaj na publicznej granicy serwisu.
     *   • Kontroler: kształt zapytania (400). Serwis: niezmienniki i wiele wejść. Baza: unikalność i spójność.
     *   • W kontrolerze NIE dawaj @Validated na klasie (Val02) — w serwisie to właściwe miejsce.
     *
     * PYTANIA KONTROLNE:
     *   1. Co się stanie? Wywołujesz service.add(new NewProduct(" ", "x", BigDecimal.ZERO)) na serwisie z @Validated.
     *      Jaki wyjątek, jakie ścieżki naruszeń i czy metoda add się wykona?
     *   2. ZNAJDŹ BŁĄD: serwis ma public String shout(@NotBlank String text), ale klasa nie ma @Validated.
     *      Programista oczekuje wyjątku dla shout(""). Co faktycznie się stanie i czemu?
     *   3. Co się stanie? service.describe(" ") woła this.normalize(sku), a normalize ma @NotBlank.
     *      Czy dostaniesz ConstraintViolationException? A dla service.normalize(" ") wołanego z testu?
     *   4. Co się stanie? service.describe(null) — jaki wyjątek i dlaczego nie ConstraintViolationException?
     *   5. Jaką ścieżkę ma naruszenie ograniczenia na WYNIKU metody stockOf i po co w ogóle walidować wynik?
     *   6. Dlaczego ConstraintViolationException z serwisu wywołanego przez kontroler kończy się (bez obsługi) kodem 500,
     *      a MethodArgumentNotValidException z kontrolera — kodem 400?
     *   7. Podaj dwa powody, dla których walidacja w kontrolerze nie wystarcza i serwis powinien chronić się sam.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Val05ServiceValidationExercisesTest
    // =================================================================================================

    // ĆWICZENIE 1 (łatwe): otwórz DiscountService (rabat procentowy). Spraw, by serwis rzucał ConstraintViolationException
    // dla ceny niedodatniej lub null oraz dla procentu spoza zakresu 0–100. Podpowiedź: czego brakuje NA KLASIE (sekcja 1)?
    // i jakie ograniczenia przy parametrach (@NotNull, @Positive, @Min, @Max)?
    //
    // ĆWICZENIE 2 (średnie): otwórz GreetingService. Ma @Validated, ale ograniczenie stoi na metodzie clean(), którą
    // greet() woła przez this — więc greet(" ") nie rzuca wyjątku. Napraw tak, by greet(" ") i greet(null) wołane z zewnątrz
    // rzucały ConstraintViolationException. Podpowiedź: sekcja 4 — ograniczenie ma stać na publicznej granicy.

    /**
     * ĆWICZENIE 3 (trudniejsze): zamień ConstraintViolationException na mapę „nazwa pola → komunikat”, gdzie nazwa pola to
     * OSTATNI element ścieżki naruszenia (np. add.command.name → "name"). Mapa ma być posortowana po kluczu (TreeMap);
     * gdy jedno pole ma kilka komunikatów, zostaw leksykograficznie najmniejszy.
     * Podpowiedź: dla każdego naruszenia (getConstraintViolations()) przejdź po getPropertyPath() — to iterowalne węzły
     * (Path.Node), a getName() daje nazwę; zapamiętaj ostatnią.
     */
    static Map<String, String> violationsByField(ConstraintViolationException exception) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. ConstraintViolationException; ścieżki add.command.name, add.command.price, add.command.sku; metoda add się
     *      nie wykona (licznik bez zmian). Sprawdzone testem (section2).
     *   2. Nic: shout("") wykona się i zwróci "!". Bez @Validated na klasie bean nie jest owijany w proxy walidacji,
     *      więc ograniczenie jest tylko ozdobą. Sprawdzone testem (section5).
     *   3. Nie, describe(" ") zwróci "SKU: " bez błędu — this.normalize omija proxy. Natomiast service.normalize(" ")
     *      wołane z zewnątrz (przez proxy) rzuci ConstraintViolationException. Sprawdzone testem (section4).
     *   4. NullPointerException z ciała normalize (strip() na null). Walidacja nie zadziałała (omija ją wywołanie przez
     *      this), więc null dotarł do kodu. Sprawdzone testem (section4).
     *   5. stockOf.<return value>. Walidacja wyniku zatrzymuje uszkodzone dane (ujemny stan magazynowy) zaraz przy źródle
     *      (zawiedź szybko), zamiast pozwolić im wędrować dalej. Sprawdzone testem (section3).
     *   6. Spring MVC ma wbudowaną obsługę MethodArgumentNotValidException (zamienia na 400), a ConstraintViolationException
     *      z serwisu jest dla niego zwykłym nieobsłużonym wyjątkiem — kończy się 500. Zamianę na 400 robi dopiero
     *      własna obsługa błędów (s06). Sprawdzone testem (section6: wyjątek wychodzi z MockMvc; w Val02 — status 500 na
     *      prawdziwym serwerze).
     *   7. (a) serwis wołają też inni niż kontroler: zadania cykliczne, konsumenci kolejek, inne serwisy, testy;
     *      (b) niezmienniki i wyniki, których kontroler nie widzi. Dodatkowo spójność przy równoległych zapisach gwarantuje
     *      dopiero baza (UNIQUE).
     */
    // </editor-fold>
}
