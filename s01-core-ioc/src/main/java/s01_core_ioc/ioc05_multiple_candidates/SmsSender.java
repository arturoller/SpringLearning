package s01_core_ioc.ioc05_multiple_candidates;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Powiadomienia SMS. Nazwa beana (domyślna): "smsSender". */
@Component
@Order(2)
public class SmsSender implements NotificationSender {

    @Override
    public String channel() {
        return "SMS";
    }

    @Override
    public String send(String recipient, String message) {
        return "[SMS] do " + recipient + ": " + message;
    }
}
