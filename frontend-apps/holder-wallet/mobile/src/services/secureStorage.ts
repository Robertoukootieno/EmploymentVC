/**
 * Secure Storage Service
 * 
 * Provides secure storage for sensitive data using platform-specific secure storage
 */

import {MMKV} from 'react-native-mmkv';
import Keychain from 'react-native-keychain';
import {Platform} from 'react-native';
import CryptoJS from 'react-native-crypto-js';

export interface SecureStorageOptions {
  encrypt?: boolean;
  useKeychain?: boolean;
  accessGroup?: string; // iOS only
  service?: string;
}

class SecureStorage {
  private mmkv: MMKV;
  private encryptionKey: string;

  constructor() {
    // Initialize MMKV with encryption
    this.mmkv = new MMKV({
      id: 'provenly-secure-storage',
      encryptionKey: 'provenly-encryption-key-2024',
    });
    
    this.encryptionKey = 'provenly-aes-key-2024';
  }

  /**
   * Store a value securely
   */
  async setItem(
    key: string,
    value: string,
    options: SecureStorageOptions = {}
  ): Promise<void> {
    try {
      const {
        encrypt = false,
        useKeychain = false,
        accessGroup,
        service = 'ProvenlyWallet',
      } = options;

      let finalValue = value;

      // Encrypt if requested
      if (encrypt) {
        finalValue = this.encrypt(value);
      }

      if (useKeychain) {
        // Use platform keychain for highly sensitive data
        await this.setKeychainItem(key, finalValue, {accessGroup, service});
      } else {
        // Use MMKV for regular secure storage
        this.mmkv.set(key, finalValue);
      }
    } catch (error) {
      console.error(`Failed to store item ${key}:`, error);
      throw new Error(`Failed to store secure item: ${key}`);
    }
  }

  /**
   * Retrieve a value securely
   */
  async getItem(
    key: string,
    options: SecureStorageOptions = {}
  ): Promise<string | null> {
    try {
      const {
        encrypt = false,
        useKeychain = false,
        accessGroup,
        service = 'ProvenlyWallet',
      } = options;

      let value: string | null = null;

      if (useKeychain) {
        // Get from platform keychain
        value = await this.getKeychainItem(key, {accessGroup, service});
      } else {
        // Get from MMKV
        value = this.mmkv.getString(key) || null;
      }

      if (value && encrypt) {
        // Decrypt if needed
        value = this.decrypt(value);
      }

      return value;
    } catch (error) {
      console.error(`Failed to retrieve item ${key}:`, error);
      return null;
    }
  }

  /**
   * Remove a value securely
   */
  async removeItem(
    key: string,
    options: SecureStorageOptions = {}
  ): Promise<void> {
    try {
      const {
        useKeychain = false,
        accessGroup,
        service = 'ProvenlyWallet',
      } = options;

      if (useKeychain) {
        // Remove from platform keychain
        await this.removeKeychainItem(key, {accessGroup, service});
      } else {
        // Remove from MMKV
        this.mmkv.delete(key);
      }
    } catch (error) {
      console.error(`Failed to remove item ${key}:`, error);
      throw new Error(`Failed to remove secure item: ${key}`);
    }
  }

  /**
   * Check if a key exists
   */
  async hasItem(
    key: string,
    options: SecureStorageOptions = {}
  ): Promise<boolean> {
    try {
      const {useKeychain = false} = options;

      if (useKeychain) {
        const value = await this.getKeychainItem(key, options);
        return value !== null;
      } else {
        return this.mmkv.contains(key);
      }
    } catch (error) {
      console.error(`Failed to check item ${key}:`, error);
      return false;
    }
  }

  /**
   * Get all keys
   */
  getAllKeys(useKeychain: boolean = false): string[] {
    if (useKeychain) {
      // Keychain doesn't support listing all keys
      return [];
    } else {
      return this.mmkv.getAllKeys();
    }
  }

  /**
   * Clear all data
   */
  async clearAll(options: SecureStorageOptions = {}): Promise<void> {
    try {
      const {useKeychain = false} = options;

      if (useKeychain) {
        // Clear keychain items (this is limited)
        await Keychain.resetInternetCredentials(options.service || 'ProvenlyWallet');
      } else {
        // Clear MMKV
        this.mmkv.clearAll();
      }
    } catch (error) {
      console.error('Failed to clear all items:', error);
      throw new Error('Failed to clear secure storage');
    }
  }

  /**
   * Store object as JSON
   */
  async setObject(
    key: string,
    object: any,
    options: SecureStorageOptions = {}
  ): Promise<void> {
    const jsonString = JSON.stringify(object);
    await this.setItem(key, jsonString, options);
  }

  /**
   * Retrieve object from JSON
   */
  async getObject<T = any>(
    key: string,
    options: SecureStorageOptions = {}
  ): Promise<T | null> {
    const jsonString = await this.getItem(key, options);
    if (!jsonString) {
      return null;
    }

    try {
      return JSON.parse(jsonString) as T;
    } catch (error) {
      console.error(`Failed to parse JSON for key ${key}:`, error);
      return null;
    }
  }

  /**
   * Store credentials in keychain
   */
  private async setKeychainItem(
    key: string,
    value: string,
    options: {accessGroup?: string; service?: string} = {}
  ): Promise<void> {
    const {accessGroup, service = 'ProvenlyWallet'} = options;

    const keychainOptions: Keychain.Options = {
      service,
      accessGroup: Platform.OS === 'ios' ? accessGroup : undefined,
      accessControl: Keychain.ACCESS_CONTROL.BIOMETRY_ANY_OR_DEVICE_PASSCODE,
      authenticatePrompt: 'Authenticate to access secure data',
    };

    await Keychain.setInternetCredentials(key, key, value, keychainOptions);
  }

  /**
   * Retrieve credentials from keychain
   */
  private async getKeychainItem(
    key: string,
    options: {accessGroup?: string; service?: string} = {}
  ): Promise<string | null> {
    const {accessGroup, service = 'ProvenlyWallet'} = options;

    try {
      const keychainOptions: Keychain.Options = {
        service,
        accessGroup: Platform.OS === 'ios' ? accessGroup : undefined,
        authenticatePrompt: 'Authenticate to access secure data',
      };

      const credentials = await Keychain.getInternetCredentials(key, keychainOptions);
      
      if (credentials && credentials.password) {
        return credentials.password;
      }
      
      return null;
    } catch (error: any) {
      if (error.message === 'UserCancel') {
        // User cancelled authentication
        return null;
      }
      throw error;
    }
  }

  /**
   * Remove credentials from keychain
   */
  private async removeKeychainItem(
    key: string,
    options: {accessGroup?: string; service?: string} = {}
  ): Promise<void> {
    const {service = 'ProvenlyWallet'} = options;
    await Keychain.resetInternetCredentials(key);
  }

  /**
   * Encrypt data using AES
   */
  private encrypt(data: string): string {
    try {
      const encrypted = CryptoJS.AES.encrypt(data, this.encryptionKey).toString();
      return encrypted;
    } catch (error) {
      console.error('Encryption failed:', error);
      throw new Error('Failed to encrypt data');
    }
  }

  /**
   * Decrypt data using AES
   */
  private decrypt(encryptedData: string): string {
    try {
      const decrypted = CryptoJS.AES.decrypt(encryptedData, this.encryptionKey);
      const decryptedString = decrypted.toString(CryptoJS.enc.Utf8);
      
      if (!decryptedString) {
        throw new Error('Decryption resulted in empty string');
      }
      
      return decryptedString;
    } catch (error) {
      console.error('Decryption failed:', error);
      throw new Error('Failed to decrypt data');
    }
  }

  /**
   * Generate a secure random key
   */
  generateSecureKey(length: number = 32): string {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    
    for (let i = 0; i < length; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    
    return result;
  }

  /**
   * Check storage health
   */
  async checkHealth(): Promise<{
    mmkvWorking: boolean;
    keychainWorking: boolean;
    encryptionWorking: boolean;
  }> {
    const testKey = 'health-check-test';
    const testValue = 'test-value-' + Date.now();
    
    let mmkvWorking = false;
    let keychainWorking = false;
    let encryptionWorking = false;

    try {
      // Test MMKV
      this.mmkv.set(testKey, testValue);
      const mmkvResult = this.mmkv.getString(testKey);
      mmkvWorking = mmkvResult === testValue;
      this.mmkv.delete(testKey);
    } catch (error) {
      console.error('MMKV health check failed:', error);
    }

    try {
      // Test Keychain
      await this.setKeychainItem(testKey, testValue);
      const keychainResult = await this.getKeychainItem(testKey);
      keychainWorking = keychainResult === testValue;
      await this.removeKeychainItem(testKey);
    } catch (error) {
      console.error('Keychain health check failed:', error);
    }

    try {
      // Test Encryption
      const encrypted = this.encrypt(testValue);
      const decrypted = this.decrypt(encrypted);
      encryptionWorking = decrypted === testValue;
    } catch (error) {
      console.error('Encryption health check failed:', error);
    }

    return {
      mmkvWorking,
      keychainWorking,
      encryptionWorking,
    };
  }
}

// Export singleton instance
export const secureStorage = new SecureStorage();
