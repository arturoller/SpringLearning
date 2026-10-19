package s18_aop.aop03_custom_annotations;

import org.springframework.stereotype.Service;

/**
 * Raporty. {@code @MeasureTime} na KLASIE = pomiar każdej metody z progiem 1000 ms. monthlyReport ma własną
 * adnotację z progiem 10 ms — ustawienie metody wygrywa (MeasureTimeAspect.measureClass ją pomija).
 */
@Service
@MeasureTime(warnAboveMillis = 1000)
public class ReportService {

    public String dailyReport() {
        return "Raport dzienny";
    }

    @MeasureTime(warnAboveMillis = 10)
    public String monthlyReport() {
        return "Raport miesięczny";
    }
}
