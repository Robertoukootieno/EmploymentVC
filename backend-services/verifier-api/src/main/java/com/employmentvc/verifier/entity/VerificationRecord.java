package com.employmentvc.verifier.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "verification_records")
@Data
@NoArgsConstructor
public class VerificationRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "verification_id", unique = true, nullable = false)
    private String verificationId;
    
    @Column(name = "verified", nullable = false)
    private boolean verified;
    
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
    
    @Column(name = "message")
    private String message;
    
    @Column(name = "presentation", columnDefinition = "TEXT")
    private String presentation;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
