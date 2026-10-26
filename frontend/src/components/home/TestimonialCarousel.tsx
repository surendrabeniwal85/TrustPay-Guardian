import React, { useState } from 'react';

interface Testimonial {
  id: number;
  author: string;
  role: string;
  quote: string;
  badge: string;
}

const TESTIMONIALS: Testimonial[] = [
  {
    id: 1,
    author: 'DevOps & Reliability Engineer',
    role: 'Simulated Fintech Reviewer',
    quote: 'The deterministic ascending-ID row lock design completely eliminated deadlock rollbacks under 20 parallel transfer threads. A model implementation of concurrency safety.',
    badge: 'Concurrency & Deadlock Resilience',
  },
  {
    id: 2,
    author: 'Principal Security Analyst',
    role: 'Simulated Fraud Auditor',
    quote: 'Unlike opaque machine learning models that cannot explain why a transfer was flagged, the 7-Rule Strategy engine produces exact point contributions and evidence for every alert.',
    badge: 'Explainable Forensic AI',
  },
  {
    id: 3,
    author: 'Core Banking Architect',
    role: 'Simulated Platform Reviewer',
    quote: 'The in-memory token lifecycle paired with HttpOnly refresh cookies and 404 anti-enumeration responses satisfies enterprise banking security guidelines without compromising user experience.',
    badge: 'Zero-Trust Architecture',
  },
];

export const TestimonialCarousel: React.FC = () => {
  const [currentIndex, setCurrentIndex] = useState(0);

  const prevSlide = () => {
    setCurrentIndex((prev) => (prev === 0 ? TESTIMONIALS.length - 1 : prev - 1));
  };

  const nextSlide = () => {
    setCurrentIndex((prev) => (prev === TESTIMONIALS.length - 1 ? 0 : prev + 1));
  };

  const active = TESTIMONIALS[currentIndex];

  return (
    <section className="py-5 py-lg-6" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
      <div className="container">
        <div className="text-center mb-4">
          <div className="d-inline-flex align-items-center gap-1 badge bg-warning text-dark mb-2 px-3 py-1 fw-bold">
            <i className="bi bi-info-circle-fill"></i> Educational Demonstration Content
          </div>
          <h2 className="display-6 fw-bold mb-2">Simulated Peer Reviews</h2>
          <p className="text-muted mx-auto small" style={{ maxWidth: '550px' }}>
            Synthetic architectural feedback evaluating full-stack consistency, concurrency guarantees, and fraud defenses.
          </p>
        </div>

        <div className="row justify-content-center">
          <div className="col-12 col-lg-8">
            <div className="card shadow-sm p-4 p-md-5 tpg-card border text-center position-relative">
              <span className="badge bg-primary-subtle text-primary align-self-center mb-3">
                {active.badge}
              </span>

              <p className="fs-5 fst-italic text-body mb-4 lh-base" style={{ minHeight: '90px' }}>
                &ldquo;{active.quote}&rdquo;
              </p>

              <div>
                <strong className="d-block text-primary fs-6">{active.author}</strong>
                <span className="text-muted small">{active.role}</span>
              </div>

              {/* Controls */}
              <div className="d-flex justify-content-center align-items-center gap-3 mt-4 pt-3 border-top" style={{ borderColor: 'var(--tpg-border-color)' }}>
                <button
                  type="button"
                  onClick={prevSlide}
                  className="btn btn-outline-secondary btn-sm rounded-circle p-2"
                  style={{ width: '36px', height: '36px' }}
                  aria-label="Previous testimonial slide"
                >
                  <i className="bi bi-chevron-left"></i>
                </button>

                <div className="d-flex gap-1">
                  {TESTIMONIALS.map((t, idx) => (
                    <button
                      key={t.id}
                      type="button"
                      onClick={() => setCurrentIndex(idx)}
                      className={`btn p-1 rounded-circle border-0 ${idx === currentIndex ? 'bg-primary' : 'bg-secondary bg-opacity-25'}`}
                      style={{ width: '10px', height: '10px' }}
                      aria-label={`Go to slide ${idx + 1}`}
                    />
                  ))}
                </div>

                <button
                  type="button"
                  onClick={nextSlide}
                  className="btn btn-outline-secondary btn-sm rounded-circle p-2"
                  style={{ width: '36px', height: '36px' }}
                  aria-label="Next testimonial slide"
                >
                  <i className="bi bi-chevron-right"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
