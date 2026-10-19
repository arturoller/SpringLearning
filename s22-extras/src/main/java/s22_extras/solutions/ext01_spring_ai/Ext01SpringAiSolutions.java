package s22_extras.solutions.ext01_spring_ai;

import org.springframework.ai.chat.client.ChatClient;
import s22_extras.ext01_spring_ai.CatalogProduct;
import s22_extras.ext01_spring_ai.Ext01SpringAi.PriceCheck;

import java.util.Objects;
import java.util.regex.Pattern;

/** Rozwiązania wzorcowe ćwiczeń lekcji Ext01SpringAi — zajrzyj dopiero po własnej próbie! */
public final class Ext01SpringAiSolutions {

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    /** PESEL: dokładnie 11 cyfr jako osobne słowo. */
    private static final Pattern PESEL = Pattern.compile("\\b\\d{11}\\b");
    /** Telefon: opcjonalnie +48, potem 3 grupy po 3 cyfry oddzielone spacją, myślnikiem albo niczym. */
    private static final Pattern PHONE = Pattern.compile("(\\+48 ?)?\\b\\d{3}[ -]?\\d{3}[ -]?\\d{3}\\b");

    private Ext01SpringAiSolutions() {
    }

    static String solution1(ChatClient chatClient, String productName) {
        String answer = chatClient.prompt()
                .system("Jesteś copywriterem sklepu. Odpowiadasz po polsku, jednym zdaniem.")
                .user("Napisz slogan dla produktu: " + productName)   // bez param(...) — szablon nie jest renderowany
                .call()
                .content();
        return answer == null ? "" : answer.strip();
    }

    static PriceCheck solution2(ChatClient chatClient, CatalogProduct product) {
        PriceCheck check = chatClient.prompt()
                .user(u -> u.text("Zaproponuj cenę produktu. SKU: {sku}, nazwa: {name}, obecna cena: {price} zł.")
                        .param("sku", product.sku())
                        .param("name", product.name())
                        .param("price", product.price()))
                .call()
                .entity(PriceCheck.class);
        if (check == null || check.suggestedPrice() == null || check.suggestedPrice().signum() <= 0) {
            throw new IllegalStateException("Model zwrócił niepoprawną cenę");
        }
        if (!Objects.equals(check.sku(), product.sku())) {
            throw new IllegalStateException("Model pomylił produkt");
        }
        return check;
    }

    static String solution3(String text) {
        String masked = EMAIL.matcher(text).replaceAll("[EMAIL]");
        masked = PESEL.matcher(masked).replaceAll("[PESEL]");     // najpierw 11 cyfr, żeby telefon nie „zjadł” 9 z nich
        return PHONE.matcher(masked).replaceAll("[TELEFON]");
    }
}
