package s05_validation.val03_nested_and_groups;

import jakarta.validation.Validator;

import java.util.List;

/**
 * <pre>
 * TEMAT: Walidacja zagnieżdżona, elementy kolekcji i grupy walidacji
 *        (nested = zagnieżdżony; cascade = kaskada, czyli zejście walidacji do obiektu w środku; container element =
 *         element kontenera, np. elementu listy; group = grupa reguł; default group = grupa domyślna)
 *
 * W SKRÓCIE:
 *   Walidator sprawdza adnotacje obiektu, który dostał — ale NIE zagląda sam do obiektów w jego polach. Żeby sprawdzić
 *   adres wewnątrz zamówienia albo każdą pozycję na liście, oznaczasz pole adnotacją @Valid (kaskada). Reguły dla
 *   samych elementów kolekcji (np. „każdy tag niepusty”) zapisujesz przy typie elementu: List z NotBlank String w nawiasach
 *   ostrych. Wreszcie GRUPY: ten sam rekord ma inne wymagania przy tworzeniu (id puste) i aktualizacji (id obowiązkowe) —
 *   każdą regułę przypisujesz do grupy i wybierasz grupę przy walidacji.
 *
 * ANALOGIA: kontrola jakości paczki z paczkami w środku.
 *   Kontroler sprawdza zewnętrzny karton (zamówienie). Jeśli w środku są mniejsze pudełka (adres, pozycje), musisz mu
 *   napisać na kartonie „zajrzyj do środka” (@Valid) — inaczej zobaczy tylko karton. Grupy to różne listy kontrolne:
 *   „przyjęcie towaru” i „wydanie towaru” sprawdzają to samo pudełko, ale pod innym kątem.
 *
 * JAK TO DZIAŁA:
 *   validator.validate(order)
 *     1. sprawdza reguły pól zamówienia (customer, address, items) w grupie Default
 *     2. pole z @Valid → wchodzi do obiektu i sprawdza jego reguły; ścieżka: "address.postalCode"
 *     3. lista z elementami @Valid → wchodzi do każdego elementu; ścieżka: "items[1].quantity"
 *     4. element listy z własną regułą (@NotBlank na String) → ścieżka: "tags[1].<list element>"
 *   validator.validate(product, OnCreate.class)  → sprawdza TYLKO reguły przypisane do OnCreate
 *   Spring: @Valid w kontrolerze = grupa Default; @Validated(OnCreate.class) = wskazana grupa.
 *
 * SŁÓWKA:
 *   cascade = kaskada; nested object = obiekt zagnieżdżony; list element = element listy; type use = użycie przy typie
 *   (adnotacja zapisana w nawiasach ostrych typu); group = grupa; default = domyślna; marker interface = interfejs-znacznik
 *   (pusty, służy tylko jako nazwa); group sequence = sekwencja grup; on create = przy tworzeniu; on update = przy aktualizacji.
 *
 * ZOBACZ TEŻ: s05_validation/Val01BeanValidation (podstawowe adnotacje), s05_validation/Val02ValidInControllers
 *             (@Valid w kontrolerach), s05_validation/Val04CustomValidators, s04_web_rest/Rest05CrudApi (tworzenie
 *             i aktualizacja zasobu — tam grupy się przydają).
 * </pre>
 */
public final class Val03NestedAndGroups {

    private Val03NestedAndGroups() {
    }

    // =================================================================================================
    // 1. Obiekt zagnieżdżony — @Valid uruchamia kaskadę
    // =================================================================================================
    //
    //   record OrderRequest(@NotBlank String customer, @NotNull @Valid Address address, ...)
    //   record Address(@NotBlank String street, @Pattern(regexp = "\\d{2}-\\d{3}") String postalCode)
    //
    // Naruszenia wewnątrz adresu mają ścieżkę "address.street", "address.postalCode" — dzięki temu klient wie, GDZIE
    // jest błąd. @Valid sprawdza adres tylko gdy NIE jest null; jeśli adres ma być obowiązkowy, dodaj @NotNull obok.
    //
    // PUŁAPKA: pole z adresem bez @Valid (Val03Types.LooseOrder). Dlaczego to groźne? Reguły w Address nie robią NIC,
    //   walidacja nie schodzi do środka i nie ma żadnego ostrzeżenia — zamówienie z pustą ulicą i kodem „bad” przechodzi.
    //   Sprawdzone testem Val03NestedAndGroupsTest.nestedObjects.
    //
    // DOBRA PRAKTYKA: przy każdym polu typu „obiekt z regułami” zadaj sobie pytanie: czy mam tu @Valid? Dlaczego?
    //   Zapomniane @Valid jest cichym błędem, a test z błędnym zagnieżdżonym polem (jak w tej lekcji) go ujawnia.

    // =================================================================================================
    // 2. Elementy listy — List z @Valid przy typie elementu
    // =================================================================================================
    //
    //   @NotEmpty List<@NotNull @Valid OrderItem> items
    //
    // Zapis w nawiasach ostrych to adnotacja „przy typie” (type use, od Javy 8 / Bean Validation 2.0): dotyczy
    // ELEMENTÓW listy. Starszy zapis {@code @Valid List<OrderItem> items} też kaskaduje na elementy (Val03Types.OldStyleOrder)
    // — oba działają, ale zapis z nawiasami pozwala dodać reguły per element (np. @NotNull).
    // Ścieżka błędu zawiera indeks: "items[1].quantity".
    //
    // PUŁAPKA: element null w liście. {@code List<@Valid OrderItem>} przepuszcza null po cichu, bo @Valid pomija null.
    //   Dlaczego to ważne? JSON {@code "items":[{...}, null]} przejdzie walidację, a pierwsza próba użycia pozycji da
    //   NullPointerException. Rozwiązanie: @NotNull przy typie elementu (jak w OrderRequest).
    //   Sprawdzone testem Val03NestedAndGroupsTest.listElements.

    // =================================================================================================
    // 3. Reguły dla elementów kontenera — List<@NotBlank String>
    // =================================================================================================
    //
    //   @NotEmpty List<@NotBlank @Size(max = 10) String> tags
    //
    // @NotEmpty przy samym polu mówi o LIŚCIE (nie pusta), a adnotacje w nawiasach — o każdym TEKŚCIE na niej.
    // Ścieżka błędu elementu to "tags[1].<list element>" (<list element> to nazwa „węzła” elementu listy; nie ma pola,
    // bo String nie ma właściwości). To działa też dla Map (osobno reguły dla kluczy i wartości) i Optional.
    //
    // PUŁAPKA: {@code @NotBlank List<String> tags} — adnotacja przy polu, nie przy elemencie. Dlaczego to błąd? Dla listy
    //   @NotBlank nie ma walidatora (UnexpectedTypeException), a Ty chciałeś sprawdzać teksty w środku. Adnotacje
    //   elementu idą do nawiasów ostrych. Sprawdzone testem Val03NestedAndGroupsTest.containerElements.

    // =================================================================================================
    // 4. Grupy walidacji — różne reguły dla różnych operacji
    // =================================================================================================
    //
    //   interface OnCreate {}   interface OnUpdate {}           (puste interfejsy-znaczniki)
    //   record ProductDto(
    //       @Null(groups = OnCreate.class) @NotNull(groups = OnUpdate.class) Long id,
    //       @NotBlank(groups = {OnCreate.class, OnUpdate.class}) String name)
    //
    //   validator.validate(dto, OnCreate.class)  // id musi być puste, nazwa obowiązkowa
    //   validator.validate(dto, OnUpdate.class)  // id obowiązkowe, nazwa obowiązkowa
    //   validator.validate(dto)                  // grupa Default: reguły z groups = ... NIE są sprawdzane
    //
    // Reguła bez groups należy do grupy Default (domyślnej). Reguły z groups = X należą TYLKO do X.
    //
    // PUŁAPKA: grupa Default. Walidujesz grupą OnCreate rekord, w którym część reguł nie ma grupy (Val03Types.MixedDto).
    //   Dlaczego te reguły milczą? Grupa OnCreate NIE obejmuje grupy Default — sprawdzane są wyłącznie reguły z OnCreate.
    //   Reguła @NotBlank bez grupy zostaje pominięta i pusta nazwa przechodzi. Naprawa: podaj obie grupy
    //   (Default.class i OnCreate.class) albo zadeklaruj {@code interface OnCreate extends Default}.
    //   Sprawdzone testem Val03NestedAndGroupsTest.groupsAndDefault.
    //
    // DOBRA PRAKTYKA: grupy stosuj oszczędnie — jeśli różnica między tworzeniem a aktualizacją to jedno pole (id),
    //   często prościej mieć dwa osobne rekordy (CreateProductRequest, UpdateProductRequest). Dlaczego? Każda grupa to
    //   dodatkowe miejsce na pomyłkę (zapomniana grupa = reguła, która nie działa), a dwa typy mówią wprost,
    //   co wolno przy której operacji.

    // =================================================================================================
    // 5. Grupy w kontrolerze — @Validated(OnCreate.class) zamiast @Valid
    // =================================================================================================
    //
    //   @PostMapping("/products")  public ... create(@Validated(OnCreate.class) @RequestBody ProductDto product)
    //   @PutMapping("/products")   public ... update(@Validated(OnUpdate.class) @RequestBody ProductDto product)
    //
    // @Valid (z Jakarty) nie umie podać grupy — zawsze Default. @Validated (ze Springa) przyjmuje grupy.
    //
    // PYTANIE REKRUTACYJNE: czym różni się @Valid od @Validated? @Valid jest z jakarta.validation: włącza walidację
    //   argumentu i kaskadę na zagnieżdżone obiekty, ale bez grup. @Validated jest ze Springa (org.springframework.
    //   validation.annotation): przyjmuje grupy; na klasie włącza walidację metod serwisu przez proxy (Val05ServiceValidation);
    //   NIE służy do kaskady na pola.
    //
    // PUŁAPKA: w kontrolerze dajesz zwykłe @Valid, a reguły ProductDto są przypisane do OnCreate/OnUpdate. Dlaczego nic
    //   się nie dzieje? @Valid sprawdza grupę Default, w której nie ma żadnej reguły — każdy JSON przechodzi.
    //   Sprawdzone testem Val03NestedAndGroupsWebMvcTest.groupsInController.
    //
    // Testy: Val03NestedAndGroupsWebMvcTest.nestedInController i Val03NestedAndGroupsWebMvcTest.groupsInController.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Walidator nie schodzi sam do pól-obiektów: potrzebne @Valid (kaskada); obowiązkowy obiekt = @NotNull + @Valid.
     *   • Ścieżki: "address.street", "items[1].quantity", "tags[1].<list element>".
     *   • Lista: List z @NotNull @Valid OrderItem w nawiasach — @NotNull łapie element null, który @Valid pomija.
     *   • Reguły dla elementów kontenera idą w nawiasy ostre; adnotacja przy polu dotyczy kontenera.
     *   • Grupa = interfejs-znacznik; groups = X.class przy regule; validate(obiekt, X.class) sprawdza tylko X.
     *   • Reguła bez groups jest w grupie Default; zwykła grupa jej NIE obejmuje (podaj Default.class lub extends Default).
     *   • Kontroler: @Valid = Default; @Validated(Grupa.class) = wybrana grupa.
     *   • Wiele grup ma swój koszt — dwa osobne rekordy bywają prostsze.
     *
     * PYTANIA KONTROLNE:
     *   1. PYTANIE REKRUTACYJNE: czym różni się @Valid od @Validated?
     *   2. Co się stanie? Rekord LooseOrder ma pole {@code Address address} bez @Valid, a adres ma pustą ulicę i kod „bad”.
     *   3. Co się stanie? Lista pozycji to {@code List<@Valid OrderItem>} i jedną z pozycji jest null.
     *   4. Jaką ścieżkę ma błąd ilości w DRUGIEJ pozycji zamówienia? A błąd pustego drugiego taga na liście tagów?
     *   5. Co się stanie? Walidujesz grupą OnCreate rekord MixedDto z id = 5 i pustą nazwą, a nazwa ma @NotBlank bez grupy.
     *   6. ZNAJDŹ BŁĄD: kontroler ma {@code @Valid @RequestBody ProductDto}, a w ProductDto wszystkie reguły mają
     *      groups = OnCreate.class lub OnUpdate.class. Dlaczego żadne dane nie są odrzucane i jak to naprawić?
     *   7. Jak sprawdzić jednocześnie reguły bez grupy i reguły grupy OnCreate?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Val03NestedAndGroupsExercisesTest
    // =================================================================================================

    /** Adres do ćwiczenia 1 (reguły gotowe). */
    public record Exercise1Address(@jakarta.validation.constraints.NotBlank String city) {
    }

    /**
     * ĆWICZENIE 1 (łatwe): przesyłka ma mieć niepusty odbiorcę (receiver) i OBOWIĄZKOWY adres, którego reguły
     * (miasto niepuste) mają działać. Dopisz adnotacje przy komponentach.
     * Podpowiedź: dla adresu potrzebne są DWIE adnotacje — jedna o obowiązkowości, druga o zejściu do środka.
     */
    public record Exercise1Shipment(
            // TODO: dopisz adnotacje (receiver, address)
            String receiver,
            Exercise1Address address) {
    }

    /** Pozycja do ćwiczenia 2 (reguły gotowe). */
    public record Exercise2Line(
            @jakarta.validation.constraints.NotBlank String sku,
            @jakarta.validation.constraints.Positive int qty) {
    }

    /**
     * ĆWICZENIE 2 (średnie): koszyk ma mieć co najmniej jedną pozycję, każda pozycja ma być niepustym obiektem
     * ze sprawdzonymi regułami, a każdy kod rabatowy na liście coupons — niepusty tekst (sama lista kuponów może być null).
     * Podpowiedź: adnotacje elementów idą w nawiasy ostre przy typie; pamiętaj o elemencie null.
     */
    public record Exercise2Basket(
            // TODO: dopisz adnotacje (lines, coupons)
            List<Exercise2Line> lines,
            List<String> coupons) {
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): klient ma e-mail niepusty w obu operacjach; id ma być PUSTE przy tworzeniu
     * (Val03Types.OnCreate) i OBOWIĄZKOWE przy aktualizacji (Val03Types.OnUpdate). Dopisz adnotacje z grupami,
     * a potem zaimplementuj exercise3: wybierz grupę według parametru {@code creating} i zwróć POSORTOWANE nazwy pól
     * z naruszeniami.
     * Podpowiedź: {@code @Null(groups = ...)}, {@code @NotNull(groups = ...)}, {@code validator.validate(obiekt, grupa)}.
     */
    public record Exercise3Customer(
            // TODO: dopisz adnotacje z grupami (id, email)
            Long id,
            String email) {
    }

    /** Sprawdza klienta w grupie odpowiedniej do operacji i zwraca posortowane nazwy pól z błędami. */
    public static List<String> exercise3(Validator validator, Exercise3Customer customer, boolean creating) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. @Valid (jakarta.validation) włącza walidację argumentu i kaskadę na zagnieżdżone obiekty/elementy, ale nie
     *      przyjmuje grup. @Validated (Spring) przyjmuje grupy i na klasie włącza walidację metod przez proxy; nie
     *      służy do kaskady na pola.
     *   2. Brak naruszeń — walidator nie wchodzi do adresu bez @Valid; reguły Address są martwe.
     *      Sprawdzone testem Val03NestedAndGroupsTest.nestedObjects.
     *   3. Brak naruszeń — @Valid pomija null, więc element null przechodzi po cichu (dopiero @NotNull go łapie).
     *      Sprawdzone testem Val03NestedAndGroupsTest.listElements.
     *   4. "items[1].quantity" (numeracja od 0, więc druga pozycja to indeks 1) oraz "tags[1].<list element>".
     *      Sprawdzone testami Val03NestedAndGroupsTest.nestedObjects i Val03NestedAndGroupsTest.containerElements.
     *   5. Naruszenie tylko na id (@Null w grupie OnCreate); pusta nazwa PRZECHODZI, bo reguła bez grupy jest w Default,
     *      a OnCreate nie obejmuje Default. Sprawdzone testem Val03NestedAndGroupsTest.groupsAndDefault.
     *   6. @Valid uruchamia tylko grupę Default, w której nie ma żadnej reguły. Naprawa: @Validated(OnCreate.class)
     *      (lub OnUpdate.class) w kontrolerze. Sprawdzone testem Val03NestedAndGroupsWebMvcTest.groupsInController.
     *   7. validator.validate(obiekt, Default.class, OnCreate.class); w kontrolerze @Validated({Default.class,
     *      OnCreate.class}). Sprawdzone testami Val03NestedAndGroupsTest.groupsAndDefault
     *      i Val03NestedAndGroupsWebMvcTest.groupsInController.
     */
    // </editor-fold>
}
