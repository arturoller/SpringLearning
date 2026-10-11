package s01_core_ioc.solutions.ioc04_bean_methods;

import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.ZoneId;

/** Rozwiązanie ćwiczenia 2: zegar ze strefą warszawską jako bean. */
public class Ioc04Exercise2ConfigSolution {

    @Bean
    public Clock warsawClock() {
        return Clock.system(ZoneId.of("Europe/Warsaw"));
    }
}
