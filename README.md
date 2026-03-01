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