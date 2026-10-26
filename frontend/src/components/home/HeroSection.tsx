import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';

const HEADLINES = [
  'Explainable Fraud Forensics',
  'Pessimistic Concurrency Row-Locks',
  'Real-Time Risk Scoring (0–100)',
  'Atomic Double-Entry Ledgers',
];

export const HeroSection: React.FC = () => {
  const [headlineIndex, setHeadlineIndex] = useState(0);
  const [videoError, setVideoError] = useState(false);

  useEffect(() => {
    const prefersReducedMotion =
      typeof window !== 'undefined' &&
      window.matchMedia &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    if (prefersReducedMotion) return;

    const timer = setInterval(() => {
      setHeadlineIndex((prev) => (prev + 1) % HEADLINES.length);
    }, 3500);

    return () => clearInterval(timer);
  }, []);

  return (
    <section className="position-relative overflow-hidden py-5 py-lg-6" style={{ minHeight: '620px', display: 'flex', alignItems: 'center' }}>
      {/* Background Video with Poster Fallback & Gradient Overlay */}
      <div className="position-absolute top-0 start-0 w-100 h-100" style={{ zIndex: 0 }}>
        {!videoError ? (
          <video
            autoPlay
            muted
            loop
            playsInline
            poster="/media/hero-poster.jpg"
            onError={() => setVideoError(true)}
            className="w-100 h-100"
            style={{ objectFit: 'cover', opacity: 0.25 }}
            aria-hidden="true"
          >
            <source src="/media/hero.mp4" type="video/mp4" />
          </video>
        ) : null}

        {/* Gradient Overlay */}
        <div
          className="position-absolute top-0 start-0 w-100 h-100"
          style={{
            background: 'linear-gradient(135deg, rgba(15, 23, 42, 0.95) 0%, rgba(30, 41, 59, 0.88) 100%)',
          }}
        />
      </div>

      <div className="container position-relative" style={{ zIndex: 1 }}>
        <div className="row align-items-center">
          <div className="col-lg-8 mx-auto text-center">
            {/* Simulation Badge */}
            <div className="d-inline-flex align-items-center gap-2 px-3 py-1 rounded-pill bg-primary-subtle text-primary border border-primary-subtle mb-3 small fw-semibold">
              <i className="bi bi-shield-check"></i> Educational Digital Banking Platform
            </div>

            {/* Main Headline */}
            <h1 className="display-4 fw-bolder mb-3 text-white tracking-tight">
              Fraud-Aware Digital Banking with{' '}
              <span className="text-primary d-block transition-all">
                {HEADLINES[headlineIndex]}
              </span>
            </h1>

            <p className="lead text-light text-opacity-75 mb-4 mx-auto" style={{ maxWidth: '640px' }}>
              Experience production-grade Java full-stack engineering: atomic balance transfers, 7 deterministic fraud rules,
              explainable analyst triage desk, and complete audit governance.
            </p>

            {/* CTAs */}
            <div className="d-flex flex-wrap justify-content-center gap-3 mb-4">
              <Link to="/register" className="btn btn-primary btn-lg px-4 py-2 fw-semibold d-inline-flex align-items-center gap-2">
                <span>Open Simulated Account</span>
                <i className="bi bi-arrow-right"></i>
              </Link>
              <Link to="/login" className="btn btn-outline-light btn-lg px-4 py-2 fw-semibold">
                Sign In to Platform
              </Link>
            </div>

            {/* Quick Feature Badges */}
            <div className="d-flex flex-wrap justify-content-center gap-3 text-light text-opacity-75 small">
              <span className="d-flex align-items-center gap-1">
                <i className="bi bi-check-circle-fill text-success"></i> Java 25 LTS
              </span>
              <span className="d-flex align-items-center gap-1">
                <i className="bi bi-check-circle-fill text-success"></i> Spring Boot 4.1.1
              </span>
              <span className="d-flex align-items-center gap-1">
                <i className="bi bi-check-circle-fill text-success"></i> MySQL 8.4 LTS
              </span>
              <span className="d-flex align-items-center gap-1">
                <i className="bi bi-check-circle-fill text-success"></i> Zero Real Money
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
