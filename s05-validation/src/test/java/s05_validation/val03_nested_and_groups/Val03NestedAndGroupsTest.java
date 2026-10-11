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
import s05_validation.val03_nested_and_groups.Val03Types.Address;
import s05_validation.val03_nested_and_groups.Val03Types.LooseOrder;
import s05_validation.val03_nested_and_groups.Val03Types.MixedDto;
import s05_validation.val03_nested_and_groups.Val03Types.NullItemsOrder;
import s05_validation.val03_nested_and_groups.Val03Types.OldStyleOrder;
import s05_validation.val03_nested_and_groups.Val03Types.OnCreate;
import s05_validation.val03_nested_and_groups.Val03Types.OnUpdate;
import s05_validation.val03_nested_and_groups.Val03Types.OrderItem;
import s05_validation.val03_nested_and_groups.Val03Types.OrderRequest;
import s05_validation.val03_nested_and_groups.Val03Types.ProductDto;
import s05_validation.val03_nested_and_groups.Val03Types.TagList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Val03NestedAndGroups — zwykłe testy jednostkowe z samym Validatorem (bez Springa). Kontroler sprawdza
 * Val03NestedAndGroupsWebMvcTest.
 */
class Val03NestedAndGroupsTest {

    static ValidatorFactory factory;
    static Validator validator;

    @BeforeAll
    static void create() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    /** Ścieżki naruszeń, posortowane (Set nie ma kolejności). */
    static List<String> paths(Set<? extends ConstraintViolation<?>> violations) {
        List<String> result = new ArrayList<>();
        violations.forEach(v -> result.add(v.getPropertyPath().toString()));
        result.sort(String::compareTo);
        return result;
    }

    @Test
    @DisplayName("1. @Valid schodzi do obiektu zagnieżdżonego (ścieżka address.xxx); bez @Valid reguły adresu są martwe")
    void nestedObjects() {
        OrderRequest order = new OrderRequest("Ala", new Address("", "123"), List.of(new OrderItem("A-1", 1)));
        assertThat(paths(validator.validate(order))).containsExactly("address.postalCode", "address.street");

        // adres obowiązkowy: @NotNull; null nie wchodzi do kaskady
        assertThat(paths(validator.validate(new OrderRequest("Ala", null, List.of(new OrderItem("A-1", 1))))))
                .containsExactly("address");

        // PUŁAPKA: brak @Valid — ten sam zły adres przechodzi
        assertThat(validator.validate(new LooseOrder("Ala", new Address("", "bad")))).isEmpty();
    }

    @Test
    @DisplayName("2. Elementy listy: ścieżka z indeksem (items[1].quantity); @Valid pomija null, łapie go dopiero @NotNull")
    void listElements() {
        Address ok = new Address("Długa 1", "00-001");
        OrderRequest order = new OrderRequest("Ala", ok, List.of(new OrderItem("A-1", 1), new OrderItem("", 0)));
        assertThat(paths(validator.validate(order))).containsExactly("items[1].quantity", "items[1].sku");

        // starszy zapis @Valid na polu z listą kaskaduje tak samo
        assertThat(paths(validator.validate(new OldStyleOrder(List.of(new OrderItem("A-1", 1), new OrderItem("", 1))))))
                .containsExactly("items[1].sku");

        // element null: List<@Valid OrderItem> przepuszcza, List<@NotNull @Valid OrderItem> zgłasza
        assertThat(validator.validate(new NullItemsOrder(Arrays.asList(new OrderItem("A-1", 1), null)))).isEmpty();
        assertThat(paths(validator.validate(new OrderRequest("Ala", ok, Arrays.asList(new OrderItem("A-1", 1), null)))))
                .containsExactly("items[1].<list element>");
    }

    @Test
    @DisplayName("3. Reguły elementów kontenera: List<@NotBlank String> sprawdza każdy tag osobno")
    void containerElements() {
        TagList tags = new TagList(List.of("ok", " ", "bardzodlugitagpowyzej"));
        assertThat(paths(validator.validate(tags)))
                .containsExactly("tags[1].<list element>", "tags[2].<list element>");
        assertThat(validator.validate(new TagList(List.of("tani", "nowy")))).isEmpty();
        assertThat(paths(validator.validate(new TagList(List.of())))).containsExactly("tags");   // @NotEmpty dotyczy listy

        // adnotacja tekstowa przy POLU z listą (zamiast w nawiasach przy elemencie) → UnexpectedTypeException
        record WrongPlace(@jakarta.validation.constraints.NotBlank List<String> tags) {
        }
        assertThatThrownBy(() -> validator.validate(new WrongPlace(List.of("a"))))
                .isInstanceOf(jakarta.validation.UnexpectedTypeException.class);
    }

    @Test
    @DisplayName("4. Grupy: OnCreate wymaga pustego id, OnUpdate — id; grupa Default nie sprawdza reguł z groups")
    void groups() {
        assertThat(paths(validator.validate(new ProductDto(5L, "Mleko"), OnCreate.class))).containsExactly("id");
        assertThat(validator.validate(new ProductDto(null, "Mleko"), OnCreate.class)).isEmpty();

        assertThat(paths(validator.validate(new ProductDto(null, "Mleko"), OnUpdate.class))).containsExactly("id");
        assertThat(validator.validate(new ProductDto(5L, "Mleko"), OnUpdate.class)).isEmpty();

        // nazwa obowiązkowa w obu grupach
        assertThat(paths(validator.validate(new ProductDto(null, " "), OnCreate.class))).containsExactly("name");
        assertThat(paths(validator.validate(new ProductDto(5L, " "), OnUpdate.class))).containsExactly("name");

        // grupa Default (domyślna) nie widzi reguł przypisanych do OnCreate/OnUpdate
        assertThat(validator.validate(new ProductDto(5L, " "))).isEmpty();
    }

    @Test
    @DisplayName("4b. Pułapka Default: grupa OnCreate pomija reguły bez grupy — trzeba podać też Default.class")
    void groupsAndDefault() {
        MixedDto dto = new MixedDto(5L, "");
        assertThat(paths(validator.validate(dto, OnCreate.class))).containsExactly("id");          // nazwa przepuszczona!
        assertThat(paths(validator.validate(dto))).containsExactly("name");                        // sama Default: bez id
        assertThat(paths(validator.validate(dto, Default.class, OnCreate.class))).containsExactly("id", "name");
    }
}
