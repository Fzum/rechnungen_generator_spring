# Implementation Plan: Headless Invoice CLI

**Branch**: `001-headless-invoice-cli` | **Date**: 2026-06-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-headless-invoice-cli/spec.md`

> **Re-baselined 2026-06-15**: An earlier draft of this plan targeted a hexagonal
> ports/adapters architecture that turned out not to exist in the working tree. This version is
> written against the **actual** committed code: a flat Spring-Boot `CommandLineRunner` that drives
> `UnternehmenUserPrompt` → `LeistungszeitraumPrompt` → `StundenleistungPrompt` → `PdfCreator` →
> `EmailInitiator` → `PathOpener`, with the domain modelled as `model.Unternehmen`.

## Summary

Add a non-interactive mode to the existing interactive invoice CLI so the Claude `create-invoice`
skill can run it with no prompts and read a single machine-readable JSON result (invoice id,
totals, file paths). Introduce a small `HoursProvider` abstraction (manual value or Harvest API
fetch). Replace the attachment-less `mailto:` draft with an Apple Mail draft (AppleScript)
pre-attached with the generated invoice PDF plus the client's official Harvest monthly report PDF,
opened for manual review and never auto-sent. Local-only on macOS. No dependency upgrades, no web
service.

## Technical Context

**Language/Version**: Kotlin 1.7.21 on the JVM, Java 17 (unchanged — pinned by constitution).

**Primary Dependencies**: Spring Boot 3.0.0 (DI/config container; the app stays a
`CommandLineRunner`), iText 7.2.4 (PDF). Reused, not added: Jackson (already on the Spring Boot
classpath) for JSON output and Harvest response parsing; `java.net.http.HttpClient` (JDK built-in)
for the Harvest call. **No new third-party dependency.**

**Storage**: Filesystem only — `kunden/*.properties`, `unternehmen/*.properties`, `application.yaml`
beside the JAR (via `ExternalConfigLoader` + bundled fallback); generated PDFs under
`rechnungen/<kunde-shortName>/`. No database.

**Testing**: JUnit 5 + AssertJ (existing), incl. ParameterizedTest. Source roots `src/main/kotlin`,
`src/test/kotlin` (non-standard, set in `pom.xml`).

**Target Platform**: macOS desktop (JVM). Only macOS side effects are implemented; `WINDOWS`/`OTHER`
throw or are `TODO()` stubs, selected by `getOperatingSystem()` factories.

**Project Type**: Single-project interactive command-line desktop tool (now also non-interactive).

**Performance Goals**: Not performance-sensitive — one invoice per run, at most one Harvest HTTP
request. Correctness is the binding constraint.

**Constraints**: In `--format=json`, the result MUST be the only thing on stdout (Spring banner off,
logs routed to stderr). Money stays `BigDecimal` for net/USt/gross. Email is never auto-sent. The
Harvest report PDF is supplied as a path and attached verbatim (never self-generated).

**Scale/Scope**: Single user, a few customers/issuers, one invoice per run.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Assessment | Status |
|-----------|------------|--------|
| I. Configuration Over Code | New per-customer `harvest-project-id` lives in `kunden/*.properties` (external via `ExternalConfigLoader`, bundled fallback); Harvest credentials come from env via `HarvestProperties`. Nothing customer/issuer/credential hardcoded. | PASS |
| II. Financial Correctness (NON-NEGOTIABLE) | Net/USt/gross stay `BigDecimal`. The inline USt calc in `PdfCreator` is **extracted** to a shared `calculateUstBetrag(net, ust)` used by both the PDF and the JSON result, guaranteeing identical numbers; backed by a new exact-value test. `calculateNettobetrag` reused unchanged. Harvest hours parse to `BigDecimal`, never `Double`. | PASS |
| III. Test-First for Core Logic | New pure logic — CLI arg parsing/validation, USt extraction, Harvest-response→hours, AppleScript building, JSON serialization — written test-first (JUnit 5 + AssertJ). Interactive prompts, AppleScript exec, Finder open, and HTTP I/O stay side-effectful and exempt; their pure helpers are tested. | PASS |
| IV. Platform Behavior Behind Abstractions | The Apple Mail draft stays behind `EmailInitiator`, selected by `EmailInitiatorConfiguration` on `getOperatingSystem()`; `MacEmailInitiator` is rewritten, `WindowsEmailInitiator` stub updated to the new signature (still `TODO`). Finder open stays behind `PathOpener`. Harvest HTTP is isolated in a `HarvestHoursProvider`/client. Unsupported OS still throws. | PASS |
| V. Simplicity & Single Responsibility | Matches the existing flat style; adds a small `cli/` surface, a 3-class `hours/` abstraction, a Harvest client, and one AppleScript builder. Two small extractions (a shared properties-loader and the period formatter) are justified by concrete reuse between interactive and non-interactive paths (removes duplication). Arg parsing uses Spring's built-in `ApplicationArguments` — no picocli, no new dependency. | PASS |

**Technology Constraints check**: Stays Kotlin/Java 17/Maven/iText 7; Spring Boot remains DI/config
only; the app remains a `CommandLineRunner` and does **not** become a web service — non-interactive
arguments are still a CLI surface. `abrechnung`/`email`/new `harvest` settings bind via
`@ConfigurationProperties`. **No constitution amendment required; no violations.**

## Project Structure

### Documentation (this feature)

```text
specs/001-headless-invoice-cli/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   ├── cli-contract.md
│   └── generation-result.schema.json
├── checklists/requirements.md
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

Existing flat layout under `BASE = src/main/kotlin/at/sellemond/software/rechnung_generator_spring`,
`TEST = src/test/kotlin/...`. New (▲) and changed (△):

```text
BASE/
├── RechnungGeneratorSpringApplication.kt   △ runner: empty args → interactive (unchanged); args → non-interactive
├── cli/                                     ▲ non-interactive surface
│   ├── HoursSource.kt                       ▲ MANUAL | HARVEST
│   ├── OutputFormat.kt                      ▲ TEXT | JSON
│   ├── InvoiceCliArgs.kt                    ▲ parsed+validated options
│   ├── InvoiceCliArgsParser.kt             ▲ ApplicationArguments -> InvoiceCliArgs (+validation, typed errors)
│   ├── GenerationResult.kt                  ▲ GenerationResult + GenerationError DTOs
│   ├── GenerationResultJsonWriter.kt        ▲ Jackson -> stdout (single object)
│   └── NonInteractiveInvoiceRunner.kt       ▲ orchestrates load → hours → PDF → draft → result → exit code
├── hours/                                   ▲ hours abstraction
│   ├── HoursProvider.kt                     ▲ interface
│   ├── ManualHoursProvider.kt               ▲ returns supplied value
│   ├── HarvestHoursProvider.kt              ▲ Harvest billable hours -> BigDecimal
│   └── HoursProviders.kt                    ▲ select provider by HoursSource
├── harvest/
│   ├── HarvestProperties.kt                 ▲ @ConfigurationProperties(harvest): account-id/access-token/base-url (env)
│   └── HarvestTimeReportClient.kt           ▲ java.net.http GET /reports/time/projects + row model
├── prompt/
│   ├── UnternehmenPropertiesLoader.kt       ▲ load kunde/issuer Properties by short-name (extracted, shared)
│   ├── UnternehmenUserPrompt.kt             △ use the shared loader (de-dupe)
│   ├── LeistungszeitraumFactory.kt          ▲ build LeistungszeitraumPromptResult from year/month (extracted formatter)
│   └── LeistungszeitraumPrompt.kt           △ use the shared factory
├── termination/email/
│   ├── EmailInitiator.kt                    △ initiateEmail(kunde, zeitraum, attachments: List<Path>)
│   ├── MacEmailInitiator.kt                 △ AppleScript draft + attachments (never send)
│   ├── WindowsEmailInitiator.kt             △ stub updated to new signature
│   └── AppleMailScriptBuilder.kt            ▲ pure osascript text builder (unit-tested)
├── PdfCreator.kt                            △ use shared calculateUstBetrag (parity with JSON totals)
└── util/UtilFunctions.kt                    △ add calculateUstBetrag(net, ust) + replaceUmlauts(s)

src/main/resources/
├── application.yaml                         △ banner off; harvest defaults
├── logback-spring.xml                       ▲ route console logging to STDERR (keep stdout clean for JSON)
└── kunden/test-kunde.properties             △ add harvest-project-id example
README.md                                    △ document non-interactive flags + env vars

TEST/
├── cli/InvoiceCliArgsParserTest.kt          ▲
├── cli/GenerationResultJsonWriterTest.kt    ▲
├── hours/HarvestHoursProviderTest.kt        ▲
├── termination/email/AppleMailScriptBuilderTest.kt ▲
└── UstBetragCalculatorTest.kt               ▲ (exact-value USt extraction)
```

**Structure Decision**: Keep the flat package style. The non-interactive path is a sibling
orchestrator (`NonInteractiveInvoiceRunner`) that reuses the **same** `PdfCreator`,
`EmailInitiator`, and `PathOpener` the interactive runner uses — so the PDF, totals, and side
effects stay identical. New behaviour is additive; the interactive flow is only refactored to share
the extracted loader/period factory (no behavioural change).

## Complexity Tracking

> No constitution violations. The two extractions (`UnternehmenPropertiesLoader`,
> `LeistungszeitraumFactory`) and the `calculateUstBetrag` extraction are de-duplication driven by a
> concrete reuse need between interactive and non-interactive paths, not speculative generality.

## Phase Status

- [x] Phase 0 — research.md (decisions re-based on the real architecture; 0 NEEDS CLARIFICATION)
- [x] Phase 1 — data-model.md, contracts/, quickstart.md, agent context updated
- [x] Constitution re-check after design: still PASS
- [ ] Phase 2 — tasks.md (regenerated by this re-plan)
