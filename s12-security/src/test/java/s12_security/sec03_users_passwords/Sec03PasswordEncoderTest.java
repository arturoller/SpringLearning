package s12_security.sec03_users_passwords;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 2 lekcji Sec03UsersPasswords: {@code PasswordEncoder}. Zwykłe testy jednostkowe — kodery haseł to zwykłe obiekty,
 * Spring nie jest potrzebny. Hasła TYLKO DO NAUKI.
 */
class Sec03PasswordEncoderTest {

    @Test
    @DisplayName("Sekcja 2: BCrypt — to samo hasło daje za każdym razem INNY hash (losowa sól), a matches() i tak rozpoznaje oba")
    void bcryptSaltsEveryHash() {
        PasswordEncoder bcrypt = new BCryptPasswordEncoder();
        String first = bcrypt.encode("haslo-testowe");
        String second = bcrypt.encode("haslo-testowe");

        assertThat(first).isNotEqualTo(second);
        assertThat(bcrypt.matches("haslo-testowe", first)).isTrue();
        assertThat(bcrypt.matches("haslo-testowe", second)).isTrue();
        assertThat(bcrypt.matches("zle-haslo", first)).isFalse();
        // budowa hasha: $2a$ (wersja) 10$ (koszt) + 22 znaki soli + 31 znaków wyniku = 60 znaków
        assertThat(first).startsWith("$2a$10$").hasSize(60);
    }

    @Test
    @DisplayName("Sekcja 2: koszt (strength) BCrypt jest zapisany w hashu; niższy koszt niż obecny = hash do przepisania")
    void bcryptStrength() {
        String cheap = new BCryptPasswordEncoder(4).encode("haslo-testowe");
        assertThat(cheap).startsWith("$2a$04$");
        assertThat(new BCryptPasswordEncoder(12).encode("haslo-testowe")).startsWith("$2a$12$");
        assertThat(new BCryptPasswordEncoder().upgradeEncoding(cheap)).isTrue();    // domyślny koszt 10 > 4
    }

    @Test
    @DisplayName("Sekcja 2 (pułapka): BCrypt liczy tylko 72 bajty — dłuższe hasło encode() odrzuca, a matches() po cichu ucina")
    void bcrypt72Bytes() {
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        assertThatThrownBy(() -> bcrypt.encode("a".repeat(73)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("password cannot be more than 72 bytes");
        assertThatThrownBy(() -> bcrypt.encode("ą".repeat(37)))                  // 37 znaków, ale 74 bajty w UTF-8
                .isInstanceOf(IllegalArgumentException.class);
        String hash72 = bcrypt.encode("a".repeat(72));
        assertThat(bcrypt.matches("a".repeat(72) + "cokolwiek", hash72)).isTrue();   // reszta po 72. bajcie nie ma znaczenia
    }

    @Test
    @DisplayName("Sekcja 2: DelegatingPasswordEncoder — nowe hasła jako {bcrypt}…, a stare {noop}… dalej działają i są do przepisania")
    void delegatingEncoder() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String hash = encoder.encode("haslo-testowe");

        assertThat(hash).startsWith("{bcrypt}$2a$10$");
        assertThat(encoder.matches("haslo-testowe", hash)).isTrue();
        assertThat(encoder.matches("haslo-testowe", "{noop}haslo-testowe")).isTrue();
        assertThat(encoder.upgradeEncoding("{noop}haslo-testowe")).isTrue();       // inny algorytm niż domyślny → przepisz
        assertThat(encoder.upgradeEncoding(hash)).isFalse();
    }

    @Test
    @DisplayName("Sekcja 2 (pułapka): hash BEZ prefiksu {id} w DelegatingPasswordEncoder → IllegalArgumentException")
    void hashWithoutPrefix() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String plainBcrypt = new BCryptPasswordEncoder().encode("haslo-testowe");   // brak "{bcrypt}" z przodu
        assertThatThrownBy(() -> encoder.matches("haslo-testowe", plainBcrypt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("each password must have a password encoding prefix");
    }

    @Test
    @DisplayName("Sekcja 2: SHA-256 bez soli — to samo hasło zawsze daje ten sam skrót (i liczy się w nanosekundy)")
    void sha256IsDeterministic() throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        String first = HexFormat.of().formatHex(sha256.digest("haslo-testowe".getBytes(StandardCharsets.UTF_8)));
        String second = HexFormat.of().formatHex(sha256.digest("haslo-testowe".getBytes(StandardCharsets.UTF_8)));
        assertThat(first).isEqualTo(second);     // dwóch użytkowników z tym samym hasłem = ten sam wpis w bazie
    }

    @Test
    @DisplayName("Sekcja 2: Argon2PasswordEncoder istnieje w spring-security-crypto, ale bez biblioteki Bouncy Castle nie działa")
    void argon2NeedsBouncyCastle() {
        assertThatThrownBy(() -> Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode("haslo-testowe"))
                .isInstanceOf(NoClassDefFoundError.class)
                .hasMessageContaining("org/bouncycastle");
    }
}
