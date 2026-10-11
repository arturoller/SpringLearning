package s03_boot_basics.boot03_spring_application;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * ImportOptionsRunner = runner (uruchamiacz) importu cennika. ApplicationRunner dostaje argumenty już
 * PRZETWORZONE (ApplicationArguments): opcje {@code --nazwa=wartość} osobno, zwykłe słowa osobno.
 * {@code @Order(1)} (kolejność) — wykona się PRZED StockReportRunner ({@code @Order(2)}).
 */
@Component
@Order(1)
public class ImportOptionsRunner implements ApplicationRunner {

    private final RunLog runLog;

    public ImportOptionsRunner(RunLog runLog) {
        this.runLog = runLog;
    }

    @Override
    public void run(ApplicationArguments args) {
        // getOptionValues zwraca listę (opcję można podać kilka razy) albo null, gdy opcji nie ma
        String file = args.containsOption("import.file") ? args.getOptionValues("import.file").getFirst() : "brak";
        runLog.add("ApplicationRunner: plik=" + file + ", słowa=" + args.getNonOptionArgs());
    }
}
