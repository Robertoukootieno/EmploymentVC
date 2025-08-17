/**
 * Biometric Authentication Service
 * 
 * Handles biometric authentication (Face ID, Touch ID, Fingerprint) across iOS and Android
 */

import ReactNativeBiometrics, {BiometryTypes} from 'react-native-biometrics';
import {Platform, Alert} from 'react-native';
import {secureStorage} from './secureStorage';

export interface BiometricResult {
  success: boolean;
  error?: string;
  signature?: string;
  publicKey?: string;
}

export interface BiometricCapabilities {
  available: boolean;
  biometryType: BiometryTypes | null;
  error?: string;
}

class BiometricService {
  private rnBiometrics: ReactNativeBiometrics;

  constructor() {
    this.rnBiometrics = new ReactNativeBiometrics({
      allowDeviceCredentials: true, // Allow PIN/Pattern as fallback
    });
  }

  /**
   * Check if biometric authentication is available on the device
   */
  async isAvailable(): Promise<boolean> {
    try {
      const {available} = await this.rnBiometrics.isSensorAvailable();
      return available;
    } catch (error) {
      console.error('Error checking biometric availability:', error);
      return false;
    }
  }

  /**
   * Get detailed biometric capabilities
   */
  async getCapabilities(): Promise<BiometricCapabilities> {
    try {
      const {available, biometryType, error} = await this.rnBiometrics.isSensorAvailable();
      return {
        available,
        biometryType,
        error,
      };
    } catch (error: any) {
      return {
        available: false,
        biometryType: null,
        error: error.message || 'Unknown error',
      };
    }
  }

  /**
   * Get user-friendly biometric type name
   */
  async getBiometricTypeName(): Promise<string> {
    const capabilities = await this.getCapabilities();
    
    switch (capabilities.biometryType) {
      case BiometryTypes.FaceID:
        return 'Face ID';
      case BiometryTypes.TouchID:
        return 'Touch ID';
      case BiometryTypes.Biometrics:
        return Platform.OS === 'android' ? 'Fingerprint' : 'Biometrics';
      default:
        return 'Biometric Authentication';
    }
  }

  /**
   * Authenticate user with biometrics
   */
  async authenticate(promptMessage?: string): Promise<BiometricResult> {
    try {
      const capabilities = await this.getCapabilities();
      
      if (!capabilities.available) {
        return {
          success: false,
          error: 'Biometric authentication is not available on this device',
        };
      }

      const biometricTypeName = await this.getBiometricTypeName();
      const message = promptMessage || `Use ${biometricTypeName} to authenticate`;

      const {success, error} = await this.rnBiometrics.simplePrompt({
        promptMessage: message,
        cancelButtonText: 'Cancel',
      });

      if (success) {
        // Store successful authentication timestamp
        await secureStorage.setItem('lastBiometricAuth', Date.now().toString());
        
        return {
          success: true,
        };
      } else {
        return {
          success: false,
          error: error || 'Biometric authentication failed',
        };
      }
    } catch (error: any) {
      console.error('Biometric authentication error:', error);
      return {
        success: false,
        error: this.parseError(error),
      };
    }
  }

  /**
   * Authenticate with signature (for cryptographic operations)
   */
  async authenticateWithSignature(
    payload: string,
    promptMessage?: string
  ): Promise<BiometricResult> {
    try {
      const capabilities = await this.getCapabilities();
      
      if (!capabilities.available) {
        return {
          success: false,
          error: 'Biometric authentication is not available on this device',
        };
      }

      // Check if biometric key exists
      const {keysExist} = await this.rnBiometrics.biometricKeysExist();
      
      if (!keysExist) {
        // Create biometric key pair
        const keyResult = await this.createBiometricKey();
        if (!keyResult.success) {
          return keyResult;
        }
      }

      const biometricTypeName = await this.getBiometricTypeName();
      const message = promptMessage || `Use ${biometricTypeName} to sign the request`;

      const {success, signature, error} = await this.rnBiometrics.createSignature({
        promptMessage: message,
        payload,
        cancelButtonText: 'Cancel',
      });

      if (success && signature) {
        await secureStorage.setItem('lastBiometricAuth', Date.now().toString());
        
        return {
          success: true,
          signature,
        };
      } else {
        return {
          success: false,
          error: error || 'Biometric signature failed',
        };
      }
    } catch (error: any) {
      console.error('Biometric signature error:', error);
      return {
        success: false,
        error: this.parseError(error),
      };
    }
  }

  /**
   * Create biometric key pair for cryptographic operations
   */
  async createBiometricKey(): Promise<BiometricResult> {
    try {
      const {publicKey} = await this.rnBiometrics.createKeys();
      
      // Store public key securely
      await secureStorage.setItem('biometricPublicKey', publicKey);
      
      return {
        success: true,
        publicKey,
      };
    } catch (error: any) {
      console.error('Error creating biometric key:', error);
      return {
        success: false,
        error: this.parseError(error),
      };
    }
  }

  /**
   * Delete biometric keys
   */
  async deleteBiometricKeys(): Promise<BiometricResult> {
    try {
      const {keysDeleted} = await this.rnBiometrics.deleteKeys();
      
      if (keysDeleted) {
        // Remove stored public key
        await secureStorage.removeItem('biometricPublicKey');
        await secureStorage.removeItem('lastBiometricAuth');
        
        return {
          success: true,
        };
      } else {
        return {
          success: false,
          error: 'Failed to delete biometric keys',
        };
      }
    } catch (error: any) {
      console.error('Error deleting biometric keys:', error);
      return {
        success: false,
        error: this.parseError(error),
      };
    }
  }

  /**
   * Check if biometric keys exist
   */
  async biometricKeysExist(): Promise<boolean> {
    try {
      const {keysExist} = await this.rnBiometrics.biometricKeysExist();
      return keysExist;
    } catch (error) {
      console.error('Error checking biometric keys:', error);
      return false;
    }
  }

  /**
   * Get stored biometric public key
   */
  async getBiometricPublicKey(): Promise<string | null> {
    return secureStorage.getItem('biometricPublicKey');
  }

  /**
   * Check if user has recently authenticated with biometrics
   */
  async hasRecentAuthentication(maxAgeMinutes: number = 5): Promise<boolean> {
    try {
      const lastAuthStr = await secureStorage.getItem('lastBiometricAuth');
      if (!lastAuthStr) {
        return false;
      }

      const lastAuth = parseInt(lastAuthStr, 10);
      const now = Date.now();
      const maxAge = maxAgeMinutes * 60 * 1000; // Convert to milliseconds

      return (now - lastAuth) < maxAge;
    } catch (error) {
      console.error('Error checking recent authentication:', error);
      return false;
    }
  }

  /**
   * Enable biometric authentication for the app
   */
  async enableBiometricAuth(): Promise<BiometricResult> {
    try {
      const capabilities = await this.getCapabilities();
      
      if (!capabilities.available) {
        return {
          success: false,
          error: 'Biometric authentication is not available on this device',
        };
      }

      // Test authentication
      const authResult = await this.authenticate('Enable biometric authentication');
      
      if (authResult.success) {
        // Create keys for future cryptographic operations
        const keyResult = await this.createBiometricKey();
        
        if (keyResult.success) {
          await secureStorage.setItem('biometricEnabled', 'true');
          return {
            success: true,
            publicKey: keyResult.publicKey,
          };
        } else {
          return keyResult;
        }
      } else {
        return authResult;
      }
    } catch (error: any) {
      console.error('Error enabling biometric auth:', error);
      return {
        success: false,
        error: this.parseError(error),
      };
    }
  }

  /**
   * Disable biometric authentication for the app
   */
  async disableBiometricAuth(): Promise<BiometricResult> {
    try {
      // Delete biometric keys
      const deleteResult = await this.deleteBiometricKeys();
      
      if (deleteResult.success) {
        await secureStorage.removeItem('biometricEnabled');
        return {
          success: true,
        };
      } else {
        return deleteResult;
      }
    } catch (error: any) {
      console.error('Error disabling biometric auth:', error);
      return {
        success: false,
        error: this.parseError(error),
      };
    }
  }

  /**
   * Check if biometric authentication is enabled for the app
   */
  async isBiometricEnabled(): Promise<boolean> {
    try {
      const enabled = await secureStorage.getItem('biometricEnabled');
      return enabled === 'true';
    } catch (error) {
      console.error('Error checking biometric enabled status:', error);
      return false;
    }
  }

  /**
   * Show biometric setup prompt
   */
  async showSetupPrompt(): Promise<void> {
    const biometricTypeName = await this.getBiometricTypeName();
    
    Alert.alert(
      `Enable ${biometricTypeName}`,
      `Use ${biometricTypeName} to quickly and securely access your wallet.`,
      [
        {
          text: 'Not Now',
          style: 'cancel',
        },
        {
          text: 'Enable',
          onPress: async () => {
            const result = await this.enableBiometricAuth();
            if (!result.success) {
              Alert.alert(
                'Setup Failed',
                result.error || 'Failed to enable biometric authentication'
              );
            }
          },
        },
      ]
    );
  }

  /**
   * Parse and format error messages
   */
  private parseError(error: any): string {
    if (typeof error === 'string') {
      return error;
    }

    if (error?.message) {
      return error.message;
    }

    // Handle specific error codes
    if (error?.code) {
      switch (error.code) {
        case 'UserCancel':
          return 'Authentication was cancelled';
        case 'UserFallback':
          return 'User chose to use fallback authentication';
        case 'SystemCancel':
          return 'Authentication was cancelled by the system';
        case 'BiometryNotAvailable':
          return 'Biometric authentication is not available';
        case 'BiometryNotEnrolled':
          return 'No biometric credentials are enrolled';
        case 'BiometryLockout':
          return 'Biometric authentication is locked out';
        default:
          return `Authentication failed (${error.code})`;
      }
    }

    return 'Biometric authentication failed';
  }
}

export const biometricService = new BiometricService();
