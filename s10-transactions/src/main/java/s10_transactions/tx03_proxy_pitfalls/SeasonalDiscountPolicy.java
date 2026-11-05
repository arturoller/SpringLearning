package s10_transactions.tx03_proxy_pitfalls;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rabat sezonowy (seasonal = sezonowy) — implementacja interfejsu z {@code @Transactional}. Domyślnie (Boot:
 * {@code spring.aop.proxy-target-class=true}) dostaje proxy CGLIB jak każda klasa. Z {@code proxy-target-class=false}
 * dostaje proxy JDK, które implementuje TYLKO interfejs — wtedy bean jest dostępny wyłącznie jako DiscountPolicy (test).
 * Dlatego w tej lekcji nikt nie wstrzykuje go po klasie.
 */
@Service
public class SeasonalDiscountPolicy implements DiscountPolicy {

    @Override
    @Transactional(readOnly = true)
    public int discountPercent(String product) {
        return "Monitor".equals(product) ? 10 : 0;
    }
}
