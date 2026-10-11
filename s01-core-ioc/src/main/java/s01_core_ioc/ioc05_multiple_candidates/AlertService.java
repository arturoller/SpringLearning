package s01_core_ioc.ioc05_multiple_candidates;

/** Alarmy dla dyżurnego (klasa bez adnotacji — beana tworzy metoda {@code @Bean}, patrz ćwiczenie 3). */
public class AlertService {

    private final NotificationSender sender;

    public AlertService(NotificationSender sender) {
        this.sender = sender;
    }

    public String alert(String message) {
        return sender.send("dyzur@sklep.example", message);
    }
}
