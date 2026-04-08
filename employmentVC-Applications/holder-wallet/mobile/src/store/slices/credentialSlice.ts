import {createSlice} from '@reduxjs/toolkit';

const credentialSlice = createSlice({
  name: 'credential',
  initialState: {
    items: [],
    loading: false,
    error: null as string | null,
  },
  reducers: {},
});

export default credentialSlice.reducer;
