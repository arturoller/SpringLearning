package s10_transactions.tx03_pitfalls.expose_proxy;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Sekcja 2 lekcji s10_transactions/Tx03ProxyPitfalls: {@code @EnableAspectJAutoProxy(exposeProxy = true)} (expose proxy =
 * udostępnij proxy) — proxy przy każdym wywołaniu zapisuje się w ThreadLocal, a {@code AopContext.currentProxy()}
 * je zwraca. Osobny pakiet testów, żeby ustawienie nie dotyczyło aplikacji lekcji.
 */
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class ExposeProxyApplication {
}
