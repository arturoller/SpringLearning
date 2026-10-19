package s20_modulith.solutions.mod01_modules;

import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

import java.util.List;

/**
 * Rozwiązania wzorcowe ćwiczeń z lekcji Mod01Modules. Pakiet {@code solutions} leży POZA pakietem głównym
 * {@code s20_modulith.mod01_modules}, więc Spring Modulith nie traktuje go jako modułu aplikacji lekcji.
 */
public final class Mod01ModulesSolutions {

    private Mod01ModulesSolutions() {
    }

    /** ĆWICZENIE 1: nazwy wszystkich modułów, alfabetycznie. */
    public static List<String> solution1(ApplicationModules modules) {
        return modules.stream()
                .map(module -> module.getIdentifier().toString())
                .sorted()
                .toList();
    }

    /** ĆWICZENIE 2: czy architektura jest poprawna — bez wyjątku (detectViolations zamiast verify). */
    public static boolean solution2(ApplicationModules modules) {
        return !modules.detectViolations().hasViolations();
    }

    /** ĆWICZENIE 3: czy typ należy do API swojego modułu; typ spoza modułów → false. */
    public static boolean solution3(ApplicationModules modules, Class<?> type) {
        return modules.getModuleByType(type)
                .map(module -> module.isExposed(type))
                .orElse(false);
    }

    /** ĆWICZENIE 4: posortowane nazwy modułów, od których bezpośrednio zależy podany moduł. */
    public static List<String> solution4(ApplicationModules modules, String moduleName) {
        ApplicationModule module = modules.getModuleByName(moduleName)
                .orElseThrow(() -> new IllegalArgumentException("Nie ma modułu: " + moduleName));
        return module.getDirectDependencies(modules)
                .uniqueModules()
                .map(dependency -> dependency.getIdentifier().toString())
                .sorted()
                .toList();
    }
}
