package com.biblionet.memberservice.service;

import com.biblionet.memberservice.dto.MemberRequestDto;
import com.biblionet.memberservice.dto.MemberResponseDto;

import java.util.List;

public interface MemberService {

    MemberResponseDto createMember(MemberRequestDto request);

    List<MemberResponseDto> getAllMembers();

    MemberResponseDto getMemberById(Long id);

}
