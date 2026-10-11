package s01_core_ioc.ioc05_multiple_candidates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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

    private static Throwable rootCause(Throwable t) {
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t;
    }
}
