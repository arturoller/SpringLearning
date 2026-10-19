package s21_capstone.cap02_reservations.reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;

/**
 * Jedna zajęta godzina jednego stanowiska — wiersz tabeli {@code occupied_slots} (klucz główny: desk_id + slot_start).
 * {@code @Embeddable} (osadzalny — nie ma własnej tożsamości, żyje wewnątrz rezerwacji); od Hibernate 6.2 może nim
 * być rekord. equals/hashCode rekordu porównują wartości — tego potrzebuje zbiór {@code Set<OccupiedSlot>}.
 */
@Embeddable
public record OccupiedSlot(
        @Column(name = "desk_id", nullable = false) Long deskId,
        @Column(name = "slot_start", nullable = false) LocalDateTime slotStart) {
}
