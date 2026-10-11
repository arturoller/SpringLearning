package s01_core_ioc.ioc05_multiple_candidates;

import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Powiadomienia e-mail. {@code @Primary} (główny) — wygrywa, gdy ktoś prosi o JEDEN NotificationSender bez bliższych wskazówek. */
@Component
@Primary
@Order(1) // order = kolejność; w List<NotificationSender> ten bean będzie pierwszy
public class EmailSender implements NotificationSender {

    @Override
    public String channel() {
        return "EMAIL";
    }

    @Override
    public String send(String recipient, String message) {
        return "[EMAIL] do " + recipient + ": " + message;
    }
}
