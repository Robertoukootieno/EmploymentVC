import {createAsyncThunk, createSlice} from '@reduxjs/toolkit';

interface AuthState {
  isAuthenticated: boolean;
  hasCompletedOnboarding: boolean;
  loading: {
    authenticating: boolean;
  };
  error: string | null;
}

const initialState: AuthState = {
  isAuthenticated: false,
  hasCompletedOnboarding: true,
  loading: {
    authenticating: false,
  },
  error: null,
};

export const login = createAsyncThunk(
  'auth/login',
  async (_payload: {email: string; password: string}) => ({ok: true})
);

export const loginWithWeb3 = createAsyncThunk(
  'auth/loginWithWeb3',
  async (_payload: {walletAddress: string; signature: string; message: string}) => ({ok: true})
);

export const loginWithBiometric = createAsyncThunk('auth/loginWithBiometric', async () => ({ok: true}));

export const loginWithPIN = createAsyncThunk('auth/loginWithPIN', async (_payload: {pin: string}) => ({ok: true}));

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    logout: (state) => {
      state.isAuthenticated = false;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(login.pending, (state) => {
        state.loading.authenticating = true;
        state.error = null;
      })
      .addCase(login.fulfilled, (state) => {
        state.loading.authenticating = false;
        state.isAuthenticated = true;
      })
      .addCase(login.rejected, (state) => {
        state.loading.authenticating = false;
        state.error = 'Authentication failed';
      })
      .addCase(loginWithWeb3.fulfilled, (state) => {
        state.isAuthenticated = true;
      })
      .addCase(loginWithBiometric.fulfilled, (state) => {
        state.isAuthenticated = true;
      })
      .addCase(loginWithPIN.fulfilled, (state) => {
        state.isAuthenticated = true;
      });
  },
});

export const {logout} = authSlice.actions;
export default authSlice.reducer;
