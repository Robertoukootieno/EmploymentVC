package com.employmentvc.verifier.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchVerificationRequest {
    
    @NotEmpty(message = "Presentations list cannot be empty")
    @Valid
    private List<VerificationRequest> presentations;
}
