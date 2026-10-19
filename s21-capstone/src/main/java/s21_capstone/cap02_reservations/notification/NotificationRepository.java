package s21_capstone.cap02_reservations.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repozytorium powiadomień. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByReservationIdOrderById(Long reservationId);

    List<Notification> findAllByOrderByIdDesc();
}
