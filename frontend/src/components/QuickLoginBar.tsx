import React from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

export const QuickLoginBar: React.FC = () => {
  const { user, quickLogin } = useAuth();
  const navigate = useNavigate();

  const handleQuickLogin = async (role: 'customer1' | 'customer2' | 'analyst' | 'admin') => {
    try {
      await quickLogin(role);
      if (role === 'admin') {
        navigate('/admin');
      } else if (role === 'analyst') {
        navigate('/analyst');
      } else {
        navigate('/dashboard');
      }
    } catch (err) {
      console.error('Quick login failed:', err);
    }
  };

  return (
    <div className="bg-body-tertiary border-bottom py-2 px-3">
      <div className="container d-flex flex-wrap align-items-center justify-content-between gap-2 small">
        <div className="d-flex align-items-center gap-2">
          <span className="badge bg-primary text-uppercase">Demo Quick-Switch</span>
          <span className="text-secondary d-none d-md-inline">
            1-Click login as test personas:
          </span>
        </div>
        <div className="d-flex flex-wrap gap-2">
          <button
            onClick={() => handleQuickLogin('customer1')}
            className={`btn btn-sm ${user?.email === 'john.doe@example.com' ? 'btn-primary' : 'btn-outline-primary'}`}
            title="Customer 1: John Doe (₹50k demo account)"
          >
            👤 Customer 1 (John)
          </button>
          <button
            onClick={() => handleQuickLogin('customer2')}
            className={`btn btn-sm ${user?.email === 'jane.smith@example.com' ? 'btn-primary' : 'btn-outline-primary'}`}
            title="Customer 2: Jane Smith (₹30k demo account)"
          >
            👤 Customer 2 (Jane)
          </button>
          <button
            onClick={() => handleQuickLogin('analyst')}
            className={`btn btn-sm ${user?.role === 'FRAUD_ANALYST' ? 'btn-warning text-dark fw-semibold' : 'btn-outline-warning'}`}
            title="Fraud Analyst: analyst@trustpay.com"
          >
            🔍 Fraud Analyst
          </button>
          <button
            onClick={() => handleQuickLogin('admin')}
            className={`btn btn-sm ${user?.role === 'ADMIN' ? 'btn-danger fw-semibold' : 'btn-outline-danger'}`}
            title="Administrator: admin@trustpay.com"
          >
            ⚙️ Administrator
          </button>
        </div>
      </div>
    </div>
  );
};
