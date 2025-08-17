/**
 * QR Code Scanner Screen
 * 
 * Handles QR code scanning for credential verification and presentation requests
 */

import React, {useEffect, useState, useRef} from 'react';
import {
  View,
  Text,
  StyleSheet,
  Alert,
  TouchableOpacity,
  Animated,
  Dimensions,
  Platform,
} from 'react-native';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import {
  Camera,
  useCameraDevices,
  useCodeScanner,
  CameraPermissionStatus,
} from 'react-native-vision-camera';
import Icon from 'react-native-vector-icons/MaterialIcons';

// Redux
import {useAppSelector, useAppDispatch} from '../store';
import {verifyCredential, createPresentation} from '../store/slices/credentialSlice';

// Components
import LoadingSpinner from '../components/LoadingSpinner';
import PermissionRequest from '../components/PermissionRequest';

// Services
import {qrCodeService} from '../services/qrCodeService';
import {presentationService} from '../services/presentationService';

// Types
import {PresentationRequest} from '../types/credential';

// Styles
import {Colors} from '../styles/colors';
import {Typography} from '../styles/typography';
import {Spacing} from '../styles/spacing';

const {width: screenWidth, height: screenHeight} = Dimensions.get('window');

interface ScannedData {
  type: 'credential' | 'presentation_request' | 'verification_request' | 'unknown';
  data: any;
}

const ScanScreen: React.FC = () => {
  const insets = useSafeAreaInsets();
  const dispatch = useAppDispatch();
  
  const [cameraPermission, setCameraPermission] = useState<CameraPermissionStatus>('not-determined');
  const [isActive, setIsActive] = useState(true);
  const [isProcessing, setIsProcessing] = useState(false);
  const [flashEnabled, setFlashEnabled] = useState(false);
  const [scanMode, setScanMode] = useState<'auto' | 'manual'>('auto');
  
  const scanAnimation = useRef(new Animated.Value(0)).current;
  const devices = useCameraDevices();
  const device = devices.back;

  const {activeWallet} = useAppSelector((state) => state.wallet);

  useEffect(() => {
    checkCameraPermission();
    startScanAnimation();
  }, []);

  useEffect(() => {
    // Pause scanning when screen is not focused
    const unsubscribe = () => {
      setIsActive(false);
    };
    return unsubscribe;
  }, []);

  const checkCameraPermission = async () => {
    const permission = await Camera.getCameraPermissionStatus();
    setCameraPermission(permission);
    
    if (permission === 'not-determined') {
      const newPermission = await Camera.requestCameraPermission();
      setCameraPermission(newPermission);
    }
  };

  const startScanAnimation = () => {
    Animated.loop(
      Animated.sequence([
        Animated.timing(scanAnimation, {
          toValue: 1,
          duration: 2000,
          useNativeDriver: true,
        }),
        Animated.timing(scanAnimation, {
          toValue: 0,
          duration: 2000,
          useNativeDriver: true,
        }),
      ])
    ).start();
  };

  const codeScanner = useCodeScanner({
    codeTypes: ['qr', 'ean-13'],
    onCodeScanned: (codes) => {
      if (isProcessing || !isActive) return;
      
      const code = codes[0];
      if (code?.value) {
        handleQRCodeScanned(code.value);
      }
    },
  });

  const handleQRCodeScanned = async (data: string) => {
    if (isProcessing) return;
    
    setIsProcessing(true);
    setIsActive(false);

    try {
      const scannedData = await qrCodeService.parseQRCode(data);
      await processScannedData(scannedData);
    } catch (error) {
      console.error('Failed to process QR code:', error);
      Alert.alert(
        'Invalid QR Code',
        'The scanned QR code is not recognized or supported.',
        [
          {
            text: 'Try Again',
            onPress: () => {
              setIsProcessing(false);
              setIsActive(true);
            },
          },
        ]
      );
    }
  };

  const processScannedData = async (scannedData: ScannedData) => {
    switch (scannedData.type) {
      case 'credential':
        await handleCredentialQR(scannedData.data);
        break;
      case 'presentation_request':
        await handlePresentationRequest(scannedData.data);
        break;
      case 'verification_request':
        await handleVerificationRequest(scannedData.data);
        break;
      default:
        Alert.alert(
          'Unsupported QR Code',
          'This QR code type is not supported by the wallet.',
          [
            {
              text: 'OK',
              onPress: () => {
                setIsProcessing(false);
                setIsActive(true);
              },
            },
          ]
        );
    }
  };

  const handleCredentialQR = async (credentialData: any) => {
    try {
      // Verify the credential first
      const verificationResult = await dispatch(verifyCredential({
        credential: credentialData,
      })).unwrap();

      if (verificationResult.valid) {
        Alert.alert(
          'Valid Credential Found',
          'Would you like to add this credential to your wallet?',
          [
            {
              text: 'Cancel',
              style: 'cancel',
              onPress: () => {
                setIsProcessing(false);
                setIsActive(true);
              },
            },
            {
              text: 'Add to Wallet',
              onPress: async () => {
                // Navigate to credential import flow
                console.log('Import credential:', credentialData);
                setIsProcessing(false);
                setIsActive(true);
              },
            },
          ]
        );
      } else {
        Alert.alert(
          'Invalid Credential',
          'The scanned credential is not valid or has been revoked.',
          [
            {
              text: 'OK',
              onPress: () => {
                setIsProcessing(false);
                setIsActive(true);
              },
            },
          ]
        );
      }
    } catch (error) {
      Alert.alert(
        'Verification Failed',
        'Failed to verify the credential. Please try again.',
        [
          {
            text: 'OK',
            onPress: () => {
              setIsProcessing(false);
              setIsActive(true);
            },
          },
        ]
      );
    }
  };

  const handlePresentationRequest = async (requestData: PresentationRequest) => {
    if (!activeWallet) {
      Alert.alert('No Active Wallet', 'Please select a wallet first.');
      return;
    }

    Alert.alert(
      'Presentation Request',
      `${requestData.verifier.name} is requesting to verify your credentials.\n\nPurpose: ${requestData.purpose}`,
      [
        {
          text: 'Decline',
          style: 'cancel',
          onPress: () => {
            setIsProcessing(false);
            setIsActive(true);
          },
        },
        {
          text: 'Review Request',
          onPress: () => {
            // Navigate to presentation review screen
            console.log('Review presentation request:', requestData);
            setIsProcessing(false);
            setIsActive(true);
          },
        },
      ]
    );
  };

  const handleVerificationRequest = async (verificationData: any) => {
    // Handle verification request (when acting as a verifier)
    Alert.alert(
      'Verification Mode',
      'This QR code contains a credential for verification. Switch to verifier mode?',
      [
        {
          text: 'Cancel',
          style: 'cancel',
          onPress: () => {
            setIsProcessing(false);
            setIsActive(true);
          },
        },
        {
          text: 'Verify',
          onPress: async () => {
            // Process verification
            console.log('Verify credential:', verificationData);
            setIsProcessing(false);
            setIsActive(true);
          },
        },
      ]
    );
  };

  const toggleFlash = () => {
    setFlashEnabled(!flashEnabled);
  };

  const toggleScanMode = () => {
    setScanMode(scanMode === 'auto' ? 'manual' : 'auto');
  };

  if (cameraPermission === 'denied') {
    return (
      <PermissionRequest
        icon="camera-alt"
        title="Camera Permission Required"
        message="To scan QR codes, please allow camera access in your device settings."
        onRetry={checkCameraPermission}
      />
    );
  }

  if (!device) {
    return (
      <View style={[styles.container, styles.centered]}>
        <LoadingSpinner size="large" />
        <Text style={styles.loadingText}>Initializing camera...</Text>
      </View>
    );
  }

  const scanLineTranslateY = scanAnimation.interpolate({
    inputRange: [0, 1],
    outputRange: [0, 200],
  });

  return (
    <View style={styles.container}>
      <Camera
        style={StyleSheet.absoluteFill}
        device={device}
        isActive={isActive && cameraPermission === 'granted'}
        codeScanner={codeScanner}
        torch={flashEnabled ? 'on' : 'off'}
      />

      {/* Header */}
      <View style={[styles.header, {paddingTop: insets.top}]}>
        <TouchableOpacity style={styles.headerButton} onPress={() => {}}>
          <Icon name="arrow-back" size={24} color={Colors.white} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Scan QR Code</Text>
        <TouchableOpacity style={styles.headerButton} onPress={toggleFlash}>
          <Icon 
            name={flashEnabled ? "flash-on" : "flash-off"} 
            size={24} 
            color={Colors.white} 
          />
        </TouchableOpacity>
      </View>

      {/* Scan Area */}
      <View style={styles.scanArea}>
        <View style={styles.scanFrame}>
          {/* Corner indicators */}
          <View style={[styles.corner, styles.topLeft]} />
          <View style={[styles.corner, styles.topRight]} />
          <View style={[styles.corner, styles.bottomLeft]} />
          <View style={[styles.corner, styles.bottomRight]} />
          
          {/* Animated scan line */}
          <Animated.View 
            style={[
              styles.scanLine,
              {
                transform: [{translateY: scanLineTranslateY}],
              },
            ]} 
          />
        </View>
      </View>

      {/* Instructions */}
      <View style={styles.instructions}>
        <Text style={styles.instructionTitle}>
          {isProcessing ? 'Processing...' : 'Position QR code within the frame'}
        </Text>
        <Text style={styles.instructionText}>
          {isProcessing 
            ? 'Please wait while we process the QR code'
            : 'The QR code will be scanned automatically'
          }
        </Text>
      </View>

      {/* Controls */}
      <View style={[styles.controls, {paddingBottom: insets.bottom}]}>
        <TouchableOpacity style={styles.controlButton} onPress={toggleScanMode}>
          <Icon 
            name={scanMode === 'auto' ? 'auto-awesome' : 'touch-app'} 
            size={24} 
            color={Colors.white} 
          />
          <Text style={styles.controlText}>
            {scanMode === 'auto' ? 'Auto' : 'Manual'}
          </Text>
        </TouchableOpacity>
        
        <TouchableOpacity style={styles.controlButton} onPress={() => {}}>
          <Icon name="photo-library" size={24} color={Colors.white} />
          <Text style={styles.controlText}>Gallery</Text>
        </TouchableOpacity>
      </View>

      {/* Processing Overlay */}
      {isProcessing && (
        <View style={styles.processingOverlay}>
          <LoadingSpinner size="large" color={Colors.white} />
          <Text style={styles.processingText}>Processing QR Code...</Text>
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.black,
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
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    zIndex: 1,
  },
  headerButton: {
    padding: Spacing.sm,
  },
  headerTitle: {
    fontSize: Typography.sizes.lg,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.white,
  },
  scanArea: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  scanFrame: {
    width: 250,
    height: 250,
    position: 'relative',
  },
  corner: {
    position: 'absolute',
    width: 20,
    height: 20,
    borderColor: Colors.primary,
    borderWidth: 3,
  },
  topLeft: {
    top: 0,
    left: 0,
    borderRightWidth: 0,
    borderBottomWidth: 0,
  },
  topRight: {
    top: 0,
    right: 0,
    borderLeftWidth: 0,
    borderBottomWidth: 0,
  },
  bottomLeft: {
    bottom: 0,
    left: 0,
    borderRightWidth: 0,
    borderTopWidth: 0,
  },
  bottomRight: {
    bottom: 0,
    right: 0,
    borderLeftWidth: 0,
    borderTopWidth: 0,
  },
  scanLine: {
    position: 'absolute',
    left: 0,
    right: 0,
    height: 2,
    backgroundColor: Colors.primary,
    shadowColor: Colors.primary,
    shadowOffset: {width: 0, height: 0},
    shadowOpacity: 0.8,
    shadowRadius: 4,
  },
  instructions: {
    position: 'absolute',
    bottom: 150,
    left: 0,
    right: 0,
    paddingHorizontal: Spacing.xl,
    alignItems: 'center',
  },
  instructionTitle: {
    fontSize: Typography.sizes.lg,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.white,
    textAlign: 'center',
    marginBottom: Spacing.sm,
  },
  instructionText: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.regular,
    color: Colors.white,
    textAlign: 'center',
    opacity: 0.8,
  },
  controls: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingHorizontal: Spacing.xl,
    paddingVertical: Spacing.lg,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
  },
  controlButton: {
    alignItems: 'center',
    padding: Spacing.md,
  },
  controlText: {
    fontSize: Typography.sizes.sm,
    fontFamily: Typography.fonts.medium,
    color: Colors.white,
    marginTop: Spacing.xs,
  },
  processingOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 2,
  },
  processingText: {
    fontSize: Typography.sizes.lg,
    fontFamily: Typography.fonts.semiBold,
    color: Colors.white,
    marginTop: Spacing.lg,
  },
  loadingText: {
    fontSize: Typography.sizes.md,
    fontFamily: Typography.fonts.regular,
    color: Colors.white,
    marginTop: Spacing.md,
  },
});

export default ScanScreen;
