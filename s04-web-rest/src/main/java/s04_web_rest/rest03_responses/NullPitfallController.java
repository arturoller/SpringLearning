package s04_web_rest.rest03_responses;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * NullPitfallController = kontroler z CELOWYM błędem: zwraca null, gdy produktu nie ma. Nie kopiuj go!
 * Klient dostaje wtedy 200 OK i PUSTE ciało — „sukces” bez danych.
 */
@RestController
@RequestMapping("/api/pitfalls")
public class NullPitfallController {

    private static final Product LAPTOP = new Product("ELE-001", "Laptop Pro 14", new BigDecimal("5499.99"), 7);

    /** Celowo błędnie: nieznany sku → return null → 200 OK, puste ciało. */
    @GetMapping("/null-product/{sku}")
    public Product nullProduct(@PathVariable String sku) {
        return LAPTOP.sku().equals(sku) ? LAPTOP : null;
    }

    /** Celowo błędnie: null zamiast ResponseEntity → też 200 OK i puste ciało. */
    @GetMapping("/null-entity/{sku}")
    public ResponseEntity<Product> nullEntity(@PathVariable String sku) {
        return LAPTOP.sku().equals(sku) ? ResponseEntity.ok(LAPTOP) : null;
    }
}
