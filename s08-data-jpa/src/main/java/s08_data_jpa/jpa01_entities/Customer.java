package s08_data_jpa.jpa01_entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.time.LocalDate;

/**
 * Klient — druga encja lekcji, pokazuje ustawienia DOMYŚLNE (bez @Table, bez @Column) i dwie rzeczy do zapamiętania:
 * <ul>
 *   <li>{@code @GeneratedValue} bez strategii = AUTO → Hibernate 7 na H2 wybiera SEKWENCJĘ {@code customer_seq}
 *       z krokiem 50 (sprawdza test sekcji 2), a nie autonumerację kolumny;</li>
 *   <li>pole enum bez {@code @Enumerated} = ORDINAL (numer stałej) — PUŁAPKA, nie rób tak w swoim kodzie.</li>
 * </ul>
 * Nazwy kolumn powstają z nazw pól: {@code registeredOn} → {@code registered_on} (strategia nazw ustawiana przez
 * Spring Boot: {@code PhysicalNamingStrategySnakeCaseImpl} — camelCase zamienia na snake_case).
 */
@Entity
public class Customer {

    @Id
    @GeneratedValue // generated value = wartość generowana; strategia AUTO (domyślna)
    private Long id;

    private String email; // bez @Column: VARCHAR(255), może być NULL

    private CustomerLevel level; // PUŁAPKA: domyślnie ORDINAL → kolumna TINYINT z liczbą 0/1

    private LocalDate registeredOn; // LocalDate = sama data → kolumna DATE

    protected Customer() {
    }

    public Customer(String email, CustomerLevel level, LocalDate registeredOn) {
        this.email = email;
        this.level = level;
        this.registeredOn = registeredOn;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public CustomerLevel getLevel() {
        return level;
    }

    public LocalDate getRegisteredOn() {
        return registeredOn;
    }
}
