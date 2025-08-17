/**
 * Wallet Redux Slice
 * 
 * Manages wallet state for both custodial and non-custodial wallets
 */

import {createSlice, createAsyncThunk, PayloadAction} from '@reduxjs/toolkit';
import {
  Wallet,
  WalletType,
  WalletStatus,
  WalletCreationRequest,
  CreateWalletResponse,
  WalletListResponse,
  WalletDetailsResponse,
} from '../../types/wallet';
import {walletService} from '../../services/walletService';

interface WalletState {
  // Current active wallet
  activeWallet: Wallet | null;
  
  // All user wallets
  wallets: Wallet[];
  
  // Loading states
  loading: {
    creating: boolean;
    fetching: boolean;
    updating: boolean;
    deleting: boolean;
  };
  
  // Error states
  error: string | null;
  
  // Wallet details
  walletDetails: {
    [walletId: string]: WalletDetailsResponse;
  };
  
  // Pagination
  pagination: {
    page: number;
    size: number;
    totalCount: number;
    totalPages: number;
  };
}

const initialState: WalletState = {
  activeWallet: null,
  wallets: [],
  loading: {
    creating: false,
    fetching: false,
    updating: false,
    deleting: false,
  },
  error: null,
  walletDetails: {},
  pagination: {
    page: 0,
    size: 20,
    totalCount: 0,
    totalPages: 0,
  },
};

// Async Thunks
export const createWallet = createAsyncThunk(
  'wallet/create',
  async (request: WalletCreationRequest, {rejectWithValue}) => {
    try {
      const response = await walletService.createWallet(request);
      return response;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to create wallet');
    }
  }
);

export const fetchWallets = createAsyncThunk(
  'wallet/fetchAll',
  async (params: {page?: number; size?: number} = {}, {rejectWithValue}) => {
    try {
      const response = await walletService.getWallets(params);
      return response;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch wallets');
    }
  }
);

export const fetchWalletDetails = createAsyncThunk(
  'wallet/fetchDetails',
  async (walletId: string, {rejectWithValue}) => {
    try {
      const response = await walletService.getWalletDetails(walletId);
      return {walletId, details: response};
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch wallet details');
    }
  }
);

export const updateWallet = createAsyncThunk(
  'wallet/update',
  async (
    {walletId, updates}: {walletId: string; updates: Partial<Wallet>},
    {rejectWithValue}
  ) => {
    try {
      const response = await walletService.updateWallet(walletId, updates);
      return response;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to update wallet');
    }
  }
);

export const deleteWallet = createAsyncThunk(
  'wallet/delete',
  async (walletId: string, {rejectWithValue}) => {
    try {
      await walletService.deleteWallet(walletId);
      return walletId;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to delete wallet');
    }
  }
);

export const setActiveWallet = createAsyncThunk(
  'wallet/setActive',
  async (walletId: string, {rejectWithValue}) => {
    try {
      const response = await walletService.setActiveWallet(walletId);
      return response;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to set active wallet');
    }
  }
);

// Wallet Slice
const walletSlice = createSlice({
  name: 'wallet',
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null;
    },
    
    clearWalletDetails: (state, action: PayloadAction<string>) => {
      delete state.walletDetails[action.payload];
    },
    
    updateWalletStatus: (
      state,
      action: PayloadAction<{walletId: string; status: WalletStatus}>
    ) => {
      const {walletId, status} = action.payload;
      const wallet = state.wallets.find(w => w.id === walletId);
      if (wallet) {
        wallet.status = status;
      }
      if (state.activeWallet?.id === walletId) {
        state.activeWallet.status = status;
      }
    },
    
    incrementCredentialCount: (state, action: PayloadAction<string>) => {
      const walletId = action.payload;
      const wallet = state.wallets.find(w => w.id === walletId);
      if (wallet) {
        wallet.credentialCount += 1;
      }
      if (state.activeWallet?.id === walletId) {
        state.activeWallet.credentialCount += 1;
      }
    },
    
    decrementCredentialCount: (state, action: PayloadAction<string>) => {
      const walletId = action.payload;
      const wallet = state.wallets.find(w => w.id === walletId);
      if (wallet && wallet.credentialCount > 0) {
        wallet.credentialCount -= 1;
      }
      if (state.activeWallet?.id === walletId && state.activeWallet.credentialCount > 0) {
        state.activeWallet.credentialCount -= 1;
      }
    },
  },
  
  extraReducers: (builder) => {
    // Create Wallet
    builder
      .addCase(createWallet.pending, (state) => {
        state.loading.creating = true;
        state.error = null;
      })
      .addCase(createWallet.fulfilled, (state, action) => {
        state.loading.creating = false;
        state.wallets.push(action.payload.wallet);
        if (state.wallets.length === 1) {
          state.activeWallet = action.payload.wallet;
        }
      })
      .addCase(createWallet.rejected, (state, action) => {
        state.loading.creating = false;
        state.error = action.payload as string;
      });

    // Fetch Wallets
    builder
      .addCase(fetchWallets.pending, (state) => {
        state.loading.fetching = true;
        state.error = null;
      })
      .addCase(fetchWallets.fulfilled, (state, action) => {
        state.loading.fetching = false;
        state.wallets = action.payload.wallets;
        state.pagination = {
          page: action.payload.page,
          size: action.payload.size,
          totalCount: action.payload.totalCount,
          totalPages: Math.ceil(action.payload.totalCount / action.payload.size),
        };
        
        // Set active wallet if none is set
        if (!state.activeWallet && state.wallets.length > 0) {
          state.activeWallet = state.wallets[0];
        }
      })
      .addCase(fetchWallets.rejected, (state, action) => {
        state.loading.fetching = false;
        state.error = action.payload as string;
      });

    // Fetch Wallet Details
    builder
      .addCase(fetchWalletDetails.pending, (state) => {
        state.loading.fetching = true;
      })
      .addCase(fetchWalletDetails.fulfilled, (state, action) => {
        state.loading.fetching = false;
        const {walletId, details} = action.payload;
        state.walletDetails[walletId] = details;
      })
      .addCase(fetchWalletDetails.rejected, (state, action) => {
        state.loading.fetching = false;
        state.error = action.payload as string;
      });

    // Update Wallet
    builder
      .addCase(updateWallet.pending, (state) => {
        state.loading.updating = true;
        state.error = null;
      })
      .addCase(updateWallet.fulfilled, (state, action) => {
        state.loading.updating = false;
        const updatedWallet = action.payload;
        const index = state.wallets.findIndex(w => w.id === updatedWallet.id);
        if (index !== -1) {
          state.wallets[index] = updatedWallet;
        }
        if (state.activeWallet?.id === updatedWallet.id) {
          state.activeWallet = updatedWallet;
        }
      })
      .addCase(updateWallet.rejected, (state, action) => {
        state.loading.updating = false;
        state.error = action.payload as string;
      });

    // Delete Wallet
    builder
      .addCase(deleteWallet.pending, (state) => {
        state.loading.deleting = true;
        state.error = null;
      })
      .addCase(deleteWallet.fulfilled, (state, action) => {
        state.loading.deleting = false;
        const walletId = action.payload;
        state.wallets = state.wallets.filter(w => w.id !== walletId);
        if (state.activeWallet?.id === walletId) {
          state.activeWallet = state.wallets.length > 0 ? state.wallets[0] : null;
        }
        delete state.walletDetails[walletId];
      })
      .addCase(deleteWallet.rejected, (state, action) => {
        state.loading.deleting = false;
        state.error = action.payload as string;
      });

    // Set Active Wallet
    builder
      .addCase(setActiveWallet.pending, (state) => {
        state.loading.updating = true;
      })
      .addCase(setActiveWallet.fulfilled, (state, action) => {
        state.loading.updating = false;
        state.activeWallet = action.payload;
      })
      .addCase(setActiveWallet.rejected, (state, action) => {
        state.loading.updating = false;
        state.error = action.payload as string;
      });
  },
});

export const {
  clearError,
  clearWalletDetails,
  updateWalletStatus,
  incrementCredentialCount,
  decrementCredentialCount,
} = walletSlice.actions;

export default walletSlice.reducer;
