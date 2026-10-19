package s22_extras.solutions.ext02_messaging;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import s22_extras.ext02_messaging.OrderEventPublisher;
import s22_extras.ext02_messaging.OrderPlaced;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Rozwiązania wzorcowe ćwiczeń lekcji Ext02Messaging — zajrzyj dopiero po własnej próbie! */
public final class Ext02MessagingSolutions {

    private Ext02MessagingSolutions() {
    }

    static ProducerRecord<String, OrderPlaced> solution1(OrderPlaced event) {
        ProducerRecord<String, OrderPlaced> record = new ProducerRecord<>(OrderEventPublisher.TOPIC, event.orderId(), event);
        record.headers().add("zrodlo", "sklep-www".getBytes(StandardCharsets.UTF_8));
        return record;
    }

    static JacksonJsonDeserializer<OrderPlaced> solution2() {
        // Klasa docelowa podana wprost + ignorowanie nagłówków typu: nie ma czego „ufać” — zawsze OrderPlaced
        return new JacksonJsonDeserializer<>(OrderPlaced.class).ignoreTypeHeaders();
    }

    static Map<Long, BigDecimal> solution3(List<OrderPlaced> deliveries) {
        Set<String> processed = new HashSet<>();
        Map<Long, BigDecimal> totals = new TreeMap<>();          // TreeMap = klucze posortowane rosnąco
        for (OrderPlaced event : deliveries) {
            if (processed.add(event.orderId())) {                // add zwraca false, gdy orderId już było — duplikat
                totals.merge(event.customerId(), event.total(), BigDecimal::add);
            }
        }
        return totals;
    }
}
