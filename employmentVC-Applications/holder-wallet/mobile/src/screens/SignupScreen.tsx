/**
 * Signup Screen
 * 
 * User registration with multiple account types and DID creation options
 */

import React, {useState} from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  ScrollView,
  Alert,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import Icon from 'react-native-vector-icons/MaterialIcons';
import LinearGradient from 'react-native-linear-gradient';

// Redux
import {useAppDispatch, useAppSelector} from '../store';
import {register} from '../store/slices/authSlice';

// Components
import LoadingSpinner from '../components/LoadingSpinner';
import CheckBox from '../components/CheckBox';
import PasswordStrengthIndicator from '../components/PasswordStrengthIndicator';

// Services
import {didService} from '../services/didService';
import {keyService} from '../services/keyService';

// Types
import {WalletType} from '../types/wallet';
import {KeyType} from '../types/crypto';

// Styles
import {Colors} from '../styles/colors';
import {Typography} from '../styles/typography';
import {Spacing} from '../styles/spacing';

interface SignupFormData {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirmPassword: string;
  organization?: string;
  role: 'individual' | 'organization' | 'verifier';
  walletType: WalletType;
  didMethod: 'ebsi' | 'ethr' | 'key' | 'web';
  keyType: KeyType;
  acceptTerms: boolean;
  acceptPrivacy: boolean;
  enableBiometric: boolean;
}

const SignupScreen: React.FC = () => {
  const insets = useSafeAreaInsets();
  const dispatch = useAppDispatch();

  const [formData, setFormData] = useState<SignupFormData>({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: '',
    organization: '',
    role: 'individual',
    walletType: WalletType.CUSTODIAL,
    didMethod: 'ebsi',
    keyType: KeyType.SECP256R1,
    acceptTerms: false,
    acceptPrivacy: false,
    enableBiometric: false,
  });

  const [currentStep, setCurrentStep] = useState(1);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [passwordStrength, setPasswordStrength] = useState(0);

  const {loading, error} = useAppSelector((state) => state.auth);

  const updateFormData = (field: keyof SignupFormData, value: any) => {
    setFormData(prev => ({...prev, [field]: value}));
  };

  const validateStep1 = (): boolean => {
    if (!formData.firstName.trim()) {
      Alert.alert('Error', 'First name is required');
      return false;
    }
    if (!formData.lastName.trim()) {
      Alert.alert('Error', 'Last name is required');
      return false;
    }
    if (!formData.email.trim()) {
      Alert.alert('Error', 'Email is required');
      return false;
    }
    if (!/\S+@\S+\.\S+/.test(formData.email)) {
      Alert.alert('Error', 'Please enter a valid email address');
      return false;
    }
    if (formData.role === 'organization' && !formData.organization?.trim()) {
      Alert.alert('Error', 'Organization name is required');
      return false;
    }
    return true;
  };

  const validateStep2 = (): boolean => {
    if (formData.password.length < 8) {
      Alert.alert('Error', 'Password must be at least 8 characters long');
      return false;
    }
    if (formData.password !== formData.confirmPassword) {
      Alert.alert('Error', 'Passwords do not match');
      return false;
    }
    if (passwordStrength < 3) {
      Alert.alert('Error', 'Please choose a stronger password');
      return false;
    }
    return true;
  };

  const validateStep3 = (): boolean => {
    if (!formData.acceptTerms) {
      Alert.alert('Error', 'You must accept the Terms of Service');
      return false;
    }
    if (!formData.acceptPrivacy) {
      Alert.alert('Error', 'You must accept the Privacy Policy');
      return false;
    }
    return true;
  };

  const handleNext = () => {
    let isValid = false;
    
    switch (currentStep) {
      case 1:
        isValid = validateStep1();
        break;
      case 2:
        isValid = validateStep2();
        break;
      case 3:
        isValid = validateStep3();
        break;
    }

    if (isValid) {
      if (currentStep < 4) {
        setCurrentStep(currentStep + 1);
      } else {
        handleSignup();
      }
    }
  };

  const handleBack = () => {
    if (currentStep > 1) {
      setCurrentStep(currentStep - 1);
    }
  };

  const handleSignup = async () => {
    try {
      // Generate DID and keys based on selections
      const keyPair = await keyService.generateKeyPair(formData.keyType);
      const did = await didService.createDID(formData.didMethod, keyPair.publicKey);

      const registrationData = {
        firstName: formData.firstName,
        lastName: formData.lastName,
        email: formData.email,
        password: formData.password,
        organization: formData.organization,
        role: formData.role,
        walletType: formData.walletType,
        did: did.id,
        publicKey: keyPair.publicKey,
        keyType: formData.keyType,
        didMethod: formData.didMethod,
        enableBiometric: formData.enableBiometric,
        acceptTerms: formData.acceptTerms,
        acceptPrivacy: formData.acceptPrivacy,
      };

      await dispatch(register(registrationData)).unwrap();
      
      Alert.alert(
        'Registration Successful',
        'Your account has been created successfully. You can now start using your wallet.',
        [
          {
            text: 'Get Started',
            onPress: () => {
              // Navigation will be handled by the auth state change
            },
          },
        ]
      );
    } catch (error: any) {
      Alert.alert('Registration Failed', error.message || 'Failed to create account');
    }
  };

  const handlePasswordChange = (password: string) => {
    updateFormData('password', password);
    
    // Calculate password strength
    let strength = 0;
    if (password.length >= 8) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/[a-z]/.test(password)) strength++;
    if (/[0-9]/.test(password)) strength++;
    if (/[^A-Za-z0-9]/.test(password)) strength++;
    
    setPasswordStrength(strength);
  };

  const renderStep1 = () => (
    <View style={styles.stepContainer}>
      <Text style={styles.stepTitle}>Personal Information</Text>
      
      <View style={styles.inputContainer}>
        <Icon name="person" size={20} color={Colors.textSecondary} style={styles.inputIcon} />
        <TextInput
          style={styles.textInput}
          placeholder="First Name"
          placeholderTextColor={Colors.textSecondary}
          value={formData.firstName}
          onChangeText={(text) => updateFormData('firstName', text)}
          autoCapitalize="words"
        />
      </View>

      <View style={styles.inputContainer}>
        <Icon name="person" size={20} color={Colors.textSecondary} style={styles.inputIcon} />
        <TextInput
          style={styles.textInput}
          placeholder="Last Name"
          placeholderTextColor={Colors.textSecondary}
          value={formData.lastName}
          onChangeText={(text) => updateFormData('lastName', text)}
          autoCapitalize="words"
        />
      </View>

      <View style={styles.inputContainer}>
        <Icon name="email" size={20} color={Colors.textSecondary} style={styles.inputIcon} />
        <TextInput
          style={styles.textInput}
          placeholder="Email Address"
          placeholderTextColor={Colors.textSecondary}
          value={formData.email}
          onChangeText={(text) => updateFormData('email', text)}
          keyboardType="email-address"
          autoCapitalize="none"
          autoCorrect={false}
        />
      </View>

      <Text style={styles.sectionTitle}>Account Type</Text>
      <View style={styles.roleSelector}>
        {[
          {key: 'individual', label: 'Individual', icon: 'person'},
          {key: 'organization', label: 'Organization', icon: 'business'},
          {key: 'verifier', label: 'Verifier', icon: 'verified'},
        ].map((role) => (
          <TouchableOpacity
            key={role.key}
            style={[
              styles.roleOption,
              formData.role === role.key && styles.roleOptionSelected,
            ]}
            onPress={() => updateFormData('role', role.key)}
          >
            <Icon
              name={role.icon}
              size={24}
              color={formData.role === role.key ? Colors.primary : Colors.textSecondary}
            />
            <Text
              style={[
                styles.roleOptionText,
                formData.role === role.key && styles.roleOptionTextSelected,
              ]}
            >
              {role.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {formData.role === 'organization' && (
        <View style={styles.inputContainer}>
          <Icon name="business" size={20} color={Colors.textSecondary} style={styles.inputIcon} />
          <TextInput
            style={styles.textInput}
            placeholder="Organization Name"
            placeholderTextColor={Colors.textSecondary}
            value={formData.organization}
            onChangeText={(text) => updateFormData('organization', text)}
            autoCapitalize="words"
          />
        </View>
      )}
    </View>
  );

  const renderStep2 = () => (
    <View style={styles.stepContainer}>
      <Text style={styles.stepTitle}>Security Setup</Text>
      
      <View style={styles.inputContainer}>
        <Icon name="lock" size={20} color={Colors.textSecondary} style={styles.inputIcon} />
        <TextInput
          style={styles.textInput}
          placeholder="Password"
          placeholderTextColor={Colors.textSecondary}
          value={formData.password}
          onChangeText={handlePasswordChange}
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

      <PasswordStrengthIndicator strength={passwordStrength} />

      <View style={styles.inputContainer}>
        <Icon name="lock" size={20} color={Colors.textSecondary} style={styles.inputIcon} />
        <TextInput
          style={styles.textInput}
          placeholder="Confirm Password"
          placeholderTextColor={Colors.textSecondary}
          value={formData.confirmPassword}
          onChangeText={(text) => updateFormData('confirmPassword', text)}
          secureTextEntry={!showConfirmPassword}
          autoCapitalize="none"
          autoCorrect={false}
        />
        <TouchableOpacity
          style={styles.passwordToggle}
          onPress={() => setShowConfirmPassword(!showConfirmPassword)}
        >
          <Icon
            name={showConfirmPassword ? 'visibility-off' : 'visibility'}
            size={20}
            color={Colors.textSecondary}
          />
        </TouchableOpacity>
      </View>

      <CheckBox
        checked={formData.enableBiometric}
        onPress={() => updateFormData('enableBiometric', !formData.enableBiometric)}
        label="Enable biometric authentication"
        style={styles.checkbox}
      />
    </View>
  );

  const renderStep3 = () => (
    <View style={styles.stepContainer}>
      <Text style={styles.stepTitle}>Wallet Configuration</Text>
      
      <Text style={styles.sectionTitle}>Wallet Type</Text>
      <View style={styles.walletTypeSelector}>
        {[
          {
            key: WalletType.CUSTODIAL,
            label: 'Custodial Wallet',
            description: 'We manage your keys securely',
            icon: 'security',
          },
          {
            key: WalletType.NON_CUSTODIAL,
            label: 'Non-Custodial Wallet',
            description: 'You control your keys',
            icon: 'key',
          },
        ].map((wallet) => (
          <TouchableOpacity
            key={wallet.key}
            style={[
              styles.walletTypeOption,
              formData.walletType === wallet.key && styles.walletTypeOptionSelected,
            ]}
            onPress={() => updateFormData('walletType', wallet.key)}
          >
            <Icon
              name={wallet.icon}
              size={24}
              color={formData.walletType === wallet.key ? Colors.primary : Colors.textSecondary}
            />
            <View style={styles.walletTypeContent}>
              <Text
                style={[
                  styles.walletTypeLabel,
                  formData.walletType === wallet.key && styles.walletTypeLabelSelected,
                ]}
              >
                {wallet.label}
              </Text>
              <Text style={styles.walletTypeDescription}>{wallet.description}</Text>
            </View>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.sectionTitle}>DID Method</Text>
      <View style={styles.didMethodSelector}>
        {[
          {key: 'ebsi', label: 'EBSI', description: 'European Blockchain Services'},
          {key: 'ethr', label: 'Ethereum', description: 'Ethereum-based DID'},
          {key: 'key', label: 'Key-based', description: 'Simple key-based DID'},
          {key: 'web', label: 'Web DID', description: 'Web-based DID'},
        ].map((method) => (
          <TouchableOpacity
            key={method.key}
            style={[
              styles.didMethodOption,
              formData.didMethod === method.key && styles.didMethodOptionSelected,
            ]}
            onPress={() => updateFormData('didMethod', method.key)}
          >
            <Text
              style={[
                styles.didMethodLabel,
                formData.didMethod === method.key && styles.didMethodLabelSelected,
              ]}
            >
              {method.label}
            </Text>
            <Text style={styles.didMethodDescription}>{method.description}</Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.sectionTitle}>Key Type</Text>
      <View style={styles.keyTypeSelector}>
        {[
          {key: KeyType.SECP256R1, label: 'SECP256R1', description: 'NIST P-256 curve'},
          {key: KeyType.SECP256K1, label: 'SECP256K1', description: 'Bitcoin/Ethereum curve'},
          {key: KeyType.RSA, label: 'RSA-2048', description: 'RSA 2048-bit keys'},
          {key: KeyType.ED25519, label: 'Ed25519', description: 'Edwards curve'},
        ].map((keyType) => (
          <TouchableOpacity
            key={keyType.key}
            style={[
              styles.keyTypeOption,
              formData.keyType === keyType.key && styles.keyTypeOptionSelected,
            ]}
            onPress={() => updateFormData('keyType', keyType.key)}
          >
            <Text
              style={[
                styles.keyTypeLabel,
                formData.keyType === keyType.key && styles.keyTypeLabelSelected,
              ]}
            >
              {keyType.label}
            </Text>
            <Text style={styles.keyTypeDescription}>{keyType.description}</Text>
          </TouchableOpacity>
        ))}
      </View>
    </View>
  );

  const renderStep4 = () => (
    <View style={styles.stepContainer}>
      <Text style={styles.stepTitle}>Terms & Conditions</Text>
      
      <CheckBox
        checked={formData.acceptTerms}
        onPress={() => updateFormData('acceptTerms', !formData.acceptTerms)}
        label="I accept the Terms of Service"
        style={styles.checkbox}
      />

      <CheckBox
        checked={formData.acceptPrivacy}
        onPress={() => updateFormData('acceptPrivacy', !formData.acceptPrivacy)}
        label="I accept the Privacy Policy"
        style={styles.checkbox}
      />

      <View style={styles.summaryContainer}>
        <Text style={styles.summaryTitle}>Account Summary</Text>
        <Text style={styles.summaryText}>Name: {formData.firstName} {formData.lastName}</Text>
        <Text style={styles.summaryText}>Email: {formData.email}</Text>
        <Text style={styles.summaryText}>Role: {formData.role}</Text>
        <Text style={styles.summaryText}>Wallet: {formData.walletType}</Text>
        <Text style={styles.summaryText}>DID Method: {formData.didMethod}</Text>
        <Text style={styles.summaryText}>Key Type: {formData.keyType}</Text>
      </View>
    </View>
  );

  const renderStepIndicator = () => (
    <View style={styles.stepIndicator}>
      {[1, 2, 3, 4].map((step) => (
        <View key={step} style={styles.stepIndicatorContainer}>
          <View
            style={[
              styles.stepIndicatorDot,
              currentStep >= step && styles.stepIndicatorDotActive,
            ]}
          >
            <Text
              style={[
                styles.stepIndicatorText,
                currentStep >= step && styles.stepIndicatorTextActive,
              ]}
            >
              {step}
            </Text>
          </View>
          {step < 4 && (
            <View
              style={[
                styles.stepIndicatorLine,
                currentStep > step && styles.stepIndicatorLineActive,
              ]}
            />
          )}
        </View>
      ))}
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
          {/* Header */}
          <View style={styles.header}>
            <TouchableOpacity style={styles.backButton} onPress={handleBack}>
              <Icon name="arrow-back" size={24} color={Colors.white} />
            </TouchableOpacity>
            <Text style={styles.title}>Create Account</Text>
            <View style={styles.placeholder} />
          </View>

          {/* Step Indicator */}
          {renderStepIndicator()}

          {/* Form Card */}
          <View style={styles.formCard}>
            {currentStep === 1 && renderStep1()}
            {currentStep === 2 && renderStep2()}
            {currentStep === 3 && renderStep3()}
            {currentStep === 4 && renderStep4()}

            {/* Navigation Buttons */}
            <View style={styles.buttonContainer}>
              {currentStep > 1 && (
                <TouchableOpacity style={styles.backButtonSecondary} onPress={handleBack}>
                  <Text style={styles.backButtonText}>Back</Text>
                </TouchableOpacity>
              )}
              
              <TouchableOpacity
                style={[
                  styles.nextButton,
                  loading.authenticating && styles.nextButtonDisabled,
                ]}
                onPress={handleNext}
                disabled={loading.authenticating}
              >
                {loading.authenticating ? (
                  <LoadingSpinner size="small" color={Colors.white} />
                ) : (
                  <Text style={styles.nextButtonText}>
                    {currentStep === 4 ? 'Create Account' : 'Next'}
                  </Text>
                )}
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
    padding: Spacing.lg,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.lg,
  },
  backButton: {
    padding: Spacing.sm,
  },
  title: {
    fontSize: Typography.sizes.xl,
    fontFamily: Typography.fonts.bold,
    color: Colors.white,
  },
  placeholder: {
    width: 40,
  },
  stepIndicator: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: Spacing.xl,
  },
  stepIndicatorContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  stepIndicatorDot: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: 'rgba(255, 255, 255, 0.3)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  stepIndicatorDotActive: {
    backgroundColor: Colors.white,
  },
  stepIndicatorText: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.white,
  },
  stepIndicatorTextActive: {
    color: Colors.primary,
  },
  stepIndicatorLine: {
    width: 40,
    height: 2,
    backgroundColor: 'rgba(255, 255, 255, 0.3)',
    marginHorizontal: Spacing.sm,
  },
  stepIndicatorLineActive: {
    backgroundColor: Colors.white,
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
  stepContainer: {
    marginBottom: Spacing.lg,
  },
  stepTitle: {
    fontSize: Typography.sizes.lg,
    fontFamily: Typography.fonts.bold,
    color: Colors.textPrimary,
    marginBottom: Spacing.lg,
    textAlign: 'center',
  },
  sectionTitle: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.textPrimary,
    marginTop: Spacing.lg,
    marginBottom: Spacing.md,
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
  roleSelector: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: Spacing.md,
  },
  roleOption: {
    flex: 1,
    alignItems: 'center',
    padding: Spacing.md,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 8,
    marginHorizontal: Spacing.xs,
  },
  roleOptionSelected: {
    borderColor: Colors.primary,
    backgroundColor: Colors.primaryLight,
  },
  roleOptionText: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.medium,
    color: Colors.textSecondary,
    marginTop: Spacing.xs,
  },
  roleOptionTextSelected: {
    color: Colors.primary,
  },
  walletTypeSelector: {
    marginBottom: Spacing.md,
  },
  walletTypeOption: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: Spacing.md,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 8,
    marginBottom: Spacing.sm,
  },
  walletTypeOptionSelected: {
    borderColor: Colors.primary,
    backgroundColor: Colors.primaryLight,
  },
  walletTypeContent: {
    flex: 1,
    marginLeft: Spacing.md,
  },
  walletTypeLabel: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.textPrimary,
  },
  walletTypeLabelSelected: {
    color: Colors.primary,
  },
  walletTypeDescription: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.regular,
    color: Colors.textSecondary,
    marginTop: Spacing.xs,
  },
  didMethodSelector: {
    marginBottom: Spacing.md,
  },
  didMethodOption: {
    padding: Spacing.md,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 8,
    marginBottom: Spacing.sm,
  },
  didMethodOptionSelected: {
    borderColor: Colors.primary,
    backgroundColor: Colors.primaryLight,
  },
  didMethodLabel: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.textPrimary,
  },
  didMethodLabelSelected: {
    color: Colors.primary,
  },
  didMethodDescription: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.regular,
    color: Colors.textSecondary,
    marginTop: Spacing.xs,
  },
  keyTypeSelector: {
    marginBottom: Spacing.md,
  },
  keyTypeOption: {
    padding: Spacing.md,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 8,
    marginBottom: Spacing.sm,
  },
  keyTypeOptionSelected: {
    borderColor: Colors.primary,
    backgroundColor: Colors.primaryLight,
  },
  keyTypeLabel: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.textPrimary,
  },
  keyTypeLabelSelected: {
    color: Colors.primary,
  },
  keyTypeDescription: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.regular,
    color: Colors.textSecondary,
    marginTop: Spacing.xs,
  },
  checkbox: {
    marginBottom: Spacing.md,
  },
  summaryContainer: {
    backgroundColor: Colors.background,
    borderRadius: 8,
    padding: Spacing.md,
    marginTop: Spacing.lg,
  },
  summaryTitle: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.textPrimary,
    marginBottom: Spacing.sm,
  },
  summaryText: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.regular,
    color: Colors.textSecondary,
    marginBottom: Spacing.xs,
  },
  buttonContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: Spacing.lg,
  },
  backButtonSecondary: {
    paddingVertical: Spacing.md,
    paddingHorizontal: Spacing.lg,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  backButtonText: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.textSecondary,
  },
  nextButton: {
    backgroundColor: Colors.primary,
    paddingVertical: Spacing.md,
    paddingHorizontal: Spacing.xl,
    borderRadius: 8,
    minWidth: 120,
    alignItems: 'center',
    justifyContent: 'center',
  },
  nextButtonDisabled: {
    backgroundColor: Colors.textSecondary,
  },
  nextButtonText: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.white,
  },
});

export default SignupScreen;
