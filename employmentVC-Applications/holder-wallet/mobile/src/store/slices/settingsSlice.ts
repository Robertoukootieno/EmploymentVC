import {createSlice} from '@reduxjs/toolkit';

const settingsSlice = createSlice({
  name: 'settings',
  initialState: {
    darkMode: false,
    notifications: true,
  },
  reducers: {},
});

export default settingsSlice.reducer;
