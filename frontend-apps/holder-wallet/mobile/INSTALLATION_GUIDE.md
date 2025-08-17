# 📱 **Provenly Mobile Wallet - Installation Guide**

## 🔧 **Dependency Issues Fixed**

I've resolved multiple dependency conflicts:
- ❌ Removed deprecated `@react-native-camera/camera` (replaced with `react-native-vision-camera`)
- ❌ Removed duplicate `@react-native-keychain/keychain` dependency
- ❌ Removed deprecated `react-native-qrcode-scanner`
- ❌ Removed unused `react-native-wallet-connect`
- ✅ Updated `react-native-svg` to v14.1.0 for compatibility
- ✅ Updated `react-native-vision-camera` to v3.6.17

## ✅ **Quick Fix - Option 1 (Recommended)**

Run the installation with legacy peer deps to resolve conflicts:

```bash
cd frontend-apps/holder-wallet/mobile

# Clear any existing node_modules and lock files
rm -rf node_modules package-lock.json yarn.lock

# Install with legacy peer deps
npm install --legacy-peer-deps

# For iOS
cd ios && pod install && cd ..

# Start the development server
npm start

# Run on device/simulator
npm run ios    # or npm run android
```

## ✅ **Alternative Fix - Option 2**

If you prefer to use exact versions, install specific compatible versions:

```bash
cd frontend-apps/holder-wallet/mobile

# Clear existing installations
rm -rf node_modules package-lock.json

# Install core dependencies first
npm install react@18.2.0 react-native@0.72.6

# Install compatible SVG and QR code libraries
npm install react-native-svg@14.1.0 react-native-qrcode-svg@6.3.0

# Install remaining dependencies
npm install --legacy-peer-deps

# iOS setup
cd ios && pod install && cd ..
```

## ✅ **Alternative Fix - Option 3 (Force Resolution)**

Add resolution overrides to package.json:

```json
{
  "resolutions": {
    "react-native-svg": "^14.1.0"
  },
  "overrides": {
    "react-native-svg": "^14.1.0"
  }
}
```

Then run:
```bash
npm install --force
```

## 🚀 **Complete Setup Process**

### **1. Prerequisites**
```bash
# Node.js 16+ required
node --version

# React Native CLI
npm install -g @react-native-community/cli

# For iOS development
xcode-select --install

# For Android development
# Install Android Studio and SDK
```

### **2. Project Setup**
```bash
# Navigate to project
cd frontend-apps/holder-wallet/mobile

# Install dependencies (use one of the methods above)
npm install --legacy-peer-deps

# iOS specific setup
cd ios
pod install
cd ..

# Android specific setup (if needed)
npx react-native doctor
```

### **3. Development**
```bash
# Start Metro bundler
npm start

# Run on iOS
npm run ios
# or
npx react-native run-ios

# Run on Android
npm run android
# or
npx react-native run-android
```

## 🔍 **Troubleshooting Common Issues**

### **Issue 1: Metro bundler errors**
```bash
# Clear Metro cache
npx react-native start --reset-cache

# Clear all caches
npm start -- --reset-cache
```

### **Issue 2: iOS build errors**
```bash
cd ios
pod deintegrate
pod install
cd ..
npm run ios
```

### **Issue 3: Android build errors**
```bash
cd android
./gradlew clean
cd ..
npm run android
```

### **Issue 4: Crypto polyfill errors**
The project includes polyfills in `src/polyfills.ts`. If you encounter crypto-related errors:

1. Ensure polyfills are imported in `index.js`
2. Check metro.config.js has correct aliases
3. Verify all crypto dependencies are installed

### **Issue 5: TypeScript errors**
```bash
# Check TypeScript configuration
npx tsc --noEmit

# Install missing type definitions
npm install --save-dev @types/uuid @types/elliptic
```

## 📦 **Key Dependencies Explained**

### **Core React Native**
- `react-native@0.72.6` - Main framework
- `react-native-svg@14.1.0` - SVG support (updated for compatibility)
- `react-native-qrcode-svg@6.3.0` - QR code generation

### **Cryptography**
- `ethers@^6.8.1` - Ethereum utilities
- `elliptic@^6.5.4` - Elliptic curve cryptography
- `node-forge@^1.3.1` - RSA and certificate handling
- `crypto-browserify@^3.12.0` - Crypto polyfills

### **Storage & Security**
- `react-native-mmkv@^2.10.1` - Fast encrypted storage
- `react-native-keychain@^8.1.3` - Secure keychain access
- `react-native-biometrics@^3.0.1` - Biometric authentication

### **Navigation & UI**
- `@react-navigation/native@^6.1.9` - Navigation
- `react-native-vector-icons@^10.0.2` - Icons
- `react-native-linear-gradient@^2.8.3` - Gradients

## 🎯 **Verification Steps**

After successful installation, verify everything works:

```bash
# 1. Check if Metro starts without errors
npm start

# 2. Build for iOS (in another terminal)
npm run ios

# 3. Build for Android
npm run android

# 4. Run tests (if available)
npm test

# 5. Check TypeScript compilation
npx tsc --noEmit
```

## 🆘 **Still Having Issues?**

If you continue to have dependency conflicts:

1. **Use Node Version Manager (nvm)**:
   ```bash
   nvm install 18
   nvm use 18
   ```

2. **Clear all caches**:
   ```bash
   npm cache clean --force
   npx react-native start --reset-cache
   ```

3. **Use Yarn instead of npm**:
   ```bash
   yarn install
   yarn ios
   ```

4. **Check React Native environment**:
   ```bash
   npx react-native doctor
   ```

## ✅ **Success Indicators**

You'll know the setup is successful when:
- ✅ `npm install` completes without ERESOLVE errors
- ✅ Metro bundler starts without warnings
- ✅ App builds and runs on simulator/device
- ✅ No TypeScript compilation errors
- ✅ All screens and features work correctly

The enhanced mobile wallet with all new features (signup, DID creation, key management, VC/VP handling) should now be fully functional! 🎉📱
