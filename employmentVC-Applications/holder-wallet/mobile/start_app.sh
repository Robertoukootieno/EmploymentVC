#!/bin/bash

# 🚀 Start Provenly Holder Wallet Mobile App
# This script sets up and runs the enhanced mobile wallet

echo "🚀 Starting Provenly Holder Wallet Mobile App..."
echo ""

# Navigate to mobile directory
cd "$(dirname "$0")"

# Check if node_modules exists and has the security fix
echo "🔍 Checking dependencies..."
if [ -d "node_modules" ]; then
    FORGE_VERSION=$(npm list node-forge --depth=0 2>/dev/null | grep node-forge | awk -F@ '{print $2}')
    if [[ "$FORGE_VERSION" == "1.3.1"* ]]; then
        echo "⚠️  Old vulnerable node-forge detected (1.3.1)"
        echo "📦 Reinstalling with security fix..."
        rm -rf node_modules package-lock.json
        npm install --legacy-peer-deps
    elif [[ "$FORGE_VERSION" == "1.3.2"* ]] || [[ "$FORGE_VERSION" > "1.3.2" ]]; then
        echo "✅ Dependencies OK (node-forge $FORGE_VERSION - secure)"
    else
        echo "📦 Installing dependencies..."
        npm install --legacy-peer-deps
    fi
else
    echo "📦 Installing dependencies for the first time..."
    npm install --legacy-peer-deps
fi

echo ""
echo "🔍 Checking for native projects..."

# Check if iOS/Android directories exist
HAS_IOS=false
HAS_ANDROID=false

if [ -d "ios" ]; then
    HAS_IOS=true
    echo "✅ iOS project found"
else
    echo "⚠️  iOS project not found"
fi

if [ -d "android" ]; then
    HAS_ANDROID=true
    echo "✅ Android project found"
else
    echo "⚠️  Android project not found"
fi

# If neither exists, we need to initialize
if [ "$HAS_IOS" = false ] && [ "$HAS_ANDROID" = false ]; then
    echo ""
    echo "❌ Native projects not initialized!"
    echo ""
    echo "📱 This React Native project needs native iOS/Android projects."
    echo ""
    echo "🔧 Options to fix this:"
    echo ""
    echo "Option 1: Use Expo (Recommended for quick start)"
    echo "  npx expo init --template blank-typescript"
    echo ""
    echo "Option 2: Initialize React Native CLI project"
    echo "  npx react-native init ProvenlyWallet --template react-native-template-typescript"
    echo "  Then copy all src/ files to the new project"
    echo ""
    echo "Option 3: Create native projects in current directory"
    echo "  This requires manual setup of ios/ and android/ folders"
    echo ""
    read -p "Would you like me to start Metro bundler anyway? (y/n) " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Cancelled. Please initialize native projects first."
        exit 1
    fi
fi

echo ""
echo "🚀 Starting Metro Bundler..."
echo ""
echo "📱 After Metro starts, you can:"
if [ "$HAS_IOS" = true ]; then
    echo "  • Press 'i' for iOS simulator"
fi
if [ "$HAS_ANDROID" = true ]; then
    echo "  • Press 'a' for Android emulator"
fi
echo "  • Press 'r' to reload"
echo "  • Press 'd' to open developer menu"
echo ""
echo "Or in a new terminal, run:"
if [ "$HAS_IOS" = true ]; then
    echo "  npm run ios"
fi
if [ "$HAS_ANDROID" = true ]; then
    echo "  npm run android"
fi
echo ""

# Start Metro
npm start

