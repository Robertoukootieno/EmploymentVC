/**
 * Cryptographic Types and Interfaces
 * 
 * Defines types for key management, DIDs, and cryptographic operations
 */

export enum KeyType {
  RSA = 'RSA',
  SECP256R1 = 'SECP256R1',
  SECP256K1 = 'SECP256K1',
  ED25519 = 'ED25519',
}

export enum KeyPurpose {
  AUTHENTICATION = 'authentication',
  ASSERTION_METHOD = 'assertionMethod',
  KEY_AGREEMENT = 'keyAgreement',
  CAPABILITY_INVOCATION = 'capabilityInvocation',
  CAPABILITY_DELEGATION = 'capabilityDelegation',
}

export enum SignatureAlgorithm {
  RS256 = 'RS256',
  RS384 = 'RS384',
  RS512 = 'RS512',
  ES256 = 'ES256',
  ES384 = 'ES384',
  ES512 = 'ES512',
  ES256K = 'ES256K',
  EdDSA = 'EdDSA',
}

export type DIDMethod = 'ebsi' | 'ethr' | 'key' | 'web';

export interface KeyPair {
  publicKey: string;
  privateKey: string;
  keyType: KeyType;
  keyId: string;
  createdAt: string;
  algorithm: string;
  curve?: string;
  keySize?: number;
}

export interface KeyMetadata {
  keyId: string;
  keyType: KeyType;
  algorithm: string;
  curve?: string;
  keySize?: number;
  purpose: KeyPurpose[];
  createdAt: string;
  lastUsed?: string;
  isHardwareBacked: boolean;
  description?: string;
  tags?: string[];
}

export interface SignatureResult {
  signature: string;
  algorithm: string;
  keyId: string;
  timestamp: string;
  publicKey?: string;
}

export interface VerificationResult {
  valid: boolean;
  keyId?: string;
  algorithm?: string;
  publicKey?: string;
  error?: string;
  timestamp: string;
}

export interface EncryptionResult {
  encryptedData: string;
  algorithm: string;
  keyId: string;
  iv?: string;
  timestamp: string;
}

export interface DecryptionResult {
  decryptedData: string;
  keyId: string;
  algorithm: string;
  timestamp: string;
}

// DID Types

export interface DIDDocument {
  '@context': string[];
  id: string;
  controller?: string | string[];
  verificationMethod: VerificationMethod[];
  authentication: (string | VerificationMethod)[];
  assertionMethod?: (string | VerificationMethod)[];
  keyAgreement?: (string | VerificationMethod)[];
  capabilityInvocation?: (string | VerificationMethod)[];
  capabilityDelegation?: (string | VerificationMethod)[];
  service?: ServiceEndpoint[];
  created?: string;
  updated?: string;
  proof?: DIDProof;
  alsoKnownAs?: string[];
}

export interface VerificationMethod {
  id: string;
  type: string;
  controller: string;
  publicKeyJwk?: JsonWebKey;
  publicKeyMultibase?: string;
  publicKeyBase58?: string;
  publicKeyHex?: string;
  publicKeyPem?: string;
  blockchainAccountId?: string;
  ethereumAddress?: string;
}

export interface ServiceEndpoint {
  id: string;
  type: string | string[];
  serviceEndpoint: string | string[] | object;
  description?: string;
  routingKeys?: string[];
  accept?: string[];
}

export interface DIDProof {
  type: string;
  created: string;
  verificationMethod: string;
  proofPurpose: string;
  proofValue: string;
  challenge?: string;
  domain?: string;
  nonce?: string;
}

export interface DIDResolutionResult {
  didDocument: DIDDocument | null;
  didResolutionMetadata: DIDResolutionMetadata;
  didDocumentMetadata: DIDDocumentMetadata;
}

export interface DIDResolutionMetadata {
  contentType?: string;
  error?: string;
  errorMessage?: string;
  retrieved?: string;
  nextUpdate?: string;
  nextVersionId?: string;
}

export interface DIDDocumentMetadata {
  created?: string;
  updated?: string;
  deactivated?: boolean;
  versionId?: string;
  nextUpdate?: string;
  nextVersionId?: string;
  equivalentId?: string[];
  canonicalId?: string;
}

export interface DIDCreationOptions {
  keyType?: KeyType;
  controller?: string;
  service?: ServiceEndpoint[];
  verificationMethods?: VerificationMethod[];
  purposes?: KeyPurpose[];
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

// JSON Web Key (JWK) Types

export interface JsonWebKey {
  kty: 'RSA' | 'EC' | 'OKP' | 'oct';
  use?: 'sig' | 'enc';
  key_ops?: string[];
  alg?: string;
  kid?: string;
  x5u?: string;
  x5c?: string[];
  x5t?: string;
  'x5t#S256'?: string;
  // RSA specific
  n?: string;
  e?: string;
  d?: string;
  p?: string;
  q?: string;
  dp?: string;
  dq?: string;
  qi?: string;
  // EC specific
  crv?: string;
  x?: string;
  y?: string;
  // OKP specific (Ed25519, X25519)
  // x is used for public key
  // d is used for private key
  // Symmetric key specific
  k?: string;
}

// Cryptographic Operation Types

export interface HashOptions {
  algorithm: 'SHA-256' | 'SHA-384' | 'SHA-512' | 'SHA3-256' | 'SHA3-384' | 'SHA3-512';
  encoding?: 'hex' | 'base64' | 'base64url';
}

export interface HashResult {
  hash: string;
  algorithm: string;
  encoding: string;
  timestamp: string;
}

export interface KeyDerivationOptions {
  algorithm: 'PBKDF2' | 'scrypt' | 'Argon2';
  iterations?: number;
  salt?: string;
  keyLength?: number;
  memory?: number; // For scrypt and Argon2
  parallelism?: number; // For Argon2
}

export interface KeyDerivationResult {
  derivedKey: string;
  salt: string;
  algorithm: string;
  iterations?: number;
  keyLength: number;
  timestamp: string;
}

// Certificate Types

export interface X509Certificate {
  version: number;
  serialNumber: string;
  issuer: string;
  subject: string;
  notBefore: string;
  notAfter: string;
  publicKey: string;
  signature: string;
  signatureAlgorithm: string;
  extensions?: X509Extension[];
}

export interface X509Extension {
  oid: string;
  critical: boolean;
  value: string;
}

// Multicodec and Multibase Types

export interface MulticodecInfo {
  name: string;
  tag: string;
  code: number;
  description: string;
}

export interface MultibaseInfo {
  encoding: string;
  code: string;
  description: string;
}

// Key Exchange Types

export interface KeyExchangeRequest {
  publicKey: string;
  algorithm: string;
  keyId: string;
  timestamp: string;
}

export interface KeyExchangeResponse {
  sharedSecret: string;
  algorithm: string;
  keyId: string;
  timestamp: string;
}

// Hardware Security Module (HSM) Types

export interface HSMCapabilities {
  available: boolean;
  keyGeneration: boolean;
  signing: boolean;
  encryption: boolean;
  keyStorage: boolean;
  supportedAlgorithms: string[];
  maxKeySize: number;
}

export interface HSMKeyInfo {
  keyId: string;
  keyType: KeyType;
  algorithm: string;
  isHardwareBacked: boolean;
  canExport: boolean;
  purposes: KeyPurpose[];
}

// Backup and Recovery Types

export interface KeyBackup {
  keyId: string;
  encryptedPrivateKey: string;
  publicKey: string;
  metadata: KeyMetadata;
  backupMethod: 'mnemonic' | 'encrypted' | 'split';
  createdAt: string;
  expiresAt?: string;
}

export interface MnemonicBackup {
  mnemonic: string;
  wordCount: 12 | 15 | 18 | 21 | 24;
  language: string;
  passphrase?: string;
  derivationPath?: string;
}

export interface ShamirSecretSharing {
  threshold: number;
  shares: string[];
  shareCount: number;
  keyId: string;
  createdAt: string;
}

// Audit and Compliance Types

export interface CryptographicAuditLog {
  id: string;
  operation: 'generate' | 'sign' | 'verify' | 'encrypt' | 'decrypt' | 'derive';
  keyId: string;
  algorithm: string;
  success: boolean;
  error?: string;
  timestamp: string;
  metadata?: Record<string, any>;
}

export interface ComplianceReport {
  reportId: string;
  period: {
    start: string;
    end: string;
  };
  keyOperations: CryptographicAuditLog[];
  securityEvents: SecurityEvent[];
  compliance: {
    standard: string;
    status: 'compliant' | 'non-compliant' | 'partial';
    issues: string[];
  };
  generatedAt: string;
}

export interface SecurityEvent {
  id: string;
  type: 'key_compromise' | 'unauthorized_access' | 'algorithm_weakness' | 'certificate_expiry';
  severity: 'low' | 'medium' | 'high' | 'critical';
  description: string;
  affectedKeys: string[];
  timestamp: string;
  resolved: boolean;
  resolution?: string;
}
