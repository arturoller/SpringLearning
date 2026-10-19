package s14_openapi.api03_groups_and_versions;

import org.springdoc.core.filters.OpenApiMethodFilter;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;

/**
 * Filtr metod dla grupy dokumentu „wersja N API”: zostawia w grupie tylko te metody, które NAPRAWDĘ obsłużą zapytanie
 * z wersją N. Zasada Spring Framework 7 dla wersji bazowej "K+": metoda obsługuje wersje od K wzwyż, ale gdy dla tego
 * samego adresu jest metoda z wyższą wersją bazową (nie wyższą niż N) — ona wygrywa. Tu dla prostoty porównujemy tylko
 * numer główny wersji (1, 2…) i tylko metody {@code @GetMapping}.
 */
public final class VersionGroupFilter implements OpenApiMethodFilter {

    private final int major;

    public VersionGroupFilter(int major) {
        this.major = major;
    }

    @Override
    public boolean isMethodToInclude(Method method) {
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        if (mapping == null || mapping.version().isEmpty()) {
            return true;   // metoda bez wersji (albo nie-GET) — dostępna w każdej wersji
        }
        int baseline = baselineOf(mapping);
        if (baseline > major) {
            return false;  // wersja bazowa wyższa niż ta grupa — metoda jeszcze nie istnieje
        }
        // wygrywa metoda o NAJWYŻSZEJ wersji bazowej <= major dla tego samego adresu
        for (Method other : method.getDeclaringClass().getDeclaredMethods()) {
            GetMapping otherMapping = other.getAnnotation(GetMapping.class);
            if (otherMapping == null || other.equals(method) || otherMapping.version().isEmpty()
                    || !java.util.Arrays.equals(otherMapping.path(), mapping.path())) {
                continue;
            }
            int otherBaseline = baselineOf(otherMapping);
            if (otherBaseline <= major && otherBaseline > baseline) {
                return false;
            }
        }
        return true;
    }

    /** "2.0+" → 2; "1.0" → 1 (tylko numer główny). */
    private static int baselineOf(GetMapping mapping) {
        String version = mapping.version();
        return Integer.parseInt(version.split("[.+]")[0]);
    }
}
