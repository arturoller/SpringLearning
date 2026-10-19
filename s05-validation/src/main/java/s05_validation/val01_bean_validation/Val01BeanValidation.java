package s05_validation.val01_bean_validation;

import jakarta.validation.Validator;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * TEMAT: Bean Validation — reguły poprawności danych zapisane adnotacjami na rekordzie
 *        (validation = walidacja, sprawdzanie poprawności; constraint = ograniczenie, reguła; violation = naruszenie reguły)
 *
 * W SKRÓCIE:
 *   Dane z zewnątrz (formularz, JSON, parametr adresu) są NIEZAUFANE: mogą być puste, za długie, ujemne, w złym formacie.
 *   Zamiast pisać w każdej metodzie ciąg {@code if (name == null || name.isBlank()) ...}, opisujemy reguły adnotacjami
 *   przy samych danych ({@code @NotBlank String name}), a gotowy walidator sprawdza je wszystkie naraz i oddaje listę
 *   naruszeń. Specyfikacja nazywa się Jakarta Bean Validation (pakiet {@code jakarta.validation}), jej implementacją jest
 *   Hibernate Validator, a Spring Boot (starter {@code spring-boot-starter-validation}) tworzy gotowy bean {@code Validator}.
 *
 * ANALOGIA: kontrola jakości przy dostawie do sklepu.
 *   Towar przyjeżdża z zewnątrz. Zamiast liczyć na to, że każdy magazynier sam zapamięta, co sprawdzić, na paczce jest
 *   naklejona lista kontrolna („waga większa od 0”, „etykieta wypełniona”, „kod w formacie ELE-001”). Kontroler (Validator)
 *   przechodzi po liście i oddaje raport ze WSZYSTKIMI usterkami, a nie tylko z pierwszą.
 *
 * JAK TO DZIAŁA:
 *   PRZED (reguły rozproszone w kodzie, łatwo o pominięcie):
 *     if (form.name() == null || form.name().isBlank()) throw ...;
 *     if (form.price() == null || form.price().signum() <= 0) throw ...;   // i tak w każdej metodzie
 *
 *   PO (reguły przy danych, jeden walidator):
 *     record ProductForm(@NotBlank String name, @NotNull @DecimalMin("0.01") BigDecimal price) { }
 *     Set{@code <ConstraintViolation<ProductForm>>} violations = validator.validate(form);
 *     violations.isEmpty()  →  dane poprawne
 *     każde naruszenie: getPropertyPath() (które pole), getMessage() (komunikat), getInvalidValue() (zła wartość)
 *
 *   Pod spodem: Spring Boot (ValidationAutoConfiguration) rejestruje bean "defaultValidator" typu
 *   LocalValidatorFactoryBean — to jedna klasa spinająca Hibernate Validator ze Springiem. Implementuje ona ZARAZ
 *   {@code jakarta.validation.Validator} i springowy {@code org.springframework.validation.Validator}. Bean Validation
 *   działa też bez Springa ({@code Validation.buildDefaultValidatorFactory()}) — sekcja 7.
 *
 * SŁÓWKA:
 *   constraint = ograniczenie (reguła na polu); violation = naruszenie reguły; property path = ścieżka właściwości
 *   (nazwa pola, np. "name"); invalid value = niepoprawna wartość; message = komunikat; message template = szablon
 *   komunikatu (tekst z {klucz} lub {min}, zanim zostanie wypełniony); interpolation = interpolacja (podstawianie
 *   wartości do szablonu); blank = „biały” (pusty albo same spacje); locale = ustawienia regionalne (język komunikatów).
 *
 * ZOBACZ TEŻ: s05_validation/Val02ValidInControllers (walidacja danych przychodzących do kontrolera),
 *             s05_validation/Val04CustomValidators (własne adnotacje), t19_annotations_reflection/Annotations04Validator
 *             (jak mały walidator oparty na adnotacjach działa „w środku”), t09_records/Records01Basics (rekordy).
 * </pre>
 */
public final class Val01BeanValidation {

    private Val01BeanValidation() {
    }

    // =================================================================================================
    // 1. START: pakiet jakarta.validation i bean Validator
    // =================================================================================================
    //
    // W pom.xml modułu jest starter spring-boot-starter-validation. Przynosi interfejsy z jakarta.validation
    // (@NotNull, Validator...) i Hibernate Validator (implementację, wersja 9 w Boot 4.1).
    //
    // UWAGA (stare tutoriale): do Spring Boota 2 adnotacje były w pakiecie javax.validation. Od Boota 3 (Jakarta EE 9)
    // jest jakarta.validation. Gdy kod z internetu nie kompiluje się przez "javax.validation" — zamień na "jakarta.validation".
    //
    // Kontekst Springa tej lekcji startuje Val01Application. Test pobiera z niego bean Validator i sprawdza, że to
    // LocalValidatorFactoryBean o nazwie defaultValidator (test: section1_validatorBean).

    // =================================================================================================
    // 2. PODSTAWOWE OGRANICZENIA NA REKORDZIE (ProductForm)
    // =================================================================================================
    //
    // Adnotacje stawiamy na składowych rekordu (record components):
    //   @NotBlank   — tekst niepusty i nie same spacje              @Pattern — tekst pasuje do wyrażenia regularnego
    //   @NotNull    — wartość podana (nie null)                      @Size    — długość tekstu (lub rozmiar kolekcji)
    //   @DecimalMin — liczba dziesiętna nie mniejsza niż podana      @Min/@Max — liczba całkowita w zakresie
    //   @Positive   — większe od zera (0 NIE jest dodatnie)          @Email, @Past, @Future — adres e-mail, data z przeszłości/przyszłości
    //
    // Walidator zwraca ZBIÓR naruszeń (Set) — nie lista i nie ma gwarantowanej kolejności. Dlatego w testach
    // zamieniamy ścieżki pól na posortowany zbiór, zamiast liczyć na kolejność.
    //
    // PUŁAPKA: oczekiwać, że Set naruszeń ma stałą kolejność (np. „pierwsze naruszenie to name”). Dlaczego? Zbiór nie
    //   gwarantuje kolejności, więc test przejdzie dziś, a jutro (inna wersja biblioteki, inny JDK) zaświeci na czerwono.
    //   Sortuj przed porównaniem.
    //
    // Test: section2_basicConstraints.

    // =================================================================================================
    // 3. @NotNull vs @NotEmpty vs @NotBlank vs @Size — RÓŻNICE
    // =================================================================================================
    //
    //   wartość        @NotNull   @NotEmpty   @NotBlank   @Size(min = 2)
    //   null           błąd       błąd        błąd        OK  ← uwaga!
    //   ""   (pusty)   OK         błąd        błąd        błąd
    //   " "  (spacja)  OK         OK          błąd        błąd
    //   "x"            OK         OK          OK          błąd
    //   "xy"           OK         OK          OK          OK
    //
    // Reguła kciuka: dla TEKSTU z formularza zwykle chcesz @NotBlank (zabrania null, "" i samych spacji). @NotEmpty
    // ma sens dla kolekcji i map (lista nie może być pusta); @NotNull — gdy pusty tekst jest dopuszczalny.
    //
    // PUŁAPKA: używać @NotEmpty dla nazwy użytkownika i myśleć, że "   " (same spacje) zostanie odrzucone. Dlaczego nie?
    //   @NotEmpty sprawdza tylko, czy długość jest większa od zera — trzy spacje to długość 3, więc przechodzą.
    //   Użyj @NotBlank, który obcina białe znaki przed sprawdzeniem.
    //
    // Test: section3_notNullNotEmptyNotBlank (tabelka powyżej jest sprawdzona uruchomieniem).

    // =================================================================================================
    // 4. null JEST POPRAWNY dla większości ograniczeń
    // =================================================================================================
    //
    // Reguła specyfikacji: ograniczenia poza @NotNull, @NotEmpty i @NotBlank uznają null za POPRAWNĄ wartość. Wynika to
    // z podziału obowiązków: „czy pole jest wymagane” to osobne pytanie od „czy wartość ma dobry format”.
    // Dotyczy to m.in. @Size, @Min, @Max, @Positive, @Pattern, @Email, @Past, @Future.
    //
    // PUŁAPKA: pole z samym {@code @Size(min = 3) String description} i przekonanie „opis jest obowiązkowy”. Dlaczego to błąd?
    //   Brakujące pole w JSON-ie daje null, a null przechodzi @Size bez uwag. Jeśli pole ma być wymagane, dodaj
    //   {@code @NotNull} (albo użyj {@code @NotBlank}). Ta sama pułapka dotyczy {@code @Email} i {@code @Positive}.
    //
    // PUŁAPKA: ufać, że {@code @Email} sprawdza, czy adres naprawdę istnieje. Dlaczego nie? To tylko kontrola formatu,
    //   a i ona jest łagodna: "jan@example" (bez kropki i domeny najwyższego poziomu) jest uznane za poprawne, a
    //   pusty tekst "" także. Prawdziwy adres potwierdza się wysłaniem wiadomości z linkiem.
    //
    // PUŁAPKA: {@code @Past} dla dzisiejszej daty zgłasza błąd (dziś nie jest „przeszłością”). Test z "dzisiaj"
    //   zależałby od zegara systemowego, więc w testach używamy dat odległych (1990-01-01, 2999-01-01).
    //
    // Test: section4_nullIsValid.

    // =================================================================================================
    // 5. Validator, ConstraintViolation
    // =================================================================================================
    //
    // validator.validate(obiekt)                          → Set{@code <ConstraintViolation<T>>} dla całego obiektu
    // validator.validateValue(Klasa.class, "pole", wartość) → sprawdza JEDNĄ wartość względem reguł pola (bez tworzenia obiektu)
    // ConstraintViolation to „raport usterki”:
    //   getPropertyPath()                          — nazwa pola ("name"), przy zagnieżdżeniu "items[0].qty" (Val03)
    //   getMessage()                               — komunikat po interpolacji (już wypełniony)
    //   getMessageTemplate()                       — szablon przed interpolacją, np. "{jakarta.validation.constraints.NotBlank.message}"
    //   getInvalidValue()                          — wartość, która złamała regułę
    //   getRootBeanClass()                         — klasa walidowanego obiektu
    //   getConstraintDescriptor().getAnnotation()  — adnotacja, która zgłosiła naruszenie
    //
    // DOBRA PRAKTYKA: w testach i w logice rozpoznawaj naruszenia po ścieżce pola i typie adnotacji, a nie po treści
    //   komunikatu. Dlaczego? Komunikat zależy od języka (sekcja 6) i łatwo go zmienić, a pole i typ reguły są stabilne.
    //
    // Test: section5_constraintViolation.

    // =================================================================================================
    // 6. KOMUNIKATY: własne, z pliku ValidationMessages.properties i język
    // =================================================================================================
    //
    // a) Własny tekst wprost:        @NotBlank(message = "Nazwa produktu jest wymagana")
    // b) Interpolacja wartości:      @Size(min = 3, max = 20, message = "Opis: od {min} do {max} znaków")  → "Opis: od 3 do 20 znaków"
    //    (w klamrach nazwy atrybutów adnotacji; {value} dla @DecimalMin)
    // c) Klucz z pliku:              @NotNull(message = "{val01.price.required}")
    //    Klucz musi leżeć w pliku ValidationMessages.properties w korzeniu classpath (src/main/resources). Nazwa pliku
    //    jest narzucona przez specyfikację. Plik jest czytany jako UTF-8, więc polskie litery wpisujesz wprost
    //    (w Javie 8 pliki .properties czytano jako ISO-8859-1 i trzeba było pisać sekwencje unikodowe — w nowszych nie).
    // d) Komunikaty DOMYŚLNE (bez message) — Hibernate Validator ma je w wielu językach, w tym po polsku. Język wybiera
    //    się z ustawień regionalnych: w aplikacji Springa z kontekstu żądania (nagłówek Accept-Language), w teście
    //    z LocaleContextHolder.
    //
    // PUŁAPKA: porównywać w teście domyślny komunikat ("must be less than or equal to 1000") bez ustalenia języka.
    //   Dlaczego? Na komputerze z polskimi ustawieniami ten sam komunikat brzmi „musi być równe lub mniejsze od 1000” —
    //   test przejdzie u autora, a padnie u kolegi (albo na serwerze CI). Testuj pole i typ reguły albo własny
    //   komunikat, a gdy musisz sprawdzić domyślny tekst — ustaw język jawnie.
    //
    // DOBRA PRAKTYKA: własne komunikaty z kluczem (c) trzymaj w jednym pliku. Dlaczego? Jedno miejsce do tłumaczenia
    //   i poprawek, a adnotacje w kodzie pozostają krótkie.
    //
    // Test: section6_messages.

    // =================================================================================================
    // 7. BEAN VALIDATION BEZ SPRINGA
    // =================================================================================================
    //
    // Bean Validation to osobna biblioteka. Walidator zbudujesz też ręcznie:
    //   try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) { Validator v = factory.getValidator(); ... }
    // Spring Boot robi to samo, tylko trzyma fabrykę jako bean (jedna na całą aplikację).
    //
    // DOBRA PRAKTYKA: w aplikacji Springa wstrzykuj gotowy bean Validator, zamiast budować własną fabrykę. Dlaczego?
    //   Budowanie fabryki jest kosztowne (skanuje adnotacje, ładuje komunikaty), a bean Springa jest jedną, współdzieloną
    //   instancją, już zintegrowaną z kontekstem Springa (język, beany w walidatorach — Val04).
    //
    // Test: section7_withoutSpring (zwykły test, bez kontekstu Springa — szybki).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Adnotacje: jakarta.validation.constraints.* (od Boota 3; wcześniej javax.validation).
     *   • Tekst z formularza: @NotBlank. Kolekcja: @NotEmpty. Tylko „podane”: @NotNull.
     *   • Poza @NotNull/@NotEmpty/@NotBlank każda reguła uznaje null za poprawny — wymagane pole = reguła formatu + @NotNull.
     *   • @Positive: większe od 0 (0 odpada); @Min(0): od zera włącznie. @Email jest łagodny ("jan@example" przechodzi).
     *   • validator.validate(obiekt) → Set naruszeń (bez kolejności); ścieżka pola, komunikat, zła wartość, adnotacja.
     *   • Komunikaty: message = "tekst", "{min}/{max}" w tekście, "{klucz}" z ValidationMessages.properties (UTF-8).
     *   • Domyślne komunikaty zależą od języka — testuj pole i typ reguły, nie tekst.
     *   • Spring Boot: bean defaultValidator (LocalValidatorFactoryBean); Bean Validation działa też bez Springa.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się {@code @NotEmpty} od {@code @NotBlank}? Dla jakiej wartości tekstowej dają różny wynik?
     *   2. Co się stanie? Pole {@code @Size(min = 3) String description}, a walidujesz rekord z {@code description = null}.
     *      Ile naruszeń zgłosi walidator?
     *   3. ZNAJDŹ BŁĄD: {@code record Signup(@Email String email) { }} — autor twierdzi, że rejestracja bez e-maila
     *      jest odrzucana. Dlaczego nie jest i jak to naprawić?
     *   4. Co się stanie? Walidujesz {@code new CustomerForm("Jan", "jan@example", null, null)}. Ile naruszeń?
     *   5. Gdzie leży plik z własnymi komunikatami, jak się nazywa i w jakim kodowaniu go zapisujesz?
     *   6. Dlaczego test porównujący tekst domyślnego komunikatu może przejść u Ciebie, a paść na serwerze CI?
     *   7. Co zwraca {@code validator.validate(obiekt)}, gdy wszystko jest poprawne — {@code null} czy pusty zbiór?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Val01BeanValidationExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć posortowaną alfabetycznie listę NAZW PÓL (ścieżek właściwości), które mają naruszenia
     * w podanym formularzu, bez powtórzeń. Poprawny formularz → pusta lista.
     * Podpowiedź: validator.validate(form), na każdym naruszeniu getPropertyPath().toString(), potem sortowanie
     * (strumień z sorted() albo TreeSet).
     */
    static List<String> fieldsWithErrors(Validator validator, ProductForm form) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // ĆWICZENIE 2 (średnie): otwórz rekord EmployeeForm (plik EmployeeForm.java) i dopisz do jego składowych adnotacje
    // Bean Validation: imię — niepuste i nie same spacje; pensja — podana ORAZ większa od zera; e-mail — podany ORAZ
    // w poprawnym formacie. (Ten ćwiczebny rekord leży w osobnym pliku — tutaj nie ma kodu do uzupełnienia.)
    // Podpowiedź: pamiętaj z sekcji 4, że sam @Email i sam @Positive przepuszczają null.

    /**
     * ĆWICZENIE 3 (trudniejsze): zwróć mapę „nazwa pola → posortowana lista komunikatów” dla wszystkich naruszeń
     * (jedno pole może mieć kilka naruszeń). Mapa ma być posortowana po nazwie pola (TreeMap).
     * Poprawny obiekt → pusta mapa.
     * Podpowiedź: pętla po naruszeniach, {@code computeIfAbsent(pole, k -> new ArrayList<>()).add(komunikat)}, na końcu
     * sortowanie list.
     */
    static Map<String, List<String>> messagesByField(Validator validator, Object object) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Dla tekstu złożonego z samych spacji (np. "   "): @NotEmpty go przepuszcza (długość większa od 0),
     *      @NotBlank odrzuca. Dla null i "" oba zgłaszają błąd. Sprawdzone testem (section3).
     *   2. Zero naruszeń — @Size uznaje null za poprawny. Żeby pole było wymagane, dodaj @NotNull (albo użyj @NotBlank).
     *      Sprawdzone testem (section4).
     *   3. @Email przepuszcza null (i pusty tekst), więc brak e-maila nie jest błędem. Poprawka: @NotBlank @Email
     *      (albo @NotNull @Email). Sprawdzone testem (section4: null i "" dają 0 naruszeń).
     *   4. Zero naruszeń: "jan@example" przechodzi @Email (łagodny format), a null w datach jest poprawny.
     *      Sprawdzone testem (section4).
     *   5. src/main/resources/ValidationMessages.properties (korzeń classpath, nazwa narzucona przez specyfikację),
     *      zapisany w UTF-8 — polskie litery wpisujesz wprost. W adnotacji: message = "{klucz}". Sprawdzone
     *      testem (section6: polskie litery z pliku wróciły w komunikacie).
     *   6. Domyślne komunikaty zależą od języka JVM/żądania. U Ciebie (pl) jest po polsku, na serwerze CI często po
     *      angielsku. Sprawdzone testem (section6: ten sam @Max daje różny tekst dla en i pl).
     *   7. Pusty zbiór (isEmpty() == true), nigdy null. Sprawdzone testem (section2).
     */
    // </editor-fold>
}
