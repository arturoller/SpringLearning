package s10_transactions.tx03_pitfalls.final_class;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Klasa {@code final} (nie można po niej dziedziczyć) z {@code @Transactional} — CGLIB nie zrobi podklasy (test). */
@Service
public final class FinalPriceService {

    @Transactional
    public void raise() {
    }
}
