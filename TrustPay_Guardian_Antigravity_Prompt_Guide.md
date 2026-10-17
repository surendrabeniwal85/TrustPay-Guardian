# Antigravity Prompt Guide

## TrustPay Guardian: Fraud-Aware Digital Banking Platform

This guide tells Antigravity how to build the project **in a controlled
sequence**, verify every stage, and explain the result. It is
intentionally separate from the PRD: the PRD describes what the product
must be; this guide describes how the coding agent should work.

## 1. How to use this guide

1.  Open the correct project root in Antigravity.
2.  Attach or make available `TrustPay_Guardian_PRD.md` and
    `Java FS Syllabus.pdf`.
3.  Paste **Prompt 0** first. Do not ask the agent to generate the
    entire app immediately.
4.  Review the inventory and plan. Confirm versions and repository
    location.
5.  Run one phase prompt at a time.
6.  After each phase, inspect the changed files, run the commands, and
    check the acceptance criteria.
7.  Commit only after checks pass.
8.  If a check fails, ask Antigravity to diagnose and fix the specific
    failure before moving forward.
9.  Keep this guide and the PRD in the repository under `docs/` or the
    root, as appropriate.
10. Never paste real secrets into prompts or commit them.

**Important:** The attached syllabus lists Core Java, modern Java 8--21,
Git, Maven, SQL/MySQL-compatible relational concepts, Spring Boot REST
APIs, Spring Security/JWT, backend testing, logging, SOLID/design
patterns, HTML/CSS/Bootstrap, JavaScript, TypeScript, React, frontend
testing, full-stack integration, microservices concepts, Docker/CI/CD,
cloud awareness, OWASP, AI-augmented development, Agile, and a capstone.
The stack below uses these topics where they fit. MySQL is mandatory for
this project. Advanced fraud workflow, idempotency, concurrency, audit
trail, migrations, and threat modelling are project-specific additions.

------------------------------------------------------------------------

## 2. Rules that apply to every prompt

Paste these rules at the end of any phase prompt if Antigravity stops
following the workflow:

-   Work only in the opened TrustPay Guardian repository.
-   Read the PRD and this guide before making changes.
-   Inspect existing files before creating or replacing files.
-   Do not delete or overwrite useful code without explaining why and
    asking first.
-   Do not implement several unverified phases in one pass.
-   Keep the application a modular monolith for v1.
-   Use MySQL, not H2 or SQLite as the primary application database. H2
    may only be used if explicitly justified for isolated unit tests;
    integration tests should verify MySQL compatibility.
-   Use Java 25 LTS as the required baseline, with the latest security patch
    in the Java 25 line verified on the implementation date.
-   Use the latest stable Spring Boot 4.1.x GA patch compatible with Java 25;
    never select milestone, RC or snapshot builds.
-   Use MySQL 9.7 LTS as the preferred database target, unless the available
    environment requires a documented and tested alternative such as MySQL 8.4 LTS.
-   Verify exact stable versions for React, TypeScript, Node.js LTS, Maven GA,
    build plugins and testing libraries from official release pages before
    creating configuration. Record versions and source URLs in
    `docs/TECH_STACK_AND_VERSION_POLICY.md`.
-   Configure `maven.compiler.release=25` (or the equivalent supported compiler
    plugin configuration), and align IDE, terminal, CI and runtime versions.
-   Do not use preview/nightly dependencies or casually mix Spring Boot 3 and 4
    libraries. If a dependency is incompatible, stop and report a verified fix.
-   Keep frontend and backend contracts aligned and documented.
-   Do not invent routes, schema, methods, test outcomes, repository
    history, or screenshots.
-   Do not report a test as passing unless it was actually run and the
    result was observed.
-   Do not use fake metrics labelled as real/live.
-   Do not connect to real banks or move real funds. All transfers are
    simulations.
-   Do not commit `.env`, credentials, JWT secrets, passwords, real
    personal data, or generated local data.
-   Do not expose secrets in logs, errors, documentation, screenshots,
    or sample output.
-   Keep controllers thin, services responsible for business rules,
    repositories responsible for persistence, and DTOs as API
    boundaries.
-   Enforce authentication, roles, and ownership checks on the backend.
-   Include loading, empty, success, error, and forbidden UI states.
-   After changes, summarize files created/changed, commands run, test
    results, known issues, and next phase.
-   Stop at the phase gate. Do not continue automatically to the next
    phase.

------------------------------------------------------------------------

## 3. Prompt 0 --- Repository inspection and planning (run first)

``` text
You are the senior software architect and mentor for:
TrustPay Guardian: Fraud-Aware Digital Banking Platform.

Read the complete PRD and the attached Java Full Stack syllabus. Treat the PRD as the functional specification and the syllabus as the curriculum constraint. MySQL is mandatory.

Do not write application code yet.

Inspect the current repository, Git status/history where available, installed tool versions, and existing files. Report:
1. Current root and exact folder structure.
2. Existing frontend/backend/database stack and entry points.
3. Java, Maven, Node, npm, and MySQL versions where detectable.
4. Existing application behaviour and features, distinguishing verified facts from assumptions.
5. Gaps against the PRD.
6. Version compatibility recommendations.
7. Proposed folder/file structure, adapted to what exists.
8. Proposed database entities and migration order.
9. Proposed REST endpoints and role permissions.
10. Threats and important security decisions.
11. Implementation phases and exact acceptance gates.
12. Commands I will run on Windows PowerShell and alternatives for macOS/Linux where useful.
13. Risks, unknowns, and questions that genuinely block progress.

Create or update only planning documentation if needed. Do not change source code, configuration, database contents, or application behaviour. Do not invent Git history or pretend timestamps prove the true authoring sequence.

Finish with a concise plan and wait for my approval before implementing Phase 1.
```

### Gate 0 checklist

-   [ ] Existing files inventoried.
-   [ ] Version compatibility explained.
-   [ ] MySQL chosen and local setup understood.
-   [ ] Architecture is modular monolith.
-   [ ] Role matrix and transaction states defined.
-   [ ] No application code changed before approval.

------------------------------------------------------------------------

## 4. Prompt 1 --- Repository skeleton and toolchain

``` text
Implement Phase 1 only for TrustPay Guardian, using the approved plan and PRD.

Set up or normalize a repository with:
- frontend/: Vite + React + TypeScript.
- backend/: Spring Boot + Maven.
- docs/: PRD links, architecture and project workflow placeholders only where useful.
- .github/workflows/ci.yml.
- root README.md, .gitignore, .editorconfig, .env.example, and useful GitHub templates.
- MySQL local-development configuration placeholders; do not expose credentials.

Use a Java/Spring Boot combination verified to be compatible. Keep MySQL as the application database. Add a minimal backend health endpoint and a minimal frontend shell that identifies the project by its exact title.

Do not implement authentication, transfers, fraud rules, or dashboards in this phase.

Run the backend compile/test command and frontend lint/build commands that are available. Report exact commands and actual results. If something cannot be run because a tool is missing, say so and provide the next step. Stop at the Phase 1 gate.
```

### Gate 1

-   [ ] Backend compiles.
-   [ ] Frontend production build succeeds.
-   [ ] Root and module ignore rules prevent secrets and generated files
    being committed.
-   [ ] Local setup instructions match the actual files.
-   [ ] Health endpoint responds as documented.

------------------------------------------------------------------------

## 5. Prompt 2 --- Database and domain foundations

``` text
Implement Phase 2 only.

Review the PRD data model and refine it before coding. Use MySQL and Flyway versioned migrations. Create only the tables required for the first working increment: users, roles, user_roles, accounts, transactions, and the chosen ledger representation. Add fraud/audit tables only if their initial relationships are defined clearly or defer them to their designated phase.

Requirements:
- Explicit primary keys, foreign keys, unique constraints, indexes, and deliberate deletion rules.
- Use DECIMAL for stored simulated monetary amounts and BigDecimal in Java.
- Store timestamps consistently and document timezone handling.
- Never use floating-point types for monetary values.
- Avoid duplicate sources of truth for balances. If a balance column and ledger are both maintained, define atomic update and reconciliation rules.
- Add entities, repositories, and only the minimum required tests.
- Do not implement authentication or UI features yet.

Verify migrations against a dedicated local development/test MySQL database. Do not run destructive SQL against an unknown database. Update docs/DATABASE_DESIGN.md with the actual schema and ER diagram. Stop after the schema and tests are verified.
```

### Gate 2

-   [ ] Clean migration succeeds on an empty dedicated database.
-   [ ] Entity relationships match SQL constraints.
-   [ ] Monetary types and balance model are documented.
-   [ ] Tests use a safe database.
-   [ ] No production data is touched.

------------------------------------------------------------------------

## 6. Prompt 3 --- Authentication and backend authorization

``` text
Implement Phase 3 only: secure authentication and role-based authorization.

Use Spring Security and a compatible JWT implementation. Implement:
- Customer registration.
- Login.
- Password hashing with an adaptive password encoder.
- A /api/v1/auth/me endpoint.
- Logout/token invalidation behaviour appropriate to the chosen token design.
- Roles CUSTOMER, FRAUD_ANALYST, ADMIN.
- Public registration can assign CUSTOMER only.
- Protected backend routes and clear 401/403 behaviour.
- Safe DTOs and validation.
- Global exception handling.
- A documented token-storage decision for the React app.
- Rate limiting or a modest documented protection against repeated login attempts.
- No passwords, raw tokens, hashes, or secrets in logs.

Never trust a role supplied by the client. Do not rely on frontend route hiding as security. Prevent privilege escalation and check account status.

Add JUnit/Mockito/MockMvc tests for registration, login, invalid credentials, role restrictions, unauthenticated access, and attempts to register as ADMIN. Update API and security docs. Run tests and stop at this phase gate.
```

### Gate 3

-   [ ] Passwords are hashed.
-   [ ] Registration cannot grant privileged roles.
-   [ ] API role restrictions have negative tests.
-   [ ] No secret is hardcoded.
-   [ ] Token lifecycle and storage risks are documented.

------------------------------------------------------------------------

## 7. Prompt 4 --- Public landing page, registration/login UI, theme

``` text
Implement Phase 4 only: the initial user-facing shell.

Build an accessible, responsive React + TypeScript UI using HTML5, CSS3, and Bootstrap 5-compatible styling. Use the PRD's design direction and exact title:
TrustPay Guardian: Fraud-Aware Digital Banking Platform.

Create:
- Public home page with feature sections and a how-it-works flow.
- Login and registration forms wired to the actual backend API.
- Dark/light theme toggle with persisted preference.
- Shared navigation, footer, buttons, form controls, and toast/error components.
- Relevant project video or original lightweight animation on the home page. Prefer a local, licensed/owned asset. If no valid asset exists, use a polished original animation and document the media placeholder; never use unlicensed media.
- No autoplay sound, reduced-motion support, captions or text equivalent, and a poster/fallback.
- Loading, success, field-error, server-error, and offline states.
- Responsive layout and keyboard-accessible focus states.

Do not fake authentication or invent API responses. If the backend endpoint is not available, stop and report the dependency rather than hardcoding a successful login. Add frontend tests for validation, theme, and auth flow. Stop at this phase gate.
```

### Gate 4

-   [ ] Theme works and persists.
-   [ ] Login/register use actual API contracts.
-   [ ] Public home page includes accessible media/fallback.
-   [ ] Mobile and keyboard interactions are checked.
-   [ ] No fake authenticated state is presented as real.

------------------------------------------------------------------------

## 8. Prompt 5 --- Customer dashboard and account ownership

``` text
Implement Phase 5 only.

Create customer account and dashboard functionality:
- Customer dashboard layout.
- Simulated account summary.
- Recent transaction list and paginated transaction history.
- Profile details limited to approved fields.
- Account details with safe masked demo identifiers.
- Filters where the backend supports them.
- Useful empty/loading/error states.
- Responsive charts only when based on API data or clearly labelled synthetic seed data.

Add backend endpoints and service methods as needed, but keep the API documented and consistent. Every account and transaction query must be scoped to the authenticated user. Do not trust userId or account ownership values from the client. Test attempts to access another user's records.

Add deterministic development seed data without committing usable production credentials. Update the architecture, API, database and frontend/backend flow documentation. Run tests and stop.
```

### Gate 5

-   [ ] Customer sees only their own data.
-   [ ] Pagination/filter behaviour matches API documentation.
-   [ ] Demo values are clearly labelled.
-   [ ] Ownership checks have negative tests.

------------------------------------------------------------------------

## 9. Prompt 6 --- Safe simulated transfers

``` text
Implement Phase 6 only: simulated transfer processing.

Build the transfer flow using the PRD:
- Transfer form and backend request DTO.
- Validate positive amount, currency, source ownership, source and recipient status, available balance, and configured limits.
- Use BigDecimal and MySQL DECIMAL.
- Use a database transaction for balance/ledger/transaction writes.
- Define explicit transfer statuses and allowed transitions.
- Add idempotency protection so a retried submission does not create a duplicate transfer.
- Add appropriate row locking or another verified concurrency strategy to prevent overspending from concurrent requests.
- Do not debit funds for transactions placed on hold.
- Return a unique reference and clear simulated status.
- Never imply that real money has moved.

Write unit and integration tests for success, invalid amount, insufficient funds, wrong owner, inactive account, duplicate idempotency key, rollback, and concurrent transfer attempts. If concurrency behaviour cannot be reliably tested in the current environment, document the limitation and provide a repeatable manual test.

Update API docs, data-flow diagrams, and transaction state documentation. Stop at the gate.
```

### Gate 6

-   [ ] Transfer updates are atomic.
-   [ ] Duplicate requests do not create duplicate transfers.
-   [ ] Concurrent requests cannot overspend.
-   [ ] Held transfers do not debit balances.
-   [ ] Error responses are safe and documented.

------------------------------------------------------------------------

## 10. Prompt 7 --- Fraud-risk engine

``` text
Implement Phase 7 only: an explainable, deterministic fraud-risk engine.

Do not claim this is machine learning. Use clear interfaces/classes and documented rules, such as:
- Amount above configured demo limit.
- Rapid repeated transfers in a defined time window.
- Repeated failed transfer attempts.
- Recipient flagged in synthetic demo data.
- Optional simple account-history deviation only if it is deterministic and testable.

Each rule must produce a stable rule code, bounded score contribution, and human-readable explanation. Return a documented score from 0 to 100 and risk band. Keep thresholds in typed configuration or a single policy component; avoid unexplained magic numbers.

Define the transfer decision policy:
- Low risk: eligible for simulated approval after all normal validations.
- Medium risk: pending review or an explicit confirmation flow.
- High risk: hold/reject according to the documented policy and create a case.
- No held transfer should debit balances before approval.

Persist risk result and rule hits needed for explainability. Avoid protected personal characteristics. Add tests for every rule, score boundaries, multiple rules, missing data, and the integration with transfer processing. Update fraud engine docs and diagrams. Stop.
```

### Gate 7

-   [ ] Rule outputs are explainable.
-   [ ] Score boundaries are tested.
-   [ ] Risk decision and transfer status agree.
-   [ ] No false claim of ML or production-grade detection.

------------------------------------------------------------------------

## 11. Prompt 8 --- Fraud analyst case workflow

``` text
Implement Phase 8 only.

Build the analyst dashboard and backend case workflow:
- Paginated/filterable fraud case queue.
- Case detail with risk score, reason codes, transaction reference, status, and relevant history.
- Assign/review functionality only if specified and permissioned.
- Decision values such as CONFIRMED_FRAUD, FALSE_POSITIVE, and NEEDS_REVIEW, with required reason text.
- Valid state transitions and protection against duplicate or stale decisions.
- Record reviewer identity and timestamps.
- Create audit events for every review decision.
- Enforce FRAUD_ANALYST or ADMIN permission on the backend.
- Do not expose unrelated customer data.

Add backend and frontend tests for permitted/forbidden actions, invalid transitions, required reasons, and audit creation. Update docs and stop at the gate.
```

### Gate 8

-   [ ] Analyst-only routes reject customers.
-   [ ] Every decision is attributable and auditable.
-   [ ] Invalid case transitions are rejected.
-   [ ] Queue filtering/pagination works against actual data.

------------------------------------------------------------------------

## 12. Prompt 9 --- Admin dashboard and audit trail

``` text
Implement Phase 9 only.

Create the protected administrator experience:
- Summary cards based on actual API data.
- User search and pagination.
- Safe account activation/deactivation.
- Role assignment limited to approved roles.
- Recent audit events and filters.
- Optional fraud-rule settings only if configuration validation, authorization, audit, and tests are included.

Security constraints:
- Enforce ADMIN on the backend.
- Prevent self-role escalation.
- Prevent accidentally removing/deactivating the final active administrator.
- Do not expose password hashes, tokens, secrets, or unnecessary PII.
- Prefer deactivation to destructive deletion.
- Audit every sensitive administrative action.
- Never expose an API to modify or delete historical audit events.

Add negative authorization tests and UI confirmation for consequential actions. Update admin and security docs. Stop at the gate.
```

### Gate 9

-   [ ] Non-admin API calls are denied.
-   [ ] Privilege escalation is blocked.
-   [ ] Audit records are created for admin changes.
-   [ ] User list is paginated and safely filtered.

------------------------------------------------------------------------

## 13. Prompt 10 --- API quality, logging, health, and documentation

``` text
Implement Phase 10 only.

Improve quality without changing product behaviour unnecessarily:
- Generate and validate OpenAPI 3 documentation and Swagger UI.
- Add consistent API error responses and field validation details.
- Add SLF4J/Logback logging with safe contextual identifiers.
- Ensure secrets, tokens, passwords, and unnecessary PII are not logged.
- Add safe Spring Boot Actuator health endpoints and restrict sensitive actuator data.
- Add JaCoCo coverage reporting.
- Add SonarQube/SonarLint configuration only if it can be used without making the build fragile.
- Add frontend lint/format configuration and verify it.
- Add accessibility checks and tests where practical.
- Ensure tests reflect actual requirements rather than merely asserting that code executes.

Run all relevant tests and builds. Report exact results and unresolved warnings. Update docs and stop.
```

### Gate 10

-   [ ] OpenAPI matches actual controllers and DTOs.
-   [ ] Error response examples match runtime responses.
-   [ ] Sensitive data is absent from logs.
-   [ ] Health endpoint exposure is safe.
-   [ ] Coverage is reported accurately, not treated as proof of
    correctness.

------------------------------------------------------------------------

## 14. Prompt 11 --- Docker, CI, and reproducible setup

``` text
Implement Phase 11 only.

Add Docker support and GitHub Actions in a maintainable way:
- Dockerfile for backend.
- Dockerfile for frontend if useful for the selected deployment mode.
- compose.yaml for local frontend/backend/MySQL development.
- Health checks and service dependencies where appropriate.
- Persistent local MySQL volume only for local development; document reset procedures and warn before destructive resets.
- Environment variable placeholders; no real credentials.
- GitHub Actions workflow to build/test backend, lint/test/build frontend, and publish test results.
- Pin or constrain dependency versions appropriately and commit the frontend lockfile and Maven wrapper.
- Keep Kubernetes as awareness/documentation only for v1.

Test a clean build or clearly report any environment limitation. Ensure compose configuration does not expose MySQL publicly by default. Update setup and deployment docs. Stop.
```

### Gate 11

-   [ ] Clean build instructions are accurate.
-   [ ] GitHub Actions checks are meaningful.
-   [ ] Secrets are supplied through environment configuration.
-   [ ] MySQL is not unintentionally exposed.
-   [ ] CI fails when required tests fail.

------------------------------------------------------------------------

## 15. Prompt 12 --- Final documentation and project walkthrough

``` text
Implement the final documentation and review phase for:
TrustPay Guardian: Fraud-Aware Digital Banking Platform.

Inspect the complete implementation and verify every documentation claim against actual files and test results.

Create or update:
- docs/PROJECT_OVERVIEW.md
- docs/PROJECT_STRUCTURE.md
- docs/ARCHITECTURE.md
- docs/DEVELOPMENT_WORKFLOW.md
- docs/BACKEND_CODE_WALKTHROUGH.md
- docs/API_DOCUMENTATION.md
- docs/DATABASE_DESIGN.md
- docs/FRONTEND_BACKEND_FLOW.md
- docs/SECURITY_AND_CONFIGURATION.md
- docs/TESTING_AND_DEBUGGING.md
- docs/DEPLOYMENT_GUIDE.md
- docs/LEARNING_AND_INTERVIEW_GUIDE.md
- docs/GLOSSARY.md
- docs/KNOWN_ISSUES_AND_IMPROVEMENTS.md
- docs/REQUIREMENTS_TRACEABILITY.md
- docs/THREAT_MODEL.md
- architecture decision records for key decisions.

Also update the root README with exact setup, commands, architecture diagram, stack, limitations, demo instructions and links to every document.

Requirements:
1. Inspect every major source folder and identify the actual application entry point.
2. Trace real features from UI to API to service to repository to MySQL and back.
3. Explain important files, classes, methods, inputs, outputs, callers, dependencies and side effects.
4. Document every actual API route, method, request/response, role requirement, validation and error.
5. Describe the schema and migrations that exist, not a hypothetical schema.
6. Explain application startup, configuration, test, build, debug and deployment workflows.
7. Distinguish verified development history from reconstructed dependency order and recommended rebuild order.
8. Use Git history only when available; do not fabricate sequence or authorship.
9. Include at least 30 project-specific interview questions with detailed answers.
10. Provide beginner-friendly exercises that require me to make changes myself.
11. Mark incomplete features, known issues, and future improvements honestly.
12. Link the docs to one another and ensure all file paths are valid.

Do not change application code in this phase unless a documentation-blocking defect is found; if so, report it separately and ask before changing behaviour. Run the final tests/builds and report actual results. Stop after the walkthrough is complete.
```

### Gate 12

-   [ ] All documentation matches source code.
-   [ ] All actual routes and database tables are documented.
-   [ ] Development history is not invented.
-   [ ] At least 30 project-specific Q&As are included.
-   [ ] A clean-start demonstration has been performed or the missing
    steps are documented.

------------------------------------------------------------------------

## 16. Prompt 13 --- Independent code review and security review

``` text
Perform a review only; do not refactor the entire application automatically.

Review TrustPay Guardian against the PRD, attached Java Full Stack syllabus, and OWASP-oriented secure coding practices.

Check:
- Broken access control and missing role checks.
- Customer access to another customer's accounts or transactions.
- Registration role escalation.
- JWT validation, expiry, storage, logout/revocation, and secret management.
- SQL injection risks and unsafe dynamic queries.
- Validation and error-message leakage.
- Password handling and brute-force controls.
- CORS/CSRF decisions.
- Transaction atomicity, idempotency, concurrency, and balance/ledger consistency.
- Fraud score boundaries and status transition logic.
- Audit completeness and sensitive-data leakage.
- Missing indexes, pagination issues, N+1 queries, and performance concerns.
- Frontend XSS risks, unsafe HTML, inaccessible controls, and misleading demo data.
- Dependency vulnerabilities and CI gaps where tools are available.
- Documentation inaccuracies.

Produce docs/SECURITY_REVIEW.md and docs/CODE_REVIEW.md with severity, evidence (file path and method), reproduction or test, impact, and recommended fix. Distinguish confirmed vulnerabilities from possible concerns. Fix only critical, clearly bounded defects after describing the changes, and run regression tests. Do not claim formal compliance or a professional penetration test.
```

------------------------------------------------------------------------

## 17. Prompt 14 --- Demonstration and viva rehearsal

``` text
Prepare me to demonstrate and defend TrustPay Guardian: Fraud-Aware Digital Banking Platform.

Use the actual implementation and its docs. Create docs/DEMO_SCRIPT.md containing:
1. A 5-minute demo sequence.
2. A 10-minute detailed demo sequence.
3. Test accounts/roles using safe development-only instructions.
4. The exact UI actions and API calls to demonstrate.
5. One low-risk transfer and one high-risk/held transfer using synthetic data.
6. A fraud analyst decision and the resulting audit event.
7. An admin action and a denied unauthorized action.
8. What database records should change after each step.
9. Expected results and troubleshooting notes.
10. A clear explanation that no real funds move.

Then quiz me one question at a time. Ask about actual class names, controller/service/repository responsibilities, SQL schema, Spring Security, JWT, transaction safety, risk scoring, testing, Git, Maven, and architecture trade-offs. Wait for my response, assess it, explain gaps, and ask the next question. Do not reveal all answers before I try.
```

------------------------------------------------------------------------

## 18. Prompt for a failure or build error

Use this whenever a phase fails. Paste the exact terminal output after
the prompt.

``` text
Diagnose this failure in TrustPay Guardian.

Do not guess and do not rewrite unrelated files.

1. Identify the earliest meaningful error in the log.
2. Explain the likely root cause and evidence.
3. Inspect the relevant configuration and source files.
4. Propose the smallest safe fix.
5. Explain what the fix changes and any side effects.
6. Apply only the targeted fix after describing it.
7. Run the narrowest relevant test, then the required phase checks.
8. Report the exact command and actual result.
9. Update documentation only if the setup or behaviour changed.
10. Do not proceed to the next phase until the current gate passes.

Error output:
[PASTE THE FULL RELEVANT ERROR HERE]
```

------------------------------------------------------------------------

## 19. GitHub workflow guide

### Initial repository setup

1.  Create a GitHub repository named `trustpay-guardian` if it does not
    already exist.
2.  Initialize Git at the project root, not inside the frontend or
    backend subdirectory.
3.  Review `.gitignore` before the first commit.
4.  Ensure `.env`, credentials, `target/`, `node_modules/`, logs,
    generated reports, and IDE-specific files are not staged.
5.  Add the PRD and prompt guide under `docs/` or keep the PRD at the
    root and link to it from README.
6.  Make the first commit only after the skeleton builds.

### Suggested commit rhythm

-   Commit 1: `chore: initialize full-stack workspace`
-   Commit 2: `feat(database): add initial MySQL migrations`
-   Commit 3: `feat(auth): add customer authentication and RBAC`
-   Commit 4: `feat(ui): add public pages and theme toggle`
-   Commit 5: `feat(account): add customer account dashboard`
-   Commit 6: `feat(transaction): add simulated transfer workflow`
-   Commit 7: `feat(fraud): add explainable risk rules and cases`
-   Commit 8: `feat(admin): add user management and audit views`
-   Commit 9: `test: add full-stack regression coverage`
-   Commit 10: `docs: add project walkthrough and demo guide`

Only use a commit message when the corresponding changes actually exist
and pass the intended checks.

### Before each push

Run the applicable checks, inspect `git status`, inspect the diff, and
verify that no secret or generated file is included. Never force-push or
delete branches as a routine fix.

------------------------------------------------------------------------

## 20. Recommended learning order for the student

Use the project to learn, not just to generate files.

1.  Core Java: classes, interfaces, collections, exceptions, generics,
    streams, records, and `BigDecimal`.
2.  Git and Maven: repository, commits, branches, `pom.xml`, lifecycle,
    dependencies.
3.  SQL/MySQL: joins, constraints, indexes, transactions, normalization,
    isolation.
4.  Spring Core: dependency injection, beans, configuration.
5.  Spring Boot REST: controllers, DTOs, validation, error handling,
    OpenAPI.
6.  JPA: entities, repositories, relationships, transactions, query
    behaviour.
7.  Spring Security/JWT: authentication, authorization, filters, roles,
    token lifecycle.
8.  React/TypeScript: components, props, state, hooks, forms, routing.
9.  Full-stack integration: API calls, CORS, error mapping, protected
    routes.
10. Testing: JUnit, Mockito, MockMvc, React Testing Library, Playwright
    smoke tests.
11. Advanced project logic: transaction concurrency, idempotency, audit
    events, fraud rules.
12. Operations: logs, Actuator, Docker, CI/CD, safe configuration.
13. Architecture and communication: explain trade-offs, limitations, and
    why the v1 design is a modular monolith.

For each feature, use this learning loop: **Read the requirement →
inspect the UI → call the API → trace controller → trace service →
inspect repository/SQL → run tests → change one small thing yourself →
explain it aloud.**

------------------------------------------------------------------------

## 21. Final release checklist

### Product

-   [ ] Exact project title appears in README and home page.
-   [ ] Public home page has relevant accessible media or an original
    animation fallback.
-   [ ] Login, registration, dark/light theme and role-specific
    dashboards work.
-   [ ] Customer, analyst, and admin workflows work.
-   [ ] All transactions are clearly simulated.

### Backend and database

-   [ ] MySQL is the primary database.
-   [ ] Versioned migrations work from a clean database.
-   [ ] Passwords are hashed.
-   [ ] JWT/security and RBAC are tested.
-   [ ] Ownership is checked on every customer-specific endpoint.
-   [ ] Transfer state, balance updates, idempotency and concurrency are
    tested.
-   [ ] Fraud rules are explainable and tested.
-   [ ] Audit events cover sensitive actions.

### Frontend

-   [ ] API contracts match backend.
-   [ ] Loading/error/empty/success states are implemented.
-   [ ] Responsive and keyboard checks are completed.
-   [ ] Theme preference persists.
-   [ ] No fake data is presented as live data.

### Quality and GitHub

-   [ ] Backend tests/build pass.
-   [ ] Frontend lint/tests/build pass.
-   [ ] CI runs required checks.
-   [ ] No secrets or real personal data are committed.
-   [ ] Commit history is meaningful.
-   [ ] README commands work on a clean clone or limitations are
    explicit.

### Documentation

-   [ ] All 14 core docs plus traceability and threat model exist as
    appropriate.
-   [ ] Every path, API and schema claim is verified.
-   [ ] Historical sequence is distinguished from recommended sequence.
-   [ ] Known issues and limitations are disclosed.
-   [ ] At least 30 project-specific interview questions are included.
-   [ ] Demo script is reproducible.

**Final principle:** Antigravity is a coding assistant, not proof of
correctness. Treat every generated change as a proposal that must pass
code review, tests, and a real run before being accepted.


## Mandatory toolchain/version preflight (run before Prompt 0 changes files)

Copy and run this instruction before implementation:

> Treat Java 25 LTS as mandatory. Before creating or editing application code,
> inspect the installed environment and verify official current stable versions
> for Java 25 patch, Spring Boot 4.1.x, Maven stable GA, MySQL 9.7 LTS, React,
> TypeScript, Node.js LTS, Vite and all selected libraries. Use official release
> pages, not blogs or copied snippets alone. Record the date checked, exact
> versions, official URLs and compatibility findings in
> `docs/TECH_STACK_AND_VERSION_POLICY.md`. Do not use preview, milestone, RC,
> snapshot or nightly releases. Configure Maven to compile with release 25.
> Run `java -version`, `javac -version`, `mvn -version`, `node --version`,
> `npm --version`, and query the MySQL server version. If the machine does not
> have the required runtime, report the missing prerequisite and give exact
> installation instructions; do not silently target Java 21. Confirm Spring
> Boot 4.x dependencies and Jakarta package names are compatible. Then propose
> a phase plan and wait for approval before destructive changes or large edits.

Official release references checked on 2026-10-17:
- Java 25: https://www.oracle.com/java/technologies/javase/25all-relnotes.html
- Spring Boot system requirements: https://docs.spring.io/spring-boot/system-requirements.html
- React versions: https://react.dev/versions
- Maven release history: https://maven.apache.org/docs/history.html
- MySQL release model: https://dev.mysql.com/doc/refman/26.7/en/mysql-releases.html


## 22. Alignment with the detailed reference prompt guide

The Claude reference guide uses a more granular, backend-first build sequence and stronger phase gates. Keep this guide's prompts, but follow this expanded order when the work is substantial; do not skip or combine phases just because Antigravity offers to do so:

1. **Phase 0 — Repository/tooling preflight:** inventory the repository and tool versions; approve a plan before code changes.
2. **Phase 1 — Skeleton:** Maven wrapper, React/Vite placeholder, environment examples, Docker Compose for MySQL and MailHog, health checks and Git hygiene.
3. **Phase 2 — Database/JPA:** implement Flyway V1–V6, all entities/enums/repositories, schema constraints and ERD; no business controllers yet.
4. **Phase 3 — Shared backend foundation:** exception mapping with ProblemDetail, request IDs, masking/money utilities, CORS, OpenAPI, configuration and append-only audit service.
5. **Phase 4 — Authentication/RBAC:** registration, login, refresh rotation/reuse detection, logout, lockout, JWT, rate limits, role/ownership tests and dev-only seed identities.
6. **Phase 5 — Accounts/beneficiaries:** account ownership, masked numbers, statements, beneficiary validation and IDOR tests.
7. **Phase 6 — Transactions:** atomic idempotent transfer/deposit/history, row locks, concurrency tests and a temporary `RiskAssessor` interface only until fraud phase.
8. **Phase 7 — Fraud engine/analyst workflow:** seven configurable rules, score bands, alerts/hits, release/confirm-block, analyst-only resolution and boundary tests.
9. **Phase 8 — Notifications/admin/stats:** notification APIs, optional MailHog email, user/account/rule/settings/audit admin APIs, public safe aggregate stats and complete endpoint role matrix.
10. **Phase 9 — Backend hardening/release gate:** coverage, N+1 review, OpenAPI completeness, OWASP self-review and v0.1.0 milestone.
11. **Phase 10 — Frontend shell/auth:** theme, shared layout, API client, login/register and secure token lifecycle.
12. **Phase 11 — Customer features:** accounts, beneficiaries, transfer form, transaction history, notifications and realistic UI states.
13. **Phase 12 — Analyst/admin UI:** fraud queue and detail, decision notes, admin management, audit viewer and statistics.
14. **Phase 13 — Integration/accessibility/end-to-end:** test every role, dark/light theme, responsive layouts, API failures, keyboard navigation and Playwright smoke flow.
15. **Phase 14 — Docker/CI/reproducible setup:** backend/frontend containers as useful, Compose, GitHub Actions, lockfile/wrapper and safe MySQL networking.
16. **Phase 15 — Final docs/review/demo:** verify docs against real code, security review, traceability, threat model, 30+ viva Q&As, demo script and v1.0.0 release gate.

### Additional non-negotiable checks adopted from the reference

- Use package-by-feature, small explainable classes, Java records for DTOs, no Lombok, and constructor injection.
- Use `ProblemDetail`-style errors with field errors and request IDs; do not leak stack traces.
- Use MySQL integration tests, preferably Testcontainers. Prove a negative balance violates the database CHECK constraint.
- Prove a duplicate idempotency key does not debit twice and a concurrency test does not overdraw accounts or change total system money.
- Test all seven fraud rules below, at, and above their boundaries. Confirm customers never receive internal rule details.
- Use a parameterised endpoint-role matrix for anonymous, customer, analyst and admin identities.
- Aim for ≥80% meaningful coverage in service/fraud/security packages at the backend hardening gate; inspect N+1 queries and document the security review.
- Include MailHog only for development; do not expose MySQL publicly in Compose. Kubernetes is documentation/awareness only in v1.
- At each phase, show changed files, exact commands, actual test/build results, PRD deviations, manual verification steps, and the next gate. Stop and wait for approval.

### Resolve conflicts before implementing

The two PRDs began with different levels of specificity and a few alternative design sketches. The reference-alignment addendum in the updated PRD establishes the concrete default for the canonical schema, seven fraud rules, score thresholds, refresh-token design, transaction states and UI acceptance. If Antigravity discovers a genuine contradiction, it must pause and ask rather than invent a hybrid. Keep `docs/DECISIONS.md` updated with the chosen resolution.
