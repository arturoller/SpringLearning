package s01_core_ioc.ioc05_multiple_candidates;

import org.springframework.stereotype.Service;

/** Potwierdzenie zamówienia: prosi o JEDEN NotificationSender, więc dostaje beana oznaczonego {@code @Primary} (e-mail). */
@Service
public class OrderConfirmationService {

    private final NotificationSender sender;

    public OrderConfirmationService(NotificationSender sender) {
        this.sender = sender;
    }

    public String confirm(String customer, int orderId) {
        return sender.send(customer, "Zamówienie nr " + orderId + " przyjęte");
    }
}
