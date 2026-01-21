/**
 * DID (Decentralized Identifier) Service
 * 
 * Handles creation, resolution, and management of DIDs across different methods
 * Supports EBSI, Ethereum, Key-based, and Web DIDs
 */

import {apiClient} from './apiClient';
import {keyService} from './keyService';
import {secureStorage} from './secureStorage';

export type DIDMethod = 'ebsi' | 'ethr' | 'key' | 'web';

export interface DIDDocument {
  '@context': string[];
  id: string;
  controller?: string;
  verificationMethod: VerificationMethod[];
  authentication: string[];
  assertionMethod: string[];
  keyAgreement?: string[];
  capabilityInvocation?: string[];
  capabilityDelegation?: string[];
  service?: ServiceEndpoint[];
  created?: string;
  updated?: string;
  proof?: Proof;
}

export interface VerificationMethod {
  id: string;
  type: string;
  controller: string;
  publicKeyJwk?: any;
  publicKeyMultibase?: string;
  publicKeyBase58?: string;
  publicKeyHex?: string;
}

export interface ServiceEndpoint {
  id: string;
  type: string;
  serviceEndpoint: string | object;
  description?: string;
}

export interface Proof {
  type: string;
  created: string;
  verificationMethod: string;
  proofPurpose: string;
  proofValue: string;
}

export interface DIDCreationOptions {
  keyType?: string;
  controller?: string;
  service?: ServiceEndpoint[];
  verificationMethods?: VerificationMethod[];
}

export interface DIDCreationResult {
  id: string;
  document: DIDDocument;
  keys: {
    keyId: string;
    publicKey: string;
    privateKey: string;
  };
  metadata: {
    method: DIDMethod;
    network?: string;
    created: string;
    lastUpdated: string;
  };
}

export interface DIDResolutionResult {
  didDocument: DIDDocument | null;
  didResolutionMetadata: {
    contentType?: string;
    error?: string;
    errorMessage?: string;
  };
  didDocumentMetadata: {
    created?: string;
    updated?: string;
    deactivated?: boolean;
    versionId?: string;
  };
}

class DIDService {
  private readonly DID_STORAGE_PREFIX = 'did_';
  private readonly DID_METADATA_PREFIX = 'did_meta_';

  /**
   * Create a new DID using the specified method
   */
  async createDID(
    method: DIDMethod,
    publicKey: string,
    options?: DIDCreationOptions
  ): Promise<DIDCreationResult> {
    try {
      switch (method) {
        case 'ebsi':
          return await this.createEBSIDID(publicKey, options);
        case 'ethr':
          return await this.createEthereumDID(publicKey, options);
        case 'key':
          return await this.createKeyDID(publicKey, options);
        case 'web':
          return await this.createWebDID(publicKey, options);
        default:
          throw new Error(`Unsupported DID method: ${method}`);
      }
    } catch (error) {
      console.error('Failed to create DID:', error);
      throw new Error(`Failed to create DID: ${error}`);
    }
  }

  /**
   * Create EBSI DID (European Blockchain Services Infrastructure)
   */
  private async createEBSIDID(
    publicKey: string,
    options?: DIDCreationOptions
  ): Promise<DIDCreationResult> {
    try {
      // Generate DID identifier from public key
      const identifier = this.generateEBSIIdentifier(publicKey);
      const did = `did:ebsi:${identifier}`;

      // Create DID document
      const didDocument: DIDDocument = {
        '@context': [
          'https://www.w3.org/ns/did/v1',
          'https://w3id.org/security/suites/jws-2020/v1',
        ],
        id: did,
        controller: options?.controller || did,
        verificationMethod: [
          {
            id: `${did}#keys-1`,
            type: 'JsonWebKey2020',
            controller: did,
            publicKeyJwk: this.convertPublicKeyToJWK(publicKey, options?.keyType || 'SECP256R1'),
          },
        ],
        authentication: [`${did}#keys-1`],
        assertionMethod: [`${did}#keys-1`],
        service: options?.service || [],
      };

      // Register DID on EBSI network
      await this.registerEBSIDID(did, didDocument);

      const result: DIDCreationResult = {
        id: did,
        document: didDocument,
        keys: {
          keyId: `${did}#keys-1`,
          publicKey,
          privateKey: '', // Private key should be retrieved from key service
        },
        metadata: {
          method: 'ebsi',
          network: 'ebsi-testnet',
          created: new Date().toISOString(),
          lastUpdated: new Date().toISOString(),
        },
      };

      await this.storeDID(result);
      return result;
    } catch (error) {
      console.error('Failed to create EBSI DID:', error);
      throw new Error(`Failed to create EBSI DID: ${error}`);
    }
  }

  /**
   * Create Ethereum DID
   */
  private async createEthereumDID(
    publicKey: string,
    options?: DIDCreationOptions
  ): Promise<DIDCreationResult> {
    try {
      // Generate Ethereum address from public key
      const address = this.generateEthereumAddress(publicKey);
      const did = `did:ethr:${address}`;

      const didDocument: DIDDocument = {
        '@context': [
          'https://www.w3.org/ns/did/v1',
          'https://w3id.org/security/suites/secp256k1recovery2020/v2',
        ],
        id: did,
        controller: options?.controller || did,
        verificationMethod: [
          {
            id: `${did}#controller`,
            type: 'EcdsaSecp256k1RecoveryMethod2020',
            controller: did,
            publicKeyHex: publicKey,
          },
        ],
        authentication: [`${did}#controller`],
        assertionMethod: [`${did}#controller`],
        service: options?.service || [],
      };

      // Register DID on Ethereum network
      await this.registerEthereumDID(did, didDocument);

      const result: DIDCreationResult = {
        id: did,
        document: didDocument,
        keys: {
          keyId: `${did}#controller`,
          publicKey,
          privateKey: '',
        },
        metadata: {
          method: 'ethr',
          network: 'sepolia',
          created: new Date().toISOString(),
          lastUpdated: new Date().toISOString(),
        },
      };

      await this.storeDID(result);
      return result;
    } catch (error) {
      console.error('Failed to create Ethereum DID:', error);
      throw new Error(`Failed to create Ethereum DID: ${error}`);
    }
  }

  /**
   * Create Key-based DID
   */
  private async createKeyDID(
    publicKey: string,
    options?: DIDCreationOptions
  ): Promise<DIDCreationResult> {
    try {
      // Generate DID from public key using multibase encoding
      const multibaseKey = this.encodePublicKeyMultibase(publicKey);
      const did = `did:key:${multibaseKey}`;

      const didDocument: DIDDocument = {
        '@context': [
          'https://www.w3.org/ns/did/v1',
          'https://w3id.org/security/suites/ed25519-2020/v1',
        ],
        id: did,
        verificationMethod: [
          {
            id: `${did}#${multibaseKey}`,
            type: 'Ed25519VerificationKey2020',
            controller: did,
            publicKeyMultibase: multibaseKey,
          },
        ],
        authentication: [`${did}#${multibaseKey}`],
        assertionMethod: [`${did}#${multibaseKey}`],
        service: options?.service || [],
      };

      const result: DIDCreationResult = {
        id: did,
        document: didDocument,
        keys: {
          keyId: `${did}#${multibaseKey}`,
          publicKey,
          privateKey: '',
        },
        metadata: {
          method: 'key',
          created: new Date().toISOString(),
          lastUpdated: new Date().toISOString(),
        },
      };

      await this.storeDID(result);
      return result;
    } catch (error) {
      console.error('Failed to create Key DID:', error);
      throw new Error(`Failed to create Key DID: ${error}`);
    }
  }

  /**
   * Create Web DID
   */
  private async createWebDID(
    publicKey: string,
    options?: DIDCreationOptions
  ): Promise<DIDCreationResult> {
    try {
      // For demo purposes, use a placeholder domain
      const domain = 'wallet.provenly.io';
      const identifier = this.generateWebIdentifier();
      const did = `did:web:${domain}:users:${identifier}`;

      const didDocument: DIDDocument = {
        '@context': [
          'https://www.w3.org/ns/did/v1',
          'https://w3id.org/security/suites/jws-2020/v1',
        ],
        id: did,
        controller: options?.controller || did,
        verificationMethod: [
          {
            id: `${did}#key-1`,
            type: 'JsonWebKey2020',
            controller: did,
            publicKeyJwk: this.convertPublicKeyToJWK(publicKey, options?.keyType || 'SECP256R1'),
          },
        ],
        authentication: [`${did}#key-1`],
        assertionMethod: [`${did}#key-1`],
        service: options?.service || [
          {
            id: `${did}#provenly-wallet`,
            type: 'ProvenlyWallet',
            serviceEndpoint: `https://${domain}/users/${identifier}`,
          },
        ],
      };

      // Publish DID document to web location
      await this.publishWebDID(did, didDocument);

      const result: DIDCreationResult = {
        id: did,
        document: didDocument,
        keys: {
          keyId: `${did}#key-1`,
          publicKey,
          privateKey: '',
        },
        metadata: {
          method: 'web',
          network: domain,
          created: new Date().toISOString(),
          lastUpdated: new Date().toISOString(),
        },
      };

      await this.storeDID(result);
      return result;
    } catch (error) {
      console.error('Failed to create Web DID:', error);
      throw new Error(`Failed to create Web DID: ${error}`);
    }
  }

  /**
   * Resolve a DID to its DID document
   */
  async resolveDID(did: string): Promise<DIDResolutionResult> {
    try {
      const method = this.extractDIDMethod(did);

      switch (method) {
        case 'ebsi':
          return await this.resolveEBSIDID(did);
        case 'ethr':
          return await this.resolveEthereumDID(did);
        case 'key':
          return await this.resolveKeyDID(did);
        case 'web':
          return await this.resolveWebDID(did);
        default:
          return {
            didDocument: null,
            didResolutionMetadata: {
              error: 'methodNotSupported',
              errorMessage: `DID method '${method}' is not supported`,
            },
            didDocumentMetadata: {},
          };
      }
    } catch (error) {
      console.error('Failed to resolve DID:', error);
      return {
        didDocument: null,
        didResolutionMetadata: {
          error: 'internalError',
          errorMessage: `Failed to resolve DID: ${error}`,
        },
        didDocumentMetadata: {},
      };
    }
  }

  /**
   * Update a DID document
   */
  async updateDID(did: string, updates: Partial<DIDDocument>): Promise<DIDDocument> {
    try {
      const stored = await this.getStoredDID(did);
      if (!stored) {
        throw new Error('DID not found in local storage');
      }

      const updatedDocument: DIDDocument = {
        ...stored.document,
        ...updates,
        updated: new Date().toISOString(),
      };

      const method = this.extractDIDMethod(did);
      
      // Update on the respective network
      switch (method) {
        case 'ebsi':
          await this.updateEBSIDID(did, updatedDocument);
          break;
        case 'ethr':
          await this.updateEthereumDID(did, updatedDocument);
          break;
        case 'web':
          await this.updateWebDID(did, updatedDocument);
          break;
        case 'key':
          // Key DIDs are immutable
          throw new Error('Key DIDs cannot be updated');
      }

      // Update local storage
      stored.document = updatedDocument;
      stored.metadata.lastUpdated = new Date().toISOString();
      await this.storeDID(stored);

      return updatedDocument;
    } catch (error) {
      console.error('Failed to update DID:', error);
      throw new Error(`Failed to update DID: ${error}`);
    }
  }

  /**
   * Deactivate a DID
   */
  async deactivateDID(did: string): Promise<void> {
    try {
      const method = this.extractDIDMethod(did);

      switch (method) {
        case 'ebsi':
          await this.deactivateEBSIDID(did);
          break;
        case 'ethr':
          await this.deactivateEthereumDID(did);
          break;
        case 'web':
          await this.deactivateWebDID(did);
          break;
        case 'key':
          // Key DIDs cannot be deactivated, only forgotten
          break;
      }

      // Remove from local storage
      await this.removeStoredDID(did);
    } catch (error) {
      console.error('Failed to deactivate DID:', error);
      throw new Error(`Failed to deactivate DID: ${error}`);
    }
  }

  /**
   * List all stored DIDs
   */
  async listDIDs(): Promise<DIDCreationResult[]> {
    try {
      const keys = secureStorage.getAllKeys();
      const didKeys = keys.filter(key => key.startsWith(this.DID_STORAGE_PREFIX));
      
      const dids: DIDCreationResult[] = [];
      for (const key of didKeys) {
        const did = await secureStorage.getObject<DIDCreationResult>(key);
        if (did) {
          dids.push(did);
        }
      }
      
      return dids.sort((a, b) => 
        new Date(b.metadata.created).getTime() - new Date(a.metadata.created).getTime()
      );
    } catch (error) {
      console.error('Failed to list DIDs:', error);
      return [];
    }
  }

  // Private helper methods

  private extractDIDMethod(did: string): DIDMethod {
    const parts = did.split(':');
    if (parts.length < 3 || parts[0] !== 'did') {
      throw new Error('Invalid DID format');
    }
    return parts[1] as DIDMethod;
  }

  private generateEBSIIdentifier(publicKey: string): string {
    // Generate EBSI identifier from public key hash
    return `z${publicKey.substring(0, 32)}`;
  }

  private generateEthereumAddress(publicKey: string): string {
    // Generate Ethereum address from public key
    // This is a simplified implementation
    return `0x${publicKey.substring(2, 42)}`;
  }

  private generateWebIdentifier(): string {
    return `user_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  private encodePublicKeyMultibase(publicKey: string): string {
    // Encode public key in multibase format
    // This is a simplified implementation
    return `z${Buffer.from(publicKey, 'hex').toString('base64url')}`;
  }

  private convertPublicKeyToJWK(publicKey: string, keyType: string): any {
    // Convert public key to JWK format
    return {
      kty: keyType === 'RSA' ? 'RSA' : 'EC',
      use: 'sig',
      key_ops: ['verify'],
      alg: keyType === 'RSA' ? 'RS256' : 'ES256',
      x: publicKey.substring(0, 32),
      y: publicKey.substring(32, 64),
    };
  }

  private async storeDID(did: DIDCreationResult): Promise<void> {
    await secureStorage.setObject(`${this.DID_STORAGE_PREFIX}${did.id}`, did, {encrypt: true});
  }

  private async getStoredDID(did: string): Promise<DIDCreationResult | null> {
    return await secureStorage.getObject<DIDCreationResult>(
      `${this.DID_STORAGE_PREFIX}${did}`,
      {encrypt: true}
    );
  }

  private async removeStoredDID(did: string): Promise<void> {
    await secureStorage.removeItem(`${this.DID_STORAGE_PREFIX}${did}`);
  }

  // Network-specific methods (simplified implementations)

  private async registerEBSIDID(did: string, document: DIDDocument): Promise<void> {
    await apiClient.post('/api/v1/did/ebsi/register', {did, document});
  }

  private async registerEthereumDID(did: string, document: DIDDocument): Promise<void> {
    await apiClient.post('/api/v1/did/ethr/register', {did, document});
  }

  private async publishWebDID(did: string, document: DIDDocument): Promise<void> {
    await apiClient.post('/api/v1/did/web/publish', {did, document});
  }

  private async resolveEBSIDID(did: string): Promise<DIDResolutionResult> {
    const response = await apiClient.get(`/api/v1/did/ebsi/resolve/${encodeURIComponent(did)}`);
    return response.data;
  }

  private async resolveEthereumDID(did: string): Promise<DIDResolutionResult> {
    const response = await apiClient.get(`/api/v1/did/ethr/resolve/${encodeURIComponent(did)}`);
    return response.data;
  }

  private async resolveKeyDID(did: string): Promise<DIDResolutionResult> {
    // Key DIDs can be resolved locally
    const stored = await this.getStoredDID(did);
    if (stored) {
      return {
        didDocument: stored.document,
        didResolutionMetadata: {contentType: 'application/did+ld+json'},
        didDocumentMetadata: {
          created: stored.metadata.created,
          updated: stored.metadata.lastUpdated,
        },
      };
    }
    
    return {
      didDocument: null,
      didResolutionMetadata: {error: 'notFound'},
      didDocumentMetadata: {},
    };
  }

  private async resolveWebDID(did: string): Promise<DIDResolutionResult> {
    const response = await apiClient.get(`/api/v1/did/web/resolve/${encodeURIComponent(did)}`);
    return response.data;
  }

  private async updateEBSIDID(did: string, document: DIDDocument): Promise<void> {
    await apiClient.put(`/api/v1/did/ebsi/update/${encodeURIComponent(did)}`, {document});
  }

  private async updateEthereumDID(did: string, document: DIDDocument): Promise<void> {
    await apiClient.put(`/api/v1/did/ethr/update/${encodeURIComponent(did)}`, {document});
  }

  private async updateWebDID(did: string, document: DIDDocument): Promise<void> {
    await apiClient.put(`/api/v1/did/web/update/${encodeURIComponent(did)}`, {document});
  }

  private async deactivateEBSIDID(did: string): Promise<void> {
    await apiClient.delete(`/api/v1/did/ebsi/deactivate/${encodeURIComponent(did)}`);
  }

  private async deactivateEthereumDID(did: string): Promise<void> {
    await apiClient.delete(`/api/v1/did/ethr/deactivate/${encodeURIComponent(did)}`);
  }

  private async deactivateWebDID(did: string): Promise<void> {
    await apiClient.delete(`/api/v1/did/web/deactivate/${encodeURIComponent(did)}`);
  }
}

export const didService = new DIDService();
