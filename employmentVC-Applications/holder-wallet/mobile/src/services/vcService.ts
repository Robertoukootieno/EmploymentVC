/**
 * Verifiable Credential (VC) Service
 * 
 * Handles creation, verification, and management of W3C Verifiable Credentials
 */

import {
  VerifiableCredential,
  CredentialPresentation,
  CredentialSubject,
  CredentialProof,
  PresentationProof,
  SelectiveDisclosure,
} from '../types/credential';
import {keyService} from './keyService';
import {didService} from './didService';
import {apiClient} from './apiClient';

export interface VCCreationOptions {
  issuerDID: string;
  subjectDID: string;
  credentialType: string[];
  credentialSubject: CredentialSubject;
  expirationDate?: string;
  credentialStatus?: {
    id: string;
    type: string;
  };
  evidence?: Array<{
    type: string[];
    verifier?: string;
    evidenceDocument?: string;
  }>;
  termsOfUse?: Array<{
    type: string;
    prohibition?: string[];
  }>;
}

export interface VPCreationOptions {
  holderDID: string;
  credentials: VerifiableCredential[];
  challenge: string;
  domain?: string;
  selectiveDisclosure?: SelectiveDisclosure[];
  expirationDate?: string;
}

export interface VCVerificationOptions {
  checkRevocation?: boolean;
  checkExpiration?: boolean;
  trustedIssuers?: string[];
  requiredProofPurpose?: string;
}

export interface VCVerificationResult {
  valid: boolean;
  checks: {
    signatureValid: boolean;
    issuerValid: boolean;
    notExpired: boolean;
    notRevoked: boolean;
    schemaValid: boolean;
  };
  errors: string[];
  warnings: string[];
  verifiedAt: string;
}

class VCService {
  private readonly VC_CONTEXT = [
    'https://www.w3.org/2018/credentials/v1',
    'https://www.w3.org/2018/credentials/examples/v1',
  ];

  private readonly VP_CONTEXT = [
    'https://www.w3.org/2018/credentials/v1',
  ];

  /**
   * Create a new Verifiable Credential
   */
  async createCredential(options: VCCreationOptions): Promise<VerifiableCredential> {
    try {
      const credentialId = this.generateCredentialId();
      const issuanceDate = new Date().toISOString();

      // Create base credential
      const credential: Omit<VerifiableCredential, 'proof'> = {
        '@context': this.VC_CONTEXT,
        id: credentialId,
        type: ['VerifiableCredential', ...options.credentialType],
        issuer: options.issuerDID,
        issuanceDate,
        expirationDate: options.expirationDate,
        credentialSubject: {
          ...options.credentialSubject,
          id: options.subjectDID,
        },
        credentialStatus: options.credentialStatus,
      };

      // Add optional fields
      if (options.evidence) {
        (credential as any).evidence = options.evidence;
      }
      if (options.termsOfUse) {
        (credential as any).termsOfUse = options.termsOfUse;
      }

      // Create proof
      const proof = await this.createCredentialProof(credential, options.issuerDID);

      return {
        ...credential,
        proof,
      };
    } catch (error) {
      console.error('Failed to create credential:', error);
      throw new Error(`Failed to create verifiable credential: ${error}`);
    }
  }

  /**
   * Create a Verifiable Presentation
   */
  async createPresentation(options: VPCreationOptions): Promise<CredentialPresentation> {
    try {
      const presentationId = this.generatePresentationId();
      const created = new Date().toISOString();

      // Apply selective disclosure if specified
      const processedCredentials = options.selectiveDisclosure
        ? this.applySelectiveDisclosure(options.credentials, options.selectiveDisclosure)
        : options.credentials;

      // Create base presentation
      const presentation: Omit<CredentialPresentation, 'proof'> = {
        '@context': this.VP_CONTEXT,
        id: presentationId,
        type: ['VerifiablePresentation'],
        holder: options.holderDID,
        verifiableCredential: processedCredentials,
        created,
        expirationDate: options.expirationDate,
      };

      // Create proof
      const proof = await this.createPresentationProof(
        presentation,
        options.holderDID,
        options.challenge,
        options.domain
      );

      return {
        ...presentation,
        proof,
      };
    } catch (error) {
      console.error('Failed to create presentation:', error);
      throw new Error(`Failed to create verifiable presentation: ${error}`);
    }
  }

  /**
   * Verify a Verifiable Credential
   */
  async verifyCredential(
    credential: VerifiableCredential,
    options: VCVerificationOptions = {}
  ): Promise<VCVerificationResult> {
    const result: VCVerificationResult = {
      valid: false,
      checks: {
        signatureValid: false,
        issuerValid: false,
        notExpired: true,
        notRevoked: true,
        schemaValid: true,
      },
      errors: [],
      warnings: [],
      verifiedAt: new Date().toISOString(),
    };

    try {
      // 1. Verify signature
      result.checks.signatureValid = await this.verifyCredentialSignature(credential);
      if (!result.checks.signatureValid) {
        result.errors.push('Invalid credential signature');
      }

      // 2. Verify issuer
      result.checks.issuerValid = await this.verifyIssuer(credential, options.trustedIssuers);
      if (!result.checks.issuerValid) {
        result.errors.push('Untrusted or invalid issuer');
      }

      // 3. Check expiration
      if (options.checkExpiration !== false) {
        result.checks.notExpired = this.checkExpiration(credential);
        if (!result.checks.notExpired) {
          result.errors.push('Credential has expired');
        }
      }

      // 4. Check revocation status
      if (options.checkRevocation !== false && credential.credentialStatus) {
        result.checks.notRevoked = await this.checkRevocationStatus(credential);
        if (!result.checks.notRevoked) {
          result.errors.push('Credential has been revoked');
        }
      }

      // 5. Validate schema (if available)
      if (credential.credentialSchema) {
        result.checks.schemaValid = await this.validateSchema(credential);
        if (!result.checks.schemaValid) {
          result.errors.push('Credential does not conform to schema');
        }
      }

      // Overall validity
      result.valid = Object.values(result.checks).every(check => check === true);

      return result;
    } catch (error) {
      console.error('Failed to verify credential:', error);
      result.errors.push(`Verification failed: ${error}`);
      return result;
    }
  }

  /**
   * Verify a Verifiable Presentation
   */
  async verifyPresentation(
    presentation: CredentialPresentation,
    challenge: string,
    domain?: string,
    options: VCVerificationOptions = {}
  ): Promise<VCVerificationResult> {
    const result: VCVerificationResult = {
      valid: false,
      checks: {
        signatureValid: false,
        issuerValid: true, // Not applicable for presentations
        notExpired: true,
        notRevoked: true,
        schemaValid: true,
      },
      errors: [],
      warnings: [],
      verifiedAt: new Date().toISOString(),
    };

    try {
      // 1. Verify presentation signature
      result.checks.signatureValid = await this.verifyPresentationSignature(
        presentation,
        challenge,
        domain
      );
      if (!result.checks.signatureValid) {
        result.errors.push('Invalid presentation signature');
      }

      // 2. Check presentation expiration
      if (presentation.expirationDate) {
        const now = new Date();
        const expiration = new Date(presentation.expirationDate);
        result.checks.notExpired = now < expiration;
        if (!result.checks.notExpired) {
          result.errors.push('Presentation has expired');
        }
      }

      // 3. Verify each credential in the presentation
      const credentialResults = await Promise.all(
        presentation.verifiableCredential.map(credential =>
          this.verifyCredential(credential, options)
        )
      );

      // Aggregate credential verification results
      const allCredentialsValid = credentialResults.every(cr => cr.valid);
      if (!allCredentialsValid) {
        result.errors.push('One or more credentials in the presentation are invalid');
        credentialResults.forEach((cr, index) => {
          if (!cr.valid) {
            result.errors.push(`Credential ${index + 1}: ${cr.errors.join(', ')}`);
          }
        });
      }

      // Overall validity
      result.valid = result.checks.signatureValid && 
                    result.checks.notExpired && 
                    allCredentialsValid;

      return result;
    } catch (error) {
      console.error('Failed to verify presentation:', error);
      result.errors.push(`Verification failed: ${error}`);
      return result;
    }
  }

  /**
   * Apply selective disclosure to credentials
   */
  private applySelectiveDisclosure(
    credentials: VerifiableCredential[],
    selectiveDisclosure: SelectiveDisclosure[]
  ): VerifiableCredential[] {
    return credentials.map(credential => {
      const disclosure = selectiveDisclosure.find(
        sd => sd.credentialId === credential.id
      );

      if (!disclosure) {
        return credential;
      }

      // Create a copy of the credential
      const processedCredential = JSON.parse(JSON.stringify(credential));

      // Remove hidden fields from credential subject
      disclosure.hiddenFields.forEach(field => {
        if (processedCredential.credentialSubject[field] !== undefined) {
          delete processedCredential.credentialSubject[field];
        }
      });

      // Add selective disclosure proof
      if (!processedCredential.proof.selectiveDisclosure) {
        processedCredential.proof.selectiveDisclosure = {
          disclosedFields: disclosure.disclosedFields,
          hiddenFields: disclosure.hiddenFields,
          purpose: disclosure.purpose,
        };
      }

      return processedCredential;
    });
  }

  /**
   * Create credential proof
   */
  private async createCredentialProof(
    credential: Omit<VerifiableCredential, 'proof'>,
    issuerDID: string
  ): Promise<CredentialProof> {
    try {
      // Get issuer's signing key
      const issuerKeys = await keyService.listKeys();
      const signingKey = issuerKeys.find(key => 
        key.purpose.includes('assertionMethod' as any)
      );

      if (!signingKey) {
        throw new Error('No signing key found for issuer');
      }

      // Create canonical representation for signing
      const canonicalCredential = this.canonicalize(credential);

      // Sign the credential
      const signatureResult = await keyService.signData(
        signingKey.keyId,
        canonicalCredential
      );

      return {
        type: 'JsonWebSignature2020',
        created: new Date().toISOString(),
        verificationMethod: `${issuerDID}#${signingKey.keyId}`,
        proofPurpose: 'assertionMethod',
        proofValue: signatureResult.signature,
      };
    } catch (error) {
      console.error('Failed to create credential proof:', error);
      throw new Error(`Failed to create credential proof: ${error}`);
    }
  }

  /**
   * Create presentation proof
   */
  private async createPresentationProof(
    presentation: Omit<CredentialPresentation, 'proof'>,
    holderDID: string,
    challenge: string,
    domain?: string
  ): Promise<PresentationProof> {
    try {
      // Get holder's authentication key
      const holderKeys = await keyService.listKeys();
      const authKey = holderKeys.find(key => 
        key.purpose.includes('authentication' as any)
      );

      if (!authKey) {
        throw new Error('No authentication key found for holder');
      }

      // Create canonical representation for signing
      const canonicalPresentation = this.canonicalize({
        ...presentation,
        challenge,
        domain,
      });

      // Sign the presentation
      const signatureResult = await keyService.signData(
        authKey.keyId,
        canonicalPresentation
      );

      return {
        type: 'JsonWebSignature2020',
        created: new Date().toISOString(),
        verificationMethod: `${holderDID}#${authKey.keyId}`,
        proofPurpose: 'authentication',
        challenge,
        domain,
        proofValue: signatureResult.signature,
      };
    } catch (error) {
      console.error('Failed to create presentation proof:', error);
      throw new Error(`Failed to create presentation proof: ${error}`);
    }
  }

  /**
   * Verify credential signature
   */
  private async verifyCredentialSignature(credential: VerifiableCredential): Promise<boolean> {
    try {
      // Extract verification method
      const verificationMethod = credential.proof.verificationMethod;
      const keyId = this.extractKeyIdFromVerificationMethod(verificationMethod);

      // Create canonical representation
      const {proof, ...credentialWithoutProof} = credential;
      const canonicalCredential = this.canonicalize(credentialWithoutProof);

      // Verify signature
      const verificationResult = await keyService.verifySignature(
        keyId,
        canonicalCredential,
        proof.proofValue
      );

      return verificationResult.valid;
    } catch (error) {
      console.error('Failed to verify credential signature:', error);
      return false;
    }
  }

  /**
   * Verify presentation signature
   */
  private async verifyPresentationSignature(
    presentation: CredentialPresentation,
    challenge: string,
    domain?: string
  ): Promise<boolean> {
    try {
      // Verify challenge matches
      if (presentation.proof.challenge !== challenge) {
        return false;
      }

      // Verify domain matches (if provided)
      if (domain && presentation.proof.domain !== domain) {
        return false;
      }

      // Extract verification method
      const verificationMethod = presentation.proof.verificationMethod;
      const keyId = this.extractKeyIdFromVerificationMethod(verificationMethod);

      // Create canonical representation
      const {proof, ...presentationWithoutProof} = presentation;
      const canonicalPresentation = this.canonicalize({
        ...presentationWithoutProof,
        challenge,
        domain,
      });

      // Verify signature
      const verificationResult = await keyService.verifySignature(
        keyId,
        canonicalPresentation,
        proof.proofValue
      );

      return verificationResult.valid;
    } catch (error) {
      console.error('Failed to verify presentation signature:', error);
      return false;
    }
  }

  /**
   * Verify issuer
   */
  private async verifyIssuer(
    credential: VerifiableCredential,
    trustedIssuers?: string[]
  ): Promise<boolean> {
    try {
      const issuerDID = typeof credential.issuer === 'string' 
        ? credential.issuer 
        : credential.issuer.id;

      // Check against trusted issuers list
      if (trustedIssuers && trustedIssuers.length > 0) {
        return trustedIssuers.includes(issuerDID);
      }

      // Resolve issuer DID to verify it exists and is valid
      const resolutionResult = await didService.resolveDID(issuerDID);
      return resolutionResult.didDocument !== null && 
             !resolutionResult.didDocumentMetadata.deactivated;
    } catch (error) {
      console.error('Failed to verify issuer:', error);
      return false;
    }
  }

  /**
   * Check credential expiration
   */
  private checkExpiration(credential: VerifiableCredential): boolean {
    if (!credential.expirationDate) {
      return true; // No expiration date means it doesn't expire
    }

    const now = new Date();
    const expiration = new Date(credential.expirationDate);
    return now < expiration;
  }

  /**
   * Check revocation status
   */
  private async checkRevocationStatus(credential: VerifiableCredential): Promise<boolean> {
    try {
      if (!credential.credentialStatus) {
        return true; // No status means not revoked
      }

      // Call revocation status API
      const response = await apiClient.get(
        `/api/v1/credentials/status/${credential.credentialStatus.id}`
      );

      return response.data.status === 'active';
    } catch (error) {
      console.error('Failed to check revocation status:', error);
      return false; // Assume revoked if we can't check
    }
  }

  /**
   * Validate credential against schema
   */
  private async validateSchema(credential: VerifiableCredential): Promise<boolean> {
    try {
      if (!credential.credentialSchema) {
        return true; // No schema means valid
      }

      // Validate against schema
      const response = await apiClient.post('/api/v1/credentials/validate-schema', {
        credential,
        schemaId: credential.credentialSchema.id,
      });

      return response.data.valid;
    } catch (error) {
      console.error('Failed to validate schema:', error);
      return false;
    }
  }

  /**
   * Canonicalize object for signing
   */
  private canonicalize(obj: any): string {
    // This is a simplified canonicalization
    // In production, use a proper JSON-LD canonicalization library
    return JSON.stringify(obj, Object.keys(obj).sort());
  }

  /**
   * Extract key ID from verification method
   */
  private extractKeyIdFromVerificationMethod(verificationMethod: string): string {
    // Extract key ID from DID URL (e.g., "did:example:123#key-1" -> "key-1")
    const parts = verificationMethod.split('#');
    return parts.length > 1 ? parts[1] : verificationMethod;
  }

  /**
   * Generate unique credential ID
   */
  private generateCredentialId(): string {
    return `urn:uuid:${this.generateUUID()}`;
  }

  /**
   * Generate unique presentation ID
   */
  private generatePresentationId(): string {
    return `urn:uuid:${this.generateUUID()}`;
  }

  /**
   * Generate UUID v4
   */
  private generateUUID(): string {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0;
      const v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }
}

export const vcService = new VCService();
