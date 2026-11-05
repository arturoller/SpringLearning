package s12_security.sec03_users_passwords;

import java.util.List;

/**
 * To, co o użytkowniku widzi klient API: nazwa i role. BEZ hasha hasła i bez id z bazy (sekcja 5, s09_jpa_relations/Rel06EntityToDto).
 */
public record UserView(String username, List<String> roles) {
}
