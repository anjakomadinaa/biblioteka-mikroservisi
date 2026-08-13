package com.biblionet.memberservice.controller;

import com.biblionet.memberservice.dto.MemberRequestDto;
import com.biblionet.memberservice.dto.MemberResponseDto;
import com.biblionet.memberservice.exception.ResourceNotFoundException;
import com.biblionet.memberservice.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    @Test
    void createMemberReturns201WithCreatedMember() throws Exception {
        MemberRequestDto request = new MemberRequestDto("Ana", "Anić", "ana@example.com");
        MemberResponseDto response = new MemberResponseDto(1L, "Ana", "Anić", "ana@example.com", LocalDate.of(2026, 8, 13));
        given(memberService.createMember(any(MemberRequestDto.class))).willReturn(response);

        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.membershipDate").value("2026-08-13"));
    }

    @Test
    void createMemberWithInvalidPayloadReturns400WithFieldErrors() throws Exception {
        MemberRequestDto request = new MemberRequestDto("", "Anić", "nije-email");

        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/members"))
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.email").exists());

        verifyNoInteractions(memberService);
    }

    @Test
    void createMemberWithDuplicateEmailReturns409() throws Exception {
        given(memberService.createMember(any(MemberRequestDto.class)))
                .willThrow(new org.springframework.dao.DataIntegrityViolationException("unique constraint"));

        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MemberRequestDto("Ana", "Anić", "ana@example.com"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Član sa datim email-om već postoji"))
                .andExpect(jsonPath("$.path").value("/members"));
    }

    @Test
    void getAllMembersReturnsList() throws Exception {
        given(memberService.getAllMembers()).willReturn(List.of(
                new MemberResponseDto(1L, "Ana", "Anić", "ana@example.com", LocalDate.of(2026, 8, 13)),
                new MemberResponseDto(2L, "Marko", "Marković", "marko@example.com", LocalDate.of(2026, 8, 12))
        ));

        mockMvc.perform(get("/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].lastName").value("Marković"));
    }

    @Test
    void getMemberByIdReturnsMember() throws Exception {
        given(memberService.getMemberById(1L)).willReturn(
                new MemberResponseDto(1L, "Ana", "Anić", "ana@example.com", LocalDate.of(2026, 8, 13)));

        mockMvc.perform(get("/members/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Ana"));
    }

    @Test
    void getMemberByIdReturns404WhenMissing() throws Exception {
        given(memberService.getMemberById(99L))
                .willThrow(new ResourceNotFoundException("Član sa ID 99 nije pronađen"));

        mockMvc.perform(get("/members/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Član sa ID 99 nije pronađen"))
                .andExpect(jsonPath("$.path").value("/members/99"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

}
