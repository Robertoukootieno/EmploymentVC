/**
 * Redux Store Configuration
 * 
 * Configures the Redux store with persistence and middleware
 */

import {configureStore, combineReducers} from '@reduxjs/toolkit';
import {persistStore, persistReducer} from 'redux-persist';
import {MMKV} from 'react-native-mmkv';

// Reducers
import authReducer from './slices/authSlice';
import walletReducer from './slices/walletSlice';
import credentialReducer from './slices/credentialSlice';
import settingsReducer from './slices/settingsSlice';
import uiReducer from './slices/uiSlice';

// MMKV Storage for better performance and security
const storage = new MMKV({
  id: 'provenly-wallet-storage',
  encryptionKey: 'provenly-wallet-encryption-key',
});

// Redux Persist Storage Adapter
const reduxStorage = {
  setItem: (key: string, value: string) => {
    storage.set(key, value);
    return Promise.resolve(true);
  },
  getItem: (key: string) => {
    const value = storage.getString(key);
    return Promise.resolve(value);
  },
  removeItem: (key: string) => {
    storage.delete(key);
    return Promise.resolve();
  },
};

// Root Reducer
const rootReducer = combineReducers({
  auth: authReducer,
  wallet: walletReducer,
  credential: credentialReducer,
  settings: settingsReducer,
  ui: uiReducer,
});

// Persist Configuration
const persistConfig = {
  key: 'root',
  storage: reduxStorage,
  whitelist: ['auth', 'wallet', 'settings'], // Only persist these slices
  blacklist: ['ui'], // Don't persist UI state
};

// Persisted Reducer
const persistedReducer = persistReducer(persistConfig, rootReducer);

// Store Configuration
export const store = configureStore({
  reducer: persistedReducer,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        ignoredActions: [
          'persist/PERSIST',
          'persist/REHYDRATE',
          'persist/PAUSE',
          'persist/PURGE',
          'persist/REGISTER',
        ],
      },
    }),
  devTools: __DEV__,
});

// Persistor
export const persistor = persistStore(store);

// Types
export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;

// Typed Hooks
import {useDispatch, useSelector, TypedUseSelectorHook} from 'react-redux';

export const useAppDispatch = () => useDispatch<AppDispatch>();
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
