package com.biblionet.bookservice.repository;

import com.biblionet.bookservice.entity.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testira ponasanje entiteta na pravoj bazi - unique constraint na ISBN-u
 * i podrazumevana dostupnost se ne mogu proveriti mokovanim repozitorijumom.
 */
@DataJpaTest
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Test
    void newBookIsPersistedAsAvailable() {
        Book saved = bookRepository.saveAndFlush(new Book("Seobe", "Miloš Crnjanski", "978-86-7654-321-0"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.isAvailable()).isTrue();
    }

    @Test
    void availabilityChangeIsPersisted() {
        Book saved = bookRepository.saveAndFlush(new Book("Seobe", "Miloš Crnjanski", "978-86-7654-321-0"));
        saved.setAvailable(false);
        bookRepository.saveAndFlush(saved);

        assertThat(bookRepository.findById(saved.getId()).orElseThrow().isAvailable()).isFalse();
    }

    @Test
    void duplicateIsbnIsRejectedByUniqueConstraint() {
        bookRepository.saveAndFlush(new Book("Seobe", "Miloš Crnjanski", "978-86-0000-000-0"));

        assertThatThrownBy(() ->
                bookRepository.saveAndFlush(new Book("Druga knjiga", "Drugi autor", "978-86-0000-000-0")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sameTitleWithDifferentIsbnIsAllowed() {
        bookRepository.saveAndFlush(new Book("Seobe", "Miloš Crnjanski", "978-86-1111-111-1"));
        bookRepository.saveAndFlush(new Book("Seobe", "Miloš Crnjanski", "978-86-2222-222-2"));

        assertThat(bookRepository.findAll()).hasSize(2);
    }

}
