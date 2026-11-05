package s10_transactions.tx03_proxy_pitfalls;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sekcja 3 lekcji Tx03ProxyPitfalls: {@code @Transactional} na metodach o różnej widoczności i z modyfikatorami.
 * Proxy CGLIB to PODKLASA: może przesłonić (override) tylko to, co da się przesłonić w podklasie z tego samego
 * pakietu — public, protected i package-private TAK; private, final i static NIE.
 */
@Service
public class VisibilityShowcase {

    private final JdbcClient jdbc;

    public VisibilityShowcase(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public TxState publicMethod() {
        return TxState.now();
    }

    @Transactional
    protected TxState protectedMethod() {
        return TxState.now();
    }

    @Transactional
    TxState packagePrivateMethod() { // package-private = widoczna w pakiecie (bez modyfikatora)
        return TxState.now();
    }

    /** Publiczne „okno” do metody prywatnej (spoza klasy prywatnej nie da się wywołać). */
    public TxState callPrivateMethod() {
        return privateMethod();
    }

    @Transactional // ignorowana: podklasa nie widzi metod private
    private TxState privateMethod() {
        return TxState.now();
    }

    /**
     * {@code final} — podklasa nie może jej przesłonić, więc wywołanie na proxy uruchamia tę metodę NA SAMYM PROXY.
     * Proxy CGLIB jest tworzone bez konstruktora (Objenesis), więc jego pola są puste: {@code jdbc == null} (test).
     */
    @Transactional
    public final TxState finalMethod() {
        return TxState.now();
    }

    @Transactional
    public final boolean finalMethodSeesDependency() {
        return jdbc != null;
    }

    public boolean publicMethodSeesDependency() {
        return jdbc != null;
    }

    /** {@code static} — należy do klasy, nie do obiektu; nie ma tu żadnego proxy. */
    @Transactional
    public static TxState staticMethod() {
        return TxState.now();
    }
}
