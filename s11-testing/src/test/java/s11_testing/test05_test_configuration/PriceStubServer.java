package s11_testing.test05_test_configuration;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Serwer-atrapa usługi cen (stub = zaślepka) na wbudowanym w JDK {@code HttpServer}. Port 0 = „daj mi dowolny wolny
 * port” — system wybiera go przy starcie, więc adres znamy dopiero W CZASIE DZIAŁANIA. Właśnie dlatego test podaje go
 * Springowi przez {@code @DynamicPropertySource}, a nie wpisuje na sztywno do pliku właściwości.
 * <p>
 * Odpowiada na {@code GET /prices/{kod}}: 200 i cena z mapy albo 404.
 */
public final class PriceStubServer implements AutoCloseable {

    private final HttpServer server;

    private PriceStubServer(HttpServer server) {
        this.server = server;
    }

    public static PriceStubServer start(Map<String, String> prices) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/prices/", exchange -> {
                String code = exchange.getRequestURI().getPath().substring("/prices/".length());
                String price = prices.get(code);
                byte[] body = (price == null ? "brak" : price).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(price == null ? 404 : 200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.start();
            return new PriceStubServer(server);
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się uruchomić serwera-atrapy", e);
        }
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + port();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
