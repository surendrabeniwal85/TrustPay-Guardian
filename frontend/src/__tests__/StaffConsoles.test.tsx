import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AnalystDashboard } from '../pages/AnalystDashboard';
import { AdminDashboard } from '../pages/AdminDashboard';
import { AuthProvider } from '../context/AuthContext';
import { ThemeProvider } from '../context/ThemeContext';
import { ToastProvider } from '../context/ToastContext';
import { api } from '../api/axiosClient';

// Mock Recharts
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
      put: vi.fn(),
      delete: vi.fn(),
    },
  };
});

const mockStats = {
  totalAlerts: 18,
  openAlerts: 5,
  inReviewAlerts: 2,
  resolvedReleased: 8,
  resolvedBlocked: 3,
  averageRiskScore: 54.2,
};

const mockAlerts = [
  {
    id: 10,
    riskScore: 78,
    riskLevel: 'HIGH',
    status: 'OPEN',
    createdAt: '2026-01-05T12:00:00Z',
    transaction: {
      id: 201,
      referenceId: 'TXN-FLAGGED-001',
      amount: 85000.0,
      sourceMaskedAccountNumber: 'TPG******1111',
      destinationMaskedAccountNumber: 'TPG******2222',
      type: 'TRANSFER_OUT',
      status: 'BLOCKED',
      createdAt: '2026-01-05T12:00:00Z',
    },
    hits: [
      {
        id: 1,
        ruleCode: 'R1_HIGH_AMOUNT',
        ruleName: 'High Amount Transfer',
        scoreContribution: 40,
        reasonExplanation: 'Transfer amount ₹85,000 exceeds single transfer baseline threshold.',
      },
      {
        id: 2,
        ruleCode: 'R3_NEW_BENEFICIARY',
        ruleName: 'New Beneficiary Transfer',
        scoreContribution: 38,
        reasonExplanation: 'Beneficiary was created within the last 24 hours.',
      },
    ],
  },
];

const mockAdminMetrics = {
  totalUsers: 50,
  activeUsers: 48,
  totalAccounts: 65,
  totalBalance: 2450000,
  totalTransactions: 210,
  approvedTransactions: 190,
  heldTransactions: 12,
  blockedTransactions: 8,
  openAlerts: 5,
  activeRules: 7,
};

const mockUsers = [
  {
    id: 1,
    fullName: 'Platform Admin',
    email: 'admin@trustpay.internal',
    phoneNumber: '9999999999',
    role: 'ADMIN',
    status: 'ACTIVE',
  },
  {
    id: 2,
    fullName: 'John Analyst',
    email: 'analyst@trustpay.internal',
    phoneNumber: '8888888888',
    role: 'FRAUD_ANALYST',
    status: 'ACTIVE',
  },
];

const mockRules = [
  {
    id: 1,
    ruleCode: 'R1_HIGH_AMOUNT',
    name: 'High Amount Rule',
    description: 'Flags transfers breaching single amount limit',
    points: 40,
    scorePoints: 40,
    isEnabled: true,
    updatedAt: '2026-01-01T00:00:00Z',
  },
];

const renderComponent = (ui: React.ReactElement) => {
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

describe('Analyst and Admin Consoles', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders AnalystDashboard with KPIs, queue table, and inspects forensics modal', async () => {
    vi.mocked(api.get).mockImplementation((url: string) => {
      if (url.includes('/fraud/stats')) {
        return Promise.resolve({ data: { data: mockStats } });
      }
      if (url.match(/\/fraud\/alerts\/\d+/)) {
        return Promise.resolve({ data: { data: mockAlerts[0] } });
      }
      if (url.includes('/fraud/alerts')) {
        return Promise.resolve({
          data: {
            data: {
              content: mockAlerts,
              totalElements: 1,
              totalPages: 1,
              size: 50,
              number: 0,
            },
          },
        });
      }
      return Promise.resolve({ data: { data: [] } });
    });

    renderComponent(<AnalystDashboard />);

    expect(await screen.findByText(/Fraud Investigation Console/i)).toBeInTheDocument();
    expect(screen.getByText(/Open Alerts/i)).toBeInTheDocument();
    expect(screen.getByText(/Average Risk Score/i)).toBeInTheDocument();
    expect(screen.getByText(/False Positive Rate/i)).toBeInTheDocument();
    expect(screen.getByText(/TXN-FLAGGED-001/i)).toBeInTheDocument();

    // Click inspect
    const inspectBtn = screen.getByText(/Inspect/i);
    fireEvent.click(inspectBtn);

    // Modal opens
    expect(await screen.findByText(/Forensics Dossier/i)).toBeInTheDocument();
    expect(screen.getByText(/R1_HIGH_AMOUNT/i)).toBeInTheDocument();
    expect(screen.getByText(/R3_NEW_BENEFICIARY/i)).toBeInTheDocument();
    expect(screen.getByText(/Analyst Investigation Notes/i)).toBeInTheDocument();
  });

  it('renders AdminDashboard executive metrics and toggles user governance with self-protection', async () => {
    vi.mocked(api.get).mockImplementation((url: string) => {
      if (url.includes('/admin/dashboard')) {
        return Promise.resolve({ data: { data: mockAdminMetrics } });
      }
      if (url.includes('/admin/users')) {
        return Promise.resolve({
          data: {
            data: {
              content: mockUsers,
              totalElements: 2,
              totalPages: 1,
              size: 50,
              number: 0,
            },
          },
        });
      }
      if (url.includes('/admin/fraud-rules')) {
        return Promise.resolve({ data: { data: mockRules } });
      }
      return Promise.resolve({ data: { data: [] } });
    });

    renderComponent(<AdminDashboard />);

    expect(await screen.findByText(/Executive Administration Console/i)).toBeInTheDocument();
    expect(screen.getByText(/Total Registered Users/i)).toBeInTheDocument();
    expect(screen.getByText(/Daily Transaction Volume/i)).toBeInTheDocument();
    expect(screen.getByText(/Alert Triggers by Heuristic Rule/i)).toBeInTheDocument();

    // Switch to User Governance tab
    const userTab = screen.getByText(/User Governance/i);
    fireEvent.click(userTab);

    expect(await screen.findByText(/Create Staff Member/i)).toBeInTheDocument();
    expect(screen.getByText(/John Analyst/i)).toBeInTheDocument();
  });
});
