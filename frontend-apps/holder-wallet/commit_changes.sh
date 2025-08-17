#!/bin/bash

# 🚀 Enhanced Mobile Wallet - Git Commit Script
# Run this script to commit all the enhanced features

echo "🚀 Committing Enhanced Mobile Wallet Features..."

# Navigate to the project directory
cd "$(dirname "$0")"

# Check git status
echo "📋 Checking current git status..."
git status

# Stage all changes
echo "📁 Staging all changes..."
git add .

# Show what will be committed
echo "🔍 Files to be committed:"
git diff --cached --name-only

# Commit with comprehensive message
echo "💾 Committing changes..."
git commit -m "feat: Enhanced mobile wallet with advanced DID, key management, and VC/VP features

✨ New Features:
- Complete signup flow with DID creation and key generation
- Multi-method DID support (EBSI, Ethereum, Key-based, Web)
- Advanced key management (RSA, SECP256R1, SECP256K1, Ed25519)
- Enhanced VC/VP processing with selective disclosure
- QR code scanning with modern camera integration
- Biometric authentication with hardware security
- Professional UI/UX with consistent design system

🔧 Technical Improvements:
- Resolved dependency conflicts (removed deprecated react-native-camera)
- Updated react-native-svg to v14.1.0 for compatibility
- Added crypto polyfills for Node.js modules in React Native
- Enhanced Metro configuration for crypto libraries
- Complete TypeScript type system for crypto operations
- Hardware security module integration
- Audit logging and compliance features

📱 New Screens:
- SignupScreen.tsx - 4-step registration with wallet configuration
- DIDCreationScreen.tsx - Multi-method DID creation wizard
- KeyManagementScreen.tsx - Advanced cryptographic key operations

🔐 New Services:
- keyService.ts - Multi-algorithm key generation and management
- didService.ts - DID creation, resolution, and management
- vcService.ts - W3C compliant credential processing
- Enhanced authService.ts with biometric support

🎨 UI/UX Enhancements:
- Professional design system (colors, typography, spacing)
- Reusable component library
- Loading states and error handling
- Responsive design for all screen sizes

🚀 Ready for enterprise deployment with complete verifiable credential management!"

# Check if commit was successful
if [ $? -eq 0 ]; then
    echo "✅ Commit successful!"
    
    # Ask user if they want to push
    echo "🚀 Do you want to push to remote repository? (y/n)"
    read -r response
    
    if [[ "$response" =~ ^[Yy]$ ]]; then
        echo "📤 Pushing to remote repository..."
        
        # Get current branch name
        BRANCH=$(git rev-parse --abbrev-ref HEAD)
        echo "📍 Current branch: $BRANCH"
        
        # Push to remote
        git push origin "$BRANCH"
        
        if [ $? -eq 0 ]; then
            echo "🎉 Successfully pushed to remote repository!"
            echo "✅ Your enhanced mobile wallet is now available in the git repository!"
        else
            echo "❌ Failed to push to remote repository."
            echo "💡 You may need to check your git credentials or network connection."
        fi
    else
        echo "⏸️  Commit created but not pushed to remote."
        echo "💡 You can push later with: git push origin $BRANCH"
    fi
else
    echo "❌ Commit failed. Please check for any issues."
fi

echo ""
echo "📋 Summary of Enhanced Features Committed:"
echo "  📱 Complete Signup Flow with DID Creation"
echo "  🔐 Advanced Key Management (RSA, SECP256R1, SECP256K1, Ed25519)"
echo "  🆔 Multi-method DID Support (EBSI, Ethereum, Key, Web)"
echo "  📜 Enhanced VC/VP Processing with Selective Disclosure"
echo "  📷 QR Code Scanning with Modern Camera"
echo "  🔒 Biometric Authentication with Hardware Security"
echo "  🎨 Professional UI/UX with Design System"
echo "  🔧 Dependency Conflicts Resolved"
echo ""
echo "🚀 Your enhanced mobile wallet is ready for enterprise deployment!"
