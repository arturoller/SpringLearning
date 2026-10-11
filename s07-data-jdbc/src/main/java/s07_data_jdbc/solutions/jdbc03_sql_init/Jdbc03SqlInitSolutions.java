package s07_data_jdbc.solutions.jdbc03_sql_init;

import org.springframework.jdbc.core.simple.JdbcClient;
import s07_data_jdbc.jdbc03_sql_init.SqlInitSupport;

import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jdbc03SqlInit — zajrzyj dopiero po własnej próbie! */
public final class Jdbc03SqlInitSolutions {

    private Jdbc03SqlInitSolutions() {
    }

    static List<String> solution1(JdbcClient jdbcClient) {
        return jdbcClient.sql("SELECT name FROM categories ORDER BY name").query(String.class).list();
    }

    static boolean solution2(String mode) {
        try (var context = SqlInitSupport.start(
                "spring.sql.init.mode=" + mode,
                "spring.sql.init.schema-locations=classpath:db/jdbc03/schema-simple.sql")) {
            return SqlInitSupport.tableNames(context).contains("GADGETS");
        }
    }

    static List<String> solution3() {
        try (var context = SqlInitSupport.start(
                "spring.sql.init.continue-on-error=true",
                "spring.sql.init.schema-locations=classpath:db/jdbc03/schema-with-error.sql")) {
            return SqlInitSupport.tableNames(context);
        }
    }
}
