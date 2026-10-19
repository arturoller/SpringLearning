package s01_core_ioc.ioc05_multiple_candidates;

import jakarta.annotation.Priority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Fallback;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Ioc05MultipleCandidates. {@code @SpringBootTest} startuje kontekst Ioc05Application (sekcje z gotowymi
 * usługami), a {@code ApplicationContextRunner} buduje małe konteksty z wybranymi klasami — także takie, które
 * CELOWO się nie uruchamiają (bo przykłady błędów nie mogą trafić do aplikacji lekcji).
 */
@SpringBootTest
class Ioc05MultipleCandidatesTest {

    @Autowired
    OrderConfirmationService confirmation;
    @Autowired
    DeliveryNotifier delivery;
    @Autowired
    BroadcastService broadcast;

    @Test
    @DisplayName("1. Dwóch kandydatów, żadnego wyboru: NoUniqueBeanDefinitionException")
    void twoCandidatesWithoutChoiceFail() {
        new ApplicationContextRunner()
                .withUserConfiguration(SmsSender.class, PushSender.class, RenamedParameterConsumer.class)
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    Throwable root = rootCause(ctx.getStartupFailure());
                    assertThat(root).isInstanceOf(NoUniqueBeanDefinitionException.class);
                    assertThat(root.getMessage()).contains("expected single matching bean but found 2");
                    assertThat(root.getMessage()).contains("smsSender").contains("pushSender");
                });
    }

    @Test
    @DisplayName("2. @Primary rozstrzyga: usługa dostaje wysyłacz e-mail")
    void primaryWins() {
        assertThat(confirmation.confirm("ola@example.com", 17)).startsWith("[EMAIL]");
        new ApplicationContextRunner()
                .withUserConfiguration(EmailSender.class, SmsSender.class, RenamedParameterConsumer.class)
                .run(ctx -> assertThat(ctx.getBean(RenamedParameterConsumer.class).sender())
                        .isInstanceOf(EmailSender.class));
    }

    @Test
    @DisplayName("2. Dwa beany @Primary tego samego typu to znowu błąd")
    void twoPrimariesFail() {
        new ApplicationContextRunner()
                .withUserConfiguration(EmailSender.class, RenamedParameterConsumer.class)
                .withBean("smsSender", SmsSender.class, SmsSender::new, bd -> bd.setPrimary(true))
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    Throwable root = rootCause(ctx.getStartupFailure());
                    assertThat(root).isInstanceOf(NoUniqueBeanDefinitionException.class);
                    assertThat(root.getMessage()).contains("more than one 'primary' bean found");
                });
    }

    @Test
    @DisplayName("3. @Qualifier wskazuje beana po nazwie i wygrywa z @Primary")
    void qualifierBeatsPrimary() {
        assertThat(delivery.courierOnTheWay("600100200")).startsWith("[SMS]");
    }

    @Test
    @DisplayName("4. List<NotificationSender>: wszystkie beany w kolejności @Order")
    void listInjectsAllInOrder() {
        assertThat(broadcast.channelsInOrder()).containsExactly("EMAIL", "SMS", "PUSH");
        assertThat(broadcast.sendToAll("ola", "Cześć")).hasSize(3).first().asString().startsWith("[EMAIL]");
    }

    @Test
    @DisplayName("4. Map<String, NotificationSender>: kluczem jest nazwa beana")
    void mapInjectsByBeanName() {
        List<String> names = broadcast.byName().keySet().stream().sorted().toList();
        assertThat(names).containsExactly("emailSender", "pushSender", "smsSender");
        assertThat(broadcast.byName().get("smsSender")).isInstanceOf(SmsSender.class);
    }

    @Test
    @DisplayName("4. Dla beanów z metod @Bean @Order z klasy działa, a @Primary z klasy — nie (ćwiczenie 2)")
    void orderFromClassWorksForBeanMethods() {
        new ApplicationContextRunner()
                .withUserConfiguration(ReversedSendersConfig.class)
                .withBean(BroadcastService.class)
                .run(ctx -> {
                    // metody zarejestrowane w kolejności push, sms, email — lista i tak idzie według @Order z klas
                    assertThat(ctx.getBean(BroadcastService.class).channelsInOrder()).containsExactly("EMAIL", "SMS", "PUSH");
                    // @Primary z klasy EmailSender jest pomijany: jednego beana nie da się wybrać
                    assertThatThrownBy(() -> ctx.getBean(NotificationSender.class))
                            .isInstanceOf(NoUniqueBeanDefinitionException.class);
                });
    }

    @Test
    @DisplayName("5. Awaryjnie bean dopasowany po nazwie parametru konstruktora (działa dzięki -parameters)")
    void fallbackByParameterName() {
        new ApplicationContextRunner()
                .withUserConfiguration(SmsSender.class, PushSender.class, ByParameterNameConsumer.class)
                .run(ctx -> assertThat(ctx.getBean(ByParameterNameConsumer.class).sender())
                        .isInstanceOf(SmsSender.class));
    }

    @Test
    @DisplayName("5. PUŁAPKA: ta sama klasa z inną nazwą parametru przestaje działać, a @Primary wygrywa z nazwą")
    void nameFallbackIsFragile() {
        // zmieniona nazwa parametru (sender zamiast smsSender) -> brak dopasowania -> błąd
        new ApplicationContextRunner()
                .withUserConfiguration(SmsSender.class, PushSender.class, RenamedParameterConsumer.class)
                .run(ctx -> assertThat(ctx).hasFailed());
        // parametr nazywa się smsSender, ale @Primary ma EmailSender -> dostajemy e-mail, nie SMS
        new ApplicationContextRunner()
                .withUserConfiguration(EmailSender.class, SmsSender.class, ByParameterNameConsumer.class)
                .run(ctx -> assertThat(ctx.getBean(ByParameterNameConsumer.class).sender())
                        .isInstanceOf(EmailSender.class));
    }

    @Test
    @DisplayName("5. @Fallback odpada, gdy jest inny kandydat (nawet przy pasującej nazwie); @Priority: mniejsza liczba wygrywa")
    void fallbackAndPriority() {
        // @Fallback + zwykły bean -> wygrywa zwykły; bean awaryjny sam -> używany
        new ApplicationContextRunner()
                .withUserConfiguration(SmsSender.class, BackupSender.class, RenamedParameterConsumer.class)
                .run(ctx -> assertThat(ctx.getBean(RenamedParameterConsumer.class).sender().channel()).isEqualTo("SMS"));
        new ApplicationContextRunner()
                .withUserConfiguration(BackupSender.class, RenamedParameterConsumer.class)
                .run(ctx -> assertThat(ctx.getBean(RenamedParameterConsumer.class).sender().channel()).isEqualTo("BACKUP"));

        // parametr nazywa się smsSender, ale bean "smsSender" jest awaryjny (setFallback(true) = to samo co @Fallback)
        // -> odpada przed sprawdzeniem nazwy, wygrywa PushSender
        new ApplicationContextRunner()
                .withBean("smsSender", SmsSender.class, SmsSender::new, bd -> bd.setFallback(true))
                .withUserConfiguration(PushSender.class, ByParameterNameConsumer.class)
                .run(ctx -> assertThat(ctx.getBean(ByParameterNameConsumer.class).sender().channel()).isEqualTo("PUSH"));

        // brak @Primary i pasującej nazwy -> rozstrzyga @Priority (mniejsza liczba)
        new ApplicationContextRunner()
                .withUserConfiguration(RegularSender.class, UrgentSender.class, RenamedParameterConsumer.class)
                .run(ctx -> assertThat(ctx.getBean(RenamedParameterConsumer.class).sender().channel()).isEqualTo("URGENT"));
    }

    /** Metody @Bean w odwrotnej kolejności (push, sms, email). Klasa bez adnotacji — rejestruje ją test. */
    static class ReversedSendersConfig {
        @Bean
        NotificationSender push() {
            return new PushSender();
        }

        @Bean
        NotificationSender sms() {
            return new SmsSender();
        }

        @Bean
        NotificationSender email() {
            return new EmailSender();
        }
    }

    /** Wysyłacz awaryjny: {@code @Fallback} (bez {@code @Component} — rejestruje go test). */
    @Fallback
    static class BackupSender implements NotificationSender {
        @Override
        public String channel() {
            return "BACKUP";
        }

        @Override
        public String send(String recipient, String message) {
            return "[BACKUP] do " + recipient + ": " + message;
        }
    }

    /** {@code @Priority(1)} (priority = priorytet) — mniejsza liczba, więc wygrywa z RegularSender. */
    @Priority(1)
    static class UrgentSender implements NotificationSender {
        @Override
        public String channel() {
            return "URGENT";
        }

        @Override
        public String send(String recipient, String message) {
            return "[URGENT] do " + recipient + ": " + message;
        }
    }

    /** {@code @Priority(2)} — większa liczba, więc przegrywa z UrgentSender. */
    @Priority(2)
    static class RegularSender implements NotificationSender {
        @Override
        public String channel() {
            return "REGULAR";
        }

        @Override
        public String send(String recipient, String message) {
            return "[REGULAR] do " + recipient + ": " + message;
        }
    }

    private static Throwable rootCause(Throwable t) {
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t;
    }
}
