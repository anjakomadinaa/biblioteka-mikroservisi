package com.biblionet.loanservice.client;

import com.biblionet.loanservice.dto.AvailabilityRequestDto;
import com.biblionet.loanservice.dto.BookDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "book-service")
public interface BookClient {

    @GetMapping("/books/{id}")
    BookDto getById(@PathVariable("id") Long id);

    @PatchMapping("/books/{id}/availability")
    BookDto updateAvailability(@PathVariable("id") Long id, @RequestBody AvailabilityRequestDto request);

}
