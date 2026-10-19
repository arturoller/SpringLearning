package s21_capstone.cap02_reservations.waitlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import s21_capstone.cap02_reservations.desk.Desk;

import java.time.LocalDateTime;

/**
 * Wpis na liście oczekujących (waitlist) na JEDNĄ godzinę jednego stanowiska — infrastruktura pod ĆWICZENIE 3
 * projektu Cap02Reservations (tabela z migracji V3__waitlist.sql). Kolejka: kto ma mniejsze id, ten pierwszy.
 */
@Entity
@Table(name = "waitlist_entries")
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "desk_id")
    private Desk desk;

    @Column(name = "slot_start", nullable = false)
    private LocalDateTime slotStart;

    @Column(name = "customer_email", nullable = false, length = 100)
    private String customerEmail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected WaitlistEntry() {
    }

    public WaitlistEntry(Desk desk, LocalDateTime slotStart, String customerEmail, LocalDateTime createdAt) {
        this.desk = desk;
        this.slotStart = slotStart;
        this.customerEmail = customerEmail;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Desk getDesk() {
        return desk;
    }

    public LocalDateTime getSlotStart() {
        return slotStart;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
