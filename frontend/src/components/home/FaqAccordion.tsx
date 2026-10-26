import React from 'react';

interface FaqItem {
  id: string;
  question: string;
  answer: string;
}

const FAQS: FaqItem[] = [
  {
    id: 'faq1',
    question: 'Is TrustPay Guardian connected to real banking rails or UPI networks?',
    answer: 'No. TrustPay Guardian is strictly an educational simulation platform designed to showcase production-grade Java full-stack engineering and explainable fraud risk triage. All bank account numbers, customer names, balances, and transfer transactions are completely synthetic. It does not move real currency and is not certified under PCI-DSS or banking regulatory bodies.',
  },
  {
    id: 'faq2',
    question: 'How does the 7-Rule Deterministic Fraud Engine evaluate transfers?',
    answer: 'Rather than using opaque or non-deterministic AI models, our engine applies 7 modular Strategy-pattern rules: High Amount (R1), Velocity Spike (R2), Deviation from 30-Day Average (R3), New Unverified Beneficiary (R4), Account Dormancy (R5), Circular Routing Loop (R6), and Recent Failed Login Attempts (R7). Each rule generates explicit points, clamping the final cumulative risk score between 0 and 100 with full forensic traceability.',
  },
  {
    id: 'faq3',
    question: 'How does the platform guarantee concurrency safety and prevent database deadlocks?',
    answer: 'When transferring funds between two accounts, the application server acquires pessimistic row locks strictly in ascending numeric account ID order (minId locked first, then maxId). This eliminates circular lock dependency chains in relational databases. Furthermore, client-provided UUID Idempotency-Keys cached with database unique constraints guarantee that network retries or double-clicks never result in double debits.',
  },
  {
    id: 'faq4',
    question: 'Why are JWT access tokens never stored in localStorage or sessionStorage?',
    answer: 'Storing sensitive authentication tokens in web storage makes them directly vulnerable to Cross-Site Scripting (XSS) exfiltration attacks. TrustPay Guardian keeps the short-lived access token strictly in application memory, using a 7-day HttpOnly, SameSite=Strict refresh cookie with automatic silent refresh rotation and theft reuse detection.',
  },
  {
    id: 'faq5',
    question: 'How does the Fraud Analyst triage desk resolve flagged transfers?',
    answer: 'Transfers with risk scores between 30 and 69 enter PENDING_REVIEW status, placing a dynamic pending hold on the sender balance. A certified fraud analyst reviews the forensic rule hits, enters mandatory decision notes, and selects either Release (re-checks balance and completes ledger transfer) or Confirm Block (cancels the hold and notifies the customer).',
  },
];

export const FaqAccordion: React.FC = () => {
  return (
    <section className="py-5 py-lg-6 border-bottom" style={{ backgroundColor: 'var(--tpg-bg-surface)', borderColor: 'var(--tpg-border-color)' }}>
      <div className="container" style={{ maxWidth: '840px' }}>
        <div className="text-center mb-5">
          <span className="badge bg-secondary-subtle text-secondary mb-2 px-3 py-1 fw-semibold">
            Frequently Asked Questions
          </span>
          <h2 className="display-6 fw-bold mb-3">Architecture & Technical FAQs</h2>
          <p className="text-muted">
            Key architectural decisions, security boundaries, and simulation details explained.
          </p>
        </div>

        <div className="accordion shadow-sm" id="faqAccordion">
          {FAQS.map((faq, index) => (
            <div key={faq.id} className="accordion-item border" style={{ backgroundColor: 'var(--tpg-bg-card)', borderColor: 'var(--tpg-border-color)' }}>
              <h3 className="accordion-header" id={`heading-${faq.id}`}>
                <button
                  className={`accordion-button fw-semibold ${index !== 0 ? 'collapsed' : ''}`}
                  type="button"
                  data-bs-toggle="collapse"
                  data-bs-target={`#collapse-${faq.id}`}
                  aria-expanded={index === 0 ? 'true' : 'false'}
                  aria-controls={`collapse-${faq.id}`}
                  style={{ backgroundColor: 'var(--tpg-bg-card)', color: 'var(--tpg-text-primary)' }}
                >
                  {faq.question}
                </button>
              </h3>
              <div
                id={`collapse-${faq.id}`}
                className={`accordion-collapse collapse ${index === 0 ? 'show' : ''}`}
                aria-labelledby={`heading-${faq.id}`}
                data-bs-parent="#faqAccordion"
              >
                <div className="accordion-body text-body-secondary small lh-base">
                  {faq.answer}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};
