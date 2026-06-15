<!--
SYNC IMPACT REPORT
Version change: (template) → 1.0.0
Bump rationale: Initial ratification of a concrete constitution from the template.
Modified principles: n/a (initial definition)
  - [PRINCIPLE_1] → I. Configuration Over Code
  - [PRINCIPLE_2] → II. Financial Correctness (NON-NEGOTIABLE)
  - [PRINCIPLE_3] → III. Test-First for Core Logic
  - [PRINCIPLE_4] → IV. Platform Behavior Behind Abstractions
  - [PRINCIPLE_5] → V. Simplicity & Single Responsibility
Added sections:
  - Technology Constraints
  - Development Workflow & Quality Gates
Removed sections: none
Templates requiring updates:
  - .specify/templates/plan-template.md ✅ aligned (Constitution Check gate is constitution-driven, no edit needed)
  - .specify/templates/spec-template.md ✅ aligned (no principle-specific references)
  - .specify/templates/tasks-template.md ✅ aligned (tests already optional/principle-driven)
Follow-up TODOs: none
-->

# Rechnung Generator Constitution

## Core Principles

### I. Configuration Over Code

Every customer, issuer, billing rate, tax setting, and message template MUST be
configurable through external files (`kunden/*.properties`, `unternehmen/*.properties`,
`application.yaml`) placed beside the JAR, with the bundled resources serving only as
fallback defaults. No customer-, issuer-, or deployment-specific data may be hardcoded
in Kotlin sources.

Rationale: the same unchanged binary must be handed to different users and reused for
different clients without recompilation.

### II. Financial Correctness (NON-NEGOTIABLE)

All monetary and tax math (net amount, VAT, gross total) MUST use `BigDecimal` — never
`Double`/`Float` — and MUST be backed by unit tests that assert exact expected values.
Any change to a calculation ships with the test that proves it. Rounding and currency
formatting go through the shared `util/` helpers, not ad-hoc inline code.

Rationale: invoices are legal and financial documents; a rounding error is a defect that
reaches a customer's books.

### III. Test-First for Core Logic

Pure logic — calculators, parsers, UID/date generators, config loading — MUST have
JUnit 5 + AssertJ unit tests, and new or changed core logic is written test-first
(failing test before implementation). Interactive stdin prompts and OS side effects are
exempt where automated testing is impractical, but their pure helpers are not.

Rationale: correctness lives in the testable core; keeping it covered is what makes the
untested edges safe to leave untested.

### IV. Platform Behavior Behind Abstractions

OS-dependent side effects (opening files/folders, launching the mail client) MUST sit
behind an interface and be selected at runtime by a configuration factory keyed on
`getOperatingSystem()`. Unsupported platforms MUST fail explicitly (throw), never
silently no-op. Adding a platform is additive: a new implementation plus a factory branch.

Rationale: keeps the core portable and prevents platform code from leaking into business
logic.

### V. Simplicity & Single Responsibility

Prefer the simplest design that works: small single-purpose classes, early returns,
composition over inheritance, and reuse of existing `util/` helpers over new abstractions.
New layers, patterns, or indirection MUST be justified against a concrete current need
(YAGNI); speculative generality is rejected in review.

Rationale: a small personal tool stays maintainable only while it stays small and direct.

## Technology Constraints

- Language/runtime: Kotlin on the JVM, Java 17. Build tool: Maven (`./mvnw`).
- Spring Boot is used as the DI/configuration container only; the application MUST remain
  an interactive CLI (`CommandLineRunner`) and MUST NOT grow into a web service.
- PDF generation uses iText 7.
- Sources live under `src/main/kotlin` and `src/test/kotlin`; money is `BigDecimal`, dates
  use `java.time`.
- Externalized config binds via `@ConfigurationProperties` (`abrechnung`, `email`); do not
  bypass it with manual file parsing for those settings.

## Development Workflow & Quality Gates

- `./mvnw package` MUST succeed and `./mvnw test` MUST pass before any change is merged.
- Changes to billing, tax, parsing, or formatting logic MUST update or add the
  corresponding tests in the same change.
- Follow existing code conventions, including the default-package/top-level class import
  style already present in the codebase.
- Changes touching PDF generation, email, or file-opening MUST be smoke-tested on macOS,
  which is the only fully implemented platform.

## Governance

This constitution supersedes ad-hoc practices. Amendments MUST be made by editing this
file, bumping the version per the policy below, and updating the Last Amended date; the
Sync Impact Report at the top MUST be refreshed. Pull requests and reviews MUST verify
compliance, and any deviation MUST be recorded under Complexity Tracking in the affected
plan with its justification.

Versioning policy (semantic):
- MAJOR: backward-incompatible removal or redefinition of a principle or governance rule.
- MINOR: a new principle/section or materially expanded guidance.
- PATCH: clarifications, wording, or non-semantic refinements.

Runtime development guidance lives in `CLAUDE.md` and `README.md`; this constitution governs
the non-negotiable rules above.

**Version**: 1.0.0 | **Ratified**: 2026-06-15 | **Last Amended**: 2026-06-15
