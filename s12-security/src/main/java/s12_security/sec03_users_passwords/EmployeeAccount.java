package s12_security.sec03_users_passwords;

import java.util.List;

/**
 * EmployeeAccount = konto pracownika tak, jak leży w „bazie” (jeden wiersz tabeli kont).
 * Rekord jest niezmienny; hasło trzymamy WYŁĄCZNIE jako skrót (hash) — nigdy jawnym tekstem.
 *
 * @param username     login
 * @param passwordHash skrót hasła z prefiksem algorytmu, np. {@code {bcrypt}$2a$10$...}
 * @param roles        role bez prefiksu ROLE_, np. ADMIN
 * @param active       czy konto jest aktywne (false = zablokowane)
 */
public record EmployeeAccount(String username, String passwordHash, List<String> roles, boolean active) {
}
