package s06_error_handling.err03_controller_advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.ControllerAdvice;

import static org.assertj.core.api.Assertions.assertThat;

/** Izolacja lekcji: kontekst Err03Application zawiera tylko advice z własnego pakietu. */
@SpringBootTest
class Err03AdviceIsolationTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("7. Advice tej lekcji (co najmniej trzy) pochodzą z pakietu lekcji; kontrolerów z Err02 w tym kontekście nie ma")
    void adviceBelongsToLessonOnly() {
        assertThat(context.getBeansWithAnnotation(ControllerAdvice.class).values())
                .extracting(bean -> bean.getClass().getSimpleName())
                .contains("GlobalExceptionHandler", "PaymentAdvice", "OrderAdvice");
        assertThat(context.getBeansWithAnnotation(ControllerAdvice.class).values())
                .allSatisfy(bean -> assertThat(bean.getClass().getPackageName()).isEqualTo("s06_error_handling.err03_controller_advice"));
        assertThat(context.getBeanNamesForType(s06_error_handling.err02_exception_handler.CatalogController.class)).isEmpty();
    }
}
