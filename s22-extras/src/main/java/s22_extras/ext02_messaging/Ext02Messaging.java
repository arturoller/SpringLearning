package s22_extras.ext02_messaging;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * TEMAT: Kolejki wiadomości — Kafka w Springu: KafkaTemplate, @KafkaListener, grupy konsumentów, JSON przez Jackson 3,
 *        test z kontenerem Kafki; RabbitMQ dla porównania
 *        (message broker = pośrednik wiadomości; producer = producent/nadawca; consumer = konsument/odbiorca)
 *
 * W SKRÓCIE:
 *   • Kafka to „dziennik zdarzeń”: producent DOPISUJE wiadomość do tematu (topic), konsumenci CZYTAJĄ ją w swoim tempie.
 *     Nadawca nie czeka na odbiorców i nawet nie wie, ilu ich jest.
 *   • KafkaTemplate.send(temat, klucz, wartość) — wysyłka (asynchroniczna). @KafkaListener — metoda wołana dla każdej
 *     wiadomości. Boot konfiguruje oba z właściwości spring.kafka.*.
 *   • Temat dzieli się na partycje. Ten sam klucz → ta sama partycja → zachowana kolejność. Między partycjami
 *     kolejności NIE ma.
 *   • Grupa konsumentów: w jednej grupie każda wiadomość trafia do JEDNEGO konsumenta; różne grupy dostają KAŻDĄ wiadomość.
 *   • JSON: w Spring Kafka 4 (Boot 4) JacksonJsonSerializer/JacksonJsonDeserializer (Jackson 3) + zaufane pakiety.
 *
 * ANALOGIA: tablica ogłoszeń w biurze sklepu z numerowanymi kartkami.
 *   Sprzedawca (producent) przypina kartkę „ZAM-001: słuchawki” i wraca do klientów — nie czeka, aż ktoś przeczyta.
 *   Magazyn i księgowość (dwie GRUPY) czytają tę samą tablicę, każde zapisuje sobie numer ostatniej przeczytanej kartki
 *   (offset). Kartek się nie zdejmuje — nowy pracownik może przeczytać wszystko od początku. Tablic jest kilka
 *   (partycje): kartki jednego zamówienia zawsze na tej samej tablicy, więc czyta się je po kolei.
 *
 * JAK TO DZIAŁA:
 *   POST /api/orders → OrderEventPublisher.publish(event)
 *     → KafkaTemplate.send("sklep.zamowienia", "ZAM-001", event)
 *     → JacksonJsonSerializer: {"orderId":"ZAM-001",...} + nagłówek __TypeId__ = s22_extras.ext02_messaging.OrderPlaced
 *     → partycja = hash(klucz) % 3 → broker dopisuje na koniec partycji (offset 0, 1, 2...)
 *   Kontener słuchacza (osobny wątek, np. „ext02-magazyn-0-C-1”) w pętli: poll() → JacksonJsonDeserializer
 *     (sprawdza zaufane pakiety) → WarehouseListener.onOrderPlaced(event, partycja, offset) → zapis pozycji grupy (commit).
 *   To samo niezależnie robi grupa „faktury”.
 *
 * SŁÓWKA:
 *   topic = temat; partition = partycja (część tematu, uporządkowany dziennik); offset = przesunięcie (numer pozycji
 *   w partycji); key = klucz; consumer group = grupa konsumentów; poll = odpytanie (pobranie porcji wiadomości);
 *   commit = zatwierdzenie (zapis pozycji odczytu); broker = serwer Kafki; serializer = serializator (obiekt → bajty);
 *   trusted packages = zaufane pakiety; at-least-once = co najmniej raz (możliwe duplikaty); idempotent = idempotentny
 *   (powtórzenie nie zmienia wyniku); outbox = skrzynka nadawcza (wzorzec).
 *
 * ZOBACZ TEŻ: s17_events/Evt01ApplicationEvents (zdarzenia w jednej aplikacji — bez brokera),
 *             s17_events/Evt03AsyncAndOutbox (wzorzec outbox), s20_modulith/Mod02ModuleEvents (zdarzenia między
 *             modułami), s11_testing/Test06Testcontainers (kontenery w testach), t21_concurrency/Concurrency05CompletableFuture
 *             (wynik asynchroniczny), t21_concurrency/Concurrency06ConcurrentCollections (lista dla wielu wątków).
 * </pre>
 */
public final class Ext02Messaging {

    private Ext02Messaging() {
    }

    // =================================================================================================
    // 1. KIEDY PO TO SIĘGNĄĆ (A KIEDY NIE)
    // =================================================================================================
    //
    // Broker wiadomości przydaje się, gdy:
    //   • kilka NIEZALEŻNYCH usług reaguje na to samo zdarzenie (zamówienie → magazyn, faktury, e-mail, analityka),
    //   • nadawca nie może czekać na odbiorców albo odbiorca bywa wyłączony (wiadomości czekają w brokerze),
    //   • trzeba wyrównać szczyty ruchu (Black Friday: zamówienia spływają szybciej, niż magazyn je kompletuje).
    // NIE potrzebujesz brokera, gdy:
    //   • wszystko dzieje się w JEDNEJ aplikacji — wystarczą zdarzenia Springa (s17_events/Evt01ApplicationEvents),
    //   • klient potrzebuje odpowiedzi od razu (np. „czy produkt jest dostępny?”) — to zwykłe zapytanie HTTP.
    //
    // DOBRA PRAKTYKA: wiadomość opisuje FAKT w czasie przeszłym („OrderPlaced” — złożono zamówienie), a nie polecenie
    //   („ShipOrder”). Dlaczego? Fakt nie zakłada, kto go słucha — jutro dojdzie nowa grupa i nadawca się nie zmieni.

    // =================================================================================================
    // 2. KAFKA W PIGUŁCE — TEMAT, PARTYCJA, OFFSET, GRUPA
    // =================================================================================================
    //
    //   temat sklep.zamowienia
    //   ├── partycja 0:  [off 0] [off 1] [off 2] ...      ← każda partycja to dziennik: tylko DOPISYWANIE na koniec
    //   ├── partycja 1:  [off 0: ZAM-001] ...
    //   └── partycja 2:  [off 0: ZAM-002] [off 1: ZAM-002] ...
    //   grupa „magazyn”: pozycje odczytu (offsety) dla każdej partycji; grupa „faktury”: WŁASNE pozycje.
    //
    // • Wiadomości NIE znikają po przeczytaniu (w odróżnieniu od klasycznej kolejki) — zostają przez czas retencji
    //   (retention, np. 7 dni). Nowa grupa może przeczytać wszystko od początku.
    // • Partycja wynika z klucza (hash klucza modulo liczba partycji). Sprawdzone uruchomieniem przy 3 partycjach:
    //   ZAM-001 → partycja 1, ZAM-002 → partycja 2.
    // • Temat zakłada bean NewTopic (Ext02Application: 3 partycje, 1 replika) — KafkaAdmin robi to przy starcie.
    //   Bez beana temat powstałby przy pierwszym wysłaniu, o ile broker ma auto.create.topics.enable (domyślnie tak),
    //   z liczbą partycji num.partitions brokera (domyślnie 1).
    // • Lokalny broker: s22-extras/compose/ext02/compose.yaml (tryb KRaft — Kafka 4 nie używa już ZooKeepera).

    // =================================================================================================
    // 3. PRODUCENT — KafkaTemplate (OrderEventPublisher)
    // =================================================================================================
    //
    //   CompletableFuture<SendResult<String, OrderPlaced>> wynik = kafkaTemplate.send(TOPIC, event.orderId(), event);
    //
    // send() NIE czeka na zapis w brokerze — zwraca CompletableFuture. SendResult (wynik wysłania) zawiera
    // RecordMetadata: temat, partycję i offset nadany przez broker.
    // Konfiguracja (application-ext02.properties): spring.kafka.bootstrap-servers (adres brokera),
    // spring.kafka.producer.value-serializer (JacksonJsonSerializer). Klucz: domyślny StringSerializer.
    //
    // PUŁAPKA: „wyślij i zapomnij” (fire-and-forget) — wywołanie send() bez sprawdzenia wyniku. Dlaczego groźne? Błąd
    //   wysyłki ląduje w CompletableFuture, którego nikt nie czyta — zamówienie „zniknęło”, a w kodzie nie ma śladu.
    //   Minimum: wynik.whenComplete((ok, błąd) -> log błędu) albo get(...) z limitem czasu, gdy MUSISZ mieć pewność.
    //
    // PUŁAPKA: send() „asynchroniczny”, a jednak blokuje. Bez brokera (sprawdzone uruchomieniem) pierwszy send() czeka
    //   na metadane tematu do max.block.ms = 60 s, po czym rzuca KafkaException „Send failed” z przyczyną
    //   TimeoutException „Topic sklep.zamowienia not present in metadata after 60000 ms.” — wątek HTTP był zajęty
    //   minutę, klient dostał 500. Dlaczego ważne? Przy awarii brokera wszystkie wątki serwera mogą utknąć w send().

    // =================================================================================================
    // 4. KONSUMENT I GRUPY — @KafkaListener (WarehouseListener, InvoiceListener)
    // =================================================================================================
    //
    //   @KafkaListener(id = "ext02-magazyn", topics = "sklep.zamowienia", groupId = "magazyn")
    //   public void onOrderPlaced(OrderPlaced event,
    //                             @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
    //                             @Header(KafkaHeaders.OFFSET) long offset) { ... }
    //
    // Pod spodem: Boot (KafkaAutoConfiguration) włącza obsługę adnotacji, a dla każdej metody @KafkaListener powstaje
    // kontener (ConcurrentMessageListenerContainer) z własnym wątkiem konsumenta — w logu: „magazyn: partitions assigned:
    // [sklep.zamowienia-0, sklep.zamowienia-1, sklep.zamowienia-2]” (jeden konsument w grupie = wszystkie partycje).
    //
    // • Różne grupy (magazyn, faktury) dostają KAŻDĄ wiadomość — to „rozgłaszanie” (publish-subscribe).
    // • W jednej grupie każda partycja ma JEDNEGO konsumenta. Więcej konsumentów (concurrency = "3" albo kilka instancji
    //   aplikacji) = podział partycji między nich. Konsumentów ponad liczbę partycji nic nie robi.
    // • spring.kafka.consumer.auto-offset-reset=earliest: nowa grupa (bez zapisanej pozycji) czyta od początku.
    //   Domyślne latest = tylko wiadomości wysłane PO podłączeniu się grupy.
    //
    // PUŁAPKA: kolejność tylko w obrębie partycji. Sprawdzone uruchomieniem: wysłano ZAM-001, potem 2× ZAM-002, a oba
    //   słuchacze obsłużyły najpierw ZAM-002 (partycja 2), potem ZAM-001 (partycja 1). Dlaczego? Partycje to osobne
    //   dzienniki czytane niezależnie. Co musi być po kolei (zdarzenia JEDNEGO zamówienia) — ma mieć ten sam klucz.
    //
    // PUŁAPKA: dostarczenie „co najmniej raz” (at-least-once). Gdy aplikacja padnie po obsłużeniu wiadomości, a przed
    //   zatwierdzeniem pozycji, po restarcie dostanie ją DRUGI raz. Dlaczego ważne? Druga faktura za to samo zamówienie.
    //   Konsument musi być idempotentny — pamiętać, co już obsłużył (np. po orderId) — ćwiczenie 3.

    // =================================================================================================
    // 5. JSON PRZEZ JACKSONA 3 — SERIALIZATORY SPRING KAFKA 4
    // =================================================================================================
    //
    //   spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JacksonJsonSerializer
    //   spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JacksonJsonDeserializer
    //   spring.kafka.consumer.properties.spring.json.trusted.packages=s22_extras.ext02_messaging
    //
    // Serializator zapisuje JSON {"orderId":"ZAM-001","customerId":1,"skus":["ELE-003"],"total":349.90} i dodaje nagłówek
    // __TypeId__ z pełną nazwą klasy. Deserializator tworzy obiekt TEJ klasy — ale tylko z zaufanych pakietów
    // (bezpieczeństwo: obca wiadomość nie może kazać utworzyć dowolnej klasy). Komunikaty z uruchomienia:
    //   brak zaufanego pakietu → IllegalArgumentException „The class 's22_extras.ext02_messaging.OrderPlaced' is not in
    //                             the trusted packages: [java.util, java.lang]. If you believe this class is safe to
    //                             deserialize, please provide its name. ...”
    //   brak nagłówka typu     → IllegalStateException „No type information in headers and no default type provided”
    //
    // PUŁAPKA: tutoriale sprzed Boota 4 używają org.springframework.kafka.support.serializer.JsonSerializer. W Spring Kafka 4
    //   jest przestarzały (do usunięcia) i oparty na Jacksonie 2 (com.fasterxml.jackson), którego Boot 4 nie dostarcza
    //   (Boot 4 = Jackson 3, pakiet tools.jackson). Wynik (sprawdzone): NoClassDefFoundError z brakującą klasą Jacksona 2,
    //   np. „com/fasterxml/jackson/databind/JavaType” albo „com/fasterxml/jackson/core/type/TypeReference” (zależnie od
    //   tego, którą JVM sprawdzi najpierw). Dlaczego ważne? Kopiując starą konfigurację dostajesz błąd dopiero przy
    //   tworzeniu producenta, nie przy kompilacji.
    //
    // DOBRA PRAKTYKA: ErrorHandlingDeserializer (deserializator z obsługą błędów) jako opakowanie JacksonJsonDeserializer.
    //   Dlaczego? Wiadomość, której nie da się odczytać („zatruta wiadomość”, poison pill), zamienia się wtedy w zwykły
    //   błąd obsługi (logowanie, temat martwych wiadomości — DLT) zamiast psuć pętlę odczytu konsumenta.

    // =================================================================================================
    // 6. TEST Z PRAWDZIWĄ KAFKĄ — TESTCONTAINERS + @ServiceConnection (Ext02MessagingKafkaTest)
    // =================================================================================================
    //
    //   @Testcontainers(disabledWithoutDocker = true)      ← bez Dockera klasa jest POMIJANA, build przechodzi
    //   @SpringBootTest @ActiveProfiles("ext02")
    //   class Ext02MessagingKafkaTest {
    //       @Container @ServiceConnection
    //       static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.2.0");   ← JAWNY tag obrazu
    //
    // @ServiceConnection: Boot sam podstawia spring.kafka.bootstrap-servers adresem kontenera (losowy port) — fabryka
    // ApacheKafkaContainerConnectionDetailsFactory z modułu spring-boot-kafka. KafkaContainer z pakietu
    // org.testcontainers.kafka przyjmuje obrazy apache/kafka i apache/kafka-native (sprawdzone w kodzie Testcontainers 2.0).
    // Słuchacze działają w INNYM wątku niż test, więc test czeka na wynik: Awaitility (await().untilAsserted(...)).
    //
    // PUŁAPKA: apache/kafka-native (Kafka jako program natywny GraalVM — s22_extras/Ext06NativeImage) startuje szybciej
    //   i zużywa mniej pamięci, ale jest oznaczony jako eksperymentalny. U nas jego kontener raz padł przy starcie
    //   („SegfaultHandler caught a segfault”, a Testcontainers: „Container startup failed”). Dlaczego ważne? Test
    //   z niestabilnym obrazem jest „migający” (raz zielony, raz czerwony). Testy tej lekcji używają apache/kafka (JVM).
    //
    // DOBRA PRAKTYKA: sprawdzaj w teście to, co ważne dla biznesu (obie grupy dostały zamówienie, kolejność w partycji),
    //   a nie szczegóły brokera. Dlaczego? Test jest wtedy odporny na zmianę wersji Kafki i liczby partycji.

    // =================================================================================================
    // 7. IZOLACJA W MODULE I START BEZ BROKERA
    // =================================================================================================
    //
    // Starter Kafki jest na classpath CAŁEGO modułu. W innych lekcjach Boot tworzy KafkaTemplate i KafkaAdmin, ale nie ma
    // słuchaczy ani beanów NewTopic — więc nic nie łączy się z brokerem (sprawdzone: zero logów Kafki przy starcie).
    // W tej lekcji jest inaczej. Ext02Application BEZ działającej Kafki (sprawdzone uruchomieniem):
    //   • KafkaAdmin próbuje założyć temat ~30 s: ERROR „Could not configure topics” (TimeoutException „Timed out waiting
    //     for a node assignment. Call: fetchMetadata”), aplikacja startuje dopiero po ok. 45 s,
    //   • konsumenci co chwilę logują WARN „Connection to node -1 (localhost/127.0.0.1:9092) could not be established.”,
    //   • POST /api/orders czeka 60 s i kończy się 500 (sekcja 3).
    // spring.kafka.admin.fail-fast=true zamienia pierwszy punkt w błąd startu — szybka i czytelna informacja.

    // =================================================================================================
    // 8. RabbitMQ — PORÓWNANIE (KIEDY KTÓRY)
    // =================================================================================================
    //
    //                      Kafka                                   RabbitMQ
    //   model              dziennik: wiadomości zostają (retencja)  kolejka: wiadomość znika po potwierdzeniu (ack)
    //   odczyt             konsument sam pobiera (poll), offset      broker wypycha do konsumenta (push)
    //   powtórka           można czytać od początku (nowa grupa)     raz przetworzonej wiadomości już nie ma
    //   kolejność          w obrębie partycji (klucz)                w obrębie kolejki (przy jednym konsumencie)
    //   trasowanie         temat + klucz                             exchange + routing key — bogate reguły
    //   typowe użycie      strumienie zdarzeń, duży ruch, analityka  zadania do wykonania, polecenia, RPC, opóźnienia
    //   w Springu          spring-boot-starter-kafka                 spring-boot-starter-amqp
    //                      KafkaTemplate, @KafkaListener             RabbitTemplate, @RabbitListener
    // Reguła kciuka: „wiele usług reaguje na fakty i chcę je móc odtworzyć” → Kafka; „kolejka zadań do rozdzielenia
    // między pracowników” → RabbitMQ. Oba rozwiązują 80% typowych potrzeb — wybierz to, co zespół zna.

    // =================================================================================================
    // 9. OUTBOX — DLACZEGO send() W TRANSAKCJI BAZY TO ZA MAŁO
    // =================================================================================================
    //
    // Zapis zamówienia do bazy i send() do Kafki to DWA systemy (dual write — podwójny zapis). Możliwe scenariusze:
    // baza zatwierdzona, a Kafka niedostępna → magazyn nigdy się nie dowie; wiadomość wysłana, a transakcja wycofana →
    // magazyn kompletuje nieistniejące zamówienie. Wzorzec outbox (s17_events/Evt03AsyncAndOutbox): w TEJ SAMEJ
    // transakcji zapisujesz zamówienie i wiersz „do wysłania” w tabeli outbox, a osobny proces przekazuje te wiersze do
    // Kafki (i oznacza jako wysłane). Spring Modulith robi to za Ciebie — „event externalization”
    // (s20_modulith/Mod02ModuleEvents).
    //
    // CO CZYTAĆ DALEJ: dokumentacja Spring for Apache Kafka (docs.spring.io/spring-kafka): DefaultErrorHandler i tematy
    // ponowień (@RetryableTopic), transakcje Kafki, Kafka Streams; dokumentacja Spring AMQP (RabbitMQ).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Wysyłka: kafkaTemplate.send(temat, klucz, wartość) → CompletableFuture<SendResult> — sprawdzaj wynik.
     *   • Odbiór: @KafkaListener(id = ..., topics = ..., groupId = ...) + @Header(KafkaHeaders.RECEIVED_PARTITION).
     *   • Ten sam klucz → ta sama partycja → kolejność. Między partycjami kolejności nie ma.
     *   • Różne grupy dostają każdą wiadomość; w grupie partycja ma jednego konsumenta.
     *   • JSON (Boot 4): JacksonJsonSerializer / JacksonJsonDeserializer + spring.json.trusted.packages.
     *   • Nowa grupa od początku tematu: spring.kafka.consumer.auto-offset-reset=earliest.
     *   • Temat: bean NewTopic (TopicBuilder.name(...).partitions(3).replicas(1).build()).
     *   • Test: @Testcontainers(disabledWithoutDocker = true) + @Container @ServiceConnection KafkaContainer + Awaitility.
     *   • Co najmniej raz → konsument idempotentny. Baza + Kafka → outbox.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się grupa konsumentów od partycji? Ile razy wiadomość dostaną dwie różne grupy?
     *   2. Co się stanie? Wyślesz ZAM-001, potem ZAM-002 (różne klucze, 3 partycje). Czy magazyn na pewno obsłuży
     *      je w tej kolejności?
     *   3. Co się stanie? Konsument ma JacksonJsonDeserializer, ale nie ma spring.json.trusted.packages.
     *   4. ZNAJDŹ BŁĄD: konfiguracja skopiowana z tutoriala dla Boota 3:
     *      spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
     *   5. Co się stanie, gdy uruchomisz Ext02Application bez działającej Kafki, a potem wyślesz POST /api/orders?
     *   6. Dlaczego konsument powinien być idempotentny?
     *   7. Dlaczego test z kontenerem Kafki ma disabledWithoutDocker = true i jak test czeka na słuchacza?
     *   8. Kiedy wybrałbyś RabbitMQ zamiast Kafki?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Ext02MessagingExercisesTest (bez brokera — zwykłe testy jednostkowe)
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zbuduj ProducerRecord (rekord producenta — wiadomość przed wysłaniem) dla zdarzenia:
     * temat OrderEventPublisher.TOPIC, klucz = orderId, wartość = zdarzenie oraz nagłówek "zrodlo" o wartości
     * "sklep-www" (bajty UTF-8). Taki rekord można przekazać do kafkaTemplate.send(record).
     * Podpowiedź: new ProducerRecord<>(temat, klucz, wartość), potem record.headers().add(nazwa, bajty).
     */
    static ProducerRecord<String, OrderPlaced> exercise1(OrderPlaced event) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): inny serwis wysyła zamówienia z nagłówkiem __TypeId__ = "pl.sklep.zamowienia.Zamowienie"
     * (jego klasa — u nas jej nie ma). Zwróć deserializator, który IGNORUJE nagłówki typu i zawsze czyta OrderPlaced.
     * Ma działać także dla wiadomości w ogóle bez nagłówków.
     * Podpowiedź: konstruktor JacksonJsonDeserializer z klasą docelową i metoda ignoreTypeHeaders().
     */
    static JacksonJsonDeserializer<OrderPlaced> exercise2() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): konsument idempotentny. Dostajesz wiadomości w kolejności dostarczenia — z duplikatami
     * (Kafka gwarantuje „co najmniej raz”). Policz sumę zamówień każdego klienta (customerId → suma total), licząc każde
     * orderId tylko RAZ (pierwsze wystąpienie). Wynik posortowany rosnąco po customerId.
     * Przykład: [ZAM-001 (klient 1, 349.90), ZAM-002 (klient 4, 64.99), ZAM-001 (klient 1, 349.90)] → {1=349.90, 4=64.99}.
     * Podpowiedź: Set przetworzonych orderId (add zwraca false dla duplikatu), TreeMap i merge(..., BigDecimal::add)
     * (t16_streams/Streams15BigDecimalMoney).
     */
    static Map<Long, BigDecimal> exercise3(List<OrderPlaced> deliveries) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Partycja to część TEMATU (dziennik z kolejnością). Grupa to zespół KONSUMENTÓW z własnymi pozycjami odczytu:
     *      w grupie każda partycja ma jednego konsumenta. Dwie różne grupy dostaną wiadomość dwa razy — każda raz
     *      (sprawdzone testem Ext02MessagingKafkaTest: magazyn i faktury odebrały to samo zamówienie po jednym razie).
     *   2. Nie. Różne klucze → zwykle różne partycje (ZAM-001 → 1, ZAM-002 → 2), a partycje są czytane niezależnie.
     *      W uruchomieniu ręcznym obie grupy obsłużyły najpierw ZAM-002, potem ZAM-001. Kolejność jest pewna tylko dla
     *      wiadomości z tym samym kluczem (test: dwie wiadomości ZAM-002 → ta sama partycja, rosnące offsety).
     *   3. Odczyt kończy się IllegalArgumentException „The class 's22_extras.ext02_messaging.OrderPlaced' is not in the
     *      trusted packages: [java.util, java.lang]. ...” — domyślnie zaufane są tylko java.util i java.lang.
     *      Sprawdzone testem Ext02MessagingTest.
     *   4. JsonSerializer to stara klasa oparta na Jacksonie 2 (przestarzała w Spring Kafka 4). Boot 4 ma Jacksona 3, więc
     *      tworzenie serializatora kończy się NoClassDefFoundError z brakującą klasą z pakietu com/fasterxml/jackson
     *      (np. „com/fasterxml/jackson/databind/JavaType”). Poprawnie:
     *      org.springframework.kafka.support.serializer.JacksonJsonSerializer. Sprawdzone testem.
     *   5. Start trwa ok. 45 s: KafkaAdmin przez ok. 30 s próbuje założyć temat (ERROR „Could not configure topics”),
     *      aplikacja jednak wstaje (spring.kafka.admin.fail-fast=false), konsumenci logują WARN „Connection to node -1
     *      ... could not be established”. POST czeka 60 s (max.block.ms) i kończy się 500: KafkaException „Send failed”,
     *      przyczyna „Topic sklep.zamowienia not present in metadata after 60000 ms.”. Sprawdzone uruchomieniem.
     *   6. Bo Kafka dostarcza co najmniej raz: po awarii między obsługą a zatwierdzeniem offsetu wiadomość przyjdzie
     *      ponownie. Konsument idempotentny pamięta obsłużone identyfikatory i duplikat ignoruje (ćwiczenie 3).
     *   7. Test potrzebuje prawdziwego brokera w kontenerze; bez Dockera klasa jest pomijana, więc ./mvnw test przechodzi
     *      u każdego. Słuchacz działa w wątku kontenera Kafki, więc test czeka Awaitility: await().atMost(...)
     *      .untilAsserted(...) — sprawdza warunek wielokrotnie, aż przejdzie albo minie limit.
     *   8. Gdy potrzebuję kolejki ZADAŃ rozdzielanych między pracowników, bogatego trasowania (exchange, routing key),
     *      wiadomości opóźnionych albo prostego „wykonaj i zapomnij” — bez potrzeby odtwarzania historii zdarzeń.
     */
    // </editor-fold>
}
