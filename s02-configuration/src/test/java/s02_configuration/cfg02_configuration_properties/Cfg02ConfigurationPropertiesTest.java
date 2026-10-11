package s02_configuration.cfg02_configuration_properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.unit.DataSize;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Cfg02ConfigurationProperties. Sekcje 2–3 i 7 korzystają z pełnej aplikacji ({@code @SpringBootTest},
 * profil cfg02 = plik application-cfg02.properties). Pozostałe używają lekkiego {@code ApplicationContextRunner}
 * (kontekst budowany w teście, bez skanowania pakietu) — tu wystarczy, bo sprawdzamy samo wiązanie.
 */
@SpringBootTest
@ActiveProfiles("cfg02")
class Cfg02ConfigurationPropertiesTest {

    @Autowired
    DeliveryProperties properties;

    @Autowired
    DeliveryService service;

    @Autowired
    ApplicationContext context;

    /** Klasa konfiguracyjna tylko dla testów: jawnie włącza rekord (bez @Configuration — skanowanie jej nie widzi). */
    @EnableConfigurationProperties(DeliveryProperties.class)
    static class EnableDelivery {
    }

    /** Rekord bez żadnych @DefaultValue — pokazuje, co daje brak klucza. Nie jest beanem; wiążemy go Binderem. */
    record NoDefaults(int number, String text, boolean flag, List<String> items, Map<String, String> map) {
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withUserConfiguration(EnableDelivery.class);
    }

    @Test
    @DisplayName("2. Rekord dostaje wartości z application-cfg02.properties przez konstruktor (polskie litery zapisane kodami unicode)")
    void recordBoundFromFile() {
        assertThat(properties.carrier()).isEqualTo("KurierExpress");
        assertThat(properties.timeout()).isEqualTo(Duration.ofSeconds(45));
        assertThat(properties.warehouse().city()).isEqualTo("Łódź");
        assertThat(properties.warehouse().street()).isEqualTo("Magazynowa 5");
        // rekord jest też beanem, który dostaje usługa
        assertThat(service.describe()).isEqualTo("KurierExpress z magazynu w Łódź");
    }

    @Test
    @DisplayName("3. Lista i mapa: lista po przecinku, mapa z kluczem w ostatnim członie nazwy")
    void listAndMap() {
        assertThat(properties.cities()).containsExactly("Warszawa", "Kraków", "Gdańsk");
        assertThat(properties.prices()).containsOnlyKeys("warszawa", "krakow");
        assertThat(properties.prices().get("krakow")).isEqualByComparingTo("15.00");
        assertThat(service.delivers("Kraków")).isTrue();
        assertThat(service.priceFor("warszawa")).isEqualByComparingTo(new BigDecimal("12.50"));

        // PUŁAPKA: polskie znaki w kluczu mapy są usuwane ("Kraków" → "Krakw")
        Binder binder = new Binder(new MapConfigurationPropertySource(Map.of("x.map.Kraków", "1")));
        assertThat(binder.bindOrCreate("x", Bindable.of(NoDefaults.class)).map()).containsOnlyKeys("Krakw");
    }

    @Test
    @DisplayName("4. @DefaultValue i konwersja: brakujące klucze dają wartości domyślne, a tekst zamienia się w Duration i DataSize")
    void defaultsAndConversion() {
        // plik nie ma max-parcels ani max-label-size → wartości z @DefaultValue
        assertThat(properties.maxParcels()).isEqualTo(5);
        assertThat(properties.maxLabelSize()).isEqualTo(DataSize.ofKilobytes(512));

        // lekki kontekst: żadnych kluczy → same wartości domyślne, a zagnieżdżony rekord i kolekcje istnieją (puste)
        runner().run(context -> {
            DeliveryProperties defaults = context.getBean(DeliveryProperties.class);
            assertThat(defaults.timeout()).isEqualTo(Duration.ofSeconds(30));
            assertThat(defaults.carrier()).isNull();
            assertThat(defaults.warehouse().city()).isEqualTo("Warszawa");
            assertThat(defaults.cities()).isEmpty();
            assertThat(defaults.prices()).isEmpty();
        });

        // własne wartości w innych jednostkach
        runner().withPropertyValues("cfg02.shop.delivery.timeout=2m", "cfg02.shop.delivery.max-label-size=2MB")
                .run(context -> {
                    DeliveryProperties custom = context.getBean(DeliveryProperties.class);
                    assertThat(custom.timeout()).isEqualTo(Duration.ofMinutes(2));
                    assertThat(custom.maxLabelSize()).isEqualTo(DataSize.ofMegabytes(2));
                });

        // BEZ @DefaultValue: brak klucza = null / 0 / false — także dla List i Map
        NoDefaults empty = new Binder(new MapConfigurationPropertySource(Map.of("inne.klucze", "1")))
                .bindOrCreate("x", Bindable.of(NoDefaults.class));
        assertThat(empty.number()).isZero();
        assertThat(empty.text()).isNull();
        assertThat(empty.flag()).isFalse();
        assertThat(empty.items()).isNull();
        assertThat(empty.map()).isNull();
    }

    @Test
    @DisplayName("5. Samo @ConfigurationProperties nie tworzy beana; potrzebne jest skanowanie albo @EnableConfigurationProperties")
    void enabling() {
        // pełna aplikacja ma bean dzięki @ConfigurationPropertiesScan w Cfg02Application
        assertThat(context.getBeanNamesForType(DeliveryProperties.class)).hasSize(1);

        // lekki kontekst bez włączenia: brak beana
        new ApplicationContextRunner().run(ctx ->
                assertThat(ctx.getBeanNamesForType(DeliveryProperties.class)).isEmpty());
        // z @EnableConfigurationProperties: bean jest
        runner().run(ctx -> assertThat(ctx.getBeanNamesForType(DeliveryProperties.class)).hasSize(1));
    }

    @Test
    @DisplayName("6. Zły typ wartości zatrzymuje start, a literówka w nazwie klucza jest cicho ignorowana")
    void bindingErrors() {
        runner().withPropertyValues("cfg02.shop.delivery.timeout=abc").run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(ctx.getStartupFailure()).hasStackTraceContaining("cfg02.shop.delivery.timeout");
        });

        runner().withPropertyValues("cfg02.shop.delivery.carier=DPD").run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(DeliveryProperties.class).carrier()).isNull();   // literówka: carier ≠ carrier
        });
    }

    @Test
    @DisplayName("7. Usługa z rekordem testuje się zwykłym new — bez Springa")
    void testWithoutSpring() {
        DeliveryProperties props = new DeliveryProperties("DHL", 3, Duration.ofSeconds(10), DataSize.ofBytes(100),
                new DeliveryProperties.Warehouse("Poznań", "Różana 1"), List.of("Poznań"),
                Map.of("poznan", new BigDecimal("9.99")));
        DeliveryService unit = new DeliveryService(props);

        assertThat(unit.delivers("Poznań")).isTrue();
        assertThat(unit.delivers("Gdynia")).isFalse();
        assertThat(unit.priceFor("poznan")).isEqualByComparingTo("9.99");
        assertThat(unit.describe()).isEqualTo("DHL z magazynu w Poznań");
    }
}
