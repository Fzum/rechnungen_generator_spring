# Implementation Plan: Headless Invoice CLI

**Branch**: `main` (Spec Kit git extension not installed; no feature branch) | **Date**: 2026-06-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-headless-invoice-cli/spec.md`

## Summary

Add a non-interactive mode to the existing interactive Kotlin/Spring-Boot invoice CLI so the
Claude `create-invoice` skill can run it end-to-end with no prompts and read a single
machine-readable JSON result (invoice number, totals, file paths). Introduce an interchangeable
`HoursProvider` outbound port (manual value or Harvest API fetch). Replace the attachment-less
`mailto:` draft with an Apple Mail draft (via AppleScript) pre-attached with the generated
invoice PDF plus the client's official Harvest monthly report PDF, opened for manual review and
never auto-sent. Everything stays local on macOS. No dependency upgrades, no web service, no
new runtime — consistent with the constitution.

## Technical Context

**Language/Version**: Kotlin 1.7.21 on the JVM, Java 17 (unchanged — pinned by constitution).

**Primary Dependencies**: Spring Boot 3.0.0 (DI/config container only), iText 7.2.4 (PDF).
Reused, not added: Jackson (already on the Spring Boot classpath) for JSON output and Harvest
response parsing; `java.net.http.HttpClient` (JDK built-in) for the Harvest call. **No new
third-party dependency is introduced.**

**Storage**: Filesystem only — `kunden/*.properties`, `unternehmen/*.properties`,
`application.yaml` beside the JAR; generated PDFs under `rechnungen/<kunde>/`. No database.

**Testing**: JUnit 5 + AssertJ (existing). Source roots `src/main/kotlin`, `src/test/kotlin`.

**Target Platform**: macOS desktop (JVM). Only macOS side effects are implemented; other
platforms throw, per existing `getOperatingSystem()` factories.

**Project Type**: Single-project interactive command-line desktop tool (now also non-interactive).

**Performance Goals**: Not performance-sensitive — one invoice per invocation, at most one
Harvest HTTP request. The binding constraint is correctness, not throughput.

**Constraints**: In JSON mode, the machine-readable result MUST be the only content on stdout
(logs/banner/prompts go elsewhere or are suppressed). Money stays `BigDecimal`. The email is
never sent automatically. The Harvest report PDF is supplied as a path and attached verbatim.

**Scale/Scope**: Single user, a handful of customers/issuers, one invoice per run.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Assessment | Status |
|-----------|------------|--------|
| I. Configuration Over Code | New per-customer `harvest-project-id` lives in `kunden/*.properties` (external, with bundled fallback); Harvest credentials come from environment variables. No customer/issuer/credential data hardcoded. | PASS |
| II. Financial Correctness (NON-NEGOTIABLE) | No change to net/VAT/gross math; hours stay `BigDecimal`; `HarvestHoursProvider` parses hours into `BigDecimal`, never `Double`. Existing calculator tests remain authoritative; provider hour-parsing gets its own exact-value tests. | PASS |
| III. Test-First for Core Logic | New pure logic — CLI argument parsing/validation, Harvest-response hours extraction, result serialization — is written test-first with JUnit 5 + AssertJ. AppleScript/Finder/HTTP side effects sit behind ports and are exempt; their pure helpers (e.g. AppleScript string building, request building) are tested. | PASS |
| IV. Platform Behavior Behind Abstractions | The Apple Mail draft remains behind `MailDraftPort`, selected by the existing OS factory; `MacEmailInitiator` is replaced by an AppleScript implementation. Unsupported platforms still throw. Finder open stays behind `FileOpenPort`. Harvest HTTP sits behind the new `HoursProvider` port. | PASS |
| V. Simplicity & Single Responsibility | Reuses the existing hexagonal ports/adapters and domain model; adds one outbound port (`HoursProvider`) with two small implementations, one CLI-args value object, and one result DTO. No new framework, no web layer, no new dependency. Argument parsing uses Spring Boot's built-in `ApplicationArguments` rather than adding picocli (YAGNI). | PASS |

**Technology Constraints check**: Stays Kotlin/Java 17/Maven/iText 7; Spring Boot remains
DI/config only; the app remains a `CommandLineRunner` CLI and does **not** become a web service —
non-interactive arguments are still a CLI surface. Externalized `abrechnung`/`email` config keeps
binding via `@ConfigurationProperties`. **No constitution amendment required; no violations.**

## Project Structure

### Documentation (this feature)

```text
specs/001-headless-invoice-cli/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── cli-contract.md
│   └── generation-result.schema.json
├── checklists/
│   └── requirements.md  # from /speckit.specify
└── tasks.md             # Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

Reuses the existing hexagonal layout under `src/main/kotlin/at/sellemond/software/rechnung_generator_spring`.
New (▲) and changed (△) files:

```text
src/main/kotlin/.../rechnung_generator_spring/
├── RechnungGeneratorSpringApplication.kt        △ CommandLineRunner branches interactive vs non-interactive
├── cli/                                          ▲ non-interactive entry surface
│   ├── InvoiceCliArgs.kt                         ▲ parsed + validated CLI options (value object)
│   ├── InvoiceCliArgsParser.kt                   ▲ ApplicationArguments -> InvoiceCliArgs (+ validation)
│   └── GenerationResultJsonWriter.kt             ▲ serialize result -> stdout JSON (Jackson)
├── application/
│   ├── GenerateInvoiceCommand.kt                 △ carries HoursSource + reportPdfPath + side-effect flags
│   ├── GenerateInvoiceUseCase.kt                 △ resolve hours via HoursProvider; gate mail/open; richer result
│   └── port/out/
│       ├── HoursProvider.kt                      ▲ new outbound port: fetch(period, customer) -> BigDecimal
│       └── MailDraftPort.kt                      △ openDraft(invoice, attachments: List<Path>)
├── adapters/out/
│   ├── hours/
│   │   ├── ManualHoursProvider.kt                ▲ returns the supplied value
│   │   └── HarvestHoursProvider.kt               ▲ HttpClient call to Harvest; parse billable hours
│   └── harvest/
│       ├── HarvestProperties.kt                  ▲ @ConfigurationProperties(harvest): account id/token from env
│       └── HarvestTimeReportClient.kt            ▲ thin HTTP client + response model
├── termination/email/
│   ├── MacEmailInitiator.kt                      △ replace mailto: with AppleScript (osascript) + attachments
│   └── AppleMailScriptBuilder.kt                 ▲ pure builder for the osascript payload (unit-tested)
└── model/ AbrechnungProperties.kt                (unchanged)

src/main/resources/kunden/test-kunde.properties   △ add harvest-project-id example
README.md                                          △ document non-interactive flags + env vars

src/test/kotlin/.../
├── cli/InvoiceCliArgsParserTest.kt               ▲ parsing + validation + precedence
├── cli/GenerationResultJsonWriterTest.kt         ▲ JSON shape/values
├── adapters/out/hours/HarvestHoursProviderTest.kt▲ response -> BigDecimal hours (exact)
└── termination/email/AppleMailScriptBuilderTest.kt ▲ script/escaping for paths + templates
```

**Structure Decision**: Single-project hexagonal layout (already in use). The feature slots a
new outbound port (`HoursProvider`) and a Harvest adapter alongside the existing
`InvoiceDocumentPort`/`MailDraftPort`/`FileOpenPort`, adds a thin `cli/` surface for the
non-interactive entry, and swaps the email adapter's transport. The domain core, billing math,
PDF generation, and config-loading are untouched.

## Complexity Tracking

> No constitution violations. Nothing to justify. The one judgment call — not adding picocli —
> is recorded as a Simplicity decision in research.md and keeps the dependency set unchanged.

## Phase Status

- [x] Phase 0 — research.md (all technical unknowns resolved; 0 NEEDS CLARIFICATION)
- [x] Phase 1 — data-model.md, contracts/, quickstart.md, agent context updated
- [x] Constitution re-check after design: still PASS (no new dependency, ports preserved, CLI-only)
- [ ] Phase 2 — tasks.md (created by `/speckit.tasks`)
