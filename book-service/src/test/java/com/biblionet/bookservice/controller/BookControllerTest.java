package com.biblionet.bookservice.controller;

import com.biblionet.bookservice.dto.AvailabilityRequestDto;
import com.biblionet.bookservice.dto.BookRequestDto;
import com.biblionet.bookservice.dto.BookResponseDto;
import com.biblionet.bookservice.exception.ResourceNotFoundException;
import com.biblionet.bookservice.service.BookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookService bookService;

    @Test
    void createBookReturns201WithAvailableBook() throws Exception {
        BookRequestDto request = new BookRequestDto("Na Drini ćuprija", "Ivo Andrić", "978-86-1234-567-8");
        BookResponseDto response = new BookResponseDto(1L, "Na Drini ćuprija", "Ivo Andrić", "978-86-1234-567-8", true);
        given(bookService.createBook(any(BookRequestDto.class))).willReturn(response);

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Na Drini ćuprija"))
                .andExpect(jsonPath("$.author").value("Ivo Andrić"))
                .andExpect(jsonPath("$.isbn").value("978-86-1234-567-8"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void createBookWithInvalidPayloadReturns400WithFieldErrors() throws Exception {
        BookRequestDto request = new BookRequestDto("", "Ivo Andrić", "");

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/books"))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.isbn").exists());

        verifyNoInteractions(bookService);
    }

    @Test
    void createBookWithDuplicateIsbnReturns409() throws Exception {
        given(bookService.createBook(any(BookRequestDto.class)))
                .willThrow(new org.springframework.dao.DataIntegrityViolationException("unique constraint"));

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BookRequestDto("Seobe", "Miloš Crnjanski", "978-86-7654-321-0"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Knjiga sa datim ISBN-om već postoji"))
                .andExpect(jsonPath("$.path").value("/books"));
    }

    @Test
    void getAllBooksReturnsList() throws Exception {
        given(bookService.getAllBooks()).willReturn(List.of(
                new BookResponseDto(1L, "Na Drini ćuprija", "Ivo Andrić", "978-86-1234-567-8", true),
                new BookResponseDto(2L, "Seobe", "Miloš Crnjanski", "978-86-7654-321-0", false)
        ));

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].title").value("Seobe"))
                .andExpect(jsonPath("$[1].available").value(false));
    }

    @Test
    void getBookByIdReturnsBook() throws Exception {
        given(bookService.getBookById(1L)).willReturn(
                new BookResponseDto(1L, "Na Drini ćuprija", "Ivo Andrić", "978-86-1234-567-8", true));

        mockMvc.perform(get("/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Na Drini ćuprija"));
    }

    @Test
    void getBookByIdReturns404WhenMissing() throws Exception {
        given(bookService.getBookById(99L))
                .willThrow(new ResourceNotFoundException("Knjiga sa ID 99 nije pronađena"));

        mockMvc.perform(get("/books/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Knjiga sa ID 99 nije pronađena"))
                .andExpect(jsonPath("$.path").value("/books/99"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void updateAvailabilityMarksBookAsUnavailable() throws Exception {
        given(bookService.updateAvailability(1L, false)).willReturn(
                new BookResponseDto(1L, "Na Drini ćuprija", "Ivo Andrić", "978-86-1234-567-8", false));

        mockMvc.perform(patch("/books/1/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AvailabilityRequestDto(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.available").value(false));

        verify(bookService).updateAvailability(1L, false);
    }

    @Test
    void updateAvailabilityMarksBookAsAvailable() throws Exception {
        given(bookService.updateAvailability(1L, true)).willReturn(
                new BookResponseDto(1L, "Na Drini ćuprija", "Ivo Andrić", "978-86-1234-567-8", true));

        mockMvc.perform(patch("/books/1/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AvailabilityRequestDto(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));

        verify(bookService).updateAvailability(1L, true);
    }

    @Test
    void updateAvailabilityReturns404WhenBookMissing() throws Exception {
        given(bookService.updateAvailability(99L, false))
                .willThrow(new ResourceNotFoundException("Knjiga sa ID 99 nije pronađena"));

        mockMvc.perform(patch("/books/99/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AvailabilityRequestDto(false))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Knjiga sa ID 99 nije pronađena"))
                .andExpect(jsonPath("$.path").value("/books/99/availability"));
    }

    @Test
    void updateAvailabilityWithMissingFieldReturns400() throws Exception {
        mockMvc.perform(patch("/books/1/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.available").exists());

        verifyNoInteractions(bookService);
    }

}
