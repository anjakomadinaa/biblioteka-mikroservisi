package com.biblionet.bookservice.service;

import com.biblionet.bookservice.dto.BookRequestDto;
import com.biblionet.bookservice.dto.BookResponseDto;
import com.biblionet.bookservice.entity.Book;
import com.biblionet.bookservice.exception.ResourceNotFoundException;
import com.biblionet.bookservice.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit testovi servisnog sloja - repozitorijum je mokovan.
 */
@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    private BookServiceImpl bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookServiceImpl(bookRepository);
    }

    @Test
    void createdBookIsAlwaysAvailableRegardlessOfRequest() {
        given(bookRepository.save(any(Book.class))).willAnswer(inv -> inv.getArgument(0));

        bookService.createBook(new BookRequestDto("Seobe", "Miloš Crnjanski", "978-86-7654-321-0"));

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        Book saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Seobe");
        assertThat(saved.getAuthor()).isEqualTo("Miloš Crnjanski");
        assertThat(saved.getIsbn()).isEqualTo("978-86-7654-321-0");
        assertThat(saved.isAvailable()).isTrue();
    }

    @Test
    void getAllBooksMapsEveryEntity() {
        given(bookRepository.findAll()).willReturn(List.of(
                new Book("Seobe", "Miloš Crnjanski", "978-86-7654-321-0"),
                new Book("Prokleta avlija", "Ivo Andrić", "978-86-1111-222-3")));

        List<BookResponseDto> result = bookService.getAllBooks();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(BookResponseDto::getTitle)
                .containsExactly("Seobe", "Prokleta avlija");
    }

    @Test
    void getBookByIdThrowsWhenBookDoesNotExist() {
        given(bookRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Knjiga sa ID 99 nije pronađena");
    }

    @Test
    void updateAvailabilitySavesChangedFlag() {
        Book book = new Book("Seobe", "Miloš Crnjanski", "978-86-7654-321-0");
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(bookRepository.save(any(Book.class))).willAnswer(inv -> inv.getArgument(0));

        BookResponseDto result = bookService.updateAvailability(1L, false);

        assertThat(result.isAvailable()).isFalse();
        assertThat(book.isAvailable()).isFalse();
        verify(bookRepository).save(book);
    }

    @Test
    void updateAvailabilityCanMakeBookAvailableAgain() {
        Book book = new Book("Seobe", "Miloš Crnjanski", "978-86-7654-321-0");
        book.setAvailable(false);
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(bookRepository.save(any(Book.class))).willAnswer(inv -> inv.getArgument(0));

        assertThat(bookService.updateAvailability(1L, true).isAvailable()).isTrue();
    }

    @Test
    void updateAvailabilityThrowsAndSavesNothingWhenBookMissing() {
        given(bookRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.updateAvailability(99L, false))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(bookRepository, never()).save(any());
    }

}
