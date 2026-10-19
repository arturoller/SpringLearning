package s07_data_jdbc.jdbc03_sql_init;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;

/** Proste zapytania o pracowników — służą testom do sprawdzenia, co zrobiły skrypty SQL przy starcie. */
@Component
public class EmployeeQueries {

    private final JdbcClient jdbc;

    public EmployeeQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public int count() {
        return jdbc.sql("SELECT COUNT(*) FROM employee").query(Integer.class).single();
    }

    public List<String> namesOf(String department) {
        return jdbc.sql("SELECT name FROM employee WHERE department = :department ORDER BY name")
                .param("department", department)
                .query(String.class)
                .list();
    }

    /** Czy tabela o danej nazwie istnieje w bieżącym schemacie (słownik bazy: INFORMATION_SCHEMA). */
    public boolean tableExists(String tableName) {
        return jdbc.sql("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = :name")
                .param("name", tableName.toUpperCase())   // H2 przechowuje nazwy wielkimi literami
                .query(Integer.class)
                .single() > 0;
    }
}
