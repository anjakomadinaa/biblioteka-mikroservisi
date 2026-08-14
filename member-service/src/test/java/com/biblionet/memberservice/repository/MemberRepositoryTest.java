package com.biblionet.memberservice.repository;

import com.biblionet.memberservice.entity.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testira ponasanje entiteta na pravoj bazi - @PrePersist i unique constraint
 * se ne mogu proveriti mokovanim repozitorijumom.
 */
@DataJpaTest
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void membershipDateIsAssignedAutomaticallyOnSave() {
        Member member = new Member("Ana", "Anić", "ana@example.com");
        assertThat(member.getMembershipDate()).isNull();

        Member saved = memberRepository.saveAndFlush(member);

        assertThat(saved.getMembershipDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void explicitMembershipDateIsNotOverwritten() {
        Member member = new Member("Ana", "Anić", "ana@example.com");
        member.setMembershipDate(LocalDate.of(2020, 1, 15));

        Member saved = memberRepository.saveAndFlush(member);

        assertThat(saved.getMembershipDate()).isEqualTo(LocalDate.of(2020, 1, 15));
    }

    @Test
    void idIsGeneratedOnSave() {
        Member saved = memberRepository.saveAndFlush(new Member("Ana", "Anić", "ana@example.com"));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void duplicateEmailIsRejectedByUniqueConstraint() {
        memberRepository.saveAndFlush(new Member("Ana", "Anić", "ista@example.com"));

        assertThatThrownBy(() ->
                memberRepository.saveAndFlush(new Member("Marko", "Marić", "ista@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void differentEmailsAreAllowed() {
        memberRepository.saveAndFlush(new Member("Ana", "Anić", "ana@example.com"));
        memberRepository.saveAndFlush(new Member("Marko", "Marić", "marko@example.com"));

        assertThat(memberRepository.findAll()).hasSize(2);
    }

}
