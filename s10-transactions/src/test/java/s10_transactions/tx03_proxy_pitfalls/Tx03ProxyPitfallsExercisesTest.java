package s10_transactions.tx03_proxy_pitfalls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import s10_transactions.tx03_proxy_pitfalls.ExerciseProductService.Rename;

import java.util.List;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy ĆWICZEŃ lekcji Tx03ProxyPitfalls. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Pełna aplikacja BEZ transakcji testu; dane przywraca {@code @BeforeEach}.
 */
@Tag("cwiczenie")
@SpringBootTest(classes = Tx03Application.class)
@ActiveProfiles("tx03")
class Tx03ProxyPitfallsExercisesTest {

    private static final String RENAME_TX = "s10_transactions.tx03_proxy_pitfalls.ExerciseProductService.renameOneInNewTransaction";

    @Autowired
    private ProxyExercises exercises;
    @Autowired
    private ExerciseProductService products;
    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void resetData() {
        Tx03TestData.reset(jdbc);
    }

    @Test
    @DisplayName("Ćwiczenie 1: rozpoznanie proxy CGLIB, proxy JDK i zwykłego obiektu")
    void exercise1() {
        assertThat(exercises.describeProxy(products)).isEqualTo("CGLIB");

        ProxyFactory factory = new ProxyFactory(new SeasonalDiscountPolicy()); // proxy factory = fabryka proxy
        factory.addInterface(DiscountPolicy.class);
        assertThat(exercises.describeProxy(factory.getProxy())).isEqualTo("JDK");

        assertThat(exercises.describeProxy(new SeasonalDiscountPolicy())).isEqualTo("BRAK");
    }

    @Test
    @DisplayName("Ćwiczenie 2: zmiana nazw — wszystkie albo żadna")
    void exercise2() {
        products.renameAll(List.of(new Rename(1L, "Klawiatura PRO"), new Rename(2L, "Mysz PRO")));
        assertThat(Tx03TestData.products(jdbc)).containsExactly("Klawiatura PRO=100.00", "Mysz PRO=50.00", "Monitor=800.00");

        assertThatThrownBy(() -> products.renameAll(List.of(new Rename(3L, "Monitor 4K"), new Rename(1L, " "))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(Tx03TestData.products(jdbc)).as("zmiana monitora musi zostać wycofana")
                .containsExactly("Klawiatura PRO=100.00", "Mysz PRO=50.00", "Monitor=800.00");
    }

    @Test
    @DisplayName("Ćwiczenie 3: każda zmiana nazwy w osobnej transakcji (przez proxy), puste nazwy pominięte")
    void exercise3() {
        List<String> names = products.renameEachSeparately(
                List.of(new Rename(1L, "Klawiatura PRO"), new Rename(2L, ""), new Rename(3L, "Monitor 4K")));

        assertThat(names).containsExactly(RENAME_TX, RENAME_TX);
        assertThat(Tx03TestData.products(jdbc)).containsExactly("Klawiatura PRO=100.00", "Mysz=50.00", "Monitor 4K=800.00");
    }

    @Test
    @DisplayName("Ćwiczenie 4: zmiana nazwy w innym wątku — i w transakcji tego wątku")
    void exercise4() {
        ProxyExercises.WorkerReport report = exercises.renameInWorkerThread(2L, "Mysz bezprzewodowa");

        assertThat(report.threadName()).isNotEqualTo(Thread.currentThread().getName());
        assertThat(report.transactionActive()).isTrue();
        assertThat(Tx03TestData.products(jdbc)).contains("Mysz bezprzewodowa=50.00");

        assertThatThrownBy(() -> exercises.renameInWorkerThread(1L, " "))
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
        assertThat(Tx03TestData.products(jdbc)).contains("Klawiatura=100.00");
    }
}
