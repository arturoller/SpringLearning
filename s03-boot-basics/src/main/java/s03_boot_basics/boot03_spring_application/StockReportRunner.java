package s03_boot_basics.boot03_spring_application;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * StockReportRunner = runner raportu stanów magazynowych. CommandLineRunner dostaje SUROWE argumenty
 * ({@code String...}) — dokładnie to, co trafiło do main. {@code @Order(2)} — wykona się po ImportOptionsRunner.
 */
@Component
@Order(2)
public class StockReportRunner implements CommandLineRunner {

    private final RunLog runLog;

    public StockReportRunner(RunLog runLog) {
        this.runLog = runLog;
    }

    @Override
    public void run(String... args) {
        runLog.add("CommandLineRunner: argumenty=" + Arrays.toString(args));
    }
}
