package s00_start.start07_glossary;

import java.util.List;

/**
 * Tabela terminologii kursu — jednolite polskie odpowiedniki, których używają WSZYSTKIE lekcje.
 *
 * <p>Każda reguła mówi: jak nazywamy pojęcie ({@code preferred}) i jakich kalek językowych NIE używamy
 * ({@code avoid}). Dzięki temu cały kurs brzmi spójnie, a Ty widzisz w dokumentacji i na forach, że te same
 * pojęcia bywają tłumaczone różnie — i wiesz, że to to samo.</p>
 */
public final class CourseTerminology {

    /**
     * Jedna reguła.
     *
     * @param english   nazwa angielska
     * @param preferred polski odpowiednik używany w kursie
     * @param avoid     formy, których unikamy (kalki, błędne tłumaczenia); może być pusta
     */
    public record Rule(String english, String preferred, List<String> avoid) {
    }

    public static final List<Rule> RULES = List.of(
            new Rule("bean", "bean (ziarno)", List.of("fasola", "fasolka")),
            new Rule("container", "kontener", List.of()),
            new Rule("application context", "kontekst aplikacji", List.of()),
            new Rule("dependency injection", "wstrzykiwanie zależności", List.of("zastrzyk zależności", "iniekcja zależności")),
            new Rule("inversion of control", "odwrócenie sterowania", List.of("inwersja kontroli")),
            new Rule("endpoint", "punkt końcowy (endpoint)", List.of("końcówka")),
            new Rule("immutable", "niezmienny", List.of("niemutowalny", "niezmutowalny")),
            new Rule("mutable", "zmienny", List.of("mutowalny")),
            new Rule("side effect", "efekt uboczny", List.of("side-effect")),
            new Rule("shadowing / hiding", "przesłanianie / ukrywanie", List.of("cieniowanie")),
            new Rule("property", "właściwość", List.of("własność konfiguracji")),
            new Rule("scope", "zasięg", List.of()),
            new Rule("proxy", "proxy (obiekt pośredniczący)", List.of("pełnomocnik")),
            new Rule("test double", "dubler testowy", List.of("podwójny test")),
            new Rule("slice test", "test wycinkowy", List.of("test plasterkowy")),
            new Rule("autowiring", "automatyczne wiązanie (autowiring)", List.of("autowajrowanie")),
            new Rule("rollback", "wycofanie (rollback)", List.of()),
            new Rule("thread-safe", "bezpieczny wątkowo", List.of("wątkowo-bezpieczny")),
            new Rule("callback", "wywołanie zwrotne", List.of())
    );

    private CourseTerminology() {
    }
}
