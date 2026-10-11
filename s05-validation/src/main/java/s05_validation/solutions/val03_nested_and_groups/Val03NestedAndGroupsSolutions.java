package s05_validation.solutions.val03_nested_and_groups;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;
import s05_validation.val03_nested_and_groups.Val03Types.OnCreate;
import s05_validation.val03_nested_and_groups.Val03Types.OnUpdate;

import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Val03NestedAndGroups (test: Val03NestedAndGroupsSolutionsTest). */
public final class Val03NestedAndGroupsSolutions {

    private Val03NestedAndGroupsSolutions() {
    }

    public record Address(@NotBlank String city) {
    }

    /** Rozwiązanie 1: obowiązkowy adres to @NotNull + @Valid (null nie wchodzi do kaskady, dlatego osobno @NotNull). */
    public record Shipment(@NotBlank String receiver, @NotNull @Valid Address address) {
    }

    public record Line(@NotBlank String sku, @Positive int qty) {
    }

    /** Rozwiązanie 2: adnotacje elementów w nawiasach ostrych; @NotNull łapie null, bo @Valid go pomija. */
    public record Basket(
            @NotEmpty List<@NotNull @Valid Line> lines,
            List<@NotBlank String> coupons) {
    }

    /** Rozwiązanie 3: reguły z grupami. */
    public record Customer(
            @Null(groups = OnCreate.class) @NotNull(groups = OnUpdate.class) Long id,
            @NotBlank(groups = {OnCreate.class, OnUpdate.class}) String email) {
    }

    public static List<String> solution3(Validator validator, Customer customer, boolean creating) {
        Class<?> group = creating ? OnCreate.class : OnUpdate.class;
        return validator.validate(customer, group).stream()
                .map((ConstraintViolation<Customer> v) -> v.getPropertyPath().toString())
                .sorted()
                .toList();
    }
}
