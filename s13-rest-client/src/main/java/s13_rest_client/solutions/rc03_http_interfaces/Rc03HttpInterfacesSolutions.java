package s13_rest_client.solutions.rc03_http_interfaces;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import s13_rest_client.rc03_http_interfaces.SupplierOffer;
import s13_rest_client.rc03_http_interfaces.SupplierOrder;
import s13_rest_client.rc03_http_interfaces.SupplierOrderConfirmation;

import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rc03HttpInterfaces — zajrzyj dopiero po własnej próbie! */
public final class Rc03HttpInterfacesSolutions {

    private Rc03HttpInterfacesSolutions() {
    }

    /** Ćwiczenia 1–3: ExerciseSupplierApi po zamianie metod default na metody HTTP. */
    public interface SolutionSupplierApi {

        @GetExchange("/supplier/api/offers/{sku}")
        SupplierOffer offer(@PathVariable String sku);

        @GetExchange("/supplier/api/offers")
        List<SupplierOffer> offers(@RequestParam String category);

        @PostExchange(url = "/supplier/api/orders", contentType = MediaType.APPLICATION_JSON_VALUE)
        SupplierOrderConfirmation order(@RequestBody SupplierOrder order,
                                        @RequestHeader("Idempotency-Key") String idempotencyKey);
    }

    static <S> S solution4(RestClient restClient, Class<S> serviceType) {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(serviceType);
    }
}
