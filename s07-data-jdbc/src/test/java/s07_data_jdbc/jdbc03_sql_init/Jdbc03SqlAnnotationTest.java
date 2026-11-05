package s07_data_jdbc.jdbc03_sql_init;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

/**
 * Testy lekcji Jdbc03SqlInit, sekcja 3: adnotacja @Sql. Wycinek @JdbcTest z profilem jdbc03 daje 3 produkty
 * ze skryptów startowych; każdy test działa w transakcji wycofywanej na końcu. Testy NIE zależą od kolejności.
 */
@JdbcTest
@ActiveProfiles("jdbc03")
@TestExecutionListeners(listeners = Jdbc03SqlAnnotationTest.AfterPhaseProbe.class, mergeMode = MERGE_WITH_DEFAULTS)
class Jdbc03SqlAnnotationTest {

    @Autowired
    private JdbcClient jdbc;

    private long count() {
        return jdbc.sql("SELECT COUNT(*) FROM product").query(Long.class).single();
    }

    @Test
    @DisplayName("Sekcja 3: bez @Sql test widzi tylko 3 produkty ze skryptów startowych")
    void withoutSql() {
        assertThat(count()).isEqualTo(3);
    }

    @Test
    @Sql("/jdbc03/extra-products.sql")
    @DisplayName("Sekcja 3: @Sql wykonuje skrypt z src/test/resources PRZED metodą testu")
    void sqlScriptRunsBeforeTest() {
        assertThat(count()).isEqualTo(5);
        assertThat(jdbc.sql("SELECT name FROM product WHERE category = 'dom' ORDER BY name").query(String.class).list())
                .containsExactly("Długopis", "Kubek", "Lampka", "Zeszyt");
    }

    @Test
    @Sql("/jdbc03/extra-products.sql")
    @DisplayName("Sekcja 3: ten sam skrypt w drugim teście — dane z poprzedniego testu zniknęły (rollback), nie ma 7 produktów")
    void sqlScriptDataDoesNotLeakBetweenTests() {
        assertThat(count()).isEqualTo(5);
    }

    @Test
    @Sql(statements = "DELETE FROM product")
    @DisplayName("Sekcja 3: @Sql(statements = …) wykonuje polecenia wpisane w adnotacji")
    void inlineStatements() {
        assertThat(count()).isZero();
    }

    @Test
    @SqlGroup({
            @Sql("/jdbc03/clear-products.sql"),
            @Sql("/jdbc03/extra-products.sql")
    })
    @DisplayName("Sekcja 3: @SqlGroup (albo kilka @Sql) wykonuje skrypty po kolei: najpierw czyszczenie, potem dane")
    void sqlGroupRunsScriptsInOrder() {
        assertThat(count()).isEqualTo(2);
    }

    @Test
    @Sql(scripts = "/jdbc03/mark-after.sql", executionPhase = AFTER_TEST_METHOD)
    @DisplayName("Sekcja 3: executionPhase = AFTER_TEST_METHOD wykonuje skrypt dopiero PO metodzie testu")
    void afterTestMethodPhase() {
        assertThat(count()).as("w trakcie testu znacznika jeszcze nie ma").isEqualTo(3);
        // Sprawdzenie, że skrypt wykonał się PO teście, robi AfterPhaseProbe poniżej (rzuca AssertionError, gdy nie).
    }

    /**
     * Własny nasłuchiwacz testów (TestExecutionListener): ma kolejność 4500, czyli jego afterTestMethod biegnie
     * PO afterTestMethod nasłuchiwacza @Sql (5000), a PRZED wycofaniem transakcji (4000). Dzięki temu widzi
     * skutek skryptu z fazy AFTER_TEST_METHOD, zanim zniknie w rollbacku.
     */
    static class AfterPhaseProbe extends AbstractTestExecutionListener {

        @Override
        public int getOrder() {
            return 4500;
        }

        @Override
        public void afterTestMethod(TestContext testContext) {
            if (!testContext.getTestMethod().getName().equals("afterTestMethodPhase")) {
                return;
            }
            JdbcClient client = testContext.getApplicationContext().getBean(JdbcClient.class);
            long markers = client.sql("SELECT COUNT(*) FROM product WHERE name = 'Znacznik po teście'")
                    .query(Long.class).single();
            assertThat(markers).as("skrypt AFTER_TEST_METHOD powinien już się wykonać").isEqualTo(1);
        }
    }
}
