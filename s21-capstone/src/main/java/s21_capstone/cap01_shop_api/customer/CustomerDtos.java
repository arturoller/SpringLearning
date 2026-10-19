package s21_capstone.cap01_shop_api.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO klientów: żądanie utworzenia i odpowiedź. */
public final class CustomerDtos {

    private CustomerDtos() {
    }

    /**
     * POST /api/customers. Email opcjonalny, ale jeśli podany — poprawny i unikalny. {@code vip} jako {@code Boolean}:
     * brak pola = {@code null} = „nie VIP” (z {@code boolean} Jackson 3 odrzuciłby JSON bez tego pola — ProductDtos).
     */
    public record CreateCustomerRequest(
            @NotBlank(message = "imię i nazwisko są wymagane")
            @Size(max = 100, message = "imię i nazwisko: najwyżej 100 znaków")
            String name,
            @NotBlank(message = "miasto jest wymagane")
            @Size(max = 50, message = "miasto: najwyżej 50 znaków")
            String city,
            @Email(message = "niepoprawny adres email")
            @Size(max = 100, message = "email: najwyżej 100 znaków")
            String email,
            Boolean vip) {
    }

    public record CustomerResponse(Long id, String name, String city, String email, boolean vip) {

        public static CustomerResponse from(Customer customer) {
            return new CustomerResponse(customer.getId(), customer.getName(), customer.getCity(), customer.getEmail(),
                    customer.isVip());
        }
    }
}
