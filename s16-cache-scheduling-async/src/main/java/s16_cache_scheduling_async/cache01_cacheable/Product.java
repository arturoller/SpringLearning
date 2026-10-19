package s16_cache_scheduling_async.cache01_cacheable;

/** Produkt sklepu (niezmienny rekord). Cena w groszach (long), żeby nie mieszać BigDecimal do lekcji o cache. */
public record Product(String sku, String name, String category, long priceGrosze) {
}
