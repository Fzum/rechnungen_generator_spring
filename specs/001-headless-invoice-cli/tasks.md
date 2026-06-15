# Tasks: Headless Invoice CLI

**Input**: Design documents from `/specs/001-headless-invoice-cli/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Included for pure logic only — required by Constitution III. OS side effects
(AppleScript/Finder), HTTP transport, and interactive prompts stay behind their seams and are
exempt; their pure helpers are tested.

**Organization**: Grouped by user story (P1→P3) for independent implementation/testing.

> **Re-based** on the actual flat architecture (`RechnungGeneratorCommandLineRunner` →
> `UnternehmenUserPrompt`/`LeistungszeitraumPrompt`/`StundenleistungPrompt` → `PdfCreator` →
> `EmailInitiator` → `PathOpener`; domain `model.Unternehmen`). All paths below are real.

## Path Conventions

`BASE = src/main/kotlin/at/sellemond/software/rechnung_generator_spring`,
`TEST = src/test/kotlin/at/sellemond/software/rechnung_generator_spring`.

---

## Phase 1: Setup

- [X] T001 Run `./mvnw test` and confirm the existing suite is green before any change (baseline) — green after `clean` (22 tests); stale `target/` from the lost hexagonal refactor removed

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared scaffolding, two de-dup extractions, and the totals-parity helper every story
needs. Delivers no user story alone.

**⚠️ CRITICAL**: No user-story work begins until this phase is complete.

- [ ] T002 [P] Add `HoursSource` enum (`MANUAL`, `HARVEST`) in `BASE/cli/HoursSource.kt`
- [ ] T003 [P] Add `OutputFormat` enum (`TEXT`, `JSON`) in `BASE/cli/OutputFormat.kt`
- [ ] T004 [P] Write failing `UstBetragCalculatorTest` (exact `BigDecimal` USt for sample net/ust pairs) in `TEST/UstBetragCalculatorTest.kt`
- [ ] T005 Add `calculateUstBetrag(net: BigDecimal, ust: Int): BigDecimal` and `replaceUmlauts(s: String)` to `BASE/util/UtilFunctions.kt`; refactor `PdfCreator.getUstBetrag` to call `calculateUstBetrag` (parity) — satisfies T004 in `BASE/PdfCreator.kt`
- [ ] T006 Add `HoursProvider` interface `fetch(year: Int, month: Int, customer: Properties): BigDecimal` in `BASE/hours/HoursProvider.kt`
- [ ] T007 [P] Implement `ManualHoursProvider` (returns the supplied value) in `BASE/hours/ManualHoursProvider.kt`
- [ ] T008 Extract `UnternehmenPropertiesLoader` (`listOptions(folder)`, `load(folder, name)` — external via `ExternalConfigLoader`, else bundled) in `BASE/prompt/UnternehmenPropertiesLoader.kt`; refactor `UnternehmenUserPrompt` to use it (no behaviour change) in `BASE/prompt/UnternehmenUserPrompt.kt`
- [ ] T009 Extract `LeistungszeitraumFactory.create(year, month): LeistungszeitraumPromptResult` (the `zeitraum` formatting) in `BASE/prompt/LeistungszeitraumFactory.kt`; refactor `LeistungszeitraumPrompt` to use it in `BASE/prompt/LeistungszeitraumPrompt.kt`
- [ ] T010 Add `GenerationResult` + `GenerationError` DTOs (fields per data-model.md) in `BASE/cli/GenerationResult.kt`
- [ ] T011 Add `src/main/resources/logback-spring.xml` routing the console appender to `System.err`; set `spring.main.banner-mode=off` in `src/main/resources/application.yaml`
- [ ] T012 Add interactive-vs-non-interactive dispatch in `RechnungGeneratorCommandLineRunner.run()` — empty args → existing interactive flow **unchanged**; args present → delegate to injected `NonInteractiveInvoiceRunner` (stub for now) in `BASE/RechnungGeneratorSpringApplication.kt`

**Checkpoint**: compiles, `./mvnw test` green, bare `java -jar …` still runs the interactive prompts unchanged.

---

## Phase 3: User Story 1 - Non-interactive generation + machine-readable result (Priority: P1) 🎯 MVP

**Goal**: Run with all values as arguments, no prompts, emit one JSON result.

**Independent Test**: `generate --issuer=… --customer=… --period=2026-05 --hours=142.5 --mail=false --open=false --format=json` exits `0`, prints one schema-valid JSON object, no prompts; bare invocation still interactive.

### Tests for User Story 1 (write first, ensure they FAIL)

- [ ] T013 [P] [US1] `InvoiceCliArgsParserTest` — issuer/customer existence (via loader), `YYYY-MM` parse + future rejection, manual hours required, format default, error/exit-code mapping in `TEST/cli/InvoiceCliArgsParserTest.kt`
- [ ] T014 [P] [US1] `GenerationResultJsonWriterTest` — required fields, `BigDecimal` scale, single-object output, error-envelope shape (validate vs `contracts/generation-result.schema.json`) in `TEST/cli/GenerationResultJsonWriterTest.kt`

### Implementation for User Story 1

- [ ] T015 [P] [US1] `InvoiceCliArgs` validated value object in `BASE/cli/InvoiceCliArgs.kt`
- [ ] T016 [US1] `InvoiceCliArgsParser` (`ApplicationArguments` → `InvoiceCliArgs`; flags `--issuer/--customer/--period/--hours/--hours-source/--mail/--open/--format`; up-front validation) — satisfies T013 in `BASE/cli/InvoiceCliArgsParser.kt`
- [ ] T017 [US1] `GenerationResultJsonWriter` (Jackson, sole JSON object / error envelope to stdout) — satisfies T014 in `BASE/cli/GenerationResultJsonWriter.kt`
- [ ] T018 [US1] `NonInteractiveInvoiceRunner`: load issuer/customer `Properties` (loader) → build period (factory) → resolve hours (`ManualHoursProvider`) → build `UnternehmenPromptResult` → `PdfCreator.createRechnung(...)` → compute net/vat/gross → gate `PathOpener` by `--open` → write text/JSON in `BASE/cli/NonInteractiveInvoiceRunner.kt`
- [ ] T019 [US1] Map outcomes to exit codes (`0/1/2`) and emit `GenerationError` via `exitProcess` on validation/unexpected failure in `BASE/cli/NonInteractiveInvoiceRunner.kt`

**Checkpoint**: US1 quickstart passes (headless manual-hours JSON; interactive unchanged). **Shippable MVP.**

---

## Phase 4: User Story 2 - Apple Mail draft with both attachments (Priority: P2)

**Goal**: Open an Apple Mail draft, templated, with exactly two attachments (invoice PDF + supplied
Harvest report PDF); never sent.

**Independent Test**: with `--report-pdf=<existing>.pdf` (mail on) → draft opens with both attachments, unsent; `attachments` JSON lists both; non-existent report path → exit `2`, `MISSING_REPORT_PDF`, no draft.

### Tests for User Story 2 (write first, ensure they FAIL)

- [ ] T020 [P] [US2] `AppleMailScriptBuilderTest` — `"`/`\` escaping, two `POSIX file` attachments, recipient set, period-templated + umlaut-fixed subject/body, **no `send`** in `TEST/termination/email/AppleMailScriptBuilderTest.kt`

### Implementation for User Story 2

- [ ] T021 [US2] `AppleMailScriptBuilder` (pure osascript text from recipient/subject/body/attachments) — satisfies T020 in `BASE/termination/email/AppleMailScriptBuilder.kt`
- [ ] T022 [US2] Change `EmailInitiator.initiateEmail(kunde, zeitraum, attachments: List<Path>)` signature in `BASE/termination/email/EmailInitiator.kt`
- [ ] T023 [US2] Rewrite `MacEmailInitiator` to write the built script to a temp `.scpt` and run `osascript` (visible draft, never send); update `WindowsEmailInitiator` to the new signature in `BASE/termination/email/MacEmailInitiator.kt` and `BASE/termination/email/WindowsEmailInitiator.kt`
- [ ] T024 [US2] In `NonInteractiveInvoiceRunner`, when `--mail`, call `initiateEmail` with `[invoicePdf, reportPdf]` and populate `GenerationResult.attachments` in `BASE/cli/NonInteractiveInvoiceRunner.kt`
- [ ] T025 [US2] Extend `InvoiceCliArgsParser` with `--report-pdf` (required + readable when `--mail=true`, else `MISSING_REPORT_PDF`/exit `2`) and add a parser test case in `BASE/cli/InvoiceCliArgsParser.kt` + `TEST/cli/InvoiceCliArgsParserTest.kt`

**Checkpoint**: US2 quickstart passes — draft with exactly two attachments, unsent; missing report fails clearly.

---

## Phase 5: User Story 3 - Harvest hours source (Priority: P3)

**Goal**: Fetch billable hours from Harvest when `--hours-source=harvest`; `--hours` overrides.

**Independent Test**: `--hours-source=harvest` (token set) → `hours` equals Harvest billable total; `--hours=99` overrides; bad creds → exit `3`, `HARVEST_FETCH_FAILED`, no invoice.

### Tests for User Story 3 (write first, ensure they FAIL)

- [ ] T026 [P] [US3] `HarvestHoursProviderTest` — response row → exact `BigDecimal` billable hours; no row for the configured project id → error (stubbed client) in `TEST/hours/HarvestHoursProviderTest.kt`

### Implementation for User Story 3

- [ ] T027 [P] [US3] `HarvestProperties` `@ConfigurationProperties(prefix="harvest")` (`account-id`/`access-token`/`base-url`, env-backed) in `BASE/harvest/HarvestProperties.kt`
- [ ] T028 [US3] `HarvestTimeReportClient` (`java.net.http` GET `/reports/time/projects?from&to` with auth headers) + row model in `BASE/harvest/HarvestTimeReportClient.kt`
- [ ] T029 [US3] `HarvestHoursProvider` — derive `from`/`to` from period, find row by `project_id == harvest-project-id` (read from customer `Properties`), read `billable_hours` as `BigDecimal`; satisfies T026 in `BASE/hours/HarvestHoursProvider.kt`
- [ ] T030 [US3] `HoursProviders` selecting `HoursProvider` by `HoursSource`; wire into `NonInteractiveInvoiceRunner`; explicit `--hours` overrides Harvest in `BASE/hours/HoursProviders.kt`
- [ ] T031 [US3] Add `harvest-project-id` example to `src/main/resources/kunden/test-kunde.properties` (read directly from customer `Properties`, no `model.Unternehmen` change)
- [ ] T032 [US3] Extend `InvoiceCliArgsParser`/runner: `--hours-source`, require mapped project for harvest (`UNMAPPED_HARVEST_PROJECT`/exit `2`), Harvest failures → exit `3`; add parser test in `BASE/cli/InvoiceCliArgsParser.kt` + `TEST/cli/InvoiceCliArgsParserTest.kt`

**Checkpoint**: US3 quickstart passes — Harvest hours match, override works, failure exits `3`.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T033 [P] Update `README.md` with non-interactive flags, exit codes, env vars (`HARVEST_ACCOUNT_ID`/`HARVEST_ACCESS_TOKEN`), and example invocations
- [ ] T034 [P] Update the `create-invoice` skill to drive the jar via flags and parse the JSON result (drop the `expect`/two-pass discovery) in `~/.claude/skills/create-invoice/SKILL.md` (external to repo)
- [ ] T035 Run full `quickstart.md` validation on macOS (smoke-test PDF, Apple Mail draft, Finder open per Constitution Dev Workflow)
- [ ] T036 Final gate: `./mvnw package` and `./mvnw test` both green

---

## Dependencies & Execution Order

- **Setup (T001)** → **Foundational (T002–T012, blocks all)** → **US1 (T013–T019)** → US2 / US3 → Polish.
- **US1** is the MVP and establishes the non-interactive entry that US2/US3 extend.
- **US2** (T020–T025) and **US3** (T026–T032) are independent of each other.
- Within a story: pure-logic tests first and failing (Constitution III); signature changes before adapter rewrites; parser flag before its validation test.

### Parallel Opportunities

- Foundational: T002, T003, T004, T007 `[P]`.
- US1: T013, T014, T015 `[P]`.
- US3: T026, T027 `[P]`.
- Polish: T033, T034 `[P]`.
- US2 and US3 can proceed in parallel once US1 lands.

---

## Implementation Strategy

**MVP first**: T001 → T002–T012 → T013–T019, then STOP and validate the US1 quickstart (headless
JSON result; interactive unchanged) — this alone replaces the skill's `expect`/two-pass driving.
Then add US2 (mail attachments), then US3 (Harvest hours). Each story is independently testable.

## Notes

- `[P]` = different files, no incomplete dependency. `[US#]` maps to spec.md stories.
- Net/USt/gross stay `BigDecimal`; `calculateNettobetrag` reused; `calculateUstBetrag` shared with
  `PdfCreator` for parity (Constitution II).
- Verify pure-logic tests fail before implementing (Constitution III).
- Smoke-test PDF/Mail/Finder on macOS — the only fully implemented platform.
- Commit after each task or logical group.
