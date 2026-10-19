package s08_data_jpa.solutions.jpa01_entities;

import jakarta.persistence.EntityManager;
import s08_data_jpa.jpa01_entities.OrderStatus;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jpa01Entities (ćwiczenia 1 i 2 — w klasie ShopOrder obok). */
public final class Jpa01EntitiesSolutions {

    private Jpa01EntitiesSolutions() {
    }

    /**
     * Rozwiązanie 3: find zwraca obiekt ZARZĄDZANY, więc wystarczy setter. Przy flush (albo commicie) Hibernate
     * porówna obiekt ze zdjęciem z chwili wczytania i sam wyśle UPDATE. Żadnego persist/merge.
     */
    public static void solution3(EntityManager entityManager, String orderNumber) {
        ShopOrder order = entityManager.find(ShopOrder.class, orderNumber);
        order.setStatus(OrderStatus.WYSLANE);
    }
}
