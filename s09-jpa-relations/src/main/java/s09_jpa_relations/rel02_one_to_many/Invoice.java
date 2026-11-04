package s09_jpa_relations.rel02_one_to_many;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Faktura — przykład z sekcji 2 lekcji: JEDNOKIERUNKOWE {@code @OneToMany} z {@code @JoinColumn}. Pozycja faktury
 * ({@link InvoiceLine}) NIE ma pola z fakturą, więc o kluczu obcym {@code invoice_line.invoice_id} decyduje lista
 * w fakturze (to ona jest tu stroną właściciela). Koszt: dodatkowe UPDATE-y (sprawdzone testem).
 */
@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, length = 20)
    private String invoiceNumber;

    // Bez mappedBy, za to @JoinColumn: "klucz obcy invoice_id jest w tabeli pozycji, a pilnuje go ta lista".
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "invoice_id")
    @OrderBy("id")
    private List<InvoiceLine> lines = new ArrayList<>();

    protected Invoice() { // dla Hibernate
    }

    public Invoice(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Long getId() {
        return id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public List<InvoiceLine> getLines() {
        return lines;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Invoice other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Invoice.class.hashCode();
    }

    @Override
    public String toString() {
        return "Invoice[id=" + id + ", invoiceNumber=" + invoiceNumber + "]";
    }
}
