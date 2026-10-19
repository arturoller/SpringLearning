package s13_rest_client.rc02_errors_and_timeouts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import java.net.SocketTimeoutException;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rc02ErrorsAndTimeouts — sekcja 5: NASZE API nie przepuszcza błędów dostawcy jako 500.
 *
 * <p>Dwie atrapy naraz: MockMvc wysyła zapytanie do NASZEGO kontrolera (bez serwera), a MockRestServiceServer udaje
 * dostawcę, do którego kontroler dzwoni przez SupplierStockClient. Cały kontekst (@SpringBootTest), bo potrzebny jest
 * i kontroler, i klient; jest w nim tylko jeden RestClient.Builder, więc automatyczna atrapa dostawcy działa.</p>
 */
@SpringBootTest(properties = "rc02.supplier.base-url=http://dostawca.test")
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
@ActiveProfiles("rc02")
class Rc02ErrorsAndTimeoutsShopTest {

    private static final String STOCK_URL = "http://dostawca.test/supplier/api/stock/";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MockRestServiceServer supplier;

    @Test
    @DisplayName("5. Dostawca ma produkt → nasze API: 200 OK z jego stanem magazynu")
    void ok() throws Exception {
        supplier.expect(requestTo(STOCK_URL + "ELE-001"))
                .andRespond(withSuccess("{\"sku\":\"ELE-001\",\"available\":7}", MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/shop/availability/ELE-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(7));
    }

    @Test
    @DisplayName("5. Dostawca: 404 → nasze API: 404 z ProblemDetail po polsku")
    void notFound() throws Exception {
        supplier.expect(requestTo(STOCK_URL + "XXX-999")).andRespond(withResourceNotFound());

        mockMvc.perform(get("/api/shop/availability/XXX-999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Dostawca nie zna produktu XXX-999"));
    }

    @Test
    @DisplayName("5. Dostawca: 500 → nasze API: 503 (a nie 500) bez szczegółów technicznych dostawcy")
    void supplierError() throws Exception {
        supplier.expect(requestTo(STOCK_URL + "AWARIA")).andRespond(withServerError());

        mockMvc.perform(get("/api/shop/availability/AWARIA"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Dostawca chwilowo niedostępny — spróbuj za chwilę"));
    }

    @Test
    @DisplayName("5. Dostawca nie odpowiada (błąd sieci) → nasze API: też 503")
    void supplierTimeout() throws Exception {
        supplier.expect(requestTo(STOCK_URL + "SPO-001"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        mockMvc.perform(get("/api/shop/availability/SPO-001"))
                .andExpect(status().isServiceUnavailable());
    }
}
