package s01_core_ioc.ioc06_scopes;

import org.springframework.stereotype.Service;

/** Raport dla właściciela sklepu — czyta ten sam licznik, który zwiększa HomePageService. */
@Service
public class ReportService {

    private final VisitCounter counter;

    public ReportService(VisitCounter counter) {
        this.counter = counter;
    }

    public int totalVisits() {
        return counter.count();
    }
}
