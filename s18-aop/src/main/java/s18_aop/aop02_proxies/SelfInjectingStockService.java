package s18_aop.aop02_proxies;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Obejście nr 2: wstrzyknięcie SAMEGO SIEBIE z {@code @Lazy} (lazy = leniwy). Spring wstrzykuje „leniwego
 * pośrednika”, który dopiero przy pierwszym użyciu pobiera z kontenera gotowy bean — czyli PROXY.
 * Bez {@code @Lazy} start kończy się błędem cyklu („Requested bean is currently in creation…”), bo bean
 * w konstruktorze potrzebowałby samego siebie, zanim powstanie (s10_transactions/Tx03ProxyPitfalls, sekcja 5).
 */
@Service
public class SelfInjectingStockService {

    private final SelfInjectingStockService self;

    public SelfInjectingStockService(@Lazy SelfInjectingStockService self) {
        this.self = self;
    }

    public String reserve(String sku, int quantity) {
        return "zarezerwowano " + quantity + " x " + sku;
    }

    public List<String> reserveAll(List<String> skus) {
        return skus.stream().map(sku -> self.reserve(sku, 1)).toList(); // self, nie this
    }
}
