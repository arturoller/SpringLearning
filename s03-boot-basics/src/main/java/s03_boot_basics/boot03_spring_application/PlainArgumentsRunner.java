package s03_boot_basics.boot03_spring_application;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * {@code CommandLineRunner} dostaje argumenty „na surowo” — tablicę {@code String[]} dokładnie taką, jaką dostał
 * {@code main}. Nie rozróżnia opcji ({@code --x=y}) od zwykłych argumentów. @Order(1) = biegnie jako pierwszy.
 */
@Component
@Order(1)
public class PlainArgumentsRunner implements CommandLineRunner {

    private final ShopStartupLog log;

    public PlainArgumentsRunner(ShopStartupLog log) {
        this.log = log;
    }

    @Override
    public void run(String... args) {
        log.add("CommandLineRunner: " + Arrays.toString(args));
    }
}
