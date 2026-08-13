package com.biblionet.bookservice.dto;

import jakarta.validation.constraints.NotNull;

public class AvailabilityRequestDto {

    @NotNull(message = "Polje available je obavezno")
    private Boolean available;

    public AvailabilityRequestDto() {
    }

    public AvailabilityRequestDto(Boolean available) {
        this.available = available;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

}
