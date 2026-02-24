package com.employmentvc.verifier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchVerificationResponse {
    private String batchId;
    private int totalCount;
    private int successCount;
    private int failureCount;
    private List<VerificationResponse> results;
}
