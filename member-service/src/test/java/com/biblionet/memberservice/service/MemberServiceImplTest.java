package com.biblionet.memberservice.service;

import com.biblionet.memberservice.dto.MemberRequestDto;
import com.biblionet.memberservice.dto.MemberResponseDto;
import com.biblionet.memberservice.entity.Member;
import com.biblionet.memberservice.exception.ResourceNotFoundException;
import com.biblionet.memberservice.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Unit testovi servisnog sloja - repozitorijum je mokovan, proverava se
 * mapiranje DTO <-> entitet i ponasanje kada clan ne postoji.
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    private MemberServiceImpl memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberServiceImpl(memberRepository);
    }

    @Test
    void createMemberMapsRequestToEntity() {
        given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));

        memberService.createMember(new MemberRequestDto("Ana", "Anić", "ana@example.com"));

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        Member saved = captor.getValue();
        assertThat(saved.getFirstName()).isEqualTo("Ana");
        assertThat(saved.getLastName()).isEqualTo("Anić");
        assertThat(saved.getEmail()).isEqualTo("ana@example.com");
    }

    @Test
    void createMemberReturnsResponseWithAllFields() {
        Member persisted = memberWithId(1L, "Ana", "Anić", "ana@example.com");
        given(memberRepository.save(any(Member.class))).willReturn(persisted);

        MemberResponseDto response = memberService.createMember(
                new MemberRequestDto("Ana", "Anić", "ana@example.com"));

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFirstName()).isEqualTo("Ana");
        assertThat(response.getLastName()).isEqualTo("Anić");
        assertThat(response.getEmail()).isEqualTo("ana@example.com");
        assertThat(response.getMembershipDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void getAllMembersMapsEveryEntity() {
        given(memberRepository.findAll()).willReturn(List.of(
                memberWithId(1L, "Ana", "Anić", "ana@example.com"),
                memberWithId(2L, "Marko", "Marić", "marko@example.com")));

        List<MemberResponseDto> result = memberService.getAllMembers();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(MemberResponseDto::getEmail)
                .containsExactly("ana@example.com", "marko@example.com");
    }

    @Test
    void getAllMembersReturnsEmptyListWhenNoMembers() {
        given(memberRepository.findAll()).willReturn(List.of());

        assertThat(memberService.getAllMembers()).isEmpty();
    }

    @Test
    void getMemberByIdReturnsMappedMember() {
        given(memberRepository.findById(1L))
                .willReturn(Optional.of(memberWithId(1L, "Ana", "Anić", "ana@example.com")));

        assertThat(memberService.getMemberById(1L).getFirstName()).isEqualTo("Ana");
    }

    @Test
    void getMemberByIdThrowsWhenMemberDoesNotExist() {
        given(memberRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMemberById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Član sa ID 99 nije pronađen");
    }

    private Member memberWithId(Long id, String firstName, String lastName, String email) {
        Member member = new Member(firstName, lastName, email);
        member.setMembershipDate(LocalDate.now());
        try {
            var idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(member, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return member;
    }

}
