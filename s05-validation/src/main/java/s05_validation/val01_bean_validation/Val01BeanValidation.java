package s05_validation.val01_bean_validation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * <pre>
 * TEMAT: Bean Validation — adnotacje sprawdzające dane i programowy Validator
 *        (bean validation = walidacja obiektów; validation = sprawdzanie poprawności; constraint = ograniczenie,
 *         czyli warunek, który dane muszą spełnić; validator = obiekt, który sprawdza ograniczenia)
 *
 * W SKRÓCIE:
 *   Zamiast pisać w każdej metodzie ciąg if-ów (if (name == null || name.isBlank()) ...), opisujesz reguły ADNOTACJAMI
 *   przy polach: @NotBlank, @Size, @Positive... Samą kontrolę robi biblioteka. Specyfikacja nazywa się Jakarta Bean
 *   Validation (pakiet jakarta.validation), a jej implementacja to Hibernate Validator (mimo nazwy nie ma nic wspólnego
 *   z bazą danych!). Boot dokłada ją starterem spring-boot-starter-validation. Wynikiem sprawdzenia jest ZBIÓR naruszeń
 *   (ConstraintViolation) — pusty zbiór oznacza, że dane są poprawne.
 *
 * ANALOGIA: kontrola bagażu na lotnisku.
 *   Regulamin (adnotacje) wisi na tablicy: „maks. 23 kg”, „bez płynów powyżej 100 ml”. Kontroler (Validator) przechodzi
 *   przez bagaż, sprawdza WSZYSTKIE reguły i spisuje wszystkie naruszenia na jednej kartce — nie zatrzymuje się na
 *   pierwszym. Pusta kartka = można lecieć.
 *
 * JAK TO DZIAŁA:
 *   record NewProduct(@NotBlank @Size(min = 3, max = 40) String name, @Positive int priceGrosze, ...)
 *   Set(ConstraintViolation) violations = validator.validate(new NewProduct(...));
 *     1. Validator czyta adnotacje przez refleksję (reflection) — z pól, a dla rekordów z komponentów
 *     2. dla każdej reguły woła jej ConstraintValidator (np. NotBlankValidator)
 *     3. każdą porażkę zamienia w ConstraintViolation: ścieżka pola (propertyPath), wartość, szablon komunikatu
 *     4. komunikat powstaje z szablonu + ustawień językowych (locale)
 *   Spring Boot: ValidationAutoConfiguration tworzy bean defaultValidator (LocalValidatorFactoryBean), który jest
 *   jednocześnie jakarta.validation.Validator i org.springframework.validation.Validator — wstrzykujesz go, gdzie trzeba.
 *   Samo porównanie z adnotacjami (jak w t19_annotations_reflection/Annotations04Validator) — tu zrobiła je biblioteka.
 *
 * SŁÓWKA:
 *   constraint = ograniczenie; violation = naruszenie; property path = ścieżka właściwości (nazwa pola);
 *   invalid value = niepoprawna wartość; message template = szablon komunikatu; interpolation = podstawianie wartości
 *   do komunikatu; locale = ustawienia językowe; null = brak wartości; blank = pusty lub z samych spacji;
 *   past = przeszłość; future = przyszłość; clock provider = dostawca zegara.
 *
 * ZOBACZ TEŻ: t19_annotations_reflection/Annotations04Validator (własny mini-walidator na refleksji),
 *             s05_validation/Val02ValidInControllers (te same adnotacje w kontrolerach),
 *             s05_validation/Val03NestedAndGroups, s05_validation/Val04CustomValidators.
 * </pre>
 */
public final class Val01BeanValidation {

    private Val01BeanValidation() {
    }

    // =================================================================================================
    // 1. @NotNull, @NotEmpty, @NotBlank — trzy „prawie to samo”, które różnią się w szczegółach
    // =================================================================================================
    //
    // Wartość tekstu:   null      ""       "   " (spacje)   "x"
    //   @NotNull        BŁĄD      ok       ok               ok        (tylko „nie null”)
    //   @NotEmpty       BŁĄD      BŁĄD     ok               ok        (nie null i długość > 0; także kolekcje i mapy)
    //   @NotBlank       BŁĄD      BŁĄD     BŁĄD             ok        (nie null i jest co najmniej jeden znak niebiały;
    //                                                                  TYLKO dla tekstów)
    //
    // Sprawdzone testem Val01BeanValidationTest.notNullNotEmptyNotBlank.
    //
    // PUŁAPKA: dla pola tekstowego „obowiązkowego” wpisujesz @NotNull — a użytkownik wysyła "" albo "   " i przechodzi.
    //   Dlaczego? @NotNull sprawdza WYŁĄCZNIE null. Formularz z pustym polem zwykle wysyła pusty tekst, nie null.
    //
    // DOBRA PRAKTYKA: dla tekstów używaj @NotBlank, dla kolekcji @NotEmpty, dla liczb i dat @NotNull.
    //   Dlaczego? To najmocniejsza kontrola, która ma sens dla danego typu. @NotBlank na liczbie w ogóle nie
    //   zadziała (zob. sekcja 6).

    /** Sonda do sekcji 1: to samo pole trzy razy, za każdym razem z inną adnotacją. */
    public record NullabilityProbe(@NotNull String notNull, @NotEmpty String notEmpty, @NotBlank String notBlank) {
    }

    // =================================================================================================
    // 2. Adnotacje na komponentach rekordu — @Size, @Min, @Max, @Positive, @Email, @Pattern
    // =================================================================================================
    //
    // Rekord (record) to idealny nośnik danych z formularza: niezmienny, krótki. Adnotację dajesz prosto przy komponencie
    // (parametrze rekordu) — Java sama przenosi ją na pole, akcesor i parametr konstruktora, a Validator czyta pole.
    //
    //   @Size(min, max)   długość tekstu / liczba elementów kolekcji
    //   @Min, @Max        zakres liczb całkowitych (włącznie z granicami)
    //   @Positive         większa od zera (0 NIE jest dodatnie; @PositiveOrZero dopuszcza 0)
    //   @Email            wygląd adresu e-mail
    //   @Pattern(regexp)  wyrażenie regularne pasujące do CAŁEGO tekstu
    //
    // PUŁAPKA: zasada „null jest poprawny”. Wszystkie te adnotacje oprócz @NotNull/@NotEmpty/@NotBlank uznają null
    //   za poprawną wartość! @Size(min = 3) na polu null NIE zgłasza błędu. Dlaczego? Specyfikacja dzieli obowiązki:
    //   „czy wartość jest?” sprawdza @NotNull, „czy wartość ma dobry kształt?” — reszta. Dzięki temu pole opcjonalne
    //   (może być puste, ale jeśli jest, ma pasować) da się opisać jedną adnotacją. Pole obowiązkowe = @NotBlank
    //   PLUS adnotacja kształtu.
    //
    // PUŁAPKA: @Email przepuszcza „jan@localhost” (domena bez kropki). Dlaczego? Wzorzec jest celowo liberalny, bo
    //   adresy wewnątrz sieci firmowej bywają takie. Chcesz wymagać kropki? Dodaj @Pattern z własnym wyrażeniem.
    //
    // PUŁAPKA: @Pattern(regexp = "[A-Z]{3}") dla "ABCD" zgłasza błąd, bo wzorzec musi pasować do CAŁEGO tekstu
    //   (jak String.matches), a nie do jego fragmentu (jak Matcher.find). Dlaczego to ważne? W innych językach i
    //   narzędziach wyrażenie często szuka fragmentu — tu nie.

    /** NewProduct = nowy produkt wysyłany przez formularz sklepu. Wszystkie reguły opisane adnotacjami przy komponentach. */
    public record NewProduct(
            @NotBlank(message = "Nazwa produktu jest wymagana")
            @Size(min = 3, max = 40, message = "Nazwa ma mieć od 3 do 40 znaków")
            String name,                                    // name = nazwa
            @NotBlank
            @Pattern(regexp = "[A-Z]{3}-\\d{4}", message = "Kod ma postać ABC-1234")
            String code,                                    // code = kod produktu
            @Positive int priceGrosze,                      // price = cena (w groszach, żeby uniknąć błędów zaokrągleń)
            @Min(0) @Max(1000) int stock,                   // stock = stan magazynowy
            @Email String contactEmail,                     // contact e-mail = e-mail do kontaktu (opcjonalny)
            @Past LocalDate producedOn,                     // produced on = wyprodukowano (data z przeszłości)
            @Future LocalDate bestBefore) {                 // best before = najlepiej spożyć przed (data z przyszłości)

        /** Poprawny produkt — punkt wyjścia do testów; w testach podmieniamy pojedyncze pola. */
        public static NewProduct valid() {
            return new NewProduct("Mleko 3,2%", "MLK-0001", 349, 120, "dostawca@sklep.pl",
                    LocalDate.of(2000, 1, 1), LocalDate.of(2999, 1, 1));
        }
    }

    // =================================================================================================
    // 3. @Past i @Future — daty bez wołania zegara w naszym kodzie
    // =================================================================================================
    //
    // @Past / @Future porównują datę z „teraz”, a „teraz” bierze Validator z zegara. W kursie nie używamy
    // LocalDate.now() w logice (testy musiałyby zależeć od dnia uruchomienia). Są dwie drogi:
    //   a) daty z pewnym zapasem: 2000-01-01 jest w przeszłości, 2999-01-01 w przyszłości — w każdym dniu testu;
    //   b) ClockProvider (dostawca zegara) w konfiguracji fabryki walidatorów: wstawiasz zegar stały (Clock.fixed),
    //      więc „teraz” jest ustalone i granicę (dziś) można testować co do dnia.
    //
    //   Validation.byDefaultProvider().configure().clockProvider(() -> Clock.fixed(...)).buildValidatorFactory()
    //
    // PUŁAPKA: @Past dla DZISIEJSZEJ daty zgłasza błąd, a @PastOrPresent — nie. Dlaczego? „Past” to ściśle wcześniej
    //   niż teraz; dla LocalDate „dziś” nie jest ani przeszłością, ani przyszłością. Szukasz „nie później niż dziś”?
    //   Użyj @PastOrPresent. Sprawdzone testem Val01BeanValidationTest.pastAndFuture (zegar stały).

    // =================================================================================================
    // 4. Programowy Validator i ConstraintViolation
    // =================================================================================================
    //
    //   try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
    //       Validator validator = factory.getValidator();                  // bezpieczny wątkowo, tworzymy raz
    //       Set(ConstraintViolation(NewProduct)) violations = validator.validate(product);
    //   }
    //
    // Co niesie ConstraintViolation:
    //   getPropertyPath()      ścieżka do pola, np. "name" (przy zagnieżdżeniu: "items[0].name" — lekcja Val03NestedAndGroups)
    //   getInvalidValue()      wartość, która nie przeszła
    //   getMessage()           gotowy komunikat (po podstawieniu wartości i wyborze języka)
    //   getMessageTemplate()   szablon komunikatu przed podstawieniem, np. "{jakarta.validation.constraints.NotBlank.message}"
    //   getConstraintDescriptor().getAnnotation()   sama adnotacja, np. @Size(min = 3, max = 40)
    //
    // Zbiór violations jest typu Set — nie ma gwarantowanej kolejności. Dlatego w testach SORTUJ (zob. Val01BeanValidationTest).
    //
    // DOBRA PRAKTYKA: fabrykę walidatorów buduj raz (w Springu to bean) i używaj jej Validatora wielokrotnie.
    //   Dlaczego? Zbudowanie fabryki jest drogie (skanowanie adnotacji, ładowanie komunikatów), a sam Validator jest
    //   bezpieczny wątkowo.
    //
    // PUŁAPKA: Validator zwraca naruszenia, ale NIE rzuca wyjątku. Dlaczego to ważne? Jeśli zapomnisz sprawdzić wynik
    //   (violations.isEmpty()), niepoprawne dane przejdą dalej bez żadnego sygnału.

    // =================================================================================================
    // 5. Komunikaty: własny tekst, podstawianie wartości i język
    // =================================================================================================
    //
    // Własny komunikat: @Size(min = 3, max = 40, message = "Nazwa ma mieć od 3 do 40 znaków").
    // Szablon może zawierać wartości adnotacji w klamrach: message = "Nazwa: od {min} do {max} znaków" → „od 3 do 40”.
    // Bez własnego message komunikat pochodzi z paczki tłumaczeń Hibernate Validatora i zależy od LOCALE:
    //   angielski: „must not be blank”, polski: „nie może być odstępem” (paczka ValidationMessages_pl istnieje).
    // Locale wybiera Validator z ustawień domyślnych JVM (Locale.getDefault()) — komputer ucznia i serwer CI mogą mieć
    // inne!
    //
    // PUŁAPKA: test, który porównuje domyślny komunikat („must not be blank”), przechodzi na Twoim komputerze, a
    //   na serwerze z innym językiem się wywraca. Dlaczego? Komunikat zależy od ustawień językowych środowiska.
    //   Test powinien sprawdzać ścieżkę pola i TYP ograniczenia (NotBlank.class), a komunikat — tylko własny
    //   (message = "...") albo przy jawnie ustawionym języku (defaultLocale w konfiguracji fabryki).
    //
    // DOBRA PRAKTYKA: komunikaty dla użytkownika dawaj w message = "..." lub w kluczach {klucz} z własnego pliku
    //   ValidationMessages.properties. Dlaczego? Domyślne teksty są techniczne („size must be between 3 and 40”),
    //   a własne mogą mówić językiem aplikacji.

    // =================================================================================================
    // 6. Nieobsługiwany typ: adnotacja musi pasować do typu pola
    // =================================================================================================
    //
    // @Size działa na tekstach, kolekcjach, mapach i tablicach — NIE na Integer. @NotBlank działa tylko na tekstach.
    // Zła para (np. @Size(min = 2) Integer qty) nie jest cichym „pomijaniem”: Validator rzuca wyjątek
    // UnexpectedTypeException przy pierwszym validate(...) tego obiektu (unexpected type = nieoczekiwany typ).
    // Błąd widać więc od razu, nie na produkcji — pod warunkiem, że obiekt przeszedł przez walidację w teście.

    /** Sonda do sekcji 5: komunikaty domyślne (zależne od języka) i własny szablon z {min} i {max}. */
    public record MessageProbe(
            @NotBlank String plain,                                                  // komunikat domyślny
            @Size(min = 2, max = 5, message = "od {min} do {max} znaków") String templated) {   // własny szablon
    }

    /** Zły przykład do sekcji 6: @Size na liczbie. NIE używaj tak w prawdziwym kodzie. */
    public record BrokenQuantity(@Size(min = 2) Integer qty) {
    }

    // =================================================================================================
    // 7. Validator jako bean Springa
    // =================================================================================================
    //
    // W aplikacji Spring Boot nie budujesz fabryki ręcznie — prosisz o gotowy bean:
    //   public ProductChecker(jakarta.validation.Validator validator) { ... }   // wstrzykiwanie przez konstruktor
    // To LocalValidatorFactoryBean z autokonfiguracji. Zaleta: ten sam Validator (i te same ustawienia) ma też Spring MVC,
    // a w lekcji Val04CustomValidators zobaczysz, że walidatory własnych ograniczeń mogą wtedy dostać beany Springa.
    // Test: Val01BeanValidationSpringTest.

    /** Sprawdza produkt i zwraca posortowane nazwy pól z błędami. Przykład użycia wstrzykniętego Validatora. */
    public static List<String> invalidFields(jakarta.validation.Validator validator, Object bean) {
        Set<jakarta.validation.ConstraintViolation<Object>> violations = validator.validate(bean);
        return violations.stream().map(v -> v.getPropertyPath().toString()).sorted().toList();
    }

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Reguły = adnotacje jakarta.validation.constraints.* przy polach / komponentach rekordu.
     *   • Wynik validator.validate(obiekt) = Set naruszeń; pusty zbiór = poprawne. Nie rzuca wyjątku.
     *   • Teksty: @NotBlank; kolekcje: @NotEmpty; liczby i daty: @NotNull. @NotNull nie widzi "" ani "  ".
     *   • Reszta adnotacji (@Size, @Email, @Pattern, @Min...) przepuszcza null — obowiązkowość to osobne @NotNull/@NotBlank.
     *   • @Positive: > 0; @Pattern pasuje do całego tekstu; @Email przepuszcza „jan@localhost”.
     *   • @Past/@Future liczą względem zegara: testuj dalekimi datami albo ClockProvider ze stałym zegarem.
     *   • Komunikaty zależą od locale: w testach sprawdzaj ścieżkę pola i typ ograniczenia, nie domyślny tekst.
     *   • Zły typ pola (@Size na Integer) → UnexpectedTypeException.
     *   • W Springu Validator to gotowy bean (jakarta.validation.Validator).
     *
     * PYTANIA KONTROLNE:
     *   1. PYTANIE REKRUTACYJNE: czym różnią się @NotNull, @NotEmpty i @NotBlank? Który wybierzesz dla nazwy produktu?
     *   2. Co się stanie? Pole {@code @Size(min = 3) String name} ma wartość null. Czy będzie naruszenie?
     *   3. Co się stanie? Adres „jan@localhost” sprawdzamy adnotacją @Email.
     *   4. Co się stanie? Dajesz {@code @Size(min = 2) Integer qty} i wołasz validator.validate(...).
     *   5. ZNAJDŹ BŁĄD: test porównuje {@code getMessage()} z tekstem „must not be blank” i zawodzi na komputerze kolegi.
     *      Dlaczego i jak go naprawić?
     *   6. PYTANIE REKRUTACYJNE: czym jest Jakarta Bean Validation, a czym Hibernate Validator?
     *   7. Co się stanie? Pole {@code @Past LocalDate} dostaje dzisiejszą datę (zegar stały na ten dzień).
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Val01BeanValidationExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): dopisz adnotacje do komponentów rekordu Exercise1Customer:
     * name — nie może być pusty ani z samych spacji; email — poprawny adres; age — co najmniej 18.
     * Podpowiedź: {@code @NotBlank}, {@code @Email}, {@code @Min(18)} — pamiętaj, że @Email przepuszcza null,
     * więc dla obowiązkowego e-maila potrzeba jeszcze {@code @NotBlank}.
     */
    public record Exercise1Customer(
            // TODO: dopisz adnotacje przy każdym komponencie (name, email, age)
            String name,
            String email,
            int age) {
    }

    /**
     * ĆWICZENIE 2 (średnie): zaimplementuj metodę — sprawdź obiekt podanym Validatorem i zwróć POSORTOWANĄ listę
     * tekstów w postaci {@code "pole: komunikat"} (np. {@code "name: Nazwa jest wymagana"}).
     * Podpowiedź: {@code validator.validate(bean)}, potem {@code getPropertyPath()} i {@code getMessage()}, na końcu sortuj.
     */
    public static List<String> exercise2(jakarta.validation.Validator validator, Object bean) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): opisz regułami rekord Exercise3Employee (pracownik), z DOKŁADNYMI komunikatami:
     * login — od 3 do 12 znaków, same litery a-z (małe), komunikat dla długości "Login: 3-12 znaków", dla liter
     * "Login: tylko małe litery"; salary (pensja w groszach) — od 1 do 1000000 (włącznie), komunikat
     * "Pensja: od 1 do 1000000" dla obu granic; hiredOn — data z przeszłości, komunikat "Data zatrudnienia musi być z przeszłości".
     * Podpowiedź: dwie adnotacje na jednym polu (@Size i @Pattern) dają dwa niezależne naruszenia; null przepuszcza
     * się samo, więc dodaj {@code @NotNull} dla loginu (salary jest typu int, więc null mu nie grozi).
     */
    public record Exercise3Employee(
            // TODO: dopisz adnotacje z komunikatami (login, salary, hiredOn)
            String login,
            int salary,
            LocalDate hiredOn) {
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. @NotNull: tylko „nie null”. @NotEmpty: nie null i niepusty (długość/rozmiar > 0), także dla kolekcji i map.
     *      @NotBlank: tylko teksty; nie null, niepusty i z co najmniej jednym znakiem niebiałym. Dla nazwy produktu
     *      @NotBlank — odrzuca null, "" i "   ". Sprawdzone testem Val01BeanValidationTest.notNullNotEmptyNotBlank.
     *   2. Nie będzie naruszenia: @Size uznaje null za poprawny. Obowiązkowość wymaga @NotNull/@NotBlank.
     *      Sprawdzone testem Val01BeanValidationTest.constraintsOnRecord.
     *   3. Adres przejdzie jako poprawny — @Email jest liberalny. Sprawdzone testem Val01BeanValidationTest.constraintsOnRecord.
     *   4. Wyjątek jakarta.validation.UnexpectedTypeException (HV000030: nie znaleziono walidatora dla @Size i typu
     *      Integer). Sprawdzone testem Val01BeanValidationTest.unsupportedType.
     *   5. Domyślny komunikat zależy od Locale.getDefault() — na polskim komputerze brzmi „nie może być odstępem”.
     *      Naprawa: asercja na ścieżkę pola i typ ograniczenia (NotBlank) albo własny message / jawny defaultLocale
     *      w fabryce. Sprawdzone testem Val01BeanValidationTest.messages.
     *   6. Jakarta Bean Validation to SPECYFIKACJA (interfejsy i adnotacje w pakiecie jakarta.validation), Hibernate
     *      Validator to jej główna IMPLEMENTACJA (silnik sprawdzający). Nazwa „Hibernate” nie oznacza zależności od bazy.
     *   7. Naruszenie: @Past jest ściśle „przed teraz”, dzisiejsza data nie przechodzi (@PastOrPresent by przeszła).
     *      Sprawdzone testem Val01BeanValidationTest.pastAndFuture.
     */
    // </editor-fold>
}
