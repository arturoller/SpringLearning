package s05_validation.val03_nested_and_groups;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy kontrolerów lekcji Val03NestedAndGroups. Wycinek: {@code @WebMvcTest} dwóch kontrolerów + {@code @MockitoBean}
 * serwisu (sprawdzamy, czy błędne dane go w ogóle dotarły). Błędy czytamy z {@code MethodArgumentNotValidException}
 * (Val02ValidInControllers), a nie z treści odpowiedzi, bo bez własnej obsługi w MockMvc jest ona pusta.
 */
@WebMvcTest({OrderController.class, OrderV2Controller.class})
class Val03NestedAndGroupsControllerTest {

    static final String ADDRESS = "\"address\":{\"street\":\"Długa 1\",\"city\":\"Gdańsk\",\"postalCode\":\"80-001\"}";
    static final String LINES = "\"lines\":[{\"productName\":\"Kawa\",\"quantity\":2},{\"productName\":\"Herbata\",\"quantity\":1}]";
    static final String TAGS = "\"tags\":[\"pilne\"]";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderService service;

    private static String order(String prefix) {
        return "{" + prefix + ADDRESS + "," + LINES + "," + TAGS + "}";
    }

    /** Pola z błędami jako posortowane napisy "ścieżka:Kod". */
    private static List<String> errorsOf(MvcResult result) {
        MethodArgumentNotValidException exception = (MethodArgumentNotValidException) result.getResolvedException();
        return exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ":" + error.getCode())
                .sorted()
                .toList();
    }

    // ---------------------------------------------------------------- 1 i 2

    @Test
    @DisplayName("Sekcje 1–2: zagnieżdżony adres, lista pozycji i etykiety — ścieżki z kropką i indeksem")
    void nestedAndCollectionPathsInController() throws Exception {
        String bad = "{" + "\"address\":{\"street\":\"\",\"city\":\" \",\"postalCode\":\"1\"},"
                + "\"lines\":[{\"productName\":\"Kawa\",\"quantity\":2},{\"productName\":\"\",\"quantity\":0}],"
                + "\"tags\":[\"a\",\" \"]}";

        MvcResult result = mockMvc.perform(post("/val03/orders").contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest()).andReturn();

        assertThat(errorsOf(result)).containsExactly(
                "address.city:NotBlank", "address.postalCode:Pattern", "address.street:NotBlank",
                "lines[1].productName:NotBlank", "lines[1].quantity:Min", "tags[1]:NotBlank");
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Sekcja 2: brak adresu i pusta lista pozycji to błędy samych pól address i lines")
    void missingAddressAndEmptyLines() throws Exception {
        MvcResult result = mockMvc.perform(post("/val03/orders").contentType(MediaType.APPLICATION_JSON).content("{\"lines\":[]}"))
                .andExpect(status().isBadRequest()).andReturn();

        assertThat(errorsOf(result)).containsExactly("address:NotNull", "lines:NotEmpty");
    }

    // ---------------------------------------------------------------- 3

    @Test
    @DisplayName("Sekcja 3: POST z @Validated(OnCreate) — bez id → 201, serwis dostaje dane")
    void createWithoutId() throws Exception {
        given(service.create(any(), any(), any())).willReturn(100L);

        mockMvc.perform(post("/val03/orders").contentType(MediaType.APPLICATION_JSON).content(order("")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100));
        verify(service).create(new Address("Długa 1", "Gdańsk", "80-001"),
                List.of(new OrderLine("Kawa", 2), new OrderLine("Herbata", 1)), List.of("pilne"));
    }

    @Test
    @DisplayName("Sekcja 3: POST z id → 400 (id:Null), bo klient nie może narzucić numeru zamówienia")
    void createWithIdIsRejected() throws Exception {
        MvcResult result = mockMvc.perform(post("/val03/orders").contentType(MediaType.APPLICATION_JSON).content(order("\"id\":5,")))
                .andExpect(status().isBadRequest()).andReturn();

        assertThat(errorsOf(result)).containsExactly("id:Null");
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Sekcja 3: PUT z @Validated(OnUpdate) — bez id → 400 (id:NotNull), z id → 200")
    void update() throws Exception {
        MvcResult noId = mockMvc.perform(put("/val03/orders").contentType(MediaType.APPLICATION_JSON).content(order("")))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(errorsOf(noId)).containsExactly("id:NotNull");
        verifyNoInteractions(service);

        mockMvc.perform(put("/val03/orders").contentType(MediaType.APPLICATION_JSON).content(order("\"id\":5,")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
        verify(service).update(eq(5L), any(), any(), any());
    }

    @Test
    @DisplayName("Sekcja 3: OnCreate rozszerza Default, więc przy tworzeniu sprawdzany jest też adres")
    void createStillChecksUngroupedRules() throws Exception {
        String badAddress = "{\"address\":{\"street\":\"\",\"city\":\"Gdańsk\"}," + LINES + "}";

        MvcResult result = mockMvc.perform(post("/val03/orders").contentType(MediaType.APPLICATION_JSON).content(badAddress))
                .andExpect(status().isBadRequest()).andReturn();

        assertThat(errorsOf(result)).containsExactly("address.street:NotBlank");
    }

    @Test
    @DisplayName("Sekcja 3 (PUŁAPKA): zwykłe @Validated bez grupy → grupa Default, więc id przy tworzeniu przechodzi")
    void validatedWithoutGroupIgnoresCreateRules() throws Exception {
        given(service.create(any(), any(), any())).willReturn(100L);

        mockMvc.perform(post("/val03/orders/default-group").contentType(MediaType.APPLICATION_JSON).content(order("\"id\":5,")))
                .andExpect(status().isCreated());   // reguła @Null(groups = OnCreate) nie zadziałała
    }

    // ---------------------------------------------------------------- 5

    @Test
    @DisplayName("Sekcja 5: osobne DTO — CreateOrder nie ma pola id, więc id z JSON-a jest po prostu ignorowane")
    void separateDtoForCreate() throws Exception {
        given(service.create(any(), any(), any())).willReturn(100L);

        mockMvc.perform(post("/val03/v2/orders").contentType(MediaType.APPLICATION_JSON).content(order("\"id\":7,")))
                .andExpect(status().isCreated());   // żadnych grup, a id nie ma jak się prześlizgnąć
        verify(service).create(any(), any(), any());
    }

    @Test
    @DisplayName("Sekcja 5: osobne DTO zmiany wymaga pola version — bez grup")
    void separateDtoForUpdate() throws Exception {
        MvcResult result = mockMvc.perform(put("/val03/v2/orders/1").contentType(MediaType.APPLICATION_JSON).content(order("")))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(errorsOf(result)).containsExactly("version:NotNull");

        mockMvc.perform(put("/val03/v2/orders/1").contentType(MediaType.APPLICATION_JSON).content(order("\"version\":3,")))
                .andExpect(status().isOk());
    }
}
