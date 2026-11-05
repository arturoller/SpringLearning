package s05_validation.val03_nested_and_groups;

/**
 * <pre>
 * TEMAT: Walidacja zagnieżdżona, kolekcje i grupy walidacji
 *        (nested = zagnieżdżony, obiekt w obiekcie; cascade = kaskada, "schodzenie w głąb"; collection = kolekcja,
 *         lista elementów; group = grupa, nazwany zestaw reguł; sequence = sekwencja, ustalona kolejność;
 *         marker interface = interfejs-znacznik, pusty interfejs służący tylko jako nazwa)
 *
 * W SKRÓCIE:
 *   Prawdziwe dane to nie płaski formularz: zamówienie ma adres (obiekt w obiekcie) i listę pozycji. Bean Validation
 *   NIE schodzi w głąb samo — musisz je o to poprosić adnotacją {@code @Valid} na polu albo na elemencie listy.
 *   Ścieżka błędu mówi wtedy dokładnie gdzie: {@code address.city}, {@code lines[1].quantity}. Drugi temat: ten sam
 *   obiekt bywa inny przy tworzeniu (id musi być puste) i przy zmianie (id musi być podane). Rozróżniasz to GRUPAMI
 *   walidacji — nazwanymi zestawami reguł, które wybierasz przez {@code @Validated(Grupa.class)}.
 *
 * ANALOGIA: odprawa bagażu przed lotem.
 *   Pasażer (zamówienie) ma walizkę (adres) i torby (pozycje). Strażnik sprawdza, czy pasażer ma bilet — ale do walizki
 *   zajrzy tylko wtedy, gdy ma polecenie "otwórz i sprawdź w środku" (to {@code @Valid}). Bez polecenia widzi tylko
 *   "walizka jest". Grupy to różne bramki: bramka "wylot" (tworzenie) ma inne reguły niż bramka "przylot" (zmiana),
 *   ale obie sprawdzają też paszport (reguły wspólne, czyli grupa Default).
 *
 * JAK TO DZIAŁA:
 *   1. {@code validator.validate(obiekt)} sprawdza reguły NA TYM obiekcie. Pole z innym obiektem sprawdza tylko własnymi
 *      adnotacjami ({@code @NotNull} — "jest czy go nie ma").
 *   2. {@code @Valid} na polu mówi: "wejdź do tego obiektu i sprawdź jego reguły", i tak dalej w głąb (kaskada).
 *   3. {@code List<@Valid OrderLine>} i {@code List<@NotBlank String>} to ograniczenia na ELEMENTACH kolekcji; ścieżka dostaje
 *      indeks: {@code lines[1].quantity}.
 *   4. Każde ograniczenie ma atrybut {@code groups} (domyślnie: grupa {@code Default}). Walidator uruchamia tylko reguły
 *      z żądanej grupy: {@code validator.validate(obiekt, OnCreate.class)}.
 *   5. W kontrolerze grupę wskazuje {@code @Validated(OnCreate.class)} przy {@code @RequestBody} — {@code @Valid} grup nie
 *      przyjmuje.
 *   6. {@code @GroupSequence} ustala kolejność grup i przerywa po pierwszej, która znajdzie błędy.
 *
 * SŁÓWKA:
 *   address = adres; line = wiersz, pozycja; tag = etykieta; recipient = odbiorca; shipment = przesyłka; coupon =
 *   kupon; basic = podstawowy; advanced = zaawansowany; version = wersja; extend = rozszerzać (dziedziczyć po).
 *
 * ZOBACZ TEŻ: s05_validation/Val01BeanValidation (adnotacje i Validator),
 *             s05_validation/Val02ValidInControllers (@Valid @RequestBody, MethodArgumentNotValidException),
 *             s05_validation/Val04CustomValidators (własna reguła obejmująca kilka pól),
 *             s04_web_rest/Rest04Json (DTO i Jackson, rekordy),
 *             s04_web_rest/Rest05CrudApi (tworzenie i zmiana jako osobne czynności),
 *             s06_error_handling/Err04ProblemDetail (jak oddać klientowi listę błędów ze ścieżkami),
 *             t12_collections/Collections02Lists (listy w Javie).
 * </pre>
 */
public final class Val03NestedAndGroups {

    private Val03NestedAndGroups() {
    }

    // =================================================================================================
    // 1. OBIEKT W OBIEKCIE: @Valid (UncheckedNesting, Address, OrderRequest)
    // =================================================================================================
    //
    //     public record Forgot(@NotNull Address address, …)           // @NotNull: "adres jest", nic więcej
    //     public record Cascade(@NotNull @Valid Address address, …)    // + "sprawdź też wnętrze adresu"
    //
    // Adres {@code new Address("Długa 1", " ", "80-001")} ma puste miasto (reguła @NotBlank w Address). Test
    // Val03NestedAndGroupsTest:
    //   • Forgot (bez @Valid)  → ZERO naruszeń: puste miasto przechodzi niezauważone;
    //   • Cascade (z @Valid)   → jedno naruszenie, ścieżka {@code address.city} — kropka oddziela poziomy.
    //
    // Dwie osobne sprawy, które łatwo pomylić:
    //   • {@code @NotNull} odpowiada na pytanie "czy adres w ogóle jest?";
    //   • {@code @Valid} odpowiada na pytanie "czy to, co w adresie, jest poprawne?".
    // Zwykle potrzebujesz obu: {@code @NotNull @Valid Address address}. Samo {@code @Valid} przy null NIE zgłosi błędu —
    // pole null jest pomijane.
    //
    // PUŁAPKA: zapomniane {@code @Valid} na polu zagnieżdżonym. Dlaczego? To najczęstsza dziura w walidacji: kontroler ma
    //   {@code @Valid}, DTO ma piękne reguły w adresie, a mimo to do bazy trafia zamówienie z pustym miastem — bo pole
    //   {@code address} nie ma kaskady. Brak błędu kompilacji, brak ostrzeżenia, brak wyjątku. Jedyną obroną jest test z
    //   błędem w zagnieżdżonym obiekcie (jak w Val03NestedAndGroupsTest.withoutValidNestedObjectIsNotChecked).
    //
    // DOBRA PRAKTYKA: w testach sprawdzaj PEŁNĄ ścieżkę błędu ({@code address.city}), a nie tylko "jest 400". Dlaczego?
    //   Sama liczba błędów albo status nie wykryje, że kaskada przestała działać na jednym z poziomów.

    // =================================================================================================
    // 2. KOLEKCJE: List<@Valid ...>, @NotEmpty, ograniczenia na elementach (OrderRequest, OrderLine)
    // =================================================================================================
    //
    //     @NotEmpty List<@NotNull @Valid OrderLine> lines     // lista niepusta, a każda pozycja sprawdzona w głąb
    //     List<@NotBlank String> tags                         // etykiety opcjonalne, ale żadna pusta
    //
    // Adnotację kładziesz PRZED typem elementu (w nawiasie ostrym), nie przed polem: to ograniczenie dotyczy elementu, a
    // nie całej listy (type-use annotation — adnotacja przy użyciu typu). Ścieżki z testów:
    //
    //   Co jest złe                          | Ścieżka w walidatorze (Val03NestedAndGroupsTest) | Ścieżka w Springu (FieldError)
    //   -------------------------------------+--------------------------------------------------+----------------------------
    //   druga pozycja bez nazwy              | lines[1].productName                             | lines[1].productName
    //   druga pozycja z ilością 0            | lines[1].quantity                                | lines[1].quantity
    //   pusta lista                          | lines                                            | lines
    //   druga etykieta pusta                 | tags[1].<list element>                           | tags[1]
    //   element null w liście pozycji        | lines[1].<list element>                          | lines[1]
    //
    // Zwróć uwagę na drobną różnicę: w czystym walidatorze ścieżka elementu prostego typu (String) kończy się tekstem
    // {@code <list element>}; Spring przy tworzeniu {@code FieldError} (Val02ValidInControllers) obcina go do {@code tags[1]}.
    //
    // PUŁAPKA: element {@code null} na liście. Dlaczego? {@code @Valid} i większość ograniczeń przepuszcza null, więc JSON
    //   {@code "lines":[{…}, null]} jest poprawny, a w kodzie czeka cię NullPointerException na drugim elemencie. Dlatego
    //   {@code List<@NotNull @Valid OrderLine>}: najpierw "element jest", potem "element jest poprawny" (test
    //   nullElementsInList).
    //
    // PUŁAPKA: {@code @NotEmpty} na samej liście nie sprawdza elementów, a {@code @Valid} na samej liście nie wymaga,
    //   żeby ona w ogóle miała elementy. Dlaczego? To trzy osobne pytania: czy lista jest (null), czy ma elementy
    //   (@NotEmpty), czy elementy są poprawne (@Valid na elemencie). Pole opcjonalne ({@code tags}) może być null —
    //   ale gdy jest, jego elementy nadal podlegają regułom.

    // =================================================================================================
    // 3. GRUPY: OnCreate / OnUpdate (OrderRequest, OrderController, PlainGroupsForm)
    // =================================================================================================
    //
    // Problem: pole {@code id}. Przy tworzeniu zamówienia klient NIE może go podać (nadaje serwer — inaczej nadpisze cudze).
    // Przy zmianie MUSI je podać (inaczej nie wiadomo, co zmieniamy). Jedno DTO, dwie sprzeczne reguły.
    //
    //     public interface OnCreate extends Default { }     // pusty interfejs-znacznik = nazwa grupy
    //     public interface OnUpdate extends Default { }
    //
    //     @Null(groups = OnCreate.class) @NotNull(groups = OnUpdate.class) Long id
    //
    // W kontrolerze:
    //     @PostMapping public … create(@Validated(OnCreate.class) @RequestBody OrderRequest order)
    //     @PutMapping  public … update(@Validated(OnUpdate.class) @RequestBody OrderRequest order)
    //
    // {@code @Valid} nie ma atrybutu na grupy — dlatego tu @Validated Springa (Val02ValidInControllers, sekcja 1).
    //
    // CO DZIEJE SIĘ Z REGUŁAMI BEZ GRUPY? Mają grupę {@code Default}. Zasada (sprawdzona testami):
    //   • {@code validate(obiekt)} i {@code @Validated} bez argumentu → grupa Default: widać tylko reguły bez {@code groups};
    //     reguły z {@code groups = OnCreate.class} są POMIJANE (id = 5 przy tworzeniu przechodzi — pułapka poniżej);
    //   • {@code validate(obiekt, OnCreate.class)} → reguły oznaczone OnCreate, A TAKŻE reguły Default, o ile OnCreate
    //     {@code extends Default} (tak jest u nas: adres sprawdzany także przy tworzeniu);
    //   • grupa, która NIE rozszerza Default (PlainGroupsForm.Create) → reguły bez grupy są POMIJANE. W teście
    //     {@code @NotBlank name} nie zadziałało, gdy waliduje się tylko Create.
    // Można podać kilka grup naraz: {@code validate(obiekt, Create.class, Default.class)}.
    //
    // PUŁAPKA: grupa niedziedzicząca po {@code Default}. Dlaczego? Programista dodaje {@code @Validated(Create.class)} i
    //   z zaskoczeniem odkrywa, że WSZYSTKIE zwykłe reguły (@NotBlank name…) przestały działać. Adnotacje bez grupy należą
    //   do Default, a Default nie jest włączany automatycznie, gdy wskażesz inną grupę. Rozwiązanie: grupy własne
    //   rozszerzają Default (jak OnCreate/OnUpdate tutaj).
    //
    // PUŁAPKA: {@code @Validated} bez grupy tam, gdzie liczysz na reguły grupowe. Dlaczego? Metoda
    //   {@code createDefaultGroup} (OrderController) ma zwykłe {@code @Validated} — grupa Default — więc {@code id = 5}
    //   przy tworzeniu przechodzi, a odpowiedź to 201 (test validatedWithoutGroupIgnoresCreateRules). Nic nie ostrzega.

    // =================================================================================================
    // 4. KOLEJNOŚĆ: @GroupSequence (CouponForm, CouponChecks)
    // =================================================================================================
    //
    // Domyślnie wszystkie reguły sprawdzane są "naraz" w nieokreślonej kolejności. Czasem jedne sprawdzenia są tanie
    // (czy podano kod?), a inne drogie (czy kod istnieje w bazie? czy kupon nie był użyty?). Po co uruchamiać drogie,
    // gdy kodu w ogóle nie podano?
    //
    //     @GroupSequence({Basic.class, Advanced.class})
    //     public interface InOrder { }                       // sekwencja: najpierw Basic, potem Advanced
    //
    //     CouponForm: @NotBlank(groups = Basic) + @Pattern(groups = Advanced) na code,
    //                 @NotNull(groups = Basic) + @Max(groups = Advanced) na discountPercent
    //
    //   validate(new CouponForm("", 99), Basic.class, Advanced.class) → 3 błędy (code:NotBlank, code:Pattern, discountPercent:Max)
    //   validate(new CouponForm("", 99), InOrder.class)               → 1 błąd  (code:NotBlank) — Advanced w ogóle nie ruszone
    //   validate(new CouponForm("zly", 99), InOrder.class)            → 2 błędy (code:Pattern, discountPercent:Max)
    //
    // Reguła: grupy z sekwencji są sprawdzane po kolei, a PO PIERWSZEJ GRUPIE, KTÓRA ZNALAZŁA BŁĘDY, przerywamy. Klient
    // dostaje wtedy najpierw poprawki podstawowe; po ich naprawie — kolejne. To także ochrona przed błędami: reguła
    // zaawansowana nie musi radzić sobie z wartością null, bo grupa Basic już ją odrzuciła.
    //
    // Sekwencję można też położyć na klasie, żeby zmienić kolejność wewnątrz samej grupy Default (przydatne w
    // Val04CustomValidators dla kosztownych własnych walidatorów).
    //
    // DOBRA PRAKTYKA: używaj {@code @GroupSequence} oszczędnie — tylko tam, gdzie część reguł naprawdę jest kosztowna
    //   (baza, sieć) albo wymaga, żeby wcześniejsze reguły przeszły. Dlaczego? To dodatkowa warstwa, która utrudnia
    //   zrozumienie, a klient dostaje błędy "partiami" (poprawia, wysyła, dostaje następne), co bywa irytujące.

    // =================================================================================================
    // 5. DOBRA PRAKTYKA: OSOBNE DTO ZAMIAST GRUP (OrderV2Controller)
    // =================================================================================================
    //
    //     public record CreateOrder(@NotNull @Valid Address address, @NotEmpty List<…> lines, …) { }       // bez id
    //     public record UpdateOrder(@NotNull Integer version, @NotNull @Valid Address address, …) { }      // z version
    //
    // Grupy działają, ale mają koszty: reguły jednego DTO są rozproszone po grupach, trzeba pamiętać o
    // {@code @Validated(Grupa.class)} w KAŻDEJ metodzie (zapomnisz — pułapka z sekcji 3), a pola, które w danym przypadku
    // nie mają sensu, wciąż istnieją w DTO. Osobne DTO:
    //   • reguły czyta się od razu — brak groups, brak {@code Default};
    //   • pola niedozwolone przy tworzeniu (id) w ogóle nie istnieją — nie da się ich "przypadkiem" przyjąć. Boot nie każe
    //     Jacksonowi odrzucać nieznanych pól, więc {@code "id": 7} w JSON-ie jest po prostu pomijane (test separateDtoForCreate);
    //   • zwykłe {@code @Valid} wystarcza (żadnych specjalnych adnotacji Springa);
    //   • obiekty wejściowe mogą się potem niezależnie rozwijać (np. UpdateOrder dostaje version).
    // To ten sam wzorzec, co oddzielne DTO wejściowe/wyjściowe w Rest05CrudApi, sekcja 3 (obrona przed "mass assignment").
    //
    // Grupy zostaw na sytuacje, gdy DTO naprawdę jest jedno (np. wspólny formularz) albo gdy dochodzi do tego walidacja
    // w serwisie lub bazie, gdzie nie ma kontrolera (Val05ServiceValidation).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Zagnieżdżony obiekt: {@code @NotNull @Valid Address address} — {@code @Valid} wchodzi w głąb; bez niego wnętrze NIE
     *     jest sprawdzane. Ścieżka: {@code address.city}.
     *   • Kolekcje: {@code @NotEmpty List<@NotNull @Valid OrderLine> lines}, {@code List<@NotBlank String> tags}. Ścieżka:
     *     {@code lines[1].quantity}, {@code tags[1]}. Element null przechodzi, jeśli nie ma @NotNull.
     *   • Grupy: interfejsy-znaczniki; {@code groups = OnCreate.class} na regule; w kontrolerze
     *     {@code @Validated(OnCreate.class)} ({@code @Valid} grup nie przyjmuje).
     *   • Reguły bez grupy = Default. Grupa rozszerzająca Default dołącza je; grupa niedziedzicząca po Default je POMIJA.
     *   • {@code @Validated} bez argumentu = Default (reguły grupowe nie działają!).
     *   • {@code @GroupSequence({A.class, B.class})}: A przed B; po pierwszej grupie z błędami koniec.
     *   • Osobne DTO dla tworzenia i zmiany są zwykle prostsze i bezpieczniejsze niż grupy.
     *
     * PYTANIA KONTROLNE:
     *   1. Po co {@code @Valid} na polu {@code Address address}, skoro jest już {@code @NotNull}? Co sprawdza każde z nich?
     *   2. Co się stanie? DTO ma {@code @NotNull Address address}, bez {@code @Valid}; klient wysyła adres z pustym miastem
     *      (a {@code Address.city} ma {@code @NotBlank}). Ile będzie naruszeń?
     *   3. Jaką ścieżkę ma błąd w drugiej pozycji listy {@code lines}, gdy jej {@code quantity} wynosi 0?
     *   4. Co się stanie? JSON zawiera {@code "lines":[{…poprawna pozycja…}, null]}, a pole to {@code List<@Valid OrderLine>}
     *      (bez @NotNull na elemencie). Czy walidacja zgłosi błąd i czym to grozi?
     *   5. Co się stanie? Metoda ma {@code @Validated @RequestBody OrderRequest order} (bez grupy), a reguła brzmi
     *      {@code @Null(groups = OnCreate.class) Long id}. Klient przy tworzeniu wysyła {@code "id": 5}. Jaki status?
     *   6. ZNAJDŹ BŁĄD: {@code interface Create { }} (bez {@code extends Default}) i {@code @Validated(Create.class)} w
     *      kontrolerze. Reguła {@code @NotBlank String name} (bez grupy) nie wyłapuje pustej nazwy. Dlaczego i jak naprawić?
     *   7. Dlaczego nie można napisać {@code @Valid(OnCreate.class)}? Czego użyjesz w zamian?
     *   8. Co robi {@code @GroupSequence({Basic.class, Advanced.class})}, gdy grupa Basic znajdzie błąd? Po co to się
     *      stosuje?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielet: ExerciseController, testy: Val03NestedAndGroupsExercisesTest (@Tag("cwiczenie"))
    // =================================================================================================
    //
    // Budujesz walidację przesyłek (/val03/cw/shipments). Mapowania są gotowe; uzupełniasz ShipmentRequest i AddressDto
    // adnotacjami oraz ciała metod. Rozwiązania wzorcowe: Val03SolutionsController
    // (s05_validation.solutions.val03_nested_and_groups).
    //
    // ĆWICZENIE 1 (łatwe): create — recipient niepusty, adres wymagany i sprawdzany w głąb (street i city niepuste). Metoda
    //   odpowiada 201 i odsyła otrzymaną przesyłkę. Podpowiedź: {@code @NotNull @Valid} przy polu address, {@code @Valid} przy
    //   parametrze.
    //
    // ĆWICZENIE 2 (łatwe): parcelCodes — lista niepusta, żaden kod nie może być pusty (ścieżka błędu: parcelCodes[1]).
    //   Podpowiedź: {@code @NotEmpty List<@NotBlank String>}.
    //
    // ĆWICZENIE 3 (średnie): grupy — id przy tworzeniu musi być null (grupa OnCreate), przy zmianie (PUT) wymagane (OnUpdate).
    //   Zmień {@code @Valid} na {@code @Validated(Grupa.class)} i uzupełnij update. Podpowiedź: {@code @Null(groups = …) @NotNull(groups = …)}.
    //
    // ĆWICZENIE 4 (średnie): fieldPaths — posortowana lista unikalnych ścieżek pól z błędami dla wskazanych grup.
    //   Podpowiedź: validator.validate(obiekt, grupy); violation.getPropertyPath().toString(); distinct(), sorted().
    //
    // ĆWICZENIE 5 (trudniejsze): validateInOrder — dodaj do interfejsu Checks adnotację
    //   {@code @GroupSequence({CouponChecks.Basic.class, CouponChecks.Advanced.class})} i zwróć posortowane
    //   "ścieżka:Adnotacja" z walidacji kuponu w tej kolejności (przy błędzie w Basic grupa Advanced nie jest sprawdzana).

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. {@code @NotNull} sprawdza, czy adres w ogóle jest (nie null). {@code @Valid} każe sprawdzić reguły WEWNĄTRZ adresu
     *      (street, city, postalCode). Bez {@code @Valid} wnętrze jest pomijane. Potrzebujesz obu. Test sekcji 1.
     *   2. Zero naruszeń: puste miasto przechodzi niezauważone, bo kaskada nie jest włączona. Sprawdzone w teście
     *      withoutValidNestedObjectIsNotChecked.
     *   3. {@code lines[1].quantity} (indeksy liczone od zera, więc druga pozycja ma indeks 1). Test collectionElementPaths.
     *   4. Nie — walidacja nie zgłosi błędu: {@code @Valid} pomija null, a samo ograniczenie nie zabrania go. Kod, który
     *      potem przechodzi po liście, dostanie NullPointerException. Dodaj {@code @NotNull} na elemencie:
     *      {@code List<@NotNull @Valid OrderLine>}. Test nullElementsInList (z @NotNull błąd pojawia się jako
     *      {@code lines[1].<list element>}).
     *   5. 201 — zwykłe {@code @Validated} to grupa Default, a reguła {@code @Null} należy do OnCreate, więc nie jest
     *      sprawdzana. Sprawdzone w teście validatedWithoutGroupIgnoresCreateRules.
     *   6. Grupa Create nie rozszerza Default, więc po jej wskazaniu reguły bez grupy (czyli Default) są pomijane. Napraw:
     *      {@code interface Create extends Default { }} albo wskaż obie grupy {@code @Validated({Create.class, Default.class})}.
     *      Test groupNotExtendingDefaultSkipsUngroupedRules.
     *   7. {@code @Valid} to adnotacja ze specyfikacji i nie ma atrybutu grup. W Spring MVC używasz
     *      {@code @Validated(OnCreate.class)} (adnotacja Springa).
     *   8. Gdy Basic znajdzie choć jeden błąd, grupa Advanced nie jest w ogóle sprawdzana — zwracane są błędy tylko z
     *      Basic. Po co: żeby nie uruchamiać drogich sprawdzeń (baza, sieć) dla danych oczywiście błędnych i żeby reguły
     *      zaawansowane mogły zakładać, że podstawowe już przeszły. Test sequenceStopsAfterFirstFailingGroup.
     */
    // </editor-fold>
}
