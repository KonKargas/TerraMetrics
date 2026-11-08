<div align="center">

<img src="src/main/resources/static/images/biglogo.png" alt="TerraMetrics Logo" width="360">

# TerraMetrics

### Climate Data Analytics Platform

Explore historical climate indicators with interactive charts, country comparisons, a global choropleth map, and exportable reports.

![Java](https://img.shields.io/badge/Java-21%2B-orange?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.1-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-Templates-005F0F?style=flat-square&logo=thymeleaf&logoColor=white)
![SQLite](https://img.shields.io/badge/Database-SQLite-003B57?style=flat-square&logo=sqlite&logoColor=white)
![Chart.js](https://img.shields.io/badge/Charts-Chart.js-FF6384?style=flat-square&logo=chartdotjs&logoColor=white)
![D3.js](https://img.shields.io/badge/Maps-D3.js-F9A03C?style=flat-square&logo=d3dotjs&logoColor=white)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white)

[Features](#features) · [Technology Stack](#technology-stack) · [Run Locally](#run-locally) · [Screenshots](#application-screenshots) · [Data & Methodology](#data-and-methodology)

</div>

---

## Overview

**TerraMetrics** is a Java web application for exploring historical climate data from multiple datasets. It provides a unified, SQLite-backed interface for geographic exploration, time-series analysis, multi-country comparisons, and report generation. A Java ETL pipeline transforms source CSV files into a normalized relational database.

### Database Snapshot

| Metric | Count / Range |
|---|---:|
| Climate observations | **150,219** |
| Countries | **252** |
| Geographic entities (including non-country entries) | **275** |
| Climate indicators | **28** |
| Data sources | **4** |
| Observed years | **1961–2022** |

*These figures refer to the SQLite snapshot included in the repository. Coverage varies by country, indicator, and dataset.*

## Features

| Module | Capabilities |
|---|---|
| **Overview Dashboard** | Dataset totals, geographic coverage, and observation ranges. |
| **Countries Explorer** | Browse and filter countries; view individual country details. |
| **Climate Indicators** | Explore indicators by dataset and inspect metadata. |
| **Data Visualizations** | Interactive time-series charts, year ranges, and aggregation options. |
| **Comparisons** | Compare country trends and explore correlations among indicators. |
| **Global Climate Map** | Interactive choropleth with indicator/year filters, hover details, and selectable countries. |
| **Reports & Export** | Preview filtered results, export CSV, and print or save reports as PDF. |
| **Data Sources** | Dataset provenance, source metadata, and quality checks. |
| **ETL Pipeline** | Transform raw CSVs, populate SQLite tables, and create database backups. |

### Included Datasets

| Dataset Key | Indicators |
|---|---:|
| `TEMPERATURE` | 1 |
| `DISASTERS` | 7 |
| `FOREST_CARBON` | 6 |
| `LAND_COVER` | 14 |

## Technology Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4.1.1, Spring Web MVC |
| Frontend | Thymeleaf, HTML, CSS, JavaScript |
| Persistence | SQLite, Spring JDBC (`JdbcTemplate`) |
| Data processing | Java, OpenCSV |
| Visualizations | Chart.js, D3.js, TopoJSON, world-atlas |
| Build | Maven, Maven Wrapper |

**Note:** Some chart and map libraries are served from CDNs, so internet connectivity is required for these visualizations unless the assets are made available locally.

## Application Architecture

```text
Browser (Thymeleaf / CSS / JavaScript)
                  |
          Spring MVC Controllers
                  |
             Service Layer
                  |
       JDBC Repository Layer
                  |
           SQLite climate.db

Raw CSV -> Java ETL -> Normalized CSV -> SQLite
```

### Project Structure

```text
src/main/java/com/kkargas/terrametrics/
  controller/           HTTP endpoints and pages
  service/              Business, validation and reporting logic
  repository/           Spring JDBC queries
  etl/                  CSV transformation, import and backup
src/main/resources/
  templates/            Thymeleaf pages and fragments
  static/css/           Application styling
  static/js/            Chart, map and report scripts
  static/images/        Branding assets
  data/                 Source CSV files
  tf_data/              Transformed CSV output
data/climate.db          Included SQLite snapshot
```

## Run Locally

### Requirements

- **JDK 21** or compatible newer JDK (the Maven project targets Java 21).
- Internet access for Maven dependencies and the frontend visualization CDNs.
- Git, if cloning from the repository.

### Clone and Run

```bash
git clone https://github.com/KonKargas/TerraMetrics.git
cd TerraMetrics
```

**Windows:**

```bat
mvnw.cmd spring-boot:run
```

**macOS / Linux:**

```bash
./mvnw spring-boot:run
```

Open **http://localhost:8080/**.

The repository includes the populated `data/climate.db` SQLite database. Run Maven from the repository root so its relative path resolves correctly.

### Database Configuration

The default JDBC URL is:

```properties
spring.datasource.url=jdbc:sqlite:${TERRAMETRICS_DB_PATH:data/climate.db}
```

You can override the database file location using the `TERRAMETRICS_DB_PATH` environment variable.

### Rebuild the Database (Optional)

Source CSV files are located in `src/main/resources/data/`. Run the following Java entry point from your IDE with the repository root as the working directory:

```text
com.kkargas.terrametrics.etl.ClimateEtlRunner
```

The ETL process transforms CSV data into `src/main/resources/tf_data/`, populates `data/climate.db`, and creates `data/backups/climate-backup.db`.

> **Warning:** Re-importing can replace the existing database. Back up any modifications to `data/climate.db` before running the ETL process.

## Application Screenshots

### Overview Dashboard

Explore dataset totals, geographic coverage, and available climate indicators.

![TerraMetrics Dashboard](src/main/resources/static/screenshots/dashboard.png)

### Countries Explorer

Browse country data and examine available climate records.

![Countries Explorer](src/main/resources/static/screenshots/countries.png)

### Country Comparisons

Compare climate trends and values across geographic regions.

![Country Comparisons](src/main/resources/static/screenshots/comparisons.png)

### Global Climate Map

Visualize geographic patterns using an interactive world map.

![Global Climate Map](src/main/resources/static/screenshots/map.png)

### Data Visualization

Analyze climate indicators through interactive charts.

![Data Visualization](src/main/resources/static/screenshots/visualizations.png)

## Data and Methodology

The database's `sources` table contains provenance records referencing **FAO / FAOSTAT** datasets and **EM-DAT (CRED / UCLouvain)**. Source CSV files and their transformations are historical data snapshots, **not live data feeds**. Geographic and temporal coverage varies by indicator.

**Dataset licensing:** Some FAO records refer to `CC BY-NC-SA 3.0 IGO` while other data providers apply their own reuse rules. A software license does **not** automatically authorize redistribution or commercial use of these datasets or the resulting SQLite database. Verify each provider's requirements before reuse. See [DATA_SOURCES.md](DATA_SOURCES.md).

### Interpretation Notes

- Missing measurements are not treated as zero.
- Aggregation uses available records and may reflect partial period coverage.
- Choropleth colors represent an indicator's numeric values, not necessarily climate severity.
- Correlation does not establish causation.

## Limitations

- Historical datasets are not automatically updated.
- CDN-loaded visualization assets require internet access.
- PDF reports use the browser's **Print / Save as PDF** workflow; there is no dedicated server-side PDF generator.
- TerraMetrics is an educational/analytical project, not an official climate-risk assessment service.

## Author

**Konstantinos Kargas**  
Full-Stack Software Engineer

- Website: [kkargas.com](https://kkargas.com)
- GitHub: [KonKargas](https://github.com/KonKargas)

---

*TerraMetrics — climate data exploration, environmental insights, and interactive analytics.*
