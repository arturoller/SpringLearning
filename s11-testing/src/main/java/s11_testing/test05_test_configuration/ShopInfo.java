package s11_testing.test05_test_configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Nazwa sklepu z właściwości {@code test05.shop.name}. Lekcja ustawia tę jedną właściwość w wielu miejscach
 * naraz (profil, plik testowy, {@code @TestPropertySource}, {@code @SpringBootTest(properties)},
 * {@code @DynamicPropertySource}) i sprawdza, które źródło wygrywa.
 */
@Component
public class ShopInfo {

    private final String name;

    public ShopInfo(@Value("${test05.shop.name}") String name) { // @Value = wartość wstawiona z właściwości
        this.name = name;
    }

    public String name() {
        return name;
    }
}
