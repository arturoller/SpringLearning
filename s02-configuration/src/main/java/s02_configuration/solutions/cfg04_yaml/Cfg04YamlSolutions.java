package s02_configuration.solutions.cfg04_yaml;

/** Rozwiązania wzorcowe ćwiczeń lekcji Cfg04Yaml — zajrzyj dopiero po własnej próbie! */
public final class Cfg04YamlSolutions {

    private Cfg04YamlSolutions() {
    }

    /** Rozwiązanie 1: prefiks raz, wcięcia po dwie spacje, lista z myślnikami. */
    public static String solution1() {
        return """
                cfg04:
                  exercise:
                    name: Kawiarnia
                    tables: 12
                    open: true
                    menu:
                      - kawa
                      - herbata
                """;
    }

    /** Rozwiązanie 2: separator --- i warunek on-profile w drugim dokumencie. */
    public static String solution2() {
        return """
                cfg04:
                  exercise:
                    greeting: Witaj
                ---
                spring:
                  config:
                    activate:
                      on-profile: cfg04-promo
                cfg04:
                  exercise:
                    greeting: Promocja!
                """;
    }

    /** Rozwiązanie 3: user i pool wcięte o jeden poziom głębiej — wewnątrz db. */
    public static String solution3() {
        return """
                cfg04:
                  exercise:
                    db:
                      url: jdbc:h2:mem:cfg04
                      user: sa
                      pool:
                        max: 5
                """;
    }
}
