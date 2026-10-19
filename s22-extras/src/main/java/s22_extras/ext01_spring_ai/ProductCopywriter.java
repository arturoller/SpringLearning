package s22_extras.ext01_spring_ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Copywriter sklepu oparty na modelu językowym (lekcja Ext01SpringAi, sekcje 3 i 4).
 *
 * <p>ChatClient (klient czatu) to „RestClient dla modeli AI”: płynne API do budowania promptu i odbierania odpowiedzi.
 * Boot daje bean {@code ChatClient.Builder} (zasięg prototype — każde wstrzyknięcie dostaje NOWY builder), a my
 * budujemy z niego własnego klienta z domyślną wiadomością systemową.</p>
 */
@Service
public class ProductCopywriter {

    /**
     * Wiadomość systemowa (system message) — „instrukcja stanowiskowa” dla modelu: kim jest, w jakim języku pisze,
     * czego mu nie wolno. Model dostaje ją przed KAŻDYM pytaniem tego klienta.
     */
    static final String SYSTEM_PROMPT = """
            Jesteś copywriterem polskiego sklepu internetowego. Piszesz po polsku, krótko i rzeczowo.
            Nie wymyślasz parametrów technicznych ani cen, których nie ma w danych produktu.""";

    private final ChatClient chatClient;

    public ProductCopywriter(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)   // defaultSystem = domyślna wiadomość systemowa
                .build();
    }

    /** Sekcja 3: odpowiedź jako zwykły tekst — {@code call().content()}. */
    public String shortDescription(CatalogProduct product) {
        return chatClient.prompt()
                .user(u -> u.text("Napisz jedno zdanie zachęty do zakupu produktu {name} z kategorii {category}.")
                        .param("name", product.name())            // param = parametr szablonu {name}
                        .param("category", product.category()))
                .call()                                            // call = wywołaj model (synchronicznie)
                .content();                                        // content = treść odpowiedzi jako String
    }

    /** Sekcja 4: odpowiedź jako rekord — {@code call().entity(ProductCard.class)}. */
    public ProductCard card(CatalogProduct product) {
        return chatClient.prompt()
                .user(u -> u.text("""
                                Przygotuj kartę produktu do sklepu.
                                Nazwa: {name}
                                Kategoria: {category}
                                Cena: {price} zł""")
                        .param("name", product.name())
                        .param("category", product.category())
                        .param("price", product.price()))
                .call()
                .entity(ProductCard.class);                        // entity = encja/obiekt — JSON z modelu → rekord
    }
}
