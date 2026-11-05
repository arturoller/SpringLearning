package s05_validation.solutions.val03_nested_and_groups;

import jakarta.validation.GroupSequence;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import s05_validation.val03_nested_and_groups.CouponChecks;
import s05_validation.val03_nested_and_groups.CouponForm;
import s05_validation.val03_nested_and_groups.OnCreate;
import s05_validation.val03_nested_and_groups.OnUpdate;

import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Val03NestedAndGroups — zajrzyj dopiero po własnej próbie! */
@RestController
@RequestMapping("/val03/cw/shipments")
public class Val03SolutionsController {

    public record AddressDto(@NotBlank String street, @NotBlank String city) {
    }

    public record ShipmentRequest(
            @Null(groups = OnCreate.class) @NotNull(groups = OnUpdate.class) Long id,
            @NotBlank String recipient,
            @NotNull @Valid AddressDto address,
            @NotEmpty List<@NotBlank String> parcelCodes) {
    }

    @PostMapping
    public ResponseEntity<ShipmentRequest> create(@Validated(OnCreate.class) @RequestBody ShipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    @PutMapping
    public ResponseEntity<ShipmentRequest> update(@Validated(OnUpdate.class) @RequestBody ShipmentRequest request) {
        return ResponseEntity.ok(request);
    }

    public static List<String> fieldPaths(Validator validator, Object object, Class<?>... groups) {
        return validator.validate(object, groups).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .distinct()
                .sorted()
                .toList();
    }

    @GroupSequence({CouponChecks.Basic.class, CouponChecks.Advanced.class})
    public interface Checks {
    }

    public static List<String> validateInOrder(Validator validator, CouponForm form) {
        return validator.validate(form, Checks.class).stream()
                .map(v -> v.getPropertyPath() + ":" + v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())
                .sorted()
                .toList();
    }
}
