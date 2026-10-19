package s04_web_rest.rest04_json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import s04_web_rest.rest04_json.JsonModels.Customer;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 6 lekcji Rest04Json: dostrajać (customizer) czy podmieniać (własny bean JsonMapper)?
 * ApplicationContextRunner (uruchamiacz kontekstu) startuje mały kontekst z SAMĄ autokonfiguracją Jacksona —
 * szybko i bez serwera. W obu testach ustawiamy tę samą właściwość spring.jackson.default-property-inclusion=non_null.
 */
class Rest04JsonOwnMapperTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
            .withPropertyValues("spring.jackson.default-property-inclusion=non_null");

    private static final Customer MARIA = new Customer(2, "Maria Nowak", null);

    @Test
    @DisplayName("6. Mapper Boota czyta spring.jackson.* — pole null znika")
    void bootMapperUsesProperties() {
        runner.run(context -> assertThat(context.getBean(JsonMapper.class).writeValueAsString(MARIA))
                .isEqualTo("{\"id\":2,\"name\":\"Maria Nowak\"}"));
    }

    @Test
    @DisplayName("6. PUŁAPKA: własny bean JsonMapper wyłącza mapper Boota — właściwości spring.jackson.* przestają działać")
    void ownMapperIgnoresProperties() {
        runner.withUserConfiguration(OwnMapperConfig.class)
                .run(context -> assertThat(context.getBean(JsonMapper.class).writeValueAsString(MARIA))
                        .isEqualTo("{\"id\":2,\"name\":\"Maria Nowak\",\"email\":null}"));
    }

    @Test
    @DisplayName("6. Customizer DOSTRAJA mapper Boota — właściwości nadal działają, a do tego nasza zmiana")
    void customizerKeepsProperties() {
        runner.withUserConfiguration(JsonConfig.class)
                .run(context -> {
                    JsonMapper mapper = context.getBean(JsonMapper.class);
                    assertThat(mapper.writeValueAsString(MARIA)).isEqualTo("{\"id\":2,\"name\":\"Maria Nowak\"}");
                    assertThat(mapper.readValue("\"ksiazki\"", JsonModels.Category.class))
                            .isEqualTo(JsonModels.Category.KSIAZKI);
                });
    }

    @Test
    @DisplayName("1. spring.jackson.use-jackson2-defaults=true — zachowanie jak w Boot 3: enum po nazwie, brak int → 0")
    void jackson2Defaults() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withPropertyValues("spring.jackson.use-jackson2-defaults=true")
                .run(context -> {
                    JsonMapper mapper = context.getBean(JsonMapper.class);
                    // enum z własnym toString() znowu po NAZWIE stałej (Jackson 3 domyślnie: "książki")
                    assertThat(mapper.writeValueAsString(JsonModels.PrettyCategory.KSIAZKI)).isEqualTo("\"KSIAZKI\"");
                    // brakujące pole int znowu po cichu = 0 (Jackson 3 domyślnie: błąd)
                    assertThat(mapper.readValue("{\"sku\":\"SPO-001\"}", JsonModels.NewProduct.class).stock()).isZero();
                    // daty nadal jako tekst ISO — tak było też w Boot 3
                    assertThat(mapper.writeValueAsString(java.time.LocalDate.of(2026, 10, 19))).isEqualTo("\"2026-10-19\"");
                });
    }

    /** Celowo źle: własny JsonMapper zbudowany od zera — Boot się wycofuje (@ConditionalOnMissingBean). */
    @Configuration(proxyBeanMethods = false)
    static class OwnMapperConfig {

        @Bean
        JsonMapper myJsonMapper() {
            return JsonMapper.builder().build();   // własny mapper: domyślne ustawienia Jacksona, bez spring.jackson.*
        }
    }
}
