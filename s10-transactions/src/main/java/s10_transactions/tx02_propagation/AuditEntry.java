package s10_transactions.tx02_propagation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** AuditEntry = wpis w dzienniku audytu (audit log): krótki opis tego, co się wydarzyło. Tabela audit_log. */
@Entity
@Table(name = "audit_log")
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String message;

    protected AuditEntry() {
    }

    public AuditEntry(String message) {
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }
}
