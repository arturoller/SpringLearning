package s05_validation.val03_nested_and_groups;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.groups.Default;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Val03NestedAndGroups bez Springa: zwykły {@code Validator} (Val01BeanValidation). Zagnieżdżenia, listy i grupy
 * to funkcje Bean Validation, nie Springa — dlatego najtańszy test wystarczy. Wersje z kontrolerem są w
 * Val03NestedAndGroupsControllerTest.
 */
class Val03NestedAndGroupsTest {

    static ValidatorFactory factory;
    static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        factory.close();
    }

    /** Naruszenia jako posortowane napisy "ścieżka:Adnotacja". */
    static List<String> described(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(v -> v.getPropertyPath() + ":" + v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())
                .sorted()
                .toList();
    }

    static final Address BAD_ADDRESS = new Address("Długa 1", " ", "80-001");
    static final Address GOOD_ADDRESS = new Address("Długa 1", "Gdańsk", "80-001");
    static final OrderLine GOOD_LINE = new OrderLine("Kawa", 2);

    // ---------------------------------------------------------------- 1

    @Test
    @DisplayName("Sekcja 1 (PUŁAPKA): bez @Valid na polu błędy wnętrza adresu NIE są wykrywane")
    void withoutValidNestedObjectIsNotChecked() {
        assertThat(validator.validate(new UncheckedNesting.Forgot(BAD_ADDRESS, List.of(GOOD_LINE)))).isEmpty();
    }

    @Test
    @DisplayName("Sekcja 1: z @Valid błąd adresu jest wykrywany, a ścieżka to address.city")
    void withValidNestedObjectIsChecked() {
        assertThat(described(validator.validate(new UncheckedNesting.Cascade(BAD_ADDRESS, List.of(GOOD_LINE)))))
                .containsExactly("address.city:NotBlank");
        // @NotNull sprawdza samo istnienie adresu, @Valid jego wnętrze
        assertThat(described(validator.validate(new UncheckedNesting.Cascade(null, List.of(GOOD_LINE)))))
                .containsExactly("address:NotNull");
    }

    @Test
    @DisplayName("Sekcja 1: @Valid schodzi na każdy poziom — ścieżki mają tyle członów, ile poziomów zagnieżdżenia")
    void pathsFollowTheNesting() {
        Address allBad = new Address("", "", "1");
        assertThat(described(validator.validate(new UncheckedNesting.Cascade(allBad, null))))
                .containsExactly("address.city:NotBlank", "address.postalCode:Pattern", "address.street:NotBlank");
    }

    // ---------------------------------------------------------------- 2

    @Test
    @DisplayName("Sekcja 2: błędy w elementach listy mają ścieżkę z indeksem — lines[1].quantity")
    void collectionElementPaths() {
        UncheckedNesting.Cascade order = new UncheckedNesting.Cascade(GOOD_ADDRESS,
                List.of(GOOD_LINE, new OrderLine("", 0)));

        assertThat(described(validator.validate(order)))
                .containsExactly("lines[1].productName:NotBlank", "lines[1].quantity:Min");
    }

    @Test
    @DisplayName("Sekcja 2: element null na liście przechodzi, dopóki nie dasz mu @NotNull")
    void nullElementsInList() {
        OrderRequest withNullLine = new OrderRequest(null, GOOD_ADDRESS, java.util.Arrays.asList(GOOD_LINE, null), null);

        assertThat(described(validator.validate(withNullLine))).containsExactly("lines[1].<list element>:NotNull");
        // @Valid pomija null: bez @NotNull na elemencie lista [GOOD_LINE, null] byłaby poprawna
    }

    @Test
    @DisplayName("Sekcja 2: @NotEmpty odrzuca pustą listę, a List<@NotBlank String> sprawdza każdy tekst")
    void notEmptyAndElementConstraints() {
        OrderRequest empty = new OrderRequest(null, GOOD_ADDRESS, List.of(), null);
        assertThat(described(validator.validate(empty))).containsExactly("lines:NotEmpty");

        OrderRequest badTags = new OrderRequest(null, GOOD_ADDRESS, List.of(GOOD_LINE), List.of("pilne", " ", ""));
        assertThat(described(validator.validate(badTags)))
                .containsExactly("tags[1].<list element>:NotBlank", "tags[2].<list element>:NotBlank");

        // tags jest opcjonalne: null (brak listy) nie łamie nic
        assertThat(validator.validate(new OrderRequest(null, GOOD_ADDRESS, List.of(GOOD_LINE), null))).isEmpty();
    }

    // ---------------------------------------------------------------- 3

    @Test
    @DisplayName("Sekcja 3: grupa Default, OnCreate i OnUpdate widzą różne reguły dla id")
    void groupsSelectRules() {
        OrderRequest withId = new OrderRequest(5L, GOOD_ADDRESS, List.of(GOOD_LINE), null);
        OrderRequest withoutId = new OrderRequest(null, GOOD_ADDRESS, List.of(GOOD_LINE), null);

        // Default: reguły z groups = OnCreate/OnUpdate NIE są sprawdzane
        assertThat(validator.validate(withId, Default.class)).isEmpty();
        assertThat(validator.validate(withoutId, Default.class)).isEmpty();
        // OnCreate: id musi być null
        assertThat(described(validator.validate(withId, OnCreate.class))).containsExactly("id:Null");
        assertThat(validator.validate(withoutId, OnCreate.class)).isEmpty();
        // OnUpdate: id musi być podany
        assertThat(described(validator.validate(withoutId, OnUpdate.class))).containsExactly("id:NotNull");
        assertThat(validator.validate(withId, OnUpdate.class)).isEmpty();
    }

    @Test
    @DisplayName("Sekcja 3: OnCreate rozszerza Default, więc reguły bez grupy (adres) też są sprawdzane")
    void groupExtendingDefaultKeepsDefaultRules() {
        OrderRequest badAddress = new OrderRequest(null, BAD_ADDRESS, List.of(GOOD_LINE), null);

        assertThat(described(validator.validate(badAddress, OnCreate.class))).containsExactly("address.city:NotBlank");
        assertThat(described(validator.validate(badAddress, OnUpdate.class)))
                .containsExactly("address.city:NotBlank", "id:NotNull");
    }

    @Test
    @DisplayName("Sekcja 3 (PUŁAPKA): grupa, która NIE rozszerza Default, pomija wszystkie reguły bez grupy")
    void groupNotExtendingDefaultSkipsUngroupedRules() {
        PlainGroupsForm form = new PlainGroupsForm(5L, "");   // id ma być null, name nie może być puste

        assertThat(described(validator.validate(form, PlainGroupsForm.Create.class))).containsExactly("id:Null");   // name pominięte!
        assertThat(described(validator.validate(form, Default.class))).containsExactly("name:NotBlank");           // id pominięte
        assertThat(described(validator.validate(form, PlainGroupsForm.Create.class, Default.class)))
                .containsExactly("id:Null", "name:NotBlank");   // można podać kilka grup naraz
        assertThat(described(validator.validate(form))).containsExactly("name:NotBlank");   // bez grupy = Default
    }

    // ---------------------------------------------------------------- 4

    @Test
    @DisplayName("Sekcja 4: bez sekwencji wszystkie grupy działają naraz i dają wszystkie błędy")
    void withoutSequenceAllGroupsRun() {
        CouponForm form = new CouponForm("", 99);

        assertThat(described(validator.validate(form, CouponChecks.Basic.class, CouponChecks.Advanced.class)))
                .containsExactly("code:NotBlank", "code:Pattern", "discountPercent:Max");
    }

    @Test
    @DisplayName("Sekcja 4: @GroupSequence — gdy tanie sprawdzenia zawiodą, drogie w ogóle się nie uruchamiają")
    void sequenceStopsAfterFirstFailingGroup() {
        // kod pusty → grupa Basic ma błąd → Advanced (Pattern, Max) jest pominięta, mimo że zniżka 99 jest za duża
        assertThat(described(validator.validate(new CouponForm("", 99), CouponChecks.InOrder.class)))
                .containsExactly("code:NotBlank");
        // Basic przechodzi → dopiero teraz sprawdzana jest grupa Advanced
        assertThat(described(validator.validate(new CouponForm("zly", 99), CouponChecks.InOrder.class)))
                .containsExactly("code:Pattern", "discountPercent:Max");
        assertThat(validator.validate(new CouponForm("ABC-1234", 50), CouponChecks.InOrder.class)).isEmpty();
        assertThat(Arrays.asList(CouponChecks.InOrder.class.getAnnotation(jakarta.validation.GroupSequence.class).value()))
                .containsExactly(CouponChecks.Basic.class, CouponChecks.Advanced.class);
    }
}
