package s21_capstone.cap03_library_with_auth.user;

/**
 * Rola użytkownika biblioteki. W Spring Security staje się uprawnieniem z prefiksem ROLE_ (ROLE_BIBLIOTEKARZ),
 * a w regułach piszemy {@code hasRole('BIBLIOTEKARZ')} — bez prefiksu (s12_security/Sec02Authorization).
 */
public enum Role {
    BIBLIOTEKARZ, CZYTELNIK
}
