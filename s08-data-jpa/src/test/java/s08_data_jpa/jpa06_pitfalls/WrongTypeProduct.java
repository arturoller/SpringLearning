package s08_data_jpa.jpa06_pitfalls;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Sekcja 2 lekcji Jpa06FlywayWithJpa: encja z ZŁYM typem pola. W tabeli (migracja V1) kolumna stock to INTEGER,
 * a tu pole jest tekstem (String → Hibernate oczekuje VARCHAR). Pozostałe kolumny tabeli celowo pominięte —
 * validate sprawdza tylko pola encji, nadmiarowe kolumny w tabeli mu nie przeszkadzają.
 */
@Entity
@Table(name = "product")
public class WrongTypeProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stock; // BŁĄD: w bazie INTEGER

    protected WrongTypeProduct() {
    }

    public Long getId() {
        return id;
    }

    public String getStock() {
        return stock;
    }
}
