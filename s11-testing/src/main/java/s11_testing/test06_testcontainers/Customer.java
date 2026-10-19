package s11_testing.test06_testcontainers;

/** Customer = klient sklepu (wiersz tabeli {@code customer}). Rekord — JdbcClient mapuje kolumny na składowe. */
public record Customer(Long id, String name, String city, String email, boolean vip) {
}
