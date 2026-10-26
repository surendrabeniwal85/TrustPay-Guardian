import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { api, setAccessToken, registerLogoutCallback } from '../api/axiosClient';
import { UserSummary, AuthResponse, ApiResponse } from '../types';

interface AuthContextType {
  user: UserSummary | null;
  role: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (fullName: string, email: string, phoneNumber: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  quickLogin: (roleKey: 'admin' | 'analyst' | 'customer1' | 'customer2') => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserSummary | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const clearSession = useCallback(() => {
    setUser(null);
    setAccessToken(null);
  }, []);

  const refreshUser = useCallback(async () => {
    try {
      const res = await api.get<ApiResponse<UserSummary>>('/auth/me');
      if (res.data?.data) {
        setUser(res.data.data);
      }
    } catch {
      clearSession();
    }
  }, [clearSession]);

  // Silent refresh on initial page mount (PRD Section 9 requirement)
  useEffect(() => {
    registerLogoutCallback(clearSession);

    const performSilentRefresh = async () => {
      try {
        const refreshRes = await api.post<ApiResponse<AuthResponse>>('/auth/refresh');
        if (refreshRes.data?.data) {
          const authData = refreshRes.data.data;
          setAccessToken(authData.accessToken);
          setUser(authData.user);
        }
      } catch {
        // No active refresh session or invalid token - user remains unauthenticated
        clearSession();
      } finally {
        setIsLoading(false);
      }
    };

    performSilentRefresh();
  }, [clearSession]);

  const login = async (email: string, password: string) => {
    const res = await api.post<ApiResponse<AuthResponse>>('/auth/login', { email, password });
    if (res.data?.data) {
      const authData = res.data.data;
      setAccessToken(authData.accessToken);
      setUser(authData.user);
    }
  };

  const register = async (fullName: string, email: string, phoneNumber: string, password: string) => {
    const res = await api.post<ApiResponse<AuthResponse>>('/auth/register', {
      fullName,
      email,
      phoneNumber,
      password,
    });
    if (res.data?.data) {
      const authData = res.data.data;
      setAccessToken(authData.accessToken);
      setUser(authData.user);
    }
  };

  const logout = async () => {
    try {
      await api.post('/auth/logout');
    } catch {
      // Ignore network errors on logout
    } finally {
      clearSession();
    }
  };

  const quickLogin = async (roleKey: 'admin' | 'analyst' | 'customer1' | 'customer2') => {
    let email = '';
    let password = '';

    switch (roleKey) {
      case 'admin':
        email = 'admin@trustpay.com';
        password = 'Admin@123';
        break;
      case 'analyst':
        email = 'analyst@trustpay.com';
        password = 'Analyst@123';
        break;
      case 'customer1':
        email = 'john.doe@example.com';
        password = 'Customer@123';
        break;
      case 'customer2':
        email = 'jane.smith@example.com';
        password = 'Customer@123';
        break;
    }

    await login(email, password);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        role: user?.role || null,
        isAuthenticated: !!user,
        isLoading,
        login,
        register,
        logout,
        quickLogin,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
