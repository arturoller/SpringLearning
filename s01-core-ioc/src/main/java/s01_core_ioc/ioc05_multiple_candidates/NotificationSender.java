package s01_core_ioc.ioc05_multiple_candidates;

/** Wysyłacz powiadomień (notification sender) — interfejs z kilkoma implementacjami: e-mail, SMS, push. */
public interface NotificationSender {

    /** Nazwa kanału, np. "EMAIL". */
    String channel();

    /** Wysyła wiadomość i zwraca opis tego, co wysłano (w prawdziwym sklepie wywołałby bramkę e-mail/SMS). */
    String send(String recipient, String message);
}
