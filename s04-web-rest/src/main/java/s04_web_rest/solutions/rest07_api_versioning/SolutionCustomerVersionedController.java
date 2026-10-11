package s04_web_rest.solutions.rest07_api_versioning;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import s04_web_rest.rest07_api_versioning.CustomerVersionedController.CustomerV1;
import s04_web_rest.rest07_api_versioning.CustomerVersionedController.CustomerV2;

/** Rozwiązanie wzorcowe ćwiczenia 2 lekcji Rest07ApiVersioning — ten sam adres w dwóch wersjach. */
@RestController
public class SolutionCustomerVersionedController {

    static final String FULL_NAME = "Anna Nowak";

    @GetMapping(path = "/api/customers/{id}", version = "1.0")
    public CustomerV1 oneV1(@PathVariable long id) {
        return new CustomerV1(id, FULL_NAME);
    }

    @GetMapping(path = "/api/customers/{id}", version = "2.0")
    public CustomerV2 oneV2(@PathVariable long id) {
        String[] parts = FULL_NAME.split(" ", 2);
        return new CustomerV2(id, parts[0], parts[1]);
    }
}
