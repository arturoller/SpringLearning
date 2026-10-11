package s05_validation.val03_nested_and_groups;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Typy używane w lekcji Val03NestedAndGroups (zamówienia sklepu i produkty z grupami). Wydzielone do osobnego pliku,
 * żeby plik lekcji zawierał wyłącznie tłumaczenie i komentarze.
 */
public final class Val03Types {

    private Val03Types() {
    }

    // --- grupy walidacji (sekcja 4): zwykłe puste interfejsy — znaczniki ---------------------------

    /** Grupa „przy tworzeniu” (on create = przy tworzeniu). */
    public interface OnCreate {
    }

    /** Grupa „przy aktualizacji” (on update = przy aktualizacji). */
    public interface OnUpdate {
    }

    // --- obiekty zagnieżdżone (sekcje 1-3) ------------------------------------------------------------

    /** Address = adres dostawy. */
    public record Address(@NotBlank String street, @Pattern(regexp = "\\d{2}-\\d{3}") String postalCode) {
    }

    /** OrderItem = pozycja zamówienia. */
    public record OrderItem(@NotBlank String sku, @Positive int quantity) {
    }

    /** OrderRequest = zamówienie: adres jako obiekt zagnieżdżony i lista pozycji; @Valid włącza „zejście w głąb”. */
    public record OrderRequest(
            @NotBlank String customer,
            @NotNull @Valid Address address,
            @NotEmpty List<@NotNull @Valid OrderItem> items) {      // element listy: nie null i sprawdzony w głąb
    }

    /** OldStyleOrder = starszy zapis kaskady: {@code @Valid} na polu z listą (działa tak samo jak {@code List<@Valid OrderItem>}). */
    public record OldStyleOrder(@Valid List<OrderItem> items) {
    }

    /** LooseOrder = zamówienie, w którym ZAPOMNIANO o @Valid — reguły adresu są martwe (pułapka z sekcji 1). */
    public record LooseOrder(@NotBlank String customer, Address address) {
    }

    /** NullItemsOrder = lista z elementami bez @NotNull: element null jest przepuszczany po cichu (sekcja 2). */
    public record NullItemsOrder(List<@Valid OrderItem> items) {
    }

    /** TagList = ograniczenia na elementach kontenera: każdy tag niepusty i krótki (sekcja 3). */
    public record TagList(@NotEmpty List<@NotBlank @jakarta.validation.constraints.Size(max = 10) String> tags) {
    }

    // --- grupy (sekcje 4 i 5) ------------------------------------------------------------------------

    /**
     * ProductDto = produkt, którego reguły zależą od operacji: przy tworzeniu id ma być puste (nadaje je serwer),
     * przy aktualizacji — obowiązkowe. Nazwa jest obowiązkowa w obu grupach.
     */
    public record ProductDto(
            @Null(groups = OnCreate.class, message = "Przy tworzeniu id ma być puste")
            @NotNull(groups = OnUpdate.class, message = "Przy aktualizacji id jest wymagane")
            Long id,
            @NotBlank(groups = {OnCreate.class, OnUpdate.class}, message = "Nazwa jest wymagana")
            String name) {
    }

    /** MixedDto = pułapka grupy Default: name nie ma grupy (czyli jest w Default), id ma grupę OnCreate. */
    public record MixedDto(
            @Null(groups = OnCreate.class) Long id,
            @NotBlank String name) {
    }
}
