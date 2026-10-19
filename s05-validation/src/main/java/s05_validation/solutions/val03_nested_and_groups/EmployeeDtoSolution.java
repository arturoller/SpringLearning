package s05_validation.solutions.val03_nested_and_groups;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;
import s05_validation.val03_nested_and_groups.OnCreate;
import s05_validation.val03_nested_and_groups.OnUpdate;

import java.math.BigDecimal;

/** Rozwiązanie wzorcowe ćwiczenia 3 lekcji Val03NestedAndGroups (EmployeeDto z grupami). */
public record EmployeeDtoSolution(
        @Null(groups = OnCreate.class) @NotNull(groups = OnUpdate.class) Long id,
        @NotBlank String name,                                  // bez groups = grupa Default (wchodzi do OnCreate i OnUpdate)
        @NotNull(groups = OnUpdate.class) @Positive BigDecimal salary) {   // @Positive przepuszcza null przy tworzeniu
}
