/**
 * Wallet Service
 * 
 * Handles API communication for wallet operations
 */

import {apiClient} from './apiClient';
import {
  Wallet,
  WalletCreationRequest,
  CreateWalletResponse,
  WalletListResponse,
  WalletDetailsResponse,
} from '../types/wallet';

class WalletService {
  private readonly baseUrl = '/api/v1/wallets';

  /**
   * Create a new wallet (custodial or non-custodial)
   */
  async createWallet(request: WalletCreationRequest): Promise<CreateWalletResponse> {
    const endpoint = request.type === 'CUSTODIAL' 
      ? `${this.baseUrl}/custodial`
      : `${this.baseUrl}/non-custodial`;

    const response = await apiClient.post<CreateWalletResponse>(endpoint, request);
    return response.data;
  }

  /**
   * Get all wallets for the current user
   */
  async getWallets(params: {page?: number; size?: number} = {}): Promise<WalletListResponse> {
    const response = await apiClient.get<WalletListResponse>(this.baseUrl, {
      params: {
        page: params.page || 0,
        size: params.size || 20,
      },
    });
    return response.data;
  }

  /**
   * Get detailed information about a specific wallet
   */
  async getWalletDetails(walletId: string): Promise<WalletDetailsResponse> {
    const response = await apiClient.get<WalletDetailsResponse>(`${this.baseUrl}/${walletId}`);
    return response.data;
  }

  /**
   * Update wallet metadata and configuration
   */
  async updateWallet(walletId: string, updates: Partial<Wallet>): Promise<Wallet> {
    const response = await apiClient.put<Wallet>(`${this.baseUrl}/${walletId}`, updates);
    return response.data;
  }

  /**
   * Delete a wallet
   */
  async deleteWallet(walletId: string): Promise<void> {
    await apiClient.delete(`${this.baseUrl}/${walletId}`);
  }

  /**
   * Set the active wallet for the user
   */
  async setActiveWallet(walletId: string): Promise<Wallet> {
    const response = await apiClient.post<Wallet>(`${this.baseUrl}/${walletId}/activate`);
    return response.data;
  }

  /**
   * Lock a wallet (requires re-authentication to unlock)
   */
  async lockWallet(walletId: string): Promise<void> {
    await apiClient.post(`${this.baseUrl}/${walletId}/lock`);
  }

  /**
   * Unlock a wallet with authentication
   */
  async unlockWallet(walletId: string, authCredential: string): Promise<void> {
    await apiClient.post(`${this.baseUrl}/${walletId}/unlock`, {
      authCredential,
    });
  }

  /**
   * Create a backup of a custodial wallet
   */
  async createBackup(walletId: string): Promise<{backupData: string; checksum: string}> {
    const response = await apiClient.post<{backupData: string; checksum: string}>(
      `${this.baseUrl}/custodial/${walletId}/backup`
    );
    return response.data;
  }

  /**
   * Restore a wallet from backup
   */
  async restoreFromBackup(backupData: string, authCredential: string): Promise<Wallet> {
    const response = await apiClient.post<Wallet>(`${this.baseUrl}/restore`, {
      backupData,
      authCredential,
    });
    return response.data;
  }

  /**
   * Get wallet statistics
   */
  async getWalletStats(walletId: string): Promise<any> {
    const response = await apiClient.get(`${this.baseUrl}/${walletId}/stats`);
    return response.data;
  }

  /**
   * Get wallet activity history
   */
  async getWalletActivity(
    walletId: string,
    params: {page?: number; size?: number; type?: string} = {}
  ): Promise<any> {
    const response = await apiClient.get(`${this.baseUrl}/${walletId}/activity`, {
      params,
    });
    return response.data;
  }

  /**
   * Update wallet security settings
   */
  async updateSecuritySettings(
    walletId: string,
    settings: {
      biometricEnabled?: boolean;
      pinRequired?: boolean;
      sessionTimeout?: number;
    }
  ): Promise<void> {
    await apiClient.put(`${this.baseUrl}/${walletId}/security`, settings);
  }

  /**
   * Verify wallet ownership (for non-custodial wallets)
   */
  async verifyOwnership(walletId: string, proof: any): Promise<{verified: boolean}> {
    const response = await apiClient.post<{verified: boolean}>(
      `${this.baseUrl}/non-custodial/${walletId}/verify-ownership`,
      {proof}
    );
    return response.data;
  }

  /**
   * Sync non-custodial wallet metadata
   */
  async syncWallet(walletId: string): Promise<void> {
    await apiClient.post(`${this.baseUrl}/non-custodial/${walletId}/sync`);
  }

  /**
   * Export wallet data (for migration or backup)
   */
  async exportWallet(walletId: string, format: 'json' | 'csv' = 'json'): Promise<any> {
    const response = await apiClient.get(`${this.baseUrl}/${walletId}/export`, {
      params: {format},
    });
    return response.data;
  }

  /**
   * Import wallet data
   */
  async importWallet(data: any, type: 'backup' | 'migration'): Promise<Wallet> {
    const response = await apiClient.post<Wallet>(`${this.baseUrl}/import`, {
      data,
      type,
    });
    return response.data;
  }
}

export const walletService = new WalletService();
