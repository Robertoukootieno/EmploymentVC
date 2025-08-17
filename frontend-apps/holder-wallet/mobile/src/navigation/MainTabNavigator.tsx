/**
 * Main Tab Navigator
 * 
 * Bottom tab navigation for the main app functionality
 */

import React from 'react';
import {createBottomTabNavigator} from '@react-navigation/bottom-tabs';
import {Platform} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialIcons';

// Stack Navigators
import WalletStackNavigator from './WalletStackNavigator';
import CredentialsStackNavigator from './CredentialsStackNavigator';
import ScanStackNavigator from './ScanStackNavigator';
import SettingsStackNavigator from './SettingsStackNavigator';

// Styles
import {Colors} from '../styles/colors';
import {Typography} from '../styles/typography';

export type MainTabParamList = {
  WalletTab: undefined;
  CredentialsTab: undefined;
  ScanTab: undefined;
  SettingsTab: undefined;
};

const Tab = createBottomTabNavigator<MainTabParamList>();

const MainTabNavigator: React.FC = () => {
  return (
    <Tab.Navigator
      screenOptions={({route}) => ({
        headerShown: false,
        tabBarIcon: ({focused, color, size}) => {
          let iconName: string;

          switch (route.name) {
            case 'WalletTab':
              iconName = 'account-balance-wallet';
              break;
            case 'CredentialsTab':
              iconName = 'badge';
              break;
            case 'ScanTab':
              iconName = 'qr-code-scanner';
              break;
            case 'SettingsTab':
              iconName = 'settings';
              break;
            default:
              iconName = 'help';
          }

          return <Icon name={iconName} size={size} color={color} />;
        },
        tabBarActiveTintColor: Colors.primary,
        tabBarInactiveTintColor: Colors.textSecondary,
        tabBarStyle: {
          backgroundColor: Colors.surface,
          borderTopColor: Colors.border,
          borderTopWidth: 1,
          paddingBottom: Platform.OS === 'ios' ? 20 : 5,
          paddingTop: 5,
          height: Platform.OS === 'ios' ? 85 : 60,
        },
        tabBarLabelStyle: {
          fontSize: Typography.sizes.xs,
          fontFamily: Typography.fonts.medium,
          marginTop: 2,
        },
        tabBarItemStyle: {
          paddingVertical: 5,
        },
      })}
    >
      <Tab.Screen
        name="WalletTab"
        component={WalletStackNavigator}
        options={{
          tabBarLabel: 'Wallet',
        }}
      />
      <Tab.Screen
        name="CredentialsTab"
        component={CredentialsStackNavigator}
        options={{
          tabBarLabel: 'Credentials',
        }}
      />
      <Tab.Screen
        name="ScanTab"
        component={ScanStackNavigator}
        options={{
          tabBarLabel: 'Scan',
        }}
      />
      <Tab.Screen
        name="SettingsTab"
        component={SettingsStackNavigator}
        options={{
          tabBarLabel: 'Settings',
        }}
      />
    </Tab.Navigator>
  );
};

export default MainTabNavigator;
