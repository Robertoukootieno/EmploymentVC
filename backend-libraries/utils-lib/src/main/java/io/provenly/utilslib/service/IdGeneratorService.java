package io.provenly.utilslib.service;

import java.util.UUID;

/**
 * Shared ID generation helper.
 */
public class IdGeneratorService {

    public String newUuid() {
        return UUID.randomUUID().toString();
    }

    public String newUrnUuid() {
        return "urn:uuid:" + UUID.randomUUID();
    }
}
