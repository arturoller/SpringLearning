package s22_extras.ext01_spring_ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lekcja Ext01SpringAi, sekcja 5 (PUŁAPKA i pytanie kontrolne 2): atrapa modelu obok autokonfiguracji Ollamy.
 * Zwykły test JUnit, który sam startuje małą aplikację ({@code SpringApplicationBuilder}) — bez serwera WWW.
 */
class Ext01SpringAiTwoModelsTest {

    @Test
    @DisplayName("5. PUŁAPKA: atrapa obok Ollamy (bez spring.ai.model.chat=none) → dwa ChatModel, builder ChatClienta nie powstaje")
    void twoChatModelsBreakChatClientBuilder() {
        // Argument z linii poleceń ma pierwszeństwo przed application.properties modułu (tam jest none)
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TwoModelsConfig.class)
                .web(WebApplicationType.NONE)
                .run("--spring.ai.model.chat=ollama")) {

            assertThat(context.getBeanNamesForType(ChatModel.class))
                    .containsExactlyInAnyOrder("fakeChatModel", "ollamaChatModel");
            assertThatThrownBy(() -> context.getBean(ChatClient.Builder.class))
                    .isInstanceOf(UnsatisfiedDependencyException.class)
                    .hasMessageContaining("No qualifying bean of type 'org.springframework.ai.chat.model.ChatModel' available:"
                            + " expected single matching bean but found 2: fakeChatModel,ollamaChatModel");
        }
    }

    /**
     * Minimalna aplikacja: autokonfiguracja + atrapa modelu, bez skanowania pakietu lekcji. Klasa zagnieżdżona w klasie
     * testowej — skanowanie Ext01Application jej NIE znajdzie (Boot pomija klasy testowe i ich klasy wewnętrzne).
     */
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class TwoModelsConfig {
        @Bean
        FakeChatModel fakeChatModel() {
            return new FakeChatModel();
        }
    }
}
