package com.saneb.domain.member.service;

import com.saneb.domain.member.dto.MemberBasicInfoResponse;
import com.saneb.domain.member.dto.MemberBasicInfoSaveRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface MemberBasicInfoService {

    MemberBasicInfoResponse selectMyBasicInfo(Authentication authentication);

    MemberBasicInfoResponse saveMyBasicInfo(Authentication authentication, MemberBasicInfoSaveRequest request);

    MemberBasicInfoResponse selectMemberBasicInfo(Authentication authentication, UUID userId);

    MemberBasicInfoResponse saveMemberBasicInfo(
            Authentication authentication,
            UUID userId,
            MemberBasicInfoSaveRequest request
    );
}
