package s05_validation.val03_nested_and_groups;

import jakarta.validation.GroupSequence;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy ĆWICZEŃ lekcji Val03NestedAndGroups. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@WebMvcTest(ExerciseController.class)
class Val03NestedAndGroupsExercisesTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    Validator validator;

    private static String shipment(String id, String recipient, String street, String city, String codes) {
        return "{" + id + "\"recipient\":\"" + recipient + "\",\"address\":{\"street\":\"" + street + "\",\"city\":\"" + city
                + "\"},\"parcelCodes\":" + codes + "}";
    }

    private static List<String> errorsOf(MvcResult result) {
        MethodArgumentNotValidException exception = (MethodArgumentNotValidException) result.getResolvedException();
        return exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ":" + error.getCode()).sorted().toList();
    }

    @Test
    @DisplayName("Ćwiczenie 1: zagnieżdżony adres jest sprawdzany w głąb (address.city), recipient niepusty")
    void exercise1() throws Exception {
        mockMvc.perform(post("/val03/cw/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content(shipment("", "Ewa", "Długa 1", "Gdańsk", "[\"P1\"]")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipient").value("Ewa"));

        MvcResult result = mockMvc.perform(post("/val03/cw/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content(shipment("", " ", "Długa 1", " ", "[\"P1\"]")))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(errorsOf(result)).containsExactly("address.city:NotBlank", "recipient:NotBlank");
    }

    @Test
    @DisplayName("Ćwiczenie 2: lista kodów — niepusta, a każdy kod niepusty (parcelCodes[1])")
    void exercise2() throws Exception {
        MvcResult empty = mockMvc.perform(post("/val03/cw/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content(shipment("", "Ewa", "Długa 1", "Gdańsk", "[]")))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(errorsOf(empty)).containsExactly("parcelCodes:NotEmpty");

        MvcResult blank = mockMvc.perform(post("/val03/cw/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content(shipment("", "Ewa", "Długa 1", "Gdańsk", "[\"P1\",\" \"]")))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(errorsOf(blank)).containsExactly("parcelCodes[1]:NotBlank");
    }

    @Test
    @DisplayName("Ćwiczenie 3: grupy — POST z id → 400, PUT bez id → 400, PUT z id → 200")
    void exercise3() throws Exception {
        MvcResult createWithId = mockMvc.perform(post("/val03/cw/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content(shipment("\"id\":5,", "Ewa", "Długa 1", "Gdańsk", "[\"P1\"]")))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(errorsOf(createWithId)).containsExactly("id:Null");

        MvcResult updateWithoutId = mockMvc.perform(put("/val03/cw/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content(shipment("", "Ewa", "Długa 1", "Gdańsk", "[\"P1\"]")))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(errorsOf(updateWithoutId)).containsExactly("id:NotNull");

        mockMvc.perform(put("/val03/cw/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content(shipment("\"id\":5,", "Ewa", "Długa 1", "Gdańsk", "[\"P1\"]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    @DisplayName("Ćwiczenie 4: fieldPaths — posortowane, unikalne ścieżki dla wskazanych grup")
    void exercise4() {
        PlainGroupsForm form = new PlainGroupsForm(5L, "");

        assertThat(ExerciseController.fieldPaths(validator, form)).containsExactly("name");
        assertThat(ExerciseController.fieldPaths(validator, form, PlainGroupsForm.Create.class)).containsExactly("id");
        assertThat(ExerciseController.fieldPaths(validator, form, PlainGroupsForm.Create.class, jakarta.validation.groups.Default.class))
                .containsExactly("id", "name");
        assertThat(ExerciseController.fieldPaths(validator, new OrderLine("", 0))).containsExactly("productName", "quantity");
    }

    @Test
    @DisplayName("Ćwiczenie 5: validateInOrder — sekwencja Checks zatrzymuje się po pierwszej grupie z błędami")
    void exercise5() {
        assertThat(ExerciseController.validateInOrder(validator, new CouponForm("", 99))).containsExactly("code:NotBlank");
        assertThat(ExerciseController.validateInOrder(validator, new CouponForm("zly", 99)))
                .containsExactly("code:Pattern", "discountPercent:Max");
        assertThat(ExerciseController.validateInOrder(validator, new CouponForm("ABC-1234", 10))).isEmpty();
        assertThat(ExerciseController.Checks.class.isAnnotationPresent(GroupSequence.class)).isTrue();
    }
}
