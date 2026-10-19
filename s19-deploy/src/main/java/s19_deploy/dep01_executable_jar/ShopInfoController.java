package s19_deploy.dep01_executable_jar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Maleńki kontroler „sklepu” — wystarczy, żeby było do czego wysłać zapytanie po uruchomieniu JAR-a. */
@RestController
public class ShopInfoController {

    private final String shopName;
    private final String greeting;

    public ShopInfoController(@Value("${shop.name:Sklep domyślny}") String shopName,
                              @Value("${shop.greeting:Witamy}") String greeting) {
        this.shopName = shopName;
        this.greeting = greeting;
    }

    /** Wartości {@code shop.*} można podać z linii poleceń: {@code java -jar app.jar --shop.name=Mojsklep}. */
    @GetMapping("/api/shop")
    public Map<String, String> shop() {
        return Map.of("name", shopName, "greeting", greeting);
    }

    @GetMapping("/api/products")
    public List<String> products() {
        return List.of("Kawa ziarnista 1kg", "Czekolada gorzka", "Czysty kod");
    }
}
