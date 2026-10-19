package s22_extras.ext02_messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Konsument (consumer = odbiorca) w grupie „magazyn” — lekcja Ext02Messaging, sekcja 4. Magazyn kompletuje paczkę.
 * {@code @KafkaListener} (słuchacz Kafki): Spring tworzy kontener, który w osobnym wątku pobiera (poll) wiadomości
 * z brokera i woła tę metodę dla każdej z nich.
 */
@Component
public class WarehouseListener {

    public static final String GROUP = "magazyn";

    private static final Logger log = LoggerFactory.getLogger(WarehouseListener.class);

    /** Lista bezpieczna dla wątków — pisze wątek kontenera Kafki, czyta wątek testu albo kontrolera. */
    private final List<ReceivedOrder> received = new CopyOnWriteArrayList<>();

    @KafkaListener(id = "ext02-magazyn", topics = OrderEventPublisher.TOPIC, groupId = GROUP)
    public void onOrderPlaced(OrderPlaced event,
                              @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,   // nagłówek: numer partycji
                              @Header(KafkaHeaders.OFFSET) long offset) {               // nagłówek: pozycja w partycji
        log.info("Magazyn: kompletuję {} (partycja {}, offset {})", event.orderId(), partition, offset);
        received.add(new ReceivedOrder(GROUP, event, partition, offset));
    }

    public List<ReceivedOrder> received() {
        return List.copyOf(received);
    }
}
