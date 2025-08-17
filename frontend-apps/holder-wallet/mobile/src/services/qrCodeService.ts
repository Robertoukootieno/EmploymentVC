/**
 * QR Code Service
 * 
 * Handles QR code generation, parsing, and processing for various credential operations
 */

import QRCode from 'react-native-qrcode-svg';
import {VerifiableCredential, PresentationRequest, CredentialPresentation} from '../types/credential';

export interface QRCodeData {
  type: 'credential' | 'presentation_request' | 'verification_request' | 'wallet_connect' | 'unknown';
  version: string;
  data: any;
  metadata?: {
    issuer?: string;
    purpose?: string;
    expiresAt?: string;
    challenge?: string;
  };
}

export interface GenerateQROptions {
  size?: number;
  backgroundColor?: string;
  color?: string;
  logo?: string;
  logoSize?: number;
  logoBackgroundColor?: string;
  logoMargin?: number;
  quietZone?: number;
}

class QRCodeService {
  private readonly QR_VERSION = '1.0';
  private readonly QR_PREFIX = 'provenly://';

  /**
   * Parse QR code data and determine its type and content
   */
  async parseQRCode(qrData: string): Promise<QRCodeData> {
    try {
      // Handle Provenly-specific QR codes
      if (qrData.startsWith(this.QR_PREFIX)) {
        return this.parseProvenlyQR(qrData);
      }

      // Handle standard JSON data
      if (qrData.startsWith('{') && qrData.endsWith('}')) {
        return this.parseJSONQR(qrData);
      }

      // Handle URLs
      if (qrData.startsWith('http://') || qrData.startsWith('https://')) {
        return this.parseURLQR(qrData);
      }

      // Handle DID URLs
      if (qrData.startsWith('did:')) {
        return this.parseDIDQR(qrData);
      }

      // Unknown format
      return {
        type: 'unknown',
        version: this.QR_VERSION,
        data: qrData,
      };
    } catch (error) {
      console.error('Failed to parse QR code:', error);
      throw new Error('Invalid QR code format');
    }
  }

  /**
   * Parse Provenly-specific QR codes
   */
  private parseProvenlyQR(qrData: string): QRCodeData {
    const url = new URL(qrData);
    const type = url.pathname.substring(1); // Remove leading slash
    const params = Object.fromEntries(url.searchParams);

    switch (type) {
      case 'credential':
        return this.parseCredentialQR(params);
      case 'presentation-request':
        return this.parsePresentationRequestQR(params);
      case 'verification':
        return this.parseVerificationQR(params);
      case 'wallet-connect':
        return this.parseWalletConnectQR(params);
      default:
        throw new Error(`Unknown Provenly QR type: ${type}`);
    }
  }

  /**
   * Parse JSON-formatted QR codes
   */
  private parseJSONQR(qrData: string): QRCodeData {
    const data = JSON.parse(qrData);

    // Check for Verifiable Credential
    if (data['@context'] && data.credentialSubject && data.proof) {
      return {
        type: 'credential',
        version: this.QR_VERSION,
        data: data as VerifiableCredential,
        metadata: {
          issuer: typeof data.issuer === 'string' ? data.issuer : data.issuer?.id,
          expiresAt: data.expirationDate,
        },
      };
    }

    // Check for Presentation Request
    if (data.type === 'PresentationRequest' || data.requestedCredentials) {
      return {
        type: 'presentation_request',
        version: this.QR_VERSION,
        data: data as PresentationRequest,
        metadata: {
          purpose: data.purpose,
          expiresAt: data.expiresAt,
          challenge: data.challenge,
        },
      };
    }

    // Check for Verifiable Presentation
    if (data['@context'] && data.verifiableCredential && data.proof) {
      return {
        type: 'verification_request',
        version: this.QR_VERSION,
        data: data as CredentialPresentation,
      };
    }

    return {
      type: 'unknown',
      version: this.QR_VERSION,
      data,
    };
  }

  /**
   * Parse URL-based QR codes
   */
  private parseURLQR(qrData: string): QRCodeData {
    const url = new URL(qrData);

    // Check for credential verification URLs
    if (url.pathname.includes('/verify') || url.searchParams.has('credential')) {
      return {
        type: 'verification_request',
        version: this.QR_VERSION,
        data: {
          url: qrData,
          challenge: url.searchParams.get('challenge'),
        },
        metadata: {
          challenge: url.searchParams.get('challenge') || undefined,
        },
      };
    }

    // Check for presentation request URLs
    if (url.pathname.includes('/present') || url.searchParams.has('presentation')) {
      return {
        type: 'presentation_request',
        version: this.QR_VERSION,
        data: {
          url: qrData,
          challenge: url.searchParams.get('challenge'),
          purpose: url.searchParams.get('purpose'),
        },
        metadata: {
          purpose: url.searchParams.get('purpose') || undefined,
          challenge: url.searchParams.get('challenge') || undefined,
        },
      };
    }

    return {
      type: 'unknown',
      version: this.QR_VERSION,
      data: {url: qrData},
    };
  }

  /**
   * Parse DID-based QR codes
   */
  private parseDIDQR(qrData: string): QRCodeData {
    return {
      type: 'wallet_connect',
      version: this.QR_VERSION,
      data: {
        did: qrData,
      },
    };
  }

  /**
   * Parse credential QR parameters
   */
  private parseCredentialQR(params: Record<string, string>): QRCodeData {
    const credentialUrl = params.url;
    const challenge = params.challenge;

    if (!credentialUrl) {
      throw new Error('Missing credential URL in QR code');
    }

    return {
      type: 'credential',
      version: this.QR_VERSION,
      data: {
        url: credentialUrl,
        challenge,
      },
      metadata: {
        challenge,
      },
    };
  }

  /**
   * Parse presentation request QR parameters
   */
  private parsePresentationRequestQR(params: Record<string, string>): QRCodeData {
    const requestUrl = params.url;
    const challenge = params.challenge;
    const purpose = params.purpose;

    if (!requestUrl) {
      throw new Error('Missing request URL in QR code');
    }

    return {
      type: 'presentation_request',
      version: this.QR_VERSION,
      data: {
        url: requestUrl,
        challenge,
        purpose,
      },
      metadata: {
        purpose,
        challenge,
      },
    };
  }

  /**
   * Parse verification QR parameters
   */
  private parseVerificationQR(params: Record<string, string>): QRCodeData {
    const verificationUrl = params.url;
    const challenge = params.challenge;

    if (!verificationUrl) {
      throw new Error('Missing verification URL in QR code');
    }

    return {
      type: 'verification_request',
      version: this.QR_VERSION,
      data: {
        url: verificationUrl,
        challenge,
      },
      metadata: {
        challenge,
      },
    };
  }

  /**
   * Parse wallet connect QR parameters
   */
  private parseWalletConnectQR(params: Record<string, string>): QRCodeData {
    const did = params.did;
    const challenge = params.challenge;

    if (!did) {
      throw new Error('Missing DID in wallet connect QR code');
    }

    return {
      type: 'wallet_connect',
      version: this.QR_VERSION,
      data: {
        did,
        challenge,
      },
      metadata: {
        challenge,
      },
    };
  }

  /**
   * Generate QR code for a credential
   */
  generateCredentialQR(credential: VerifiableCredential, options?: GenerateQROptions): string {
    const qrData = {
      type: 'credential',
      version: this.QR_VERSION,
      credential,
    };

    return JSON.stringify(qrData);
  }

  /**
   * Generate QR code for a presentation request
   */
  generatePresentationRequestQR(request: PresentationRequest, options?: GenerateQROptions): string {
    const url = `${this.QR_PREFIX}presentation-request?url=${encodeURIComponent(request.id)}&challenge=${request.challenge}&purpose=${encodeURIComponent(request.purpose)}`;
    return url;
  }

  /**
   * Generate QR code for credential verification
   */
  generateVerificationQR(presentation: CredentialPresentation, options?: GenerateQROptions): string {
    const qrData = {
      type: 'verification',
      version: this.QR_VERSION,
      presentation,
    };

    return JSON.stringify(qrData);
  }

  /**
   * Generate QR code for wallet connection
   */
  generateWalletConnectQR(did: string, challenge?: string, options?: GenerateQROptions): string {
    const params = new URLSearchParams({did});
    if (challenge) {
      params.append('challenge', challenge);
    }

    return `${this.QR_PREFIX}wallet-connect?${params.toString()}`;
  }

  /**
   * Generate QR code SVG component
   */
  generateQRCodeSVG(data: string, options: GenerateQROptions = {}) {
    const {
      size = 200,
      backgroundColor = 'white',
      color = 'black',
      logo,
      logoSize = 40,
      logoBackgroundColor = 'white',
      logoMargin = 2,
      quietZone = 10,
    } = options;

    return (
      <QRCode
        value={data}
        size={size}
        backgroundColor={backgroundColor}
        color={color}
        logo={logo ? {uri: logo} : undefined}
        logoSize={logoSize}
        logoBackgroundColor={logoBackgroundColor}
        logoMargin={logoMargin}
        quietZone={quietZone}
        enableLinearGradient={false}
        getRef={(ref) => {
          // Store reference if needed
        }}
      />
    );
  }

  /**
   * Validate QR code data structure
   */
  validateQRData(data: any): boolean {
    try {
      if (typeof data === 'string') {
        // Try to parse as JSON
        JSON.parse(data);
        return true;
      }

      if (typeof data === 'object' && data !== null) {
        return true;
      }

      return false;
    } catch (error) {
      return false;
    }
  }

  /**
   * Get QR code type from data
   */
  getQRType(qrData: string): string {
    try {
      const parsed = this.parseQRCode(qrData);
      return parsed.type;
    } catch (error) {
      return 'unknown';
    }
  }

  /**
   * Check if QR code is expired
   */
  isQRExpired(qrData: QRCodeData): boolean {
    if (!qrData.metadata?.expiresAt) {
      return false;
    }

    const expirationDate = new Date(qrData.metadata.expiresAt);
    const now = new Date();

    return now > expirationDate;
  }

  /**
   * Extract metadata from QR code
   */
  extractMetadata(qrData: QRCodeData): Record<string, any> {
    return {
      type: qrData.type,
      version: qrData.version,
      ...qrData.metadata,
      isExpired: this.isQRExpired(qrData),
    };
  }
}

export const qrCodeService = new QRCodeService();
