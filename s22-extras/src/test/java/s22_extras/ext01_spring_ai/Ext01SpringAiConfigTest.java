package s22_extras.ext01_spring_ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lekcja Ext01SpringAi, sekcja 2: profil ext01 włącza Ollamę. Test NIE łączy się z Ollamą — sprawdza tylko, że kontekst
 * wstaje bez niej, a bean modelu jest gotowy do użycia.
 */
@SpringBootTest
@ActiveProfiles("ext01")   // profil aplikacji: spring.ai.model.chat=ollama
class Ext01SpringAiConfigTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    Environment environment;

    @Test
    @DisplayName("2. Profil ext01: kontekst wstaje BEZ działającej Ollamy, OllamaChatModel jest jedynym ChatModel")
    void ollamaModelCreatedWithoutConnecting() {
        assertThat(context.getBeanNamesForType(ChatModel.class)).containsExactly("ollamaChatModel");
        assertThat(context.getBean(ChatModel.class)).isInstanceOf(OllamaChatModel.class);
        assertThat(environment.getProperty("spring.ai.ollama.chat.model")).isEqualTo("llama3.2:1b");
        assertThat(environment.getProperty("spring.ai.ollama.init.pull-model-strategy"))
                .as("brak właściwości = domyślne never: nie pobieraj modelu przy starcie, więc nie łącz się z Ollamą")
                .isNull();
    }
}
