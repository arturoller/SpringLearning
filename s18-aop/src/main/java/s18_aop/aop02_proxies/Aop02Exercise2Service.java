package s18_aop.aop02_proxies;

import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

/**
 * ĆWICZENIE 2 (średnie) lekcji Aop02Proxies — opis w klasie lekcji. Bez {@code @Component}: test rejestruje tę klasę
 * w małym kontekście (ApplicationContextRunner) razem z CountingAspect, więc nie wpływa na testy lekcji.
 */
public class Aop02Exercise2Service {

    private final ObjectProvider<Aop02Exercise2Service> self;

    public Aop02Exercise2Service(ObjectProvider<Aop02Exercise2Service> self) {
        this.self = self;
    }

    public String reserve(String sku, int quantity) {
        return "zarezerwowano " + quantity + " x " + sku;
    }

    public List<String> reserveAll(List<String> skus) {
        // TODO: dla każdego SKU wywołaj reserve(sku, 1) PRZEZ PROXY (nie przez this) i zwróć listę wyników
        throw new UnsupportedOperationException("TODO");
    }
}
