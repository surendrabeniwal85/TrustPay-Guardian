import React, { useState } from 'react';

interface PipelineStage {
  id: number;
  title: string;
  badge: string;
  icon: string;
  description: string;
  technicalDetails: string;
}

const STAGES: PipelineStage[] = [
  {
    id: 1,
    title: 'Idempotency & Tenant Guard',
    badge: 'Stage 1',
    icon: 'bi-fingerprint',
    description: 'Each transfer request presents a client-generated UUID Idempotency-Key.',
    technicalDetails: 'Unique index in MySQL prevents double debits during network retries or double clicks.',
  },
  {
    id: 2,
    title: 'Ascending-ID Row Locks',
    badge: 'Stage 2',
    icon: 'bi-lock-fill',
    description: 'Pessimistic row locking acquires source and destination locks in strictly ascending order.',
    technicalDetails: 'Eliminates cross-transaction deadlock cycles on bidirectional concurrent transfers.',
  },
  {
    id: 3,
    title: '7-Rule Strategy Engine',
    badge: 'Stage 3',
    icon: 'bi-cpu-fill',
    description: 'Stateless Strategy-pattern rules evaluate amount, velocity spikes, dormancies, and logins.',
    technicalDetails: 'Produces a cumulative, clamped 0–100 risk score with explainable forensic hit records.',
  },
  {
    id: 4,
    title: 'Dynamic Band Routing',
    badge: 'Stage 4',
    icon: 'bi-diagram-3-fill',
    description: 'Transactions are triaged according to configurable threshold boundaries.',
    technicalDetails: 'Low Risk: Instant Approval; Medium Risk: Funds Reserved for Review; High Risk: Blocked.',
  },
  {
    id: 5,
    title: 'Analyst Forensics & Triage',
    badge: 'Stage 5',
    icon: 'bi-shield-check',
    description: 'Dedicated fraud analyst console provides forensic evidence and decision modals.',
    technicalDetails: 'Release re-validates balance and transfers money; Confirm Block cancels the reservation.',
  },
];

export const FraudPipelineSection: React.FC = () => {
  const [selectedStage, setSelectedStage] = useState<number>(3);

  return (
    <section id="how-it-works" className="py-5 py-lg-6" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
      <div className="container">
        <div className="text-center mb-5">
          <span className="badge bg-primary-subtle text-primary mb-2 px-3 py-1 fw-semibold">
            Explainable Detection Architecture
          </span>
          <h2 className="display-6 fw-bold mb-3">How Fraud Detection Works</h2>
          <p className="text-muted mx-auto" style={{ maxWidth: '600px' }}>
            From the moment Send Money is clicked to final ledger settlement, every transfer passes through our deterministic pipeline.
          </p>
        </div>

        {/* Pipeline Stage Cards Grid */}
        <div className="row g-3 mb-4">
          {STAGES.map((stage) => {
            const isSelected = selectedStage === stage.id;
            return (
              <div key={stage.id} className="col-12 col-md-6 col-lg">
                <div
                  onClick={() => setSelectedStage(stage.id)}
                  className={`p-3 rounded-3 h-100 cursor-pointer transition-all border ${
                    isSelected
                      ? 'border-primary bg-primary-subtle text-primary shadow'
                      : 'border-secondary-subtle bg-body-tertiary text-body'
                  }`}
                  style={{ cursor: 'pointer' }}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      setSelectedStage(stage.id);
                    }
                  }}
                  aria-pressed={isSelected}
                >
                  <div className="d-flex align-items-center justify-content-between mb-2">
                    <span className="badge bg-secondary-subtle text-secondary small">{stage.badge}</span>
                    <i className={`bi ${stage.icon} fs-5`}></i>
                  </div>
                  <h3 className="h6 fw-bold mb-1">{stage.title}</h3>
                  <p className="small text-muted mb-0 d-none d-lg-block" style={{ fontSize: '0.8rem' }}>
                    {stage.description}
                  </p>
                </div>
              </div>
            );
          })}
        </div>

        {/* Selected Stage Detail Callout */}
        <div className="card border-primary-subtle shadow-sm p-4 bg-body-tertiary">
          <div className="row align-items-center">
            <div className="col-md-8">
              <div className="d-flex align-items-center gap-2 mb-2">
                <span className="badge bg-primary">Active Inspection</span>
                <h3 className="h5 fw-bold mb-0">{STAGES[selectedStage - 1].title}</h3>
              </div>
              <p className="text-body mb-2">{STAGES[selectedStage - 1].description}</p>
              <div className="text-muted small">
                <i className="bi bi-gear-fill me-1 text-primary"></i>
                <strong>Engine Implementation:</strong> {STAGES[selectedStage - 1].technicalDetails}
              </div>
            </div>
            <div className="col-md-4 text-md-end mt-3 mt-md-0">
              <div className="d-inline-flex flex-column align-items-md-end">
                <span className="small text-muted">Execution Latency</span>
                <span className="fw-bold fs-4 text-success">&lt; 15 ms</span>
                <span className="badge bg-success-subtle text-success">Zero AI Hallucinations</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
