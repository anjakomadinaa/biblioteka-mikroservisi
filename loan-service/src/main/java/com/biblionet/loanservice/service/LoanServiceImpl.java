package com.biblionet.loanservice.service;

import com.biblionet.loanservice.client.BookClient;
import com.biblionet.loanservice.client.MemberClient;
import com.biblionet.loanservice.config.RabbitMQConfig;
import com.biblionet.loanservice.dto.AvailabilityRequestDto;
import com.biblionet.loanservice.dto.BookDto;
import com.biblionet.loanservice.dto.LoanEvent;
import com.biblionet.loanservice.dto.LoanRequestDto;
import com.biblionet.loanservice.dto.LoanResponseDto;
import com.biblionet.loanservice.entity.Loan;
import com.biblionet.loanservice.entity.LoanStatus;
import com.biblionet.loanservice.exception.BookNotAvailableException;
import com.biblionet.loanservice.exception.InvalidLoanStateException;
import com.biblionet.loanservice.exception.ResourceNotFoundException;
import com.biblionet.loanservice.exception.ServiceUnavailableException;
import com.biblionet.loanservice.repository.LoanRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Napomena o konzistentnosti: ovaj servis menja stanje u dva sistema (sopstvena baza i
 * book-service). Lokalna transakcija ne moze da pokrije udaljeni poziv, pa se upis u bazu
 * i udaljeni poziv izvrsavaju odvojeno, a neuspeh udaljenog poziva se rucno kompenzuje.
 * Objavljivanje eventa je best-effort - notifikacija nije razlog da pozajmica ne uspe.
 */
@Service
public class LoanServiceImpl implements LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanServiceImpl.class);

    private static final String EVENT_LOAN_CREATED = "LOAN_CREATED";
    private static final String EVENT_LOAN_RETURNED = "LOAN_RETURNED";
    private static final String ROUTING_KEY_CREATED = "loan.created";
    private static final String ROUTING_KEY_RETURNED = "loan.returned";

    private final LoanRepository loanRepository;
    private final MemberClient memberClient;
    private final BookClient bookClient;
    private final RabbitTemplate rabbitTemplate;

    public LoanServiceImpl(LoanRepository loanRepository, MemberClient memberClient, BookClient bookClient,
                           RabbitTemplate rabbitTemplate) {
        this.loanRepository = loanRepository;
        this.memberClient = memberClient;
        this.bookClient = bookClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public LoanResponseDto createLoan(LoanRequestDto request) {
        requireMember(request.getMemberId());
        BookDto book = requireBook(request.getBookId());

        if (!book.isAvailable()) {
            throw new BookNotAvailableException("Knjiga sa ID " + request.getBookId() + " trenutno nije dostupna");
        }
        if (loanRepository.existsByBookIdAndStatus(request.getBookId(), LoanStatus.ACTIVE)) {
            throw new BookNotAvailableException("Knjiga sa ID " + request.getBookId() + " je već pozajmljena");
        }

        Loan loan = loanRepository.save(new Loan(request.getMemberId(), request.getBookId()));

        try {
            bookClient.updateAvailability(loan.getBookId(), new AvailabilityRequestDto(false));
        } catch (RuntimeException ex) {
            loanRepository.delete(loan);
            throw remoteFailure("Pozajmica nije kreirana jer book-service nije dostupan", ex);
        }

        publishEvent(EVENT_LOAN_CREATED, ROUTING_KEY_CREATED, loan);
        return toResponse(loan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanResponseDto> getAllLoans() {
        return loanRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LoanResponseDto getLoanById(Long id) {
        return toResponse(findLoanOrThrow(id));
    }

    @Override
    public LoanResponseDto returnLoan(Long id) {
        Loan loan = findLoanOrThrow(id);

        if (loan.isReturned()) {
            throw new InvalidLoanStateException("Pozajmica sa ID " + id + " je već vraćena");
        }

        loan.markReturned();
        loanRepository.save(loan);

        try {
            bookClient.updateAvailability(loan.getBookId(), new AvailabilityRequestDto(true));
        } catch (RuntimeException ex) {
            loan.revertReturn();
            loanRepository.save(loan);
            throw remoteFailure("Vraćanje nije evidentirano jer book-service nije dostupan", ex);
        }

        publishEvent(EVENT_LOAN_RETURNED, ROUTING_KEY_RETURNED, loan);
        return toResponse(loan);
    }

    private void requireMember(Long memberId) {
        try {
            memberClient.getById(memberId);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Član sa ID " + memberId + " nije pronađen");
        } catch (FeignException ex) {
            throw new ServiceUnavailableException("member-service trenutno nije dostupan");
        }
    }

    private BookDto requireBook(Long bookId) {
        try {
            return bookClient.getById(bookId);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Knjiga sa ID " + bookId + " nije pronađena");
        } catch (FeignException ex) {
            throw new ServiceUnavailableException("book-service trenutno nije dostupan");
        }
    }

    private Loan findLoanOrThrow(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pozajmica sa ID " + id + " nije pronađena"));
    }

    private RuntimeException remoteFailure(String message, RuntimeException cause) {
        if (cause instanceof FeignException) {
            log.error("{}", message, cause);
            return new ServiceUnavailableException(message);
        }
        return cause;
    }

    /**
     * Event se objavljuje tek nakon sto su baza i book-service vec konzistentni, pa
     * neuspeh brokera ne sme da obori zahtev - gubi se samo notifikacija.
     */
    private void publishEvent(String eventType, String routingKey, Loan loan) {
        LoanEvent event = new LoanEvent(
                eventType,
                loan.getId(),
                loan.getMemberId(),
                loan.getBookId(),
                LocalDateTime.now()
        );
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.LOAN_EXCHANGE, routingKey, event);
        } catch (AmqpException ex) {
            log.error("Event {} za pozajmicu {} nije objavljen", eventType, loan.getId(), ex);
        }
    }

    private LoanResponseDto toResponse(Loan loan) {
        return new LoanResponseDto(
                loan.getId(),
                loan.getMemberId(),
                loan.getBookId(),
                loan.getLoanDate(),
                loan.getDueDate(),
                loan.getReturnDate(),
                loan.getStatus()
        );
    }

}
