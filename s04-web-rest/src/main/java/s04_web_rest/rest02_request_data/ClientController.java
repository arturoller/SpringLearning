package s04_web_rest.rest02_request_data;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sekcja 3 lekcji: nagłówki zapytania ({@code @RequestHeader}) i ciasteczka ({@code @CookieValue}). */
@RestController
@RequestMapping("/rest02/client")
public class ClientController {

    /**
     * X-Shop-Client — własny nagłówek (nazwy własnych nagłówków tradycyjnie zaczynano od "X-"). Jest WYMAGANY: brak → 400.
     * Accept-Language to standardowy nagłówek z językiem klienta; defaultValue = "pl" na wypadek, gdy go nie ma.
     * Nazwy nagłówków są podawane jawnie, bo w Javie nie można użyć myślnika w nazwie parametru.
     */
    @GetMapping("/info")
    public String info(@RequestHeader("X-Shop-Client") String client,
                       @RequestHeader(name = "Accept-Language", defaultValue = "pl") String language) {
        return "klient=" + client + ", język=" + language;
    }

    /** Ciasteczko (cookie) shopSession — opcjonalne; brak → "brak sesji". */
    @GetMapping("/session")
    public String session(@CookieValue(name = "shopSession", required = false) String session) {
        return session == null ? "brak sesji" : "sesja=" + session;
    }
}
