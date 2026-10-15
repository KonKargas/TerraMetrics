
document.addEventListener("DOMContentLoaded", () => {

    if (window.lucide) lucide.createIcons();

    const $ = id => document.getElementById(id);
    const selectedCountries = new Map();

    let trendsChart = null;
    let correlationChart = null;
    let trendsRequest = 0;
    let correlationRequest = 0;

    const palette = [
        "#159b77", "#3b82f6", "#f59e0b",
        "#a855f7", "#ef4444"
    ];

    const number = value => Number(value).toLocaleString(
        "en-US", { maximumFractionDigits: 3 }
    );

    function setMessage(id, text) {
        $(id).textContent = text;
    }

    function setTab(mode) {
        const trends = mode === "trends";

        $("trendsSection").hidden = !trends;
        $("correlationSection").hidden = trends;

        $("trendsTab").classList.toggle("active", trends);
        $("correlationTab").classList.toggle("active", !trends);

        $("trendsTab").setAttribute(
            "aria-selected", String(trends));
        $("correlationTab").setAttribute(
            "aria-selected", String(!trends));

        // Charts need resizing after their tab becomes visible.
        requestAnimationFrame(() => {
            if (trends) trendsChart?.resize();
            else correlationChart?.resize();
        });
    }

    $("trendsTab").addEventListener(
        "click", () => setTab("trends"));

    $("correlationTab").addEventListener(
        "click", () => setTab("correlation"));

    // ---------------------------------------------
    // Countries selection
    // ---------------------------------------------

    function renderSelectedCountries() {
        const container = $("selectedCountries");
        container.replaceChildren();

        for (const [id, name] of selectedCountries) {
            const chip = document.createElement("span");
            chip.className = "country-chip";

            const label = document.createElement("span");
            label.textContent = name;

            const remove = document.createElement("button");
            remove.type = "button";
            remove.textContent = "×";
            remove.setAttribute("aria-label", `Remove ${name}`);

            remove.addEventListener("click", () => {
                selectedCountries.delete(id);
                renderSelectedCountries();
            });

            chip.append(label, remove);
            container.appendChild(chip);
        }
    }

    function addCountry(id) {
        const option = Array.from(
            $("countryPicker").options
        ).find(item => item.value === String(id));

        if (!option || !id || selectedCountries.has(String(id)))
            return;

        if (selectedCountries.size >= 5) {
            setMessage("trendsMessage",
                "You can compare up to five countries.");
            return;
        }

        selectedCountries.set(String(id), option.textContent);
        renderSelectedCountries();
    }

    $("countryPicker").addEventListener("change", event => {
        addCountry(event.target.value);
        event.target.value = "";
    });

    // ---------------------------------------------
    // Indicators
    // ---------------------------------------------

    async function loadIndicators(datasetId, indicatorId) {
        const dataset = $(datasetId).value;
        const select = $(indicatorId);

        select.replaceChildren(
            new Option("Select indicator", "")
        );
        select.disabled = true;

        if (!dataset) return;

        try {
            const params = new URLSearchParams({ dataset });

            const response = await fetch(
                `/comparisons/api/indicators?${params}`
            );

            if (!response.ok) {
                throw new Error("Cannot load indicators.");
            }

            const indicators = await response.json();

            if ($(datasetId).value !== dataset) return;

            for (const indicator of indicators) {
                select.add(new Option(
                    indicator.name, indicator.id
                ));
            }

            select.disabled = false;

            if (indicators.length) select.selectedIndex = 1;

        } catch (error) {
            console.error(error);
            setMessage(
                indicatorId === "trendIndicator"
                    ? "trendsMessage" : "correlationMessage",
                error.message
            );
        }
    }

    [
        ["trendDataset", "trendIndicator"],
        ["correlationDatasetX", "correlationIndicatorX"],
        ["correlationDatasetY", "correlationIndicatorY"]
    ].forEach(([dataset, indicator]) => {
        $(dataset).addEventListener("change", () =>
            loadIndicators(dataset, indicator));
    });

    function validYears(start, end) {
        return Number.isInteger(start)
            && Number.isInteger(end)
            && start >= 1800
            && end <= 2200
            && start <= end;
    }

    function destroyTrends() {
        trendsChart?.destroy();
        trendsChart = null;
        $("downloadTrends").disabled = true;
    }

    function destroyCorrelation() {
        correlationChart?.destroy();
        correlationChart = null;
        $("downloadCorrelation").disabled = true;
    }

    // ---------------------------------------------
    // Multi-country trends
    // ---------------------------------------------

    function renderTrends(data) {
        destroyTrends();

        const records = data.records;

        if (!records.length) {
            $("trendsSummary").replaceChildren();
            setMessage("trendsMessage",
                "No observations match these filters.");
            return;
        }

        const years = [...new Set(
            records.map(r => Number(r.year))
        )].sort((a, b) => a - b);

        const labels = years.map(year =>
            data.period === 1
                ? String(year)
                : `${year}–${year + data.period - 1}`
        );

        const datasets = data.countries.map((country, index) => {
            const points = new Map(
                records
                    .filter(r =>
                        Number(r.entityId) === Number(country.id))
                    .map(r => [Number(r.year), Number(r.value)])
            );

            return {
                label: country.name,
                data: years.map(y => points.get(y) ?? null),
                borderColor: palette[index % palette.length],
                backgroundColor: palette[index % palette.length],
                borderWidth: 2.5,
                tension: 0.25,
                spanGaps: false,
                pointRadius: 2,
                pointHoverRadius: 6
            };
        });

        if (typeof Chart === "undefined") {
            setMessage("trendsMessage",
                "Chart.js could not be loaded.");
            return;
        }

        trendsChart = new Chart($("trendsChart"), {
            type: $("trendChartType").value,
            data: { labels, datasets },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: {
                    mode: "index",
                    intersect: false
                },
                plugins: {
                    legend: {
                        position: "bottom"
                    },
                    tooltip: {
                        callbacks: {
                            label: context => {
                                if (context.parsed.y == null)
                                    return `${context.dataset.label}: N/A`;

                                return `${context.dataset.label}: `
                                    + number(context.parsed.y);
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: { maxTicksLimit: 12 }
                    },
                    y: {
                        title: {
                            display: true,
                            text: data.indicator.unit || "Value"
                        }
                    }
                }
            }
        });

        $("trendsTitle").textContent = data.indicator.name;
        $("trendsSubtitle").textContent =
            `${data.aggregation} · ${data.period}-year period`
            + ` · ${data.indicator.unit || ""}`;

        $("downloadTrends").disabled = false;

        renderTrendsSummary(data);

        setMessage("trendsMessage",
            `${records.length} aggregated observations loaded.`);
    }

    function renderTrendsSummary(data) {
        const tbody = $("trendsSummary");
        tbody.replaceChildren();

        for (const country of data.countries) {
            const values = data.records
                .filter(r =>
                    Number(r.entityId) === Number(country.id))
                .map(r => Number(r.value));

            const tr = document.createElement("tr");

            let fields;

            if (values.length === 0) {
                fields = [country.name, "0", "—", "—", "—"];
            } else {
                const sum = values.reduce((a, b) => a + b, 0);

                fields = [
                    country.name,
                    String(values.length),
                    number(Math.min(...values)),
                    number(Math.max(...values)),
                    number(sum / values.length)
                ];
            }

            for (const value of fields) {
                const td = document.createElement("td");
                td.textContent = value;
                tr.appendChild(td);
            }

            tbody.appendChild(tr);
        }
    }

    async function loadTrends() {
        if (selectedCountries.size === 0) {
            setMessage("trendsMessage",
                "Please select at least one country.");
            return;
        }

        const from = Number($("trendStart").value);
        const to = Number($("trendEnd").value);

        if (!validYears(from, to)) {
            setMessage("trendsMessage",
                "Please enter a valid year range.");
            return;
        }

        const version = ++trendsRequest;
        destroyTrends();

        const params = new URLSearchParams({
            indicatorId: $("trendIndicator").value,
            startYear: from,
            endYear: to,
            period: $("trendPeriod").value,
            aggregation: $("trendFunction").value
        });

        for (const id of selectedCountries.keys()) {
            params.append("countryIds", id);
        }

        setMessage("trendsMessage", "Loading comparison...");

        try {
            const response = await fetch(
                `/comparisons/api/trends?${params}`
            );

            if (!response.ok)
                throw new Error(`Request failed: HTTP ${response.status}`);

            const data = await response.json();

            if (version !== trendsRequest) return;

            renderTrends(data);

        } catch (error) {
            if (version !== trendsRequest) return;
            setMessage("trendsMessage", error.message);
        }
    }

    $("trendsForm").addEventListener("submit", event => {
        event.preventDefault();
        loadTrends();
    });

    $("resetTrends").addEventListener("click", () => {
        ++trendsRequest;
        $("trendsForm").reset();

        selectedCountries.clear();
        renderSelectedCountries();

        $("trendIndicator").replaceChildren(
            new Option("Select indicator", "")
        );
        $("trendIndicator").disabled = true;

        destroyTrends();
        $("trendsSummary").replaceChildren();

        $("trendsTitle").textContent = "Climate Trends";
        $("trendsSubtitle").textContent =
            "Choose countries and an indicator.";

        setMessage("trendsMessage", "");
    });

    // ---------------------------------------------
    // Correlation analysis
    // ---------------------------------------------

    function pearsonCorrelation(points) {
        const n = points.length;
        if (n < 3) return null;

        const meanX = points.reduce((s, p) => s + p.x, 0) / n;
        const meanY = points.reduce((s, p) => s + p.y, 0) / n;

        let numerator = 0;
        let denominatorX = 0;
        let denominatorY = 0;

        for (const point of points) {
            const dx = point.x - meanX;
            const dy = point.y - meanY;

            numerator += dx * dy;
            denominatorX += dx * dx;
            denominatorY += dy * dy;
        }

        const denominator =
            Math.sqrt(denominatorX * denominatorY);

        return denominator === 0
            ? null
            : numerator / denominator;
    }

    function renderCorrelation(data) {
        destroyCorrelation();

        const points = data.records.map(r => ({
            x: Number(r.x),
            y: Number(r.y),
            year: Number(r.year)
        }));

        $("matchedYears").textContent = points.length;

        const correlation = pearsonCorrelation(points);

        $("correlationCoefficient").textContent =
            correlation === null
                ? "N/A"
                : correlation.toFixed(4);

        if (!points.length) {
            setMessage("correlationMessage",
                "No matching years available.");
            return;
        }

        if (typeof Chart === "undefined") {
            setMessage("correlationMessage",
                "Chart.js could not be loaded.");
            return;
        }

        correlationChart = new Chart($("correlationChart"), {
            type: "scatter",
            data: {
                datasets: [{
                    label: data.country.name,
                    data: points,
                    backgroundColor: "rgba(21,155,119,0.75)",
                    borderColor: "#159b77",
                    pointRadius: 5,
                    pointHoverRadius: 8
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: context => {
                                const p = context.raw;
                                return `${p.year}: (${number(p.x)}, `
                                    + `${number(p.y)})`;
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        type: "linear",
                        title: {
                            display: true,
                            text: data.indicatorX.name
                                + " (" + data.indicatorX.unit + ")"
                        }
                    },
                    y: {
                        title: {
                            display: true,
                            text: data.indicatorY.name
                                + " (" + data.indicatorY.unit + ")"
                        }
                    }
                }
            }
        });

        $("correlationSubtitle").textContent =
            `${data.country.name} · `
            + `${data.indicatorX.name} vs ${data.indicatorY.name}`;

        $("downloadCorrelation").disabled = false;

        setMessage("correlationMessage",
            `${points.length} matching years analyzed.`);
    }

    async function loadCorrelation() {
        const from = Number($("correlationStart").value);
        const to = Number($("correlationEnd").value);

        if (!validYears(from, to)) {
            setMessage("correlationMessage",
                "Invalid year range.");
            return;
        }

        if ($("correlationIndicatorX").value ===
            $("correlationIndicatorY").value) {
            setMessage("correlationMessage",
                "Select two different indicators.");
            return;
        }

        const version = ++correlationRequest;

        destroyCorrelation();
        $("matchedYears").textContent = "—";
        $("correlationCoefficient").textContent = "—";

        const params = new URLSearchParams({
            countryId: $("correlationCountry").value,
            indicatorX: $("correlationIndicatorX").value,
            indicatorY: $("correlationIndicatorY").value,
            startYear: from,
            endYear: to
        });

        setMessage("correlationMessage",
            "Analyzing correlation...");

        try {
            const response = await fetch(
                `/comparisons/api/correlation?${params}`
            );

            if (!response.ok)
                throw new Error(`Request failed: HTTP ${response.status}`);

            const data = await response.json();

            if (version !== correlationRequest) return;

            renderCorrelation(data);

        } catch (error) {
            if (version !== correlationRequest) return;
            setMessage("correlationMessage", error.message);
        }
    }

    $("correlationForm").addEventListener("submit", event => {
        event.preventDefault();
        loadCorrelation();
    });

    // ---------------------------------------------
    // PNG exports
    // ---------------------------------------------

    function downloadChart(chart, filename) {
        if (!chart) return;

        const link = document.createElement("a");
        link.href = chart.toBase64Image();
        link.download = filename;
        link.click();
    }

    $("downloadTrends").addEventListener("click", () =>
        downloadChart(trendsChart,
            "terrametrics-country-comparison.png"));

    $("downloadCorrelation").addEventListener("click", () =>
        downloadChart(correlationChart,
            "terrametrics-correlation.png"));

    // Initial selections
    const greece = Array.from(
        $("countryPicker").options
    ).find(option => option.textContent.trim() === "Greece");

    if (greece) addCountry(greece.value);

    $("trendDataset").value = "TEMPERATURE";
    loadIndicators("trendDataset", "trendIndicator");
});
