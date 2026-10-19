package s03_boot_basics.boot04_starters;

import org.springframework.util.ClassUtils;

import java.util.Comparator;
import java.util.List;

/**
 * „Czy technologia jest na ścieżce klas?” — tak samo pyta {@code @ConditionalOnClass}: szuka jednej charakterystycznej
 * klasy (indicator class = klasa-wskaźnik). Rekord łączy technologię, jej klasę-wskaźnik i starter Boota 4,
 * który ją dociąga. Nazwy klas sprawdzone w jarach z lokalnego repozytorium Mavena.
 *
 * @param technology     nazwa technologii (po polsku)
 * @param indicatorClass pełna nazwa klasy, która jest na ścieżce klas tylko wtedy, gdy technologię dołączono
 * @param starter        starter Boota 4, który ją dociąga
 */
public record TechnologyCheck(String technology, String indicatorClass, String starter) {

    /** Znane technologie — część z nich jest w tym module, część celowo nie (każdy dział ma tylko swoje startery). */
    public static final List<TechnologyCheck> KNOWN = List.of(
            new TechnologyCheck("JSON (Jackson 3)", "tools.jackson.databind.json.JsonMapper", "spring-boot-starter-jackson"),
            new TechnologyCheck("Logowanie (Logback)", "ch.qos.logback.classic.Logger", "spring-boot-starter"),
            new TechnologyCheck("YAML", "org.yaml.snakeyaml.Yaml", "spring-boot-starter"),
            new TechnologyCheck("Web MVC (serwlety)", "org.springframework.web.servlet.DispatcherServlet", "spring-boot-starter-webmvc"),
            new TechnologyCheck("JDBC", "org.springframework.jdbc.core.simple.JdbcClient", "spring-boot-starter-jdbc"),
            new TechnologyCheck("JPA", "jakarta.persistence.EntityManager", "spring-boot-starter-data-jpa"),
            new TechnologyCheck("Walidacja", "jakarta.validation.Validator", "spring-boot-starter-validation"),
            new TechnologyCheck("Migracje Flyway", "org.flywaydb.core.Flyway", "spring-boot-starter-flyway"),
            new TechnologyCheck("AOP (AspectJ)", "org.aspectj.weaver.Advice", "spring-boot-starter-aspectj"));

    /** Czy klasa-wskaźnik jest na ścieżce klas? */
    public boolean isPresent() {
        return ClassUtils.isPresent(indicatorClass, TechnologyCheck.class.getClassLoader());
    }

    /** Technologie obecne na ścieżce klas tego modułu, posortowane po nazwie. */
    public static List<String> presentTechnologies() {
        return KNOWN.stream().filter(TechnologyCheck::isPresent).map(TechnologyCheck::technology)
                .sorted(Comparator.naturalOrder()).toList();
    }
}
