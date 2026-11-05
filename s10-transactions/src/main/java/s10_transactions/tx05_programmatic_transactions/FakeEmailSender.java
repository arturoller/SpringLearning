package s10_transactions.tx05_programmatic_transactions;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Udawany wysyłacz e-maili (fake = atrapa) — w prawdziwej aplikacji tu byłoby połączenie z serwerem poczty (SMTP).
 * Zapamiętuje „wysłane” wiadomości, żeby testy mogły sprawdzić, CZY i KIEDY e-mail wyszedł. E-maila nie da się
 * cofnąć rollbackiem — i o to chodzi w sekcji 4 lekcji.
 */
@Component
public class FakeEmailSender {

    private final List<String> sent = new ArrayList<>();

    public synchronized void send(String to, String text) {
        sent.add(to + ": " + text);
    }

    public synchronized List<String> sent() {
        return List.copyOf(sent);
    }

    public synchronized void clear() {
        sent.clear();
    }
}
