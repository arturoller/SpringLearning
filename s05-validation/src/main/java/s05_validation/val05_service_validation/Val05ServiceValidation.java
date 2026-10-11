package s05_validation.val05_service_validation;

/**
 * <pre>
 * TEMAT: Walidacja w serwisach i w konfiguracji — @Validated na beanie i @ConfigurationProperties
 *        (service = serwis, warstwa logiki; method validation = walidacja metod, czyli parametrów i wyniku;
 *         proxy = pośrednik owijający bean; self-invocation = wywołanie własnej metody przez this;
 *         fail fast = szybka porażka, czyli błąd od razu przy starcie)
 *
 * W SKRÓCIE:
 *   Kontroler waliduje to, co przyszło z sieci. Ale serwis wołają też zadania cykliczne, kolejki i inne serwisy — one
 *   nie przechodzą przez kontroler. Dlatego serwis może sam pilnować swoich warunków: dajesz @Validated na klasie
 *   i zwykłe ograniczenia przy parametrach oraz wyniku metod. Zła wartość = ConstraintViolationException, zanim
 *   metoda się wykona. To działa przez proxy, więc ma pułapkę: wywołanie własnej metody przez this proxy omija.
 *   Podobnie @Validated na klasie @ConfigurationProperties sprawdza ustawienia przy STARCIE — aplikacja z błędną
 *   konfiguracją w ogóle się nie uruchomi (i dobrze).
 *
 * ANALOGIA: ochroniarz przy wejściu do magazynu i kontrola dostaw.
 *   Każdy, kto wchodzi głównym wejściem (przez proxy), jest sprawdzany. Pracownik, który w środku sam sobie poda
 *   paczkę z półki na półkę (this.metoda()), nie przechodzi przez bramkę. A przy otwarciu magazynu (start aplikacji)
 *   kierownik sprawdza, czy wszystkie klucze i kody (konfiguracja) są na miejscu — brak klucza = magazyn się nie otwiera.
 *
 * JAK TO DZIAŁA:
 *   @Service @Validated class PriceService { int gross(@Min(0) int net, @Max(100) int vat) }
 *     1. Boot rejestruje MethodValidationPostProcessor (ValidationAutoConfiguration)
 *     2. dla beana z @Validated powstaje proxy CGLIB (podklasa PriceService$$SpringCGLIB$$0), które trafia do kontenera
 *     3. innym beanom wstrzykiwane jest proxy; wywołanie gross(100, 500) idzie: proxy → walidacja → (OK) → prawdziwa metoda
 *     4. naruszenie → ConstraintViolationException ze ścieżką "gross.vatPercent"; wynik: "faultyDiscount.<return value>"
 *     5. wywołanie this.gross(...) w środku klasy omija proxy → bez walidacji
 *   @ConfigurationProperties + @Validated: przy bindowaniu ustawień sprawdza reguły; błąd → BindValidationException
 *   (opakowany w ConfigurationPropertiesBindException) i kontekst się nie uruchamia.
 *
 * SŁÓWKA:
 *   method validation = walidacja metod; parameter = parametr; return value = wartość zwracana; proxy = pośrednik;
 *   self-invocation = wywołanie własnej metody; bind = powiązać (ustawienia z polami klasy); fail fast = szybka
 *   porażka; startup failure = błąd przy starcie; command = polecenie; violation = naruszenie.
 *
 * ZOBACZ TEŻ: s02_configuration/Cfg02ConfigurationProperties (@ConfigurationProperties od podstaw),
 *             s05_validation/Val02ValidInControllers (walidacja na wejściu HTTP), s05_validation/Val03NestedAndGroups,
 *             t19_annotations_reflection/Annotations06DynamicProxy (jak działa proxy), s18_aop (proxy w Springu — dział AOP).
 * </pre>
 */
public final class Val05ServiceValidation {

    private Val05ServiceValidation() {
    }

    // =================================================================================================
    // 1. @Validated na serwisie — ograniczenia na parametrach
    // =================================================================================================
    //
    //   @Service @Validated
    //   public class PriceService {
    //       public int gross(@Min(0) int netGrosze, @Min(0) @Max(100) int vatPercent) { ... }
    //   }
    //
    // Wystarczą dwie rzeczy: @Validated na klasie (znacznik) i ograniczenia przy parametrach. Resztę robi Boot
    // (MethodValidationPostProcessor z ValidationAutoConfiguration). Przy złej wartości metoda NIE jest wywołana, a wołający
    // dostaje ConstraintViolationException z listą naruszeń; ścieżka błędu to "metoda.parametr" (np. "gross.vatPercent").
    //
    // DOBRA PRAKTYKA: waliduj na granicach — dane z zewnątrz w kontrolerze (DTO), a w serwisie warunki wstępne, na których
    //   serwis polega. Dlaczego? Serwis wołają też zadania cykliczne, kolejki wiadomości i inne serwisy; bez własnych
    //   ograniczeń zły argument wejdzie głębiej i da dziwny błąd (NullPointerException, zła kwota w bazie) zamiast
    //   czytelnego komunikatu.
    //
    // PUŁAPKA: ConstraintViolationException z serwisu, nieobsłużony, daje w API 500, a nie 400. Dlaczego? Wyjątek oznacza,
    //   że ktoś wywołał serwis z błędnym argumentem — to zwykle błąd programisty (kontroler powinien zatrzymać złe dane
    //   wcześniej), więc 500 jest uczciwe. Dane od użytkownika waliduj w kontrolerze; mapowanie wyjątków — dział
    //   s06_error_handling.
    //
    // Testy: Val05ServiceValidationTest.methodValidationOnService.

    // =================================================================================================
    // 2. Wynik metody i obiekt-parametr
    // =================================================================================================
    //
    //   public @Positive int faultyDiscount()                          // ograniczenie na WYNIKU, przed typem
    //   public @NotBlank String describe(@Valid @NotNull ProductCommand command)
    //
    // Ograniczenie na wyniku sprawdza, co metoda ZWRÓCIŁA — metoda się wykonuje, ale zły wynik jest odrzucany
    // (ścieżka "faultyDiscount.<return value>"). Dla parametru-obiektu @NotNull łapie null, a @Valid zagląda do pól
    // (ścieżki "describe.command.name"). Bez @Valid reguły rekordu ProductCommand nie zadziałają — jak w Val03NestedAndGroups.
    //
    // Testy: Val05ServiceValidationTest.returnValueAndObjectParameter.

    // =================================================================================================
    // 3. Pułapka: self-invocation — wywołanie przez this omija proxy
    // =================================================================================================
    //
    //   public int grossOfAll(List<Integer> nets, int vat) { ... this.gross(net, vat) ... }   // BEZ walidacji!
    //
    // Walidację robi proxy — pośrednik, który stoi PRZED prawdziwym obiektem. Kto wywołuje metodę z zewnątrz, trafia
    // najpierw w proxy. Ale wewnątrz klasy this to prawdziwy obiekt, nie proxy, więc wywołanie this.gross(...) idzie
    // prosto do metody. Zła stawka 500 przechodzi bez błędu. Tak samo zawodzą @Transactional, @Cacheable, @Async.
    //
    // PYTANIE REKRUTACYJNE: dlaczego adnotacja na metodzie (@Transactional, @Validated...) nie działa przy wywołaniu
    //   tej metody z innej metody tej samej klasy? Bo działanie adnotacji zapewnia proxy, a wywołanie przez this nie
    //   przechodzi przez proxy.
    //
    // Naprawy: (1) przenieś wywołującą logikę do INNEGO beana, który woła serwis przez wstrzyknięte proxy
    //   (BatchPriceService); (2) wyodrębnij walidowaną metodę do osobnej klasy; (3) walidację zrób ręcznie w metodzie
    //   wywołującej. Wstrzykiwanie samego siebie to ostateczność — zaciemnia kod.
    //
    // PUŁAPKA: refaktoryzacja „wyciągam wspólny kawałek do prywatnej metody i dopisuję jej ograniczenia”. Dlaczego
    //   to nie zadziała? Prywatną metodę wołasz przez this, więc ograniczenia nie są sprawdzane — a kod wygląda bezpiecznie.
    //   Sprawdzone testem Val05ServiceValidationTest.selfInvocation.
    //
    // Testy: Val05ServiceValidationTest.selfInvocation.

    // =================================================================================================
    // 4. @ConfigurationProperties + @Validated — błędna konfiguracja zatrzymuje start
    // =================================================================================================
    //
    //   @ConfigurationProperties("val05.shop") @Validated
    //   record ShopProperties(@NotBlank String name, @Min(1) @Max(1000) int maxItems, @Valid Support support)
    //
    // Przy tworzeniu beana ustawienia z application.properties są wiązane z rekordem i sprawdzane. Zły klucz
    // (val05.shop.max-items=0) → kontekst się nie uruchamia: ConfigurationPropertiesBindException, w środku BindException,
    // a na samym dole BindValidationException z listą błędów (maxItems, name, support.email). To „fail fast”.
    //
    // DOBRA PRAKTYKA: waliduj ustawienia obowiązkowe (adresy, limity, klucze). Dlaczego? Lepiej, żeby aplikacja nie
    //   wystartowała na serwerze z komunikatem „val05.shop.name: must not be blank”, niż żeby po tygodniu pierwszy
    //   klient trafił na NullPointerException.
    //
    // PUŁAPKA: brak @Validated na klasie (ShopPropertiesUnchecked) albo brak @Valid przy polu zagnieżdżonym
    //   (ShopPropertiesLoose). Dlaczego to groźne? W pierwszym przypadku reguły są, ale nikt ich nie uruchamia; w drugim
    //   pomijane są reguły w środku (support.email). Żaden z tych błędów nie zgłasza problemu — aplikacja startuje
    //   z pustą nazwą lub pustym e-mailem. Dodatkowo @Valid pomija null, więc ZAGNIEŻDŻONE ustawienia obowiązkowe
    //   wymagają @NotNull @Valid. Sprawdzone testem Val05ServiceValidationPropertiesTest.
    //
    // Demo bez uruchamiania aplikacji: ApplicationContextRunner (z spring-boot-test) buduje mały kontekst z podanymi
    // ustawieniami i pozwala zbadać błąd startu: context.getStartupFailure().
    //
    // Testy: Val05ServiceValidationPropertiesTest.

    // =================================================================================================
    // 5. Gdzie walidować — mapa warstw
    // =================================================================================================
    //
    //   wejście HTTP (kontroler, DTO)  → @Valid / @Validated(grupa)        (lekcje Val02, Val03)  → 400
    //   serwis                         → @Validated + ograniczenia metod   (ta lekcja)            → wyjątek (500 jeśli nieobsłużony)
    //   konfiguracja                   → @ConfigurationProperties+@Validated                      → start aplikacji się nie udaje
    //   baza danych                    → ograniczenia schematu (NOT NULL, UNIQUE, CHECK)          → ostatnia linia obrony
    //
    // Reguły zależne od stanu (unikalność nazwy, istnienie rekordu) sprawdza serwis w logice, nie adnotacja — adnotacja
    // nie widzi, co stanie się między sprawdzeniem a zapisem.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @Validated na klasie beana + ograniczenia przy parametrach/wyniku → ConstraintViolationException przed/po metodzie.
     *   • Robi to proxy (MethodValidationPostProcessor z autokonfiguracji Boota); innym beanom wstrzykiwane jest proxy.
     *   • Ścieżki: "metoda.parametr", "metoda.<return value>"; obiekt-parametr: @NotNull + @Valid.
     *   • Self-invocation: this.metoda() omija proxy → brak walidacji (też @Transactional i inne). Naprawa: inny bean.
     *   • @ConfigurationProperties + @Validated → zła konfiguracja = kontekst się nie uruchamia (BindValidationException).
     *   • Zagnieżdżone ustawienia: @Valid (a obowiązkowe: @NotNull @Valid); bez @Validated na klasie nic się nie sprawdza.
     *   • Kontroler: 400 dla danych od klienta; serwis: pilnuje warunków wstępnych; baza: ostatnia linia obrony.
     *
     * PYTANIA KONTROLNE:
     *   1. PYTANIE REKRUTACYJNE: dlaczego @Validated (a także @Transactional) nie działa przy wywołaniu metody przez this?
     *   2. Co się stanie? Wołasz przez wstrzyknięty bean priceService.gross(100, 500), gdzie vatPercent ma @Max(100).
     *   3. Co się stanie? Wewnątrz PriceService metoda grossOfAll woła this.gross(100, 500).
     *   4. Co się stanie? Metoda z ograniczeniem {@code public @Positive int faultyDiscount()} zwraca -5.
     *   5. ZNAJDŹ BŁĄD: ShopPropertiesUnchecked ma @NotBlank String name, a aplikacja startuje z pustą nazwą. Czego brakuje?
     *   6. Co się stanie? Ustawiasz val05.shop.max-items=0 (reguła @Min(1)) w ShopProperties z @Validated.
     *   7. ZNAJDŹ BŁĄD: w ShopPropertiesLoose pole {@code Support support} ma w środku @NotBlank String email, a pusty e-mail
     *      przechodzi. Dlaczego?
     *   8. Dlaczego serwis może mieć własne ograniczenia, skoro kontroler już waliduje DTO?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Val05ServiceValidationExercisesTest
    // =================================================================================================

    /*
     * ĆWICZENIE 1 (łatwe): w Exercise1Service włącz walidację metod (adnotacja nad klasą) i dopisz ograniczenia:
     *   priceGrosze co najmniej 0, percent od 0 do 100. Wywołanie discountedPrice(1000, 150) ma rzucić
     *   ConstraintViolationException, a discountedPrice(1000, 15) ma zwrócić 850.
     *   Podpowiedź: {@code @Validated} (ze Springa), {@code @Min}, {@code @Max}.
     *
     * ĆWICZENIE 2 (średnie): w Exercise2Service.codeFor dopisz ograniczenia: parametr obowiązkowy i sprawdzany w głąb
     *   (ProductCommand ma własne reguły), a WYNIK zgodny ze wzorcem [A-Z]{3}-\d{4}. codeFor(null) i codeFor z pustą nazwą
     *   mają rzucić ConstraintViolationException, tak samo codeFor z nazwą "ab" (wynik "AB-0100" nie pasuje do wzorca);
     *   nazwa "mleko" i cena 349 mają dać "MLE-0349".
     *   Podpowiedź: {@code @Pattern(regexp = ...)} przed typem wyniku, {@code @Valid @NotNull} przy parametrze.
     *
     * ĆWICZENIE 3 (trudniejsze): w ExerciseProperties włącz walidację ustawień i dopisz reguły: host niepusty, port od 1
     *   do 65535, admin OBOWIĄZKOWY i sprawdzany w głąb, a jego email niepusty i poprawny. Test uruchamia ApplicationContextRunner:
     *   poprawne ustawienia → kontekst startuje; zły port, pusty host, brak admina albo zły e-mail → startup failure
     *   z przyczyną BindValidationException.
     *   Podpowiedź: sekcja 4 (@Validated, @Valid) i pułapka z null (@NotNull obok @Valid).
     */

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo walidację (transakcję, cache) wykonuje proxy otaczające bean. Wywołanie z zewnątrz trafia w proxy, a this
     *      wewnątrz klasy wskazuje prawdziwy obiekt — proxy zostaje pominięte.
     *   2. ConstraintViolationException, metoda się nie wykonuje; ścieżka błędu "gross.vatPercent". Sprawdzone testem
     *      Val05ServiceValidationTest.methodValidationOnService.
     *   3. Brak wyjątku, metoda zwraca 600 (100 + 100 * 500 / 100) — walidacja pominięta.
     *      Sprawdzone testem Val05ServiceValidationTest.selfInvocation.
     *   4. Metoda się wykonuje, ale wynik jest odrzucany: ConstraintViolationException, ścieżka "faultyDiscount.<return value>".
     *      Sprawdzone testem Val05ServiceValidationTest.returnValueAndObjectParameter.
     *   5. Brakuje @Validated na klasie z @ConfigurationProperties — bez niej reguły nie są uruchamiane.
     *      Sprawdzone testem Val05ServiceValidationPropertiesTest.
     *   6. Kontekst się nie uruchamia: ConfigurationPropertiesBindException → BindException → BindValidationException
     *      (błąd maxItems: must be greater than or equal to 1). Sprawdzone testem Val05ServiceValidationPropertiesTest.
     *   7. Brakuje @Valid przy polu support — bez niego walidacja nie schodzi do zagnieżdżonych ustawień.
     *      Sprawdzone testem Val05ServiceValidationPropertiesTest.
     *   8. Serwis wołają też inne ścieżki niż kontroler (zadania cykliczne, kolejki, inne serwisy, testy) — własne
     *      ograniczenia chronią go niezależnie od tego, kto go woła.
     */
    // </editor-fold>
}
