package s05_validation.val01_bean_validation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Zwykła klasa (nie rekord): ograniczenie można położyć na POLU albo na GETTERZE (metoda odczytu). Obie wersje dają
 * tę samą ścieżkę właściwości (property path) — "name" i "nickname" — bo ścieżka opiera się na nazwie właściwości.
 */
public class LegacyCustomer {

    @NotBlank   // ograniczenie na polu
    private String name;

    private String nickname;

    public LegacyCustomer(String name, String nickname) {
        this.name = name;
        this.nickname = nickname;
    }

    public String getName() {
        return name;
    }

    @Size(min = 2)   // ograniczenie na getterze (adnotacja na metodzie getNickname)
    public String getNickname() {
        return nickname;
    }
}
