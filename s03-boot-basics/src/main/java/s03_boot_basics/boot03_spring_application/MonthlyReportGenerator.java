package s03_boot_basics.boot03_spring_application;

import org.springframework.stereotype.Component;

/**
 * „Ciężki” bean, którego aplikacja potrzebuje rzadko (raport miesięczny). Konstruktor zapisuje do dziennika,
 * kiedy bean powstał — dzięki temu test widzi różnicę między zwykłym a leniwym tworzeniem beanów
 * (lazy initialization = leniwa inicjalizacja: bean powstaje dopiero przy pierwszym użyciu).
 */
@Component
public class MonthlyReportGenerator {

    public MonthlyReportGenerator(StartupJournal journal) {
        journal.record("utworzono MonthlyReportGenerator");
    }

    public String title() {
        return "Raport miesięczny sklepu";
    }
}
