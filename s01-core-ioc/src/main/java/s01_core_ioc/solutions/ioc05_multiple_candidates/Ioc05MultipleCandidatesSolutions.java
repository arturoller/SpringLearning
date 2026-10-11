package s01_core_ioc.solutions.ioc05_multiple_candidates;

import s01_core_ioc.ioc05_multiple_candidates.NotificationSender;

import java.util.List;
import java.util.Map;

/** Rozwiązania wzorcowe ćwiczeń lekcji Ioc05MultipleCandidates — zajrzyj dopiero po własnej próbie! */
public final class Ioc05MultipleCandidatesSolutions {

    private Ioc05MultipleCandidatesSolutions() {
    }

    static List<String> solution1(Map<String, NotificationSender> senders) {
        return senders.keySet().stream().sorted().toList();
    }
}
