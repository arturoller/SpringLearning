package s12_security.solutions.sec02_authorization;

import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;
import java.util.NoSuchElementException;

/** Rozwiązanie wzorcowe ćwiczenia 4 lekcji Sec02Authorization (ExerciseSalaryService). */
public class SolutionSalaryService {

    private final Map<String, Integer> salaries = Map.of("anna", 14500, "piotr", 9800, "kasia", 7200, "tomasz", 8900);

    /** HR widzi każdą pensję; każdy pracownik — swoją (#employee = parametr metody). */
    @PreAuthorize("hasRole('HR') or #employee == authentication.name")
    public int salaryOf(String employee) {
        Integer salary = salaries.get(employee);
        if (salary == null) {
            throw new NoSuchElementException("Nie ma pracownika " + employee);
        }
        return salary;
    }
}
