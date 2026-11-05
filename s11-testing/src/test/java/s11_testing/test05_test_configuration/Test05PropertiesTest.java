package s11_testing.test05_test_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 2 lekcji Test05TestConfiguration: skąd test bierze właściwości i KTO WYGRYWA, gdy ta sama właściwość
 * ({@code test05.shop.name}) jest ustawiona w kilku miejscach. Każda klasa {@code @Nested} dokłada jedno źródło
 * więcej („drabina” priorytetów) i ma przez to WŁASNY kontekst (inny klucz pamięci podręcznej kontekstów).
 * Klasa zewnętrzna nie ma adnotacji Springa — każda zagnieżdżona ma komplet własnych.
 */
class Test05PropertiesTest {

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles("test05")
    class ProfileOnly {

        @Autowired
        ShopInfo shop;

        @Test
        @DisplayName("Sekcja 2, szczebel 1: sam profil test05 → wartość z application-test05.properties (src/main/resources)")
        void profileFile() {
            assertThat(shop.name()).isEqualTo("Sklep z profilu test05");
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles({"test05", "test05extra"})
    class TwoProfiles {

        @Autowired
        ShopInfo shop;

        @Autowired
        Environment environment;

        @Test
        @DisplayName("Sekcja 2, szczebel 2: drugi profil z src/test/resources wygrywa z pierwszym (później wymieniony = ważniejszy)")
        void laterProfileWins() {
            assertThat(environment.getActiveProfiles()).containsExactly("test05", "test05extra");
            assertThat(shop.name()).isEqualTo("Sklep z profilu testowego test05extra");
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles({"test05", "test05extra"})
    @TestPropertySource(locations = "classpath:test05/override.properties") // locations = położenie plików
    class TestPropertySourceFile {

        @Autowired
        ShopInfo shop;

        @Test
        @DisplayName("Sekcja 2, szczebel 3: plik z @TestPropertySource(locations) wygrywa z plikami profili")
        void testPropertySourceFileWins() {
            assertThat(shop.name()).isEqualTo("Sklep z pliku @TestPropertySource");
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
            properties = "test05.shop.name=Sklep z @SpringBootTest(properties)")
    @ActiveProfiles({"test05", "test05extra"})
    @TestPropertySource(locations = "classpath:test05/override.properties")
    class InlineProperties {

        @Autowired
        ShopInfo shop;

        @Test
        @DisplayName("Sekcja 2, szczebel 4: właściwości wpisane w adnotację (@SpringBootTest(properties)) wygrywają z każdym plikiem")
        void inlinePropertiesWin() {
            assertThat(shop.name()).isEqualTo("Sklep z @SpringBootTest(properties)");
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
            properties = "test05.shop.name=Sklep z @SpringBootTest(properties)")
    @ActiveProfiles({"test05", "test05extra"})
    @TestPropertySource(properties = "test05.shop.name=Sklep z @TestPropertySource(properties)")
    class BothInline {

        @Autowired
        ShopInfo shop;

        @Test
        @DisplayName("Sekcja 2: @SpringBootTest(properties) i @TestPropertySource(properties) naraz — wygrywa @TestPropertySource")
        void testPropertySourceInlineWins() {
            assertThat(shop.name()).isEqualTo("Sklep z @TestPropertySource(properties)");
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
            properties = "test05.shop.name=Sklep z @SpringBootTest(properties)")
    @ActiveProfiles({"test05", "test05extra"})
    @TestPropertySource(properties = "test05.shop.name=Sklep z @TestPropertySource(properties)")
    class DynamicOnTop {

        @DynamicPropertySource
        static void shopName(DynamicPropertyRegistry registry) {
            registry.add("test05.shop.name", () -> "Sklep z @DynamicPropertySource");
        }

        @Autowired
        ShopInfo shop;

        @Test
        @DisplayName("Sekcja 2, szczebel 5: @DynamicPropertySource wygrywa ze WSZYSTKIM")
        void dynamicWins() {
            assertThat(shop.name()).isEqualTo("Sklep z @DynamicPropertySource");
        }
    }
}
