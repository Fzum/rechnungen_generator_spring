# Phase 1 Data Model: Headless Invoice CLI

Most entities already exist in the domain core and are **reused unchanged**. New types are marked
▲. The feature adds no persistence — these are in-memory value objects plus two config additions.

## Reused domain entities (unchanged)

- **Invoice** — issuer, customer, number, issueDate, period, hours (`BigDecimal`), billing
  (`BillingDetails`), totals (`BillingTotals`). Produced by `GenerateInvoiceUseCase`.
- **Company** — shortName, name, address, contact (incl. email), uid. Used for both customer and
  issuer roles via `CompanyConfigRepository`.
- **BillingDetails** — hourlyRate, unit, activity, vatRate, iban, paymentTerms (from
  `AbrechnungProperties`).
- **BillingTotals** — net, vat, gross (`BigDecimal`), from `BillingCalculator`. Unchanged math.
- **InvoicePeriod** — month, year, label; built by `InvoicePeriodFactory`. Drives Harvest
  `from`/`to` (first/last day of month).

## ▲ New / changed types

### InvoiceCliArgs (▲ value object, `cli/`)
Parsed and validated representation of the non-interactive invocation.

| Field | Type | Notes / validation |
|-------|------|--------------------|
| issuer | String | Must match an available issuer short-name |
| customer | String | Must match an available customer short-name |
| period | InvoicePeriod | Parsed from `YYYY-MM`; rejected if malformed or in the future |
| hoursSource | HoursSource | `MANUAL` or `HARVEST` |
| hours | BigDecimal? | Required when `hoursSource == MANUAL`; if present, overrides HARVEST |
| reportPdfPath | Path? | Required when `mail == true`; file must exist and be readable |
| mail | Boolean | Default `true` |
| open | Boolean | Default `true` |
| format | OutputFormat | `TEXT` or `JSON`, default `TEXT` |

**Invariants**: exactly one period; `hours` present ⇒ used regardless of source; `hoursSource ==
HARVEST` ⇒ resolved customer has a non-blank `harvestProjectId`; `mail == true` ⇒ `reportPdfPath`
exists.

### HoursSource (▲ enum)
`MANUAL | HARVEST`. Selects the `HoursProvider` implementation per run.

### GenerateInvoiceCommand (△ changed)
Existing command gains: `hoursSource`, `reportPdfPath: Path?`, `openMail: Boolean`,
`openFiles: Boolean`. Continues to carry issuer, customer, period; `hours` becomes resolved by the
use case via the provider unless an explicit override is supplied.

### GenerationResult (▲ DTO, application/cli boundary)
The machine-readable outcome. Serialized to stdout JSON by `GenerationResultJsonWriter`.

| Field | Type | Source |
|-------|------|--------|
| invoiceNumber | String | `InvoiceNumberPolicy` |
| pdfPath | String (abs) | `InvoiceDocumentPort.create` |
| customer | String | command |
| issuer | String | command |
| period | String `YYYY-MM` | command |
| hours | BigDecimal | resolved hours |
| net | BigDecimal | totals |
| vat | BigDecimal | totals |
| gross | BigDecimal | totals |
| attachments | List<String> | [pdfPath, reportPdfPath] when mail enabled |
| warnings | List<String> | non-fatal side-effect failures (empty on a clean run) |

### GenerationError (▲ DTO)
`{ code: String, message: String }` — emitted instead of `GenerationResult` on failure, with a
matching non-zero exit code (see contracts).

### EmailDraft (▲ conceptual, inside the mail adapter)
recipient (customer email), subject, body (templated with period), attachments: exactly
[invoicePdf, harvestReportPdf]. Never sent.

### HarvestTimeReportRow (▲ adapter response model)
Subset of the Harvest project time-report response: `projectId: Long`, `billableHours:
BigDecimal`. Other fields ignored.

## ▲ Configuration additions

### Customer properties — `kunden/*.properties`
Add one optional key:
```
harvest-project-id=12345678
```
Required only when that customer is billed with `--hours-source=harvest`. Parsed alongside the
existing customer fields; bundled `test-kunde.properties` gains an example value.

### HarvestProperties — `@ConfigurationProperties(prefix = "harvest")`
Bound from environment (per Constitution I, credentials never hardcoded/committed):

| Property | Env (typical) | Notes |
|----------|---------------|-------|
| accountId | `HARVEST_ACCOUNT_ID` | Harvest account id |
| accessToken | `HARVEST_ACCESS_TOKEN` | Personal access token |
| baseUrl | (default `https://api.harvestapp.com/v2`) | Overridable for tests |

## Port summary (hexagonal)

| Port | Direction | Impl(s) | Status |
|------|-----------|---------|--------|
| `HoursProvider` ▲ | out | `ManualHoursProvider`, `HarvestHoursProvider` | new |
| `MailDraftPort` △ | out | `MacEmailInitiator` (AppleScript) | signature gains attachments |
| `InvoiceDocumentPort` | out | `PdfCreator`-backed | unchanged |
| `FileOpenPort` | out | `MacPathOpener` | unchanged (gated by `--open`) |
| `CompanyConfigRepository` | out | properties-backed | parses new `harvest-project-id` |
