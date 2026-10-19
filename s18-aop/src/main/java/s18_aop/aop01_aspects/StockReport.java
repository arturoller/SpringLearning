package s18_aop.aop01_aspects;

import org.springframework.stereotype.Service;

/**
 * Raport stanów — ofiara CELOWO zepsutego aspektu BrokenAroundAspect (PUŁAPKA z sekcji 4: {@code @Around} bez
 * {@code proceed()}). Ciało metod zapisuje się w dzienniku — test pokazuje, że NIGDY się nie wykonuje.
 */
@Service
public class StockReport {

    private final CallJournal journal;

    public StockReport(CallJournal journal) {
        this.journal = journal;
    }

    public String title() {
        journal.add("METODA title()");
        return "Raport stanów magazynu";
    }

    public int productCount() {
        journal.add("METODA productCount()");
        return 5;
    }
}
