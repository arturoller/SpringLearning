package s13_rest_client.solutions.rc01_rest_client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import s13_rest_client.rc01_rest_client.SupplierOffer;
import s13_rest_client.rc01_rest_client.SupplierOrder;

import java.net.URI;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rc01RestClient — zajrzyj dopiero po własnej próbie! */
public final class Rc01RestClientSolutions {

    private Rc01RestClientSolutions() {
    }

    static String solution1(RestClient client, String sku) {
        return client.get()
                .uri("/supplier/api/offers/{sku}", sku)
                .retrieve()
                .requiredBody(SupplierOffer.class)
                .name();
    }

    static List<String> solution2(RestClient client, String category) {
        List<SupplierOffer> offers = client.get()
                .uri(uriBuilder -> uriBuilder.path("/supplier/api/offers").queryParam("category", category).build())
                .retrieve()
                .requiredBody(new ParameterizedTypeReference<List<SupplierOffer>>() {
                });
        return offers.stream()
                .filter(offer -> offer.available() > 0)
                .map(SupplierOffer::sku)
                .sorted()
                .toList();
    }

    static URI solution3(RestClient client, String sku, int quantity) {
        return client.post()
                .uri("/supplier/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SupplierOrder(sku, quantity))
                .retrieve()
                .toBodilessEntity()        // ciało potwierdzenia nie jest potrzebne — tylko nagłówek Location
                .getHeaders()
                .getLocation();
    }

    static RestClient solution4(RestClient.Builder builder, String baseUrl, String shopId) {
        return builder
                .baseUrl(baseUrl)
                .defaultHeader("X-Shop-Id", shopId)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
