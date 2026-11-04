package s05_validation.val03_nested_and_groups;

import org.springframework.stereotype.Service;

import java.util.List;

/** Serwis zamówień — w testach podmieniany atrapą (@MockitoBean), żeby sprawdzić, czy kontroler go w ogóle zawołał. */
@Service
public class OrderService {

    public long create(Address address, List<OrderLine> lines, List<String> tags) {
        return 100;
    }

    public void update(long id, Address address, List<OrderLine> lines, List<String> tags) {
    }
}
