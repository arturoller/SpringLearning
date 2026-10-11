package scratch;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import s04_web_rest.rest04_json.Rest04Application;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

class Rest04Scratch4Test {
    record D(@JsonFormat(pattern = "dd.MM.yyyy") LocalDate day) {}
    @Test void run() {
        try (var ctx = new SpringApplicationBuilder(Rest04Application.class).properties("server.port=0", "spring.jackson.serialization.write-dates-as-timestamps=true").run()) {
            System.out.println("SCRATCH4 started; " + ctx.getBean(JsonMapper.class).writeValueAsString(LocalDate.of(2026,10,1)));
        } catch (Exception e) { Throwable t = e; while (t.getCause() != null) t = t.getCause(); System.out.println("SCRATCH4 FAIL " + e.getClass().getName() + " ROOT " + t.getClass().getName() + ": " + t.getMessage()); }
        JsonMapper m = JsonMapper.builder().build();
        System.out.println("SCRATCH4 fmt " + m.writeValueAsString(new D(LocalDate.of(2026,10,1))) + " back " + m.readValue("{\"day\":\"05.10.2026\"}", D.class));
    }
}
