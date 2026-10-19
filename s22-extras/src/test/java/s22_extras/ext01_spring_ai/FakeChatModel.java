package s22_extras.ext01_spring_ai;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ATRAPA modelu językowego (fake = atrapa) — lekcja Ext01SpringAi, sekcja 5. Żadnej sieci, żadnej losowości:
 * <ul>
 *   <li>{@link #willAnswer(String...)} — kolejka przygotowanych odpowiedzi (każde wywołanie zdejmuje jedną),</li>
 *   <li>{@link #prompts()} — każdy otrzymany Prompt, żeby test sprawdził, CO aplikacja wysłała.</li>
 * </ul>
 * ChatModel ma jedną metodę do zaimplementowania: {@code call(Prompt)}. Reszta (np. {@code call(String)}) to metody
 * domyślne interfejsu, które ostatecznie wołają właśnie ją.
 */
public class FakeChatModel implements ChatModel {

    /** Odpowiedź, gdy kolejka jest pusta — od razu widać w teście, że zabrakło przygotowanej odpowiedzi. */
    public static final String NO_ANSWER = "(atrapa: brak przygotowanej odpowiedzi)";

    private final Deque<String> answers = new ArrayDeque<>();
    private final List<Prompt> prompts = new CopyOnWriteArrayList<>();

    /** Dodaje odpowiedzi do kolejki (pierwsza podana = pierwsza zwrócona). */
    public synchronized FakeChatModel willAnswer(String... texts) {
        answers.addAll(List.of(texts));
        return this;
    }

    @Override
    public synchronized ChatResponse call(Prompt prompt) {
        prompts.add(prompt);
        String text = answers.isEmpty() ? NO_ANSWER : answers.poll();
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    public List<Prompt> prompts() {
        return List.copyOf(prompts);
    }

    public Prompt lastPrompt() {
        if (prompts.isEmpty()) {
            throw new IllegalStateException("Atrapa nie dostała jeszcze żadnego promptu");
        }
        return prompts.getLast();
    }

    /** Tekst wiadomości systemowej ostatniego promptu. */
    public String lastSystemText() {
        return lastPrompt().getSystemMessage().getText();
    }

    /** Tekst wiadomości użytkownika ostatniego promptu. */
    public String lastUserText() {
        return lastPrompt().getUserMessage().getText();
    }

    /** Czyści kolejkę odpowiedzi i zapamiętane prompty (atrapa jest beanem współdzielonym przez testy jednej klasy). */
    public synchronized void reset() {
        answers.clear();
        prompts.clear();
    }
}
