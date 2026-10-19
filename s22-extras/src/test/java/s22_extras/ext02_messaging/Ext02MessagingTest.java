package s22_extras.ext02_messaging;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lekcja Ext02Messaging, sekcja 5: serializacja JSON wiadomości — BEZ brokera (zwykłe testy jednostkowe, biegną zawsze).
 * Serializator i deserializator to zwykłe klasy Kafki: bajty ↔ obiekt.
 */
class Ext02MessagingTest {

    static final OrderPlaced ZAM_001 = new OrderPlaced("ZAM-001", 1, List.of("ELE-003"), new BigDecimal("349.90"));

    /** Konfiguracja jak w application-ext02.properties (spring.kafka.consumer.properties.spring.json.trusted.packages). */
    static final Map<String, Object> TRUSTED = Map.of("spring.json.trusted.packages", "s22_extras.ext02_messaging");

    @Test
    @DisplayName("5. JacksonJsonSerializer: JSON + nagłówek __TypeId__ z pełną nazwą klasy")
    void serializerWritesJsonAndTypeHeader() {
        RecordHeaders headers = new RecordHeaders();

        byte[] bytes = new JacksonJsonSerializer<OrderPlaced>().serialize(OrderEventPublisher.TOPIC, headers, ZAM_001);

        assertThat(new String(bytes, StandardCharsets.UTF_8))
                .isEqualTo("{\"orderId\":\"ZAM-001\",\"customerId\":1,\"skus\":[\"ELE-003\"],\"total\":349.90}");
        Header typeId = headers.lastHeader("__TypeId__");
        assertThat(new String(typeId.value(), StandardCharsets.UTF_8)).isEqualTo("s22_extras.ext02_messaging.OrderPlaced");
    }

    @Test
    @DisplayName("5. JacksonJsonDeserializer z zaufanym pakietem: bajty + nagłówek → ten sam rekord")
    void roundTripWithTrustedPackage() {
        RecordHeaders headers = new RecordHeaders();
        byte[] bytes = new JacksonJsonSerializer<OrderPlaced>().serialize(OrderEventPublisher.TOPIC, headers, ZAM_001);
        JacksonJsonDeserializer<OrderPlaced> deserializer = new JacksonJsonDeserializer<>();
        deserializer.configure(TRUSTED, false);   // false = konfiguracja dla WARTOŚCI (nie klucza)

        assertThat(deserializer.deserialize(OrderEventPublisher.TOPIC, headers, bytes)).isEqualTo(ZAM_001);
    }

    @Test
    @DisplayName("5. PUŁAPKA: brak spring.json.trusted.packages → IllegalArgumentException (pytanie 3)")
    void untrustedPackage() {
        RecordHeaders headers = new RecordHeaders();
        byte[] bytes = new JacksonJsonSerializer<OrderPlaced>().serialize(OrderEventPublisher.TOPIC, headers, ZAM_001);
        JacksonJsonDeserializer<OrderPlaced> deserializer = new JacksonJsonDeserializer<>();
        deserializer.configure(Map.of(), false);

        assertThatThrownBy(() -> deserializer.deserialize(OrderEventPublisher.TOPIC, headers, bytes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("The class 's22_extras.ext02_messaging.OrderPlaced' is not in the trusted packages:"
                        + " [java.util, java.lang]. If you believe this class is safe to deserialize, please provide its name.");
    }

    @Test
    @DisplayName("5. Brak nagłówka typu i brak typu domyślnego → IllegalStateException")
    void noTypeInformation() {
        byte[] bytes = new JacksonJsonSerializer<OrderPlaced>().serialize(OrderEventPublisher.TOPIC, new RecordHeaders(), ZAM_001);
        JacksonJsonDeserializer<OrderPlaced> deserializer = new JacksonJsonDeserializer<>();
        deserializer.configure(TRUSTED, false);

        assertThatThrownBy(() -> deserializer.deserialize(OrderEventPublisher.TOPIC, new RecordHeaders(), bytes))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No type information in headers and no default type provided");
    }

    @Test
    @DisplayName("5. PUŁAPKA: stary JsonSerializer (Jackson 2) w Boot 4 → NoClassDefFoundError (pytanie 4)")
    void oldJsonSerializerNeedsJackson2() {
        // Klasa wczytywana po nazwie — tak jak robi to Kafka z właściwości value-serializer
        assertThatThrownBy(() -> Class.forName("org.springframework.kafka.support.serializer.JsonSerializer")
                .getDeclaredConstructor().newInstance())
                .isInstanceOf(NoClassDefFoundError.class)
                // brakująca klasa Jacksona 2 — np. com/fasterxml/jackson/databind/JavaType albo
                // com/fasterxml/jackson/core/type/TypeReference (zależy, którą JVM sprawdzi najpierw)
                .hasMessageStartingWith("com/fasterxml/jackson/");
    }
}
