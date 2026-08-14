package com.biblionet.bookservice.integration;

import com.biblionet.bookservice.dto.AvailabilityRequestDto;
import com.biblionet.bookservice.dto.BookRequestDto;
import com.biblionet.bookservice.repository.BookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integracioni test - ceo kontekst, prava baza, bez mokova.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void resetState() {
        bookRepository.deleteAll();
    }

    @Test
    void createdBookIsPersistedAsAvailableAndRetrievable() throws Exception {
        Long id = createBook("Seobe", "Miloš Crnjanski", "978-86-7654-321-0");

        assertThat(bookRepository.findById(id)).isPresent();

        mockMvc.perform(get("/books/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Seobe"))
                .andExpect(jsonPath("$.available").value(true));
    }

    /** Ovaj tok koristi loan-service preko Feign-a pri pozajmljivanju i vracanju. */
    @Test
    void availabilityCanBeToggledAndIsPersisted() throws Exception {
        Long id = createBook("Seobe", "Miloš Crnjanski", "978-86-7654-321-0");

        mockMvc.perform(patch("/books/{id}/availability", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AvailabilityRequestDto(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));

        assertThat(bookRepository.findById(id).orElseThrow().isAvailable()).isFalse();

        mockMvc.perform(patch("/books/{id}/availability", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AvailabilityRequestDto(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));

        assertThat(bookRepository.findById(id).orElseThrow().isAvailable()).isTrue();
    }

    @Test
    void duplicateIsbnReturns409() throws Exception {
        createBook("Seobe", "Miloš Crnjanski", "978-86-0000-000-0");

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BookRequestDto("Druga", "Drugi", "978-86-0000-000-0"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Knjiga sa datim ISBN-om već postoji"));

        assertThat(bookRepository.findAll()).hasSize(1);
    }

    @Test
    void invalidPayloadIsRejectedAndNothingIsPersisted() throws Exception {
        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookRequestDto("", "Autor", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.isbn").exists());

        assertThat(bookRepository.findAll()).isEmpty();
    }

    @Test
    void availabilityUpdateOnUnknownBookReturns404() throws Exception {
        mockMvc.perform(patch("/books/{id}/availability", 9999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AvailabilityRequestDto(false))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private Long createBook(String title, String author, String isbn) throws Exception {
        String response = mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookRequestDto(title, author, isbn))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

}
