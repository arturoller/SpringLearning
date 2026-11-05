package s10_transactions.tx03_proxy_pitfalls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopContext;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Tx03ProxyPitfalls na PEŁNEJ aplikacji, BEZ transakcji testu (dlaczego — s10_transactions/Tx01Transactional,
 * sekcja 5). Dane przywraca {@code @BeforeEach}. Wariant z proxy JDK i błędne aplikacje: Tx03SpecialSetupTest.
 */
@SpringBootTest(classes = Tx03Application.class)
@ActiveProfiles("tx03")
class Tx03ProxyPitfallsTest {

    private static final String ORIGINAL = "[Klawiatura=100.00, Mysz=50.00, Monitor=800.00]";

    @Autowired
    private PriceService prices;
    @Autowired
    private SelfInjectingPriceService selfInjecting;
    @Autowired
    private VisibilityShowcase visibility;
    @Autowired
    private StartupShowcase startup;
    @Autowired
    private ThreadShowcase threads;
    @Autowired
    private DiscountPolicy discountPolicy;
    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void resetData() {
        Tx03TestData.reset(jdbc);
    }

    // ---- 1 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 1: Boot domyślnie robi proxy CGLIB — także dla beana z interfejsem")
    void section1CglibByDefault() {
        assertThat(AopUtils.isCglibProxy(prices)).isTrue();
        assertThat(AopUtils.isCglibProxy(discountPolicy)).as("ma interfejs, a i tak CGLIB").isTrue();
        assertThat(discountPolicy).isInstanceOf(SeasonalDiscountPolicy.class);
        assertThat(discountPolicy.getClass().getName())
                .isEqualTo("s10_transactions.tx03_proxy_pitfalls.SeasonalDiscountPolicy$$SpringCGLIB$$0");
        assertThat(AopUtils.getTargetClass(discountPolicy)).isEqualTo(SeasonalDiscountPolicy.class);
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: this.metodaTransakcyjna() — brak transakcji i częściowa podwyżka zapisana w bazie")
    void section2SelfInvocation() {
        assertThat(prices.stateSelfInvoked()).isEqualTo(TxState.NONE);
        assertThat(prices.stateInTransaction().active()).as("ta sama metoda z zewnątrz — przez proxy").isTrue();

        assertThatThrownBy(() -> prices.raiseAllSelfInvoked(20))
                .isInstanceOf(PriceLimitExceededException.class)
                .hasMessage("Cena produktu Monitor przekroczyłaby limit: 960.00");
        assertThat(Tx03TestData.products(jdbc)).as("klawiatura i mysz podrożały mimo wyjątku")
                .containsExactly("Klawiatura=120.00", "Mysz=60.00", "Monitor=800.00");
    }

    @Test
    @DisplayName("Sekcja 2: przez proxy (z zewnątrz, osobny bean, @Lazy self, ObjectProvider) — rollback całej podwyżki")
    void section2WorkingAlternatives() {
        assertThatThrownBy(() -> prices.raiseAllInTransaction(20)).isInstanceOf(PriceLimitExceededException.class);
        assertThat(Tx03TestData.products(jdbc)).hasToString(ORIGINAL);

        assertThatThrownBy(() -> prices.raiseAllViaSeparateBean(20)).isInstanceOf(PriceLimitExceededException.class);
        assertThat(Tx03TestData.products(jdbc)).hasToString(ORIGINAL);

        assertThatThrownBy(() -> selfInjecting.raiseAllViaSelf(20)).isInstanceOf(PriceLimitExceededException.class);
        assertThat(Tx03TestData.products(jdbc)).hasToString(ORIGINAL);

        assertThat(selfInjecting.stateViaLazySelf().active()).isTrue();
        assertThat(selfInjecting.stateViaObjectProvider().active()).isTrue();
    }

    @Test
    @DisplayName("Sekcja 2: AopContext.currentProxy() bez exposeProxy → IllegalStateException")
    void section2AopContextNeedsExposeProxy() {
        assertThatThrownBy(AopContext::currentProxy)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Cannot find current proxy: Set 'exposeProxy' property on Advised to 'true'");
    }

    // ---- 3 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 3: public, protected i package-private — transakcja jest; private, final, static — nie ma")
    void section3MethodVisibility() {
        assertThat(visibility.publicMethod().active()).isTrue();
        assertThat(visibility.protectedMethod().active()).isTrue();
        assertThat(visibility.packagePrivateMethod().active()).isTrue();

        assertThat(visibility.callPrivateMethod()).isEqualTo(TxState.NONE);
        assertThat(visibility.finalMethod()).isEqualTo(TxState.NONE);
        assertThat(VisibilityShowcase.staticMethod()).isEqualTo(TxState.NONE);
    }

    @Test
    @DisplayName("Sekcja 3: metoda final wykonuje się na samym proxy — jego pola są puste (null)")
    void section3FinalMethodRunsOnProxyWithNullFields() {
        assertThat(visibility.publicMethodSeesDependency()).isTrue();
        assertThat(visibility.finalMethodSeesDependency()).as("jdbc == null w proxy").isFalse();
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4: obiekt z new nie ma proxy — brak transakcji i częściowa podwyżka")
    void section4NewObjectHasNoProxy() {
        PriceService manual = new PriceService(jdbc, new PriceUpdater(jdbc));

        assertThat(AopUtils.isAopProxy(manual)).isFalse();
        assertThat(manual.stateInTransaction()).isEqualTo(TxState.NONE);
        assertThatThrownBy(() -> manual.raiseAllInTransaction(20)).isInstanceOf(PriceLimitExceededException.class);
        assertThat(Tx03TestData.products(jdbc)).containsExactly("Klawiatura=120.00", "Mysz=60.00", "Monitor=800.00");
    }

    @Test
    @DisplayName("Sekcja 4: @PostConstruct z @Transactional — bez transakcji; słuchacz ApplicationReadyEvent — z transakcją")
    void section4StartupCode() {
        assertThat(startup.seenInPostConstruct()).isEqualTo(TxState.NONE);
        assertThat(startup.seenOnApplicationReady()).isEqualTo(
                new TxState(true, "s10_transactions.tx03_proxy_pitfalls.StartupShowcase.onApplicationReady"));
    }

    // ---- 5 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 5: transakcja należy do wątku — w CompletableFuture jej nie ma")
    void section5OtherThreadHasNoTransaction() {
        TxState[] states = threads.statesInBothThreads();

        assertThat(states[0]).isEqualTo(
                new TxState(true, "s10_transactions.tx03_proxy_pitfalls.ThreadShowcase.statesInBothThreads"));
        assertThat(states[1]).isEqualTo(TxState.NONE);
    }

    @Test
    @DisplayName("Sekcja 5: zapis z innego wątku nie jest wycofywany razem z transakcją")
    void section5OtherThreadWriteSurvivesRollback() {
        assertThatThrownBy(threads::insertInBothThreadsThenFail).isInstanceOf(IllegalStateException.class);

        assertThat(Tx03TestData.products(jdbc)).contains("Kabel=19.00").doesNotContain("Słuchawki=199.00");
    }
}
