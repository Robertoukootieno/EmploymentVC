/**
 * Login Screen
 * 
 * Handles user authentication with multiple methods
 */

import React, {useState, useEffect} from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  Alert,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
} from 'react-native';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import Icon from 'react-native-vector-icons/MaterialIcons';
import LinearGradient from 'react-native-linear-gradient';

// Redux
import {useAppDispatch, useAppSelector} from '../store';
import {login, loginWithWeb3, loginWithBiometric, loginWithPIN} from '../store/slices/authSlice';

// Components
import LoadingSpinner from '../components/LoadingSpinner';
import BiometricButton from '../components/BiometricButton';
import Web3ConnectButton from '../components/Web3ConnectButton';

// Services
import {biometricService} from '../services/biometricService';

// Styles
import {Colors} from '../styles/colors';
import {Typography} from '../styles/typography';
import {Spacing} from '../styles/spacing';

const LoginScreen: React.FC = () => {
  const insets = useSafeAreaInsets();
  const dispatch = useAppDispatch();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loginMethod, setLoginMethod] = useState<'email' | 'web3' | 'biometric' | 'pin'>('email');
  const [pin, setPin] = useState('');
  const [biometricAvailable, setBiometricAvailable] = useState(false);
  const [hasStoredCredentials, setHasStoredCredentials] = useState(false);

  const {loading, error} = useAppSelector((state) => state.auth);

  useEffect(() => {
    checkBiometricAvailability();
    checkStoredCredentials();
  }, []);

  const checkBiometricAvailability = async () => {
    const available = await biometricService.isAvailable();
    setBiometricAvailable(available);
  };

  const checkStoredCredentials = async () => {
    // Check if user has previously logged in and has stored credentials
    // This would check for refresh tokens or other stored auth data
    setHasStoredCredentials(false); // Placeholder
  };

  const handleEmailLogin = async () => {
    if (!email.trim() || !password.trim()) {
      Alert.alert('Error', 'Please enter both email and password');
      return;
    }

    try {
      await dispatch(login({email: email.trim(), password})).unwrap();
    } catch (error: any) {
      Alert.alert('Login Failed', error.message || 'Invalid credentials');
    }
  };

  const handleWeb3Login = async (walletAddress: string, signature: string, message: string) => {
    try {
      await dispatch(loginWithWeb3({
        walletAddress,
        signature,
        message,
      })).unwrap();
    } catch (error: any) {
      Alert.alert('Web3 Login Failed', error.message || 'Failed to authenticate with Web3 wallet');
    }
  };

  const handleBiometricLogin = async () => {
    try {
      await dispatch(loginWithBiometric()).unwrap();
    } catch (error: any) {
      Alert.alert('Biometric Login Failed', error.message || 'Biometric authentication failed');
    }
  };

  const handlePINLogin = async () => {
    if (pin.length !== 6) {
      Alert.alert('Error', 'Please enter a 6-digit PIN');
      return;
    }

    try {
      await dispatch(loginWithPIN(pin)).unwrap();
    } catch (error: any) {
      Alert.alert('PIN Login Failed', error.message || 'Invalid PIN');
      setPin('');
    }
  };

  const handleForgotPassword = () => {
    // Navigate to forgot password screen
    console.log('Forgot password');
  };

  const handleRegister = () => {
    // Navigate to registration screen
    console.log('Navigate to register');
  };

  const renderEmailLogin = () => (
    <View style={styles.formContainer}>
      <View style={styles.inputContainer}>
        <Icon name="email" size={20} color={Colors.textSecondary} style={styles.inputIcon} />
        <TextInput
          style={styles.textInput}
          placeholder="Email address"
          placeholderTextColor={Colors.textSecondary}
          value={email}
          onChangeText={setEmail}
          keyboardType="email-address"
          autoCapitalize="none"
          autoCorrect={false}
        />
      </View>

      <View style={styles.inputContainer}>
        <Icon name="lock" size={20} color={Colors.textSecondary} style={styles.inputIcon} />
        <TextInput
          style={styles.textInput}
          placeholder="Password"
          placeholderTextColor={Colors.textSecondary}
          value={password}
          onChangeText={setPassword}
          secureTextEntry={!showPassword}
          autoCapitalize="none"
          autoCorrect={false}
        />
        <TouchableOpacity
          style={styles.passwordToggle}
          onPress={() => setShowPassword(!showPassword)}
        >
          <Icon
            name={showPassword ? 'visibility-off' : 'visibility'}
            size={20}
            color={Colors.textSecondary}
          />
        </TouchableOpacity>
      </View>

      <TouchableOpacity style={styles.forgotPassword} onPress={handleForgotPassword}>
        <Text style={styles.forgotPasswordText}>Forgot Password?</Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={[styles.loginButton, loading.authenticating && styles.loginButtonDisabled]}
        onPress={handleEmailLogin}
        disabled={loading.authenticating}
      >
        {loading.authenticating ? (
          <LoadingSpinner size="small" color={Colors.white} />
        ) : (
          <Text style={styles.loginButtonText}>Sign In</Text>
        )}
      </TouchableOpacity>
    </View>
  );

  const renderPINLogin = () => (
    <View style={styles.formContainer}>
      <Text style={styles.pinTitle}>Enter your PIN</Text>
      <View style={styles.pinContainer}>
        <TextInput
          style={styles.pinInput}
          value={pin}
          onChangeText={setPin}
          keyboardType="numeric"
          maxLength={6}
          secureTextEntry
          placeholder="••••••"
          placeholderTextColor={Colors.textSecondary}
        />
      </View>
      
      <TouchableOpacity
        style={[styles.loginButton, (loading.authenticating || pin.length !== 6) && styles.loginButtonDisabled]}
        onPress={handlePINLogin}
        disabled={loading.authenticating || pin.length !== 6}
      >
        {loading.authenticating ? (
          <LoadingSpinner size="small" color={Colors.white} />
        ) : (
          <Text style={styles.loginButtonText}>Unlock</Text>
        )}
      </TouchableOpacity>
    </View>
  );

  const renderQuickLogin = () => (
    <View style={styles.quickLoginContainer}>
      <Text style={styles.quickLoginTitle}>Quick Access</Text>
      
      {biometricAvailable && (
        <BiometricButton
          onPress={handleBiometricLogin}
          loading={loading.authenticating}
        />
      )}
      
      <TouchableOpacity
        style={styles.methodButton}
        onPress={() => setLoginMethod('pin')}
      >
        <Icon name="pin" size={24} color={Colors.primary} />
        <Text style={styles.methodButtonText}>Use PIN</Text>
      </TouchableOpacity>
    </View>
  );

  return (
    <LinearGradient
      colors={[Colors.primary, Colors.primaryDark]}
      style={[styles.container, {paddingTop: insets.top}]}
    >
      <KeyboardAvoidingView
        style={styles.keyboardAvoid}
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      >
        <ScrollView contentContainerStyle={styles.scrollContent}>
          {/* Logo and Title */}
          <View style={styles.header}>
            <View style={styles.logoContainer}>
              <Icon name="account-balance-wallet" size={64} color={Colors.white} />
            </View>
            <Text style={styles.title}>Provenly Wallet</Text>
            <Text style={styles.subtitle}>Secure Employment Credentials</Text>
          </View>

          {/* Login Form */}
          <View style={styles.formCard}>
            {/* Method Selector */}
            {hasStoredCredentials && (
              <View style={styles.methodSelector}>
                <TouchableOpacity
                  style={[styles.methodTab, loginMethod === 'email' && styles.methodTabActive]}
                  onPress={() => setLoginMethod('email')}
                >
                  <Text style={[styles.methodTabText, loginMethod === 'email' && styles.methodTabTextActive]}>
                    Email
                  </Text>
                </TouchableOpacity>
                
                <TouchableOpacity
                  style={[styles.methodTab, loginMethod === 'web3' && styles.methodTabActive]}
                  onPress={() => setLoginMethod('web3')}
                >
                  <Text style={[styles.methodTabText, loginMethod === 'web3' && styles.methodTabTextActive]}>
                    Web3
                  </Text>
                </TouchableOpacity>
              </View>
            )}

            {/* Login Methods */}
            {loginMethod === 'email' && renderEmailLogin()}
            {loginMethod === 'pin' && renderPINLogin()}
            {loginMethod === 'web3' && (
              <Web3ConnectButton
                onConnect={handleWeb3Login}
                loading={loading.authenticating}
              />
            )}

            {/* Quick Login Options */}
            {loginMethod === 'email' && hasStoredCredentials && renderQuickLogin()}

            {/* Alternative Login Methods */}
            {loginMethod === 'email' && (
              <View style={styles.alternativeLogin}>
                <View style={styles.divider}>
                  <View style={styles.dividerLine} />
                  <Text style={styles.dividerText}>or</Text>
                  <View style={styles.dividerLine} />
                </View>

                <Web3ConnectButton
                  onConnect={handleWeb3Login}
                  loading={loading.authenticating}
                  variant="outline"
                />
              </View>
            )}

            {/* Register Link */}
            <View style={styles.registerContainer}>
              <Text style={styles.registerText}>Don't have an account? </Text>
              <TouchableOpacity onPress={handleRegister}>
                <Text style={styles.registerLink}>Sign Up</Text>
              </TouchableOpacity>
            </View>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </LinearGradient>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  keyboardAvoid: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
    justifyContent: 'center',
    padding: Spacing.lg,
  },
  header: {
    alignItems: 'center',
    marginBottom: Spacing.xl,
  },
  logoContainer: {
    width: 100,
    height: 100,
    borderRadius: 50,
    backgroundColor: 'rgba(255, 255, 255, 0.2)',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: Spacing.lg,
  },
  title: {
    fontSize: Typography.sizes.xxl,
    fontFamily: Typography.fonts.bold,
    color: Colors.white,
    marginBottom: Spacing.sm,
  },
  subtitle: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.regular,
    color: Colors.white,
    opacity: 0.9,
  },
  formCard: {
    backgroundColor: Colors.white,
    borderRadius: 16,
    padding: Spacing.xl,
    shadowColor: Colors.black,
    shadowOffset: {width: 0, height: 4},
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 8,
  },
  methodSelector: {
    flexDirection: 'row',
    backgroundColor: Colors.background,
    borderRadius: 8,
    padding: 4,
    marginBottom: Spacing.lg,
  },
  methodTab: {
    flex: 1,
    paddingVertical: Spacing.sm,
    alignItems: 'center',
    borderRadius: 6,
  },
  methodTabActive: {
    backgroundColor: Colors.white,
    shadowColor: Colors.black,
    shadowOffset: {width: 0, height: 1},
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  methodTabText: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.medium,
    color: Colors.textSecondary,
  },
  methodTabTextActive: {
    color: Colors.primary,
  },
  formContainer: {
    marginBottom: Spacing.lg,
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 8,
    marginBottom: Spacing.md,
    paddingHorizontal: Spacing.md,
    backgroundColor: Colors.background,
  },
  inputIcon: {
    marginRight: Spacing.sm,
  },
  textInput: {
    flex: 1,
    paddingVertical: Spacing.md,
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.regular,
    color: Colors.textPrimary,
  },
  passwordToggle: {
    padding: Spacing.sm,
  },
  forgotPassword: {
    alignSelf: 'flex-end',
    marginBottom: Spacing.lg,
  },
  forgotPasswordText: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.medium,
    color: Colors.primary,
  },
  loginButton: {
    backgroundColor: Colors.primary,
    borderRadius: 8,
    paddingVertical: Spacing.md,
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 48,
  },
  loginButtonDisabled: {
    backgroundColor: Colors.textSecondary,
  },
  loginButtonText: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.white,
  },
  pinTitle: {
    fontSize: Typography.sizes.lg,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.textPrimary,
    textAlign: 'center',
    marginBottom: Spacing.lg,
  },
  pinContainer: {
    alignItems: 'center',
    marginBottom: Spacing.lg,
  },
  pinInput: {
    fontSize: Typography.sizes.xl,
    fontFamily: Typography.fonts.bold,
    color: Colors.textPrimary,
    textAlign: 'center',
    letterSpacing: 8,
    borderBottomWidth: 2,
    borderBottomColor: Colors.primary,
    paddingVertical: Spacing.md,
    minWidth: 150,
  },
  quickLoginContainer: {
    alignItems: 'center',
    marginTop: Spacing.lg,
    paddingTop: Spacing.lg,
    borderTopWidth: 1,
    borderTopColor: Colors.border,
  },
  quickLoginTitle: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.textPrimary,
    marginBottom: Spacing.md,
  },
  methodButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: Spacing.sm,
    paddingHorizontal: Spacing.md,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: Colors.primary,
    marginTop: Spacing.sm,
  },
  methodButtonText: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.medium,
    color: Colors.primary,
    marginLeft: Spacing.sm,
  },
  alternativeLogin: {
    marginTop: Spacing.lg,
  },
  divider: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: Spacing.lg,
  },
  dividerLine: {
    flex: 1,
    height: 1,
    backgroundColor: Colors.border,
  },
  dividerText: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.regular,
    color: Colors.textSecondary,
    marginHorizontal: Spacing.md,
  },
  registerContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: Spacing.lg,
  },
  registerText: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.regular,
    color: Colors.textSecondary,
  },
  registerLink: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.primary,
  },
});

export default LoginScreen;
