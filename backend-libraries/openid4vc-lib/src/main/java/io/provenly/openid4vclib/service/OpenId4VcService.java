package io.provenly.openid4vclib.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import io.provenly.openid4vclib.model.CredentialOffer;
import io.provenly.protocolslib.model.ProtocolMessage;
import io.provenly.protocolslib.service.ProtocolMessageService;

import java.util.List;
import java.util.Map;

/**
 * Core OpenID4VC helper service for offer and protocol envelope generation.
 */
public class OpenId4VcService {

    private static final String OPENID4VCI_OFFER_TYPE = "openid4vci.credential_offer";

    private final ObjectMapper objectMapper;
    private final ProtocolMessageService protocolMessageService;

    public OpenId4VcService() {
        this(new ObjectMapper(), new ProtocolMessageService());
    }

    public OpenId4VcService(ObjectMapper objectMapper, ProtocolMessageService protocolMessageService) {
        this.objectMapper = objectMapper;
        this.protocolMessageService = protocolMessageService;
    }

    public CredentialOffer createOffer(String issuer, List<String> credentialConfigurationIds, String grantsType) {
        CredentialOffer offer = new CredentialOffer();
        offer.setCredentialIssuer(issuer);
        offer.setCredentialConfigurationIds(credentialConfigurationIds);
        offer.setGrantsType(grantsType);
        return offer;
    }

    public String offerToJson(CredentialOffer offer) {
        try {
            return objectMapper.writeValueAsString(offer);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to serialize credential offer", exception);
        }
    }

    public ProtocolMessage wrapOfferAsProtocolMessage(String from, String to, CredentialOffer offer) {
        Map<String, Object> body = objectMapper.convertValue(offer, new TypeReference<Map<String, Object>>() {});
        return protocolMessageService.createMessage(OPENID4VCI_OFFER_TYPE, from, to, body);
    }
}
