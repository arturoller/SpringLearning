package s05_validation.val03_nested_and_groups;

import jakarta.validation.Validator;

import java.util.List;

/**
 * <pre>
 * TEMAT: Walidacja obiektów zagnieżdżonych, elementów kolekcji i grupy walidacji
 *        (nested = zagnieżdżony, leżący w środku innego obiektu; cascade = kaskada, przejście walidacji w głąb;
 *        container element = element kontenera, np. element listy; group = grupa, zestaw reguł wybierany w danej sytuacji)
 *
 * W SKRÓCIE:
 *   Realne dane mają strukturę: zamówienie ma adres i listę pozycji. Walidator domyślnie sprawdza tylko pola
 *   obiektu, który dostał — do obiektów w środku NIE zagląda, dopóki nie dopiszesz {@code @Valid} na polu (kaskada).
 *   Ograniczenia mogą też stać na elementach kolekcji: {@code List<@NotBlank String>}. A gdy ten sam rekord służy
 *   do tworzenia i do aktualizacji, ale reguły są różne (id: przy tworzeniu puste, przy aktualizacji wymagane),
 *   używamy grup: {@code @Null(groups = OnCreate.class)}.
 *
 * ANALOGIA: kontrola paczki z towarem u kuriera.
 *   Kurier sprawdza paczkę z zewnątrz (etykieta, waga). Pudełka w środku (adres, pozycje) otworzy tylko wtedy, gdy na
 *   paczce jest naklejka „sprawdź zawartość” ({@code @Valid}). Grupy to różne listy kontrolne: inna dla przyjęcia
 *   nowej przesyłki (OnCreate), inna dla zmiany istniejącej (OnUpdate).
 *
 * JAK TO DZIAŁA:
 *   PRZED (bez kaskady — błąd w adresie niewidoczny):
 *     record Order(@NotNull Address address, ...)              validate(order) → 0 naruszeń, choć ulica jest pusta
 *   PO (z kaskadą):
 *     record Order(@NotNull @Valid Address address, ...)       → address.street: Ulica jest wymagana
 *
 *   Ścieżka pola (property path) mówi, GDZIE jest błąd — zapamiętaj trzy kształty:
 *     address.street                  pole w obiekcie zagnieżdżonym
 *     lines[1].sku                    pole w elemencie listy (numeracja od 0)
 *     notes[2].{@code <list element>}       element listy (to on sam jest zły, np. pusty napis)
 *
 *   Grupy: {@code interface OnCreate extends Default {}} + {@code @Null(groups = OnCreate.class)}.
 *     validator.validate(dto)                  → grupa Default (reguły BEZ groups)
 *     validator.validate(dto, OnCreate.class)  → reguły z groups = OnCreate + (bo extends Default) reguły Default
 *     w kontrolerze: {@code @Validated(OnCreate.class) @RequestBody ProductDto dto}
 *
 * SŁÓWKA:
 *   cascade = kaskada; nested object = obiekt zagnieżdżony; container element = element kontenera (listy, mapy);
 *   property path = ścieżka właściwości; group = grupa walidacji; default group = grupa domyślna (reguły bez groups);
 *   marker interface = interfejs-znacznik (pusty, służy tylko jako etykieta); create = tworzenie; update = aktualizacja.
 *
 * ZOBACZ TEŻ: s05_validation/Val01BeanValidation (podstawy, null a reguły), s05_validation/Val02ValidInControllers
 *             ({@code @Valid} w kontrolerze), s05_validation/Val04CustomValidators (własne reguły),
 *             t09_records/Records01Basics (rekordy), t12_collections/Collections02Lists (listy).
 * </pre>
 */
public final class Val03NestedAndGroups {

    private Val03NestedAndGroups() {
    }

    // =================================================================================================
    // 1. KASKADA @Valid: adres i pozycje zamówienia (OrderRequest)
    // =================================================================================================
    //
    //   record OrderRequest(
    //       @NotBlank @Email String customerEmail,
    //       @NotNull @Valid ShippingAddress address,        // kaskada do pól adresu
    //       @NotEmpty List<@Valid OrderLine> lines,         // kaskada do KAŻDEJ pozycji
    //       List<@NotBlank String> notes) { }
    //
    // Walidator wchodzi do adresu i do każdej pozycji i zgłasza błędy z pełną ścieżką: address.street, lines[0].quantity.
    // Kaskada działa wielopoziomowo: gdyby adres miał pole z @Valid, walidacja zeszłaby jeszcze głębiej.
    // To samo działa w kontrolerze — @Valid @RequestBody OrderRequest (OrderController): FieldError.getField()
    // zwraca "address.city" albo "lines[0].quantity" (test section6).
    //
    // Test: section1_cascade, section6_cascadeInController.

    // =================================================================================================
    // 2. OGRANICZENIA NA ELEMENTACH KONTENERA i dwie warstwy listy
    // =================================================================================================
    //
    //   @NotEmpty List<@Valid OrderLine> lines     — @NotEmpty dotyczy LISTY (nie może być pusta),
    //                                                 @Valid dotyczy KAŻDEGO ELEMENTU (zajrzyj do środka)
    //   List<@NotBlank String> notes               — każda uwaga niepusta; zły element: notes[1].<list element>
    //
    // Adnotacja stoi „przy typie elementu”, w nawiasach ostrych. @Valid List<OrderLine> (adnotacja przy polu)
    // działa równoważnie dla kaskady — sprawdzone testem.
    //
    // PUŁAPKA: List<@NotBlank String> a element null — @NotBlank odrzuca także null (w przeciwieństwie
    //   do @Size, patrz Val01), więc lista Arrays.asList("a", null) da naruszenie. Uwaga: List.of(...)
    //   w ogóle nie przyjmuje null (rzuci NullPointerException przy tworzeniu), więc w danych z JSON-a null
    //   w liście oznacza po prostu błędne dane do odrzucenia.
    //
    // PUŁAPKA: @Valid na polu, które jest null, NIE zgłasza błędu — kaskada pomija brakujący obiekt. Dlaczego?
    //   „Czy obiekt jest podany” to osobna reguła (jak w Val01: null jest poprawny dla większości reguł). Gdy adres jest
    //   wymagany, napisz @NotNull @Valid.
    //
    // Test: section2_containerElements.

    // =================================================================================================
    // 3. PUŁAPKA: BRAK @Valid NA POLU ZAGNIEŻDŻONYM (OrderRequestNoCascade)
    // =================================================================================================
    //
    // PUŁAPKA: rekord adresu ma piękne ograniczenia (@NotBlank na ulicy), a zamówienie trzyma go w polu
    //   bez @Valid. Dlaczego to groźne? @Valid przy parametrze kontrolera sprawdza tylko własne pola
    //   zamówienia — adres z pustą ulicą i błędnym kodem przechodzi bez ostrzeżenia, a błąd wychodzi dopiero
    //   w bazie lub przy dostawie. Za każdym razem, gdy w rekordzie jest pole z innym rekordem lub listą rekordów,
    //   zapytaj: czy jest @Valid?
    //
    // Test: section3_noCascade (ten sam błędny adres: z kaskadą → 400, bez kaskady → 201; test section6).

    // =================================================================================================
    // 4. GRUPY WALIDACJI: OnCreate, OnUpdate, Default (ProductDto)
    // =================================================================================================
    //
    //   interface OnCreate extends Default {}      // grupa = pusty interfejs-znacznik
    //   record ProductDto(
    //       @Null(groups = OnCreate.class) @NotNull(groups = OnUpdate.class) Long id,   // reguły zależne od sytuacji
    //       @NotBlank String name,                                                      // bez groups = grupa Default
    //       ...)
    //
    //   validate(dto)                    sprawdza grupę Default (name, price)
    //   validate(dto, OnCreate.class)    sprawdza OnCreate; ponieważ OnCreate extends Default — także name, price
    //   w kontrolerze: @Validated(OnCreate.class) @RequestBody ProductDto (przy PARAMETRZE).
    //   @Valid (zwykłe) zawsze sprawdza tylko Default — reguły z groups wtedy milczą.
    //
    // PUŁAPKA: grupa, która NIE rozszerza Default (u nas Strict). Dlaczego to zaskakuje? Walidacja
    //   validate(dto, Strict.class) sprawdza WYŁĄCZNIE reguły przypisane do Strict, a wszystkie zwykłe
    //   (name, price) są pominięte — dto z pustą nazwą wygląda na poprawne. Dlatego grupy akcji (OnCreate/OnUpdate)
    //   pisz z extends Default; albo wskaż obie grupy naraz: validate(dto, Default.class, Strict.class).
    //
    // DOBRA PRAKTYKA: zamiast grup rozważ DWA osobne rekordy (CreateProductRequest bez id, UpdateProductRequest z id).
    //   Dlaczego? Typ sam mówi, jakie dane są dozwolone, nie ma niespodzianek z Default i nie da się pomylić grupy.
    //   Grupy są dobre, gdy rekordów byłoby za dużo albo reguły różnią się tylko kilkoma polami.
    //
    // Test: section4_groups, section5_groupsInController.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Obiekt w polu: @Valid na polu (kaskada). Wymagany obiekt: @NotNull @Valid. Null + samo @Valid = cisza.
     *   • Lista rekordów: @NotEmpty List<@Valid Rekord>; elementy: List<@NotBlank String>.
     *   • Ścieżki błędów: address.street, lines[0].quantity, notes[1].<list element>.
     *   • Bez @Valid na polu zagnieżdżonym adres/pozycje nie są sprawdzane (a nic nie ostrzega).
     *   • Grupa = pusty interfejs; reguła: groups = OnCreate.class; wybór: @Validated(OnCreate.class) przy parametrze.
     *   • Reguły bez groups = grupa Default; @Valid sprawdza tylko Default.
     *   • Grupa bez extends Default pomija reguły domyślne. Alternatywa dla grup: dwa osobne rekordy.
     *
     * PYTANIA KONTROLNE:
     *   1. Co się stanie? {@code validator.validate(new OrderRequestNoCascade("jan@example.com",
     *      new ShippingAddress("", "", "x"), List.of(new OrderLine("ELE-001", 1)), null))} — ile naruszeń i dlaczego?
     *   2. Jaką ścieżkę pola ma naruszenie dla trzeciej pozycji zamówienia (lines) z błędnym SKU?
     *   3. ZNAJDŹ BŁĄD: record Return(@NotEmpty List<OrderLine> lines) — autor twierdzi, że każda pozycja
     *      jest sprawdzana. Co faktycznie robi ta adnotacja i jak naprawić?
     *   4. Co się stanie? validator.validate(new ProductDto(null, "Kawa", BigDecimal.ONE, null), Strict.class) —
     *      ile naruszeń i na jakim polu? Dlaczego nie zero?
     *   5. Co się stanie? PUT /api/products/5 z ciałem {"name":"Kawa","price":1} (bez id) — jaki status i dlaczego?
     *   6. Dlaczego grupy OnCreate i OnUpdate warto pisać z extends Default?
     *   7. Pole @Valid ShippingAddress address ma wartość null. Zgłosi naruszenie? Jak wymusić obecność adresu?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Val03NestedAndGroupsExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć posortowaną listę ścieżek pól (getPropertyPath().toString()) wszystkich naruszeń.
     * Opcjonalne grupy (zmienna liczba argumentów) przekaż do validator.validate(object, groups).
     * Poprawny obiekt → pusta lista.
     */
    static List<String> paths(Validator validator, Object object, Class<?>... groups) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // ĆWICZENIE 2 (średnie): otwórz rekord ReturnRequest (plik ReturnRequest.java) i dopisz adnotacje: adres zwrotu
    // ma być podany ORAZ sprawdzany w środku; lista pozycji niepusta, a każda pozycja sprawdzana w środku.
    // Podpowiedź: sekcje 1 i 2 — @NotNull @Valid oraz @NotEmpty List<@Valid ...>.
    //
    // ĆWICZENIE 3 (trudniejsze): otwórz rekord EmployeeDto i dopisz adnotacje z grupami OnCreate/OnUpdate:
    // id — przy tworzeniu puste, przy aktualizacji wymagane; name — zawsze niepuste (nie same spacje);
    // salary — przy aktualizacji wymagane, a gdy podane (zawsze) — większe od zera.
    // Podpowiedź: reguły bez groups wchodzą do OnCreate i OnUpdate, bo te grupy rozszerzają Default; @Positive
    // przepuszcza null (Val01), więc „wymagane przy aktualizacji” to osobne @NotNull(groups = OnUpdate.class).

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Zero naruszeń. Pola zagnieżdżone (adres) nie mają @Valid, więc pusta ulica i zły kod są niewidoczne,
     *      a pola własne zamówienia są poprawne. Sprawdzone testem (section3).
     *   2. lines[2].sku (numeracja od zera). Sprawdzone testem (section1 — ten sam kształt dla lines[1].sku).
     *   3. @NotEmpty dotyczy tylko samej listy (nie może być pusta); elementy nie są sprawdzane. Naprawa:
     *      @NotEmpty List<@Valid OrderLine> lines. Sprawdzone testem (section3: lista bez @Valid
     *      nie zgłasza błędu pozycji, a z @Valid — section2).
     *   4. Jedno naruszenie na polu category. Strict nie rozszerza Default, więc sprawdza tylko reguły z groups = Strict
     *      (kategoria wymagana), a reguły bez grupy są pominięte. Sprawdzone testem (section4).
     *   5. 400 (MethodArgumentNotValidException): grupa OnUpdate wymaga id. Sprawdzone testem (section5).
     *   6. Bez extends Default walidacja grupy sprawdzałaby wyłącznie reguły z tą grupą, a name i price (bez groups)
     *      zostałyby pominięte. Sprawdzone testem (section4: Strict kontra OnCreate).
     *   7. Nie — kaskada pomija null. Wymuś obecność adnotacją @NotNull @Valid. Sprawdzone testem (section2).
     */
    // </editor-fold>
}
