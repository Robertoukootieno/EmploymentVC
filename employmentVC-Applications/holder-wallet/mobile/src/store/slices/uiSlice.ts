import {createSlice} from '@reduxjs/toolkit';

const uiSlice = createSlice({
  name: 'ui',
  initialState: {
    loading: false,
    modal: null as string | null,
  },
  reducers: {},
});

export default uiSlice.reducer;
