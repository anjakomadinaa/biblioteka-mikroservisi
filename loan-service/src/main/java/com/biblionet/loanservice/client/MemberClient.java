package com.biblionet.loanservice.client;

import com.biblionet.loanservice.dto.MemberDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "member-service")
public interface MemberClient {

    @GetMapping("/members/{id}")
    MemberDto getById(@PathVariable("id") Long id);

}
