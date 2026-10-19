package s07_data_jdbc.solutions.jdbc03_sql_init;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jdbc03SqlInit — zajrzyj dopiero po własnej próbie! */
public final class Jdbc03SqlInitSolutions {

    private Jdbc03SqlInitSolutions() {
    }

    /** Rozwiązanie 1: tryb never — Boot nie wykonuje żadnych skryptów. */
    public static String[] solution1() {
        return new String[]{"spring.sql.init.mode=never"};
    }

    /** Rozwiązanie 2: inny plik danych + ignorowanie błędów instrukcji (błędna instrukcja jest pomijana). */
    public static String[] solution2() {
        return new String[]{
                "spring.sql.init.data-locations=classpath:db/jdbc03/data-with-error.sql",
                "spring.sql.init.continue-on-error=true"
        };
    }

    /** Rozwiązanie 3: tabela (schema.sql) PRZED widokiem, który się na niej opiera (schema-views.sql). */
    public static String[] solution3() {
        return new String[]{
                "spring.sql.init.schema-locations=classpath:db/jdbc03/schema.sql,classpath:db/jdbc03/schema-views.sql"
        };
    }

    /** Rozwiązanie 4: skrypt idempotentny — IF NOT EXISTS dla tabeli i MERGE ... KEY dla wiersza (składnia H2). */
    public static String solution4() {
        return """
                CREATE TABLE IF NOT EXISTS department (id INT PRIMARY KEY, name VARCHAR(50));
                MERGE INTO department (id, name) KEY (id) VALUES (1, 'IT');
                """;
    }
}
