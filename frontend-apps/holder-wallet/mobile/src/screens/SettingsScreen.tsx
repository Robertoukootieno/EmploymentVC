/**
 * Settings Screen
 * 
 * App settings, security, and account management
 */

import React, {useState} from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  Switch,
  Alert,
  StyleSheet,
} from 'react-native';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import Icon from 'react-native-vector-icons/MaterialIcons';

// Redux
import {useAppSelector, useAppDispatch} from '../store';
import {logout} from '../store/slices/authSlice';
import {updateSettings} from '../store/slices/settingsSlice';

// Components
import SettingsSection from '../components/SettingsSection';
import SettingsItem from '../components/SettingsItem';
import UserProfile from '../components/UserProfile';

// Services
import {biometricService} from '../services/biometricService';
import {backupService} from '../services/backupService';

// Styles
import {Colors} from '../styles/colors';
import {Typography} from '../styles/typography';
import {Spacing} from '../styles/spacing';

const SettingsScreen: React.FC = () => {
  const insets = useSafeAreaInsets();
  const dispatch = useAppDispatch();

  const {user, isAuthenticated} = useAppSelector((state) => state.auth);
  const {activeWallet} = useAppSelector((state) => state.wallet);
  const {
    biometricEnabled,
    autoBackup,
    notifications,
    darkMode,
    language,
    currency,
  } = useAppSelector((state) => state.settings);

  const [biometricLoading, setBiometricLoading] = useState(false);

  const handleBiometricToggle = async (enabled: boolean) => {
    setBiometricLoading(true);
    try {
      if (enabled) {
        const isAvailable = await biometricService.isAvailable();
        if (!isAvailable) {
          Alert.alert(
            'Biometric Not Available',
            'Biometric authentication is not available on this device.'
          );
          return;
        }

        const result = await biometricService.authenticate('Enable biometric authentication');
        if (result.success) {
          dispatch(updateSettings({biometricEnabled: true}));
        } else {
          Alert.alert('Authentication Failed', result.error || 'Failed to enable biometric authentication');
        }
      } else {
        Alert.alert(
          'Disable Biometric Authentication',
          'Are you sure you want to disable biometric authentication? You will need to use your PIN to access the wallet.',
          [
            {text: 'Cancel', style: 'cancel'},
            {
              text: 'Disable',
              style: 'destructive',
              onPress: () => {
                dispatch(updateSettings({biometricEnabled: false}));
              },
            },
          ]
        );
      }
    } catch (error) {
      Alert.alert('Error', 'Failed to update biometric settings');
    } finally {
      setBiometricLoading(false);
    }
  };

  const handleAutoBackupToggle = async (enabled: boolean) => {
    if (enabled && activeWallet?.type === 'CUSTODIAL') {
      try {
        await backupService.enableAutoBackup(activeWallet.id);
        dispatch(updateSettings({autoBackup: true}));
      } catch (error) {
        Alert.alert('Error', 'Failed to enable auto backup');
      }
    } else {
      dispatch(updateSettings({autoBackup: enabled}));
    }
  };

  const handleCreateBackup = async () => {
    if (!activeWallet) {
      Alert.alert('No Active Wallet', 'Please select a wallet first.');
      return;
    }

    Alert.alert(
      'Create Backup',
      'This will create an encrypted backup of your wallet. Keep it safe!',
      [
        {text: 'Cancel', style: 'cancel'},
        {
          text: 'Create Backup',
          onPress: async () => {
            try {
              const backup = await backupService.createBackup(activeWallet.id);
              // Navigate to backup display screen
              console.log('Backup created:', backup);
            } catch (error) {
              Alert.alert('Error', 'Failed to create backup');
            }
          },
        },
      ]
    );
  };

  const handleRestoreBackup = () => {
    // Navigate to backup restoration flow
    console.log('Restore from backup');
  };

  const handleExportData = () => {
    Alert.alert(
      'Export Data',
      'Choose export format:',
      [
        {text: 'Cancel', style: 'cancel'},
        {text: 'JSON', onPress: () => console.log('Export as JSON')},
        {text: 'CSV', onPress: () => console.log('Export as CSV')},
      ]
    );
  };

  const handleDeleteAccount = () => {
    Alert.alert(
      'Delete Account',
      'This will permanently delete your account and all associated data. This action cannot be undone.',
      [
        {text: 'Cancel', style: 'cancel'},
        {
          text: 'Delete',
          style: 'destructive',
          onPress: () => {
            Alert.alert(
              'Final Confirmation',
              'Type "DELETE" to confirm account deletion:',
              [
                {text: 'Cancel', style: 'cancel'},
                {
                  text: 'Confirm',
                  style: 'destructive',
                  onPress: () => {
                    // Handle account deletion
                    console.log('Delete account');
                  },
                },
              ]
            );
          },
        },
      ]
    );
  };

  const handleLogout = () => {
    Alert.alert(
      'Sign Out',
      'Are you sure you want to sign out?',
      [
        {text: 'Cancel', style: 'cancel'},
        {
          text: 'Sign Out',
          style: 'destructive',
          onPress: () => {
            dispatch(logout());
          },
        },
      ]
    );
  };

  return (
    <ScrollView style={[styles.container, {paddingTop: insets.top}]}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Settings</Text>
      </View>

      {/* User Profile */}
      {isAuthenticated && user && (
        <UserProfile
          user={user}
          onEditProfile={() => console.log('Edit profile')}
        />
      )}

      {/* Security Settings */}
      <SettingsSection title="Security" icon="security">
        <SettingsItem
          title="Biometric Authentication"
          subtitle={biometricEnabled ? 'Enabled' : 'Disabled'}
          icon="fingerprint"
          rightComponent={
            <Switch
              value={biometricEnabled}
              onValueChange={handleBiometricToggle}
              disabled={biometricLoading}
              trackColor={{false: Colors.border, true: Colors.primary}}
              thumbColor={biometricEnabled ? Colors.white : Colors.textSecondary}
            />
          }
        />
        
        <SettingsItem
          title="Change PIN"
          subtitle="Update your wallet PIN"
          icon="lock"
          onPress={() => console.log('Change PIN')}
          showArrow
        />
        
        <SettingsItem
          title="Session Timeout"
          subtitle="Auto-lock after inactivity"
          icon="timer"
          onPress={() => console.log('Session timeout')}
          showArrow
        />
      </SettingsSection>

      {/* Backup & Recovery */}
      <SettingsSection title="Backup & Recovery" icon="backup">
        <SettingsItem
          title="Auto Backup"
          subtitle={autoBackup ? 'Enabled' : 'Disabled'}
          icon="cloud-upload"
          rightComponent={
            <Switch
              value={autoBackup}
              onValueChange={handleAutoBackupToggle}
              trackColor={{false: Colors.border, true: Colors.primary}}
              thumbColor={autoBackup ? Colors.white : Colors.textSecondary}
            />
          }
        />
        
        <SettingsItem
          title="Create Backup"
          subtitle="Backup your wallet data"
          icon="save"
          onPress={handleCreateBackup}
          showArrow
        />
        
        <SettingsItem
          title="Restore from Backup"
          subtitle="Restore wallet from backup"
          icon="restore"
          onPress={handleRestoreBackup}
          showArrow
        />
      </SettingsSection>

      {/* Preferences */}
      <SettingsSection title="Preferences" icon="tune">
        <SettingsItem
          title="Notifications"
          subtitle={notifications ? 'Enabled' : 'Disabled'}
          icon="notifications"
          rightComponent={
            <Switch
              value={notifications}
              onValueChange={(enabled) => dispatch(updateSettings({notifications: enabled}))}
              trackColor={{false: Colors.border, true: Colors.primary}}
              thumbColor={notifications ? Colors.white : Colors.textSecondary}
            />
          }
        />
        
        <SettingsItem
          title="Dark Mode"
          subtitle={darkMode ? 'Enabled' : 'Disabled'}
          icon="dark-mode"
          rightComponent={
            <Switch
              value={darkMode}
              onValueChange={(enabled) => dispatch(updateSettings({darkMode: enabled}))}
              trackColor={{false: Colors.border, true: Colors.primary}}
              thumbColor={darkMode ? Colors.white : Colors.textSecondary}
            />
          }
        />
        
        <SettingsItem
          title="Language"
          subtitle={language}
          icon="language"
          onPress={() => console.log('Change language')}
          showArrow
        />
        
        <SettingsItem
          title="Currency"
          subtitle={currency}
          icon="attach-money"
          onPress={() => console.log('Change currency')}
          showArrow
        />
      </SettingsSection>

      {/* Data Management */}
      <SettingsSection title="Data Management" icon="storage">
        <SettingsItem
          title="Export Data"
          subtitle="Export your credentials and data"
          icon="download"
          onPress={handleExportData}
          showArrow
        />
        
        <SettingsItem
          title="Clear Cache"
          subtitle="Clear temporary data"
          icon="clear"
          onPress={() => console.log('Clear cache')}
          showArrow
        />
      </SettingsSection>

      {/* Support */}
      <SettingsSection title="Support" icon="help">
        <SettingsItem
          title="Help Center"
          subtitle="Get help and support"
          icon="help-center"
          onPress={() => console.log('Help center')}
          showArrow
        />
        
        <SettingsItem
          title="Contact Support"
          subtitle="Get in touch with our team"
          icon="support"
          onPress={() => console.log('Contact support')}
          showArrow
        />
        
        <SettingsItem
          title="Privacy Policy"
          subtitle="Read our privacy policy"
          icon="privacy-tip"
          onPress={() => console.log('Privacy policy')}
          showArrow
        />
        
        <SettingsItem
          title="Terms of Service"
          subtitle="Read our terms of service"
          icon="description"
          onPress={() => console.log('Terms of service')}
          showArrow
        />
      </SettingsSection>

      {/* Account Actions */}
      <SettingsSection title="Account" icon="account-circle">
        <SettingsItem
          title="Sign Out"
          subtitle="Sign out of your account"
          icon="logout"
          onPress={handleLogout}
          showArrow
          textColor={Colors.warning}
        />
        
        <SettingsItem
          title="Delete Account"
          subtitle="Permanently delete your account"
          icon="delete-forever"
          onPress={handleDeleteAccount}
          showArrow
          textColor={Colors.error}
        />
      </SettingsSection>

      {/* App Info */}
      <View style={styles.appInfo}>
        <Text style={styles.appInfoText}>Provenly Wallet v1.0.0</Text>
        <Text style={styles.appInfoText}>Build 1.0.0 (1)</Text>
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  header: {
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
  appInfo: {
    alignItems: 'center',
    paddingVertical: Spacing.xl,
    paddingHorizontal: Spacing.lg,
  },
  appInfoText: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.regular,
    color: Colors.textSecondary,
    marginBottom: Spacing.xs,
  },
});

export default SettingsScreen;
