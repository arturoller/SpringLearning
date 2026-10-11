package scratch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class AmbiguousScratchTest {
    @RestController static class A { @GetMapping("/api/products") String a() { return "a"; } }
    @RestController static class B { @GetMapping("/api/products") String b() { return "b"; } }
    @EnableAutoConfiguration @Import({A.class, B.class}) static class Cfg {}

    @Test void run() {
        try { new SpringApplicationBuilder(Cfg.class).properties("server.port=0").run().close(); System.out.println("SCRATCH started OK"); }
        catch (Exception e) { Throwable t = e; while (t.getCause() != null) t = t.getCause(); System.out.println("SCRATCH " + e.getClass().getName() + " ROOT " + t.getClass().getName() + ": " + t.getMessage()); }
    }
}
