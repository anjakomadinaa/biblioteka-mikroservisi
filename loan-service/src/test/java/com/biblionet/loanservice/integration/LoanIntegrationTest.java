package com.biblionet.loanservice.integration;

import com.biblionet.loanservice.client.BookClient;
import com.biblionet.loanservice.client.MemberClient;
import com.biblionet.loanservice.dto.AvailabilityRequestDto;
import com.biblionet.loanservice.dto.BookDto;
import com.biblionet.loanservice.dto.LoanEvent;
import com.biblionet.loanservice.dto.LoanRequestDto;
import com.biblionet.loanservice.dto.MemberDto;
import com.biblionet.loanservice.entity.Loan;
import com.biblionet.loanservice.entity.LoanStatus;
import com.biblionet.loanservice.repository.LoanRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LoanIntegrationTest {

    private static final String LOAN_EXCHANGE = "loan.events.exchange";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoanRepository loanRepository;

    @MockitoBean
    private MemberClient memberClient;

    @MockitoBean
    private BookClient bookClient;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void resetState() {
        loanRepository.deleteAll();
    }

    @Test
    void createLoanPersistsLoanMarksBookUnavailableAndPublishesEvent() throws Exception {
        given(memberClient.getById(5L)).willReturn(new MemberDto(5L, "Ana", "Anić", "ana@example.com"));
        given(bookClient.getById(7L)).willReturn(new BookDto(7L, "Seobe", "Miloš Crnjanski", "978-86-7654-321-0", true));

        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanRequestDto(5L, 7L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.memberId").value(5))
                .andExpect(jsonPath("$.bookId").value(7))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.returnDate").doesNotExist());

        List<Loan> loans = loanRepository.findAll();
        assertThat(loans).hasSize(1);
        Loan saved = loans.get(0);
        assertThat(saved.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(saved.getLoanDate()).isEqualTo(LocalDate.now());
        assertThat(saved.getDueDate()).isEqualTo(LocalDate.now().plusDays(14));
        assertThat(saved.getReturnDate()).isNull();

        var availabilityCaptor = forClass(AvailabilityRequestDto.class);
        verify(bookClient).updateAvailability(eq(7L), availabilityCaptor.capture());
        assertThat(availabilityCaptor.getValue().getAvailable()).isFalse();

        var eventCaptor = forClass(LoanEvent.class);
        verify(rabbitTemplate).convertAndSend(eq(LOAN_EXCHANGE), eq("loan.created"), eventCaptor.capture());
        LoanEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo("LOAN_CREATED");
        assertThat(event.getLoanId()).isEqualTo(saved.getId());
        assertThat(event.getMemberId()).isEqualTo(5L);
        assertThat(event.getBookId()).isEqualTo(7L);
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void createLoanReturns409AndChangesNothingWhenBookNotAvailable() throws Exception {
        given(memberClient.getById(5L)).willReturn(new MemberDto(5L, "Ana", "Anić", "ana@example.com"));
        given(bookClient.getById(7L)).willReturn(new BookDto(7L, "Seobe", "Miloš Crnjanski", "978-86-7654-321-0", false));

        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanRequestDto(5L, 7L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Knjiga sa ID 7 trenutno nije dostupna"));

        assertThat(loanRepository.findAll()).isEmpty();
        verify(bookClient, never()).updateAvailability(anyLong(), any());
        verifyNoEventPublished();
    }

    @Test
    void createLoanReturns404WhenMemberDoesNotExist() throws Exception {
        given(memberClient.getById(99L)).willThrow(notFound("/members/99"));

        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanRequestDto(99L, 7L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Član sa ID 99 nije pronađen"));

        assertThat(loanRepository.findAll()).isEmpty();
        verifyNoInteractions(bookClient);
        verifyNoEventPublished();
    }

    @Test
    void createLoanReturns404WhenBookDoesNotExist() throws Exception {
        given(memberClient.getById(5L)).willReturn(new MemberDto(5L, "Ana", "Anić", "ana@example.com"));
        given(bookClient.getById(99L)).willThrow(notFound("/books/99"));

        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanRequestDto(5L, 99L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Knjiga sa ID 99 nije pronađena"));

        assertThat(loanRepository.findAll()).isEmpty();
        verifyNoEventPublished();
    }

    @Test
    void returnLoanMarksBookAvailableAndPublishesReturnedEvent() throws Exception {
        given(memberClient.getById(5L)).willReturn(new MemberDto(5L, "Ana", "Anić", "ana@example.com"));
        given(bookClient.getById(7L)).willReturn(new BookDto(7L, "Seobe", "Miloš Crnjanski", "978-86-7654-321-0", true));

        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanRequestDto(5L, 7L))))
                .andExpect(status().isCreated());

        Long loanId = loanRepository.findAll().get(0).getId();

        mockMvc.perform(put("/loans/{id}/return", loanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnDate").value(LocalDate.now().toString()));

        Loan returned = loanRepository.findById(loanId).orElseThrow();
        assertThat(returned.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(returned.getReturnDate()).isEqualTo(LocalDate.now());

        var availabilityCaptor = forClass(AvailabilityRequestDto.class);
        verify(bookClient, org.mockito.Mockito.times(2)).updateAvailability(eq(7L), availabilityCaptor.capture());
        assertThat(availabilityCaptor.getAllValues().get(1).getAvailable()).isTrue();

        var eventCaptor = forClass(LoanEvent.class);
        verify(rabbitTemplate).convertAndSend(eq(LOAN_EXCHANGE), eq("loan.returned"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("LOAN_RETURNED");
        assertThat(eventCaptor.getValue().getLoanId()).isEqualTo(loanId);
    }

    @Test
    void returnLoanTwiceReturns409AndDoesNotFreeTheBookAgain() throws Exception {
        Long loanId = createActiveLoan();

        mockMvc.perform(put("/loans/{id}/return", loanId))
                .andExpect(status().isOk());

        mockMvc.perform(put("/loans/{id}/return", loanId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Pozajmica sa ID " + loanId + " je već vraćena"));

        // dva poziva ukupno: available=false pri kreiranju, available=true pri prvom vracanju
        verify(bookClient, org.mockito.Mockito.times(2)).updateAvailability(eq(7L), any());
        verify(rabbitTemplate, org.mockito.Mockito.times(1))
                .convertAndSend(eq(LOAN_EXCHANGE), eq("loan.returned"), any(Object.class));
    }

    @Test
    void createLoanReturns409WhenBookAlreadyHasActiveLoan() throws Exception {
        createActiveLoan();

        // book-service i dalje tvrdi da je knjiga slobodna (zastareo flag),
        // ali lokalna evidencija zna da je pozajmljena
        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanRequestDto(5L, 7L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Knjiga sa ID 7 je već pozajmljena"));

        assertThat(loanRepository.findAll()).hasSize(1);
    }

    @Test
    void createLoanIsCompensatedWhenBookServiceFails() throws Exception {
        given(memberClient.getById(5L)).willReturn(new MemberDto(5L, "Ana", "Anić", "ana@example.com"));
        given(bookClient.getById(7L)).willReturn(new BookDto(7L, "Seobe", "Miloš Crnjanski", "978-86-7654-321-0", true));
        given(bookClient.updateAvailability(eq(7L), any())).willThrow(unavailable("/books/7/availability"));

        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanRequestDto(5L, 7L))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));

        // pozajmica ne sme da ostane u bazi ako knjiga nije rezervisana u book-service-u
        assertThat(loanRepository.findAll()).isEmpty();
        verifyNoEventPublished();
    }

    @Test
    void returnLoanIsCompensatedWhenBookServiceFails() throws Exception {
        Long loanId = createActiveLoan();
        given(bookClient.updateAvailability(eq(7L), any())).willThrow(unavailable("/books/7/availability"));

        mockMvc.perform(put("/loans/{id}/return", loanId))
                .andExpect(status().isServiceUnavailable());

        // pozajmica mora ostati aktivna - knjiga nije oslobodjena u book-service-u
        Loan afterFailure = loanRepository.findById(loanId).orElseThrow();
        assertThat(afterFailure.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(afterFailure.getReturnDate()).isNull();

        verify(rabbitTemplate, never())
                .convertAndSend(eq(LOAN_EXCHANGE), eq("loan.returned"), any(Object.class));
    }

    @Test
    void loanSucceedsEvenIfBrokerIsDown() throws Exception {
        given(memberClient.getById(5L)).willReturn(new MemberDto(5L, "Ana", "Anić", "ana@example.com"));
        given(bookClient.getById(7L)).willReturn(new BookDto(7L, "Seobe", "Miloš Crnjanski", "978-86-7654-321-0", true));
        org.mockito.BDDMockito.willThrow(new org.springframework.amqp.AmqpException("broker nedostupan"))
                .given(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // notifikacija je best-effort: pozajmica je validna i bez objavljenog eventa
        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanRequestDto(5L, 7L))))
                .andExpect(status().isCreated());

        assertThat(loanRepository.findAll()).hasSize(1);
    }

    private Long createActiveLoan() throws Exception {
        given(memberClient.getById(5L)).willReturn(new MemberDto(5L, "Ana", "Anić", "ana@example.com"));
        given(bookClient.getById(7L)).willReturn(new BookDto(7L, "Seobe", "Miloš Crnjanski", "978-86-7654-321-0", true));

        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanRequestDto(5L, 7L))))
                .andExpect(status().isCreated());

        return loanRepository.findAll().get(0).getId();
    }

    private FeignException unavailable(String url) {
        Request request = Request.create(Request.HttpMethod.PATCH, url, Map.<String, Collection<String>>of(),
                null, StandardCharsets.UTF_8, new RequestTemplate());
        return new FeignException.ServiceUnavailable("Service Unavailable", request, null, Map.of());
    }

    /**
     * Spring sam pozove metodu na mokovanom RabbitTemplate-u pri podizanju konteksta,
     * pa verifyNoInteractions ovde nije upotrebljiv - proveravamo da nije poslat nijedan event.
     */
    private void verifyNoEventPublished() {
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    private FeignException.NotFound notFound(String url) {
        Request request = Request.create(Request.HttpMethod.GET, url, Map.<String, Collection<String>>of(),
                null, StandardCharsets.UTF_8, new RequestTemplate());
        return new FeignException.NotFound("Not Found", request, null, Map.of());
    }

}
