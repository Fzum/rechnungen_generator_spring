<!-- TOC -->
* [Rechnung Generator](#rechnung-generator)
  * [PDF Library iText](#pdf-library-itext)
  * [Build](#build)
  * [Konfiguration](#konfiguration)
<!-- TOC -->

# Rechnung Generator

Dieses Commandline Programm kann für unterschiedliche Kunden und (leistende) Unternehmen eine Rechnung erstellen.

## PDF Library iText

Die verwendete Library für die Erstellung der PDFs
ist iText 7. Eine gute Dokumentation
ist [hier zu finden.](https://kb.itextpdf.com/home/it7kb/ebooks/itext-7-jump-start-tutorial-for-java)

## Build
- `mvn package` ausführen.
- `java -jar target/rechnung_generator_spring-0.0.1-SNAPSHOT.jar`

## Konfiguration

Das Programm lässt sich vollständig über Dateien konfigurieren, die **neben dem JAR** abgelegt werden.
So kann die Anwendung ohne Änderungen am Quellcode an andere Personen weitergegeben werden.

### Kunden

Lege eine oder mehrere `.properties`-Dateien im Unterordner `kunden/` neben dem JAR ab.
Das Programm erkennt alle Dateien in diesem Verzeichnis automatisch und listet sie zur Auswahl auf.

**Beispiel: `kunden/mein-kunde.properties`**
```properties
short-name=MeinKunde
name=Mein Kunde GmbH
strasse=Musterstraße 1
stadt=1010 Wien
land=Österreich
uid=ATU12345678
email=rechnungen@mein-kunde.at
```

### Unternehmen (Rechnungsaussteller)

Lege eine oder mehrere `.properties`-Dateien im Unterordner `unternehmen/` neben dem JAR ab.

**Beispiel: `unternehmen/mein-unternehmen.properties`**
```properties
short-name=MeinUnternehmen
name=Max Mustermann, BSc.
strasse=Beispielgasse 5 Top 2
stadt=1030 Wien
land=Österreich
telNr=+43 123 456 789
website=www.mein-unternehmen.at
email=office@mein-unternehmen.at
uid=ATU87654321
```

### Stundensatz & Abrechnungsparameter

Lege eine `application.yaml`-Datei neben dem JAR ab, um Stundensatz, USt., IBAN und andere
Einstellungen zu überschreiben. Spring Boot lädt diese automatisch mit höherer Priorität als die
mitgelieferte Konfiguration.

**Beispiel: `application.yaml`**
```yaml
abrechnung:
  stundensatz: 120.00
  einheit: Stunden
  taetigkeit: Software Engineering
  ust: 20
  iban: AT12 3456 7890 1234 5678
  zahlungsbedingungen: 30 Tage ohne Abzüge
```

### Verzeichnisstruktur neben dem JAR

```
rechnung-generator/
├── rechnung_generator_spring-0.0.1-SNAPSHOT.jar
├── application.yaml          ← Stundensatz, IBAN, etc. (optional)
├── kunden/
│   ├── kunde-a.properties    ← Kundendaten
│   └── kunde-b.properties
└── unternehmen/
    └── mein-unternehmen.properties  ← Daten des Rechnungsausstellers
```

Falls kein externes `kunden/`- oder `unternehmen/`-Verzeichnis vorhanden ist,
werden die im JAR enthaltenen Standarddaten verwendet.

## Nicht-interaktiver Modus (CLI)

Wird das JAR mit Argumenten gestartet, läuft es ohne Eingabeaufforderungen und gibt das Ergebnis
maschinenlesbar aus. Ohne Argumente bleibt der interaktive Modus unverändert.

```
java -jar rechnung_generator_spring-0.0.1-SNAPSHOT.jar generate \
  --issuer=<unternehmen-short-name> \
  --customer=<kunde-short-name> \
  --period=YYYY-MM \
  --hours-source=manual|harvest \
  --hours=<dezimal> \        # Pflicht bei manual; überschreibt einen harvest-Wert
  --report-pdf=<pfad> \      # Pflicht wenn --mail=true; wird an den Entwurf angehängt
  --mail=true|false \        # Apple-Mail-Entwurf öffnen (Default true)
  --open=true|false \        # PDF + Ordner im Finder öffnen (Default true)
  --format=text|json
```

Bei `--format=json` steht auf **stdout** genau ein JSON-Objekt (Logs gehen nach stderr):

```json
{"invoiceNumber":"...","pdfPath":"...","customer":"...","issuer":"...","period":"YYYY-MM",
 "hours":0,"net":0,"vat":0,"gross":0,"attachments":["..."],"warnings":[]}
```

Exit-Codes: `0` Erfolg · `2` ungültige Argumente/Validierung · `3` Harvest-Fehler · `1` unerwartet.

### Harvest-Stunden

`--hours-source=harvest` holt die fakturierbaren Stunden für die in der Kunden-`.properties`
hinterlegte `harvest-project-id`. Zugangsdaten kommen aus Umgebungsvariablen:

```
export HARVEST_ACCOUNT_ID=...
export HARVEST_ACCESS_TOKEN=...
```

### E-Mail-Entwurf

Mit `--mail=true` wird ein **Apple-Mail-Entwurf** geöffnet (mit Rechnung **und** dem offiziellen
Harvest-Monatsreport als Anhang) und **niemals automatisch versendet** — zur manuellen Kontrolle.