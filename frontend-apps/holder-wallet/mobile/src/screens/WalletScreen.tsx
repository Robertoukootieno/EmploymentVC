/**
 * Main Wallet Screen
 * 
 * Displays wallet overview, balance, and quick actions
 */

import React, {useEffect, useState} from 'react';
import {
  View,
  Text,
  ScrollView,
  RefreshControl,
  TouchableOpacity,
  Alert,
  StyleSheet,
} from 'react-native';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import Icon from 'react-native-vector-icons/MaterialIcons';
import LinearGradient from 'react-native-linear-gradient';

// Redux
import {useAppSelector, useAppDispatch} from '../store';
import {fetchWallets, fetchWalletDetails} from '../store/slices/walletSlice';

// Components
import WalletCard from '../components/WalletCard';
import QuickActionButton from '../components/QuickActionButton';
import CredentialSummary from '../components/CredentialSummary';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorMessage from '../components/ErrorMessage';

// Types
import {WalletType} from '../types/wallet';

// Styles
import {Colors} from '../styles/colors';
import {Typography} from '../styles/typography';
import {Spacing} from '../styles/spacing';

const WalletScreen: React.FC = () => {
  const insets = useSafeAreaInsets();
  const dispatch = useAppDispatch();
  
  const [refreshing, setRefreshing] = useState(false);

  const {
    activeWallet,
    wallets,
    walletDetails,
    loading,
    error,
  } = useAppSelector((state) => state.wallet);

  const {credentials} = useAppSelector((state) => state.credential);

  useEffect(() => {
    // Load wallets on mount
    dispatch(fetchWallets());
  }, [dispatch]);

  useEffect(() => {
    // Load wallet details when active wallet changes
    if (activeWallet) {
      dispatch(fetchWalletDetails(activeWallet.id));
    }
  }, [activeWallet, dispatch]);

  const handleRefresh = async () => {
    setRefreshing(true);
    try {
      await dispatch(fetchWallets()).unwrap();
      if (activeWallet) {
        await dispatch(fetchWalletDetails(activeWallet.id)).unwrap();
      }
    } catch (error) {
      console.error('Failed to refresh wallet data:', error);
    } finally {
      setRefreshing(false);
    }
  };

  const handleCreateWallet = () => {
    Alert.alert(
      'Create New Wallet',
      'Choose wallet type:',
      [
        {
          text: 'Custodial',
          onPress: () => {
            // Navigate to custodial wallet creation
            console.log('Create custodial wallet');
          },
        },
        {
          text: 'Non-Custodial',
          onPress: () => {
            // Navigate to non-custodial wallet creation
            console.log('Create non-custodial wallet');
          },
        },
        {text: 'Cancel', style: 'cancel'},
      ]
    );
  };

  const handleScanQR = () => {
    // Navigate to QR scanner
    console.log('Open QR scanner');
  };

  const handleReceiveCredential = () => {
    // Navigate to credential receiving flow
    console.log('Receive credential');
  };

  const handleCreatePresentation = () => {
    // Navigate to presentation creation
    console.log('Create presentation');
  };

  if (loading.fetching && !activeWallet) {
    return (
      <View style={[styles.container, styles.centered]}>
        <LoadingSpinner size="large" />
        <Text style={styles.loadingText}>Loading your wallet...</Text>
      </View>
    );
  }

  if (error && !activeWallet) {
    return (
      <View style={[styles.container, styles.centered]}>
        <ErrorMessage
          message={error}
          onRetry={() => dispatch(fetchWallets())}
        />
      </View>
    );
  }

  if (!activeWallet) {
    return (
      <View style={[styles.container, styles.centered]}>
        <Icon name="account-balance-wallet" size={64} color={Colors.textSecondary} />
        <Text style={styles.emptyTitle}>No Wallet Found</Text>
        <Text style={styles.emptySubtitle}>
          Create your first wallet to start managing credentials
        </Text>
        <TouchableOpacity style={styles.createButton} onPress={handleCreateWallet}>
          <Text style={styles.createButtonText}>Create Wallet</Text>
        </TouchableOpacity>
      </View>
    );
  }

  const walletDetail = walletDetails[activeWallet.id];
  const activeCredentials = credentials.filter(c => c.metadata.status === 'ACTIVE');

  return (
    <ScrollView
      style={[styles.container, {paddingTop: insets.top}]}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />
      }
    >
      {/* Header */}
      <LinearGradient
        colors={[Colors.primary, Colors.primaryDark]}
        style={styles.header}
      >
        <View style={styles.headerContent}>
          <Text style={styles.headerTitle}>My Wallet</Text>
          <TouchableOpacity style={styles.headerAction}>
            <Icon name="more-vert" size={24} color={Colors.white} />
          </TouchableOpacity>
        </View>
      </LinearGradient>

      {/* Wallet Card */}
      <View style={styles.walletCardContainer}>
        <WalletCard
          wallet={activeWallet}
          details={walletDetail}
          onPress={() => {
            // Navigate to wallet details
            console.log('Open wallet details');
          }}
        />
      </View>

      {/* Quick Actions */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Quick Actions</Text>
        <View style={styles.quickActions}>
          <QuickActionButton
            icon="qr-code-scanner"
            label="Scan QR"
            onPress={handleScanQR}
          />
          <QuickActionButton
            icon="download"
            label="Receive"
            onPress={handleReceiveCredential}
          />
          <QuickActionButton
            icon="share"
            label="Present"
            onPress={handleCreatePresentation}
          />
          <QuickActionButton
            icon="add"
            label="New Wallet"
            onPress={handleCreateWallet}
          />
        </View>
      </View>

      {/* Credentials Summary */}
      <View style={styles.section}>
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Credentials</Text>
          <TouchableOpacity>
            <Text style={styles.seeAllText}>See All</Text>
          </TouchableOpacity>
        </View>
        <CredentialSummary
          totalCount={activeWallet.credentialCount}
          activeCount={activeCredentials.length}
          recentCredentials={activeCredentials.slice(0, 3)}
        />
      </View>

      {/* Wallet Type Info */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Wallet Information</Text>
        <View style={styles.infoCard}>
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>Type</Text>
            <View style={styles.typeContainer}>
              <Icon
                name={activeWallet.type === WalletType.CUSTODIAL ? 'security' : 'key'}
                size={16}
                color={activeWallet.type === WalletType.CUSTODIAL ? Colors.success : Colors.warning}
              />
              <Text style={[
                styles.infoValue,
                {color: activeWallet.type === WalletType.CUSTODIAL ? Colors.success : Colors.warning}
              ]}>
                {activeWallet.type === WalletType.CUSTODIAL ? 'Custodial' : 'Non-Custodial'}
              </Text>
            </View>
          </View>
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>Status</Text>
            <Text style={[styles.infoValue, {color: Colors.success}]}>
              {activeWallet.status}
            </Text>
          </View>
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>Created</Text>
            <Text style={styles.infoValue}>
              {new Date(activeWallet.metadata.createdAt).toLocaleDateString()}
            </Text>
          </View>
        </View>
      </View>
    </ScrollView>
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
    padding: Spacing.lg,
  },
  header: {
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.md,
  },
  headerContent: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: Typography.sizes.xl,
    fontFamily: Typography.fonts.bold,
    color: Colors.white,
  },
  headerAction: {
    padding: Spacing.xs,
  },
  walletCardContainer: {
    paddingHorizontal: Spacing.lg,
    marginTop: -Spacing.lg,
  },
  section: {
    padding: Spacing.lg,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.md,
  },
  sectionTitle: {
    fontSize: Typography.sizes.lg,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.textPrimary,
  },
  seeAllText: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.medium,
    color: Colors.primary,
  },
  quickActions: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: Spacing.md,
  },
  loadingText: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.regular,
    color: Colors.textSecondary,
    marginTop: Spacing.md,
  },
  emptyTitle: {
    fontSize: Typography.sizes.xl,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.textPrimary,
    marginTop: Spacing.md,
    textAlign: 'center',
  },
  emptySubtitle: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.regular,
    color: Colors.textSecondary,
    marginTop: Spacing.sm,
    textAlign: 'center',
    lineHeight: 22,
  },
  createButton: {
    backgroundColor: Colors.primary,
    paddingHorizontal: Spacing.xl,
    paddingVertical: Spacing.md,
    borderRadius: 8,
    marginTop: Spacing.lg,
  },
  createButtonText: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.white,
  },
  infoCard: {
    backgroundColor: Colors.surface,
    borderRadius: 12,
    padding: Spacing.md,
    marginTop: Spacing.sm,
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
  },
  typeContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xs,
  },
});

export default WalletScreen;
