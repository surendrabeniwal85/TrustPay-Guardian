import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { notifyToast } from '../context/ToastContext';

// Base URL configured from environment with local dev proxy fallback
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

// In-memory access token storage (NEVER in localStorage/sessionStorage)
let inMemoryAccessToken: string | null = null;
let logoutCallback: (() => void) | null = null;

export const setAccessToken = (token: string | null) => {
  inMemoryAccessToken = token;
};

export const getAccessToken = (): string | null => {
  return inMemoryAccessToken;
};

export const registerLogoutCallback = (cb: () => void) => {
  logoutCallback = cb;
};

export const axiosClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // Required for HttpOnly refresh cookie transmission
});

// Request Interceptor: Attach in-memory JWT Access Token
axiosClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (inMemoryAccessToken && config.headers) {
      config.headers.Authorization = `Bearer ${inMemoryAccessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Silent Refresh on 401, Error Routing (403, 429, 5xx)
interface CustomAxiosRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

axiosClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<{ message?: string; errors?: Record<string, string> }>) => {
    const originalRequest = error.config as CustomAxiosRequestConfig;
    const status = error.response?.status;

    // 1. Handle 401 Unauthorized with Silent Refresh
    if (status === 401 && originalRequest && !originalRequest._retry) {
      const url = originalRequest.url || '';
      // Exclude login and refresh calls from auto-retry loops
      if (url.includes('/auth/login') || url.includes('/auth/refresh') || url.includes('/auth/register')) {
        return Promise.reject(error);
      }

      originalRequest._retry = true;

      try {
        // Attempt silent refresh via HttpOnly refresh cookie
        const refreshResponse = await axios.post<{ data: { accessToken: string } }>(
          `${API_BASE_URL}/auth/refresh`,
          {},
          { withCredentials: true }
        );

        const newAccessToken = refreshResponse.data?.data?.accessToken;
        if (newAccessToken) {
          setAccessToken(newAccessToken);
          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
          }
          return axiosClient(originalRequest);
        }
      } catch (refreshError) {
        // Refresh token expired or revoked - perform clean logout
        setAccessToken(null);
        if (logoutCallback) {
          logoutCallback();
        }
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
        return Promise.reject(refreshError);
      }
    }

    // 2. Handle 403 Forbidden
    if (status === 403) {
      if (window.location.pathname !== '/403') {
        window.location.href = '/403';
      }
    }

    // 3. Handle 429 Too Many Requests
    if (status === 429) {
      notifyToast('Too many requests. Please slow down and try again shortly.', 'warning', 'Rate Limited');
    }

    // 4. Handle 5xx Server Errors
    if (status && status >= 500) {
      notifyToast('A server error occurred. Our engineers are monitoring the platform.', 'danger', 'Server Error');
    }

    return Promise.reject(error);
  }
);

// Backward-compatible alias for existing imports
export const api = axiosClient;

export const getErrorMessage = (error: unknown): string => {
  if (axios.isAxiosError(error) && error.response?.data) {
    const data = error.response.data as { message?: string; errors?: Record<string, string>; detail?: string };
    if (data.message) {
      return data.message;
    }
    if (data.detail) {
      return data.detail;
    }
    if (data.errors && Object.keys(data.errors).length > 0) {
      return Object.values(data.errors).join(', ');
    }
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'An unexpected error occurred. Please try again.';
};
