# Phase 1 Data Model: Headless Invoice CLI

Re-based on the actual flat code. Existing types are reused; new types are marked ▲. No persistence
is added.

## Reused existing types (unchanged)

- **`model.Unternehmen`** — `shortName, name, adresse: Adresse, uid, contact: Contact`. Used for
  **both** customer (kunde) and issuer (unternehmen). Parsed from `Properties` by
  `UnternehmenPropertiesParser`.
- **`model.Adresse`** — `strasse, stadt, land`. **`model.Contact`** — `telNr, website, email`.
- **`AbrechnungProperties`** (`@ConfigurationProperties("abrechnung")`) — `stundensatz: Double`,
  `einheit, taetigkeit, ust: Int, iban, zahlungsbedingungen`.
- **`EmailProperties`** (`@ConfigurationProperties("email")`) — `subject, body`.
- **`UnternehmenPromptResult`** — `kundenProperties: Properties, unternehmenProperties: Properties`.
- **`LeistungszeitraumPromptResult`** — `year: Int, month: Int, zeitraum: String`.
- Helpers: `calculateNettobetrag(hours, stundensatz)`, `RechnungUidGenerator.getUid(short, year,
  month)` → `"Rechnung <short>-<year>/<MM>"`, `PdfCreator.createRechnung(...)` → PDF path string.

## ▲ New / changed types

### InvoiceCliArgs (▲ `cli/`)
Validated representation of a non-interactive invocation.

| Field | Type | Notes / validation |
|-------|------|--------------------|
| issuer | String | matches an available `unternehmen/` short-name |
| customer | String | matches an available `kunden/` short-name |
| year, month | Int | from `--period=YYYY-MM`; month 1–12; not in the future |
| hoursSource | HoursSource | `MANUAL` or `HARVEST` |
| hours | BigDecimal? | required when `MANUAL`; if present, overrides `HARVEST` |
| reportPdfPath | Path? | required when `mail == true`; file must exist + be readable |
| mail | Boolean | default `true` |
| open | Boolean | default `true` |
| format | OutputFormat | `TEXT` or `JSON`, default `TEXT` |

### HoursSource (▲ enum) — `MANUAL | HARVEST`
### OutputFormat (▲ enum) — `TEXT | JSON`

### GenerationResult (▲ DTO, `cli/`)
Serialized to stdout JSON by `GenerationResultJsonWriter` (see
`contracts/generation-result.schema.json`).

| Field | Type | Source |
|-------|------|--------|
| invoiceNumber | String | `RechnungUidGenerator.getUid(customerShort, year, month)` |
| pdfPath | String (absolute) | `File(createRechnung(...)).absolutePath` |
| customer | String | `--customer` |
| issuer | String | `--issuer` |
| period | String `YYYY-MM` | from args |
| hours | BigDecimal | resolved hours |
| net | BigDecimal | `calculateNettobetrag(hours, stundensatz)` |
| vat | BigDecimal | `calculateUstBetrag(net, ust)` |
| gross | BigDecimal | net + vat |
| attachments | List<String> | `[pdfPath, reportPdfPath]` when mail enabled, else `[]` |
| warnings | List<String> | non-fatal side-effect failures |

### GenerationError (▲ DTO)
`{ code: String, message: String }` — emitted instead of `GenerationResult` on failure, with a
matching non-zero exit code (codes in `contracts/cli-contract.md`).

### HarvestTimeReportRow (▲ `harvest/`, response model)
Subset of the Harvest project time-report response: `projectId: Long`, `billableHours: BigDecimal`.

## ▲ Configuration additions

### Customer properties — `kunden/*.properties`
Add one optional key, required only when billing that customer via Harvest:
```
harvest-project-id=12345678
```
Read directly from the customer `Properties` by `HarvestHoursProvider` (no change to
`model.Unternehmen`). Bundled `kunden/test-kunde.properties` gains an example value.

### HarvestProperties — `@ConfigurationProperties(prefix = "harvest")`
Env-backed (Constitution I):

| Property | Env (relaxed binding) | Notes |
|----------|-----------------------|-------|
| accountId | `HARVEST_ACCOUNT_ID` | Harvest account id |
| accessToken | `HARVEST_ACCESS_TOKEN` | Personal access token |
| baseUrl | (default `https://api.harvestapp.com/v2`) | Overridable for tests |

## Abstractions (flat style)

| Type | Role | Impl(s) | Status |
|------|------|---------|--------|
| `HoursProvider` ▲ | hours source | `ManualHoursProvider`, `HarvestHoursProvider` | new |
| `HoursProviders` ▲ | select by `HoursSource` | — | new |
| `EmailInitiator` △ | mail draft | `MacEmailInitiator` (AppleScript) | signature gains attachments |
| `PathOpener` | Finder reveal | `MacPathOpener` | unchanged (gated by `--open`) |
| `UnternehmenPropertiesLoader` ▲ | load kunde/issuer Properties by name | — | extracted from prompt |
| `LeistungszeitraumFactory` ▲ | build period from year/month | — | extracted from prompt |
