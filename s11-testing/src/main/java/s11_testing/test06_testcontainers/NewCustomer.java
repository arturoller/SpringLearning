package s11_testing.test06_testcontainers;

/** NewCustomer = klient do zaimportowania (bez id — nada je baza). */
public record NewCustomer(String name, String city, String email, boolean vip) {
}
