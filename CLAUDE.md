# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

An interactive **command-line** Spring Boot application (Kotlin, Java 17, Maven) that generates German-language invoices (Rechnungen) as PDFs via iText 7. Despite using `spring-boot-starter`, it is **not a web app** — all work happens in a `CommandLineRunner` that prompts on stdin and exits.

## Commands

- Build: `./mvnw package` (produces `target/rechnung_generator_spring-0.0.1-SNAPSHOT.jar`)
- Run: `java -jar target/rechnung_generator_spring-0.0.1-SNAPSHOT.jar` (interactive — reads from stdin)
- All tests: `./mvnw test`
- Single test class: `./mvnw test -Dtest=NettobetragCalculatorTest`
- Single test method: `./mvnw test -Dtest=NettobetragCalculatorTest#methodName`

Source roots are non-standard: `src/main/kotlin` and `src/test/kotlin` (set in `pom.xml`). Tests use JUnit 5 + AssertJ.

## Execution flow

`RechnungGeneratorCommandLineRunner.run()` orchestrates everything in order:
1. `UnternehmenUserPrompt` — pick a Kunde (customer) and Unternehmen (issuer) from available `.properties`.
2. `LeistungszeitraumPrompt` — year/month; defaults to the **previous** month (rolls year back in January).
3. `StundenleistungPrompt` — hours worked (`BigDecimal`).
4. `PdfCreator.createRechnung()` — writes the PDF to `rechnungen/<kunde-shortName>/rechnung_<shortName>_<month>_<year>.pdf`.
5. `EmailInitiator.initiateEmail()` — opens the OS mail client with a prefilled `mailto:` draft.
6. `PathOpener` — opens the generated PDF and its folder in the OS file manager.

Generated invoices live under `rechnungen/` (one subfolder per customer short-name), created on demand by `RechnungenFolderCreator`.

## Configuration & the external-override pattern

The app is configurable entirely via files placed **next to the JAR**, so it can be shipped without code changes (see `README.md` for full examples):

- `kunden/*.properties` and `unternehmen/*.properties` — customer and issuer data. `ExternalConfigLoader` reads these external directories first; if absent it falls back to bundled resources (`src/main/resources/kunden/test-kunde.properties`, `.../unternehmen/test-unternehmen.properties`). When only one option exists it is auto-selected with no prompt.
- `application.yaml` next to the JAR — overrides the `abrechnung` (rate, USt, IBAN, …) and `email` blocks via Spring Boot externalized config. Bundled defaults are in `src/main/resources/application.yaml`.

Properties → domain objects go through `UnternehmenPropertiesParser` (→ `Unternehmen`); rate/tax settings bind to `AbrechnungProperties` and `EmailProperties` via `@ConfigurationProperties` (enabled by `@ConfigurationPropertiesScan`).

## OS-specific behavior (`termination/`)

Side effects that touch the desktop are abstracted behind interfaces and selected at runtime by `getOperatingSystem()`:
- `EmailInitiatorConfiguration` and `PathOpenerConfiguration` are `@Configuration` factories that return the Mac or Windows implementation; `OTHER` throws `IllegalStateException`.
- **Only macOS is fully implemented.** `WindowsEmailInitiator` is a `TODO()` stub. The Mac path shells out via `Runtime.exec` (`open mailto:...`), so email/file-opening steps only work on macOS.

## Conventions worth knowing

- Several files declare top-level (default-package) names — e.g. `UnternehmenPropertiesParser`, `RechnungenFolderCreator`, `LeistungsaufstellungTable`, and `model.*` — and are imported without the `at.sellemond...` package prefix. Match the existing import style when referencing them.
- Currency/date formatting helpers (`formatCurrency`, `toEuroFormat`, etc.) and iText layout extensions (`alignToBottom`) live in `util/`. Reuse them rather than re-formatting inline.
- All monetary math uses `BigDecimal`.

<!-- SPECKIT START -->
Active feature plan: `specs/001-headless-invoice-cli/plan.md` (headless/non-interactive CLI mode,
`HoursProvider` port with manual + Harvest implementations, Apple Mail draft with invoice +
Harvest-report attachments). See its `research.md`, `data-model.md`, `contracts/`, and
`quickstart.md` for technologies, structure, and validation steps.
<!-- SPECKIT END -->
