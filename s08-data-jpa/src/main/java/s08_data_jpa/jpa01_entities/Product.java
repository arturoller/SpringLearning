package s08_data_jpa.jpa01_entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Encja (entity — obiekt Javy zapisywany jako wiersz tabeli) produktu w sklepie. Wzorcowe mapowanie lekcji Jpa01:
 * każda adnotacja ma obok komentarz, co zmienia w wygenerowanym DDL (sprawdza to test sekcji 2).
 *
 * <p>Importy z {@code jakarta.persistence} — NIE {@code javax.persistence}. Stary pakiet {@code javax} był w JPA 2.x
 * (Spring Boot 2, Hibernate 5); od Spring Boot 3 / Hibernate 6 jest tylko {@code jakarta}. W starych tutorialach
 * zobaczysz {@code import javax.persistence.Entity} — w Boot 4 to się nawet nie skompiluje.</p>
 *
 * <p>Dlaczego zwykła klasa, a nie rekord? Hibernate potrzebuje: (1) konstruktora bez argumentów, żeby utworzyć pusty
 * obiekt i wpisać do niego kolumny z wiersza; (2) zmiennego stanu — encja zarządzana zmienia się w miejscu, a
 * Hibernate wykrywa zmiany (dirty checking, sekcja 4); (3) możliwości dziedziczenia — leniwe proxy to podklasa encji.
 * Rekord jest niezmienny i {@code final}, więc nie spełnia żadnego z tych warunków. Rekordy świetnie nadają się
 * za to na projekcje i DTO (s08_data_jpa/Jpa04CustomQueries).</p>
 */
@Entity // entity = encja: Hibernate zarządza tą klasą i mapuje ją na tabelę
@Table(name = "product") // table = tabela; bez @Table nazwa pochodzi od nazwy klasy (tu wyszłoby to samo)
public class Product {

    @Id // id = klucz główny (PRIMARY KEY)
    @GeneratedValue(strategy = GenerationType.IDENTITY) // identity = kolumna z autonumeracją bazy
    private Long id; // Long, a nie long: null = „jeszcze nie zapisany” (encja nowa)

    @Column(nullable = false, length = 100, unique = true) // column = kolumna: NOT NULL, VARCHAR(100), UNIQUE
    private String name;

    @Enumerated(EnumType.STRING) // enumerated = wyliczeniowy; STRING = zapisuj NAZWĘ stałej ('COMPUTERS'), nie numer
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(nullable = false, precision = 10, scale = 2) // precision = liczba cyfr, scale = cyfry po przecinku
    private BigDecimal price; // pieniądze zawsze w BigDecimal, nigdy w double

    private int stock; // typ prosty int → kolumna NOT NULL (int nie może być null)

    @Column(name = "created_at", nullable = false) // jawna nazwa kolumny
    private Instant createdAt; // Instant = punkt na osi czasu → TIMESTAMP WITH TIME ZONE

    @Transient // transient = przejściowy: pole tylko w pamięci, Hibernate go NIE zapisuje (brak kolumny)
    private int basketQuantity;

    /** Konstruktor bez argumentów dla Hibernate — {@code protected}, żeby nie kusił w kodzie aplikacji. */
    protected Product() {
    }

    public Product(String name, Category category, BigDecimal price, int stock, Instant createdAt) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getBasketQuantity() {
        return basketQuantity;
    }

    public void setBasketQuantity(int basketQuantity) {
        this.basketQuantity = basketQuantity;
    }

    /**
     * equals oparte na id, z obsługą null (DOBRA PRAKTYKA, sekcja 5): dwie encje są równe, gdy to ten sam wiersz.
     * Encja bez id (nowa, nie zapisana) jest równa tylko sobie samej. {@code instanceof} zamiast {@code getClass()},
     * bo proxy Hibernate to PODKLASA encji; {@code other.getId()} zamiast {@code other.id}, bo pola proxy są puste.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Product other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    /**
     * Stały hashCode dla całej klasy: nie zmienia się, gdy persist nada id — dzięki temu encja nie „gubi się” w
     * HashSet (sekcja 5). Koszt: wszystkie produkty trafiają do jednego kubełka — przy zbiorach encji to bez znaczenia.
     */
    @Override
    public int hashCode() {
        return Product.class.hashCode();
    }

    @Override
    public String toString() {
        return "Product[id=" + id + ", name=" + name + ", price=" + price + ", stock=" + stock + "]";
    }
}
