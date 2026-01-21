/**
 * Credentials Screen
 * 
 * Displays and manages all user credentials
 */

import React, {useEffect, useState, useCallback} from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  RefreshControl,
  StyleSheet,
  Alert,
} from 'react-native';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import Icon from 'react-native-vector-icons/MaterialIcons';

// Redux
import {useAppSelector, useAppDispatch} from '../store';
import {fetchCredentials, deleteCredential} from '../store/slices/credentialSlice';

// Components
import CredentialCard from '../components/CredentialCard';
import SearchBar from '../components/SearchBar';
import FilterChips from '../components/FilterChips';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorMessage from '../components/ErrorMessage';
import EmptyState from '../components/EmptyState';

// Types
import {StoredCredential, CredentialStatus, CredentialType} from '../types/credential';

// Styles
import {Colors} from '../styles/colors';
import {Typography} from '../styles/typography';
import {Spacing} from '../styles/spacing';

const CredentialsScreen: React.FC = () => {
  const insets = useSafeAreaInsets();
  const dispatch = useAppDispatch();

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedFilters, setSelectedFilters] = useState<string[]>([]);
  const [refreshing, setRefreshing] = useState(false);

  const {
    credentials,
    loading,
    error,
    pagination,
  } = useAppSelector((state) => state.credential);

  const {activeWallet} = useAppSelector((state) => state.wallet);

  useEffect(() => {
    if (activeWallet) {
      loadCredentials();
    }
  }, [activeWallet]);

  const loadCredentials = useCallback(async () => {
    if (!activeWallet) return;

    try {
      await dispatch(fetchCredentials({
        walletId: activeWallet.id,
        page: 0,
        size: 20,
        search: searchQuery,
        status: selectedFilters.includes('expired') ? CredentialStatus.EXPIRED : undefined,
        type: selectedFilters.includes('employment') ? CredentialType.EMPLOYMENT_VERIFICATION : undefined,
      })).unwrap();
    } catch (error) {
      console.error('Failed to load credentials:', error);
    }
  }, [activeWallet, searchQuery, selectedFilters, dispatch]);

  const handleRefresh = async () => {
    setRefreshing(true);
    await loadCredentials();
    setRefreshing(false);
  };

  const handleSearch = (query: string) => {
    setSearchQuery(query);
  };

  const handleFilterChange = (filters: string[]) => {
    setSelectedFilters(filters);
  };

  const handleCredentialPress = (credential: StoredCredential) => {
    // Navigate to credential details
    console.log('Open credential details:', credential.metadata.id);
  };

  const handleCredentialLongPress = (credential: StoredCredential) => {
    Alert.alert(
      'Credential Actions',
      `What would you like to do with "${credential.metadata.title}"?`,
      [
        {
          text: 'View Details',
          onPress: () => handleCredentialPress(credential),
        },
        {
          text: 'Create Presentation',
          onPress: () => handleCreatePresentation(credential),
        },
        {
          text: 'Share',
          onPress: () => handleShareCredential(credential),
        },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: () => handleDeleteCredential(credential),
        },
        {text: 'Cancel', style: 'cancel'},
      ]
    );
  };

  const handleCreatePresentation = (credential: StoredCredential) => {
    // Navigate to presentation creation
    console.log('Create presentation for:', credential.metadata.id);
  };

  const handleShareCredential = (credential: StoredCredential) => {
    // Share credential via QR code or link
    console.log('Share credential:', credential.metadata.id);
  };

  const handleDeleteCredential = (credential: StoredCredential) => {
    Alert.alert(
      'Delete Credential',
      `Are you sure you want to delete "${credential.metadata.title}"? This action cannot be undone.`,
      [
        {
          text: 'Cancel',
          style: 'cancel',
        },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            try {
              await dispatch(deleteCredential({
                walletId: activeWallet!.id,
                credentialId: credential.metadata.credentialId,
              })).unwrap();
            } catch (error) {
              Alert.alert('Error', 'Failed to delete credential');
            }
          },
        },
      ]
    );
  };

  const handleAddCredential = () => {
    Alert.alert(
      'Add Credential',
      'How would you like to add a credential?',
      [
        {
          text: 'Scan QR Code',
          onPress: () => {
            // Navigate to QR scanner
            console.log('Open QR scanner');
          },
        },
        {
          text: 'Import File',
          onPress: () => {
            // Open file picker
            console.log('Import from file');
          },
        },
        {
          text: 'Manual Entry',
          onPress: () => {
            // Navigate to manual entry form
            console.log('Manual entry');
          },
        },
        {text: 'Cancel', style: 'cancel'},
      ]
    );
  };

  const renderCredentialItem = ({item}: {item: StoredCredential}) => (
    <CredentialCard
      credential={item}
      onPress={() => handleCredentialPress(item)}
      onLongPress={() => handleCredentialLongPress(item)}
    />
  );

  const filterOptions = [
    {id: 'all', label: 'All', count: credentials.length},
    {id: 'active', label: 'Active', count: credentials.filter(c => c.metadata.status === CredentialStatus.ACTIVE).length},
    {id: 'expired', label: 'Expired', count: credentials.filter(c => c.metadata.status === CredentialStatus.EXPIRED).length},
    {id: 'employment', label: 'Employment', count: credentials.filter(c => c.metadata.type === CredentialType.EMPLOYMENT_VERIFICATION).length},
    {id: 'education', label: 'Education', count: credentials.filter(c => c.metadata.type === CredentialType.EDUCATION_CREDENTIAL).length},
  ];

  if (loading.fetching && credentials.length === 0) {
    return (
      <View style={[styles.container, styles.centered]}>
        <LoadingSpinner size="large" />
        <Text style={styles.loadingText}>Loading credentials...</Text>
      </View>
    );
  }

  if (error && credentials.length === 0) {
    return (
      <View style={[styles.container, styles.centered]}>
        <ErrorMessage
          message={error}
          onRetry={loadCredentials}
        />
      </View>
    );
  }

  return (
    <View style={[styles.container, {paddingTop: insets.top}]}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>My Credentials</Text>
        <TouchableOpacity style={styles.addButton} onPress={handleAddCredential}>
          <Icon name="add" size={24} color={Colors.white} />
        </TouchableOpacity>
      </View>

      {/* Search Bar */}
      <View style={styles.searchContainer}>
        <SearchBar
          placeholder="Search credentials..."
          value={searchQuery}
          onChangeText={handleSearch}
          onSubmit={loadCredentials}
        />
      </View>

      {/* Filter Chips */}
      <View style={styles.filtersContainer}>
        <FilterChips
          options={filterOptions}
          selectedFilters={selectedFilters}
          onFiltersChange={handleFilterChange}
        />
      </View>

      {/* Credentials List */}
      {credentials.length === 0 ? (
        <EmptyState
          icon="badge"
          title="No Credentials Found"
          subtitle={searchQuery || selectedFilters.length > 0 
            ? "Try adjusting your search or filters"
            : "Add your first credential to get started"
          }
          actionText="Add Credential"
          onAction={handleAddCredential}
        />
      ) : (
        <FlatList
          data={credentials}
          renderItem={renderCredentialItem}
          keyExtractor={(item) => item.metadata.id}
          contentContainerStyle={styles.listContainer}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />
          }
          showsVerticalScrollIndicator={false}
          ItemSeparatorComponent={() => <View style={styles.separator} />}
        />
      )}

      {/* Stats Footer */}
      <View style={styles.footer}>
        <Text style={styles.footerText}>
          {credentials.length} credential{credentials.length !== 1 ? 's' : ''} total
        </Text>
        <Text style={styles.footerText}>
          {credentials.filter(c => c.metadata.status === CredentialStatus.ACTIVE).length} active
        </Text>
      </View>
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
    padding: Spacing.lg,
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
  filtersContainer: {
    paddingHorizontal: Spacing.lg,
    paddingBottom: Spacing.md,
  },
  listContainer: {
    paddingHorizontal: Spacing.lg,
    paddingBottom: Spacing.xl,
  },
  separator: {
    height: Spacing.md,
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.md,
    backgroundColor: Colors.surface,
    borderTopWidth: 1,
    borderTopColor: Colors.border,
  },
  footerText: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.medium,
    color: Colors.textSecondary,
  },
  loadingText: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.regular,
    color: Colors.textSecondary,
    marginTop: Spacing.md,
  },
});

export default CredentialsScreen;
