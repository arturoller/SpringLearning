package s21_capstone.cap01_shop_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import s21_capstone.cap01_shop_api.common.ConflictException;
import s21_capstone.cap01_shop_api.common.NotFoundException;
import s21_capstone.cap01_shop_api.order.OrderController;
import s21_capstone.cap01_shop_api.order.OrderService;
import s21_capstone.cap01_shop_api.order.OrderStatus;
import s21_capstone.cap01_shop_api.product.Category;
import s21_capstone.cap01_shop_api.product.ProductController;
import s21_capstone.cap01_shop_api.product.ProductDtos.ProductResponse;
import s21_capstone.cap01_shop_api.product.ProductService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy WYCINKA webowego projektu Cap01ShopApi: {@code @WebMvcTest} ładuje kontrolery, poradę błędów (ShopProblemAdvice)
 * i Jacksona — bez bazy i bez serwisów. Serwisy to atrapy ({@code @MockitoBean}), więc sprawdzamy WYŁĄCZNIE warstwę
 * HTTP: adresy, walidację, statusy i kształt ProblemDetail. {@code @Import(Cap01Config.class)} dokłada nasz łańcuch
 * filtrów z {@code permitAll()} — bez niego wycinek użyłby domyślnych zabezpieczeń (Cap01ShopApiDefaultSecurityTest).
 */
@WebMvcTest({ProductController.class, OrderController.class})
@Import(Cap01Config.class)
class Cap01ShopApiWebSliceTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProductService productService;

    @MockitoBean
    OrderService orderService;

    static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    @DisplayName("5. @Valid na ciele: 400 application/problem+json z mapą errors (pole → polski komunikat), serwis nie jest wołany")
    void validationErrorsAsProblemDetail() throws Exception {
        mockMvc.perform(json(post("/api/products"), """
                        {"sku":"zly","name":"","price":-1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Niepoprawne dane"))
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.errors.sku").value("sku ma format ABC-123"))
                .andExpect(jsonPath("$.errors.name").value("nazwa jest wymagana"))
                .andExpect(jsonPath("$.errors.category").value("kategoria jest wymagana"))
                .andExpect(jsonPath("$.errors.price").value("cena musi być dodatnia"))
                .andExpect(jsonPath("$.errors.stock").value("stan jest wymagany"));
        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("5. Walidacja zagnieżdżona: błąd pozycji zamówienia ma ścieżkę lines[0].quantity")
    void nestedValidation() throws Exception {
        mockMvc.perform(json(post("/api/orders"), """
                        {"customerId":1,"lines":[{"sku":"ELE-001","quantity":0},{"sku":""}]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['lines[0].quantity']").value("ilość musi być dodatnia"))
                .andExpect(jsonPath("$.errors['lines[1].sku']").value("sku jest wymagane"))
                .andExpect(jsonPath("$.errors['lines[1].quantity']").value("ilość jest wymagana"));
    }

    @Test
    @DisplayName("5. Parametry zapytania: size=51 i nieznany sort → 400 z wyjaśnieniem; zła kategoria → 400 z klasy bazowej")
    void requestParameterErrors() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.size").value("rozmiar strony: od 1 do 50"));
        mockMvc.perform(get("/api/products").param("sort", "purchasePrice"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SORT_NOT_ALLOWED"))
                .andExpect(jsonPath("$.allowed").value(org.hamcrest.Matchers.contains("name", "price", "price,desc", "sku")));
        mockMvc.perform(get("/api/products").param("category", "ZABAWKI"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Failed to convert 'category' with value: 'ZABAWKI'"));
        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("5. Wyjątki domeny z serwisu: NotFoundException → 404, ConflictException → 409 z polami szczegółów")
    void domainExceptionsMapToStatuses() throws Exception {
        when(productService.get("XXX-999")).thenThrow(new NotFoundException("produkt", "XXX-999"));
        mockMvc.perform(get("/api/products/XXX-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Nie znaleziono: produkt XXX-999"))
                .andExpect(jsonPath("$.type").value("https://sklep.example/problemy/not-found"))
                .andExpect(jsonPath("$.instance").value("/api/products/XXX-999"));

        when(orderService.changeStatus(3L, OrderStatus.OPLACONE)).thenThrow(new ConflictException(
                "ORDER_STATUS_CHANGE", "Nie można zmienić statusu zamówienia ZAM-003 z WYSLANE na OPLACONE",
                Map.of("currentStatus", "WYSLANE", "allowed", List.of("DOSTARCZONE"))));
        mockMvc.perform(json(patch("/api/orders/3/status"), """
                        {"status":"OPLACONE"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_STATUS_CHANGE"))
                .andExpect(jsonPath("$.currentStatus").value("WYSLANE"))
                .andExpect(jsonPath("$.allowed[0]").value("DOSTARCZONE"));
    }

    @Test
    @DisplayName("5. Nieznana wartość enuma w JSON-ie („ZAGUBIONE”) → 400 „Failed to read request” jeszcze przed walidacją")
    void unknownEnumValue() throws Exception {
        mockMvc.perform(json(patch("/api/orders/1/status"), """
                        {"status":"ZAGUBIONE"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Failed to read request"));
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("1. POST udany → 201 Created + Location z adresem nowego produktu, ciało to DTO bez technicznego id")
    void createdWithLocation() throws Exception {
        when(productService.create(any())).thenReturn(
                new ProductResponse("DOM-003", "Czajnik", Category.DOM, new BigDecimal("149.00"), 5, true));
        mockMvc.perform(json(post("/api/products"), """
                        {"sku":"DOM-003","name":"Czajnik","category":"DOM","price":149.00,"stock":5}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/products/DOM-003"))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    @DisplayName("1. Lista produktów: kontroler zamienia parametry na Pageable z białej listy sortowania")
    void searchBuildsPageable() throws Exception {
        when(productService.search(eq(Category.KSIAZKI), eq("kod"), any()))
                .thenReturn(new s21_capstone.cap01_shop_api.common.PageResponse<>(List.of(), 0, 5, 0, 0));
        mockMvc.perform(get("/api/products").param("category", "KSIAZKI").param("q", "kod").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"content":[],"page":0,"size":5,"totalElements":0,"totalPages":0}""", true));
    }
}
