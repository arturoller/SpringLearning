package s05_validation.val02_valid_in_controllers;

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
public class Customers {

    private final ConcurrentMap<Long, Customer> customers = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong();

    public Customers() {
        reset();
    }

    public final void reset() {
        customers.clear();
        nextId.set(2);
        customers.put(1L, new Customer(1, "Anna", "anna@example.com"));
        customers.put(2L, new Customer(2, "Jan", "jan@example.com"));
    }

    public int size() {
        return customers.size();
    }

    public Optional<Customer> find(long id) {
        return Optional.ofNullable(customers.get(id));
    }

    public Customer add(String name, String email) {
        Customer customer = new Customer(nextId.incrementAndGet(), name, email);
        customers.put(customer.id(), customer);
        return customer;
    }

    /** Zmienia nazwę i e-mail; pusty Optional, gdy klienta nie ma. */
    public Optional<Customer> update(long id, String name, String email) {
        return Optional.ofNullable(customers.computeIfPresent(id, (key, old) -> new Customer(id, name, email)));
    }
}
