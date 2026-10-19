package s05_validation.val04_custom_validators;

/**
 * <pre>
 * TEMAT: Własne ograniczenia Bean Validation: adnotacja + ConstraintValidator, reguły na poziomie klasy i zależności
 *        (custom = własny, napisany samodzielnie; validator = walidator, klasa sprawdzająca regułę;
 *        constraint = ograniczenie; composed constraint = ograniczenie złożone z innych adnotacji)
 *
 * W SKRÓCIE:
 *   Wbudowane adnotacje ({@code @NotBlank}, {@code @Pattern}...) nie znają reguł Twojego sklepu: format SKU ("ELE-001"),
 *   istnienie kategorii w słowniku, zakres dat od–do. Własne ograniczenie to DWA elementy: adnotacja (nazwa i komunikat,
 *   np. {@code @Sku}) oraz klasa {@code ConstraintValidator} z metodą {@code isValid}, która robi sprawdzenie.
 *   Reguły dotyczące kilku pól naraz piszesz jako adnotację na całej klasie ({@code @ValidDateRange}).
 *
 * ANALOGIA: pieczątka kontrolera jakości.
 *   Adnotacja to kształt pieczątki z napisem „SKU w porządku” (przystawiasz ją do pola). Walidator to kontroler, który
 *   wie, CO znaczy „w porządku”. Ten sam kontroler obsługuje każde miejsce, gdzie przystawiono pieczątkę.
 *   Ograniczenie złożone to pieczątka-zestaw: jedno przyciśnięcie robi trzy ślady z gotowych pieczątek.
 *
 * JAK TO DZIAŁA:
 *   1) adnotacja:   @Constraint(validatedBy = SkuValidator.class) @interface Sku { message(); groups(); payload(); }
 *   2) walidator:   class SkuValidator implements ConstraintValidator{@code <Sku, String>} {
 *                       boolean isValid(String value, ConstraintValidatorContext ctx) { ... }  }
 *   3) użycie:      record NewProduct(@NotNull @Sku String sku, ...)
 *   Walidacja pola z @Sku: Bean Validation tworzy (przez fabrykę) SkuValidator, wywołuje initialize(adnotacja),
 *   potem isValid(wartość, kontekst) — false = naruszenie z komunikatem z adnotacji.
 *
 *   Pod spodem w aplikacji Springa: bean "defaultValidator" (LocalValidatorFactoryBean) używa fabryki
 *   SpringConstraintValidatorFactory — walidatory powstają jako beany Springa, więc mogą mieć wstrzykiwane zależności
 *   (np. CategoryCatalog w KnownCategoryValidator). Czysta fabryka Bean Validation (bez Springa) tego nie potrafi.
 *
 * SŁÓWKA:
 *   custom constraint = własne ograniczenie; validator = walidator; initialize = inicjalizacja (odczyt ustawień adnotacji);
 *   composed constraint = ograniczenie złożone; class-level constraint = ograniczenie na poziomie klasy;
 *   context = kontekst (obiekt do budowania własnych naruszeń); template = szablon komunikatu;
 *   retention = zachowanie adnotacji (RUNTIME — dostępna w działającym programie); target = miejsca, gdzie wolno użyć adnotacji;
 *   injection = wstrzykiwanie (zależności).
 *
 * ZOBACZ TEŻ: s05_validation/Val01BeanValidation (null a ograniczenia), s05_validation/Val03NestedAndGroups (groups, payload),
 *             s05_validation/Val05ServiceValidation (walidacja w serwisie), s01_core_ioc/Ioc02ConstructorInjection
 *             (wstrzykiwanie przez konstruktor), t19_annotations_reflection/Annotations02Custom (pisanie własnych adnotacji),
 *             t19_annotations_reflection/Annotations04Validator (mały walidator oparty na adnotacjach i refleksji).
 * </pre>
 */
public final class Val04CustomValidators {

    private Val04CustomValidators() {
    }

    // =================================================================================================
    // 1. WŁASNA ADNOTACJA @Sku + SkuValidator
    // =================================================================================================
    //
    // Adnotacja (Sku.java) musi mieć:
    //   @Constraint(validatedBy = SkuValidator.class)   — wskazuje klasę walidatora
    //   @Target(...) i @Retention(RUNTIME)               — gdzie wolno jej użyć i że ma być widoczna w działającym programie
    //   TRZY atrybuty: String message(), Class<?>[] groups(), Class<? extends Payload>[] payload()
    // Walidator (SkuValidator.java): implements ConstraintValidator<Sku, String> i metoda isValid.
    //
    // ZASADA: isValid(null) zwracaj true. Dlaczego? Zgodnie z Val01 (sekcja 4) pytanie „czy wartość jest podana” należy do
    //   @NotNull. Gdy walidator formatu odrzuca null, pole OPCJONALNE staje się wymagane, a przy @NotNull dostajesz
    //   podwójny błąd (dwa komunikaty o tym samym). Sprawdzone testem (section1b).
    //
    // PUŁAPKA: pominięcie któregoś z trzech obowiązkowych atrybutów adnotacji (np. groups). Dlaczego to zaskakuje?
    //   Kod się kompiluje, a błąd pojawia się dopiero przy PIERWSZEJ walidacji: ConstraintDefinitionException
    //   z informacją o brakującym atrybucie groups. Sprawdzone testem (section1b).
    //
    // PUŁAPKA: walidator konstruowany jest raz na miejsce użycia i używany z wielu wątków — nie trzymaj w jego
    //   polach wartości zależnych od sprawdzanego obiektu. Dlaczego? Dwa równoległe zapytania nadpisałyby sobie dane
    //   i walidacja jednego klienta zależałaby od drugiego. W polach trzymaj tylko ustawienia z initialize().
    //
    // Test: section1_customAnnotation, section1b_definitionTraps.

    // =================================================================================================
    // 2. OGRANICZENIE ZŁOŻONE (composed): @ProductCode bez własnego walidatora
    // =================================================================================================
    //
    //   @Pattern(regexp = "[A-Z]{3}-[0-9]{3}")
    //   @ReportAsSingleViolation
    //   @Constraint(validatedBy = {})
    //   @interface ProductCode { message(); groups(); payload(); }
    //
    // Gdy wystarczą wbudowane reguły, możesz je „zapakować” pod własną nazwą — bez klasy walidatora. Zyskujesz czytelność
    // (@ProductCode zamiast długiego wzorca powtarzanego w 10 miejscach) i jedno miejsce zmiany.
    //
    // PUŁAPKA: ograniczenie złożone bez @ReportAsSingleViolation. Dlaczego? Naruszenie zgłasza wtedy SKŁADNIK
    //   (@Pattern) z jego komunikatem („musi pasować do wyrażenia…”), a Twój message() jest ignorowany — klient
    //   nie widzi tekstu, który napisałeś. Z @ReportAsSingleViolation jest jedno naruszenie z Twoim komunikatem.
    //   Sprawdzone testem (section2: RawCode kontra ProductCode).
    //
    // Test: section2_composedConstraint.

    // =================================================================================================
    // 3. OGRANICZENIE NA POZIOMIE KLASY: zakres dat (PromotionRequest, FlashSale)
    // =================================================================================================
    //
    //   @ValidDateRange record PromotionRequest(@NotBlank String name, @NotNull LocalDate from, @NotNull LocalDate to)
    //       implements HasDateRange
    //
    // Reguła „od nie później niż do” dotyczy dwóch pól, więc stoi na klasie (@Target(TYPE)); walidator dostaje cały obiekt.
    // Domyślnie błąd miałby pustą ścieżkę (błąd całego obiektu — w Springu to „globalny” ObjectError, nie FieldError).
    // Zwykle chcesz pokazać go przy polu, więc walidator robi:
    //   context.disableDefaultConstraintViolation();
    //   context.buildConstraintViolationWithTemplate(...).addPropertyNode("to").addConstraintViolation();
    // Własny atrybut adnotacji (reportOnField) walidator odczytuje w initialize(adnotacja) — FlashSale ustawia go na false
    // i dostaje błąd o pustej ścieżce, a w kontrolerze błąd globalny (test section3).
    //
    // DOBRA PRAKTYKA: w regule wielopolowej zwracaj true, gdy któreś z pól jest null. Dlaczego? Brak daty zgłosi już
    //   @NotNull na polu; gdyby zakres też krzyczał, klient dostałby dwa błędy o tym samym braku, a w walidatorze
    //   groziłby NullPointerException.
    //
    // Test: section3_classLevel.

    // =================================================================================================
    // 4. WSTRZYKIWANIE BEANA DO WALIDATORA (KnownCategoryValidator + CategoryCatalog)
    // =================================================================================================
    //
    // KnownCategoryValidator ma konstruktor z argumentem CategoryCatalog. W aplikacji Springa walidator tworzy
    // SpringConstraintValidatorFactory (część LocalValidatorFactoryBean, czyli beana walidatora z Boota) i dostarcza zależność —
    // sprawdzone testem z walidatorem Boota (section4), a nie tylko na papierze.
    //
    // PUŁAPKA: ten sam walidator użyty z CZYSTĄ fabryką Bean Validation (Validation.buildDefaultValidatorFactory())
    //   kończy się ValidationException "HV000064: Unable to instantiate ConstraintValidator" — bez Springa nikt nie poda
    //   argumentu konstruktora (fabryka szuka konstruktora bez argumentów). Wstrzykiwanie zależności w walidatorach
    //   działa tylko tam, gdzie walidatorem zarządza Spring.
    //
    // PUŁAPKA: w @WebMvcTest walidator z zależnością pada, bo wycinek webowy nie skanuje @Component.
    //   Dlaczego? Slice (wycinek kontekstu) ładuje tylko warstwę webową; CategoryCatalog trzeba dodać ręcznie
    //   (@Import(CategoryCatalog.class), jak w teście tej lekcji; bez niego test Val04WebMvcWithoutImportTest pokazuje błąd NoSuchBeanDefinitionException) albo użyć @SpringBootTest.
    //
    // DOBRA PRAKTYKA: walidator niech sprawdza reguły SZYBKIE i bez efektów ubocznych (słownik w pamięci, format).
    //   Reguł wymagających bazy („czy SKU jest unikalne?”) nie rób w walidatorze: zapytanie wykona się przy każdej walidacji
    //   (także gdy dane są odrzucane z innych powodów), a i tak nie chroni przed wyścigiem dwóch równoczesnych zapisów —
    //   unikalność gwarantuje ograniczenie UNIQUE w bazie, a serwis zamienia jego naruszenie na czytelny błąd.
    //
    // Test: section4_springBeanInValidator, section5_inController.

    // =================================================================================================
    // 5. BEZPIECZEŃSTWO KOMUNIKATÓW: nie sklejaj danych użytkownika z szablonem
    // =================================================================================================
    //
    // context.buildConstraintViolationWithTemplate("Zła wartość: " + value) — wartość staje się CZĘŚCIĄ SZABLONU, a w szablonie
    // fragmenty {klucz} są interpretowane. Użytkownik, który poda tekst "{jakarta.validation.constraints.NotNull.message}",
    // dostanie w odpowiedzi podstawiony cudzy komunikat (sprawdzone testem section6). Wyrażenia ${...} Hibernate Validator 9
    // w takich szablonach domyślnie nie wykonuje — ale nie polegaj na tym.
    //
    // DOBRA PRAKTYKA: wartość przekaż jako PARAMETR: context.unwrap(HibernateConstraintValidatorContext.class)
    //   .addMessageParameter("value", v) i w szablonie napisz "Zła wartość: {value}". Dlaczego? Parametr wstawiany jest
    //   dosłownie, bez interpretowania zawartych w nim klamer. Szablon ma być zawsze stałym tekstem napisanym przez Ciebie.
    //
    // Test: section6_messageTemplateInjection.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Własne ograniczenie = adnotacja (@Constraint(validatedBy), @Target, @Retention(RUNTIME), message/groups/payload)
     *     + klasa ConstraintValidator<Adnotacja, Typ> z isValid.
     *   • isValid(null) → true; „wymagane” to @NotNull. Walidator bez stanu zależnego od wartości (wiele wątków).
     *   • Ograniczenie złożone: adnotacje-składniki + @Constraint(validatedBy = {}) + @ReportAsSingleViolation (własny komunikat).
     *   • Reguła wielopolowa: adnotacja na klasie; błąd przy polu: disableDefaultConstraintViolation + addPropertyNode.
     *   • Ustawienia adnotacji odczytujesz w initialize(adnotacja).
     *   • Wstrzykiwanie beana do walidatora działa z walidatorem Springa/Boota, nie z czystą fabryką (HV000064).
     *   • @WebMvcTest nie skanuje @Component — dodaj @Import. Unikalność sprawdza baza (UNIQUE), nie walidator.
     *   • Dane użytkownika do komunikatu: addMessageParameter + {nazwa}, nigdy sklejanie z szablonem.
     *
     * PYTANIA KONTROLNE:
     *   1. Z jakich dwóch elementów składa się własne ograniczenie i jakie trzy atrybuty musi mieć adnotacja?
     *   2. Co się stanie? Adnotacja ograniczenia nie ma atrybutu groups(), a Ty walidujesz obiekt z takim polem.
     *      Czy błąd zobaczysz przy kompilacji, przy starcie aplikacji czy przy walidacji?
     *   3. ZNAJDŹ BŁĄD: isValid walidatora @Sku zaczyna się od return value.matches("[A-Z]{3-[0-9]{3}");}
     *      i pole opcjonalne bywa puste. Co pójdzie źle i jak naprawić?
     *   4. Co się stanie? Ograniczenie złożone @ProductCode nie ma @ReportAsSingleViolation, a wartość jest zła.
     *      Z którym komunikatem i jaką adnotacją zgłosi się naruszenie?
     *   5. Walidator KnownCategoryValidator (z konstruktorem CategoryCatalog) uruchamiasz przez
     *      Validation.buildDefaultValidatorFactory(). Co się stanie i dlaczego?
     *   6. Jak sprawić, aby błąd reguły „od–do” pojawił się przy polu to, a nie jako błąd całego obiektu?
     *   7. ZNAJDŹ BŁĄD: walidator buduje komunikat "Zła wartość: " + value. Co może zrobić złośliwy użytkownik
     *      i jak to naprawić?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Val04CustomValidatorsExercisesTest
    // =================================================================================================

    // ĆWICZENIE 1 (łatwe): otwórz PhoneValidator i zaimplementuj isValid dla adnotacji @Phone: null → true; poprawny numer
    // to 9 cyfr, opcjonalnie z prefiksem "+48" i dowolnymi spacjami ("600100200", "600 100 200", "+48 600 100 200");
    // wszystko inne (np. "12345", "abc", "+49 600 100 200", "") jest błędne.
    // Podpowiedź: usuń spacje (replace), odetnij prefiks (startsWith/substring), sprawdź matches("[0-9]{9}").
    //
    // ĆWICZENIE 2 (średnie): otwórz PriceRangeValidator (reguła klasy @ValidPriceRange dla PriceFilter): gdy obiekt albo
    // któraś z cen jest null — true; gdy minPrice <= maxPrice — true; w przeciwnym razie false z błędem PRZYPIĘTYM do pola
    // "maxPrice". Podpowiedź: porównuj BigDecimal przez compareTo (nie equals — 10.0 i 10.00 to ten sam zakres),
    // a pole wskaż jak w DateRangeValidator.
    //
    // ĆWICZENIE 3 (trudniejsze): otwórz adnotację Quantity i złóż ją z @Min(1) i @Max(99) tak, żeby zły numer dawał
    // JEDNO naruszenie z komunikatem "Ilość od 1 do 99". Podpowiedź: sekcja 2 — trzy adnotacje nad definicją,
    // w tym @ReportAsSingleViolation.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Adnotacja i klasa walidatora (ConstraintValidator). Adnotacja musi mieć atrybuty message, groups i payload
     *      (oraz @Constraint, @Target, @Retention). Sprawdzone testem (section1, section1b).
     *   2. Przy walidacji — wyjątek ConstraintDefinitionException (w komunikacie jest nazwa brakującego atrybutu groups).
     *      Kod kompiluje się i aplikacja startuje. Sprawdzone testem (section1b).
     *   3. Dla null poleci NullPointerException (albo — gdy zwracasz false dla null — pole opcjonalne stanie się wymagane).
     *      Naprawa: na początku if (value == null) return true;, wymagalność zostaw @NotNull.
     *      Sprawdzone testem (section1b: walidator odrzucający null robi z pola pole wymagane).
     *   4. Z komunikatem i adnotacją SKŁADNIKA (@Pattern), a nie Twoim message(). Naprawa: @ReportAsSingleViolation.
     *      Sprawdzone testem (section2).
     *   5. ValidationException "HV000064: Unable to instantiate ConstraintValidator ..." — czysta fabryka szuka
     *      konstruktora bez argumentów i nie ma skąd wziąć CategoryCatalog. Z walidatorem Springa/Boota działa.
     *      Sprawdzone testem (section4).
     *   6. Wyłączyć domyślne naruszenie i dodać nowe z węzłem właściwości: disableDefaultConstraintViolation(),
     *      buildConstraintViolationWithTemplate(...).addPropertyNode("to").addConstraintViolation(). Sprawdzone testem (section3).
     *   7. Podać wartość z kluczem w klamrach (np. "{jakarta.validation.constraints.NotNull.message}") — zostanie podmieniona
     *      na cudzy komunikat, bo szablon jest interpretowany. Naprawa: addMessageParameter("value", value) i szablon
     *      "Zła wartość: {value}". Sprawdzone testem (section6).
     */
    // </editor-fold>
}
