package s11_testing.test01_test_slices;

/**
 * ProductDto = produkt wysyłany jako JSON (DTO, Data Transfer Object — obiekt do przesyłania danych). Rekord,
 * więc jest niezmienny. Pole {@code priceZl} to cena w złotych jako tekst, np. "12.99" — wyliczana z groszy.
 */
public record ProductDto(Long id, String name, int priceGrosze, String priceZl) {

    /** Fabryka: z encji do DTO. 1299 gr → "12.99". */
    public static ProductDto from(Product product) {
        int grosze = product.getPriceGrosze();
        String zl = "%d.%02d".formatted(grosze / 100, grosze % 100);
        return new ProductDto(product.getId(), product.getName(), grosze, zl);
    }
}
