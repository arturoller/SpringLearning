package s03_boot_basics.solutions.boot01_auto_configuration;

import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Boot01AutoConfiguration — zajrzyj dopiero po własnej próbie! */
public final class Boot01AutoConfigurationSolutions {

    private static final String JDBC_PACKAGE = "org.springframework.boot.jdbc.autoconfigure.";

    private Boot01AutoConfigurationSolutions() {
    }

    /** Rozwiązanie 1: pełna nazwa klasy z Boot 4 — z getName(), więc literówka jest niemożliwa. */
    public static String solution1ExcludeProperty() {
        return "spring.autoconfigure.exclude=" + DataSourceAutoConfiguration.class.getName();
    }

    /** Rozwiązanie 2: zwykły DataSource bez puli; jego obecność wystarcza, żeby Boot ustąpił. */
    public static DataSource solution2OwnDataSource() {
        return new DriverManagerDataSource("jdbc:h2:mem:boot01-cw2", "sa", "");
    }

    /** Rozwiązanie 3: brak wpisu w raporcie = autokonfiguracja nie była nawet oceniana → false. */
    public static boolean solution3Matched(ConditionEvaluationReport report, String autoConfigurationClassName) {
        ConditionEvaluationReport.ConditionAndOutcomes outcomes =
                report.getConditionAndOutcomesBySource().get(autoConfigurationClassName);
        return outcomes != null && outcomes.isFullMatch();
    }

    /** Rozwiązanie 4: filtr kluczy (tylko klasy *AutoConfiguration najwyższego poziomu z pakietu) + filtr wyniku + sortowanie. */
    public static List<String> solution4NegativeJdbcMatches(ConditionEvaluationReport report) {
        return report.getConditionAndOutcomesBySource().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(JDBC_PACKAGE))
                .filter(entry -> !entry.getValue().isFullMatch())
                .map(entry -> entry.getKey().substring(JDBC_PACKAGE.length()))
                .filter(simpleName -> !simpleName.contains(".") && !simpleName.contains("$") && !simpleName.contains("#"))
                .filter(simpleName -> simpleName.endsWith("AutoConfiguration"))
                .sorted()
                .toList();
    }
}
