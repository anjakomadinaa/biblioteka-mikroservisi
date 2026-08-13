package com.biblionet.loanservice.dto;

public class AvailabilityRequestDto {

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
