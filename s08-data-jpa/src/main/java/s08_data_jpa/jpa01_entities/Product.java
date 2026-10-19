package s08_data_jpa.jpa01_entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Produkt sklepu jako ENCJA (entity — klasa, której obiekty Hibernate zapisuje w tabeli bazy danych).
 *
 * <p>Wymagania encji (Jakarta Persistence 3.2):</p>
 * <ul>
 *   <li>{@code @Entity} (encja) i pole z {@code @Id} (identyfikator = klucz główny tabeli);</li>
 *   <li>konstruktor bez argumentów (public albo protected) — Hibernate tworzy pusty obiekt i dopiero potem
 *       wpisuje do niego wartości z wiersza tabeli;</li>
 *   <li>klasa NIE final, metody utrwalanych pól NIE final — Hibernate tworzy podklasę-pośrednika (proxy) do
 *       leniwego ładowania (lazy loading — dział s09_jpa_relations);</li>
 *   <li>pola NIE final — Hibernate wpisuje do nich wartości po utworzeniu obiektu.</li>
 * </ul>
 * Cena w złotych jako {@code BigDecimal} (dokładne kwoty pieniężne, bez błędów zaokrągleń typu double).
 */
@Entity                                   // entity = encja; nazwa encji domyślnie = prosta nazwa klasy ("Product")
@Table(name = "products")                 // table = tabela; bez @Table tabela nazywałaby się "product"
public class Product {

    @Id                                   // id = identyfikator (klucz główny, primary key)
    @GeneratedValue                       // generated value = wartość generowana; strategia AUTO → na H2: SEKWENCJA
    private Long id;                      // Long, nie long: null = „jeszcze nie zapisany” (encja nowa)

    @Column(nullable = false, unique = true, length = 20)   // column = kolumna; NOT NULL, UNIQUE, varchar(20)
    private String sku;                   // SKU — kod produktu w sklepie (np. ELE-001); klucz biznesowy, niezmienny

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)          // enumerated = wyliczeniowy; STRING = zapisz NAZWĘ stałej, nie numer
    @Column(nullable = false)             // na H2 Hibernate 7 tworzy tu typ enum ('DOM','ELEKTRONIKA',...)
    private Category category;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)  // numeric(10,2); inna nazwa kolumny
    private BigDecimal price;

    private int stock;                    // bez @Column: kolumna "stock", typ integer, NOT NULL (bo typ prosty int)

    /** Dla Hibernate (protected = nie zachęcamy innych do tworzenia „pustych” produktów). */
    protected Product() {
    }

    public Product(String sku, String name, Category category, BigDecimal price, int stock) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
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

    /**
     * DOBRA PRAKTYKA: równość encji po KLUCZU BIZNESOWYM (sku) — unikalnym, niezmiennym i znanym od chwili
     * utworzenia obiektu (nie dopiero po zapisie, jak id z bazy). Dzięki temu hashCode się nie zmienia,
     * gdy Hibernate nada id, więc produkt nie „gubi się” w HashSet (sekcja 7 lekcji).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Product other)) {   // instanceof, nie getClass(): proxy Hibernate to PODKLASA Product
            return false;
        }
        return Objects.equals(sku, other.getSku());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sku);
    }

    @Override
    public String toString() {
        return "Product[id=" + id + ", sku=" + sku + ", name=" + name + ", price=" + price + "]";
    }
}
