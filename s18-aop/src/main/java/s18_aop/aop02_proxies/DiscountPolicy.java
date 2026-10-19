package s18_aop.aop02_proxies;

import java.math.BigDecimal;

/** Polityka rabatów — interfejs dla VipDiscountCalculator (pokaz {@code @Proxyable}, sekcja 2). */
public interface DiscountPolicy {

    /** Rabat w procentach dla klienta o podanym imieniu i nazwisku. */
    BigDecimal discountPercent(String customerName);
}
