package s10_transactions.tx03_proxy_pitfalls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Testy lekcji Tx03ProxyPitfalls — „WYNIK” każdej sekcji.
 *
 * <p>Jak w Tx01: {@code @SpringBootTest} BEZ transakcji testu — inaczej każda metoda serwisu dołączyłaby do
 * transakcji testu i nie dałoby się sprawdzić, czy SAMA otworzyła transakcję. Test leży w tym samym pakiecie co
 * serwisy, więc może wołać także metody package-private i protected (sekcja 2).</p>
 *
 * <p>Jak sprawdzić w teście, czy metoda NAPRAWDĘ działała w transakcji? Dwa sposoby, oba użyte poniżej:
 * metoda-„sonda” zwraca {@code TransactionSynchronizationManager.isActualTransactionActive()} ze swojego wnętrza,
 * a zmiana encji BEZ save() trafia do bazy tylko w transakcji (dirty checking) — więc wystarczy policzyć sztuki
 * w bazie.</p>
 */
@SpringBootTest
@ActiveProfiles("tx03")
class Tx03ProxyPitfallsTest {

    @Autowired
    StockService stockService;

    @Autowired
    ReservationService reservations;

    @Autowired
    StockWarmup warmup;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void resetStock() {
        jdbc.sql("update stock_items set quantity = 120 where sku = 'SPO-001'").update();
        jdbc.sql("update stock_items set quantity = 300 where sku = 'SPO-002'").update();
        jdbc.sql("update stock_items set quantity = 0 where sku = 'SPO-003'").update();
    }

    int quantity(String sku) {
        return jdbc.sql("select quantity from stock_items where sku = ?").param(sku).query(Integer.class).single();
    }

    /** Kawa 2 szt., Oliwa 1 szt. (brak na stanie!), Czekolada 5 szt. — w tej kolejności. */
    Map<String, Integer> orderWithMissingOil() {
        Map<String, Integer> items = new LinkedHashMap<>();
        items.put("SPO-001", 2);
        items.put("SPO-003", 1);
        items.put("SPO-002", 5);
        return items;
    }

    @Test
    @DisplayName("1. this.metoda() omija proxy: brak transakcji, a zmiana encji bez save() przepada")
    void selfInvocationSkipsTheProxy() {
        assertThat(AopUtils.isAopProxy(stockService)).as("wstrzyknięty bean to proxy").isTrue();
        assertThat(stockService.isThisAProxy()).as("this wewnątrz metody to NIE proxy").isFalse();

        assertThat(stockService.restock("SPO-001", 10)).as("wywołanie z zewnątrz — przez proxy").isTrue();
        assertThat(quantity("SPO-001")).isEqualTo(130);

        assertThat(stockService.restockViaThis("SPO-001", 10)).as("this.restock(...) — bez proxy").isFalse();
        assertThat(quantity("SPO-001")).as("dostawa po cichu przepadła").isEqualTo(130);
    }

    @Test
    @DisplayName("2. private i final — bez transakcji; protected i package-private — z transakcją (Spring 6.0+)")
    void visibilityAndFinal() {
        assertThat(stockService.callPrivateTransactional()).as("private").isFalse();
        assertThat(stockService.packagePrivateProbe()).as("package-private").isTrue();
        assertThat(stockService.protectedProbe()).as("protected").isTrue();
        assertThat(stockService.finalProbe()).as("public final").isFalse();

        assertThatThrownBy(() -> stockService.finalCountItems())
                .as("metoda final wykonuje się na obiekcie proxy, którego pola są puste")
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("because \"this.stock\" is null");
    }

    @Test
    @DisplayName("2. Klasa final z @Transactional jako bean → kontekst nie startuje (CGLIB nie zrobi podklasy)")
    void finalClassCannotBeProxied() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TransactionAutoConfiguration.class))
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .withBean(FinalPriceService.class)
                .run(context -> assertThat(context).hasFailed()
                        .getFailure()
                        .hasMessageContaining("Could not generate CGLIB subclass of class "
                                + "s10_transactions.tx03_proxy_pitfalls.FinalPriceService: Common causes of this "
                                + "problem include using a final class or a non-visible class"));
    }

    @Test
    @DisplayName("3. Wyjątek złapany w środku albo zamieniony na kontrolowany → COMMIT części zmian")
    void caughtOrCheckedExceptionCommits() {
        assertThat(reservations.reserveIgnoringErrors(orderWithMissingOil())).isEqualTo(2);
        assertThat(quantity("SPO-001")).as("kawa zdjęta").isEqualTo(118);
        assertThat(quantity("SPO-002")).as("czekolada zdjęta").isEqualTo(295);

        resetStock();
        assertThatThrownBy(() -> reservations.reserveOrThrowChecked(orderWithMissingOil()))
                .isInstanceOf(ReservationException.class)
                .hasMessage("Rezerwacja nieudana: Za mało SPO-003: jest 0, potrzeba 1");
        assertThat(quantity("SPO-001")).as("wyjątek kontrolowany → commit kawy sprzed błędu").isEqualTo(118);
        assertThat(quantity("SPO-002")).as("do czekolady pętla nie doszła").isEqualTo(300);
    }

    @Test
    @DisplayName("3. Naprawy: rollbackFor dla wyjątku kontrolowanego albo setRollbackOnly() i zwykły wynik")
    void fixesForExceptions() {
        assertThatThrownBy(() -> reservations.reserveOrThrowCheckedWithRollback(orderWithMissingOil()))
                .isInstanceOf(ReservationException.class);
        assertThat(quantity("SPO-001")).as("rollbackFor → wycofane").isEqualTo(120);

        assertThat(reservations.reserveOrReport(orderWithMissingOil())).as("metoda kończy się normalnie").isFalse();
        assertThat(quantity("SPO-001")).as("setRollbackOnly() → wycofane").isEqualTo(120);

        Map<String, Integer> available = Map.of("SPO-001", 2);
        assertThat(reservations.reserveOrReport(available)).isTrue();
        assertThat(quantity("SPO-001")).isEqualTo(118);

        // Poza metodą wywołaną przez proxy @Transactional (np. tu, w teście bez transakcji) nie ma czego oznaczać.
        assertThatThrownBy(TransactionAspectSupport::currentTransactionStatus)
                .isInstanceOf(NoTransactionException.class)
                .hasMessage("No transaction aspect-managed TransactionStatus in scope");
    }

    @Test
    @DisplayName("4. @PostConstruct: this.metoda() bez transakcji, inny bean i ApplicationReadyEvent — z transakcją")
    void postConstruct() {
        assertThat(warmup.wasTransactionInPostConstruct()).as("this.warmUp() w @PostConstruct").isFalse();
        assertThat(warmup.wasTransactionViaOtherBeanInPostConstruct()).as("inny bean w @PostConstruct").isTrue();
        assertThat(warmup.wasTransactionOnApplicationReady()).as("po starcie aplikacji").isTrue();
    }

    @Test
    @DisplayName("5. Naprawy self-invocation: inny bean, @Lazy „ja sam”, ObjectProvider, TransactionTemplate")
    void fixesForSelfInvocation() {
        assertThat(stockService.restockViaOtherBean("SPO-001", 1)).isTrue();
        assertThat(stockService.restockViaLazySelf("SPO-001", 1)).isTrue();
        assertThat(stockService.restockViaProvider("SPO-001", 1)).isTrue();
        assertThat(stockService.restockViaTemplate("SPO-001", 1)).isTrue();

        assertThat(quantity("SPO-001")).as("wszystkie 4 dostawy zapisane").isEqualTo(124);
    }
}
