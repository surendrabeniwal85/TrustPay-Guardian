# TrustPay Guardian — comparison and reconciliation with Claude reference files

## Files compared

- ChatGPT-authored: `TrustPay_Guardian_PRD.md` and `TrustPay_Guardian_Antigravity_Prompt_Guide.md`.
- Claude reference: `TrustPay Guardian Fraud-Aware Digital Banking Platform — PRD.pdf` (19 pages) and `TrustPay Guardian — Antigravity Step-by-Step Prompt Guide.pdf` (23 pages).

## Overall conclusion

The two sets aim at the same project and technology direction. The ChatGPT files were stronger as general architecture, syllabus mapping, beginner-friendly explanation, project documentation, traceability, and code walkthrough instructions. Claude's reference files were more concrete in several implementation contracts and more granular in phase-by-phase acceptance gates. I have updated the ChatGPT PRD and prompt guide to include those concrete requirements and the longer phased sequence. Use the updated ChatGPT Markdown files as your working pair, and retain the Claude PDFs as supporting references.

## Main differences

| Area | ChatGPT files before reconciliation | Claude reference files | Reconciled direction |
|---|---|---|---|
| Build sequence | 13–14 broader prompts, with database foundations and customer UI grouped more generally | 16 staged phases, with shared foundation, backend features, hardening, then frontend | Use the expanded 16-stage sequence in the updated prompt guide; phase gates must pass before proceeding |
| PRD precision | Strong broad requirements, architecture and documentation expectations; some schema/risk details were intentionally presented as proposals | Exact registration fields, token lifecycle, lockout, account limits, transfer rules, seven fraud rules, settings and seed expectations | Claude's specific defaults are incorporated into the addendum; no silent blending of conflicting contracts |
| Database | Earlier design sketch mentioned an optional ledger and alternative table naming | Canonical 11-table model with six Flyway migrations, explicit constraints/indexes and no required ledger table | Use the canonical table names and migrations from the updated addendum; do not add a ledger table without explicit design approval |
| Fraud policy | Deterministic, explainable rule-based engine and broad score band concept | Seven named rules R1–R7 with explicit default points/parameters and thresholds 40/70 | Use R1–R7 and the 40/70 bands as the baseline; test rule boundaries and persist readable hits |
| Authentication | JWT/RBAC, ownership checks and safe token handling covered generally | Specific 15-minute access JWT, seven-day hashed refresh token, rotation/reuse response, five-failure lockout for 15 minutes, BCrypt 12 and rate limits | Use the explicit security contract in the updated PRD; fail fast when required secrets are absent |
| Transaction correctness | Atomic simulated transfer, idempotency, concurrency and safe balance rules included | Mandatory UUID idempotency key, lock both account rows in ascending ID order, daily limit, defined release transaction | Keep these as required correctness gates, with tests demonstrating no duplicate debit or overdraft |
| UI | Responsive interactive homepage, video/media, theme toggle, login/register and role dashboards | More detailed landing-page content, theme persistence (`tpg-theme`), accessible states, specific React packages | Include the richer homepage and theme/accessibility acceptance; don't fake live metrics; video should be local/licensed and not autoplay with sound |
| Testing | Unit/integration/frontend/E2E strategy and documentation walkthrough | Testcontainers MySQL, DB CHECK test, endpoint-role matrix, ≥80% hardening target, N+1 review, OWASP self-review | Adopt these as explicit release gates; coverage is a quality signal, not a correctness guarantee |
| Git workflow | GitHub hygiene and CI included, with student-friendly commands | Feature branches, Conventional Commits, PR flow, release tags v0.1.0 and v1.0.0 | Use incremental commits and checks; branch/PR ceremony can be simplified for solo local-only work, but never skip validation |
| Documentation | Detailed code walkthrough set, 30+ interview Q&A and actual-code verification | Also calls for traceability, threat model, ADRs and honest security review | Include all these documents; final docs must describe implemented code, not the desired design |
| Syllabus/tooling | Explicit syllabus fit and Node.js described as frontend tooling | Node.js LTS required by Vite/npm toolchain | Node.js remains frontend tooling only; backend remains Java/Spring Boot. It need not be treated as a syllabus subject |

## Important reconciliation decisions

1. **Keep the exact title and simulated-only boundary.** No real money, real banking integration, real KYC, or real customer data.
2. **Use a modular monolith.** Do not introduce microservices, Kafka, Kubernetes deployment or ML fraud scoring into v1.
3. **Canonical persistence:** `users`, `accounts`, `beneficiaries`, `transactions`, `fraud_rules`, `fraud_alerts`, `fraud_alert_hits`, `refresh_tokens`, `notifications`, `audit_logs`, `system_settings`; six Flyway migrations V1–V6.
4. **Canonical fraud rules:** R1 high amount, R2 velocity, R3 unusual hour, R4 new beneficiary + large amount, R5 amount spike, R6 daily-limit proximity, R7 recent failed logins. Default review threshold 40 and block threshold 70.
5. **Token safety:** short-lived access JWT held in frontend memory; refresh token hash stored server-side and raw token sent only as HttpOnly SameSite=Strict cookie, rotated on every use.
6. **No role supplied at registration.** Public signup always produces CUSTOMER. Admin and analyst seed identities are development-only and use environment-provided passwords.
7. **No undocumented implementation claims.** Antigravity must show actual command results and identify deviations, blockers and unverified items.
8. **Versions are not hard-coded blindly.** Java 25, Spring Boot 4.1.x and MySQL 9.7 are targets from the documents' check date; verify stable compatible releases at implementation time. If local environment compatibility requires MySQL 8.4 LTS or a library pin, record it in `docs/DECISIONS.md` and test it.

## What I changed in the ChatGPT-authored files

- Added a detailed reference-alignment addendum to the PRD covering exact functional rules, security policy, schema/migrations, fraud scoring, UI acceptance, testing, Git/CI and final documentation requirements.
- Added an expanded staged build order to the prompt guide: repository skeleton, schema, shared backend foundation, auth, accounts/beneficiaries, transactions, fraud/analyst workflow, admin/notifications, hardening, frontend, integration, Docker/CI, final documentation and demo.
- Added explicit checks for MySQL CHECK constraints, token rotation/reuse, role matrix, fraud-rule boundaries, idempotency, concurrency, N+1 queries, security review, and the coverage goal.
- Clarified that Node.js is a frontend toolchain prerequisite for React/Vite, not the backend language or a core syllabus subject.
- Added this comparison document so the design decisions and differences remain transparent.

## Which files should you give Antigravity?

Use these updated files as the main instructions:

1. `TrustPay_Guardian_PRD.md` — product contract.
2. `TrustPay_Guardian_Antigravity_Prompt_Guide.md` — sequential build instructions.
3. `TrustPay_Guardian_Claude_Reference_Comparison.md` — reconciliation rationale; helpful context, but not a replacement for the PRD or prompt guide.
4. Your Java Full Stack syllabus PDF — curriculum constraint.

If the two PRDs or prompts appear to conflict, instruct Antigravity to pause and log a decision instead of generating a hybrid implementation.
