package s05_validation.val01_bean_validation;

/**
 * <pre>
 * TEMAT: Bean Validation — deklaratywne sprawdzanie poprawności danych adnotacjami
 *        (validation = walidacja, sprawdzanie poprawności; bean = obiekt/ziarno Javy; constraint = ograniczenie,
 *         reguła nałożona na pole; violation = naruszenie, złamanie reguły; specification = specyfikacja,
 *         opis "jak ma być"; implementation = implementacja, kod, który to realizuje)
 *
 * W SKRÓCIE:
 *   Zamiast pisać w każdej metodzie {@code if (name == null || name.isBlank()) …}, kładziesz na polu adnotację
 *   {@code @NotBlank}. Osobny obiekt, {@code Validator}, czyta te adnotacje i zwraca zbiór naruszeń
 *   ({@code Set<ConstraintViolation>}): które pole, która reguła, jaka wartość, jaki komunikat. Cała lekcja działa
 *   najpierw BEZ Springa (zwykły JUnit), dopiero na końcu pokazujemy, co dokłada Spring Boot.
 *
 * ANALOGIA: kontrola na lotnisku.
 *   Pasażer (dane z zewnątrz) nie wchodzi do samolotu (logiki biznesowej), dopóki nie przejdzie bramki. Bramka ma
 *   listę reguł (bilet jest, imię niepuste, bagaż do 23 kg). Strażnik nie zatrzymuje się na pierwszym błędzie — spisuje
 *   WSZYSTKIE braki naraz, żeby pasażer poprawił je za jednym razem. Reguły wiszą na tablicy (adnotacje), a strażnik
 *   (Validator) tylko je stosuje — nie musisz go uczyć każdej reguły od nowa.
 *
 * JAK TO DZIAŁA:
 *   1. Adnotacje z pakietu {@code jakarta.validation.constraints} kładziesz na polach, komponentach rekordu albo getterach.
 *   2. {@code Validation.buildDefaultValidatorFactory()} znajduje implementację (Hibernate Validator) i buduje fabrykę.
 *   3. {@code factory.getValidator().validate(obiekt)} zbiera adnotacje, sprawdza każde pole i zwraca naruszenia.
 *   4. Pusty zbiór = dane poprawne. Każde naruszenie zna: ścieżkę właściwości ({@code getPropertyPath()}), złamaną
 *      adnotację ({@code getConstraintDescriptor().getAnnotation()}), odrzuconą wartość ({@code getInvalidValue()})
 *      i komunikat ({@code getMessage()}).
 *   5. Spring Boot (starter {@code spring-boot-starter-validation}) sam tworzy gotowy bean {@code Validator}, a Spring MVC
 *      używa go przy {@code @Valid} (Val02ValidInControllers).
 *
 * SŁÓWKA:
 *   blank = pusty lub same białe znaki; null = brak wartości; path = ścieżka; message = komunikat; template = szablon;
 *   interpolate = wstawić wartości w szablon komunikatu; positive = dodatni; past = przeszły; future = przyszły;
 *   pattern = wzorzec; digits = cyfry; factory = fabryka; group = grupa (Val03NestedAndGroups).
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest02RequestData (@RequestBody — skąd dane trafiają do DTO),
 *             s04_web_rest/Rest05CrudApi (ręczna kontrola isValid(), którą tu zastępujemy adnotacjami),
 *             s05_validation/Val02ValidInControllers (@Valid w kontrolerach i odpowiedź 400),
 *             s05_validation/Val03NestedAndGroups (obiekty zagnieżdżone, listy, grupy),
 *             s05_validation/Val04CustomValidators (własne adnotacje),
 *             s06_error_handling/Err04ProblemDetail (jak ładnie opisać błędy walidacji klientowi).
 * </pre>
 */
public final class Val01BeanValidation {

    private Val01BeanValidation() {
    }

    // =================================================================================================
    // 1. PO CO WALIDACJA I KTO CO DOSTARCZA (Val01BeanValidationTest, sekcja 1)
    // =================================================================================================
    //
    // Granica systemu: wszystko, co przychodzi z zewnątrz (JSON od klienta, formularz, plik, odpowiedź cudzego API),
    // jest NIEZAUFANE. Sprawdzasz to RAZ, na wejściu, a dalej kod pracuje na danych, o których wie, że są poprawne.
    // Walidacja nie jest dla wygody klienta — jest dla bezpieczeństwa i spójności: ujemna ilość w zamówieniu albo
    // cena 0 zepsuje księgowość, i to nie przez błąd programisty, tylko przez jedno złośliwe zapytanie.
    //
    //   Warstwa                     | Co to jest                                | Gdzie leży
    //   ----------------------------+-------------------------------------------+------------------------------------
    //   Jakarta Bean Validation     | SPECYFIKACJA: adnotacje (@NotBlank, @Size)| jakarta.validation:jakarta.validation-api
    //                               | i interfejsy (Validator, ConstraintViolation)| (wersja 3.1.x), pakiet jakarta.validation.*
    //   Hibernate Validator         | IMPLEMENTACJA: kod, który te adnotacje    | org.hibernate.validator:hibernate-validator
    //                               | naprawdę sprawdza                         | (wersja 9.x), pakiet org.hibernate.validator.*
    //   spring-boot-starter-validation | PACZKA: ściąga obie powyższe + Tomcat Embed EL (obsługa {…} w komunikatach) i
    //                               | autokonfigurację, która tworzy bean Validator
    //
    // Specyfikacja to jak umowa, implementacja to wykonawca. Twój kod importuje TYLKO adnotacje ze specyfikacji
    // (jakarta.validation.constraints.*), więc w razie potrzeby wykonawcę da się wymienić. Do Hibernate Validator
    // sięgasz wprost rzadko (np. do własnych rozszerzeń w sekcji 6).
    //
    // PUŁAPKA: w starych tutorialach importy brzmią {@code javax.validation.constraints.NotBlank}. Dlaczego to problem?
    //   Od Spring Boot 3 (Jakarta EE 9+) pakiety {@code javax.*} zmieniły nazwę na {@code jakarta.*}. Adnotacja z
    //   javax w ogóle się nie skompiluje (tej biblioteki nie ma w projekcie) — a gdy ją dodasz "na siłę", Validator jej
    //   NIE ZOBACZY i walidacja po cichu nie działa. W Boot 4 jest wyłącznie {@code jakarta.validation.*}.
    //
    // PYTANIE REKRUTACYJNE: czym różni się Bean Validation od Hibernate Validator? (Pierwsze to specyfikacja — adnotacje
    //   i interfejsy; drugie to jej implementacja referencyjna. Jak JDBC i sterownik bazy: interfejs kontra kod.)

    // =================================================================================================
    // 2. KATALOG ADNOTACJI (ProductForm, CustomerForm; Val01BeanValidationTest, sekcja 2)
    // =================================================================================================
    //
    // Najważniejsze zasada: prawie WSZYSTKIE ograniczenia uznają null za poprawne ("brak wartości to nie zła wartość").
    // Wymagalność wyraża osobno @NotNull / @NotEmpty / @NotBlank. Kontrola ustawiona na null/pustym tekście:
    //
    //   Adnotacja  | null  | "" (pusty) | "  " (same spacje) | "ala" | Dla jakich typów
    //   -----------+-------+------------+--------------------+-------+------------------------------------------
    //   @NotNull   | błąd  | OK         | OK                 | OK    | każdy typ
    //   @NotEmpty  | błąd  | błąd       | OK                 | OK    | tekst, kolekcja, mapa, tablica
    //   @NotBlank  | błąd  | błąd       | błąd               | OK    | TYLKO tekst (CharSequence)
    //
    // Zasada kciuka: tekst z formularza = @NotBlank; liczba/data/obiekt, który musi być podany = @NotNull; lista, która
    // nie może być pusta = @NotEmpty.
    //
    // Pozostałe (nazwy z ProductForm i CustomerForm):
    //   • @Size(min, max)          — długość tekstu albo liczba elementów kolekcji/mapy/tablicy (nie wartość liczby!).
    //   • @Min / @Max              — wartość liczby całkowitej (int, long, Integer, BigDecimal…). @Min(1) @Max(1000).
    //   • @Positive / @PositiveOrZero / @Negative / @NegativeOrZero — znak liczby. Dodatnie = większe od 0.
    //   • @DecimalMin / @DecimalMax — granica podana JAKO TEKST ("0.01"), idealna dla BigDecimal (cena). Domyślnie
    //     granica jest włączona (inclusive = true); inclusive = false oznacza "ściśle większe".
    //   • @Digits(integer, fraction) — ile cyfr przed i po przecinku. Cena {@code @Digits(integer = 6, fraction = 2)}
    //     odrzuci 0.001 (trzy miejsca po przecinku) — to typowa ochrona przed groszami ułamkowymi.
    //   • @Email                   — kształt adresu e-mail (lokalna@domena); NIE sprawdza, czy skrzynka istnieje.
    //   • @Pattern(regexp)         — wyrażenie regularne dla CAŁEGO tekstu (całość musi pasować, jak matches()).
    //   • @Past / @PastOrPresent / @Future / @FutureOrPresent — daty i czasy względem "teraz".
    //   • @AssertTrue / @AssertFalse — dla pól boolean (np. zgoda na regulamin).
    //
    // Test (bez Springa): ręcznie budujemy fabrykę i walidator.
    //
    //     try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {   // fabryka jest zasobem!
    //         Validator validator = factory.getValidator();
    //         Set<ConstraintViolation<ProductForm>> violations = validator.validate(form);
    //     }
    //
    // Nie ma tu żadnego Springa — Validator to czysta Java. Fabryka jest "ciężka" (czyta adnotacje, ładuje klasy), więc
    // w teście tworzymy ją RAZ na klasę (@BeforeAll) i zamykamy w @AfterAll (albo try-with-resources).
    //
    // Naruszenie to obiekt z kilkoma informacjami. W testach nie porównuj treści komunikatu (zależy od języka systemu, zob.
    // sekcja 6), tylko: ścieżkę pola, typ adnotacji, odrzuconą wartość — one są stałe.
    //
    //     violation.getPropertyPath()                                  → "price"
    //     violation.getConstraintDescriptor().getAnnotation()          → @DecimalMin (typ: annotationType())
    //     violation.getInvalidValue()                                  → 0.001
    //
    // Walidacja nie zatrzymuje się na pierwszym błędzie: nazwa "  " (dwie spacje) łamie naraz @NotBlank i @Size(min = 3) —
    // to DWA naruszenia tego samego pola. Podobnie cena 0.001 łamie @DecimalMin("0.01") i @Digits.
    //
    // DOBRA PRAKTYKA: łącz ograniczenia na jednym polu (@NotBlank @Size(min = 3, max = 30)), zamiast pisać jedno wielkie
    //   wyrażenie regularne. Dlaczego? Każde ograniczenie ma własny komunikat i własny kod, więc klient dostaje
    //   precyzyjne "za krótkie" lub "puste", a nie bezosobowe "nie pasuje do wzorca".
    //
    // PUŁAPKA: @Email jest bardzo łagodny. Dlaczego? Wzorzec Jakarta Bean Validation akceptuje "a@b" (bez kropki w domenie),
    //   bo taki adres jest technicznie dozwolony w sieci wewnętrznej. Jeśli chcesz wymagać domeny z kropką, dołóż
    //   {@code @Pattern}. A jedyna pewna weryfikacja adresu to wysłanie na niego wiadomości z linkiem.

    // =================================================================================================
    // 3. REKORDY I ZWYKŁE KLASY: GDZIE KŁAŚĆ ADNOTACJĘ (ProductForm, LegacyCustomer)
    // =================================================================================================
    //
    // • REKORD: adnotację piszesz PRZY KOMPONENCIE, w nagłówku rekordu:
    //     public record ProductForm(@NotBlank String name, …) { }
    //   Kompilator "rozdaje" taką adnotację tam, gdzie pozwala jej @Target — a adnotacje Bean Validation mogą stać na
    //   polu, metodzie i parametrze, więc trafia ona na pole, akcesor (metodę name()) i parametr konstruktora.
    //   SPRAWDZONE testem: walidator widzi ograniczenie raz (jedno naruszenie, a nie trzy), a ścieżka to po prostu "name".
    //   Rekord jest niezmienny (immutable), więc walidujesz go PO utworzeniu — konstruktor się wykona nawet z błędnymi
    //   danymi; adnotacje nie "wyrzucą wyjątku w konstruktorze".
    //
    // • ZWYKŁA KLASA: ograniczenie kładziesz na POLU albo na GETTERZE (metodzie getXxx / isXxx). Ścieżka właściwości
    //   zawsze nazywa się jak właściwość: pole "nickname" i getter getNickname() dają to samo "nickname". Wybierz jedno
    //   miejsce w całym projekcie. Pole jest prostsze; getter ma sens, gdy ograniczenie dotyczy wartości wyliczanej.
    //
    // DOBRA PRAKTYKA: adnotacje walidacyjne na obiektach wejściowych (DTO), nie na encjach bazy. Dlaczego? Wejście z
    //   API i zapis do bazy mają różne reguły (np. pole "id" nie wolno podać przy tworzeniu, ale baza je zawiera). DTO
    //   (Rest05CrudApi, sekcja 3) pozwala mieć reguły dopasowane do konkretnej czynności.

    // =================================================================================================
    // 4. ZE SPRINGIEM: GOTOWY BEAN Validator (Val01SpringValidatorTest, Val01Application)
    // =================================================================================================
    //
    // Po dodaniu starter-validation Boot (autokonfiguracja
    // {@code org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration}) tworzy bean
    // {@code defaultValidator} typu {@code LocalValidatorFactoryBean}. To jeden obiekt, który jest naraz:
    //   • {@code jakarta.validation.Validator} — wstrzykujesz go zwykłym konstruktorem i wołasz validate(obiekt);
    //   • {@code org.springframework.validation.Validator} — "wersja Springa", używana przez Spring MVC.
    //
    // W aplikacji webowej Spring MVC ma jeszcze drugi bean, {@code mvcValidator} (ValidatorAdapter), który tylko owija
    // ten sam walidator na potrzeby kontrolerów. SPRAWDZONE testem: wstrzyknięcie {@code jakarta.validation.Validator}
    // daje {@code defaultValidator} — jest dokładnie jeden bean tego typu, więc nie ma niejednoznaczności.
    //
    // Test Val01SpringValidatorTest to @SpringBootTest(classes = Val01Application.class): cały kontekst, bo sprawdzamy
    // właśnie autokonfigurację. (Do samych adnotacji na DTO kontekst NIE jest potrzebny — patrz sekcje 2–3.)
    //
    // DOBRA PRAKTYKA: wstrzykuj Validator tam, gdzie walidujesz RĘCZNIE (np. dane z pliku CSV, z kolejki), a w
    // kontrolerach pozwól to zrobić Springowi przez {@code @Valid} (Val02ValidInControllers). Dlaczego? Mniej kodu
    // i jedno spójne miejsce obsługi błędów (s06_error_handling).

    // =================================================================================================
    // 5. PUŁAPKI (TrapForms; Val01BeanValidationTest, sekcja 5)
    // =================================================================================================
    //
    // PUŁAPKA: {@code @NotNull int quantity}. Dlaczego nic nie robi? Typ prymitywny NIGDY nie jest null: gdy brakuje
    //   wartości, dostajesz 0 (domyślną dla int), i to 0 przechodzi @NotNull. Użyj typu opakowanego {@code Integer},
    //   żeby odróżnić "nie podano" od "podano 0". (W Jacksonie 3 brak prymitywnego pola w JSON też daje już błąd 400 —
    //   wspomniane w Val02ValidInControllers — ale na wejściu z innych źródeł problem zostaje.)
    //
    // PUŁAPKA: {@code @Size} na liczbie. Dlaczego błąd? @Size zna tylko teksty, kolekcje, mapy i tablice. Dla Integer
    //   nie ma walidatora i dostajesz {@code jakarta.validation.UnexpectedTypeException} (HV000030: "No validator could be
    //   found for constraint Size validating type Integer"). Wyjątek leci przy PIERWSZYM użyciu klasy, nawet gdy wartość to
    //   null. Dla liczb użyj @Min/@Max/@Positive; dla zakresu — @Min i @Max razem.
    //
    // PUŁAPKA: {@code @Email} bez {@code @NotNull}. Dlaczego? Null przechodzi, więc formularz bez adresu uchodzi za
    //   poprawny. Pole, które MUSI być, dostaje parę: {@code @NotBlank @Email} (dla tekstu).
    //
    // PUŁAPKA: ograniczenie "dla wygody" w komentarzu zamiast w kodzie. Dlaczego? Walidator czyta wyłącznie adnotacje
    //   i metadane — zdanie "cena większa od zera" nikogo nie powstrzyma.

    // =================================================================================================
    // 6. KOMUNIKATY I JĘZYK (Val01BeanValidationTest, sekcja 6)
    // =================================================================================================
    //
    // Każda adnotacja ma atrybut {@code message}. Domyślnie to KLUCZ w klamrach, np.
    // {@code {jakarta.validation.constraints.NotBlank.message}}, który Hibernate Validator zamienia na tekst w języku
    // bieżącego Locale (ustawień regionalnych): angielskie "must not be blank" albo polskie tłumaczenie z pakietu zasobów
    // Hibernate Validatora. Dlatego te same dane dadzą RÓŻNY tekst na komputerze z polskim i angielskim systemem.
    // W testach: albo ustaw Locale jawnie (test sekcji 6: fabryka z {@code defaultLocale(…)}), albo — lepiej — porównuj
    // typ adnotacji i ścieżkę.
    //
    // Własny komunikat: {@code @Size(min = 3, message = "Nazwa musi mieć co najmniej {min} znaki")}. W szablonie:
    //   • {@code {min}}, {@code {max}}, {@code {regexp}} — wartość atrybutu adnotacji (sprawdzone: "{regexp}" w
    //     CustomerForm.phone);
    //   • {@code ${validatedValue}} — odrzucona wartość (wymaga Expression Language, stąd w starterze Tomcat Embed EL);
    //   • {@code {klucz}} — odwołanie do pliku zasobów z tłumaczeniami.
    //
    // Plik zasobów: Hibernate Validator domyślnie szuka pliku {@code ValidationMessages.properties} (oraz
    // {@code ValidationMessages_pl.properties} itd.) w KORZENIU classpath. W tej lekcji używamy dla czytelności własnego
    // pliku {@code val01/Val01Messages.properties} podpiętego przez {@code ResourceBundleMessageInterpolator} (interpolator =
    // "wstawiacz" wartości w szablon) — w prawdziwym projekcie wystarczy domyślna nazwa. Plik czytany jest jako UTF-8
    // (Java 9+), więc polskie litery piszesz wprost. SPRAWDZONE testem: komunikat z "{val01.name.short}" zawiera "zażółć gęślą".
    //
    // DOBRA PRAKTYKA: komunikaty w pliku zasobów, a nie rozsiane po adnotacjach. Dlaczego? Jedno miejsce do tłumaczenia
    //   i poprawiania; ten sam komunikat ("Pole obowiązkowe") używasz w wielu klasach. W API zwracaj klientowi raczej
    //   KOD ograniczenia (np. "NotBlank") i nazwę pola, a tekst niech zbuduje aplikacja kliencka w swoim języku.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Pakiet: {@code jakarta.validation.constraints.*} (NIE javax). Specyfikacja = Jakarta Bean Validation 3.1,
     *     implementacja = Hibernate Validator 9.x; starter: spring-boot-starter-validation.
     *   • Tekst: @NotBlank (null, "", "  " → błąd); @NotEmpty (null, "" → błąd); @NotNull (tylko null). @Size = długość.
     *   • Liczby: @Min/@Max (całkowite), @Positive, @PositiveOrZero, @DecimalMin("0.01") (BigDecimal), @Digits.
     *   • Inne: @Email (łagodny!), @Pattern (cały tekst), @Past/@Future. Prawie wszystko przepuszcza null.
     *   • Validator.validate(obiekt) → Set naruszeń; pusty = OK. Naruszenie: ścieżka, adnotacja, wartość, komunikat.
     *   • Rekord: adnotacja przy komponencie. Klasa: na polu albo getterze. Fabrykę zamykaj (try-with-resources / @AfterAll).
     *   • Spring Boot daje bean defaultValidator (LocalValidatorFactoryBean) — wstrzykuj jakarta.validation.Validator.
     *   • Komunikat zależy od Locale; własny: message = "… {min} …" lub klucz {…} w ValidationMessages.properties (UTF-8).
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się Jakarta Bean Validation od Hibernate Validator? Który pakiet importujesz w swoim kodzie?
     *   2. Co się stanie? Obiekt {@code PrimitiveQuantity(@NotNull int quantity)} z wartością 0 — ile będzie naruszeń?
     *   3. Co się stanie? {@code @Size(min = 1, max = 5) Integer quantity} — wywołujesz validate() z wartością 3. Jaki
     *      wyjątek i dlaczego? A z wartością null?
     *   4. Co się stanie? {@code EmailOnly(@Email String email)} dla null oraz dla "a@b" — ile naruszeń w każdym przypadku?
     *   5. ZNAJDŹ BŁĄD: {@code import javax.validation.constraints.NotBlank;} w projekcie Spring Boot 4. Co jest nie tak?
     *   6. Które z @NotNull, @NotEmpty, @NotBlank odrzucą tekst z samych spacji? Czy @NotBlank zadziała na Integer?
     *   7. Co się stanie? {@code ProductForm} z nazwą "  " (dwie spacje) — ile naruszeń dostaje pole name i jakie?
     *   8. Dlaczego w teście nie porównujemy tekstu komunikatu z "must not be blank"? Co porównujemy zamiast tego?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================
    //
    // Szkielet: ValidationExercises (metody z TODO), testy: Val01BeanValidationExercisesTest (@Tag("cwiczenie")).
    // Rozwiązania wzorcowe: s05_validation.solutions.val01_bean_validation.ValidationExercisesSolutions.
    // Do dyspozycji masz gotowe formularze ProductForm i CustomerForm oraz Validator z parametru.
    //
    // ĆWICZENIE 1 (łatwe): isValid — true, gdy obiekt nie łamie żadnej reguły.
    //   Podpowiedź: validator.validate(object) zwraca Set; pusty zbiór = poprawny.
    //
    // ĆWICZENIE 2 (łatwe): invalidFields — posortowana lista ścieżek pól z błędami, bez powtórzeń (pole z dwoma błędami
    //   ma być na liście raz). Podpowiedź: violation.getPropertyPath().toString(), distinct(), sorted().
    //
    // ĆWICZENIE 3 (średnie): constraintsByField — mapa pole → posortowana lista nazw złamanych adnotacji
    //   (np. "name" → ["NotBlank", "Size"]). Klucze posortowane (TreeMap).
    //   Podpowiedź: violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName().
    //
    // ĆWICZENIE 4 (średnie): isPriceAllowed — czy sama cena jest poprawna w ProductForm, bez budowania formularza.
    //   Podpowiedź: validator.validateValue(ProductForm.class, "price", price) — sprawdza jedną właściwość klasy.
    //
    // ĆWICZENIE 5 (trudniejsze): report — posortowane linie "pole: Nazwa (wartość)", np. "quantity: Min (0)".
    //   Podpowiedź: złóż tekst z getPropertyPath(), nazwy adnotacji i getInvalidValue(); String.valueOf zamieni null
    //   na "null".

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Jakarta Bean Validation to specyfikacja (adnotacje i interfejsy), Hibernate Validator — jej implementacja,
     *      czyli kod, który adnotacje sprawdza. W swoim kodzie importujesz {@code jakarta.validation.*} (specyfikacja);
     *      do {@code org.hibernate.validator.*} sięgasz rzadko, tylko po rozszerzenia.
     *   2. Zero naruszeń. int nigdy nie jest null (brak danych = 0), więc @NotNull na prymitywie nic nie daje.
     *      Użyj Integer. Sprawdzone w Val01BeanValidationTest (sekcja 5).
     *   3. {@code jakarta.validation.UnexpectedTypeException} (HV000030: nie ma walidatora dla @Size i typu Integer).
     *      Z wartością null jest TAK SAMO — wyjątek leci przy budowaniu metadanych klasy, więc nie zależy od wartości.
     *      Sprawdzone w teście sekcji 5.
     *   4. W obu przypadkach 0 naruszeń: @Email przepuszcza null, a "a@b" ma poprawny kształt lokalna@domena (kropka w
     *      domenie nie jest wymagana). Sprawdzone w teście sekcji 5; dla wymagalności dodaj @NotBlank.
     *   5. Pakiet javax.validation został zastąpiony przez jakarta.validation (od Boot 3); w projekcie nie ma tej
     *      biblioteki, więc kod się nie skompiluje, a sztucznie dodana adnotacja javax byłaby przez walidator
     *      zignorowana. Poprawnie: {@code import jakarta.validation.constraints.NotBlank;}.
     *   6. Samych spacji nie przepuści tylko @NotBlank (@NotEmpty przepuści, bo tekst nie jest pusty; @NotNull tym
     *      bardziej). @NotBlank na Integer: nie zadziała — wyjątek UnexpectedTypeException, bo działa tylko na
     *      tekstach (CharSequence). Sprawdzone w teście sekcji 2 (tabela).
     *   7. Dwa naruszenia: NotBlank (tekst jest pusty/białe znaki) i Size (długość 2 jest mniejsza niż 3). Walidacja
     *      nie zatrzymuje się na pierwszym błędzie. Sprawdzone w teście sekcji 2.
     *   8. Treść komunikatu zależy od Locale (na polskim systemie byłaby po polsku), więc test zależałby od komputera.
     *      Porównujemy rzeczy stałe: ścieżkę pola, typ adnotacji i odrzuconą wartość.
     */
    // </editor-fold>
}
