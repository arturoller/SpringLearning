package s03_boot_basics.boot01_auto_configuration;

import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/**
 * Serwis zamieniający produkt na JSON. Potrzebuje {@link JsonMapper} (mapper JSON z Jacksona 3) — i dostaje go,
 * choć w tej lekcji nikt nie napisał {@code @Bean JsonMapper}. Bean tworzy autokonfiguracja Jacksona
 * ({@code JacksonAutoConfiguration}, bean o nazwie {@code jacksonJsonMapper}).
 *
 * <p>Jackson 3: klasy są w pakietach {@code tools.jackson.*} (dawniej {@code com.fasterxml.jackson.*}), a metody
 * rzucają niekontrolowany {@code JacksonException} — nie trzeba {@code try/catch} jak przy dawnym
 * {@code JsonProcessingException}.</p>
 */
@Service
public class ProductJsonService {

    private final JsonMapper jsonMapper;

    public ProductJsonService(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public String toJson(Product product) {
        return jsonMapper.writeValueAsString(product);
    }

    /** Mapper, który dostał serwis — test sprawdza, że to ten sam obiekt co bean od Boota. */
    public JsonMapper mapper() {
        return jsonMapper;
    }
}
