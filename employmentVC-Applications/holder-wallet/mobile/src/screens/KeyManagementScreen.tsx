/**
 * Key Management Screen
 * 
 * Displays and manages cryptographic keys, public key retrieval, and key operations
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
  Modal,
} from 'react-native';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import Icon from 'react-native-vector-icons/MaterialIcons';
import QRCode from 'react-native-qrcode-svg';

// Services
import {keyService} from '../services/keyService';
import {didService} from '../services/didService';

// Types
import {KeyMetadata, KeyType, KeyPurpose} from '../types/crypto';

// Components
import LoadingSpinner from '../components/LoadingSpinner';
import KeyCard from '../components/KeyCard';
import SearchBar from '../components/SearchBar';

// Styles
import {Colors} from '../styles/colors';
import {Typography} from '../styles/typography';
import {Spacing} from '../styles/spacing';

interface KeyWithDetails extends KeyMetadata {
  publicKey?: string;
  did?: string;
}

const KeyManagementScreen: React.FC = () => {
  const insets = useSafeAreaInsets();

  const [keys, setKeys] = useState<KeyWithDetails[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedKey, setSelectedKey] = useState<KeyWithDetails | null>(null);
  const [showPublicKeyModal, setShowPublicKeyModal] = useState(false);
  const [showQRModal, setShowQRModal] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    loadKeys();
  }, []);

  const loadKeys = async () => {
    try {
      setLoading(true);
      const keyList = await keyService.listKeys();
      
      // Enhance keys with additional details
      const enhancedKeys = await Promise.all(
        keyList.map(async (key) => {
          const publicKey = await keyService.getPublicKey(key.keyId);
          const dids = await didService.listDIDs();
          const associatedDID = dids.find(did => 
            did.keys.keyId === key.keyId
          );

          return {
            ...key,
            publicKey,
            did: associatedDID?.id,
          };
        })
      );

      setKeys(enhancedKeys);
    } catch (error) {
      console.error('Failed to load keys:', error);
      Alert.alert('Error', 'Failed to load cryptographic keys');
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    setRefreshing(true);
    await loadKeys();
    setRefreshing(false);
  };

  const handleKeyPress = (key: KeyWithDetails) => {
    setSelectedKey(key);
    setShowPublicKeyModal(true);
  };

  const handleGenerateNewKey = () => {
    Alert.alert(
      'Generate New Key',
      'Choose key type:',
      [
        {text: 'Cancel', style: 'cancel'},
        {text: 'SECP256R1', onPress: () => generateKey(KeyType.SECP256R1)},
        {text: 'SECP256K1', onPress: () => generateKey(KeyType.SECP256K1)},
        {text: 'RSA-2048', onPress: () => generateKey(KeyType.RSA)},
        {text: 'Ed25519', onPress: () => generateKey(KeyType.ED25519)},
      ]
    );
  };

  const generateKey = async (keyType: KeyType) => {
    try {
      setLoading(true);
      await keyService.generateKeyPair(keyType, {
        purpose: [KeyPurpose.AUTHENTICATION, KeyPurpose.ASSERTION_METHOD],
        hardwareBacked: true,
      });
      await loadKeys();
      Alert.alert('Success', `${keyType} key pair generated successfully`);
    } catch (error) {
      console.error('Failed to generate key:', error);
      Alert.alert('Error', 'Failed to generate key pair');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteKey = (key: KeyWithDetails) => {
    Alert.alert(
      'Delete Key',
      `Are you sure you want to delete the ${key.keyType} key? This action cannot be undone.`,
      [
        {text: 'Cancel', style: 'cancel'},
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            try {
              await keyService.deleteKey(key.keyId);
              await loadKeys();
              Alert.alert('Success', 'Key deleted successfully');
            } catch (error) {
              Alert.alert('Error', 'Failed to delete key');
            }
          },
        },
      ]
    );
  };

  const handleCopyPublicKey = () => {
    if (selectedKey?.publicKey) {
      Clipboard.setString(selectedKey.publicKey);
      Alert.alert('Copied', 'Public key copied to clipboard');
    }
  };

  const handleSharePublicKey = async () => {
    if (selectedKey?.publicKey) {
      try {
        await Share.share({
          message: `Public Key (${selectedKey.keyType}):\n\n${selectedKey.publicKey}`,
          title: 'Share Public Key',
        });
      } catch (error) {
        console.error('Failed to share public key:', error);
      }
    }
  };

  const handleExportPublicKey = async (format: 'pem' | 'jwk' | 'hex') => {
    if (!selectedKey) return;

    try {
      const exportedKey = await keyService.exportPublicKey(selectedKey.keyId, format);
      if (exportedKey) {
        Clipboard.setString(exportedKey);
        Alert.alert('Exported', `Public key exported as ${format.toUpperCase()} and copied to clipboard`);
      }
    } catch (error) {
      Alert.alert('Error', 'Failed to export public key');
    }
  };

  const handleShowQR = () => {
    setShowPublicKeyModal(false);
    setShowQRModal(true);
  };

  const filteredKeys = keys.filter(key =>
    key.keyType.toLowerCase().includes(searchQuery.toLowerCase()) ||
    key.keyId.toLowerCase().includes(searchQuery.toLowerCase()) ||
    key.algorithm.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const renderKeyStats = () => (
    <View style={styles.statsContainer}>
      <View style={styles.statItem}>
        <Text style={styles.statNumber}>{keys.length}</Text>
        <Text style={styles.statLabel}>Total Keys</Text>
      </View>
      <View style={styles.statItem}>
        <Text style={styles.statNumber}>
          {keys.filter(k => k.isHardwareBacked).length}
        </Text>
        <Text style={styles.statLabel}>Hardware Backed</Text>
      </View>
      <View style={styles.statItem}>
        <Text style={styles.statNumber}>
          {keys.filter(k => k.keyType === KeyType.SECP256R1).length}
        </Text>
        <Text style={styles.statLabel}>SECP256R1</Text>
      </View>
      <View style={styles.statItem}>
        <Text style={styles.statNumber}>
          {keys.filter(k => k.keyType === KeyType.RSA).length}
        </Text>
        <Text style={styles.statLabel}>RSA</Text>
      </View>
    </View>
  );

  const renderPublicKeyModal = () => (
    <Modal
      visible={showPublicKeyModal}
      animationType="slide"
      presentationStyle="pageSheet"
      onRequestClose={() => setShowPublicKeyModal(false)}
    >
      <View style={styles.modalContainer}>
        <View style={styles.modalHeader}>
          <TouchableOpacity onPress={() => setShowPublicKeyModal(false)}>
            <Icon name="close" size={24} color={Colors.textPrimary} />
          </TouchableOpacity>
          <Text style={styles.modalTitle}>Public Key Details</Text>
          <View style={styles.placeholder} />
        </View>

        <ScrollView style={styles.modalContent}>
          {selectedKey && (
            <>
              <View style={styles.keyInfoSection}>
                <Text style={styles.sectionTitle}>Key Information</Text>
                <View style={styles.infoRow}>
                  <Text style={styles.infoLabel}>Type:</Text>
                  <Text style={styles.infoValue}>{selectedKey.keyType}</Text>
                </View>
                <View style={styles.infoRow}>
                  <Text style={styles.infoLabel}>Algorithm:</Text>
                  <Text style={styles.infoValue}>{selectedKey.algorithm}</Text>
                </View>
                <View style={styles.infoRow}>
                  <Text style={styles.infoLabel}>Key Size:</Text>
                  <Text style={styles.infoValue}>{selectedKey.keySize} bits</Text>
                </View>
                <View style={styles.infoRow}>
                  <Text style={styles.infoLabel}>Hardware Backed:</Text>
                  <Text style={[
                    styles.infoValue,
                    {color: selectedKey.isHardwareBacked ? Colors.success : Colors.warning}
                  ]}>
                    {selectedKey.isHardwareBacked ? 'Yes' : 'No'}
                  </Text>
                </View>
                {selectedKey.did && (
                  <View style={styles.infoRow}>
                    <Text style={styles.infoLabel}>Associated DID:</Text>
                    <Text style={styles.infoValue} numberOfLines={2}>
                      {selectedKey.did}
                    </Text>
                  </View>
                )}
              </View>

              <View style={styles.publicKeySection}>
                <Text style={styles.sectionTitle}>Public Key</Text>
                <View style={styles.publicKeyContainer}>
                  <ScrollView horizontal showsHorizontalScrollIndicator={false}>
                    <Text style={styles.publicKeyText}>
                      {selectedKey.publicKey}
                    </Text>
                  </ScrollView>
                </View>
              </View>

              <View style={styles.actionButtons}>
                <TouchableOpacity style={styles.actionButton} onPress={handleCopyPublicKey}>
                  <Icon name="content-copy" size={20} color={Colors.primary} />
                  <Text style={styles.actionButtonText}>Copy</Text>
                </TouchableOpacity>

                <TouchableOpacity style={styles.actionButton} onPress={handleSharePublicKey}>
                  <Icon name="share" size={20} color={Colors.primary} />
                  <Text style={styles.actionButtonText}>Share</Text>
                </TouchableOpacity>

                <TouchableOpacity style={styles.actionButton} onPress={handleShowQR}>
                  <Icon name="qr-code" size={20} color={Colors.primary} />
                  <Text style={styles.actionButtonText}>QR Code</Text>
                </TouchableOpacity>
              </View>

              <View style={styles.exportSection}>
                <Text style={styles.sectionTitle}>Export Formats</Text>
                <View style={styles.exportButtons}>
                  <TouchableOpacity 
                    style={styles.exportButton} 
                    onPress={() => handleExportPublicKey('pem')}
                  >
                    <Text style={styles.exportButtonText}>PEM</Text>
                  </TouchableOpacity>
                  <TouchableOpacity 
                    style={styles.exportButton} 
                    onPress={() => handleExportPublicKey('jwk')}
                  >
                    <Text style={styles.exportButtonText}>JWK</Text>
                  </TouchableOpacity>
                  <TouchableOpacity 
                    style={styles.exportButton} 
                    onPress={() => handleExportPublicKey('hex')}
                  >
                    <Text style={styles.exportButtonText}>HEX</Text>
                  </TouchableOpacity>
                </View>
              </View>
            </>
          )}
        </ScrollView>
      </View>
    </Modal>
  );

  const renderQRModal = () => (
    <Modal
      visible={showQRModal}
      animationType="slide"
      presentationStyle="pageSheet"
      onRequestClose={() => setShowQRModal(false)}
    >
      <View style={styles.modalContainer}>
        <View style={styles.modalHeader}>
          <TouchableOpacity onPress={() => setShowQRModal(false)}>
            <Icon name="close" size={24} color={Colors.textPrimary} />
          </TouchableOpacity>
          <Text style={styles.modalTitle}>Public Key QR Code</Text>
          <View style={styles.placeholder} />
        </View>

        <View style={styles.qrContainer}>
          {selectedKey?.publicKey && (
            <>
              <View style={styles.qrCodeWrapper}>
                <QRCode
                  value={selectedKey.publicKey}
                  size={250}
                  backgroundColor={Colors.white}
                  color={Colors.black}
                />
              </View>
              <Text style={styles.qrDescription}>
                Scan this QR code to share the public key
              </Text>
              <TouchableOpacity 
                style={styles.shareQRButton} 
                onPress={handleSharePublicKey}
              >
                <Icon name="share" size={20} color={Colors.white} />
                <Text style={styles.shareQRButtonText}>Share QR Code</Text>
              </TouchableOpacity>
            </>
          )}
        </View>
      </View>
    </Modal>
  );

  if (loading && keys.length === 0) {
    return (
      <View style={[styles.container, styles.centered]}>
        <LoadingSpinner size="large" />
        <Text style={styles.loadingText}>Loading cryptographic keys...</Text>
      </View>
    );
  }

  return (
    <View style={[styles.container, {paddingTop: insets.top}]}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Key Management</Text>
        <TouchableOpacity style={styles.addButton} onPress={handleGenerateNewKey}>
          <Icon name="add" size={24} color={Colors.white} />
        </TouchableOpacity>
      </View>

      {/* Search */}
      <View style={styles.searchContainer}>
        <SearchBar
          placeholder="Search keys..."
          value={searchQuery}
          onChangeText={setSearchQuery}
        />
      </View>

      {/* Stats */}
      {renderKeyStats()}

      {/* Keys List */}
      <ScrollView 
        style={styles.keysList}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />
        }
      >
        {filteredKeys.map((key) => (
          <KeyCard
            key={key.keyId}
            keyMetadata={key}
            onPress={() => handleKeyPress(key)}
            onDelete={() => handleDeleteKey(key)}
          />
        ))}

        {filteredKeys.length === 0 && (
          <View style={styles.emptyState}>
            <Icon name="vpn-key" size={64} color={Colors.textSecondary} />
            <Text style={styles.emptyTitle}>No Keys Found</Text>
            <Text style={styles.emptySubtitle}>
              {searchQuery ? 'Try adjusting your search' : 'Generate your first cryptographic key'}
            </Text>
          </View>
        )}
      </ScrollView>

      {/* Modals */}
      {renderPublicKeyModal()}
      {renderQRModal()}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  centered: {
    justifyContent: 'center',
    alignItems: 'center',
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
  headerTitle: {
    fontSize: Typography.sizes.xl,
    fontFamily: Typography.fonts.bold,
    color: Colors.textPrimary,
  },
  addButton: {
    backgroundColor: Colors.primary,
    borderRadius: 20,
    width: 40,
    height: 40,
    justifyContent: 'center',
    alignItems: 'center',
  },
  searchContainer: {
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.md,
  },
  statsContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingVertical: Spacing.lg,
    backgroundColor: Colors.surface,
    marginHorizontal: Spacing.lg,
    borderRadius: 12,
    marginBottom: Spacing.md,
  },
  statItem: {
    alignItems: 'center',
  },
  statNumber: {
    fontSize: Typography.sizes.xl,
    fontFamily: Typography.fonts.bold,
    color: Colors.primary,
  },
  statLabel: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.regular,
    color: Colors.textSecondary,
    marginTop: Spacing.xs,
  },
  keysList: {
    flex: 1,
    paddingHorizontal: Spacing.lg,
  },
  emptyState: {
    alignItems: 'center',
    paddingVertical: Spacing.xl,
  },
  emptyTitle: {
    fontSize: Typography.sizes.lg,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.textPrimary,
    marginTop: Spacing.md,
  },
  emptySubtitle: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.regular,
    color: Colors.textSecondary,
    marginTop: Spacing.sm,
    textAlign: 'center',
  },
  loadingText: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.regular,
    color: Colors.textSecondary,
    marginTop: Spacing.md,
  },
  modalContainer: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.md,
    backgroundColor: Colors.surface,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  modalTitle: {
    fontSize: Typography.sizes.lg,
    fontFamily: Typography.fonts.bold,
    color: Colors.textPrimary,
  },
  placeholder: {
    width: 24,
  },
  modalContent: {
    flex: 1,
    padding: Spacing.lg,
  },
  keyInfoSection: {
    backgroundColor: Colors.surface,
    borderRadius: 12,
    padding: Spacing.lg,
    marginBottom: Spacing.lg,
  },
  sectionTitle: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.textPrimary,
    marginBottom: Spacing.md,
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: Spacing.sm,
  },
  infoLabel: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.medium,
    color: Colors.textSecondary,
  },
  infoValue: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.textPrimary,
    flex: 1,
    textAlign: 'right',
  },
  publicKeySection: {
    backgroundColor: Colors.surface,
    borderRadius: 12,
    padding: Spacing.lg,
    marginBottom: Spacing.lg,
  },
  publicKeyContainer: {
    backgroundColor: Colors.background,
    borderRadius: 8,
    padding: Spacing.md,
    maxHeight: 120,
  },
  publicKeyText: {
    fontSize: Typography.sizes.xs,
    fontFamily: Typography.fonts.mono,
    color: Colors.textPrimary,
    lineHeight: 16,
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
  exportSection: {
    backgroundColor: Colors.surface,
    borderRadius: 12,
    padding: Spacing.lg,
  },
  exportButtons: {
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
  exportButton: {
    backgroundColor: Colors.primary,
    borderRadius: 8,
    paddingVertical: Spacing.sm,
    paddingHorizontal: Spacing.lg,
    minWidth: 80,
    alignItems: 'center',
  },
  exportButtonText: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.white,
  },
  qrContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: Spacing.xl,
  },
  qrCodeWrapper: {
    backgroundColor: Colors.white,
    borderRadius: 16,
    padding: Spacing.lg,
    shadowColor: Colors.black,
    shadowOffset: {width: 0, height: 4},
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 8,
  },
  qrDescription: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.regular,
    color: Colors.textSecondary,
    textAlign: 'center',
    marginTop: Spacing.lg,
    marginBottom: Spacing.xl,
  },
  shareQRButton: {
    backgroundColor: Colors.primary,
    borderRadius: 12,
    paddingVertical: Spacing.md,
    paddingHorizontal: Spacing.xl,
    flexDirection: 'row',
    alignItems: 'center',
  },
  shareQRButtonText: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.white,
    marginLeft: Spacing.sm,
  },
});

export default KeyManagementScreen;
