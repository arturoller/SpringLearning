package s11_testing.solutions.test05_test_configuration;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import s11_testing.test05_test_configuration.ClockConfig;
import s11_testing.test05_test_configuration.NewsletterConfig;
import s11_testing.test05_test_configuration.PriceClient;
import s11_testing.test05_test_configuration.PromotionService;
import s11_testing.test05_test_configuration.ShopInfo;
import s11_testing.test05_test_configuration.ViewCounter;

/**
 * Konfiguracja testów rozwiązań Test05 ({@code @SpringBootTest} szuka {@code @SpringBootConfiguration} w górę
 * pakietów). Leży w pakiecie solutions.test05_test_configuration (rodzeństwo lekcji), więc skanuje tylko ten pakiet —
 * beany lekcji dołączamy jawnie przez {@code @Import}.
 */
@SpringBootApplication
@Import({ClockConfig.class, PromotionService.class, ShopInfo.class, PriceClient.class, ViewCounter.class,
        NewsletterConfig.class})
public class Test05SolutionsApplication {
}
