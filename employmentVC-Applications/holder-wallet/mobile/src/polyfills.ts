/**
 * Polyfills for React Native
 * 
 * Sets up necessary polyfills for crypto, buffer, and other Node.js modules
 */

import 'react-native-get-random-values';
import 'react-native-url-polyfill/auto';

// Buffer polyfill
import {Buffer} from 'buffer';
global.Buffer = Buffer;

// Process polyfill
global.process = require('process');

// Crypto polyfill setup
if (typeof global.crypto === 'undefined') {
  global.crypto = {};
}

if (typeof global.crypto.getRandomValues === 'undefined') {
  global.crypto.getRandomValues = (array: any) => {
    const {getRandomValues} = require('react-native-get-random-values');
    return getRandomValues(array);
  };
}

// TextEncoder/TextDecoder polyfills
if (typeof global.TextEncoder === 'undefined') {
  global.TextEncoder = require('text-encoding').TextEncoder;
}

if (typeof global.TextDecoder === 'undefined') {
  global.TextDecoder = require('text-encoding').TextDecoder;
}

// Console polyfills for better debugging
if (typeof global.console === 'undefined') {
  global.console = {
    log: () => {},
    warn: () => {},
    error: () => {},
    info: () => {},
    debug: () => {},
  };
}

// Performance polyfill
if (typeof global.performance === 'undefined') {
  global.performance = {
    now: () => Date.now(),
  };
}

export {};
