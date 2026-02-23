package com.employmentvc.verifier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeResponse {
    private String challenge;
    private long expiresAt;
    private String verifierDid;
}
