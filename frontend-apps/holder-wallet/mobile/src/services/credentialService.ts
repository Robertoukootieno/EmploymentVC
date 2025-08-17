/**
 * Credential Service
 * 
 * Handles credential storage, retrieval, and presentation operations
 */

import {apiClient} from './apiClient';
import {
  VerifiableCredential,
  CredentialPresentation,
  StoredCredential,
  CredentialMetadata,
  StoreCredentialRequest,
  StoreCredentialResponse,
  GetCredentialRequest,
  GetCredentialResponse,
  ListCredentialsRequest,
  ListCredentialsResponse,
  CreatePresentationRequest,
  CreatePresentationResponse,
  VerifyCredentialRequest,
  VerifyCredentialResponse,
  PresentationRequest,
  SelectiveDisclosure,
} from '../types/credential';

class CredentialService {
  private readonly baseUrl = '/api/v1';

  /**
   * Store a credential in the wallet
   */
  async storeCredential(request: StoreCredentialRequest): Promise<StoreCredentialResponse> {
    const response = await apiClient.post<StoreCredentialResponse>(
      `${this.baseUrl}/wallets/${request.walletId}/credentials`,
      {
        credential: request.credential,
        metadata: request.metadata,
      }
    );
    return response.data;
  }

  /**
   * Get a specific credential from the wallet
   */
  async getCredential(request: GetCredentialRequest): Promise<GetCredentialResponse> {
    const response = await apiClient.get<GetCredentialResponse>(
      `${this.baseUrl}/wallets/${request.walletId}/credentials/${request.credentialId}`
    );
    return response.data;
  }

  /**
   * List all credentials in a wallet
   */
  async listCredentials(request: ListCredentialsRequest): Promise<ListCredentialsResponse> {
    const params = new URLSearchParams();
    
    if (request.type) params.append('type', request.type);
    if (request.status) params.append('status', request.status);
    if (request.page !== undefined) params.append('page', request.page.toString());
    if (request.size !== undefined) params.append('size', request.size.toString());
    if (request.search) params.append('search', request.search);
    if (request.tags) request.tags.forEach(tag => params.append('tags', tag));

    const response = await apiClient.get<ListCredentialsResponse>(
      `${this.baseUrl}/wallets/${request.walletId}/credentials?${params.toString()}`
    );
    return response.data;
  }

  /**
   * Delete a credential from the wallet
   */
  async deleteCredential(walletId: string, credentialId: string): Promise<void> {
    await apiClient.delete(
      `${this.baseUrl}/wallets/${walletId}/credentials/${credentialId}`
    );
  }

  /**
   * Update credential metadata
   */
  async updateCredentialMetadata(
    walletId: string,
    credentialId: string,
    metadata: Partial<CredentialMetadata>
  ): Promise<CredentialMetadata> {
    const response = await apiClient.patch<CredentialMetadata>(
      `${this.baseUrl}/wallets/${walletId}/credentials/${credentialId}/metadata`,
      metadata
    );
    return response.data;
  }

  /**
   * Create a verifiable presentation
   */
  async createPresentation(request: CreatePresentationRequest): Promise<CreatePresentationResponse> {
    const response = await apiClient.post<CreatePresentationResponse>(
      `${this.baseUrl}/wallets/${request.walletId}/presentations`,
      {
        credentialIds: request.credentialIds,
        challenge: request.challenge,
        domain: request.domain,
        selectiveDisclosure: request.selectiveDisclosure,
      }
    );
    return response.data;
  }

  /**
   * Verify a credential
   */
  async verifyCredential(request: VerifyCredentialRequest): Promise<VerifyCredentialResponse> {
    const response = await apiClient.post<VerifyCredentialResponse>(
      `${this.baseUrl}/verify/credential`,
      {
        credential: request.credential,
        challenge: request.challenge,
        domain: request.domain,
      }
    );
    return response.data;
  }

  /**
   * Verify a presentation
   */
  async verifyPresentation(
    presentation: CredentialPresentation,
    challenge?: string,
    domain?: string
  ): Promise<VerifyCredentialResponse> {
    const response = await apiClient.post<VerifyCredentialResponse>(
      `${this.baseUrl}/verify/presentation`,
      {
        presentation,
        challenge,
        domain,
      }
    );
    return response.data;
  }

  /**
   * Get presentation request details
   */
  async getPresentationRequest(requestId: string): Promise<PresentationRequest> {
    const response = await apiClient.get<PresentationRequest>(
      `${this.baseUrl}/presentation-requests/${requestId}`
    );
    return response.data;
  }

  /**
   * Submit a presentation response
   */
  async submitPresentation(
    requestId: string,
    presentation: CredentialPresentation,
    selectiveDisclosure?: SelectiveDisclosure[]
  ): Promise<void> {
    await apiClient.post(
      `${this.baseUrl}/presentation-requests/${requestId}/submit`,
      {
        presentation,
        selectiveDisclosure,
      }
    );
  }

  /**
   * Get credential schema
   */
  async getCredentialSchema(schemaId: string): Promise<any> {
    const response = await apiClient.get(`${this.baseUrl}/schemas/${schemaId}`);
    return response.data;
  }

  /**
   * Validate credential against schema
   */
  async validateCredential(credential: VerifiableCredential, schemaId?: string): Promise<{
    valid: boolean;
    errors: string[];
  }> {
    const response = await apiClient.post<{valid: boolean; errors: string[]}>(
      `${this.baseUrl}/validate/credential`,
      {
        credential,
        schemaId,
      }
    );
    return response.data;
  }

  /**
   * Get credential status (revocation check)
   */
  async getCredentialStatus(credentialId: string): Promise<{
    status: 'active' | 'revoked' | 'suspended';
    statusReason?: string;
    lastChecked: string;
  }> {
    const response = await apiClient.get(
      `${this.baseUrl}/credentials/${credentialId}/status`
    );
    return response.data;
  }

  /**
   * Refresh credential status for all credentials in wallet
   */
  async refreshCredentialStatuses(walletId: string): Promise<void> {
    await apiClient.post(`${this.baseUrl}/wallets/${walletId}/credentials/refresh-status`);
  }

  /**
   * Export credentials from wallet
   */
  async exportCredentials(
    walletId: string,
    format: 'json' | 'csv' = 'json',
    credentialIds?: string[]
  ): Promise<Blob> {
    const params = new URLSearchParams({format});
    if (credentialIds) {
      credentialIds.forEach(id => params.append('credentialIds', id));
    }

    const response = await apiClient.download(
      `${this.baseUrl}/wallets/${walletId}/credentials/export?${params.toString()}`
    );
    return response.data;
  }

  /**
   * Import credentials to wallet
   */
  async importCredentials(
    walletId: string,
    file: File | FormData,
    format: 'json' | 'csv' = 'json'
  ): Promise<{
    imported: number;
    failed: number;
    errors: string[];
  }> {
    const formData = file instanceof FormData ? file : new FormData();
    if (!(file instanceof FormData)) {
      formData.append('file', file);
    }
    formData.append('format', format);

    const response = await apiClient.upload<{
      imported: number;
      failed: number;
      errors: string[];
    }>(`${this.baseUrl}/wallets/${walletId}/credentials/import`, formData);
    
    return response.data;
  }

  /**
   * Search credentials across all wallets
   */
  async searchCredentials(query: string, filters?: {
    type?: string;
    issuer?: string;
    dateRange?: {from: string; to: string};
  }): Promise<StoredCredential[]> {
    const params = new URLSearchParams({query});
    
    if (filters?.type) params.append('type', filters.type);
    if (filters?.issuer) params.append('issuer', filters.issuer);
    if (filters?.dateRange) {
      params.append('dateFrom', filters.dateRange.from);
      params.append('dateTo', filters.dateRange.to);
    }

    const response = await apiClient.get<{credentials: StoredCredential[]}>(
      `${this.baseUrl}/credentials/search?${params.toString()}`
    );
    return response.data.credentials;
  }

  /**
   * Get credential analytics
   */
  async getCredentialAnalytics(walletId: string, period: 'week' | 'month' | 'year' = 'month'): Promise<{
    totalCredentials: number;
    activeCredentials: number;
    expiredCredentials: number;
    revokedCredentials: number;
    recentActivity: Array<{
      date: string;
      action: string;
      credentialId: string;
      credentialTitle: string;
    }>;
    credentialsByType: Record<string, number>;
    credentialsByIssuer: Record<string, number>;
  }> {
    const response = await apiClient.get(
      `${this.baseUrl}/wallets/${walletId}/analytics?period=${period}`
    );
    return response.data;
  }

  /**
   * Share credential via QR code or link
   */
  async shareCredential(
    walletId: string,
    credentialId: string,
    method: 'qr' | 'link',
    options?: {
      expiresIn?: number; // seconds
      selectiveDisclosure?: string[];
      purpose?: string;
    }
  ): Promise<{
    shareUrl: string;
    qrCode?: string;
    expiresAt: string;
  }> {
    const response = await apiClient.post(
      `${this.baseUrl}/wallets/${walletId}/credentials/${credentialId}/share`,
      {
        method,
        ...options,
      }
    );
    return response.data;
  }

  /**
   * Revoke a shared credential link
   */
  async revokeSharedCredential(shareId: string): Promise<void> {
    await apiClient.delete(`${this.baseUrl}/shared-credentials/${shareId}`);
  }
}

export const credentialService = new CredentialService();
