import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { configureStore } from '@reduxjs/toolkit';
import notificationReducer from '../store/slices/notificationSlice';
import { CustomerDashboardPage } from '../pages/customer/CustomerDashboardPage';
import { AccountsPage } from '../pages/customer/AccountsPage';
import { TransferPage } from '../pages/customer/TransferPage';
import { BeneficiariesPage } from '../pages/customer/BeneficiariesPage';
import { AuthProvider } from '../context/AuthContext';
import { ThemeProvider } from '../context/ThemeContext';
import { ToastProvider } from '../context/ToastContext';
import { api } from '../api/axiosClient';

// Mock Recharts ResponsiveContainer to avoid SVG size calculation errors in jsdom
vi.mock('recharts', async () => {
  const actual = await vi.importActual<typeof import('recharts')>('recharts');
  return {
    ...actual,
    ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
      <div style={{ width: 500, height: 300 }}>{children}</div>
    ),
  };
});

// Mock api
vi.mock('../api/axiosClient', async () => {
  const actual = await vi.importActual<typeof import('../api/axiosClient')>('../api/axiosClient');
  return {
    ...actual,
    api: {
      get: vi.fn(),
      post: vi.fn(),
      patch: vi.fn(),
      delete: vi.fn(),
    },
  };
});

const mockAccounts = [
  {
    id: 1,
    accountNumber: 'TPG0000000001',
    maskedAccountNumber: 'TPG******0001',
    accountType: 'SAVINGS',
    currency: 'INR',
    balance: 10000.0,
    availableBalance: 10000.0,
    heldAmount: 0.0,
    status: 'ACTIVE',
    dailyLimit: 100000.0,
    createdAt: '2026-01-01T00:00:00Z',
  },
];

const mockBeneficiaries = [
  {
    id: 1,
    nickname: 'Alice Office',
    recipientAccountId: 2,
    recipientAccountNumber: 'TPG0000000002',
    recipientMaskedAccountNumber: 'TPG******0002',
    recipientName: 'Alice Johnson',
    createdAt: '2026-01-02T00:00:00Z',
  },
];

const mockTransactions = [
  {
    id: 101,
    referenceId: 'TXN-ABC123DEF456',
    sourceAccountId: 1,
    sourceMaskedAccountNumber: 'TPG******0001',
    destinationAccountId: 2,
    destinationMaskedAccountNumber: 'TPG******0002',
    type: 'TRANSFER_OUT',
    amount: 500.0,
    status: 'APPROVED_SIMULATED',
    createdAt: '2026-01-03T10:00:00Z',
  },
];

const createTestStore = () =>
  configureStore({
    reducer: {
      notifications: notificationReducer,
    },
  });

const renderWithProviders = (ui: React.ReactElement) => {
  const store = createTestStore();
  return render(
    <Provider store={store}>
      <ThemeProvider>
        <ToastProvider>
          <AuthProvider>
            <BrowserRouter>{ui}</BrowserRouter>
          </AuthProvider>
        </ToastProvider>
      </ThemeProvider>
    </Provider>
  );
};

describe('Customer Area Pages & Components', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders CustomerDashboardPage with balance cards, chart, and quick actions', async () => {
    vi.mocked(api.get).mockImplementation((url: string) => {
      if (url.includes('/accounts')) {
        return Promise.resolve({ data: { data: mockAccounts } });
      }
      if (url.includes('/transactions')) {
        return Promise.resolve({
          data: {
            data: {
              content: mockTransactions,
              totalElements: 1,
              totalPages: 1,
              size: 5,
              number: 0,
            },
          },
        });
      }
      return Promise.resolve({ data: { data: [] } });
    });

    renderWithProviders(<CustomerDashboardPage />);

    expect(await screen.findByText(/Welcome back/i)).toBeInTheDocument();
    expect(screen.getByText(/Total Ledger Balance/i)).toBeInTheDocument();
    expect(screen.getByText(/Available Balance/i)).toBeInTheDocument();
    expect(screen.getByText(/Cashflow Analytics \(6 Months\)/i)).toBeInTheDocument();
    expect(screen.getByText(/Recent Activity/i)).toBeInTheDocument();
    expect(screen.getByText(/Send Money/i)).toBeInTheDocument();
  });

  it('renders AccountsPage with statement filters and CSV export button', async () => {
    vi.mocked(api.get).mockImplementation((url: string) => {
      if (url.includes('/statement')) {
        return Promise.resolve({
          data: {
            data: {
              content: mockTransactions,
              totalElements: 1,
              totalPages: 1,
              size: 10,
              number: 0,
            },
          },
        });
      }
      if (url.includes('/accounts')) {
        return Promise.resolve({ data: { data: mockAccounts } });
      }
      return Promise.resolve({ data: { data: [] } });
    });

    renderWithProviders(<AccountsPage />);

    expect(await screen.findByText(/Accounts & Statements/i)).toBeInTheDocument();
    expect(screen.getByText(/Account Statement/i)).toBeInTheDocument();
    expect(screen.getByText(/\[S\] CSV Export/i)).toBeInTheDocument();
  });

  it('renders TransferPage and handles review modal step with Idempotency guard', async () => {
    vi.mocked(api.get).mockImplementation((url: string) => {
      if (url.includes('/accounts')) {
        return Promise.resolve({ data: { data: mockAccounts } });
      }
      if (url.includes('/beneficiaries')) {
        return Promise.resolve({ data: { data: mockBeneficiaries } });
      }
      return Promise.resolve({ data: { data: [] } });
    });

    renderWithProviders(<TransferPage />);

    expect(await screen.findByText(/Send Money \(Instant Transfer\)/i)).toBeInTheDocument();
    expect(screen.getByText(/1\. Choose Source Account/i)).toBeInTheDocument();

    // Fill in amount
    const amountInput = screen.getByPlaceholderText('0.00');
    fireEvent.change(amountInput, { target: { value: '500' } });

    // Click Review & Confirm
    const reviewBtn = screen.getByText(/Review & Confirm Transfer/i);
    fireEvent.click(reviewBtn);

    // Modal should show
    expect(await screen.findByText(/Confirm Transfer Details/i)).toBeInTheDocument();
    expect(screen.getByText(/Idempotency Guard:/i)).toBeInTheDocument();
    expect(screen.getByText(/Authorize & Send/i)).toBeInTheDocument();
  });

  it('renders BeneficiariesPage and opens add beneficiary modal with verify check', async () => {
    vi.mocked(api.get).mockImplementation((url: string) => {
      if (url.includes('/beneficiaries')) {
        return Promise.resolve({ data: { data: mockBeneficiaries } });
      }
      return Promise.resolve({ data: { data: [] } });
    });

    renderWithProviders(<BeneficiariesPage />);

    expect(await screen.findByText(/Beneficiary Management/i)).toBeInTheDocument();
    expect(screen.getByText(/Alice Office/i)).toBeInTheDocument();

    const addBtn = screen.getByText(/Add Beneficiary/i);
    fireEvent.click(addBtn);

    expect(await screen.findByText(/Add Trusted Beneficiary/i)).toBeInTheDocument();
    expect(screen.getByText(/Recipient Account Number/i)).toBeInTheDocument();
  });
});
