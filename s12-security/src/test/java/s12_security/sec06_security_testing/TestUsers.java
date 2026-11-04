package s12_security.sec06_security_testing;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * SZKIELET ĆWICZENIA 1 lekcji Sec06SecurityTesting: wspólne "fabryki" użytkowników testowych. Leży w {@code src/test}, bo
 * używa biblioteki spring-security-test (dostępnej tylko w testach) — tak robi się to w prawdziwych projektach.
 */
public final class TestUsers {

    private TestUsers() {
    }

    /** Ćwiczenie 1a: zalogowany klient o podanej nazwie i roli CUSTOMER (post-procesor {@code user(...)}). */
    public static RequestPostProcessor customer(String username) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 1b: uwierzytelnienie tokenem JWT użytkownika "admin" z uprawnieniem ROLE_ADMIN (post-procesor {@code jwt()}). */
    public static RequestPostProcessor adminToken() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
