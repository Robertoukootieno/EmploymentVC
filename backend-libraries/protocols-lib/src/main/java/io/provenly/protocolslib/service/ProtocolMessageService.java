package io.provenly.protocolslib.service;

import io.provenly.protocolslib.model.ProtocolMessage;
import io.provenly.utilslib.service.IdGeneratorService;

import java.util.Map;

/**
 * Helper to build standardized protocol envelopes.
 */
public class ProtocolMessageService {

    private final IdGeneratorService idGeneratorService;

    public ProtocolMessageService() {
        this(new IdGeneratorService());
    }

    public ProtocolMessageService(IdGeneratorService idGeneratorService) {
        this.idGeneratorService = idGeneratorService;
    }

    public ProtocolMessage createMessage(String type, String from, String to, Map<String, Object> body) {
        ProtocolMessage message = new ProtocolMessage();
        message.setId(idGeneratorService.newUuid());
        message.setType(type);
        message.setFrom(from);
        message.setTo(to);
        message.setBody(body);
        return message;
    }
}
