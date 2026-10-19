package s21_capstone.cap02_reservations.notification;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Podgląd „wysłanych” powiadomień (najnowsze pierwsze) — tylko do nauki i do scenariusza .http. W prawdziwym systemie
 * takiej listy z danymi wszystkich klientów nie wystawia się publicznie.
 */
@RestController
public class NotificationController {

    /** Powiadomienie w odpowiedzi API. */
    public record NotificationResponse(Long reservationId, String recipient, String message, LocalDateTime createdAt) {
    }

    private final NotificationRepository notifications;

    public NotificationController(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @GetMapping("/api/notifications")
    @Transactional(readOnly = true)
    public List<NotificationResponse> all() {
        return notifications.findAllByOrderByIdDesc().stream()
                .map(n -> new NotificationResponse(n.getReservationId(), n.getRecipient(), n.getMessage(),
                        n.getCreatedAt()))
                .toList();
    }
}
