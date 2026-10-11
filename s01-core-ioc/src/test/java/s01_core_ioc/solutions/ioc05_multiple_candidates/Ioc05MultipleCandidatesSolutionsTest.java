package s01_core_ioc.solutions.ioc05_multiple_candidates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import s01_core_ioc.ioc05_multiple_candidates.AlertService;
import s01_core_ioc.ioc05_multiple_candidates.EmailSender;
import s01_core_ioc.ioc05_multiple_candidates.NotificationSender;
import s01_core_ioc.ioc05_multiple_candidates.PushSender;
import s01_core_ioc.ioc05_multiple_candidates.SmsSender;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Ioc05MultipleCandidatesExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Ioc05MultipleCandidatesSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: posortowane nazwy beanów z mapy")
    void solution1() {
        Map<String, NotificationSender> map = new LinkedHashMap<>();
        map.put("smsSender", new SmsSender());
        map.put("pushSender", new PushSender());
        map.put("emailSender", new EmailSender());
        assertThat(Ioc05MultipleCandidatesSolutions.solution1(map)).containsExactly("emailSender", "pushSender", "smsSender");
    }

    @Test
    @DisplayName("Wzorzec 2: @Primary na wysyłaczu e-mail")
    void solution2() {
        new ApplicationContextRunner().withUserConfiguration(Ioc05Exercise2ConfigSolution.class).run(ctx ->
                assertThat(ctx.getBean(NotificationSender.class)).isInstanceOf(EmailSender.class));
    }

    @Test
    @DisplayName("Wzorzec 3: AlertService wysyła SMS-em dzięki @Qualifier")
    void solution3() {
        new ApplicationContextRunner()
                .withBean("smsSender", NotificationSender.class, SmsSender::new)
                .withBean("pushSender", NotificationSender.class, PushSender::new)
                .withUserConfiguration(Ioc05Exercise3ConfigSolution.class)
                .run(ctx -> assertThat(ctx.getBean(AlertService.class).alert("Brak towaru")).startsWith("[SMS]"));
    }
}
