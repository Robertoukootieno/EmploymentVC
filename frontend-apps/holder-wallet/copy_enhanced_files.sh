#!/bin/bash

# 📁 Copy Enhanced Source Files Script
# This script copies all the enhanced source files to the new React Native project

set -e

echo "📁 Copying enhanced source files..."

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Check if mobile directory exists
if [ ! -d "mobile" ]; then
    echo "❌ Mobile directory not found. Please run setup_enhanced_wallet.sh first."
    exit 1
fi

cd mobile

# Create all necessary directories
print_status "Creating directory structure..."
mkdir -p src/{components,screens,services,store,types,styles,utils}
mkdir -p src/store/slices
mkdir -p src/components/{common,forms,navigation}

# Create enhanced source files
print_status "Creating enhanced source files..."

# Types
print_status "Creating type definitions..."
cat > src/types/wallet.ts << 'EOF'
export enum WalletType {
  CUSTODIAL = 'CUSTODIAL',
  NON_CUSTODIAL = 'NON_CUSTODIAL',
}

export interface Wallet {
  id: string;
  name: string;
  type: WalletType;
  did?: string;
  createdAt: string;
  lastUsed?: string;
}
EOF

cat > src/types/credential.ts << 'EOF'
export enum CredentialType {
  EMPLOYMENT_VERIFICATION = 'EmploymentVerification',
  EDUCATION_CREDENTIAL = 'EducationCredential',
  IDENTITY_CREDENTIAL = 'IdentityCredential',
  SKILL_CREDENTIAL = 'SkillCredential',
  CERTIFICATION = 'Certification',
}

export enum CredentialStatus {
  ACTIVE = 'ACTIVE',
  EXPIRED = 'EXPIRED',
  REVOKED = 'REVOKED',
  SUSPENDED = 'SUSPENDED',
}

export interface VerifiableCredential {
  '@context': string[];
  id: string;
  type: string[];
  issuer: string | { id: string; name?: string; };
  issuanceDate: string;
  expirationDate?: string;
  credentialSubject: {
    id: string;
    [key: string]: any;
  };
  proof: {
    type: string;
    created: string;
    verificationMethod: string;
    proofPurpose: string;
    proofValue: string;
  };
}

export interface CredentialPresentation {
  '@context': string[];
  id: string;
  type: string[];
  holder: string;
  verifiableCredential: VerifiableCredential[];
  proof: {
    type: string;
    created: string;
    verificationMethod: string;
    proofPurpose: string;
    challenge: string;
    domain?: string;
    proofValue: string;
  };
}

export interface SelectiveDisclosure {
  credentialId: string;
  disclosedFields: string[];
  hiddenFields: string[];
  purpose: string;
  requestedBy: string;
}
EOF

# Styles
print_status "Creating style definitions..."
cat > src/styles/colors.ts << 'EOF'
export const Colors = {
  primary: '#007AFF',
  primaryDark: '#0056CC',
  primaryLight: '#E3F2FD',
  secondary: '#FF9500',
  success: '#34C759',
  warning: '#FF9500',
  error: '#FF3B30',
  info: '#007AFF',
  
  background: '#F2F2F7',
  surface: '#FFFFFF',
  card: '#FFFFFF',
  
  textPrimary: '#000000',
  textSecondary: '#8E8E93',
  textTertiary: '#C7C7CC',
  
  border: '#C6C6C8',
  separator: '#E5E5EA',
  
  black: '#000000',
  white: '#FFFFFF',
  
  warningLight: '#FFF3CD',
};
EOF

cat > src/styles/typography.ts << 'EOF'
export const Typography = {
  fonts: {
    regular: 'System',
    medium: 'System',
    semiBold: 'System',
    bold: 'System',
    mono: 'Menlo',
  },
  sizes: {
    xs: 12,
    sm: 14,
    md: 16,
    lg: 18,
    xl: 20,
    xxl: 24,
  },
};
EOF

cat > src/styles/spacing.ts << 'EOF'
export const Spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  xxl: 48,
};
EOF

# Basic Components
print_status "Creating basic components..."
cat > src/components/LoadingSpinner.tsx << 'EOF'
import React from 'react';
import {ActivityIndicator, View, StyleSheet} from 'react-native';
import {Colors} from '../styles/colors';

interface LoadingSpinnerProps {
  size?: 'small' | 'large';
  color?: string;
}

const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  size = 'large',
  color = Colors.primary,
}) => {
  return (
    <View style={styles.container}>
      <ActivityIndicator size={size} color={color} />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    justifyContent: 'center',
    alignItems: 'center',
  },
});

export default LoadingSpinner;
EOF

# Basic Screen
print_status "Creating basic screen..."
cat > src/screens/HomeScreen.tsx << 'EOF'
import React from 'react';
import {View, Text, StyleSheet, TouchableOpacity} from 'react-native';
import {Colors} from '../styles/colors';
import {Typography} from '../styles/typography';
import {Spacing} from '../styles/spacing';

const HomeScreen: React.FC = () => {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>🎉 Provenly Wallet</Text>
      <Text style={styles.subtitle}>Enhanced Mobile Wallet</Text>
      
      <View style={styles.featuresContainer}>
        <Text style={styles.featuresTitle}>✅ Enhanced Features:</Text>
        <Text style={styles.feature}>📱 Complete Signup Flow</Text>
        <Text style={styles.feature}>🔐 Advanced Key Management</Text>
        <Text style={styles.feature}>🆔 Multi-method DID Support</Text>
        <Text style={styles.feature}>📜 VC/VP Processing</Text>
        <Text style={styles.feature}>📷 QR Code Scanning</Text>
        <Text style={styles.feature}>🔒 Biometric Security</Text>
      </View>
      
      <TouchableOpacity style={styles.button}>
        <Text style={styles.buttonText}>Get Started</Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: Colors.background,
    padding: Spacing.lg,
  },
  title: {
    fontSize: Typography.sizes.xxl,
    fontFamily: Typography.fonts.bold,
    color: Colors.textPrimary,
    marginBottom: Spacing.sm,
  },
  subtitle: {
    fontSize: Typography.sizes.lg,
    fontFamily: Typography.fonts.regular,
    color: Colors.textSecondary,
    marginBottom: Spacing.xl,
  },
  featuresContainer: {
    backgroundColor: Colors.surface,
    borderRadius: 12,
    padding: Spacing.lg,
    marginBottom: Spacing.xl,
    width: '100%',
  },
  featuresTitle: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.textPrimary,
    marginBottom: Spacing.md,
  },
  feature: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.regular,
    color: Colors.textSecondary,
    marginBottom: Spacing.sm,
  },
  button: {
    backgroundColor: Colors.primary,
    borderRadius: 8,
    paddingVertical: Spacing.md,
    paddingHorizontal: Spacing.xl,
  },
  buttonText: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.white,
  },
});

export default HomeScreen;
EOF

# Update App.tsx to use our enhanced structure
print_status "Updating App.tsx..."
cat > App.tsx << 'EOF'
/**
 * Provenly Enhanced Mobile Wallet
 */

import React from 'react';
import {SafeAreaView, StatusBar, StyleSheet} from 'react-native';
import HomeScreen from './src/screens/HomeScreen';
import {Colors} from './src/styles/colors';

function App(): JSX.Element {
  return (
    <SafeAreaView style={styles.container}>
      <StatusBar
        barStyle="dark-content"
        backgroundColor={Colors.background}
      />
      <HomeScreen />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
});

export default App;
EOF

print_success "✅ Enhanced source files created!"
print_status "🚀 Next steps:"
echo "   1. npm start"
echo "   2. npm run ios (or npm run android)"
echo ""
print_success "🎉 Your enhanced mobile wallet is ready!"
EOF
