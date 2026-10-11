package s08_data_jpa.jpa01_entities;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * LegacyProduct = produkt „ze starego systemu” — przykład PUŁAPKI: enum zapisany jako NUMER pozycji
 * ({@code EnumType.ORDINAL}; ordinal = liczba porządkowa). W kolumnie status ląduje 0, 1, 2 zamiast nazwy.
 * Nie rób tak w nowym kodzie — sekcja 4 lekcji wyjaśnia dlaczego.
 */
@Entity
@Table(name = "legacy_products")
public class LegacyProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.ORDINAL)
    private ProductStatus status;

    protected LegacyProduct() {
    }

    public LegacyProduct(String name, ProductStatus status) {
        this.name = name;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public ProductStatus getStatus() {
        return status;
    }
}
