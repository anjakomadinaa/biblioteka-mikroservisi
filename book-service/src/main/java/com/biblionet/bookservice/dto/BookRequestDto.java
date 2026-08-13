package com.biblionet.bookservice.dto;

import jakarta.validation.constraints.NotBlank;

public class BookRequestDto {

    @NotBlank(message = "Naslov je obavezan")
    private String title;

    @NotBlank(message = "Autor je obavezan")
    private String author;

    @NotBlank(message = "ISBN je obavezan")
    private String isbn;

    public BookRequestDto() {
    }

    public BookRequestDto(String title, String author, String isbn) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

}
