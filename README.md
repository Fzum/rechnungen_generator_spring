<!-- TOC -->
* [Rechnung Generator](#rechnung-generator)
  * [PDF Library iText](#pdf-library-itext)
  * [Build](#build)
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