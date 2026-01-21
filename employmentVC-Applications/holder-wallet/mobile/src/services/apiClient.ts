/**
 * API Client
 * 
 * Centralized HTTP client with authentication, error handling, and request/response interceptors
 */

import axios, {AxiosInstance, AxiosRequestConfig, AxiosResponse, AxiosError} from 'axios';
import {Alert} from 'react-native';
import {secureStorage} from './secureStorage';
import {authService} from './authService';

// API Configuration
const API_BASE_URL = __DEV__ 
  ? 'http://localhost:8080' // Development
  : 'https://api.provenly.io'; // Production

const API_TIMEOUT = 30000; // 30 seconds

export interface ApiError {
  message: string;
  code: string;
  status: number;
  details?: any;
  timestamp: string;
  path: string;
}

class ApiClient {
  private client: AxiosInstance;
  private isRefreshing = false;
  private failedQueue: Array<{
    resolve: (value: any) => void;
    reject: (error: any) => void;
  }> = [];

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      timeout: API_TIMEOUT,
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
    });

    this.setupInterceptors();
  }

  private setupInterceptors() {
    // Request Interceptor
    this.client.interceptors.request.use(
      async (config) => {
        // Add authentication token
        const token = await secureStorage.getItem('accessToken');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }

        // Add request ID for tracking
        config.headers['X-Request-ID'] = this.generateRequestId();

        // Add device information
        config.headers['X-Device-Platform'] = 'mobile';
        config.headers['X-App-Version'] = '1.0.0';

        console.log(`🚀 API Request: ${config.method?.toUpperCase()} ${config.url}`);
        return config;
      },
      (error) => {
        console.error('❌ Request Error:', error);
        return Promise.reject(error);
      }
    );

    // Response Interceptor
    this.client.interceptors.response.use(
      (response) => {
        console.log(`✅ API Response: ${response.status} ${response.config.url}`);
        return response;
      },
      async (error: AxiosError) => {
        const originalRequest = error.config as AxiosRequestConfig & {_retry?: boolean};

        // Handle 401 Unauthorized - Token Refresh
        if (error.response?.status === 401 && !originalRequest._retry) {
          if (this.isRefreshing) {
            // Queue the request while refreshing
            return new Promise((resolve, reject) => {
              this.failedQueue.push({resolve, reject});
            }).then((token) => {
              originalRequest.headers!.Authorization = `Bearer ${token}`;
              return this.client(originalRequest);
            }).catch((err) => {
              return Promise.reject(err);
            });
          }

          originalRequest._retry = true;
          this.isRefreshing = true;

          try {
            const authResponse = await authService.refreshAccessToken();
            const newToken = authResponse.accessToken;

            // Process queued requests
            this.processQueue(null, newToken);

            // Retry original request
            originalRequest.headers!.Authorization = `Bearer ${newToken}`;
            return this.client(originalRequest);
          } catch (refreshError) {
            // Refresh failed - logout user
            this.processQueue(refreshError, null);
            await authService.logout();
            
            Alert.alert(
              'Session Expired',
              'Your session has expired. Please log in again.',
              [
                {
                  text: 'OK',
                  onPress: () => {
                    // Navigate to login screen
                    // This should be handled by the navigation state
                  },
                },
              ]
            );
            
            return Promise.reject(refreshError);
          } finally {
            this.isRefreshing = false;
          }
        }

        // Handle other errors
        return this.handleError(error);
      }
    );
  }

  private processQueue(error: any, token: string | null) {
    this.failedQueue.forEach(({resolve, reject}) => {
      if (error) {
        reject(error);
      } else {
        resolve(token);
      }
    });

    this.failedQueue = [];
  }

  private handleError(error: AxiosError): Promise<never> {
    const apiError: ApiError = {
      message: 'An unexpected error occurred',
      code: 'UNKNOWN_ERROR',
      status: 0,
      timestamp: new Date().toISOString(),
      path: error.config?.url || '',
    };

    if (error.response) {
      // Server responded with error status
      const {status, data} = error.response;
      apiError.status = status;
      apiError.message = data?.message || this.getDefaultErrorMessage(status);
      apiError.code = data?.code || this.getDefaultErrorCode(status);
      apiError.details = data?.details;
    } else if (error.request) {
      // Network error
      apiError.message = 'Network error. Please check your connection.';
      apiError.code = 'NETWORK_ERROR';
    } else {
      // Request setup error
      apiError.message = error.message || 'Request failed';
      apiError.code = 'REQUEST_ERROR';
    }

    console.error('❌ API Error:', apiError);

    // Show user-friendly error for certain cases
    if (apiError.status >= 500) {
      Alert.alert(
        'Server Error',
        'Our servers are experiencing issues. Please try again later.'
      );
    } else if (apiError.code === 'NETWORK_ERROR') {
      Alert.alert(
        'Connection Error',
        'Please check your internet connection and try again.'
      );
    }

    return Promise.reject(apiError);
  }

  private getDefaultErrorMessage(status: number): string {
    switch (status) {
      case 400:
        return 'Invalid request. Please check your input.';
      case 401:
        return 'Authentication required. Please log in.';
      case 403:
        return 'Access denied. You do not have permission.';
      case 404:
        return 'Resource not found.';
      case 409:
        return 'Conflict. The resource already exists.';
      case 422:
        return 'Validation failed. Please check your input.';
      case 429:
        return 'Too many requests. Please try again later.';
      case 500:
        return 'Internal server error. Please try again later.';
      case 502:
        return 'Service unavailable. Please try again later.';
      case 503:
        return 'Service temporarily unavailable.';
      default:
        return 'An unexpected error occurred.';
    }
  }

  private getDefaultErrorCode(status: number): string {
    switch (status) {
      case 400:
        return 'BAD_REQUEST';
      case 401:
        return 'UNAUTHORIZED';
      case 403:
        return 'FORBIDDEN';
      case 404:
        return 'NOT_FOUND';
      case 409:
        return 'CONFLICT';
      case 422:
        return 'VALIDATION_ERROR';
      case 429:
        return 'RATE_LIMIT_EXCEEDED';
      case 500:
        return 'INTERNAL_SERVER_ERROR';
      case 502:
        return 'BAD_GATEWAY';
      case 503:
        return 'SERVICE_UNAVAILABLE';
      default:
        return 'UNKNOWN_ERROR';
    }
  }

  private generateRequestId(): string {
    return `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  // Public API methods
  async get<T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.client.get<T>(url, config);
  }

  async post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.client.post<T>(url, data, config);
  }

  async put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.client.put<T>(url, data, config);
  }

  async patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.client.patch<T>(url, data, config);
  }

  async delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.client.delete<T>(url, config);
  }

  // Upload file with progress
  async upload<T = any>(
    url: string,
    formData: FormData,
    onUploadProgress?: (progressEvent: any) => void
  ): Promise<AxiosResponse<T>> {
    return this.client.post<T>(url, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress,
    });
  }

  // Download file
  async download(url: string, onDownloadProgress?: (progressEvent: any) => void): Promise<AxiosResponse<Blob>> {
    return this.client.get(url, {
      responseType: 'blob',
      onDownloadProgress,
    });
  }

  // Health check
  async healthCheck(): Promise<boolean> {
    try {
      const response = await this.client.get('/health');
      return response.status === 200;
    } catch (error) {
      return false;
    }
  }

  // Update base URL (for environment switching)
  updateBaseURL(baseURL: string) {
    this.client.defaults.baseURL = baseURL;
  }

  // Get current base URL
  getBaseURL(): string {
    return this.client.defaults.baseURL || '';
  }
}

// Export singleton instance
export const apiClient = new ApiClient();
