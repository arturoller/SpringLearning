package s03_boot_basics.solutions.boot03_spring_application;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import s03_boot_basics.boot03_spring_application.ProductCatalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Boot03SpringApplication — zajrzyj dopiero po własnej próbie! */
public final class Boot03SpringApplicationSolutions {

    private Boot03SpringApplicationSolutions() {
    }

    /** Rozwiązanie 1: opcja --limit z ApplicationArguments, ograniczona rozmiarem katalogu. */
    public static class ProductCountRunner implements ApplicationRunner {

        private int count;

        @Override
        public void run(ApplicationArguments args) {
            int all = ProductCatalog.ALL_PRODUCTS.size();
            count = args.containsOption("limit")
                    ? Math.min(Integer.parseInt(args.getOptionValues("limit").getFirst()), all)
                    : all;
        }

        public int count() {
            return count;
        }
    }

    /** Rozwiązanie 2: słuchacz wszystkich zdarzeń; lista bezpieczna dla wątków, na zewnątrz — kopia. */
    public static class EventNamesRecorder implements ApplicationListener<ApplicationEvent> {

        private final List<String> names = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void onApplicationEvent(ApplicationEvent event) {
            names.add(event.getClass().getSimpleName());
        }

        public List<String> names() {
            synchronized (names) {
                return List.copyOf(names);
            }
        }
    }

    /**
     * Rozwiązanie 3: najpierw najgorszy przypadek (nic nie zaimportowano), potem częściowy sukces.
     *
     * @param imported ile wierszy zaimportowano
     * @param rejected ile wierszy odrzucono
     */
    public record ImportReport(int imported, int rejected) implements ExitCodeGenerator {

        @Override
        public int getExitCode() {
            if (imported == 0) {
                return 2;
            }
            return rejected > 0 ? 1 : 0;
        }
    }
}
