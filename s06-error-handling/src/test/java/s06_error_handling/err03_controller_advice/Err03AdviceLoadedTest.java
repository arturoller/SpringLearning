package s06_error_handling.err03_controller_advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** Sekcja 1 lekcji Err03ControllerAdvice: co ładuje {@code @WebMvcTest(ProductController.class)}. */
@WebMvcTest(ProductController.class)
class Err03AdviceLoadedTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Sekcja 1: @WebMvcTest ładuje WSZYSTKIE beany @ControllerAdvice, ale tylko wskazany kontroler")
    void webMvcTestLoadsAdvices() {
        assertThat(context.getBeansOfType(ShopAdvice.class)).hasSize(1);
        assertThat(context.getBeansOfType(FallbackAdvice.class)).hasSize(1);
        assertThat(context.getBeansOfType(ProductController.class)).hasSize(1);
        assertThat(context.getBeansOfType(OrderController.class)).isEmpty();   // innych kontrolerów nie ma
    }
}
