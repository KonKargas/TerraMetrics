# Publish TerraMetrics to GitHub

## Repository metadata

**Name:** `TerraMetrics`  
**Description:** `Climate data analytics platform built with Spring Boot, SQLite, Thymeleaf and interactive data visualizations.`  
**Topics:** `java`, `spring-boot`, `sqlite`, `thymeleaf`, `climate-data`, `data-visualization`, `d3js`, `chartjs`, `etl`, `analytics`

## First push (new empty repository)

Create a GitHub repository named `TerraMetrics`. For this prepared project, **do not auto-create a README, .gitignore, or LICENSE** on GitHub because the local files already provide them.

From the project root:

```bash
git init
git branch -M main
git add .
git status
git commit -m "Initial commit: TerraMetrics climate analytics platform"
git remote add origin https://github.com/KonKargas/TerraMetrics.git
git push -u origin main
```

If `origin` already exists, check `git remote -v` and use `git remote set-url origin https://github.com/KonKargas/TerraMetrics.git` if needed; don't add a duplicate remote.

## Pre-push checklist

- [ ] Verify third-party data redistribution permissions (see `DATA_SOURCES.md`).
- [ ] Verify `git status` does **not** include secrets, machine-specific files, or backups.
- [ ] Confirm `spring.datasource.url` points to the portable relative DB path.
- [ ] Test `mvnw.cmd spring-boot:run` (Windows) or `./mvnw spring-boot:run` (macOS/Linux).
- [ ] Add real screenshots under `docs/screenshots` and enable README image links.
- [ ] Select a software license **only if desired**; don't imply datasets are covered by it.

## If data cannot be redistributed

Remove `data/climate.db`, `src/main/resources/data/`, and `src/main/resources/tf_data/` from the public source tree **before the first commit**. Document authorized data acquisition and import. Without a populated DB, the app's pages will not work until the ETL is run successfully.
