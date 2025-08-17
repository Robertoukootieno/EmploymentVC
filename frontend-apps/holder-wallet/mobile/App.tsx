import React, {useEffect} from 'react';
import {StatusBar, Platform} from 'react-native';
import {NavigationContainer} from '@react-navigation/native';
import {Provider} from 'react-redux';
import {PersistGate} from 'redux-persist/integration/react';
import {GestureHandlerRootView} from 'react-native-gesture-handler';
import {SafeAreaProvider} from 'react-native-safe-area-context';

// Polyfills for crypto and URL
import 'react-native-get-random-values';
import 'react-native-url-polyfill/auto';

// Redux store
import {store, persistor} from './src/store';

// Navigation
import AppNavigator from './src/navigation/AppNavigator';

// Components
import LoadingScreen from './src/components/LoadingScreen';
import ErrorBoundary from './src/components/ErrorBoundary';

// Services
import {initializeServices} from './src/services';

// Styles
import {Colors} from './src/styles/colors';

/**
 * Provenly Holder Wallet Mobile App
 * 
 * Features:
 * - Custodial and Non-custodial wallet support
 * - Verifiable Credentials management
 * - QR code scanning for presentations
 * - Biometric authentication
 * - Web3 wallet integration
 * - Secure credential storage
 */
const App: React.FC = () => {
  useEffect(() => {
    // Initialize app services
    initializeServices().catch(error => {
      console.error('Failed to initialize app services:', error);
    });
  }, []);

  return (
    <ErrorBoundary>
      <GestureHandlerRootView style={{flex: 1}}>
        <SafeAreaProvider>
          <Provider store={store}>
            <PersistGate loading={<LoadingScreen />} persistor={persistor}>
              <NavigationContainer>
                <StatusBar
                  barStyle={Platform.OS === 'ios' ? 'dark-content' : 'light-content'}
                  backgroundColor={Colors.primary}
                />
                <AppNavigator />
              </NavigationContainer>
            </PersistGate>
          </Provider>
        </SafeAreaProvider>
      </GestureHandlerRootView>
    </ErrorBoundary>
  );
};

export default App;
