document.addEventListener("DOMContentLoaded", () => {
    if (window.lucide) lucide.createIcons();
    const $ = id => document.getElementById(id);
    const countries = new Map();
    let datasetRequest = 0;
    let previewRequest = 0;
    let currentQuery = null;
    const number = value => Number(value).toLocaleString("en-US", { maximumFractionDigits: 3 });

    function message(value) { $("reportMessage").textContent = value; }
    function invalidate() {
        ++previewRequest;
        currentQuery = null;
        $("exportCsv").disabled = true;
        $("exportPdf").disabled = true;
    }
    function renderChips() {
        const host = $("selectedCountries"); host.replaceChildren();
        for (const [id, name] of countries) {
            const chip = document.createElement("span"); chip.className = "country-chip";
            const label = document.createElement("span"); label.textContent = name;
            const remove = document.createElement("button"); remove.type = "button";
            remove.textContent = "×"; remove.setAttribute("aria-label", `Remove ${name}`);
            remove.addEventListener("click", () => { countries.delete(id); renderChips(); invalidate(); });
            chip.append(label, remove); host.append(chip);
        }
    }
    $("countryPicker").addEventListener("change", e => {
        const id = e.target.value; const name = e.target.selectedOptions[0]?.textContent;
        if (id && !countries.has(id)) {
            if (countries.size >= 5) message("You can choose up to five countries.");
            else { countries.set(id, name); renderChips(); invalidate(); }
        }
        e.target.value = "";
    });
    async function loadIndicators() {
        const version = ++datasetRequest;
        const dataset = $("reportDataset").value;
        const select = $("reportIndicator");
        select.replaceChildren(new Option("Select indicator", "")); select.disabled = true;
        invalidate();
        if (!dataset) return;
        try {
            const response = await fetch(`/reports/api/indicators?${new URLSearchParams({dataset})}`);
            if (!response.ok) throw new Error("Unable to load indicators.");
            const result = await response.json();
            if (version !== datasetRequest) return;
            for (const item of result) select.add(new Option(item.name, item.id));
            select.disabled = false;
            if (result.length) select.selectedIndex = 1;
        } catch (e) { if (version === datasetRequest) message(e.message); }
    }
    $("reportDataset").addEventListener("change", loadIndicators);
    ["reportIndicator", "reportStart", "reportEnd", "reportPeriod", "reportAggregation"].forEach(id =>
        $(id).addEventListener("change", invalidate));

    function buildParams() {
        const start = Number($("reportStart").value);
        const end = Number($("reportEnd").value);
        if (!countries.size) throw new Error("Choose at least one country.");
        if (!$("reportIndicator").value) throw new Error("Choose a climate indicator.");
        if (!Number.isInteger(start) || !Number.isInteger(end) || start < 1800 || end > 2200 || start > end)
            throw new Error("Enter a valid year range.");
        const params = new URLSearchParams({
            indicatorId: $("reportIndicator").value,
            startYear: String(start), endYear: String(end),
            period: $("reportPeriod").value,
            aggregation: $("reportAggregation").value
        });
        for (const id of countries.keys()) params.append("countryIds", id);
        return params;
    }

    function renderReport(data) {
        const tbody = $("reportResults"); tbody.replaceChildren();
        const rows = Array.isArray(data.rows) ? data.rows : [];
        for (const row of rows) {
            const tr = document.createElement("tr");
            const entries = [
                row.country ?? "—",
                row.periodStart == null ? "—" : (row.periodStart === row.periodEnd ? String(row.periodStart) : `${row.periodStart}–${row.periodEnd}`),
                row.value == null ? "—" : number(row.value),
                row.observations ?? "—",
                row.coveredYears == null || row.expectedYears == null ? "—" : `${row.coveredYears}/${row.expectedYears} ${row.complete ? 'Complete' : 'Partial'}`
            ];
            for (const [i, value] of entries.entries()) {
                const td = document.createElement("td"); td.textContent = value;
                if (i === 4 && !row.complete) td.classList.add("partial-cell");
                tr.append(td);
            }
            tbody.append(tr);
        }
        if (!rows.length) {
            const tr = document.createElement("tr"); const td = document.createElement("td");
            td.colSpan = 5; td.className = "empty-row"; td.textContent = "No observations match these filters.";
            tr.append(td); tbody.append(tr);
        }
        $("reportSubtitle").textContent = `${data.indicator.name} · ${data.aggregation} · ${data.period}-year grouping`;
        $("totalRows").textContent = number(data.rowCount ?? rows.length);
        $("totalSelected").textContent = number(data.countries.length);
        $("reportCoverage").textContent = `${rows.filter(r => r.complete).length} complete periods`;
        message(`${rows.length} rows generated from the SQLite database.`);
    }

    $("reportForm").addEventListener("submit", async event => {
        event.preventDefault();
        let params;
        try { params = buildParams(); } catch(e) { message(e.message); return; }
        invalidate();
        const version = previewRequest;
        message("Generating report...");
        try {
            const response = await fetch(`/reports/api/preview?${params}`);
            if (!response.ok) throw new Error(`Report request failed (HTTP ${response.status}).`);
            const result = await response.json();
            if (version !== previewRequest) return;
            renderReport(result); currentQuery = params.toString();
            $("exportCsv").disabled = false; $("exportPdf").disabled = false;
        } catch(e) { if (version === previewRequest) message(e.message); }
    });

    $("exportCsv").addEventListener("click", () => {
        if (currentQuery) window.location.href = `/reports/export.csv?${currentQuery}`;
    });
    $("exportPdf").addEventListener("click", () => {
        if (currentQuery) window.open(`/reports/print?${currentQuery}`, "_blank", "noopener,noreferrer");
    });
    $("resetReport").addEventListener("click", () => {
        invalidate(); ++datasetRequest;
        $("reportForm").reset(); countries.clear(); renderChips();
        $("reportIndicator").replaceChildren(new Option("Select indicator", ""));
        $("reportIndicator").disabled = true;
        $("reportResults").innerHTML = '<tr><td colspan="5" class="empty-row">Your report preview will appear here.</td></tr>';
        $("reportSubtitle").textContent = "Configure and generate a report to view the results.";
        ["totalRows", "totalSelected", "reportCoverage"].forEach(id => $(id).textContent = "—");
        message("");
    });
});
