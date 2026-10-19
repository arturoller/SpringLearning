package s16_cache_scheduling_async.cache01_cacheable;

/** Produkt ZMIENNY (ma setter) — pokazuje pułapkę: zmiana obiektu zwróconego z cache zmienia sam cache. */
public class MutableProduct {

    private final String sku;
    private long priceGrosze;

    public MutableProduct(String sku, long priceGrosze) {
        this.sku = sku;
        this.priceGrosze = priceGrosze;
    }

    public String getSku() {
        return sku;
    }

    public long getPriceGrosze() {
        return priceGrosze;
    }

    public void setPriceGrosze(long priceGrosze) {
        this.priceGrosze = priceGrosze;
    }
}
