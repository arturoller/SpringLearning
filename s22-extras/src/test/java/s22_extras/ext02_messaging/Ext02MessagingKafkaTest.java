package s22_extras.ext02_messaging;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.TopicDescription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Lekcja Ext02Messaging, sekcje 2–4 i 6: prawdziwa Kafka w kontenerze Dockera na czas testów tej klasy.
 * Bez Dockera klasa jest POMIJANA ({@code disabledWithoutDocker = true}), więc {@code ./mvnw test} przechodzi u każdego.
 * Każdy test używa własnych numerów zamówień — słuchacze są wspólni dla całej klasy (jeden kontekst).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("ext02")
class Ext02MessagingKafkaTest {

    /**
     * Obraz z JAWNYM tagiem (nigdy „latest”). Zwykła Kafka na JVM. Wariant apache/kafka-native (program natywny GraalVM)
     * startuje szybciej, ale u nas raz padł przy starcie z segfaultem — test ma być stabilny, więc JVM.
     */
    static final String KAFKA_IMAGE = "apache/kafka:4.2.0";

    @Container
    @ServiceConnection   // Boot podstawia spring.kafka.bootstrap-servers adresem kontenera
    static KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE);

    /** Limit czekania na słuchacza (wątek kontenera Kafki) — to nie asercja czasu, tylko „najdłużej tyle czekamy”. */
    static final Duration WAIT = Duration.ofSeconds(30);

    @Autowired
    OrderEventPublisher publisher;

    @Autowired
    WarehouseListener warehouse;

    @Autowired
    InvoiceListener invoices;

    @Autowired
    KafkaAdmin kafkaAdmin;

    static OrderPlaced order(String orderId) {
        return new OrderPlaced(orderId, 1, List.of("ELE-003"), new BigDecimal("349.90"));
    }

    static List<ReceivedOrder> withOrderId(List<ReceivedOrder> received, String orderId) {
        return received.stream().filter(r -> r.event().orderId().equals(orderId)).toList();
    }

    @Test
    @DisplayName("2. Bean NewTopic: KafkaAdmin założył temat sklep.zamowienia z 3 partycjami")
    void topicCreatedWithThreePartitions() throws Exception {
        try (AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            TopicDescription topic = client.describeTopics(List.of(OrderEventPublisher.TOPIC))
                    .allTopicNames().get(10, TimeUnit.SECONDS)
                    .get(OrderEventPublisher.TOPIC);

            assertThat(topic.partitions()).hasSize(3);
        }
    }

    @Test
    @DisplayName("3. send() zwraca CompletableFuture; SendResult: temat, partycja 0–2, offset ≥ 0, klucz = numer zamówienia")
    void sendResultHasMetadata() throws Exception {
        SendResult<String, OrderPlaced> result = publisher.publish(order("ZAM-101")).get(10, TimeUnit.SECONDS);

        assertThat(result.getRecordMetadata().topic()).isEqualTo(OrderEventPublisher.TOPIC);
        assertThat(result.getRecordMetadata().partition()).isBetween(0, 2);
        assertThat(result.getRecordMetadata().offset()).isGreaterThanOrEqualTo(0);
        assertThat(result.getProducerRecord().key()).isEqualTo("ZAM-101");
    }

    @Test
    @DisplayName("4. Dwie grupy (magazyn, faktury) dostają TĘ SAMĄ wiadomość — każda dokładnie raz")
    void bothGroupsReceiveEachMessage() {
        publisher.publish(order("ZAM-201"));

        await().atMost(WAIT).untilAsserted(() -> {
            assertThat(withOrderId(warehouse.received(), "ZAM-201")).hasSize(1);
            assertThat(withOrderId(invoices.received(), "ZAM-201")).hasSize(1);
        });
        ReceivedOrder inWarehouse = withOrderId(warehouse.received(), "ZAM-201").getFirst();
        ReceivedOrder inInvoices = withOrderId(invoices.received(), "ZAM-201").getFirst();
        assertThat(inWarehouse.event()).isEqualTo(order("ZAM-201"));            // JSON → ten sam rekord
        assertThat(inWarehouse.partition()).isEqualTo(inInvoices.partition());  // ta sama wiadomość w brokerze
        assertThat(inWarehouse.offset()).isEqualTo(inInvoices.offset());
    }

    @Test
    @DisplayName("4. Ten sam klucz → ta sama partycja, a offsety rosną w kolejności wysłania")
    void sameKeySamePartitionInOrder() throws Exception {
        OrderPlaced first = new OrderPlaced("ZAM-301", 4, List.of("SPO-001"), new BigDecimal("64.99"));
        OrderPlaced second = new OrderPlaced("ZAM-301", 4, List.of("SPO-001", "SPO-002"), new BigDecimal("72.48"));
        publisher.publish(first).get(10, TimeUnit.SECONDS);
        publisher.publish(second).get(10, TimeUnit.SECONDS);

        await().atMost(WAIT).untilAsserted(() -> assertThat(withOrderId(warehouse.received(), "ZAM-301")).hasSize(2));
        List<ReceivedOrder> received = withOrderId(warehouse.received(), "ZAM-301");
        assertThat(received).extracting(ReceivedOrder::event).containsExactly(first, second);
        assertThat(received.get(0).partition()).isEqualTo(received.get(1).partition());
        assertThat(received.get(1).offset()).isEqualTo(received.get(0).offset() + 1);
    }
}
