package s18_aop.solutions.aop02_proxies;

import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

/** Rozwiązanie wzorcowe ćwiczenia 2 lekcji Aop02Proxies: self.getObject() zwraca proxy z kontenera. */
public class Aop02Exercise2ServiceSolution {

    private final ObjectProvider<Aop02Exercise2ServiceSolution> self;

    public Aop02Exercise2ServiceSolution(ObjectProvider<Aop02Exercise2ServiceSolution> self) {
        this.self = self;
    }

    public String reserve(String sku, int quantity) {
        return "zarezerwowano " + quantity + " x " + sku;
    }

    public List<String> reserveAll(List<String> skus) {
        Aop02Exercise2ServiceSolution proxy = self.getObject(); // bean z kontenera = proxy, nie this
        return skus.stream().map(sku -> proxy.reserve(sku, 1)).toList();
    }
}
