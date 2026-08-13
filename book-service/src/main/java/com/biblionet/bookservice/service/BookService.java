package com.biblionet.bookservice.service;

import com.biblionet.bookservice.dto.BookRequestDto;
import com.biblionet.bookservice.dto.BookResponseDto;

import java.util.List;

public interface BookService {

    BookResponseDto createBook(BookRequestDto request);

    List<BookResponseDto> getAllBooks();

    BookResponseDto getBookById(Long id);

    BookResponseDto updateAvailability(Long id, boolean available);

}
