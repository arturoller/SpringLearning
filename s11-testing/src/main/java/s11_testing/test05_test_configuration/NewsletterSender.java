package s11_testing.test05_test_configuration;

/** Wysyłka newslettera (biuletynu dla klientów). Którą implementację dostaniesz, decyduje {@link NewsletterConfig}. */
public interface NewsletterSender {

    String send(String subject);

    /** Wersja „prawdziwa” — w kursie tylko udaje wysyłkę e-maili. */
    final class EmailNewsletterSender implements NewsletterSender {
        @Override
        public String send(String subject) {
            return "E-mail: " + subject;
        }
    }

    /** Wersja „nic nie rób” (no-op) — gdy newsletter jest wyłączony. */
    final class NoopNewsletterSender implements NewsletterSender {
        @Override
        public String send(String subject) {
            return "Newsletter wyłączony";
        }
    }
}
