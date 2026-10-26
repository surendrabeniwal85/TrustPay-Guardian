import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { LoginPage } from '../pages/auth/LoginPage';
import { RegisterPage } from '../pages/auth/RegisterPage';
import { AuthProvider } from '../context/AuthContext';
import { ThemeProvider } from '../context/ThemeContext';
import { ToastProvider } from '../context/ToastContext';
import { api } from '../api/axiosClient';

// Mock api
vi.mock('../api/axiosClient', async () => {
  const actual = await vi.importActual<typeof import('../api/axiosClient')>('../api/axiosClient');
  return {
    ...actual,
    api: {
      get: vi.fn(),
      post: vi.fn(),
      patch: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
    },
    setAccessToken: vi.fn(),
    getAccessToken: vi.fn(),
  };
});

const renderWithProviders = (ui: React.ReactElement) => {
  return render(
    <ThemeProvider>
      <ToastProvider>
        <AuthProvider>
          <BrowserRouter>{ui}</BrowserRouter>
        </AuthProvider>
      </ToastProvider>
    </ThemeProvider>
  );
};

describe('Auth Forms Validation and Error States', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(api.post).mockImplementation(async (url: string) => {
      if (url.includes('/auth/refresh')) {
        return Promise.reject(new Error('No active refresh token'));
      }
      return { data: { success: true, data: null } } as any;
    });
  });

  describe('LoginPage', () => {
    it('validates required email and password fields on submit', async () => {
      renderWithProviders(<LoginPage />);

      const submitBtn = screen.getByRole('button', { name: /^Sign In$/i });
      fireEvent.click(submitBtn);

      expect(await screen.findByText(/Email is required/i)).toBeInTheDocument();
      expect(screen.getByText(/Password is required/i)).toBeInTheDocument();
    });

    it('displays error message when login fails with invalid credentials', async () => {
      const authError: any = new Error('Invalid email or password');
      authError.isAxiosError = true;
      authError.response = {
        status: 401,
        data: {
          message: 'Invalid email or password',
        },
      };

      vi.mocked(api.post).mockImplementation(async (url: string) => {
        if (url.includes('/auth/refresh')) {
          return Promise.reject(new Error('No active refresh token'));
        }
        if (url.includes('/auth/login')) {
          return Promise.reject(authError);
        }
        return { data: { success: true, data: null } } as any;
      });

      renderWithProviders(<LoginPage />);

      const emailInput = screen.getByPlaceholderText(/name@example.com/i);
      const passwordInput = screen.getByPlaceholderText(/Enter password/i);

      fireEvent.change(emailInput, { target: { value: 'wrong@example.com' } });
      fireEvent.change(passwordInput, { target: { value: 'Secret123!' } });

      const submitBtn = screen.getByRole('button', { name: /^Sign In$/i });
      fireEvent.click(submitBtn);

      expect(await screen.findByText(/Invalid email or password/i)).toBeInTheDocument();
    });

    it('toggles password visibility on eye button click', () => {
      renderWithProviders(<LoginPage />);

      const passwordInput = screen.getByPlaceholderText(/Enter password/i) as HTMLInputElement;
      expect(passwordInput.type).toBe('password');

      const toggleBtn = screen.getByRole('button', { name: /Show password/i });
      fireEvent.click(toggleBtn);
      expect(passwordInput.type).toBe('text');

      fireEvent.click(toggleBtn);
      expect(passwordInput.type).toBe('password');
    });
  });

  describe('RegisterPage', () => {
    it('validates full name, email, 10-digit phone and password complexity', async () => {
      renderWithProviders(<RegisterPage />);

      const submitBtn = screen.getByRole('button', { name: /Complete Registration/i });
      fireEvent.click(submitBtn);

      expect(await screen.findByText(/Full name must be at least 2 characters/i)).toBeInTheDocument();
      expect(screen.getByText(/Email is required/i)).toBeInTheDocument();
      expect(screen.getByText(/Phone number must be exactly 10 digits/i)).toBeInTheDocument();
      expect(screen.getByText(/Password must be at least 8 characters long/i)).toBeInTheDocument();
    });

    it('displays password strength meter dynamically', () => {
      renderWithProviders(<RegisterPage />);

      const passwordInput = screen.getByPlaceholderText(/Minimum 8 characters/i);
      fireEvent.change(passwordInput, { target: { value: 'Pass123!@#' } });

      expect(screen.getByText(/Strong/i)).toBeInTheDocument();
    });
  });
});
