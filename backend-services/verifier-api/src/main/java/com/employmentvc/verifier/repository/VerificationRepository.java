package com.employmentvc.verifier.repository;

import com.employmentvc.verifier.entity.VerificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationRepository extends JpaRepository<VerificationRecord, Long> {
    
    Optional<VerificationRecord> findByVerificationId(String verificationId);
}
