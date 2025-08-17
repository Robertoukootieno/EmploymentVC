/**
 * Wallet Types and Interfaces
 * 
 * Defines the core types for both custodial and non-custodial wallets
 */

export enum WalletType {
  CUSTODIAL = 'CUSTODIAL',
  NON_CUSTODIAL = 'NON_CUSTODIAL',
}

export enum WalletStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  LOCKED = 'LOCKED',
  SUSPENDED = 'SUSPENDED',
}

export enum AuthMethod {
  PIN = 'PIN',
  BIOMETRIC = 'BIOMETRIC',
  PASSWORD = 'PASSWORD',
  WEB3_SIGNATURE = 'WEB3_SIGNATURE',
}

export interface WalletConfig {
  autoBackup: boolean;
  biometricEnabled: boolean;
  pinRequired: boolean;
  sessionTimeout: number; // in minutes
  multiSigEnabled: boolean;
  encryptionEnabled: boolean;
}

export interface WalletMetadata {
  name: string;
  description?: string;
  avatar?: string;
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface Wallet {
  id: string;
  ownerId: string;
  did?: string;
  type: WalletType;
  status: WalletStatus;
  metadata: WalletMetadata;
  config: WalletConfig;
  credentialCount: number;
  lastAccessedAt?: string;
}

export interface CustodialWallet extends Wallet {
  type: WalletType.CUSTODIAL;
  encryptionKeyId: string;
  backupEnabled: boolean;
  lastBackupAt?: string;
}

export interface NonCustodialWallet extends Wallet {
  type: WalletType.NON_CUSTODIAL;
  externalDid: string;
  metadataOnly: boolean;
  syncEnabled: boolean;
  lastSyncAt?: string;
}

export interface WalletCreationRequest {
  type: WalletType;
  name: string;
  description?: string;
  config: Partial<WalletConfig>;
  // For non-custodial wallets
  did?: string;
  ownershipProof?: any;
  // For custodial wallets
  authMethod: AuthMethod;
  authCredential: string; // PIN, password, or biometric data
}

export interface WalletBackup {
  walletId: string;
  encryptedData: string;
  backupDate: string;
  version: string;
  checksum: string;
}

export interface WalletRecovery {
  backupData: string;
  recoveryPhrase?: string;
  authCredential: string;
}

export interface WalletStats {
  totalCredentials: number;
  activeCredentials: number;
  expiredCredentials: number;
  revokedCredentials: number;
  presentationsCreated: number;
  lastActivity: string;
}

export interface WalletSecurity {
  authMethods: AuthMethod[];
  lastAuthAt: string;
  failedAttempts: number;
  lockedUntil?: string;
  securityLevel: 'LOW' | 'MEDIUM' | 'HIGH';
}

// API Response Types
export interface CreateWalletResponse {
  wallet: Wallet;
  recoveryPhrase?: string; // Only for non-custodial wallets
  backupData?: string; // Only for custodial wallets
}

export interface WalletListResponse {
  wallets: Wallet[];
  totalCount: number;
  page: number;
  size: number;
}

export interface WalletDetailsResponse {
  wallet: Wallet;
  stats: WalletStats;
  security: WalletSecurity;
  recentActivity: any[];
}
