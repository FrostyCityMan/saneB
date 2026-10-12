package com.saneb.domain.consultation.dto;

import java.util.UUID;

public record ConsultationPartnerLookupResponse(UUID userId, String name, String loginId) { }
