package s18_aop.aop02_proxies;

import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Obejście nr 3 (z OSTRZEŻENIEM): AopContext.currentProxy() — „podaj proxy, przez które właśnie mnie wywołano”.
 * Działa tylko z {@code @EnableAspectJAutoProxy(exposeProxy = true)} (patrz Aop02Application) i tylko w wątku,
 * w którym trwa wywołanie przez proxy. Wiąże kod biznesowy z mechanizmem Spring AOP — używaj w ostateczności.
 */
@Service
public class AopContextStockService {

    public String reserve(String sku, int quantity) {
        return "zarezerwowano " + quantity + " x " + sku;
    }

    public List<String> reserveAll(List<String> skus) {
        AopContextStockService proxy = (AopContextStockService) AopContext.currentProxy();
        return skus.stream().map(sku -> proxy.reserve(sku, 1)).toList();
    }
}
