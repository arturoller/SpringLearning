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
 * Konsument w grupie „faktury” — lekcja Ext02Messaging, sekcja 4. Ten sam temat co magazyn, ale INNA grupa:
 * obie grupy dostają KAŻDĄ wiadomość (każda grupa ma własną pozycję odczytu — offset).
 */
@Component
public class InvoiceListener {

    public static final String GROUP = "faktury";

    private static final Logger log = LoggerFactory.getLogger(InvoiceListener.class);

    private final List<ReceivedOrder> received = new CopyOnWriteArrayList<>();

    @KafkaListener(id = "ext02-faktury", topics = OrderEventPublisher.TOPIC, groupId = GROUP)
    public void onOrderPlaced(OrderPlaced event,
                              @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                              @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Faktury: wystawiam fakturę dla {} na {} zł", event.orderId(), event.total());
        received.add(new ReceivedOrder(GROUP, event, partition, offset));
    }

    public List<ReceivedOrder> received() {
        return List.copyOf(received);
    }
}
