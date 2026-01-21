/**
 * Presentation Service
 * 
 * Handles creation and management of verifiable presentations with selective disclosure
 */

import {
  VerifiableCredential,
  CredentialPresentation,
  PresentationRequest,
  SelectiveDisclosure,
  StoredCredential,
} from '../types/credential';
import {credentialService} from './credentialService';
import {qrCodeService} from './qrCodeService';
import {secureStorage} from './secureStorage';

export interface PresentationTemplate {
  id: string;
  name: string;
  description: string;
  requiredCredentialTypes: string[];
  selectiveDisclosureFields: string[];
  purpose: string;
  createdAt: string;
}

export interface PresentationHistory {
  id: string;
  presentationId: string;
  verifierName: string;
  verifierDid: string;
  purpose: string;
  credentialsUsed: string[];
  selectiveDisclosure: SelectiveDisclosure[];
  createdAt: string;
  status: 'pending' | 'submitted' | 'verified' | 'rejected';
}

class PresentationService {
  private readonly TEMPLATES_KEY = 'presentation_templates';
  private readonly HISTORY_KEY = 'presentation_history';

  /**
   * Create a verifiable presentation from selected credentials
   */
  async createPresentation(
    walletId: string,
    credentialIds: string[],
    challenge: string,
    domain?: string,
    selectiveDisclosure?: SelectiveDisclosure[]
  ): Promise<CredentialPresentation> {
    try {
      // Get credentials from wallet
      const credentials: VerifiableCredential[] = [];
      
      for (const credentialId of credentialIds) {
        const credentialResponse = await credentialService.getCredential({
          walletId,
          credentialId,
        });
        credentials.push(credentialResponse.credential);
      }

      // Apply selective disclosure if specified
      const processedCredentials = selectiveDisclosure 
        ? this.applySelectiveDisclosure(credentials, selectiveDisclosure)
        : credentials;

      // Create presentation
      const presentation = await credentialService.createPresentation({
        walletId,
        credentialIds,
        challenge,
        domain,
        selectiveDisclosure,
      });

      // Save to history
      await this.savePresentationToHistory({
        id: this.generateId(),
        presentationId: presentation.presentation.id,
        verifierName: 'Unknown Verifier',
        verifierDid: domain || 'unknown',
        purpose: 'Credential Verification',
        credentialsUsed: credentialIds,
        selectiveDisclosure: selectiveDisclosure || [],
        createdAt: new Date().toISOString(),
        status: 'pending',
      });

      return presentation.presentation;
    } catch (error) {
      console.error('Failed to create presentation:', error);
      throw new Error('Failed to create verifiable presentation');
    }
  }

  /**
   * Create presentation from QR code request
   */
  async createPresentationFromRequest(
    walletId: string,
    request: PresentationRequest,
    selectedCredentials: StoredCredential[],
    selectiveDisclosureChoices?: Record<string, string[]>
  ): Promise<CredentialPresentation> {
    try {
      // Build selective disclosure array
      const selectiveDisclosure: SelectiveDisclosure[] = [];
      
      if (selectiveDisclosureChoices) {
        selectedCredentials.forEach(credential => {
          const disclosedFields = selectiveDisclosureChoices[credential.metadata.credentialId];
          if (disclosedFields && disclosedFields.length > 0) {
            const allFields = this.extractCredentialFields(credential.credential);
            const hiddenFields = allFields.filter(field => !disclosedFields.includes(field));
            
            selectiveDisclosure.push({
              credentialId: credential.metadata.credentialId,
              disclosedFields,
              hiddenFields,
              purpose: request.purpose,
              requestedBy: request.verifier.id,
            });
          }
        });
      }

      const credentialIds = selectedCredentials.map(c => c.metadata.credentialId);

      const presentation = await this.createPresentation(
        walletId,
        credentialIds,
        request.challenge,
        request.domain,
        selectiveDisclosure
      );

      // Update history with verifier information
      const historyEntries = await this.getPresentationHistory();
      const latestEntry = historyEntries[0];
      if (latestEntry) {
        latestEntry.verifierName = request.verifier.name;
        latestEntry.verifierDid = request.verifier.id;
        latestEntry.purpose = request.purpose;
        await this.updatePresentationHistory(latestEntry);
      }

      return presentation;
    } catch (error) {
      console.error('Failed to create presentation from request:', error);
      throw new Error('Failed to create presentation from request');
    }
  }

  /**
   * Apply selective disclosure to credentials
   */
  private applySelectiveDisclosure(
    credentials: VerifiableCredential[],
    selectiveDisclosure: SelectiveDisclosure[]
  ): VerifiableCredential[] {
    return credentials.map(credential => {
      const disclosure = selectiveDisclosure.find(
        sd => sd.credentialId === credential.id
      );

      if (!disclosure) {
        return credential;
      }

      // Create a copy of the credential
      const processedCredential = JSON.parse(JSON.stringify(credential));

      // Remove hidden fields from credential subject
      disclosure.hiddenFields.forEach(field => {
        if (processedCredential.credentialSubject[field]) {
          delete processedCredential.credentialSubject[field];
        }
      });

      return processedCredential;
    });
  }

  /**
   * Extract all available fields from a credential
   */
  private extractCredentialFields(credential: VerifiableCredential): string[] {
    const fields: string[] = [];
    
    if (credential.credentialSubject) {
      Object.keys(credential.credentialSubject).forEach(key => {
        if (key !== 'id') { // Exclude the subject ID
          fields.push(key);
        }
      });
    }

    return fields;
  }

  /**
   * Get available fields for selective disclosure
   */
  getSelectiveDisclosureFields(credential: VerifiableCredential): Array<{
    field: string;
    label: string;
    value: any;
    required: boolean;
  }> {
    const fields: Array<{field: string; label: string; value: any; required: boolean}> = [];
    
    if (credential.credentialSubject) {
      Object.entries(credential.credentialSubject).forEach(([key, value]) => {
        if (key !== 'id') {
          fields.push({
            field: key,
            label: this.formatFieldLabel(key),
            value,
            required: this.isRequiredField(key),
          });
        }
      });
    }

    return fields;
  }

  /**
   * Format field name for display
   */
  private formatFieldLabel(fieldName: string): string {
    return fieldName
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, str => str.toUpperCase())
      .trim();
  }

  /**
   * Check if a field is required (cannot be hidden)
   */
  private isRequiredField(fieldName: string): boolean {
    const requiredFields = ['id', 'type', 'name', 'email'];
    return requiredFields.includes(fieldName.toLowerCase());
  }

  /**
   * Generate QR code for presentation
   */
  async generatePresentationQR(presentation: CredentialPresentation): Promise<string> {
    return qrCodeService.generateVerificationQR(presentation);
  }

  /**
   * Save presentation template
   */
  async savePresentationTemplate(template: PresentationTemplate): Promise<void> {
    try {
      const templates = await this.getPresentationTemplates();
      const existingIndex = templates.findIndex(t => t.id === template.id);
      
      if (existingIndex >= 0) {
        templates[existingIndex] = template;
      } else {
        templates.push(template);
      }

      await secureStorage.setObject(this.TEMPLATES_KEY, templates);
    } catch (error) {
      console.error('Failed to save presentation template:', error);
      throw new Error('Failed to save presentation template');
    }
  }

  /**
   * Get all presentation templates
   */
  async getPresentationTemplates(): Promise<PresentationTemplate[]> {
    try {
      const templates = await secureStorage.getObject<PresentationTemplate[]>(this.TEMPLATES_KEY);
      return templates || [];
    } catch (error) {
      console.error('Failed to get presentation templates:', error);
      return [];
    }
  }

  /**
   * Delete presentation template
   */
  async deletePresentationTemplate(templateId: string): Promise<void> {
    try {
      const templates = await this.getPresentationTemplates();
      const filteredTemplates = templates.filter(t => t.id !== templateId);
      await secureStorage.setObject(this.TEMPLATES_KEY, filteredTemplates);
    } catch (error) {
      console.error('Failed to delete presentation template:', error);
      throw new Error('Failed to delete presentation template');
    }
  }

  /**
   * Save presentation to history
   */
  async savePresentationToHistory(historyEntry: PresentationHistory): Promise<void> {
    try {
      const history = await this.getPresentationHistory();
      history.unshift(historyEntry); // Add to beginning
      
      // Keep only last 100 entries
      const trimmedHistory = history.slice(0, 100);
      
      await secureStorage.setObject(this.HISTORY_KEY, trimmedHistory);
    } catch (error) {
      console.error('Failed to save presentation history:', error);
      throw new Error('Failed to save presentation history');
    }
  }

  /**
   * Get presentation history
   */
  async getPresentationHistory(): Promise<PresentationHistory[]> {
    try {
      const history = await secureStorage.getObject<PresentationHistory[]>(this.HISTORY_KEY);
      return history || [];
    } catch (error) {
      console.error('Failed to get presentation history:', error);
      return [];
    }
  }

  /**
   * Update presentation history entry
   */
  async updatePresentationHistory(updatedEntry: PresentationHistory): Promise<void> {
    try {
      const history = await this.getPresentationHistory();
      const index = history.findIndex(entry => entry.id === updatedEntry.id);
      
      if (index >= 0) {
        history[index] = updatedEntry;
        await secureStorage.setObject(this.HISTORY_KEY, history);
      }
    } catch (error) {
      console.error('Failed to update presentation history:', error);
      throw new Error('Failed to update presentation history');
    }
  }

  /**
   * Clear presentation history
   */
  async clearPresentationHistory(): Promise<void> {
    try {
      await secureStorage.removeItem(this.HISTORY_KEY);
    } catch (error) {
      console.error('Failed to clear presentation history:', error);
      throw new Error('Failed to clear presentation history');
    }
  }

  /**
   * Validate presentation request
   */
  validatePresentationRequest(request: PresentationRequest): {
    valid: boolean;
    errors: string[];
  } {
    const errors: string[] = [];

    if (!request.id) {
      errors.push('Request ID is required');
    }

    if (!request.verifier?.id) {
      errors.push('Verifier ID is required');
    }

    if (!request.challenge) {
      errors.push('Challenge is required');
    }

    if (!request.requestedCredentials || request.requestedCredentials.length === 0) {
      errors.push('At least one credential type must be requested');
    }

    if (request.expiresAt) {
      const expirationDate = new Date(request.expiresAt);
      if (expirationDate < new Date()) {
        errors.push('Presentation request has expired');
      }
    }

    return {
      valid: errors.length === 0,
      errors,
    };
  }

  /**
   * Generate unique ID
   */
  private generateId(): string {
    return `pres_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }
}

export const presentationService = new PresentationService();
