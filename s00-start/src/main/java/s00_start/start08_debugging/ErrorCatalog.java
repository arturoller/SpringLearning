package s00_start.start08_debugging;

import java.util.List;
import java.util.Optional;

/**
 * Katalog typowych komunikatów błędów: po FRAGMENCIE tekstu rozpoznaje, czy to błąd KODU, czy ŚRODOWISKA,
 * i podpowiada, co zrobić. Fragmenty pochodzą z prawdziwych uruchomień (Boot 4.1.1, Maven, Mockito).
 */
public final class ErrorCatalog {

    /** CODE = błąd w kodzie/konfiguracji aplikacji; ENVIRONMENT = problem komputera, JDK, portu, narzędzi. */
    public enum Kind {
        CODE, ENVIRONMENT
    }

    public record Diagnosis(Kind kind, String cause, String fix) {
    }

    private record Rule(String fragment, Diagnosis diagnosis) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule("release version 21 not supported",
                    new Diagnosis(Kind.ENVIRONMENT, "Maven działa na starszym JDK (np. 17), a projekt wymaga Javy 21+.",
                            "Ustaw JAVA_HOME na JDK 21 lub nowszy (np. 25) i uruchom ponownie; w IntelliJ wybierz SDK projektu.")),
            new Rule("Could not self-attach to current VM",
                    new Diagnosis(Kind.ENVIRONMENT, "Mockito nie mógł sam dołączyć się do JVM (antywirus albo nowe zasady JDK).",
                            "Uruchamiaj testy przez mvnw — parent pom.xml ustawia agenta Mockito (argLine); w IntelliJ włącz argLine z Mavena.")),
            new Rule("was already in use",
                    new Diagnosis(Kind.ENVIRONMENT, "Port jest zajęty przez inny proces (np. wcześniej uruchomioną aplikację).",
                            "Zatrzymaj tamten proces albo ustaw inny port: server.port=8081 (w testach: RANDOM_PORT).")),
            new Rule("required a bean of type",
                    new Diagnosis(Kind.CODE, "Kontener nie ma beana żądanego typu.",
                            "Dodaj adnotację stereotypu albo metodę @Bean i sprawdź, czy klasa leży w skanowanym pakiecie.")),
            new Rule("required a single bean, but",
                    new Diagnosis(Kind.CODE, "Do wstrzyknięcia pasuje kilku kandydatów tego samego typu.",
                            "Oznacz jednego @Primary, wskaż @Qualifier albo przyjmij listę wszystkich.")),
            new Rule("Could not resolve placeholder",
                    new Diagnosis(Kind.CODE, "Brakuje właściwości, do której odwołuje się @Value.",
                            "Dopisz właściwość do application.properties albo podaj wartość domyślną po dwukropku.")),
            new Rule("Unable to find a @SpringBootConfiguration",
                    new Diagnosis(Kind.CODE, "Test nie znalazł klasy startowej w swoim pakiecie ani wyżej.",
                            "Dodaj klasę @SpringBootApplication w pakiecie lekcji albo wskaż konfigurację w classes."))
    );

    private ErrorCatalog() {
    }

    /** Rozpoznaje błąd po tekście komunikatu; pusty wynik oznacza „nie znam takiego komunikatu”. */
    public static Optional<Diagnosis> diagnose(String message) {
        return RULES.stream()
                .filter(rule -> message.contains(rule.fragment()))
                .map(Rule::diagnosis)
                .findFirst();
    }
}
