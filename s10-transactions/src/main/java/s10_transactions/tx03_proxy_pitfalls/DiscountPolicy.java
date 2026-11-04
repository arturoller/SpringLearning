package s10_transactions.tx03_proxy_pitfalls;

/** Polityka rabatów (discount policy) — interfejs, żeby pokazać proxy JDK w sekcji 1 lekcji Tx03ProxyPitfalls. */
public interface DiscountPolicy {

    int discountPercent(String product);
}
