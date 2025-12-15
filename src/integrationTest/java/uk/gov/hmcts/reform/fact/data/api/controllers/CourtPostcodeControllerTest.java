
package uk.gov.hmcts.reform.fact.data.api.controllers;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtPostcode;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.DuplicatedListItemException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidPostcodeMigrationRequestException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeListDto;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeMoveDto;
import uk.gov.hmcts.reform.fact.data.api.services.CourtPostcodeService;

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

@Feature("Court Postcodes Controller")
@DisplayName("Court Postcodes Controller")
@WebMvcTest(controllers = CourtPostcodeController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourtPostcodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourtPostcodeService courtPostcodeService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final UUID COURT_UUID = UUID.randomUUID();
    private static final String COURT_ID = COURT_UUID.toString();

    private static final String BASE_URL = "/courts/{courtId}/v1/postcodes";
    private static final String MIGRATE_URL = "/courts/{courtId}/v1/postcodes/move";

    @Nested
    @DisplayName("GET /courts/{courtId}/v1/postcodes")
    class GetCourtPostcodes {

        @Test
        @DisplayName("returns 200 with body when list is non-empty")
        void getReturnsOkWithBody() throws Exception {
            List<CourtPostcode> list = List.of(
                CourtPostcode.builder().courtId(COURT_UUID).postcode("SW11AA").build(),
                CourtPostcode.builder().courtId(COURT_UUID).postcode("EC11BB").build()
            );

            when(courtPostcodeService.getPostcodesByCourtId(COURT_UUID)).thenReturn(list);

            mockMvc.perform(get(BASE_URL, COURT_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].postcode", is("SW11AA")))
                .andExpect(jsonPath("$[0].courtId", is(COURT_ID)))
                .andExpect(jsonPath("$[1].postcode", is("EC11BB")))
                .andExpect(jsonPath("$[1].courtId", is(COURT_ID)));

            verify(courtPostcodeService).getPostcodesByCourtId(COURT_UUID);
        }

        @Test
        @DisplayName("returns 204 when list is empty")
        void getReturnsNoContentWhenEmpty() throws Exception {
            when(courtPostcodeService.getPostcodesByCourtId(COURT_UUID)).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL, COURT_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

            verify(courtPostcodeService).getPostcodesByCourtId(COURT_UUID);
        }

        @Test
        @DisplayName("returns 204 when service returns null")
        void getReturnsNoContentWhenNull() throws Exception {
            when(courtPostcodeService.getPostcodesByCourtId(COURT_UUID)).thenReturn(null);

            mockMvc.perform(get(BASE_URL, COURT_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

            verify(courtPostcodeService).getPostcodesByCourtId(COURT_UUID);
        }

        @Test
        @DisplayName("returns 400 on invalid UUID")
        void getReturnsBadRequestOnInvalidUuid() throws Exception {
            mockMvc.perform(get(BASE_URL, "not-a-uuid"))
                .andExpect(status().isBadRequest());
            verifyNoInteractions(courtPostcodeService);
        }

        @Test
        @DisplayName("returns 404 when service throws NotFoundException")
        void getPropagatesNotFoundException() throws Exception {
            when(courtPostcodeService.getPostcodesByCourtId(COURT_UUID))
                .thenThrow(new NotFoundException("Court not found"));

            mockMvc.perform(get(BASE_URL, COURT_ID))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /courts/{courtId}/v1/postcodes")
    class AddCourtPostcodes {

        @Test
        @DisplayName("returns 204 No Content and invokes service")
        void postReturnsNoContentAndCallsService() throws Exception {
            String body = objectMapper.writeValueAsString(
                new PostcodeListDto(asList("SW1 1AA", "EC1 1BB", "E10"))
            );

            mockMvc.perform(post(BASE_URL, COURT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isNoContent());

            ArgumentCaptor<PostcodeListDto> dtoCaptor = ArgumentCaptor.forClass(PostcodeListDto.class);
            verify(courtPostcodeService).addPostcodesToCourt(dtoCaptor.capture(), eq(COURT_UUID));
            PostcodeListDto sent = dtoCaptor.getValue();
            assertEquals(3, sent.getPostcodes().size());
            assertEquals("SW1 1AA", sent.getPostcodes().getFirst());
            assertEquals("EC1 1BB", sent.getPostcodes().get(1));
            assertEquals("E10", sent.getPostcodes().getLast());
        }

        @Test
        @DisplayName("returns 400 on invalid UUID")
        void postReturnsBadRequestOnInvalidUuid() throws Exception {
            String body = objectMapper.writeValueAsString(
                new PostcodeListDto(List.of("SW1 1AA"))
            );

            mockMvc.perform(post(BASE_URL, "not-a-uuid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(courtPostcodeService);
        }

        @Test
        @DisplayName("returns 404 when service throws NotFoundException")
        void postPropagatesNotFound() throws Exception {
            String body = objectMapper.writeValueAsString(new PostcodeListDto(List.of("SW1 1AA")));

            doThrow(new NotFoundException("Court not found"))
                .when(courtPostcodeService).addPostcodesToCourt(any(PostcodeListDto.class), any(UUID.class));

            mockMvc.perform(post(BASE_URL, COURT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 400 when service throws DuplicatedListItemException")
        void postPropagatesBadRequestOnDuplicates() throws Exception {
            String body = objectMapper.writeValueAsString(new PostcodeListDto((List.of("SW1 1AA", "sw11aa"))));

            doThrow(new DuplicatedListItemException("Duplicated Postcode"))
                .when(courtPostcodeService).addPostcodesToCourt(any(PostcodeListDto.class), any(UUID.class));

            mockMvc.perform(post(BASE_URL, COURT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /courts/{courtId}/v1/postcodes")
    class RemoveCourtPostcodes {

        @Test
        @DisplayName("returns 204 and invokes service")
        void deleteReturnsNoContentAndCallsService() throws Exception {
            String body = objectMapper.writeValueAsString(new PostcodeListDto(List.of("SW1 1AA")));

            mockMvc.perform(delete(BASE_URL, COURT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isNoContent());

            verify(courtPostcodeService).removePostcodesFromCourt(any(PostcodeListDto.class), eq(COURT_UUID));
        }

        @Test
        @DisplayName("returns 400 on invalid UUID")
        void deleteReturnsBadRequestOnInvalidUuid() throws Exception {
            String body = objectMapper.writeValueAsString(new PostcodeListDto(List.of("SW1 1AA")));

            mockMvc.perform(delete(BASE_URL, "not-a-uuid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(courtPostcodeService);
        }

        @Test
        @DisplayName("returns 404 when service throws NotFoundException for unassigned postcodes")
        void deletePropagatesNotFound() throws Exception {
            String body = objectMapper.writeValueAsString(new PostcodeListDto(List.of("EC1 1BB")));

            doThrow(new NotFoundException("Unassigned postcode"))
                .when(courtPostcodeService).removePostcodesFromCourt(any(PostcodeListDto.class), any(UUID.class));

            mockMvc.perform(delete(BASE_URL, COURT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 400 when service throws DuplicatedListItemException")
        void deletePropagatesBadRequestOnDuplicates() throws Exception {
            String body = objectMapper.writeValueAsString(new PostcodeListDto((List.of("SW1 1AA", "sw11aa"))));

            doThrow(new DuplicatedListItemException("Duplicated"))
                .when(courtPostcodeService).removePostcodesFromCourt(any(PostcodeListDto.class), any(UUID.class));

            mockMvc.perform(delete(BASE_URL, COURT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /courts/{courtId}/v1/postcodes/move")
    class MigrateCourtPostcodes {

        @Test
        @DisplayName("returns 204 No Content and calls service")
        void moveReturnsNoContentAndCallsService() throws Exception {
            String source = UUID.randomUUID().toString();
            String dest = UUID.randomUUID().toString();

            String body = """
                {
                  "sourceCourtId": "%s",
                  "destinationCourtId": "%s",
                  "postcodeList": { "postcodes": ["SW1 1AA", "EC1 1BB"] }
                }
                """.formatted(source, dest);

            mockMvc.perform(post(MIGRATE_URL, COURT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isNoContent());

            verify(courtPostcodeService).migratePostcodes(any(PostcodeMoveDto.class));
        }

        @Test
        @DisplayName("returns 400 when service throws InvalidPostcodeMigrationRequestException")
        void movePropagatesBadRequestOnInvalidMigration() throws Exception {
            String source = UUID.randomUUID().toString();

            String body = """
                {
                  "sourceCourtId": "%s",
                  "destinationCourtId": "%s",
                  "postcodeList": { "postcodes": ["SW1 1AA"] }
                }
                """.formatted(source, source);

            doThrow(new InvalidPostcodeMigrationRequestException("Source and Destination court IDs are the same"))
                .when(courtPostcodeService).migratePostcodes(any(PostcodeMoveDto.class));

            mockMvc.perform(post(MIGRATE_URL, COURT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 404 when service throws NotFoundException")
        void movePropagatesNotFound() throws Exception {
            String source = UUID.randomUUID().toString();
            String dest = UUID.randomUUID().toString();

            String body = """
                {
                  "sourceCourtId": "%s",
                  "destinationCourtId": "%s",
                  "postcodeList": { "postcodes": ["SW1 1AA"] }
                }
                """.formatted(source, dest);

            doThrow(new NotFoundException("Court not found"))
                .when(courtPostcodeService).migratePostcodes(any(PostcodeMoveDto.class));

            mockMvc.perform(post(MIGRATE_URL, COURT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isNotFound());
        }
    }
}
