package scratch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import s04_web_rest.rest04_json.CatalogProduct;
import s04_web_rest.rest04_json.Rest04Application;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.*;
import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest
@ContextConfiguration(classes = Rest04Application.class)
class Rest04ScratchTest {
    @Autowired MockMvc mvc; @Autowired ApplicationContext ctx; @Autowired JsonMapper bootMapper;
    record P(int b, String a, LocalDate d, LocalDateTime t, Instant i, Duration du, java.util.Date old) {}
    record Q(String name, int qty) {}
    @Test void run() throws Exception {
        JsonMapper plain = JsonMapper.builder().build();
        JsonMapper ctor = new JsonMapper();
        P p = new P(1, "x", LocalDate.of(2026,10,1), LocalDateTime.of(2026,10,1,8,30), Instant.parse("2026-10-01T08:30:00Z"), Duration.ofMinutes(90), new Date(0));
        System.out.println("SCRATCH plain " + plain.writeValueAsString(p));
        System.out.println("SCRATCH boot  " + bootMapper.writeValueAsString(p));
        for (var m : List.of(plain, bootMapper)) {
            String n = m == plain ? "plain" : "boot";
            System.out.println("SCRATCH " + n + " FAIL_ON_UNKNOWN=" + m.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
              + " NULL_PRIM=" + m.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
              + " DATES_TS=" + m.isEnabled(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
              + " DUR_TS=" + m.isEnabled(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
              + " SORT=" + m.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
              + " FAIL_EMPTY=" + m.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS)
              + " INDENT=" + m.isEnabled(SerializationFeature.INDENT_OUTPUT)
              + " DEFAULT_VIEW_INCL=" + m.isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION));
            try { System.out.println("SCRATCH " + n + " unknown -> " + m.readValue("{\"name\":\"a\",\"qty\":1,\"x\":2}", Q.class)); } catch (Exception e) { System.out.println("SCRATCH " + n + " unknown EX " + e.getClass().getName() + " runtime=" + (e instanceof RuntimeException)); }
            try { System.out.println("SCRATCH " + n + " missing prim -> " + m.readValue("{\"name\":\"a\"}", Q.class)); } catch (Exception e) { System.out.println("SCRATCH " + n + " missing EX " + e.getClass().getName()); }
        }
        System.out.println("SCRATCH ctor==builder unknown " + ctor.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        System.out.println("SCRATCH beans " + Arrays.toString(ctx.getBeanNamesForType(JsonMapper.class)) + " om=" + Arrays.toString(ctx.getBeanNamesForType(tools.jackson.databind.ObjectMapper.class)));
        System.out.println("SCRATCH catalog " + mvc.perform(get("/api/catalog/1")).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        System.out.println("SCRATCH catalog2 " + mvc.perform(get("/api/catalog/2")).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        var r = mvc.perform(post("/api/catalog/echo").contentType("application/json").content("{\"id\":5,\"product_name\":\"Miód\",\"priceGrosze\":100,\"addedOn\":\"2026-10-05\",\"updatedAt\":\"2026-10-05T10:00:00Z\",\"purchasePriceGrosze\":50,\"color\":\"zloty\"}")).andReturn();
        System.out.println("SCRATCH echo " + r.getResponse().getStatus() + " " + r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        var r2 = mvc.perform(post("/api/catalog/echo").contentType("application/json").content("{\"id\":5,\"name\":\"Miód\",\"priceGrosze\":100}")).andReturn();
        System.out.println("SCRATCH echo name " + r2.getResponse().getStatus() + " " + r2.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        var r3 = mvc.perform(post("/api/catalog/echo").contentType("application/json").content("{\"id\":5,\"product_name\":\"Miód\",\"priceGrosze\":100,\"addedOn\":\"01.10.2026\"}")).andReturn();
        System.out.println("SCRATCH echo baddate " + r3.getResponse().getStatus() + " " + r3.getResolvedException());
    }
}
