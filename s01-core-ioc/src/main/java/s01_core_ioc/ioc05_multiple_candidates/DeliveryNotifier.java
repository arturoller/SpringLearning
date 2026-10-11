package s01_core_ioc.ioc05_multiple_candidates;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/** Powiadomienie o dostawie kurierskiej: zawsze SMS. {@code @Qualifier("smsSender")} wskazuje beana po nazwie. */
@Service
public class DeliveryNotifier {

    private final NotificationSender sender;

    public DeliveryNotifier(@Qualifier("smsSender") NotificationSender sender) {
        this.sender = sender;
    }

    public String courierOnTheWay(String phone) {
        return sender.send(phone, "Kurier jest w drodze");
    }
}
