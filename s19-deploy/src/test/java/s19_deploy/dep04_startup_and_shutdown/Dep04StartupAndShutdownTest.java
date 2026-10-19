package s19_deploy.dep04_startup_and_shutdown;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.context.LifecycleProperties;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Testy lekcji Dep04StartupAndShutdown. Aplikację uruchamiamy ręcznie przez SpringApplication (a nie adnotacją
 * @SpringBootTest), bo test sam zamyka kontekst i obserwuje, co dzieje się w trakcie zamykania.
 * Deterministyczność: żadnych sleep ani asercji na czasie — „utknięte” żądanie trzyma zatrzask (OrderGate),
 * a na refuzowanie połączeń czeka Awaitility (ponawia sprawdzenie, aż warunek się spełni).
 */
class Dep04StartupAndShutdownTest {

    private static ConfigurableApplicationContext startWeb(String... args) {
        String[] withPort = new String[args.length + 1];
        withPort[0] = "--server.port=0";   // 0 = losowy wolny port (nigdy stały)
        System.arraycopy(args, 0, withPort, 1, args.length);
        return SpringApplication.run(Dep04Application.class, withPort);
    }

    private static int portOf(ConfigurableApplicationContext context) {
        return ((WebServerApplicationContext) context).getWebServer().getPort();
    }

    private static CompletableFuture<HttpResponse<String>> getAsync(int port, String path) {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build();
        return HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    private static boolean acceptsConnections(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 500);
            return true;
        } catch (IOException e) {
            return false;   // np. „Connection refused” — serwer przestał przyjmować nowe połączenia
        }
    }

    @Test
    @DisplayName("1. Wartości domyślne Boota 4.1: server.shutdown = graceful, limit fazy zamykania = 30 sekund")
    void gracefulShutdownIsTheDefault() {
        try (ConfigurableApplicationContext context = startWeb()) {
            assertThat(context.getBean(ServerProperties.class).getShutdown()).isEqualTo(Shutdown.GRACEFUL);
            assertThat(context.getBean(LifecycleProperties.class).getTimeoutPerShutdownPhase())
                    .isEqualTo(Duration.ofSeconds(30));
        }
    }

    @Test
    @DisplayName("2. Łagodne zamykanie: nowe połączenia odrzucone, żądanie w toku DOKOŃCZONE (200), gotowość → REFUSING_TRAFFIC")
    void gracefulShutdownFinishesRequestsInProgress() throws Exception {
        ConfigurableApplicationContext context = startWeb("--server.shutdown=graceful",
                "--spring.lifecycle.timeout-per-shutdown-phase=20s");
        int port = portOf(context);
        OrderGate gate = context.getBean(OrderGate.class);
        ReadinessLog readiness = context.getBean(ReadinessLog.class);

        CompletableFuture<HttpResponse<String>> inProgress = getAsync(port, "/api/slow-order");
        assertThat(gate.awaitEntered(10)).as("żądanie weszło do kontrolera i czeka na bramce").isTrue();

        Thread closer = new Thread(context::close);   // zamykanie w osobnym wątku — czeka na żądanie w toku
        closer.start();

        await().atMost(Duration.ofSeconds(10)).until(() -> !acceptsConnections(port));   // serwer odrzuca NOWE połączenia
        assertThat(inProgress).as("żądanie w toku jeszcze nie skończone — bramka zamknięta").isNotDone();

        gate.open();   // „kelner kończy obsługę” — żądanie może się zakończyć

        HttpResponse<String> response = inProgress.get(10, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("ZAM-001 gotowe");
        closer.join(10_000);
        assertThat(closer.isAlive()).isFalse();
        assertThat(readiness.states()).containsExactly(ReadinessState.ACCEPTING_TRAFFIC, ReadinessState.REFUSING_TRAFFIC);
    }

    @Test
    @DisplayName("2. Dla porównania: server.shutdown=immediate PRZERYWA żądanie w toku — klient dostaje błąd")
    void immediateShutdownInterruptsRequests() throws Exception {
        ConfigurableApplicationContext context = startWeb("--server.shutdown=immediate");
        int port = portOf(context);
        OrderGate gate = context.getBean(OrderGate.class);

        CompletableFuture<HttpResponse<String>> inProgress = getAsync(port, "/api/slow-order");
        assertThat(gate.awaitEntered(10)).isTrue();

        Thread closer = new Thread(context::close);
        closer.start();

        // bramka NIE została otwarta, a żądanie i tak się kończy — błędem połączenia (nie odpowiedzią 200)
        assertThat(inProgress).failsWithin(Duration.ofSeconds(10)).withThrowableOfType(ExecutionException.class)
                .withCauseInstanceOf(IOException.class);
        gate.open();
        closer.join(10_000);
        assertThat(closer.isAlive()).isFalse();
    }

    @Test
    @DisplayName("4. Czas startu: BufferingApplicationStartup zapisuje kroki startu; pierwszy to spring.boot.application.starting")
    void startupTimeline() {
        SpringApplication application = new SpringApplication(Dep04Application.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setApplicationStartup(new BufferingApplicationStartup(4096));   // pojemność bufora zdarzeń
        try (ConfigurableApplicationContext context = application.run()) {
            var events = ((BufferingApplicationStartup) context.getApplicationStartup()).getBufferedTimeline().getEvents();

            assertThat(events).isNotEmpty();
            assertThat(events.get(0).getStartupStep().getName()).isEqualTo("spring.boot.application.starting");
            assertThat(events).extracting(event -> event.getStartupStep().getName())
                    .contains("spring.boot.application.ready");
        }
    }

    @Test
    @DisplayName("5. Leniwa inicjalizacja: bean powstaje dopiero przy pierwszym użyciu (a domyślnie — przy starcie)")
    void lazyInitialization() {
        ExpensiveBean.INSTANCES.set(0);
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Dep04Application.class)
                .web(WebApplicationType.NONE).run()) {
            assertThat(ExpensiveBean.INSTANCES.get()).as("domyślnie: bean utworzony przy starcie").isEqualTo(1);
        }

        ExpensiveBean.INSTANCES.set(0);
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Dep04Application.class)
                .web(WebApplicationType.NONE).run("--spring.main.lazy-initialization=true")) {
            assertThat(ExpensiveBean.INSTANCES.get()).as("lazy: po starcie jeszcze nie ma beana").isZero();
            context.getBean(ExpensiveBean.class);
            assertThat(ExpensiveBean.INSTANCES.get()).as("lazy: bean powstał przy pierwszym użyciu").isEqualTo(1);
        }
    }
}
