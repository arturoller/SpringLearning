package s22_extras.ext02_messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Producent (producer = nadawca) zdarzeń o zamówieniach — lekcja Ext02Messaging, sekcja 3.
 * KafkaTemplate (szablon Kafki) tworzy Boot z właściwości spring.kafka.producer.*.
 */
@Service
public class OrderEventPublisher {

    /** Nazwa tematu (topic = temat — „kanał” wiadomości w Kafce). */
    public static final String TOPIC = "sklep.zamowienia";

    private final KafkaTemplate<String, OrderPlaced> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, OrderPlaced> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Wysyła zdarzenie. Klucz = numer zamówienia: wszystkie wiadomości jednego zamówienia trafią do tej samej partycji,
     * więc zachowają kolejność. Wynik jest asynchroniczny (CompletableFuture) — send() NIE czeka na brokera.
     */
    public CompletableFuture<SendResult<String, OrderPlaced>> publish(OrderPlaced event) {
        return kafkaTemplate.send(TOPIC, event.orderId(), event);
    }
}
