package s05_validation.val01_bean_validation;

import java.math.BigDecimal;

/**
 * EmployeeForm = formularz pracownika — SZKIELET do ćwiczenia 2 lekcji Val01BeanValidation.
 * TODO: dopisz adnotacje Bean Validation: name — niepuste i nie same spacje; salary — podane i większe od zera;
 * email — podany i w poprawnym formacie. Rozwiązanie wzorcowe: solutions.val01_bean_validation.
 */
public record EmployeeForm(
        String name,
        BigDecimal salary,
        String email) {
}
