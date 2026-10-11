package s01_core_ioc.ioc05_multiple_candidates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Ioc05MultipleCandidates. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Ioc05MultipleCandidatesExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: posortowane nazwy beanów z mapy")
    void exercise1() {
        Map<String, NotificationSender> map = new LinkedHashMap<>();
        map.put("smsSender", new SmsSender());
        map.put("pushSender", new PushSender());
        map.put("emailSender", new EmailSender());
        assertThat(Ioc05MultipleCandidates.exercise1(map)).containsExactly("emailSender", "pushSender", "smsSender");
    }

    @Test
    @DisplayName("Ćwiczenie 2: @Primary na wysyłaczu e-mail")
    void exercise2() {
        new ApplicationContextRunner().withUserConfiguration(Ioc05Exercise2Config.class).run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(NotificationSender.class)).isInstanceOf(EmailSender.class);
        });
    }

    @Test
    @DisplayName("Ćwiczenie 3: AlertService wysyła SMS-em dzięki @Qualifier")
    void exercise3() {
        new ApplicationContextRunner()
                .withBean("smsSender", NotificationSender.class, SmsSender::new)
                .withBean("pushSender", NotificationSender.class, PushSender::new)
                .withUserConfiguration(Ioc05Exercise3Config.class)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBean(AlertService.class).alert("Brak towaru")).startsWith("[SMS]");
                });
    }
}
