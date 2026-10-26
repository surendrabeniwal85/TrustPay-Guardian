import React, { useState, useEffect } from 'react';
import { api } from '../../api/axiosClient';
import { useInView } from '../../hooks/useInView';
import { useCountUp } from '../../hooks/useCountUp';

interface PublicStats {
  totalTransfers: number;
  totalVolume: number;
  totalUsers: number;
  alertsAnalyzed: number;
}

const DEFAULT_STATS: PublicStats = {
  totalTransfers: 154,
  totalVolume: 745000,
  totalUsers: 48,
  alertsAnalyzed: 32,
};

export const PublicStatsCounters: React.FC = () => {
  const [stats, setStats] = useState<PublicStats>(DEFAULT_STATS);
  const { ref, inView } = useInView({ threshold: 0.2, triggerOnce: true });

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const res = await api.get<{ data: PublicStats }>('/public/stats');
        if (res.data?.data) {
          setStats(res.data.data);
        }
      } catch {
        // Silently use defaults on network/backend failure
      }
    };
    fetchStats();
  }, []);

  const countTransfers = useCountUp({ end: stats.totalTransfers, active: inView, duration: 1800 });
  const countVolume = useCountUp({ end: stats.totalVolume, active: inView, duration: 2000 });
  const countUsers = useCountUp({ end: stats.totalUsers, active: inView, duration: 1500 });
  const countAlerts = useCountUp({ end: stats.alertsAnalyzed, active: inView, duration: 1600 });

  const formatCurrency = (val: number): string => {
    return '₹ ' + val.toLocaleString('en-IN');
  };

  return (
    <section ref={ref} className="py-5 border-bottom" style={{ backgroundColor: 'var(--tpg-bg-surface)', borderColor: 'var(--tpg-border-color)' }}>
      <div className="container">
        <div className="row g-4 text-center">
          {/* Total Volume */}
          <div className="col-6 col-lg-3">
            <div className="p-4 tpg-card tpg-card-hover h-100">
              <i className="bi bi-cash-stack fs-1 text-primary mb-2 d-block"></i>
              <div className="display-6 fw-bold mb-1 text-primary">
                {formatCurrency(countVolume)}
              </div>
              <div className="text-muted small fw-semibold text-uppercase tracking-wider">
                Simulated Volume
              </div>
            </div>
          </div>

          {/* Transfers Processed */}
          <div className="col-6 col-lg-3">
            <div className="p-4 tpg-card tpg-card-hover h-100">
              <i className="bi bi-arrow-repeat fs-1 text-info mb-2 d-block"></i>
              <div className="display-6 fw-bold mb-1">
                {countTransfers.toLocaleString()}
              </div>
              <div className="text-muted small fw-semibold text-uppercase tracking-wider">
                Transfers Executed
              </div>
            </div>
          </div>

          {/* Active Accounts */}
          <div className="col-6 col-lg-3">
            <div className="p-4 tpg-card tpg-card-hover h-100">
              <i className="bi bi-people fs-1 text-success mb-2 d-block"></i>
              <div className="display-6 fw-bold mb-1">
                {countUsers.toLocaleString()}
              </div>
              <div className="text-muted small fw-semibold text-uppercase tracking-wider">
                Simulated Accounts
              </div>
            </div>
          </div>

          {/* Alerts Analyzed */}
          <div className="col-6 col-lg-3">
            <div className="p-4 tpg-card tpg-card-hover h-100">
              <i className="bi bi-shield-exclamation fs-1 text-warning mb-2 d-block"></i>
              <div className="display-6 fw-bold mb-1 text-warning">
                {countAlerts.toLocaleString()}
              </div>
              <div className="text-muted small fw-semibold text-uppercase tracking-wider">
                Alerts Triaged
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
