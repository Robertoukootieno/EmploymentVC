/**
 * Main App Navigator
 * 
 * Handles navigation between authenticated and unauthenticated flows
 */

import React from 'react';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import {useAppSelector} from '../store';

// Navigators
import AuthNavigator from './AuthNavigator';
import MainTabNavigator from './MainTabNavigator';
import OnboardingNavigator from './OnboardingNavigator';

// Screens
import SplashScreen from '../screens/SplashScreen';
import WalletSetupScreen from '../screens/WalletSetupScreen';

// Types
export type RootStackParamList = {
  Splash: undefined;
  Onboarding: undefined;
  Auth: undefined;
  WalletSetup: undefined;
  Main: undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();

const AppNavigator: React.FC = () => {
  const {isAuthenticated, hasCompletedOnboarding, hasWallet, isLoading} = useAppSelector(
    (state) => ({
      isAuthenticated: state.auth.isAuthenticated,
      hasCompletedOnboarding: state.auth.hasCompletedOnboarding,
      hasWallet: state.wallet.wallets.length > 0,
      isLoading: state.auth.loading.authenticating || state.wallet.loading.fetching,
    })
  );

  // Show splash screen while loading
  if (isLoading) {
    return (
      <Stack.Navigator screenOptions={{headerShown: false}}>
        <Stack.Screen name="Splash" component={SplashScreen} />
      </Stack.Navigator>
    );
  }

  return (
    <Stack.Navigator screenOptions={{headerShown: false}}>
      {!hasCompletedOnboarding ? (
        // First time user - show onboarding
        <Stack.Screen name="Onboarding" component={OnboardingNavigator} />
      ) : !isAuthenticated ? (
        // User needs to authenticate
        <Stack.Screen name="Auth" component={AuthNavigator} />
      ) : !hasWallet ? (
        // User is authenticated but needs to create/import a wallet
        <Stack.Screen name="WalletSetup" component={WalletSetupScreen} />
      ) : (
        // User is authenticated and has a wallet - show main app
        <Stack.Screen name="Main" component={MainTabNavigator} />
      )}
    </Stack.Navigator>
  );
};

export default AppNavigator;
