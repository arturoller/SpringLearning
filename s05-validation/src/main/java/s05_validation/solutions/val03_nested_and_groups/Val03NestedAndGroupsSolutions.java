package s05_validation.solutions.val03_nested_and_groups;

import jakarta.validation.Validator;

import java.util.List;

/** Rozwiązanie wzorcowe ćwiczenia 1 lekcji Val03NestedAndGroups — zajrzyj dopiero po własnej próbie! */
public final class Val03NestedAndGroupsSolutions {

    private Val03NestedAndGroupsSolutions() {
    }

    public static List<String> paths(Validator validator, Object object, Class<?>... groups) {
        return validator.validate(object, groups).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .sorted()
                .toList();
    }
}
