package s01_core_ioc.ioc05_multiple_candidates;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Rozgłoszenie wiadomości WSZYSTKIM kanałami. Zamiast jednego beana prosimy o {@code List} (lista wszystkich beanów
 * typu NotificationSender, w kolejności z {@code @Order}) oraz {@code Map} (klucz = nazwa beana).
 */
@Service
public class BroadcastService {

    private final List<NotificationSender> senders;
    private final Map<String, NotificationSender> sendersByName;

    public BroadcastService(List<NotificationSender> senders, Map<String, NotificationSender> sendersByName) {
        this.senders = senders;
        this.sendersByName = sendersByName;
    }

    public List<String> sendToAll(String recipient, String message) {
        return senders.stream().map(sender -> sender.send(recipient, message)).toList();
    }

    public List<String> channelsInOrder() {
        return senders.stream().map(NotificationSender::channel).toList();
    }

    public Map<String, NotificationSender> byName() {
        return sendersByName;
    }
}
