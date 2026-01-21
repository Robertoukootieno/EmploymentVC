package io.provenly.auth.service;

import io.provenly.auth.dto.Web3ChallengeResponse;
import io.provenly.commons.exception.ProvenlyException;
import io.provenly.crypto.util.HashUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for Web3 wallet authentication using SIWE (Sign-In with Ethereum) protocol.
 */
@Service
@Slf4j
public class Web3AuthService {

    private static final String CHALLENGE_PREFIX = "web3:challenge:";
    private static final int CHALLENGE_EXPIRATION_MINUTES = 5;
    private static final String MESSAGE_TEMPLATE = "Sign this message to authenticate with Provenly:\n\nNonce: %s\nIssued At: %s";

    private final RedisTemplate<String, String> redisTemplate;

    public Web3AuthService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generate authentication challenge for a wallet address.
     */
    public Web3ChallengeResponse generateChallenge(String walletAddress) {
        // Validate wallet address format
        if (!isValidEthereumAddress(walletAddress)) {
            throw new ProvenlyException.ValidationException("Invalid Ethereum address format");
        }

        // Generate nonce
        String nonce = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(CHALLENGE_EXPIRATION_MINUTES, ChronoUnit.MINUTES);

        // Create challenge message
        String challenge = String.format(MESSAGE_TEMPLATE, nonce, now.toString());

        // Store nonce in Redis with expiration
        String redisKey = CHALLENGE_PREFIX + walletAddress.toLowerCase();
        redisTemplate.opsForValue().set(redisKey, nonce, CHALLENGE_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        log.info("Generated Web3 challenge for wallet: {}", walletAddress);

        return Web3ChallengeResponse.builder()
                .challenge(challenge)
                .nonce(nonce)
                .expiresAt(expiresAt.toString())
                .build();
    }

    /**
     * Verify wallet signature and authenticate.
     */
    public boolean verifySignature(String walletAddress, String signature, String message) {
        try {
            // Validate inputs
            if (!isValidEthereumAddress(walletAddress)) {
                throw new ProvenlyException.ValidationException("Invalid Ethereum address format");
            }

            // Verify nonce from Redis
            String redisKey = CHALLENGE_PREFIX + walletAddress.toLowerCase();
            String storedNonce = redisTemplate.opsForValue().get(redisKey);
            
            if (storedNonce == null) {
                log.warn("No challenge found for wallet: {}", walletAddress);
                throw new ProvenlyException.AuthenticationException("Challenge not found or expired");
            }

            // Verify nonce is in the message
            if (!message.contains(storedNonce)) {
                log.warn("Nonce mismatch for wallet: {}", walletAddress);
                throw new ProvenlyException.AuthenticationException("Invalid challenge nonce");
            }

            // Recover address from signature
            String recoveredAddress = recoverAddressFromSignature(message, signature);

            // Compare addresses (case-insensitive)
            boolean isValid = walletAddress.equalsIgnoreCase(recoveredAddress);

            if (isValid) {
                // Delete used nonce
                redisTemplate.delete(redisKey);
                log.info("Successfully verified signature for wallet: {}", walletAddress);
            } else {
                log.warn("Signature verification failed for wallet: {}. Recovered: {}", walletAddress, recoveredAddress);
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error verifying Web3 signature", e);
            throw new ProvenlyException.AuthenticationException("Signature verification failed: " + e.getMessage());
        }
    }

    /**
     * Recover Ethereum address from signature.
     */
    private String recoverAddressFromSignature(String message, String signature) {
        try {
            // Add Ethereum message prefix
            String prefixedMessage = "\u0019Ethereum Signed Message:\n" + message.length() + message;
            byte[] messageHash = HashUtils.sha256(prefixedMessage.getBytes(StandardCharsets.UTF_8));

            // Parse signature
            byte[] signatureBytes = Numeric.hexStringToByteArray(signature);
            byte v = signatureBytes[64];
            if (v < 27) {
                v += 27;
            }

            byte[] r = Arrays.copyOfRange(signatureBytes, 0, 32);
            byte[] s = Arrays.copyOfRange(signatureBytes, 32, 64);

            Sign.SignatureData signatureData = new Sign.SignatureData(v, r, s);

            // Recover public key
            BigInteger publicKey = Sign.signedMessageHashToKey(messageHash, signatureData);

            // Derive address from public key
            return "0x" + Keys.getAddress(publicKey);

        } catch (Exception e) {
            log.error("Error recovering address from signature", e);
            throw new ProvenlyException.CryptographicException("Failed to recover address from signature");
        }
    }

    /**
     * Validate Ethereum address format.
     */
    private boolean isValidEthereumAddress(String address) {
        return address != null && address.matches("^0x[a-fA-F0-9]{40}$");
    }
}

