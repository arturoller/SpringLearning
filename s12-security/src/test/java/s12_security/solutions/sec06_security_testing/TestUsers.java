package s12_security.solutions.sec06_security_testing;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/** Rozwiązanie wzorcowe ćwiczenia 1 lekcji Sec06SecurityTesting. */
public final class TestUsers {

    private TestUsers() {
    }

    public static RequestPostProcessor customer(String username) {
        return user(username).roles("CUSTOMER");
    }

    public static RequestPostProcessor adminToken() {
        return jwt().jwt(token -> token.subject("admin")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
