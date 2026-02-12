
package uk.gov.hmcts.reform.fact.data.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.models.AreaOfLawSelectionDto;
import uk.gov.hmcts.reform.fact.data.api.services.CourtSinglePointsOfEntryService;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Feature("Court Single Points of Entry Controller")
@DisplayName("Court Single Points of Entry Controller")
@WebMvcTest(controllers = CourtSinglePointsOfEntryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourtSinglePointsOfEntryControllerTest {

    static AreaOfLawType adoption = AreaOfLawType.builder()
        .id(UUID.randomUUID())
        .name("Adoption")
        .nameCy("AdoptionCy")
        .build();

    static AreaOfLawType children = AreaOfLawType.builder()
        .id(UUID.randomUUID())
        .name("Children")
        .nameCy("ChildrenCy")
        .build();

    static AreaOfLawType civilPartnership = AreaOfLawType.builder()
        .id(UUID.randomUUID())
        .name("Civil Partnership")
        .nameCy("Civil Partnership Cy")
        .build();

    static AreaOfLawType divorce = AreaOfLawType.builder()
        .id(UUID.randomUUID())
        .name("Divorce")
        .nameCy("DivorceCy")
        .build();

    static List<AreaOfLawType> allowedLocalAuthorityAreas = List.of(adoption, children, civilPartnership, divorce);

    static UUID courtId = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourtSinglePointsOfEntryService courtSinglePointOfEntryService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PATH = "/courts/{courtId}/v1/single-point-of-entry";

    @Nested
    @DisplayName("GET /courts/{courtId}/v1/single-point-of-entry")
    class GetSinglePointsOfEntry {

        @Test
        @DisplayName("returns 200 and the full DTO")
        void returns200AndExpectedList() throws Exception {

            List<AreaOfLawSelectionDto> serviceResult = List.of(
                AreaOfLawSelectionDto.asUnselected(adoption),
                AreaOfLawSelectionDto.asSelected(children),
                AreaOfLawSelectionDto.asSelected(divorce),
                AreaOfLawSelectionDto.asSelected(civilPartnership)
            );

            when(courtSinglePointOfEntryService.getCourtSinglePointsOfEntry(courtId)).thenReturn(serviceResult);

            mockMvc.perform(get(PATH, courtId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(adoption.getId().toString()))
                .andExpect(jsonPath("$[0].name").value(adoption.getName()))
                .andExpect(jsonPath("$[0].nameCy").value(adoption.getNameCy()))
                .andExpect(jsonPath("$[0].selected").value(false))
                .andExpect(jsonPath("$[1].id").value(children.getId().toString()))
                .andExpect(jsonPath("$[1].name").value(children.getName()))
                .andExpect(jsonPath("$[1].nameCy").value(children.getNameCy()))
                .andExpect(jsonPath("$[1].selected").value(true))
                .andExpect(jsonPath("$[2].id").value(divorce.getId().toString()))
                .andExpect(jsonPath("$[2].name").value(divorce.getName()))
                .andExpect(jsonPath("$[2].nameCy").value(divorce.getNameCy()))
                .andExpect(jsonPath("$[2].selected").value(true))
                .andExpect(jsonPath("$[3].id").value(civilPartnership.getId().toString()))
                .andExpect(jsonPath("$[3].name").value(civilPartnership.getName()))
                .andExpect(jsonPath("$[3].nameCy").value(civilPartnership.getNameCy()))
                .andExpect(jsonPath("$[3].selected").value(true));

            ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);
            verify(courtSinglePointOfEntryService).getCourtSinglePointsOfEntry(idCaptor.capture());
            assertEquals(courtId, idCaptor.getValue());
        }

        @Test
        @DisplayName("invalid courtId UUID returns 400 and does not hit service")
        void invalidUuidReturns400() throws Exception {
            mockMvc.perform(get(PATH, "not-a-uuid"))
                .andExpect(status().isBadRequest());
            verifyNoInteractions(courtSinglePointOfEntryService);
        }

        @Test
        @DisplayName("missing court returns 404 and does not hit service")
        void courtNotFoundReturns404() throws Exception {
            when(courtSinglePointOfEntryService.getCourtSinglePointsOfEntry(courtId))
                .thenThrow(NotFoundException.class);
            mockMvc.perform(get(PATH, courtId.toString()))
                .andExpect(status().isNotFound());
            verify(courtSinglePointOfEntryService).getCourtSinglePointsOfEntry(courtId);
        }
    }

    @Nested
    @DisplayName("PUT /courts/{courtId}/v1/single-point-of-entry")
    class UpdateSinglePointsOfEntry {

        @Test
        @DisplayName("valid payload returns 200 and calls service")
        void validPayloadReturns200() throws Exception {

            List<AreaOfLawSelectionDto> payload = List.of(
                AreaOfLawSelectionDto.asSelected(civilPartnership),
                AreaOfLawSelectionDto.asUnselected(divorce)
            );

            mockMvc.perform(put(PATH, courtId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

            verify(courtSinglePointOfEntryService)
                .updateCourtSinglePointsOfEntry(eq(courtId), eq(payload));
        }

        @Test
        @DisplayName("invalid courtId return 400")
        void invalidCourtId() throws Exception {
            mockMvc.perform(put(PATH, "invalid-uuid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(List.of(AreaOfLawSelectionDto.asSelected(
                                    civilPartnership)))))
                .andExpect(status().isBadRequest());
            verifyNoInteractions(courtSinglePointOfEntryService);
        }

        @Test
        @DisplayName("Malformed payload return 400")
        void malformedPayloadReturns400() throws Exception {
            String json = """
                [
                  {
                    "id": "%s"
                    "name": "Family",
                    "selected": true
                  }
                ]
                """.formatted(UUID.randomUUID().toString());

            mockMvc.perform(put(PATH, courtId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(courtSinglePointOfEntryService);
        }

        @Test
        @DisplayName("partially valid payload returns 400")
        void mixedValidityListReturns400() throws Exception {
            // First element valid; second missing selected
            String json = """
                [
                  {
                    "id": "%s",
                    "selected": false,
                    "name": "Civil"
                  },
                  {
                    "name": "Family"
                  }
                ]
                """.formatted(UUID.randomUUID().toString());

            mockMvc.perform(put(PATH, courtId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(courtSinglePointOfEntryService);
        }

        @Test
        @DisplayName("malformed JSON body returns 400")
        void malformedJsonReturns400() throws Exception {
            mockMvc.perform(put(PATH, courtId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("not-json"))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(courtSinglePointOfEntryService);
        }

        @Test
        @DisplayName("missing court returns 404")
        void courtNotFoundReturns404() throws Exception {

            List<AreaOfLawSelectionDto> payload = List.of(
                AreaOfLawSelectionDto.asSelected(civilPartnership),
                AreaOfLawSelectionDto.asUnselected(divorce)
            );

            doThrow(NotFoundException.class).when(courtSinglePointOfEntryService)
                .updateCourtSinglePointsOfEntry(courtId, payload);

            mockMvc.perform(put(PATH, courtId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());

            verify(courtSinglePointOfEntryService).updateCourtSinglePointsOfEntry(courtId, payload);
        }
    }
}
