package s12_security.sec02_authorization;

import java.math.BigDecimal;

/**
 * Zamówienie klienta (lekcja Sec02Authorization). Nazwa {@code ShopOrder}, a nie {@code Order}, żeby nie mylić z adnotacją
 * {@code @Order} Springa. {@code owner} (właściciel) = nazwa użytkownika, który złożył zamówienie.
 */
public record ShopOrder(long id, String owner, String product, BigDecimal total) {
}
