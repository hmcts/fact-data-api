package uk.gov.hmcts.reform.fact.data.api.controllers;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtPhoto;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtPhotoService;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import javax.imageio.ImageIO;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("Court Photo Controller")
@DisplayName("Court Photo Controller")
@WebMvcTest(CourtPhotoController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourtPhotoControllerTest {

    private static final UUID COURT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID UNKNOWN_COURT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourtPhotoService courtPhotoService;

    @Test
    @DisplayName("GET /courts/{courtId}/v1/photo returns court photo")
    void getCourtPhotoReturnsOk() throws Exception {
        CourtPhoto courtPhoto = new CourtPhoto();
        courtPhoto.setId(UUID.randomUUID());
        courtPhoto.setCourtId(COURT_ID);
        courtPhoto.setFileLink("https://example.com/photo.jpg");

        when(courtPhotoService.getCourtPhotoByCourtId(COURT_ID)).thenReturn(courtPhoto);

        mockMvc.perform(get("/courts/{courtId}/v1/photo", COURT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.courtId").value(COURT_ID.toString()))
            .andExpect(jsonPath("$.fileLink").value("https://example.com/photo.jpg"));
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/photo returns 404 when missing")
    void getCourtPhotoReturnsNotFound() throws Exception {
        when(courtPhotoService.getCourtPhotoByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new NotFoundException("Photo not found"));

        mockMvc.perform(get("/courts/{courtId}/v1/photo", UNKNOWN_COURT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/photo returns 400 for invalid UUID")
    void getCourtPhotoReturnsBadRequestForInvalidUuid() throws Exception {
        mockMvc.perform(get("/courts/{courtId}/v1/photo", "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/photo uploads photo")
    void setCourtPhotoReturnsCreated() throws Exception {
        CourtPhoto courtPhoto = new CourtPhoto();
        courtPhoto.setId(UUID.randomUUID());
        courtPhoto.setCourtId(COURT_ID);
        courtPhoto.setFileLink("https://example.com/photo.jpg");

        when(courtPhotoService.setCourtPhoto(eq(COURT_ID), any())).thenReturn(courtPhoto);

        mockMvc.perform(multipart("/courts/{courtId}/v1/photo", COURT_ID)
                            .file(buildMultipartFile())
                            .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.fileLink").value("https://example.com/photo.jpg"));
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/photo returns 404 when court missing")
    void setCourtPhotoReturnsNotFound() throws Exception {
        when(courtPhotoService.setCourtPhoto(eq(UNKNOWN_COURT_ID), any()))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(multipart("/courts/{courtId}/v1/photo", UNKNOWN_COURT_ID)
                            .file(buildMultipartFile())
                            .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/photo returns 400 for invalid UUID")
    void setCourtPhotoReturnsBadRequestForInvalidUuid() throws Exception {
        mockMvc.perform(multipart("/courts/{courtId}/v1/photo", "invalid-uuid")
                            .file(buildMultipartFile())
                            .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/photo removes photo")
    void deleteCourtPhotoReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/courts/{courtId}/v1/photo", COURT_ID))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/photo returns 404 when missing")
    void deleteCourtPhotoReturnsNotFound() throws Exception {
        doThrow(new NotFoundException("Photo not found"))
            .when(courtPhotoService).deleteCourtPhotoByCourtId(UNKNOWN_COURT_ID);

        mockMvc.perform(delete("/courts/{courtId}/v1/photo", UNKNOWN_COURT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/photo returns 400 for invalid UUID")
    void deleteCourtPhotoReturnsBadRequestForInvalidUuid() throws Exception {
        mockMvc.perform(delete("/courts/{courtId}/v1/photo", "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }

    private MockMultipartFile buildMultipartFile() {
        return new MockMultipartFile("file", "photo.jpg", "image/jpeg", createImageBytes());
    }

    private byte[] createImageBytes() {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xFFFFFF);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new RuntimeException("Failed to create test image content", exception);
        }
    }
}

