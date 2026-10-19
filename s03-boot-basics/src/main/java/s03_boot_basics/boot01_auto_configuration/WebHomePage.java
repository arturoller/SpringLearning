package s03_boot_basics.boot01_auto_configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;

/**
 * Komponent, który ma sens tylko w aplikacji webowej (strona główna sklepu).
 * {@code @ConditionalOnWebApplication} (warunkowo: tylko w aplikacji webowej) — w tym module nie ma serwera WWW,
 * więc bean NIE powstaje, a raport warunków zapisuje, dlaczego. Warunki Boota działają także na Twoich klasach,
 * nie tylko w autokonfiguracjach.
 */
@Component
@ConditionalOnWebApplication
public class WebHomePage {

    public String title() {
        return "Sklep SpringLearning — strona główna";
    }
}
