package io.provenly.openid4vclib.model;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenID4VCI credential offer representation.
 */
public class CredentialOffer {

    private String credentialIssuer;
    private List<String> credentialConfigurationIds = new ArrayList<>();
    private String grantsType;

    public String getCredentialIssuer() {
        return credentialIssuer;
    }

    public void setCredentialIssuer(String credentialIssuer) {
        this.credentialIssuer = credentialIssuer;
    }

    public List<String> getCredentialConfigurationIds() {
        return credentialConfigurationIds;
    }

    public void setCredentialConfigurationIds(List<String> credentialConfigurationIds) {
        this.credentialConfigurationIds = credentialConfigurationIds;
    }

    public String getGrantsType() {
        return grantsType;
    }

    public void setGrantsType(String grantsType) {
        this.grantsType = grantsType;
    }
}
