package s18_aop.aop03_custom_annotations;

import org.springframework.stereotype.Service;

/** Implementacja PriceArchive BEZ własnej adnotacji — aspekt audytu jej nie obejmie (sekcja 1). */
@Service
public class PriceArchiveService implements PriceArchive {

    private final AuditJournal journal;

    public PriceArchiveService(AuditJournal journal) {
        this.journal = journal;
    }

    @Override
    public String archive(String sku) {
        journal.add("METODA archive(" + sku + ")");
        return "zarchiwizowano " + sku;
    }
}
