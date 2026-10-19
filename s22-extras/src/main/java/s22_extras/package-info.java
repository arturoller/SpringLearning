/**
 * Dział s22_extras — DODATKI (opcjonalne): przegląd technologii, po które sięga się w większych projektach.
 * Każda lekcja to PRZEGLĄD: kiedy po to sięgnąć, minimalny działający przykład, pułapki i co czytać dalej.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code ext01_spring_ai} — Ext01SpringAi: Spring AI, ChatClient, rola systemowa, odpowiedź jako rekord,
 *       lokalny model Ollama, atrapa ChatModel w testach, prompt injection i dane osobowe.</li>
 *   <li>{@code ext02_messaging} — Ext02Messaging: kolejki wiadomości — Kafka (KafkaTemplate, @KafkaListener, grupy
 *       konsumentów, JSON przez Jackson 3), test z kontenerem Kafki, porównanie z RabbitMQ.</li>
 *   <li>{@code ext03_oauth2_keycloak} — Ext03OAuth2Keycloak: OAuth2/OIDC w skrócie, aplikacja jako Resource Server,
 *       role z Keycloaka zamieniane na uprawnienia Springa, Keycloak w Dockerze, testy z jwt().</li>
 *   <li>{@code ext04_tracing} — Ext04Tracing: śledzenie rozproszone — ślad i odcinek, nagłówek traceparent,
 *       identyfikator śladu w logach, eksport OpenTelemetry, próbkowanie, eksporter w pamięci w testach.</li>
 *   <li>{@code ext05_webflux_overview} — Ext05WebFluxOverview: programowanie reaktywne (Mono, Flux), kontroler WebFlux,
 *       WebTestClient, blokowanie w potoku reaktywnym i kiedy WebFlux NIE jest potrzebny.</li>
 *   <li>{@code ext06_native_image} — Ext06NativeImage: GraalVM native image, przetwarzanie AOT Springa,
 *       RuntimeHints i ich testy bez GraalVM.</li>
 * </ol>
 *
 * <p>IZOLACJA — najważniejsza zasada tego modułu. Moduł łączy startery, które zmieniają CAŁĄ aplikację, a każda lekcja
 * ma własną klasę {@code @SpringBootApplication} w swoim podpakiecie. Co zrobiliśmy, żeby się nie gryzły
 * (każdy punkt sprawdzony uruchomieniem):</p>
 * <ul>
 *   <li>Spring Security (przychodzi ze starterem spring-boot-starter-security-oauth2-resource-server): bez konfiguracji
 *       KAŻDY adres zwraca 401, a w logu nie ma nawet wygenerowanego hasła (Boot nie tworzy użytkownika, gdy na
 *       classpath jest Resource Server). Dlatego lekcje webowe poza Ext03 mają w klasie startowej własny łańcuch
 *       filtrów z {@code permitAll()} — Ext05 w wersji reaktywnej (SecurityWebFilterChain). Ext06 nie ma serwera WWW
 *       (WebApplicationType.NONE), więc filtry jej nie dotyczą.</li>
 *   <li>OpenTelemetry: eksport metryk OTLP jest wyłączony w {@code application.properties} modułu (inaczej co minutę
 *       WARN „Failed to publish metrics to OTLP receiver”). Eksporter śladów powstaje dopiero, gdy podasz adres
 *       odbiorcy — robi to tylko profil ext04. W testach starter {@code spring-boot-starter-opentelemetry-test}
 *       wyłącza eksport śladów i metryk; Ext04 włącza śledzenie adnotacją {@code @AutoConfigureTracing}.</li>
 *   <li>Spring AI: {@code spring.ai.model.chat=none} w {@code application.properties} modułu — autokonfiguracja Ollamy
 *       nie startuje; włącza ją dopiero profil ext01. Nawet włączona nie łączy się z Ollamą przy starcie.</li>
 *   <li>Kafka: autokonfiguracja tworzy KafkaTemplate i KafkaAdmin, ale bez słuchaczy (@KafkaListener) i bez beanów
 *       NewTopic nie łączy się z brokerem. Tylko Ext02 ma słuchaczy, dlatego jej test z kontekstem wymaga Dockera.</li>
 *   <li>WebFlux: gdy na classpath są i MVC, i WebFlux, Boot wybiera MVC (Tomcat). Ext05 przełącza typ aplikacji na
 *       REACTIVE (serwer Netty).</li>
 * </ul>
 *
 * <p>Usługi zewnętrzne do ręcznego uruchomienia (Ollama, Kafka, Keycloak, Jaeger) są w plikach
 * {@code s22-extras/compose/extNN/compose.yaml}. Testy ich NIE potrzebują: model AI to atrapa, Kafka to kontener
 * Testcontainers (test pomijany bez Dockera), Keycloak zastępuje {@code jwt()} i klucz testowy.</p>
 *
 * <p>Uwaga: w pakiecie działu NIE ma klasy {@code @SpringBootApplication} — skanowałaby wszystkie lekcje naraz.</p>
 */
package s22_extras;
