package s05_validation.val05_service_validation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Rejestruje {@link ShopProperties}. Sama adnotacja {@code @EnableConfigurationProperties} nie jest "stereotypem", więc
 * skanowanie pakietów (Val05Application) tej klasy nie podejmie — używa jej jawnie tylko {@code ApplicationContextRunner}.
 */
@EnableConfigurationProperties(ShopProperties.class)
public class ShopPropertiesConfig {
}
