# Phase 0 Research: Headless Invoice CLI

Re-based on the actual flat architecture. **0 NEEDS CLARIFICATION remain.**

## R1. Interactive vs non-interactive dispatch

- **Decision**: In `RechnungGeneratorCommandLineRunner.run(vararg args)`, if `args` is empty → run
  today's interactive flow unchanged. If any argument is present → delegate to
  `NonInteractiveInvoiceRunner`.
- **Rationale**: One entry point; bare `java -jar …` is byte-for-byte the current behaviour
  (FR-002). Spring Boot already forwards process args to `CommandLineRunner`.
- **Alternatives**: second runner/`@Profile` (rejected: risk both run); env-var switch (rejected:
  less explicit for an agent).

## R2. Argument parsing

- **Decision**: Parse with Spring Boot's `ApplicationArguments` (`--option=value`) in a unit-tested
  `InvoiceCliArgsParser` → validated `InvoiceCliArgs`. No new dependency.
- **Rationale**: ~7 simple flags; Constitution V (Simplicity/YAGNI); keeps `pom.xml` frozen.
- **Alternatives**: picocli (new dependency, rejected); manual `args` scan (rejected: reinvents
  `ApplicationArguments`).

## R3. Clean machine-readable stdout

- **Decision**: For `--format=json`, stdout carries exactly one JSON object. Achieve via: Spring
  banner off (`spring.main.banner-mode=off`), a `logback-spring.xml` console appender targeting
  `System.err`, and the non-interactive path writing only the final JSON to `System.out`. Any
  human/progress text from the non-interactive path goes to stderr.
- **Rationale**: FR-004 — the agent parses stdout without stripping logs. The existing interactive
  `println` prompts are never reached in non-interactive mode.
- **Alternatives**: `logging.level.root=OFF` (rejected: loses diagnostics); printing JSON to a file
  (rejected: extra round-trip; path is already in the JSON).

## R4. JSON serialization

- **Decision (corrected during implementation)**: Jackson is **not** on the classpath
  (`spring-boot-starter`, not `-web`). Hand-roll a tiny `GenerationResultJsonWriter` for the small,
  flat `GenerationResult`/`GenerationError` with explicit string escaping and `BigDecimal.toPlainString()`
  (scale preserved). No dependency added.
- **Rationale**: Avoids a new dependency for one flat object (Constitution V); full control of number
  formatting and escaping; unit-tested.
- **Alternatives**: add `jackson-module-kotlin` (rejected: new dependency for trivial gain).

## R5. Invoice totals parity (PdfCreator vs JSON result)

- **Decision**: Extract the USt computation currently inlined in `PdfCreator.getUstBetrag`
  (`net * (ust/100)`) into a shared pure `calculateUstBetrag(net: BigDecimal, ust: Int): BigDecimal`
  in `util`. `PdfCreator` and `NonInteractiveInvoiceRunner` both call it, plus the reused
  `calculateNettobetrag`. Gross = net + USt.
- **Rationale**: Constitution II — the JSON `net/vat/gross` must equal what the PDF shows; one shared
  function guarantees it. Small, justified de-duplication (Constitution V).
- **Alternatives**: recompute independently in the runner (rejected: drift risk); have
  `createRechnung` return a totals object (rejected: larger change to a tested method; can follow
  later).
- **Note**: `AbrechnungProperties.stundensatz` is `Double` and `ust` is `Int` in the existing
  config; the money values derived from them stay `BigDecimal` exactly as today (no change to that
  pattern).

## R6. Loading customer/issuer by short-name (non-interactive)

- **Decision**: Extract the "list options / load `Properties` by folder+name (external via
  `ExternalConfigLoader`, else bundled resource)" logic currently private in `UnternehmenUserPrompt`
  into a shared `UnternehmenPropertiesLoader` (`listOptions(folder)`, `load(folder, name)`).
  `UnternehmenUserPrompt` is refactored to use it; the non-interactive parser uses it to validate
  that `--issuer`/`--customer` exist and to load their `Properties`.
- **Rationale**: Reuse without duplication (Constitution V); keeps the external-override semantics
  identical for both paths (Constitution I).
- **Alternatives**: duplicate the loader in the CLI layer (rejected: divergence risk).

## R7. Building the billing period (non-interactive)

- **Decision**: Extract `getLeistungszeitraumFormatted(year, month)` (private in
  `LeistungszeitraumPrompt`) into a `LeistungszeitraumFactory.create(year, month):
  LeistungszeitraumPromptResult`. The interactive prompt uses it; the non-interactive path builds
  the period from `--period=YYYY-MM` through it. Validate the month is `01–12` and the period is not
  in the future.
- **Rationale**: The `zeitraum` label and `LeistungszeitraumPromptResult` are needed identically by
  both paths; one factory avoids drift.
- **Alternatives**: re-implement the formatting in the CLI (rejected: duplication).

## R8. Hours source abstraction

- **Decision**: `interface HoursProvider { fun fetch(year: Int, month: Int, customer: Properties): BigDecimal }`.
  `ManualHoursProvider` returns the supplied value (ignoring period/customer). `HarvestHoursProvider`
  reads `harvest-project-id` from the customer `Properties` and fetches billable hours (R9). A tiny
  `HoursProviders` component selects by `HoursSource`. An explicit `--hours` always overrides and
  forces the manual value even when `--hours-source=harvest` (FR-007).
- **Rationale**: Interchangeable, minimal (Constitution V); precedence unambiguous.
- **Alternatives**: `@Profile` per provider (rejected: per-run, not per-deploy choice).

## R9. Harvest hours fetch

- **Decision**: `HarvestTimeReportClient` calls the Harvest v2 project time report
  `GET {base}/reports/time/projects?from=YYYYMMDD&to=YYYYMMDD` with headers
  `Authorization: Bearer <access-token>`, `Harvest-Account-Id: <account-id>`, `User-Agent`. Find the
  row whose `project_id == harvest-project-id`; read `billable_hours` as `BigDecimal`. `from`/`to` =
  first/last day of the period month.
- **Config**: `HarvestProperties` (`@ConfigurationProperties(prefix="harvest")`) binds
  `HARVEST_ACCOUNT_ID`, `HARVEST_ACCESS_TOKEN`, and an overridable `base-url`
  (default `https://api.harvestapp.com/v2`) — env-backed (Constitution I).
- **Rationale**: Same per-project, date-ranged billable total the operator reads in Harvest and the
  current skill fetches; one request, no pagination for a single project row (SC-003).
- **Open note (non-blocking)**: confirm the exact field name (`billable_hours`) and date format
  against current Harvest docs during implementation; isolated to the client's row model.
- **Alternatives**: summing `/time_entries` (rejected: pagination + client-side filtering);
  reading the report PDF (rejected: PDF is attachment-only per the interview).

## R10. Apple Mail draft with attachments (AppleScript)

- **Decision**: Replace `open mailto:` in `MacEmailInitiator` with an AppleScript run via
  `osascript`. A pure `AppleMailScriptBuilder` produces the script from recipient/subject/body/
  attachment paths; the adapter writes it to a temp `.scpt` and runs `osascript <file>`. The script
  creates a **visible** outgoing message, sets recipient/subject/body, adds one `attachment` per
  file via `POSIX file`, and `activate`s Mail. It **never** calls `send`. `EmailInitiator.initiateEmail`
  gains an `attachments: List<Path>` parameter.
- **Templating**: subject/body come from `EmailProperties` with `{{leistungszeitraum}}` replaced
  (existing `replacePlaceholder`); apply the umlaut fix (extract `replaceUmlauts` from `sanitizeUrl`)
  so German text reads correctly, but **no** URL-encoding (that was only for `mailto:`).
- **Gotchas**: set `content` before adding attachments; a short `delay 1` after the last attachment
  avoids the activate-before-attach race; `POSIX file` handles spaces (the output lives under a
  Google Drive path with spaces); escape `"`/`\` when interpolating — the builder owns this and is
  tested.
- **Alternatives**: `osascript -e` per line (rejected: escaping fragility); JXA (rejected: no
  benefit); `.eml` drag (rejected: doesn't pre-attach).

## R11. Error model, exit codes, side-effect failures

- **Decision**: Validate all inputs up front in `InvoiceCliArgsParser`. On validation/Harvest/IO
  failure, write a machine-readable error object on stdout (json mode) and exit non-zero. A failure
  of a non-essential desktop side effect (mail draft / Finder open) after the PDF is written is a
  warning, not a failure — the result still reports success with a `warnings` array (FR-016). Exit
  codes: `0` success; `2` invalid arguments/validation; `3` Harvest fetch failure; `1` unexpected.
  The non-interactive path controls the code via `kotlin.system.exitProcess`.
- **Rationale**: Distinguishes "no invoice" (non-zero) from "invoice made, a nicety failed" (zero +
  warning); FR-005/FR-014/FR-016.
- **Alternatives**: always exit 0 (rejected: shells/agents rely on status).

## R12. Side-effect gating for unattended runs

- **Decision**: `--mail=true|false` and `--open=true|false` (default `true`) independently gate the
  Apple Mail draft and the Finder reveal. With both `false`, no desktop app is touched and the PDF +
  JSON still result (FR-013, SC-006).
- **Rationale**: Supports unattended generation while preserving human defaults.
- **Alternatives**: single `--headless` flag (rejected: less granular than the two concerns).
