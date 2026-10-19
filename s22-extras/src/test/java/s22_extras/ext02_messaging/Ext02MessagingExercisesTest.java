package s22_extras.ext02_messaging;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Ext02Messaging. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). Bez brokera. */
@Tag("cwiczenie")
class Ext02MessagingExercisesTest {

    static final OrderPlaced ZAM_001 = new OrderPlaced("ZAM-001", 1, List.of("ELE-003"), new BigDecimal("349.90"));

    @Test
    @DisplayName("Ćwiczenie 1: ProducerRecord z tematem, kluczem, wartością i nagłówkiem zrodlo")
    void exercise1() {
        ProducerRecord<String, OrderPlaced> record = Ext02Messaging.exercise1(ZAM_001);

        assertThat(record.topic()).isEqualTo("sklep.zamowienia");
        assertThat(record.key()).isEqualTo("ZAM-001");
        assertThat(record.value()).isEqualTo(ZAM_001);
        assertThat(new String(record.headers().lastHeader("zrodlo").value(), StandardCharsets.UTF_8)).isEqualTo("sklep-www");
    }

    @Test
    @DisplayName("Ćwiczenie 2: deserializator ignoruje obcy nagłówek typu i działa bez nagłówków")
    void exercise2() {
        RecordHeaders foreignHeaders = new RecordHeaders();
        byte[] bytes = new JacksonJsonSerializer<OrderPlaced>().noTypeInfo().serialize("t", foreignHeaders, ZAM_001);
        foreignHeaders.add("__TypeId__", "pl.sklep.zamowienia.Zamowienie".getBytes(StandardCharsets.UTF_8));

        JacksonJsonDeserializer<OrderPlaced> deserializer = Ext02Messaging.exercise2();

        assertThat(deserializer.deserialize("t", foreignHeaders, bytes)).isEqualTo(ZAM_001);
        assertThat(deserializer.deserialize("t", new RecordHeaders(), bytes)).isEqualTo(ZAM_001);
    }

    @Test
    @DisplayName("Ćwiczenie 3: konsument idempotentny — każde orderId liczone raz, sumy per klient posortowane")
    void exercise3() {
        List<OrderPlaced> deliveries = List.of(
                new OrderPlaced("ZAM-001", 1, List.of("ELE-003"), new BigDecimal("349.90")),
                new OrderPlaced("ZAM-002", 4, List.of("SPO-001"), new BigDecimal("64.99")),
                new OrderPlaced("ZAM-001", 1, List.of("ELE-003"), new BigDecimal("349.90")),   // duplikat
                new OrderPlaced("ZAM-003", 1, List.of("KSI-001"), new BigDecimal("79.00")),
                new OrderPlaced("ZAM-002", 4, List.of("SPO-001"), new BigDecimal("64.99")));   // duplikat

        Map<Long, BigDecimal> totals = Ext02Messaging.exercise3(deliveries);

        assertThat(totals).containsExactly(
                Map.entry(1L, new BigDecimal("428.90")),
                Map.entry(4L, new BigDecimal("64.99")));
        assertThat(Ext02Messaging.exercise3(List.of())).isEmpty();
    }
}
