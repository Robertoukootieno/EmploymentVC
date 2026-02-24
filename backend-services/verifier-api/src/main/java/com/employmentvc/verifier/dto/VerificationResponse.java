package com.employmentvc.verifier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponse {
    private String verificationId;
    private boolean verified;
    private long timestamp;
    private String message;
    private Map<String, Object> checks;
    private Map<String, Object> presentation;
}
