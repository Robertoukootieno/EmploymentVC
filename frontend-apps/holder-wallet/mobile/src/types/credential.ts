/**
 * Verifiable Credential Types and Interfaces
 * 
 * Defines types for managing employment credentials in the wallet
 */

export enum CredentialStatus {
  ACTIVE = 'ACTIVE',
  EXPIRED = 'EXPIRED',
  REVOKED = 'REVOKED',
  SUSPENDED = 'SUSPENDED',
}

export enum CredentialType {
  EMPLOYMENT_VERIFICATION = 'EmploymentVerification',
  EDUCATION_CREDENTIAL = 'EducationCredential',
  SKILL_CERTIFICATION = 'SkillCertification',
  PROFESSIONAL_LICENSE = 'ProfessionalLicense',
  BACKGROUND_CHECK = 'BackgroundCheck',
}

export interface CredentialSubject {
  id: string; // DID of the subject
  employeeId?: string;
  fullName: string;
  email: string;
  position: string;
  department: string;
  startDate: string;
  endDate?: string;
  salary?: {
    amount: number;
    currency: string;
    frequency: 'HOURLY' | 'MONTHLY' | 'YEARLY';
  };
  skills?: string[];
  achievements?: string[];
  [key: string]: any;
}

export interface CredentialProof {
  type: string;
  created: string;
  verificationMethod: string;
  proofPurpose: string;
  proofValue: string;
  challenge?: string;
  domain?: string;
}

export interface VerifiableCredential {
  '@context': string[];
  id: string;
  type: string[];
  issuer: string | {
    id: string;
    name?: string;
    logo?: string;
  };
  issuanceDate: string;
  expirationDate?: string;
  credentialSubject: CredentialSubject;
  proof: CredentialProof;
  credentialStatus?: {
    id: string;
    type: string;
  };
}

export interface CredentialMetadata {
  id: string;
  walletId: string;
  credentialId: string;
  type: CredentialType;
  status: CredentialStatus;
  title: string;
  description?: string;
  issuerName: string;
  issuerLogo?: string;
  issuedAt: string;
  expiresAt?: string;
  tags: string[];
  notes?: string;
  isFavorite: boolean;
  isVisible: boolean;
  lastVerifiedAt?: string;
  verificationCount: number;
}

export interface StoredCredential {
  metadata: CredentialMetadata;
  credential: VerifiableCredential;
  encryptedData?: string; // For custodial wallets
  storageLocation?: string; // For non-custodial wallets
}

export interface CredentialPresentation {
  '@context': string[];
  id: string;
  type: string[];
  holder: string;
  verifiableCredential: VerifiableCredential[];
  proof: CredentialProof;
}

export interface SelectiveDisclosure {
  credentialId: string;
  disclosedFields: string[];
  hiddenFields: string[];
  purpose: string;
  requestedBy: string;
}

export interface PresentationRequest {
  id: string;
  verifier: {
    id: string;
    name: string;
    logo?: string;
  };
  challenge: string;
  domain?: string;
  requestedCredentials: {
    type: CredentialType[];
    requiredFields: string[];
    optionalFields: string[];
  }[];
  purpose: string;
  expiresAt: string;
  selectiveDisclosure?: boolean;
}

export interface PresentationResponse {
  requestId: string;
  presentation: CredentialPresentation;
  selectiveDisclosure?: SelectiveDisclosure[];
  submittedAt: string;
}

// API Types
export interface StoreCredentialRequest {
  walletId: string;
  credential: VerifiableCredential;
  metadata: Partial<CredentialMetadata>;
}

export interface StoreCredentialResponse {
  credentialId: string;
  walletId: string;
  encrypted: boolean;
  storedAt: string;
}

export interface GetCredentialRequest {
  walletId: string;
  credentialId: string;
}

export interface GetCredentialResponse {
  credential: VerifiableCredential;
  metadata: CredentialMetadata;
  status: CredentialStatus;
  storedAt: string;
}

export interface ListCredentialsRequest {
  walletId: string;
  type?: CredentialType;
  status?: CredentialStatus;
  page?: number;
  size?: number;
  search?: string;
  tags?: string[];
}

export interface ListCredentialsResponse {
  credentials: StoredCredential[];
  totalCount: number;
  page: number;
  size: number;
  totalPages: number;
}

export interface CreatePresentationRequest {
  walletId: string;
  credentialIds: string[];
  challenge: string;
  domain?: string;
  selectiveDisclosure?: SelectiveDisclosure[];
}

export interface CreatePresentationResponse {
  presentation: CredentialPresentation;
  holder: string;
  credentialCount: number;
  createdAt: string;
}

export interface VerifyCredentialRequest {
  credential: VerifiableCredential;
  challenge?: string;
  domain?: string;
}

export interface VerifyCredentialResponse {
  valid: boolean;
  issuerValid: boolean;
  notExpired: boolean;
  notRevoked: boolean;
  signatureValid: boolean;
  errors?: string[];
  verifiedAt: string;
}
