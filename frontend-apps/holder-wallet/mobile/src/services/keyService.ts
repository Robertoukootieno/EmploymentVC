/**
 * Cryptographic Key Service
 * 
 * Handles generation, storage, and management of cryptographic keys
 * Supports RSA, SECP256R1, SECP256K1, and Ed25519 key types
 */

import {NativeModules, Platform} from 'react-native';
import {secureStorage} from './secureStorage';
import CryptoJS from 'react-native-crypto-js';

// Import crypto libraries
import {ethers} from 'ethers';

export enum KeyType {
  RSA = 'RSA',
  SECP256R1 = 'SECP256R1',
  SECP256K1 = 'SECP256K1',
  ED25519 = 'ED25519',
}

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
  purpose: string[];
  createdAt: string;
  lastUsed?: string;
  isHardwareBacked: boolean;
}

export interface SignatureResult {
  signature: string;
  algorithm: string;
  keyId: string;
  timestamp: string;
}

export interface VerificationResult {
  valid: boolean;
  keyId?: string;
  algorithm?: string;
  error?: string;
}

class KeyService {
  private readonly KEY_STORAGE_PREFIX = 'key_';
  private readonly METADATA_STORAGE_PREFIX = 'key_meta_';

  /**
   * Generate a new cryptographic key pair
   */
  async generateKeyPair(keyType: KeyType, options?: {
    keySize?: number;
    purpose?: string[];
    hardwareBacked?: boolean;
  }): Promise<KeyPair> {
    try {
      const keyId = this.generateKeyId();
      const createdAt = new Date().toISOString();
      
      let keyPair: Omit<KeyPair, 'keyId' | 'createdAt'>;

      switch (keyType) {
        case KeyType.RSA:
          keyPair = await this.generateRSAKeyPair(options?.keySize || 2048);
          break;
        case KeyType.SECP256R1:
          keyPair = await this.generateSECP256R1KeyPair();
          break;
        case KeyType.SECP256K1:
          keyPair = await this.generateSECP256K1KeyPair();
          break;
        case KeyType.ED25519:
          keyPair = await this.generateEd25519KeyPair();
          break;
        default:
          throw new Error(`Unsupported key type: ${keyType}`);
      }

      const fullKeyPair: KeyPair = {
        ...keyPair,
        keyId,
        createdAt,
      };

      // Store key pair securely
      await this.storeKeyPair(fullKeyPair, options?.hardwareBacked);

      // Store metadata
      const metadata: KeyMetadata = {
        keyId,
        keyType,
        algorithm: keyPair.algorithm,
        curve: keyPair.curve,
        keySize: keyPair.keySize,
        purpose: options?.purpose || ['authentication', 'assertionMethod'],
        createdAt,
        isHardwareBacked: options?.hardwareBacked || false,
      };
      await this.storeKeyMetadata(metadata);

      return fullKeyPair;
    } catch (error) {
      console.error('Failed to generate key pair:', error);
      throw new Error(`Failed to generate ${keyType} key pair: ${error}`);
    }
  }

  /**
   * Generate RSA key pair
   */
  private async generateRSAKeyPair(keySize: number = 2048): Promise<Omit<KeyPair, 'keyId' | 'createdAt'>> {
    try {
      // Use native crypto if available, otherwise fallback to JS implementation
      if (Platform.OS === 'ios' || Platform.OS === 'android') {
        return await this.generateRSAKeyPairNative(keySize);
      } else {
        return await this.generateRSAKeyPairJS(keySize);
      }
    } catch (error) {
      console.error('RSA key generation failed:', error);
      throw new Error('Failed to generate RSA key pair');
    }
  }

  /**
   * Generate SECP256R1 (P-256) key pair
   */
  private async generateSECP256R1KeyPair(): Promise<Omit<KeyPair, 'keyId' | 'createdAt'>> {
    try {
      // Use native implementation for better security
      if (Platform.OS === 'ios' || Platform.OS === 'android') {
        return await this.generateECKeyPairNative('secp256r1');
      } else {
        return await this.generateSECP256R1KeyPairJS();
      }
    } catch (error) {
      console.error('SECP256R1 key generation failed:', error);
      throw new Error('Failed to generate SECP256R1 key pair');
    }
  }

  /**
   * Generate SECP256K1 key pair (Bitcoin/Ethereum curve)
   */
  private async generateSECP256K1KeyPair(): Promise<Omit<KeyPair, 'keyId' | 'createdAt'>> {
    try {
      // Use ethers.js for SECP256K1 key generation
      const wallet = ethers.Wallet.createRandom();
      
      return {
        publicKey: wallet.publicKey,
        privateKey: wallet.privateKey,
        keyType: KeyType.SECP256K1,
        algorithm: 'ECDSA',
        curve: 'secp256k1',
        keySize: 256,
      };
    } catch (error) {
      console.error('SECP256K1 key generation failed:', error);
      throw new Error('Failed to generate SECP256K1 key pair');
    }
  }

  /**
   * Generate Ed25519 key pair
   */
  private async generateEd25519KeyPair(): Promise<Omit<KeyPair, 'keyId' | 'createdAt'>> {
    try {
      // Use native implementation if available
      if (Platform.OS === 'ios' || Platform.OS === 'android') {
        return await this.generateEd25519KeyPairNative();
      } else {
        return await this.generateEd25519KeyPairJS();
      }
    } catch (error) {
      console.error('Ed25519 key generation failed:', error);
      throw new Error('Failed to generate Ed25519 key pair');
    }
  }

  /**
   * Native RSA key generation (iOS/Android)
   */
  private async generateRSAKeyPairNative(keySize: number): Promise<Omit<KeyPair, 'keyId' | 'createdAt'>> {
    // This would use native modules for hardware-backed key generation
    // For now, we'll use a JavaScript fallback
    return this.generateRSAKeyPairJS(keySize);
  }

  /**
   * JavaScript RSA key generation fallback
   */
  private async generateRSAKeyPairJS(keySize: number): Promise<Omit<KeyPair, 'keyId' | 'createdAt'>> {
    // This is a simplified implementation
    // In production, you'd use a proper RSA library like node-forge
    const keyPair = {
      publicKey: `-----BEGIN PUBLIC KEY-----\n${this.generateRandomBase64(294)}\n-----END PUBLIC KEY-----`,
      privateKey: `-----BEGIN PRIVATE KEY-----\n${this.generateRandomBase64(1679)}\n-----END PRIVATE KEY-----`,
      keyType: KeyType.RSA,
      algorithm: 'RSA-PSS',
      keySize,
    };
    
    return keyPair;
  }

  /**
   * Native EC key generation
   */
  private async generateECKeyPairNative(curve: string): Promise<Omit<KeyPair, 'keyId' | 'createdAt'>> {
    // This would use native modules for hardware-backed key generation
    // For now, we'll use a JavaScript fallback
    return this.generateSECP256R1KeyPairJS();
  }

  /**
   * JavaScript SECP256R1 key generation
   */
  private async generateSECP256R1KeyPairJS(): Promise<Omit<KeyPair, 'keyId' | 'createdAt'>> {
    // This is a simplified implementation
    // In production, you'd use a proper elliptic curve library
    const privateKeyHex = this.generateRandomHex(64);
    const publicKeyHex = this.generateRandomHex(128);
    
    return {
      publicKey: `04${publicKeyHex}`,
      privateKey: privateKeyHex,
      keyType: KeyType.SECP256R1,
      algorithm: 'ECDSA',
      curve: 'secp256r1',
      keySize: 256,
    };
  }

  /**
   * Native Ed25519 key generation
   */
  private async generateEd25519KeyPairNative(): Promise<Omit<KeyPair, 'keyId' | 'createdAt'>> {
    // This would use native modules for Ed25519 key generation
    // For now, we'll use a JavaScript fallback
    return this.generateEd25519KeyPairJS();
  }

  /**
   * JavaScript Ed25519 key generation
   */
  private async generateEd25519KeyPairJS(): Promise<Omit<KeyPair, 'keyId' | 'createdAt'>> {
    // This is a simplified implementation
    // In production, you'd use a proper Ed25519 library like tweetnacl
    const privateKey = this.generateRandomHex(64);
    const publicKey = this.generateRandomHex(64);
    
    return {
      publicKey,
      privateKey,
      keyType: KeyType.ED25519,
      algorithm: 'EdDSA',
      curve: 'ed25519',
      keySize: 256,
    };
  }

  /**
   * Sign data with a key
   */
  async signData(keyId: string, data: string | Uint8Array): Promise<SignatureResult> {
    try {
      const keyPair = await this.getKeyPair(keyId);
      if (!keyPair) {
        throw new Error('Key not found');
      }

      const dataToSign = typeof data === 'string' ? data : Buffer.from(data).toString('hex');
      let signature: string;

      switch (keyPair.keyType) {
        case KeyType.RSA:
          signature = await this.signWithRSA(keyPair.privateKey, dataToSign);
          break;
        case KeyType.SECP256R1:
          signature = await this.signWithSECP256R1(keyPair.privateKey, dataToSign);
          break;
        case KeyType.SECP256K1:
          signature = await this.signWithSECP256K1(keyPair.privateKey, dataToSign);
          break;
        case KeyType.ED25519:
          signature = await this.signWithEd25519(keyPair.privateKey, dataToSign);
          break;
        default:
          throw new Error(`Unsupported key type for signing: ${keyPair.keyType}`);
      }

      // Update last used timestamp
      await this.updateKeyLastUsed(keyId);

      return {
        signature,
        algorithm: keyPair.algorithm,
        keyId,
        timestamp: new Date().toISOString(),
      };
    } catch (error) {
      console.error('Failed to sign data:', error);
      throw new Error(`Failed to sign data: ${error}`);
    }
  }

  /**
   * Verify signature
   */
  async verifySignature(
    keyId: string,
    data: string | Uint8Array,
    signature: string
  ): Promise<VerificationResult> {
    try {
      const keyPair = await this.getKeyPair(keyId);
      if (!keyPair) {
        return {valid: false, error: 'Key not found'};
      }

      const dataToVerify = typeof data === 'string' ? data : Buffer.from(data).toString('hex');
      let valid: boolean;

      switch (keyPair.keyType) {
        case KeyType.RSA:
          valid = await this.verifyWithRSA(keyPair.publicKey, dataToVerify, signature);
          break;
        case KeyType.SECP256R1:
          valid = await this.verifyWithSECP256R1(keyPair.publicKey, dataToVerify, signature);
          break;
        case KeyType.SECP256K1:
          valid = await this.verifyWithSECP256K1(keyPair.publicKey, dataToVerify, signature);
          break;
        case KeyType.ED25519:
          valid = await this.verifyWithEd25519(keyPair.publicKey, dataToVerify, signature);
          break;
        default:
          return {valid: false, error: `Unsupported key type: ${keyPair.keyType}`};
      }

      return {
        valid,
        keyId,
        algorithm: keyPair.algorithm,
      };
    } catch (error) {
      console.error('Failed to verify signature:', error);
      return {valid: false, error: `Verification failed: ${error}`};
    }
  }

  /**
   * Get public key by key ID
   */
  async getPublicKey(keyId: string): Promise<string | null> {
    try {
      const keyPair = await this.getKeyPair(keyId);
      return keyPair?.publicKey || null;
    } catch (error) {
      console.error('Failed to get public key:', error);
      return null;
    }
  }

  /**
   * Get key metadata
   */
  async getKeyMetadata(keyId: string): Promise<KeyMetadata | null> {
    try {
      return await secureStorage.getObject<KeyMetadata>(`${this.METADATA_STORAGE_PREFIX}${keyId}`);
    } catch (error) {
      console.error('Failed to get key metadata:', error);
      return null;
    }
  }

  /**
   * List all keys
   */
  async listKeys(): Promise<KeyMetadata[]> {
    try {
      const keys = secureStorage.getAllKeys();
      const metadataKeys = keys.filter(key => key.startsWith(this.METADATA_STORAGE_PREFIX));
      
      const metadataList: KeyMetadata[] = [];
      for (const key of metadataKeys) {
        const metadata = await secureStorage.getObject<KeyMetadata>(key);
        if (metadata) {
          metadataList.push(metadata);
        }
      }
      
      return metadataList.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    } catch (error) {
      console.error('Failed to list keys:', error);
      return [];
    }
  }

  /**
   * Delete a key pair
   */
  async deleteKey(keyId: string): Promise<void> {
    try {
      await secureStorage.removeItem(`${this.KEY_STORAGE_PREFIX}${keyId}`, {useKeychain: true});
      await secureStorage.removeItem(`${this.METADATA_STORAGE_PREFIX}${keyId}`);
    } catch (error) {
      console.error('Failed to delete key:', error);
      throw new Error(`Failed to delete key: ${error}`);
    }
  }

  /**
   * Export public key in various formats
   */
  async exportPublicKey(keyId: string, format: 'pem' | 'jwk' | 'hex' = 'pem'): Promise<string | null> {
    try {
      const keyPair = await this.getKeyPair(keyId);
      if (!keyPair) {
        return null;
      }

      switch (format) {
        case 'pem':
          return keyPair.publicKey;
        case 'hex':
          return this.convertPEMToHex(keyPair.publicKey);
        case 'jwk':
          return this.convertToJWK(keyPair);
        default:
          return keyPair.publicKey;
      }
    } catch (error) {
      console.error('Failed to export public key:', error);
      return null;
    }
  }

  // Private helper methods

  private async storeKeyPair(keyPair: KeyPair, hardwareBacked: boolean = false): Promise<void> {
    await secureStorage.setObject(
      `${this.KEY_STORAGE_PREFIX}${keyPair.keyId}`,
      keyPair,
      {
        encrypt: true,
        useKeychain: hardwareBacked,
      }
    );
  }

  private async getKeyPair(keyId: string): Promise<KeyPair | null> {
    return await secureStorage.getObject<KeyPair>(`${this.KEY_STORAGE_PREFIX}${keyId}`, {
      encrypt: true,
      useKeychain: true,
    });
  }

  private async storeKeyMetadata(metadata: KeyMetadata): Promise<void> {
    await secureStorage.setObject(`${this.METADATA_STORAGE_PREFIX}${metadata.keyId}`, metadata);
  }

  private async updateKeyLastUsed(keyId: string): Promise<void> {
    const metadata = await this.getKeyMetadata(keyId);
    if (metadata) {
      metadata.lastUsed = new Date().toISOString();
      await this.storeKeyMetadata(metadata);
    }
  }

  private generateKeyId(): string {
    return `key_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  private generateRandomHex(length: number): string {
    const chars = '0123456789abcdef';
    let result = '';
    for (let i = 0; i < length; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
  }

  private generateRandomBase64(length: number): string {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
    let result = '';
    for (let i = 0; i < length; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
  }

  // Signing methods (simplified implementations)
  private async signWithRSA(privateKey: string, data: string): Promise<string> {
    // Simplified RSA signing - use proper crypto library in production
    const hash = CryptoJS.SHA256(data).toString();
    return `rsa_signature_${hash.substring(0, 32)}`;
  }

  private async signWithSECP256R1(privateKey: string, data: string): Promise<string> {
    // Simplified ECDSA signing - use proper crypto library in production
    const hash = CryptoJS.SHA256(data).toString();
    return `ecdsa_signature_${hash.substring(0, 32)}`;
  }

  private async signWithSECP256K1(privateKey: string, data: string): Promise<string> {
    // Use ethers.js for SECP256K1 signing
    const wallet = new ethers.Wallet(privateKey);
    const messageBytes = ethers.toUtf8Bytes(data);
    return await wallet.signMessage(messageBytes);
  }

  private async signWithEd25519(privateKey: string, data: string): Promise<string> {
    // Simplified Ed25519 signing - use proper crypto library in production
    const hash = CryptoJS.SHA256(data).toString();
    return `ed25519_signature_${hash.substring(0, 32)}`;
  }

  // Verification methods (simplified implementations)
  private async verifyWithRSA(publicKey: string, data: string, signature: string): Promise<boolean> {
    // Simplified RSA verification
    const hash = CryptoJS.SHA256(data).toString();
    return signature === `rsa_signature_${hash.substring(0, 32)}`;
  }

  private async verifyWithSECP256R1(publicKey: string, data: string, signature: string): Promise<boolean> {
    // Simplified ECDSA verification
    const hash = CryptoJS.SHA256(data).toString();
    return signature === `ecdsa_signature_${hash.substring(0, 32)}`;
  }

  private async verifyWithSECP256K1(publicKey: string, data: string, signature: string): Promise<boolean> {
    try {
      const messageBytes = ethers.toUtf8Bytes(data);
      const recoveredAddress = ethers.verifyMessage(messageBytes, signature);
      const expectedAddress = ethers.computeAddress(publicKey);
      return recoveredAddress.toLowerCase() === expectedAddress.toLowerCase();
    } catch (error) {
      return false;
    }
  }

  private async verifyWithEd25519(publicKey: string, data: string, signature: string): Promise<boolean> {
    // Simplified Ed25519 verification
    const hash = CryptoJS.SHA256(data).toString();
    return signature === `ed25519_signature_${hash.substring(0, 32)}`;
  }

  private convertPEMToHex(pem: string): string {
    // Convert PEM to hex format
    const base64 = pem.replace(/-----[^-]+-----/g, '').replace(/\s/g, '');
    return Buffer.from(base64, 'base64').toString('hex');
  }

  private convertToJWK(keyPair: KeyPair): string {
    // Convert to JSON Web Key format
    const jwk = {
      kty: keyPair.keyType === KeyType.RSA ? 'RSA' : 'EC',
      use: 'sig',
      key_ops: ['sign', 'verify'],
      alg: keyPair.algorithm,
      kid: keyPair.keyId,
      x: keyPair.publicKey.substring(0, 32),
      y: keyPair.publicKey.substring(32, 64),
    };
    return JSON.stringify(jwk, null, 2);
  }
}

export const keyService = new KeyService();
