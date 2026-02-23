package io.provenly.walletcore.service;

import io.provenly.credentialslib.model.CredentialRecord;
import io.provenly.credentialslib.service.CredentialStoreService;
import io.provenly.didlib.model.DidRegistration;
import io.provenly.didlib.service.DidRegistryService;
import io.provenly.vccore.model.VerifiableCredential;
import io.provenly.walletcore.model.WalletProfile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core wallet orchestration service for DID and credential ownership.
 */
public class WalletService {

    private final DidRegistryService didRegistryService;
    private final CredentialStoreService credentialStoreService;
    private final Map<String, WalletProfile> wallets = new ConcurrentHashMap<>();

    public WalletService() {
        this(new DidRegistryService(), new CredentialStoreService());
    }

    public WalletService(DidRegistryService didRegistryService, CredentialStoreService credentialStoreService) {
        this.didRegistryService = didRegistryService;
        this.credentialStoreService = credentialStoreService;
    }

    public WalletProfile createWallet(String ownerId, String did) {
        WalletProfile profile = new WalletProfile();
        profile.setWalletId(UUID.randomUUID().toString());
        profile.setOwnerId(ownerId);
        profile.setDid(did);
        profile.setCreatedAt(Instant.now());

        wallets.put(profile.getWalletId(), profile);
        return profile;
    }

    public WalletProfile getWallet(String walletId) {
        return wallets.get(walletId);
    }

    public List<WalletProfile> getWalletsByOwner(String ownerId) {
        return wallets.values().stream()
                .filter(profile -> ownerId.equals(profile.getOwnerId()))
                .toList();
    }

    public DidRegistration registerDidKeyForWallet(String walletId, String publicKeyMultibase) {
        WalletProfile wallet = requireWallet(walletId);
        return didRegistryService.registerDidKey(wallet.getDid(), wallet.getOwnerId(), publicKeyMultibase);
    }

    public DidRegistration registerDidWebForWallet(String walletId, Map<String, Object> publicKeyJwk) {
        WalletProfile wallet = requireWallet(walletId);
        return didRegistryService.registerDidWeb(wallet.getDid(), wallet.getOwnerId(), publicKeyJwk);
    }

    public CredentialRecord storeCredential(String walletId, VerifiableCredential credential) {
        WalletProfile wallet = requireWallet(walletId);
        return credentialStoreService.save(wallet.getDid(), credential);
    }

    public List<CredentialRecord> getCredentials(String walletId) {
        WalletProfile wallet = requireWallet(walletId);
        return credentialStoreService.findByHolderDid(wallet.getDid());
    }

    private WalletProfile requireWallet(String walletId) {
        WalletProfile profile = wallets.get(walletId);
        if (profile == null) {
            throw new IllegalArgumentException("Wallet not found: " + walletId);
        }
        return profile;
    }
}
