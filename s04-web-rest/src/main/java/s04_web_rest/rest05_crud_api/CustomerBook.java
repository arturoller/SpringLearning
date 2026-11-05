package s04_web_rest.rest05_crud_api;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Książka klientów w pamięci — GOTOWY serwis do ćwiczeń (nie jest częścią zadania). Dwóch klientów startowych:
 * Anna (id 1) i Jan (id 2). {@link #reset()} przywraca ten stan.
 */
@Component
public class CustomerBook {

    private final ConcurrentMap<Long, Customer> customers = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong();

    public CustomerBook() {
        reset();
    }

    public final void reset() {
        customers.clear();
        nextId.set(2);
        customers.put(1L, new Customer(1, "Anna", "anna@example.com"));
        customers.put(2L, new Customer(2, "Jan", "jan@example.com"));
    }

    public Optional<Customer> find(long id) {
        return Optional.ofNullable(customers.get(id));
    }

    public Customer add(String name, String email) {
        Customer customer = new Customer(nextId.incrementAndGet(), name, email);
        customers.put(customer.id(), customer);
        return customer;
    }

    /** Zmienia nazwę; puste Optional, gdy klienta nie ma. */
    public Optional<Customer> rename(long id, String name) {
        return Optional.ofNullable(customers.computeIfPresent(id, (key, old) -> new Customer(key, name, old.email())));
    }

    /** Zmienia e-mail; puste Optional, gdy klienta nie ma. */
    public Optional<Customer> changeEmail(long id, String email) {
        return Optional.ofNullable(customers.computeIfPresent(id, (key, old) -> new Customer(key, old.name(), email)));
    }

    /** @return true, jeśli klient istniał i został usunięty */
    public boolean remove(long id) {
        return customers.remove(id) != null;
    }
}
