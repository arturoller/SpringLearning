package s01_core_ioc.ioc05_multiple_candidates;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Powiadomienia push (wyskakujące w aplikacji mobilnej). Nazwa beana: "pushSender". */
@Component
@Order(3)
public class PushSender implements NotificationSender {

    @Override
    public String channel() {
        return "PUSH";
    }

    @Override
    public String send(String recipient, String message) {
        return "[PUSH] do " + recipient + ": " + message;
    }
}
