package s01_core_ioc.ioc06_scopes;

import org.springframework.stereotype.Service;

/** Strona główna sklepu — każde otwarcie zwiększa licznik odwiedzin. */
@Service
public class HomePageService {

    private final VisitCounter counter;

    public HomePageService(VisitCounter counter) {
        this.counter = counter;
    }

    public void open() {
        counter.increment();
    }
}
