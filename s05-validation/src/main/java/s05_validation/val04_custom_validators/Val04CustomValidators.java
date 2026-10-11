package s05_validation.val04_custom_validators;

/**
 * <pre>
 * TEMAT: Własne ograniczenia i walidatory — adnotacja + ConstraintValidator
 *        (custom = własny; constraint = ograniczenie; constraint validator = walidator ograniczenia;
 *         cross-field = między polami; class-level = na poziomie klasy)
 *
 * W SKRÓCIE:
 *   Gotowe adnotacje (@NotBlank, @Size...) nie znają reguł Twojego sklepu: „kod produktu to ABC-1234”, „data końca nie
 *   przed początkiem”, „kategoria istnieje w katalogu”. Własne ograniczenie to DWA elementy: adnotacja (nazwa reguły +
 *   komunikat) i klasa ConstraintValidator (sama kontrola). Adnotację łączy z walidatorem @Constraint(validatedBy = ...).
 *   Reguły obejmujące kilka pól naraz zapisujesz adnotacją na klasie. A w Springu walidator może dostać beany
 *   (np. repozytorium), bo to Spring tworzy jego instancje.
 *
 * ANALOGIA: przepis i kucharz.
 *   Adnotacja to kartka z przepisem na ladzie („kod: trzy litery, myślnik, cyfry”) — każdy ją widzi. Walidator to
 *   kucharz, który potrafi według niej sprawdzić towar. Reguła klasowa to kontrola zestawu („czy do kanapki pasuje
 *   i chleb, i masło?”), a walidator z beanem — kucharz, który może zadzwonić do magazynu (katalog kategorii).
 *
 * JAK TO DZIAŁA:
 *   @ProductCode(digits = 4) String code
 *     1. Validator widzi adnotację z @Constraint(validatedBy = ProductCodeValidator.class)
 *     2. tworzy walidator (raz), woła initialize(adnotacja) — odczyt atrybutów
 *     3. dla każdej wartości woła isValid(wartość, kontekst): true = poprawna, false = naruszenie
 *     4. komunikat bierze z message() adnotacji (z podstawieniem {digits})
 *   Walidator klasowy dostaje cały obiekt, a błąd przypina do pola przez kontekst (buildConstraintViolationWithTemplate).
 *   Instancje walidatorów tworzy ConstraintValidatorFactory: w czystym Hibernate Validatorze przez konstruktor
 *   bezparametrowy, w Springie (LocalValidatorFactoryBean) przez kontener — stąd wstrzykiwanie beanów.
 *
 * SŁÓWKA:
 *   annotation = adnotacja; message = komunikat; groups = grupy; payload = ładunek (dodatkowe dane); retention =
 *   czas życia adnotacji; target = cel (gdzie wolno użyć); context = kontekst walidacji; property node = węzeł
 *   właściwości (część ścieżki błędu); stateless = bezstanowy; thread-safe = bezpieczny wątkowo.
 *
 * ZOBACZ TEŻ: t19_annotations_reflection/Annotations02Custom (własne adnotacje w czystej Javie),
 *             t19_annotations_reflection/Annotations04Validator (mini-walidator na refleksji),
 *             s05_validation/Val01BeanValidation, s05_validation/Val03NestedAndGroups, s05_validation/Val05ServiceValidation.
 * </pre>
 */
public final class Val04CustomValidators {

    private Val04CustomValidators() {
    }

    // =================================================================================================
    // 1. Własna adnotacja + walidator: @ProductCode
    // =================================================================================================
    //
    // Pliki: ProductCode.java (adnotacja) i ProductCodeValidator.java (kontrola). Adnotacja MUSI mieć trzy atrybuty
    // wymagane przez specyfikację: message(), groups(), payload(); oprócz nich dodajesz własne (tu: digits).
    // Ważne meta-adnotacje:
    //   @Constraint(validatedBy = ...)  łączy adnotację z klasą walidatora
    //   @Retention(RUNTIME)             adnotacja musi żyć w czasie działania — inaczej Validator jej nie widzi
    //   @Target(...)                    gdzie wolno jej użyć (pole, parametr, TYPE_USE dla elementów list)
    //
    // PUŁAPKA: zapominasz @Retention(RUNTIME). Dlaczego to podstępne? Domyślnie adnotacja jest zachowana tylko w
    //   pliku .class (CLASS) i nie dociera do refleksji; kod się kompiluje, a reguła po prostu... nie działa, bez błędu.
    //
    // Testy: Val04CustomValidatorsTest.productCodeConstraint.

    // =================================================================================================
    // 2. Konwencja dla null: null jest poprawny
    // =================================================================================================
    //
    // Wszystkie standardowe adnotacje (oprócz @NotNull/@NotEmpty/@NotBlank) uznają null za poprawny (zob. Val01BeanValidation).
    // Własne ograniczenia powinny robić tak samo: if (value == null) return true;
    //
    // DOBRA PRAKTYKA: pole obowiązkowe = @NotNull + własna adnotacja; pole opcjonalne = sama własna adnotacja.
    //   Dlaczego? Każda reguła robi jedną rzecz, a Ty składasz je jak klocki. Gdyby @ProductCode odrzucało null, nie
    //   dałoby się go użyć na polu opcjonalnym.
    //
    // PUŁAPKA: walidator, który zwraca false dla null (lub rzuca NullPointerException, bo woła value.length()). Dlaczego
    //   to problem? Opcjonalne pole nagle staje się obowiązkowe albo każde żądanie bez pola kończy się błędem 500.
    //
    // Testy: Val04CustomValidatorsTest.nullConvention.

    // =================================================================================================
    // 3. Reguła między polami — adnotacja na klasie (@ValidPromotion)
    // =================================================================================================
    //
    // Reguły „pole A względem pola B” nie da się zapisać adnotacją przy jednym polu. Adnotacja z @Target(TYPE) stoi nad
    // rekordem i walidator dostaje CAŁY obiekt (ValidPromotionValidator): data początku nie po dacie końca, zniżka nie
    // większa od ceny.
    //
    // Błąd domyślnie dotyczy całej klasy (ścieżka pusta), więc klient nie wie, które pole poprawić. Dlatego w walidatorze:
    //   context.disableDefaultConstraintViolation();     // wyłącz komunikat „na całość”
    //   context.buildConstraintViolationWithTemplate("...").addPropertyNode("validTo").addConstraintViolation();
    //
    // PUŁAPKA: budujesz własne naruszenie, ale nie wywołujesz addConstraintViolation() albo nie wyłączasz domyślnego.
    //   Dlaczego to ważne? Bez addConstraintViolation() budowniczy nic nie rejestruje; bez disableDefaultConstraintViolation()
    //   dostajesz DWA naruszenia (domyślne na całości i własne na polu) — klient widzi dwa komunikaty o jednym błędzie.
    //   Sprawdzone testem Val04CustomValidatorsTest.classLevelConstraint.
    //
    // Testy: Val04CustomValidatorsTest.classLevelConstraint.

    // =================================================================================================
    // 4. Walidator z beanami Springa — @ExistingCategory
    // =================================================================================================
    //
    // ExistingCategoryValidator ma konstruktor z parametrem CategoryCatalog (bean). W aplikacji Spring Boot Validator to
    // LocalValidatorFactoryBean, która tworzy walidatory przez kontener Springa (SpringConstraintValidatorFactory),
    // więc wstrzykuje zależności. Zwykły Validator z Validation.buildDefaultValidatorFactory() tego nie umie — Hibernate
    // Validator chce konstruktora bezparametrowego i zgłasza błąd przy pierwszym użyciu reguły.
    //
    // W teście warstwy web (@WebMvcTest) beany @Component nie są wczytywane, więc brakujący CategoryCatalog podajemy
    // jako atrapę @MockitoBean — inaczej kontekst walidatora nie zbuduje się.
    //
    // PUŁAPKA: w walidatorze łączysz się z bazą na każde pole. Dlaczego to złe? Walidacja listy 1000 elementów to 1000
    //   zapytań. Walidator ma robić tanie, szybkie kontrole; unikalność i istnienie w bazie często lepiej sprawdzić
    //   w serwisie, razem z zapisem (tylko tam wynik jest pewny — między sprawdzeniem a zapisem ktoś mógł wstawić rekord).
    //
    // DOBRA PRAKTYKA: walidator ma być bezstanowy (poza polami ustawionymi w initialize). Dlaczego? Jedna instancja jest
    //   współdzielona przez wiele wątków; zmienne pole z wynikami poprzednich sprawdzeń dałoby wyścigi.
    //
    // Testy: Val04CustomValidatorsSpringTest.springBeanInValidator, Val04CustomValidatorsTest.plainFactoryCannotInject,
    //        Val04CustomValidatorsWebMvcTest.customConstraintsInController.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Własne ograniczenie = adnotacja (message, groups, payload + własne atrybuty, @Constraint, @Retention(RUNTIME),
     *     @Target) + klasa implements ConstraintValidator<Adnotacja, Typ> z isValid.
     *   • initialize(adnotacja) czyta atrybuty; isValid(wartość, kontekst) zwraca true/false.
     *   • null → true (obowiązkowość daje @NotNull) — jak adnotacje standardowe.
     *   • Reguła między polami: @Target(TYPE), walidator dostaje cały obiekt; błąd przypinaj do pola przez
     *     disableDefaultConstraintViolation + buildConstraintViolationWithTemplate(...).addPropertyNode(...).addConstraintViolation().
     *   • W Springu walidator może mieć wstrzyknięte beany (LocalValidatorFactoryBean); w czystym Hibernate Validatorze — nie.
     *   • Walidator bezstanowy i szybki; kontrole wymagające bazy rób w serwisie.
     *
     * PYTANIA KONTROLNE:
     *   1. Z jakich dwóch elementów składa się własne ograniczenie i co je łączy?
     *   2. Co się stanie? W adnotacji brakuje @Retention(RUNTIME).
     *   3. Co się stanie? Pole opcjonalne {@code @ProductCode String code} ma wartość null. A pole z {@code @NotNull @ProductCode}?
     *   4. ZNAJDŹ BŁĄD: walidator klasowy wywołuje {@code context.buildConstraintViolationWithTemplate("Błąd").addPropertyNode("validTo")},
     *      ale kończy bez addConstraintViolation(). Co zobaczy klient?
     *   5. Co się stanie? Walidator klasowy nie wywołuje disableDefaultConstraintViolation(), a zwraca false i dodaje własne
     *      naruszenie na polu.
     *   6. Co się stanie? ExistingCategoryValidator (z konstruktorem przyjmującym CategoryCatalog) użyty z Validatorem
     *      z Validation.buildDefaultValidatorFactory().
     *   7. PYTANIE REKRUTACYJNE: dlaczego walidator ograniczenia powinien być bezstanowy (thread-safe)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Val04CustomValidatorsExercisesTest
    // =================================================================================================

    /** Rekord do ćwiczenia 1. */
    public record Exercise1Address(@PostalCode String postalCode) {
    }

    /**
     * ĆWICZENIE 1 (łatwe): zaimplementuj PostalCodeValidator.isValid: poprawny jest kod w postaci "00-000" (dwie cyfry,
     * myślnik, trzy cyfry); null jest poprawny (konwencja z sekcji 2). Podpowiedź: wyrażenie regularne {@code \\d{2}-\\d{3}}
     * i {@code String.matches}.
     */

    /** Rekord do ćwiczenia 2: komentarz o co najwyżej trzech słowach. */
    public record Exercise2Comment(@MaxWords(3) String text) {
    }

    /**
     * ĆWICZENIE 2 (średnie): zaimplementuj MaxWordsValidator: w {@code initialize} zapamiętaj {@code annotation.value()},
     * w {@code isValid} policz słowa (rozdzielone dowolną liczbą białych znaków) i porównaj z limitem. null i tekst
     * z samych spacji mają 0 słów i są poprawne. Podpowiedź: {@code value.trim()}, {@code split("\\s+")}; pusty tekst
     * po trim() trzeba obsłużyć osobno.
     */

    /** Rekord do ćwiczenia 3: rejestracja z potwierdzeniem hasła; reguła klasowa @PasswordsMatch. */
    @PasswordsMatch
    public record Exercise3Registration(String password, String confirmPassword) {
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zaimplementuj PasswordsMatchValidator: hasła muszą być równe; gdy nie są, naruszenie ma
     * mieć komunikat "Hasła muszą być takie same" i ścieżkę {@code confirmPassword} (jedno naruszenie, nie dwa).
     * Gdy któreś pole jest null — traktuj rejestrację jako poprawną (od obowiązkowości są inne adnotacje).
     * Podpowiedź: sekcja 3 (disableDefaultConstraintViolation, addPropertyNode, addConstraintViolation) i {@code Objects.equals}.
     */

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Adnotacja (nazwa reguły, komunikat, atrybuty) i klasa implementująca ConstraintValidator (kontrola). Łączy je
     *      @Constraint(validatedBy = KlasaWalidatora.class) na adnotacji.
     *   2. Adnotacja jest widoczna dla kompilatora, ale nie w czasie działania: Validator jej nie znajdzie i reguła
     *      po cichu nie działa (brak błędu). Mechanizm Javy; w testach lekcji adnotacje mają RUNTIME.
     *   3. Pole z samym @ProductCode: brak naruszenia. Pole z @NotNull @ProductCode: naruszenie z NotNull.
     *      Sprawdzone testem Val04CustomValidatorsTest.nullConvention.
     *   4. Nic — bez addConstraintViolation() budowniczy niczego nie rejestruje; po wyłączeniu domyślnego komunikatu
     *      i zwróceniu false może wręcz nie powstać żadne naruszenie, a błąd zniknie. Zawsze kończ addConstraintViolation().
     *      Mechanizm opisany w dokumentacji ConstraintValidatorContext; w lekcji pokazany poprawny wariant
     *      (Val04CustomValidatorsTest.classLevelConstraint).
     *   5. Powstają dwa naruszenia: domyślne (ścieżka pusta, cały obiekt) i własne na polu. Sprawdzone testem
     *      Val04CustomValidatorsTest.classLevelConstraint.
     *   6. Walidator nie zostanie utworzony: Hibernate Validator wymaga konstruktora bezparametrowego, więc
     *      validate(...) rzuca ValidationException. Sprawdzone testem Val04CustomValidatorsTest.plainFactoryCannotInject.
     *   7. Jedna instancja walidatora obsługuje wiele wątków jednocześnie, więc zmienny stan (np. liczniki, ostatnia
     *      wartość) powodowałby wyścigi. Bezpieczne jest tylko to, co ustawiono w initialize.
     */
    // </editor-fold>
}
