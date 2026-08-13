package com.biblionet.memberservice.service;

import com.biblionet.memberservice.dto.MemberRequestDto;
import com.biblionet.memberservice.dto.MemberResponseDto;
import com.biblionet.memberservice.entity.Member;
import com.biblionet.memberservice.exception.ResourceNotFoundException;
import com.biblionet.memberservice.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    public MemberServiceImpl(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional
    public MemberResponseDto createMember(MemberRequestDto request) {
        Member member = new Member(request.getFirstName(), request.getLastName(), request.getEmail());
        return toResponse(memberRepository.save(member));
    }

    @Override
    public List<MemberResponseDto> getAllMembers() {
        return memberRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public MemberResponseDto getMemberById(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Član sa ID " + id + " nije pronađen"));
        return toResponse(member);
    }

    private MemberResponseDto toResponse(Member member) {
        return new MemberResponseDto(
                member.getId(),
                member.getFirstName(),
                member.getLastName(),
                member.getEmail(),
                member.getMembershipDate()
        );
    }

}
