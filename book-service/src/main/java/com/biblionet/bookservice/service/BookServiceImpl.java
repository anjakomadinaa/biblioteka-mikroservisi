package com.biblionet.bookservice.service;

import com.biblionet.bookservice.dto.BookRequestDto;
import com.biblionet.bookservice.dto.BookResponseDto;
import com.biblionet.bookservice.entity.Book;
import com.biblionet.bookservice.exception.ResourceNotFoundException;
import com.biblionet.bookservice.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    public BookServiceImpl(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    @Transactional
    public BookResponseDto createBook(BookRequestDto request) {
        Book book = new Book(request.getTitle(), request.getAuthor(), request.getIsbn());
        return toResponse(bookRepository.save(book));
    }

    @Override
    public List<BookResponseDto> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public BookResponseDto getBookById(Long id) {
        return toResponse(findBookOrThrow(id));
    }

    @Override
    @Transactional
    public BookResponseDto updateAvailability(Long id, boolean available) {
        Book book = findBookOrThrow(id);
        book.setAvailable(available);
        return toResponse(bookRepository.save(book));
    }

    private Book findBookOrThrow(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Knjiga sa ID " + id + " nije pronađena"));
    }

    private BookResponseDto toResponse(Book book) {
        return new BookResponseDto(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.isAvailable()
        );
    }

}
