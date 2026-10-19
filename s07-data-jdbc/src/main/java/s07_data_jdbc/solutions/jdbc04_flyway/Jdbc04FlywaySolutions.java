package s07_data_jdbc.solutions.jdbc04_flyway;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jdbc04Flyway — zajrzyj dopiero po własnej próbie! */
public final class Jdbc04FlywaySolutions {

    private Jdbc04FlywaySolutions() {
    }

    /** Rozwiązanie 1: V + wersja 4 + dwa podkreślniki + opis „create customer” (spacja → podkreślnik) + .sql. */
    public static String solution1() {
        return "V4__create_customer.sql";
    }

    /** Rozwiązanie 2: nowa migracja z ALTER TABLE (kolumna bez NOT NULL, więc istniejące wiersze dostają NULL). */
    public static String solution2() {
        return "ALTER TABLE product ADD COLUMN description VARCHAR(200);";
    }

    /** Rozwiązanie 3: zgoda na wykonanie migracji o numerze niższym niż już wykonane. */
    public static String solution3() {
        return "spring.flyway.out-of-order=true";
    }

    /** Rozwiązanie 4: ten sam widok z niższym progiem ceny; CREATE OR REPLACE pozwala powtarzać migrację. */
    public static String solution4() {
        return """
                CREATE OR REPLACE VIEW expensive_product AS
                    SELECT id, sku, name, price FROM product WHERE price >= 300;
                """;
    }
}
