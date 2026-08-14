package com.biblionet.memberservice.integration;

import com.biblionet.memberservice.dto.MemberRequestDto;
import com.biblionet.memberservice.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integracioni test - ceo kontekst, prava baza, bez mokova.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MemberIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void resetState() {
        memberRepository.deleteAll();
    }

    @Test
    void createdMemberIsPersistedAndRetrievableById() throws Exception {
        String response = mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MemberRequestDto("Ana", "Anić", "ana@example.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.membershipDate").value(LocalDate.now().toString()))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        assertThat(memberRepository.findById(id)).isPresent();

        mockMvc.perform(get("/members/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@example.com"));
    }

    @Test
    void allCreatedMembersAppearInList() throws Exception {
        createMember("Ana", "Anić", "ana@example.com");
        createMember("Marko", "Marić", "marko@example.com");

        mockMvc.perform(get("/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        createMember("Ana", "Anić", "ista@example.com");

        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MemberRequestDto("Marko", "Marić", "ista@example.com"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Član sa datim email-om već postoji"));

        assertThat(memberRepository.findAll()).hasSize(1);
    }

    @Test
    void invalidEmailIsRejectedAndNothingIsPersisted() throws Exception {
        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MemberRequestDto("Ana", "Anić", "nije-email"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        assertThat(memberRepository.findAll()).isEmpty();
    }

    @Test
    void unknownMemberReturns404() throws Exception {
        mockMvc.perform(get("/members/{id}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private void createMember(String first, String last, String email) throws Exception {
        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MemberRequestDto(first, last, email))))
                .andExpect(status().isCreated());
    }

}
