package s12_security.sec01_basics;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.DelegatingFilterProxy;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcje 2 i 3 lekcji Sec01Basics: z czego składa się łańcuch filtrów. Potrzebny jest cały kontekst
 * ({@code @SpringBootTest}), bo zaglądamy do beanów infrastruktury: rejestracji filtra w serwerze i {@code FilterChainProxy}.
 * Serwer HTTP nie jest potrzebny (domyślne {@code webEnvironment = MOCK}).
 */
@SpringBootTest
class Sec01FilterChainTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Sekcja 2: serwer zna jeden filtr (DelegatingFilterProxy), który oddaje pracę beanowi springSecurityFilterChain")
    void delegatingFilterProxy() {
        DelegatingFilterProxyRegistrationBean registration =
                context.getBean("securityFilterChainRegistration", DelegatingFilterProxyRegistrationBean.class);
        assertThat(registration.getFilter()).isInstanceOf(DelegatingFilterProxy.class);   // jedyny filtr, który zna Tomcat
        assertThat(registration.getFilterName()).isEqualTo("springSecurityFilterChain");   // nazwa beana, do którego deleguje
        assertThat(registration.determineDispatcherTypes())                                 // działa też przy przekazaniu na /error
                .contains(DispatcherType.REQUEST, DispatcherType.ERROR);
        assertThat(context.getBean("springSecurityFilterChain")).isInstanceOf(FilterChainProxy.class);
    }

    @Test
    @DisplayName("Sekcja 2: filtry łańcucha w kolejności — logowanie przed autoryzacją, AuthorizationFilter na końcu")
    void filterOrder() {
        FilterChainProxy proxy = context.getBean(FilterChainProxy.class);
        assertThat(proxy.getFilterChains()).hasSize(1);
        List<String> names = proxy.getFilterChains().getFirst().getFilters().stream()
                .map(Filter::getClass)
                .map(Class::getSimpleName)
                .toList();
        System.out.println("Filtry łańcucha sec01: " + names);    // zobacz pełną listę w konsoli
        assertThat(names).containsSubsequence(
                "SecurityContextHolderFilter",           // wczytuje kontekst (kto jest zalogowany)
                "CsrfFilter",                            // ochrona przed CSRF (Sec04StatelessApi)
                "LogoutFilter",                          // POST /logout
                "UsernamePasswordAuthenticationFilter",  // formularz: POST /login
                "BasicAuthenticationFilter",             // nagłówek Authorization: Basic …
                "AnonymousAuthenticationFilter",         // nikt się nie zalogował → "anonim"
                "ExceptionTranslationFilter",            // wyjątki bezpieczeństwa → 401/403 (albo przekierowanie na /login)
                "AuthorizationFilter");                  // decyzja: wolno czy nie
        assertThat(names.getLast()).isEqualTo("AuthorizationFilter");
    }

    @Test
    @DisplayName("Sekcja 3: własny bean SecurityFilterChain wyłącza domyślny łańcuch Boota")
    void ownChainReplacesDefault() {
        assertThat(context.getBeanNamesForType(SecurityFilterChain.class)).containsExactly("sec01FilterChain");
        assertThat(context.containsBean("defaultSecurityFilterChain")).isFalse();
    }
}
