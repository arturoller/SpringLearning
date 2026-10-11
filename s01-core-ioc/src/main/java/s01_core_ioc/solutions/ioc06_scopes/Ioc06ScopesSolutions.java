package s01_core_ioc.solutions.ioc06_scopes;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import s01_core_ioc.ioc06_scopes.ShoppingCart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Rozwiązania wzorcowe ćwiczeń 1 i 2 lekcji Ioc06Scopes — zajrzyj dopiero po własnej próbie! */
public final class Ioc06ScopesSolutions {

    private Ioc06ScopesSolutions() {
    }

    static int solution1(ApplicationContext context) {
        // zbiór porównujący przez == (tożsamość), a nie przez equals
        Set<ShoppingCart> distinct = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < 3; i++) {
            distinct.add(context.getBean(ShoppingCart.class));
        }
        return distinct.size();
    }

    static List<Integer> solution2(ObjectProvider<ShoppingCart> cartProvider, List<List<String>> orders) {
        List<Integer> sizes = new ArrayList<>();
        for (List<String> order : orders) {
            ShoppingCart cart = cartProvider.getObject(); // nowy koszyk dla każdego zamówienia
            order.forEach(cart::add);
            sizes.add(cart.size());
        }
        return sizes;
    }
}
