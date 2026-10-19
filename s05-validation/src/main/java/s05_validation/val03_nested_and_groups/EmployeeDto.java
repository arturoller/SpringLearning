package s05_validation.val03_nested_and_groups;

import java.math.BigDecimal;

/**
 * EmployeeDto = pracownik — SZKIELET do ćwiczenia 3 lekcji Val03NestedAndGroups.
 * TODO (grupy OnCreate i OnUpdate już istnieją): id — przy tworzeniu musi być puste, przy aktualizacji wymagane;
 * name — zawsze niepuste i nie same spacje (Default); salary — przy aktualizacji wymagane, a ZAWSZE (gdy podane)
 * większe od zera. Rozwiązanie wzorcowe: solutions.val03_nested_and_groups.
 */
public record EmployeeDto(
        Long id,
        String name,
        BigDecimal salary) {
}
