package s05_validation.val03_nested_and_groups;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Kontroler zamówień z JEDNYM DTO i grupami walidacji. Uwaga: {@code @Valid} nie przyjmuje grup — do wyboru grupy
 * służy {@code @Validated(Grupa.class)} (adnotacja Springa).
 */
@RestController
@RequestMapping("/val03/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    /** Tworzenie: grupa OnCreate (id musi być null) + wszystko bez grupy (Default), bo OnCreate rozszerza Default. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> create(@Validated(OnCreate.class) @RequestBody OrderRequest order) {
        return Map.of("id", service.create(order.address(), order.lines(), order.tags()));
    }

    /** Zmiana: grupa OnUpdate (id musi być podany) + Default. */
    @PutMapping
    public Map<String, Long> update(@Validated(OnUpdate.class) @RequestBody OrderRequest order) {
        service.update(order.id(), order.address(), order.lines(), order.tags());
        return Map.of("id", order.id());
    }

    /** PUŁAPKA (sekcja 3): zwykłe {@code @Validated} bez grupy = grupa Default; reguły OnCreate/OnUpdate nie działają. */
    @PostMapping("/default-group")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> createDefaultGroup(@Validated @RequestBody OrderRequest order) {
        return Map.of("id", service.create(order.address(), order.lines(), order.tags()));
    }
}
