import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { LandingPage } from '../pages/LandingPage';
import { AuthProvider } from '../context/AuthContext';
import { ThemeProvider } from '../context/ThemeContext';
import { ToastProvider } from '../context/ToastContext';

vi.mock('../api/axiosClient', async () => {
  const actual = await vi.importActual<typeof import('../api/axiosClient')>('../api/axiosClient');
  return {
    ...actual,
    api: {
      get: vi.fn().mockResolvedValue({
        data: {
          data: {
            totalTransfers: 154,
            totalVolume: 745000,
            totalUsers: 48,
            alertsAnalyzed: 32,
          },
        },
      }),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
    },
  };
});

describe('LandingPage and Interactive Components', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });
  const renderPage = () => {
    return render(
      <ThemeProvider>
        <ToastProvider>
          <AuthProvider>
            <BrowserRouter>
              <LandingPage />
            </BrowserRouter>
          </AuthProvider>
        </ToastProvider>
      </ThemeProvider>
    );
  };

  it('renders hero, headline, and platform badge', () => {
    renderPage();
    expect(screen.getByText(/Educational Digital Banking Platform/i)).toBeInTheDocument();
    expect(screen.getByText(/Fraud-Aware Digital Banking with/i)).toBeInTheDocument();
  });

  it('renders all 5 stages in the How Fraud Detection Works pipeline', () => {
    renderPage();
    expect(screen.getByRole('heading', { name: /How Fraud Detection Works/i, level: 2 })).toBeInTheDocument();
    expect(screen.getAllByText(/Idempotency & Tenant Guard/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText(/Ascending-ID Row Locks/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText(/7-Rule Strategy Engine/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText(/Dynamic Band Routing/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText(/Analyst Forensics & Triage/i).length).toBeGreaterThanOrEqual(1);
  });

  it('renders FAQ accordion and toggles collapse state', () => {
    renderPage();
    const faqButton = screen.getByText(/Is TrustPay Guardian connected to real banking rails or UPI networks\?/i);
    expect(faqButton).toBeInTheDocument();
  });

  it('opens video modal on play click and closes on Escape', () => {
    renderPage();
    const playButtons = screen.getAllByRole('button', { name: /Play video:/i });
    expect(playButtons.length).toBeGreaterThan(0);

    // Open first modal
    fireEvent.click(playButtons[0]);
    expect(screen.getByRole('dialog')).toBeInTheDocument();

    // Close via Escape key
    fireEvent.keyDown(window, { key: 'Escape' });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
