package com.biblionet.memberservice.dto;

import java.time.LocalDate;

public class MemberResponseDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate membershipDate;

    public MemberResponseDto() {
    }

    public MemberResponseDto(Long id, String firstName, String lastName, String email, LocalDate membershipDate) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.membershipDate = membershipDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getMembershipDate() {
        return membershipDate;
    }

    public void setMembershipDate(LocalDate membershipDate) {
        this.membershipDate = membershipDate;
    }

}
