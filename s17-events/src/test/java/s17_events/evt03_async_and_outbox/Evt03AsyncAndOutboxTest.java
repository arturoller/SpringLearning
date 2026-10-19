package s17_events.evt03_async_and_outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Testy lekcji Evt03AsyncAndOutbox. {@code @SpringBootTest} (prawdziwa baza H2, prawdziwa pula wątków dla {@code @Async}).
 * Asynchroniczność testujemy deterministycznie: jawna „bramka” z zatrzaskiem (BlockingGate) i Awaitility
 * (czekanie na warunek z limitem czasu) — nigdy {@code sleep}. Testy zatwierdzają dane naprawdę, więc
 * {@code @BeforeEach} czyści stan.
 */
@SpringBootTest
@ActiveProfiles("evt03")
@ExtendWith(OutputCaptureExtension.class)
class Evt03AsyncAndOutboxTest {

    private static final BigDecimal TOTAL = new BigDecimal("349.90");

    @Autowired
    private OrderService orderService;
    @Autowired
    private OutboxOrderService outboxOrderService;
    @Autowired
    private OutboxRelay relay;
    @Autowired
    private OrderRepository repository;
    @Autowired
    private NotificationSender sender;
    @Autowired
    private BlockingGate gate;
    @Autowired
    private EventLog log;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        log.clear();
        sender.reset();
        gate.close();
    }

    @Test
    @DisplayName("1. @Async: publikujący wraca, zanim słuchacz skończy; słuchacz działa w innym wątku i bez transakcji")
    void section1() {
        orderService.publishGated("pierwsze");

        assertThat(log.whats()).isEmpty();   // publishEvent już wrócił, a słuchacz wciąż czeka na bramce
        gate.open();
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(log.entries()).hasSize(1));

        EventLog.Entry entry = log.entries().get(0);
        assertThat(entry.what()).isEqualTo("async:pierwsze (transakcja aktywna=false)");
        assertThat(entry.thread()).isNotEqualTo(Thread.currentThread().getName()).startsWith("task-");
    }

    @Test
    @DisplayName("2. Wyjątek asynchronicznego słuchacza nie wraca do publikującego — ląduje tylko w logu (ERROR)")
    void section2(CapturedOutput output) {
        assertThatCode(() -> orderService.publishFailingAsync("test")).doesNotThrowAnyException();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(output.getOut())
                .contains("Unexpected exception occurred invoking async method")
                .contains("Asynchroniczny słuchacz zawiódł: test"));
    }

    @Test
    @DisplayName("3. @Async + @TransactionalEventListener: osobny wątek, PO commicie (zamówienie już w bazie), bez transakcji")
    void section3() {
        orderService.placeOrderAsync("ZAM-001", "Jan Kowalski", TOTAL);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(log.entries()).hasSize(1));
        EventLog.Entry entry = log.entries().get(0);
        assertThat(entry.what()).isEqualTo("async-po-commicie:ZAM-001 (zamówień w bazie=1, transakcja aktywna=false)");
        assertThat(entry.thread()).isNotEqualTo(Thread.currentThread().getName());
    }

    @Test
    @DisplayName("4. Bez outboxa: gdy wysyłka po commicie zawiedzie, zamówienie zostaje, a powiadomienie przepada (nic go nie ponowi)")
    void section4(CapturedOutput output) {
        sender.setDown(true);

        assertThatCode(() -> orderService.placeOrder("ZAM-002", "Maria Nowak", TOTAL)).doesNotThrowAnyException();

        assertThat(repository.countOrders()).isEqualTo(1);          // zamówienie jest zatwierdzone
        assertThat(sender.calls()).isEmpty();                       // a powiadomienie nie wyszło
        assertThat(output.getOut()).contains("Serwer poczty niedostępny");
        sender.setDown(false);
        assertThat(sender.calls()).isEmpty();                       // po naprawie serwera nikt do tego nie wraca: zdarzenie przepadło
    }

    @Test
    @DisplayName("5. Outbox: zamówienie i wiadomość zapisują się atomowo — albo oba, albo żadne")
    void section5() {
        outboxOrderService.placeOrder("ZAM-003", "Adam Mazur", TOTAL);
        assertThat(repository.countOrders()).isEqualTo(1);
        assertThat(repository.findUnsent()).singleElement().satisfies(message -> {
            assertThat(message.orderId()).isEqualTo("ZAM-003");
            assertThat(message.type()).isEqualTo("OrderPlaced");
            assertThat(message.payload()).isEqualTo("orderId=ZAM-003;customer=Adam Mazur");
            assertThat(message.attempts()).isZero();
        });

        assertThatThrownBy(() -> outboxOrderService.placeOrderAndFail("ZAM-004", "Zofia Krawczyk", TOTAL))
                .isInstanceOf(IllegalStateException.class).hasMessage("Płatność odrzucona");
        assertThat(repository.countOrders()).isEqualTo(1);          // nadal tylko ZAM-003
        assertThat(repository.countOutbox()).isEqualTo(1);          // i tylko jego wiadomość
    }

    @Test
    @DisplayName("6. Przekaźnik: przy awarii odbiorcy wiadomość zostaje w skrzynce, a po naprawie zostaje wysłana")
    void section6() {
        outboxOrderService.placeOrder("ZAM-005", "Ola Pawlak", TOTAL);
        sender.setDown(true);

        assertThat(relay.sendPending()).isZero();
        assertThat(repository.findUnsent()).singleElement().satisfies(message -> assertThat(message.attempts()).isEqualTo(1));
        assertThat(sender.calls()).isEmpty();

        sender.setDown(false);
        assertThat(relay.sendPending()).isEqualTo(1);
        assertThat(repository.findUnsent()).isEmpty();
        assertThat(sender.calls()).hasSize(1);
        assertThat(relay.sendPending()).isZero();                   // nic więcej do wysłania
    }

    @Test
    @DisplayName("7. Co najmniej raz: awaria między wysyłką a oznaczeniem daje duplikat, który odbiorca musi rozpoznać po id")
    void section7() {
        outboxOrderService.placeOrder("ZAM-006", "Marek Król", TOTAL);
        OutboxMessage message = repository.findUnsent().get(0);

        sender.send(message);   // „wysłano, ale aplikacja padła, zanim zdążyła oznaczyć wiadomość jako wysłaną”
        assertThat(repository.findUnsent()).hasSize(1);

        relay.sendPending();    // po restarcie przekaźnik wysyła tę samą wiadomość ponownie

        assertThat(sender.calls()).hasSize(2);                      // dwa wywołania odbiorcy
        assertThat(sender.delivered()).hasSize(1);                  // ale jedna unikalna wiadomość (odbiorca odfiltrował duplikat)
        assertThat(repository.findUnsent()).isEmpty();
    }

    /** Bean z metodą @Async — do sekcji 1b. */
    static class AsyncBean {
        @Async
        public void run(CompletableFuture<String> result) {
            result.complete(Thread.currentThread().getName());
        }
    }

    /** Konfiguracja BEZ @EnableAsync (celowo bez @Configuration, żeby @SpringBootTest jej nie podchwycił). */
    static class WithoutEnableAsync {
        @Bean
        AsyncBean asyncBean() {
            return new AsyncBean();
        }
    }

    /** Ta sama konfiguracja Z @EnableAsync. */
    @EnableAsync
    static class WithEnableAsync extends WithoutEnableAsync {
    }

    @Test
    @DisplayName("1b. Bez @EnableAsync adnotacja @Async jest ignorowana (nawet gdy Boot dostarcza pulę wątków)")
    void section1b() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class));

        runner.withUserConfiguration(WithoutEnableAsync.class).run(context -> {
            CompletableFuture<String> result = new CompletableFuture<>();
            context.getBean(AsyncBean.class).run(result);
            assertThat(result).isDone();   // wykonało się od razu, w wątku wywołującego
            assertThat(result.get()).isEqualTo(Thread.currentThread().getName());
        });

        runner.withUserConfiguration(WithEnableAsync.class).run(context -> {
            CompletableFuture<String> result = new CompletableFuture<>();
            context.getBean(AsyncBean.class).run(result);
            assertThat(result.get(5, TimeUnit.SECONDS)).startsWith("task-");   // osobny wątek z puli Boota
        });
    }
}
