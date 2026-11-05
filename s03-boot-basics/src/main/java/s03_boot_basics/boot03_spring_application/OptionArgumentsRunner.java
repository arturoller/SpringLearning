package s03_boot_basics.boot03_spring_application;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * {@code ApplicationRunner} dostaje {@link ApplicationArguments} — argumenty już rozdzielone na opcje
 * ({@code --nazwa=wartość}) i zwykłe argumenty. @Order(2) = biegnie po {@link PlainArgumentsRunner}.
 */
@Component
@Order(2)
public class OptionArgumentsRunner implements ApplicationRunner {

    private final ShopStartupLog log;

    public OptionArgumentsRunner(ShopStartupLog log) {
        this.log = log;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.add("ApplicationRunner: opcje=" + args.getOptionNames().stream().sorted().toList()
                + ", zwykłe=" + args.getNonOptionArgs());
    }
}
