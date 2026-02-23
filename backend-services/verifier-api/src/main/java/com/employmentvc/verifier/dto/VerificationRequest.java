package com.employmentvc.verifier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequest {
    
    @NotBlank(message = "Challenge is required")
    private String challenge;
    
    @NotNull(message = "Presentation is required")
    private Map<String, Object> presentation;
}
