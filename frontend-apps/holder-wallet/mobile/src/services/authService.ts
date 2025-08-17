/**
 * Authentication Service
 * 
 * Handles authentication with Keycloak and Web3 wallets
 */

import {apiClient} from './apiClient';
import {secureStorage} from './secureStorage';
import {biometricService} from './biometricService';

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface Web3LoginRequest {
  walletAddress: string;
  signature: string;
  message: string;
  chainId?: number;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
  user: {
    id: string;
    email: string;
    name: string;
    roles: string[];
    walletAddress?: string;
    did?: string;
  };
}

export interface Web3Challenge {
  challenge: string;
  nonce: string;
  expiresAt: string;
}

class AuthService {
  private readonly baseUrl = '/api/v1/auth';
  private refreshTokenPromise: Promise<string> | null = null;

  /**
   * Traditional email/password login
   */
  async login(credentials: LoginCredentials): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>(`${this.baseUrl}/login`, credentials);
    await this.storeTokens(response.data);
    return response.data;
  }

  /**
   * Generate Web3 authentication challenge
   */
  async generateWeb3Challenge(walletAddress: string): Promise<Web3Challenge> {
    const response = await apiClient.post<Web3Challenge>(`${this.baseUrl}/web3/challenge`, {
      walletAddress,
    });
    return response.data;
  }

  /**
   * Verify Web3 signature and authenticate
   */
  async verifyWeb3Signature(request: Web3LoginRequest): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>(`${this.baseUrl}/web3/verify`, request);
    await this.storeTokens(response.data);
    return response.data;
  }

  /**
   * Biometric authentication (local verification + token refresh)
   */
  async authenticateWithBiometric(): Promise<AuthResponse> {
    // First verify biometric locally
    const biometricResult = await biometricService.authenticate('Authenticate to access your wallet');
    
    if (!biometricResult.success) {
      throw new Error(biometricResult.error || 'Biometric authentication failed');
    }

    // Get stored refresh token
    const refreshToken = await secureStorage.getItem('refreshToken');
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    // Refresh access token
    return this.refreshAccessToken();
  }

  /**
   * PIN-based authentication
   */
  async authenticateWithPIN(pin: string): Promise<AuthResponse> {
    const storedPINHash = await secureStorage.getItem('pinHash');
    if (!storedPINHash) {
      throw new Error('No PIN configured');
    }

    // Verify PIN locally (you might want to use a more secure method)
    const pinHash = await this.hashPIN(pin);
    if (pinHash !== storedPINHash) {
      throw new Error('Invalid PIN');
    }

    // Refresh access token
    return this.refreshAccessToken();
  }

  /**
   * Refresh access token
   */
  async refreshAccessToken(): Promise<AuthResponse> {
    // Prevent multiple simultaneous refresh requests
    if (this.refreshTokenPromise) {
      const token = await this.refreshTokenPromise;
      return this.getCurrentUser();
    }

    this.refreshTokenPromise = this.performTokenRefresh();
    
    try {
      const token = await this.refreshTokenPromise;
      return this.getCurrentUser();
    } finally {
      this.refreshTokenPromise = null;
    }
  }

  private async performTokenRefresh(): Promise<string> {
    const refreshToken = await secureStorage.getItem('refreshToken');
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    const response = await apiClient.post<AuthResponse>(`${this.baseUrl}/refresh`, {
      refreshToken,
    });

    await this.storeTokens(response.data);
    return response.data.accessToken;
  }

  /**
   * Get current user information
   */
  async getCurrentUser(): Promise<AuthResponse> {
    const response = await apiClient.get<AuthResponse>(`${this.baseUrl}/me`);
    return response.data;
  }

  /**
   * Logout and clear tokens
   */
  async logout(): Promise<void> {
    try {
      const refreshToken = await secureStorage.getItem('refreshToken');
      if (refreshToken) {
        await apiClient.post(`${this.baseUrl}/logout`, {refreshToken});
      }
    } catch (error) {
      console.warn('Failed to logout on server:', error);
    } finally {
      await this.clearTokens();
    }
  }

  /**
   * Register new account
   */
  async register(userData: {
    email: string;
    password: string;
    name: string;
    acceptTerms: boolean;
  }): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>(`${this.baseUrl}/register`, userData);
    await this.storeTokens(response.data);
    return response.data;
  }

  /**
   * Request password reset
   */
  async requestPasswordReset(email: string): Promise<void> {
    await apiClient.post(`${this.baseUrl}/password-reset/request`, {email});
  }

  /**
   * Reset password with token
   */
  async resetPassword(token: string, newPassword: string): Promise<void> {
    await apiClient.post(`${this.baseUrl}/password-reset/confirm`, {
      token,
      newPassword,
    });
  }

  /**
   * Change password (authenticated user)
   */
  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    await apiClient.post(`${this.baseUrl}/password-change`, {
      currentPassword,
      newPassword,
    });
  }

  /**
   * Set up PIN for local authentication
   */
  async setupPIN(pin: string): Promise<void> {
    const pinHash = await this.hashPIN(pin);
    await secureStorage.setItem('pinHash', pinHash);
  }

  /**
   * Change PIN
   */
  async changePIN(currentPIN: string, newPIN: string): Promise<void> {
    // Verify current PIN
    await this.authenticateWithPIN(currentPIN);
    
    // Set new PIN
    await this.setupPIN(newPIN);
  }

  /**
   * Link Web3 wallet to account
   */
  async linkWeb3Wallet(walletAddress: string, signature: string, message: string): Promise<void> {
    await apiClient.post(`${this.baseUrl}/web3/link`, {
      walletAddress,
      signature,
      message,
    });
  }

  /**
   * Unlink Web3 wallet from account
   */
  async unlinkWeb3Wallet(walletAddress: string): Promise<void> {
    await apiClient.delete(`${this.baseUrl}/web3/unlink`, {
      data: {walletAddress},
    });
  }

  /**
   * Check if user is authenticated
   */
  async isAuthenticated(): Promise<boolean> {
    try {
      const accessToken = await secureStorage.getItem('accessToken');
      if (!accessToken) {
        return false;
      }

      // Check if token is expired
      const tokenPayload = this.parseJWT(accessToken);
      const now = Date.now() / 1000;
      
      if (tokenPayload.exp < now) {
        // Token expired, try to refresh
        try {
          await this.refreshAccessToken();
          return true;
        } catch (error) {
          return false;
        }
      }

      return true;
    } catch (error) {
      return false;
    }
  }

  /**
   * Get stored access token
   */
  async getAccessToken(): Promise<string | null> {
    return secureStorage.getItem('accessToken');
  }

  /**
   * Store authentication tokens securely
   */
  private async storeTokens(authData: AuthResponse): Promise<void> {
    await Promise.all([
      secureStorage.setItem('accessToken', authData.accessToken),
      secureStorage.setItem('refreshToken', authData.refreshToken),
      secureStorage.setItem('user', JSON.stringify(authData.user)),
    ]);
  }

  /**
   * Clear all stored tokens
   */
  private async clearTokens(): Promise<void> {
    await Promise.all([
      secureStorage.removeItem('accessToken'),
      secureStorage.removeItem('refreshToken'),
      secureStorage.removeItem('user'),
      secureStorage.removeItem('pinHash'),
    ]);
  }

  /**
   * Parse JWT token payload
   */
  private parseJWT(token: string): any {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      return JSON.parse(jsonPayload);
    } catch (error) {
      throw new Error('Invalid JWT token');
    }
  }

  /**
   * Hash PIN for secure storage
   */
  private async hashPIN(pin: string): Promise<string> {
    // In a real implementation, use a proper hashing library like bcrypt
    // This is a simplified example
    const encoder = new TextEncoder();
    const data = encoder.encode(pin + 'provenly-salt');
    const hashBuffer = await crypto.subtle.digest('SHA-256', data);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
  }
}

export const authService = new AuthService();
