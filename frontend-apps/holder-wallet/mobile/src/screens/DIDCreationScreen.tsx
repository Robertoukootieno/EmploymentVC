/**
 * DID Creation Screen
 * 
 * Handles creation and management of Decentralized Identifiers (DIDs)
 */

import React, {useState, useEffect} from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  Alert,
  StyleSheet,
  Clipboard,
  Share,
} from 'react-native';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import Icon from 'react-native-vector-icons/MaterialIcons';
import QRCode from 'react-native-qrcode-svg';

// Redux
import {useAppDispatch, useAppSelector} from '../store';
import {createDID, updateDID} from '../store/slices/didSlice';

// Components
import LoadingSpinner from '../components/LoadingSpinner';
import KeyPairDisplay from '../components/KeyPairDisplay';
import DIDMethodSelector from '../components/DIDMethodSelector';

// Services
import {didService} from '../services/didService';
import {keyService} from '../services/keyService';

// Types
import {DIDDocument, DIDMethod, KeyType} from '../types/crypto';

// Styles
import {Colors} from '../styles/colors';
import {Typography} from '../styles/typography';
import {Spacing} from '../styles/spacing';

interface DIDCreationState {
  step: 'method' | 'keys' | 'creation' | 'complete';
  selectedMethod: DIDMethod;
  selectedKeyType: KeyType;
  keyPair: any;
  didDocument: DIDDocument | null;
  did: string | null;
  isLoading: boolean;
}

const DIDCreationScreen: React.FC = () => {
  const insets = useSafeAreaInsets();
  const dispatch = useAppDispatch();

  const [state, setState] = useState<DIDCreationState>({
    step: 'method',
    selectedMethod: 'ebsi',
    selectedKeyType: KeyType.SECP256R1,
    keyPair: null,
    didDocument: null,
    did: null,
    isLoading: false,
  });

  const {activeWallet} = useAppSelector((state) => state.wallet);
  const {user} = useAppSelector((state) => state.auth);

  const updateState = (updates: Partial<DIDCreationState>) => {
    setState(prev => ({...prev, ...updates}));
  };

  const handleMethodSelection = (method: DIDMethod) => {
    updateState({selectedMethod: method, step: 'keys'});
  };

  const handleKeyTypeSelection = (keyType: KeyType) => {
    updateState({selectedKeyType: keyType});
  };

  const handleGenerateKeys = async () => {
    try {
      updateState({isLoading: true});
      
      const keyPair = await keyService.generateKeyPair(state.selectedKeyType);
      
      updateState({
        keyPair,
        step: 'creation',
        isLoading: false,
      });
    } catch (error) {
      console.error('Failed to generate keys:', error);
      Alert.alert('Error', 'Failed to generate cryptographic keys');
      updateState({isLoading: false});
    }
  };

  const handleCreateDID = async () => {
    try {
      updateState({isLoading: true});

      if (!state.keyPair) {
        throw new Error('No key pair available');
      }

      // Create DID using the selected method
      const didResult = await didService.createDID(
        state.selectedMethod,
        state.keyPair.publicKey,
        {
          keyType: state.selectedKeyType,
          controller: user?.did,
          service: [
            {
              id: '#provenly-wallet',
              type: 'ProvenlyWallet',
              serviceEndpoint: 'https://wallet.provenly.io',
            },
          ],
        }
      );

      // Store DID in wallet if available
      if (activeWallet) {
        await dispatch(createDID({
          walletId: activeWallet.id,
          did: didResult.id,
          didDocument: didResult.document,
          keyPair: state.keyPair,
          method: state.selectedMethod,
          keyType: state.selectedKeyType,
        })).unwrap();
      }

      updateState({
        did: didResult.id,
        didDocument: didResult.document,
        step: 'complete',
        isLoading: false,
      });

      Alert.alert(
        'DID Created Successfully',
        `Your DID has been created and registered on the ${state.selectedMethod.toUpperCase()} network.`
      );
    } catch (error: any) {
      console.error('Failed to create DID:', error);
      Alert.alert('Error', error.message || 'Failed to create DID');
      updateState({isLoading: false});
    }
  };

  const handleCopyDID = () => {
    if (state.did) {
      Clipboard.setString(state.did);
      Alert.alert('Copied', 'DID copied to clipboard');
    }
  };

  const handleShareDID = async () => {
    if (state.did) {
      try {
        await Share.share({
          message: `My DID: ${state.did}`,
          title: 'Share DID',
        });
      } catch (error) {
        console.error('Failed to share DID:', error);
      }
    }
  };

  const handleExportKeys = () => {
    Alert.alert(
      'Export Keys',
      'This will export your private key. Keep it secure and never share it with anyone.',
      [
        {text: 'Cancel', style: 'cancel'},
        {
          text: 'Export',
          style: 'destructive',
          onPress: () => {
            // Navigate to key export screen
            console.log('Export keys');
          },
        },
      ]
    );
  };

  const renderMethodSelection = () => (
    <View style={styles.stepContainer}>
      <Text style={styles.stepTitle}>Choose DID Method</Text>
      <Text style={styles.stepDescription}>
        Select the blockchain network or method for your DID
      </Text>

      <DIDMethodSelector
        selectedMethod={state.selectedMethod}
        onMethodSelect={handleMethodSelection}
      />

      <View style={styles.methodInfo}>
        <Text style={styles.infoTitle}>About {state.selectedMethod.toUpperCase()}</Text>
        <Text style={styles.infoText}>
          {getMethodDescription(state.selectedMethod)}
        </Text>
      </View>
    </View>
  );

  const renderKeySelection = () => (
    <View style={styles.stepContainer}>
      <Text style={styles.stepTitle}>Select Key Type</Text>
      <Text style={styles.stepDescription}>
        Choose the cryptographic algorithm for your keys
      </Text>

      <View style={styles.keyTypeGrid}>
        {[
          {
            type: KeyType.SECP256R1,
            name: 'SECP256R1',
            description: 'NIST P-256 curve, widely supported',
            recommended: true,
          },
          {
            type: KeyType.SECP256K1,
            name: 'SECP256K1',
            description: 'Bitcoin/Ethereum curve',
            recommended: false,
          },
          {
            type: KeyType.RSA,
            name: 'RSA-2048',
            description: 'Traditional RSA encryption',
            recommended: false,
          },
          {
            type: KeyType.ED25519,
            name: 'Ed25519',
            description: 'Edwards curve, high performance',
            recommended: false,
          },
        ].map((keyType) => (
          <TouchableOpacity
            key={keyType.type}
            style={[
              styles.keyTypeCard,
              state.selectedKeyType === keyType.type && styles.keyTypeCardSelected,
            ]}
            onPress={() => handleKeyTypeSelection(keyType.type)}
          >
            {keyType.recommended && (
              <View style={styles.recommendedBadge}>
                <Text style={styles.recommendedText}>Recommended</Text>
              </View>
            )}
            <Text
              style={[
                styles.keyTypeName,
                state.selectedKeyType === keyType.type && styles.keyTypeNameSelected,
              ]}
            >
              {keyType.name}
            </Text>
            <Text style={styles.keyTypeDescription}>{keyType.description}</Text>
          </TouchableOpacity>
        ))}
      </View>

      <TouchableOpacity
        style={[styles.generateButton, state.isLoading && styles.generateButtonDisabled]}
        onPress={handleGenerateKeys}
        disabled={state.isLoading}
      >
        {state.isLoading ? (
          <LoadingSpinner size="small" color={Colors.white} />
        ) : (
          <>
            <Icon name="vpn-key" size={20} color={Colors.white} />
            <Text style={styles.generateButtonText}>Generate Key Pair</Text>
          </>
        )}
      </TouchableOpacity>
    </View>
  );

  const renderKeyGeneration = () => (
    <View style={styles.stepContainer}>
      <Text style={styles.stepTitle}>Key Pair Generated</Text>
      <Text style={styles.stepDescription}>
        Your cryptographic keys have been generated securely
      </Text>

      {state.keyPair && (
        <KeyPairDisplay
          keyPair={state.keyPair}
          keyType={state.selectedKeyType}
          showPrivateKey={false}
        />
      )}

      <View style={styles.securityNotice}>
        <Icon name="security" size={24} color={Colors.warning} />
        <Text style={styles.securityText}>
          Your private key is stored securely on your device and never leaves it.
        </Text>
      </View>

      <TouchableOpacity
        style={[styles.createDIDButton, state.isLoading && styles.createDIDButtonDisabled]}
        onPress={handleCreateDID}
        disabled={state.isLoading}
      >
        {state.isLoading ? (
          <LoadingSpinner size="small" color={Colors.white} />
        ) : (
          <>
            <Icon name="fingerprint" size={20} color={Colors.white} />
            <Text style={styles.createDIDButtonText}>Create DID</Text>
          </>
        )}
      </TouchableOpacity>
    </View>
  );

  const renderDIDComplete = () => (
    <View style={styles.stepContainer}>
      <View style={styles.successHeader}>
        <Icon name="check-circle" size={64} color={Colors.success} />
        <Text style={styles.successTitle}>DID Created Successfully!</Text>
        <Text style={styles.successDescription}>
          Your decentralized identifier is ready to use
        </Text>
      </View>

      <View style={styles.didContainer}>
        <Text style={styles.didLabel}>Your DID:</Text>
        <View style={styles.didDisplay}>
          <Text style={styles.didText} numberOfLines={2}>
            {state.did}
          </Text>
          <TouchableOpacity style={styles.copyButton} onPress={handleCopyDID}>
            <Icon name="content-copy" size={20} color={Colors.primary} />
          </TouchableOpacity>
        </View>
      </View>

      {state.did && (
        <View style={styles.qrContainer}>
          <Text style={styles.qrLabel}>QR Code:</Text>
          <View style={styles.qrCodeWrapper}>
            <QRCode
              value={state.did}
              size={200}
              backgroundColor={Colors.white}
              color={Colors.black}
            />
          </View>
        </View>
      )}

      <View style={styles.actionButtons}>
        <TouchableOpacity style={styles.actionButton} onPress={handleShareDID}>
          <Icon name="share" size={20} color={Colors.primary} />
          <Text style={styles.actionButtonText}>Share DID</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.actionButton} onPress={handleExportKeys}>
          <Icon name="download" size={20} color={Colors.primary} />
          <Text style={styles.actionButtonText}>Export Keys</Text>
        </TouchableOpacity>
      </View>

      {state.didDocument && (
        <View style={styles.documentContainer}>
          <Text style={styles.documentLabel}>DID Document:</Text>
          <ScrollView style={styles.documentScroll} horizontal>
            <Text style={styles.documentText}>
              {JSON.stringify(state.didDocument, null, 2)}
            </Text>
          </ScrollView>
        </View>
      )}
    </View>
  );

  const getMethodDescription = (method: DIDMethod): string => {
    switch (method) {
      case 'ebsi':
        return 'European Blockchain Services Infrastructure - Official EU blockchain network for trusted digital credentials.';
      case 'ethr':
        return 'Ethereum-based DID method using smart contracts for decentralized identity management.';
      case 'key':
        return 'Simple key-based DID method that derives identity directly from cryptographic keys.';
      case 'web':
        return 'Web-based DID method that uses HTTPS domains for identity verification.';
      default:
        return 'Decentralized identifier method for secure digital identity.';
    }
  };

  const renderStepIndicator = () => (
    <View style={styles.stepIndicator}>
      {['method', 'keys', 'creation', 'complete'].map((step, index) => (
        <View key={step} style={styles.stepIndicatorItem}>
          <View
            style={[
              styles.stepIndicatorDot,
              getStepIndex(state.step) >= index && styles.stepIndicatorDotActive,
            ]}
          >
            <Text
              style={[
                styles.stepIndicatorText,
                getStepIndex(state.step) >= index && styles.stepIndicatorTextActive,
              ]}
            >
              {index + 1}
            </Text>
          </View>
          {index < 3 && (
            <View
              style={[
                styles.stepIndicatorLine,
                getStepIndex(state.step) > index && styles.stepIndicatorLineActive,
              ]}
            />
          )}
        </View>
      ))}
    </View>
  );

  const getStepIndex = (step: string): number => {
    const steps = ['method', 'keys', 'creation', 'complete'];
    return steps.indexOf(step);
  };

  return (
    <View style={[styles.container, {paddingTop: insets.top}]}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity style={styles.backButton} onPress={() => {}}>
          <Icon name="arrow-back" size={24} color={Colors.textPrimary} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Create DID</Text>
        <View style={styles.placeholder} />
      </View>

      {/* Step Indicator */}
      {renderStepIndicator()}

      {/* Content */}
      <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
        {state.step === 'method' && renderMethodSelection()}
        {state.step === 'keys' && renderKeySelection()}
        {state.step === 'creation' && renderKeyGeneration()}
        {state.step === 'complete' && renderDIDComplete()}
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.md,
    backgroundColor: Colors.surface,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  backButton: {
    padding: Spacing.sm,
  },
  headerTitle: {
    fontSize: Typography.sizes.lg,
    fontFamily: Typography.fonts.bold,
    color: Colors.textPrimary,
  },
  placeholder: {
    width: 40,
  },
  stepIndicator: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: Spacing.lg,
    backgroundColor: Colors.surface,
  },
  stepIndicatorItem: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  stepIndicatorDot: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: Colors.border,
    justifyContent: 'center',
    alignItems: 'center',
  },
  stepIndicatorDotActive: {
    backgroundColor: Colors.primary,
  },
  stepIndicatorText: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.textSecondary,
  },
  stepIndicatorTextActive: {
    color: Colors.white,
  },
  stepIndicatorLine: {
    width: 40,
    height: 2,
    backgroundColor: Colors.border,
    marginHorizontal: Spacing.sm,
  },
  stepIndicatorLineActive: {
    backgroundColor: Colors.primary,
  },
  content: {
    flex: 1,
    padding: Spacing.lg,
  },
  stepContainer: {
    flex: 1,
  },
  stepTitle: {
    fontSize: Typography.sizes.xl,
    fontFamily: Typography.fonts.bold,
    color: Colors.textPrimary,
    textAlign: 'center',
    marginBottom: Spacing.sm,
  },
  stepDescription: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.regular,
    color: Colors.textSecondary,
    textAlign: 'center',
    marginBottom: Spacing.xl,
    lineHeight: 22,
  },
  methodInfo: {
    backgroundColor: Colors.surface,
    borderRadius: 12,
    padding: Spacing.lg,
    marginTop: Spacing.lg,
  },
  infoTitle: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.textPrimary,
    marginBottom: Spacing.sm,
  },
  infoText: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.regular,
    color: Colors.textSecondary,
    lineHeight: 20,
  },
  keyTypeGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    marginBottom: Spacing.xl,
  },
  keyTypeCard: {
    width: '48%',
    backgroundColor: Colors.surface,
    borderRadius: 12,
    padding: Spacing.md,
    marginBottom: Spacing.md,
    borderWidth: 2,
    borderColor: Colors.border,
    position: 'relative',
  },
  keyTypeCardSelected: {
    borderColor: Colors.primary,
    backgroundColor: Colors.primaryLight,
  },
  recommendedBadge: {
    position: 'absolute',
    top: -8,
    right: 8,
    backgroundColor: Colors.success,
    borderRadius: 12,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 2,
  },
  recommendedText: {
    fontSize: Typography.sizes.xs,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.white,
  },
  keyTypeName: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.textPrimary,
    marginBottom: Spacing.xs,
  },
  keyTypeNameSelected: {
    color: Colors.primary,
  },
  keyTypeDescription: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.regular,
    color: Colors.textSecondary,
    lineHeight: 18,
  },
  generateButton: {
    backgroundColor: Colors.primary,
    borderRadius: 12,
    paddingVertical: Spacing.md,
    paddingHorizontal: Spacing.lg,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  generateButtonDisabled: {
    backgroundColor: Colors.textSecondary,
  },
  generateButtonText: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.white,
    marginLeft: Spacing.sm,
  },
  securityNotice: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.warningLight,
    borderRadius: 8,
    padding: Spacing.md,
    marginVertical: Spacing.lg,
  },
  securityText: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.regular,
    color: Colors.textPrimary,
    marginLeft: Spacing.sm,
    flex: 1,
    lineHeight: 18,
  },
  createDIDButton: {
    backgroundColor: Colors.success,
    borderRadius: 12,
    paddingVertical: Spacing.md,
    paddingHorizontal: Spacing.lg,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  createDIDButtonDisabled: {
    backgroundColor: Colors.textSecondary,
  },
  createDIDButtonText: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.white,
    marginLeft: Spacing.sm,
  },
  successHeader: {
    alignItems: 'center',
    marginBottom: Spacing.xl,
  },
  successTitle: {
    fontSize: Typography.sizes.xl,
    fontFamily: Typography.fonts.bold,
    color: Colors.success,
    marginTop: Spacing.md,
    textAlign: 'center',
  },
  successDescription: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.regular,
    color: Colors.textSecondary,
    marginTop: Spacing.sm,
    textAlign: 'center',
  },
  didContainer: {
    backgroundColor: Colors.surface,
    borderRadius: 12,
    padding: Spacing.lg,
    marginBottom: Spacing.lg,
  },
  didLabel: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.textSecondary,
    marginBottom: Spacing.sm,
  },
  didDisplay: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.background,
    borderRadius: 8,
    padding: Spacing.md,
  },
  didText: {
    flex: 1,
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.mono,
    color: Colors.textPrimary,
    marginRight: Spacing.sm,
  },
  copyButton: {
    padding: Spacing.sm,
  },
  qrContainer: {
    alignItems: 'center',
    marginBottom: Spacing.lg,
  },
  qrLabel: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.textSecondary,
    marginBottom: Spacing.md,
  },
  qrCodeWrapper: {
    backgroundColor: Colors.white,
    borderRadius: 12,
    padding: Spacing.lg,
    shadowColor: Colors.black,
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 4,
  },
  actionButtons: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginBottom: Spacing.lg,
  },
  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    borderRadius: 8,
    paddingVertical: Spacing.md,
    paddingHorizontal: Spacing.lg,
    borderWidth: 1,
    borderColor: Colors.primary,
  },
  actionButtonText: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.primary,
    marginLeft: Spacing.sm,
  },
  documentContainer: {
    backgroundColor: Colors.surface,
    borderRadius: 12,
    padding: Spacing.lg,
  },
  documentLabel: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.textSecondary,
    marginBottom: Spacing.sm,
  },
  documentScroll: {
    maxHeight: 200,
  },
  documentText: {
    fontSize: Typography.sizes.xs,
    fontFamily: Typography.fonts.mono,
    color: Colors.textPrimary,
    lineHeight: 16,
  },
});

export default DIDCreationScreen;
