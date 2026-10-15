
document.addEventListener("DOMContentLoaded", async () => {

    if (window.lucide) {
        lucide.createIcons();
    }

    const form = document.getElementById("chartFilters");
    const countrySelect = document.getElementById("countrySelect");
    const datasetSelect = document.getElementById("datasetSelect");
    const indicatorSelect = document.getElementById("indicatorSelect");
    const startYear = document.getElementById("startYear");
    const endYear = document.getElementById("endYear");
    const periodSelect = document.getElementById("periodSelect");
    const chartType = document.getElementById("chartType");

    const chartTitle = document.getElementById("chartTitle");
    const chartSubtitle = document.getElementById("chartSubtitle");
    const chartMessage = document.getElementById("chartMessage");
    const downloadButton = document.getElementById("downloadChart");

    let climateChart = null;
    let chartData = null;
    let requestVersion = 0;

    function message(text) {
        chartMessage.textContent = text;
    }

    function formatValue(value) {
        return Number(value).toLocaleString("en-US", {
            maximumFractionDigits: 3
        });
    }

    function resetStatistics() {
        ["totalPoints", "minimumValue",
         "maximumValue", "averageValue"].forEach(id => {
            document.getElementById(id).textContent = "—";
        });
    }

    function clearChart() {
        if (climateChart) {
            climateChart.destroy();
            climateChart = null;
        }

        chartData = null;
        downloadButton.disabled = true;
        resetStatistics();
    }

    async function loadIndicators() {

        const dataset = datasetSelect.value;

        indicatorSelect.replaceChildren(
            new Option("Select indicator", "")
        );

        indicatorSelect.disabled = true;

        if (!dataset) return;

        try {
            const params = new URLSearchParams({ dataset });

            const response = await fetch(
                `/visualizations/api/indicators?${params}`
            );

            if (!response.ok) {
                throw new Error("Unable to load indicators.");
            }

            const indicators = await response.json();

            // Ignore an outdated response.
            if (datasetSelect.value !== dataset) return;

            for (const indicator of indicators) {
                indicatorSelect.add(
                    new Option(indicator.name, indicator.id)
                );
            }

            indicatorSelect.disabled = false;

            if (indicators.length > 0) {
                indicatorSelect.selectedIndex = 1;
            }

        } catch (error) {
            message(error.message);
        }
    }

    function updateStatistics(records) {

        const values = records.map(r => Number(r.value));

        const minimum = Math.min(...values);
        const maximum = Math.max(...values);
        const average = values.reduce(
            (sum, value) => sum + value, 0
        ) / values.length;

        document.getElementById("totalPoints")
            .textContent = records.length;

        document.getElementById("minimumValue")
            .textContent = formatValue(minimum);

        document.getElementById("maximumValue")
            .textContent = formatValue(maximum);

        document.getElementById("averageValue")
            .textContent = formatValue(average);
    }

    function renderChart(data) {

        clearChart();

        const records = data.records;

        if (!records.length) {
            message("No observations found for these filters.");
            return;
        }

        if (typeof Chart === "undefined") {
            message("Chart.js could not be loaded.");
            return;
        }

        const indicator = data.indicator;
        const unit = indicator.unit || "";

        const labels = records.map(record =>
            data.period === 1
                ? String(record.year)
                : `${record.year}–${Number(record.year) + data.period - 1}`
        );

        const values = records.map(record =>
            Number(record.value)
        );

        const canvas = document.getElementById("climateChart");

        const type = chartType.value;

        const color = "#159b77";

        climateChart = new Chart(canvas, {
            type,
            data: {
                labels,
                datasets: [{
                    label: indicator.name,
                    data: values,
                    borderColor: color,
                    backgroundColor: type === "line"
                        ? "rgba(21,155,119,0.12)"
                        : "rgba(21,155,119,0.75)",
                    borderWidth: 2,
                    tension: 0.3,
                    fill: type === "line",
                    pointRadius: type === "line" ? 2 : 0,
                    pointHoverRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: {
                    intersect: false,
                    mode: "index"
                },
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        callbacks: {
                            label: context =>
                                `${formatValue(context.parsed.y)} ${unit}`
                        }
                    }
                },
                scales: {
                    x: {
                        grid: {
                            display: false
                        },
                        ticks: {
                            maxTicksLimit: 12
                        },
                        title: {
                            display: true,
                            text: "Year"
                        }
                    },
                    y: {
                        beginAtZero: false,
                        title: {
                            display: Boolean(unit),
                            text: unit
                        },
                        ticks: {
                            callback: value => formatValue(value)
                        }
                    }
                }
            }
        });

        chartData = data;
        downloadButton.disabled = false;

        chartTitle.textContent =
            countrySelect.selectedOptions[0].text;

        chartSubtitle.textContent =
            `${indicator.name} • ${unit} • ${data.period}-year aggregation`;

        updateStatistics(records);

        message(`${records.length} data points loaded successfully.`);
    }

    async function loadChart() {

        const entityId = countrySelect.value;
        const indicatorId = indicatorSelect.value;
        const from = Number(startYear.value);
        const to = Number(endYear.value);

        const currentRequest = ++requestVersion;

        if (!entityId || !indicatorId) {
            message("Please select a country and an indicator.");
            clearChart();
            return;
        }

        if (!Number.isInteger(from) || !Number.isInteger(to)
                || from < 1800 || to > 2200 || from > to) {

            message("Please enter a valid year range.");
            clearChart();
            return;
        }

        const params = new URLSearchParams({
            entityId,
            indicatorId,
            startYear: from,
            endYear: to,
            period: periodSelect.value
        });

        message("Loading climate observations...");
        clearChart();

        try {
            const response = await fetch(
                `/visualizations/api/chart?${params}`
            );

            if (!response.ok) {
                throw new Error(
                    `Failed to load chart (HTTP ${response.status}).`
                );
            }

            const data = await response.json();

            if (currentRequest !== requestVersion) return;

            renderChart(data);

        } catch (error) {
            if (currentRequest !== requestVersion) return;
            clearChart();
            message(error.message);
        }
    }

    form.addEventListener("submit", event => {
        event.preventDefault();
        loadChart();
    });

    datasetSelect.addEventListener("change", async () => {
        ++requestVersion;
        clearChart();
        await loadIndicators();
    });

    chartType.addEventListener("change", () => {
        if (chartData) renderChart(chartData);
    });

    document.getElementById("resetFilters")
        .addEventListener("click", async () => {

            ++requestVersion;

            form.reset();
            clearChart();

            chartTitle.textContent = "Climate Trend Analysis";
            chartSubtitle.textContent =
                "Select filters to explore climate data.";

            await loadIndicators();
            await loadChart();
        });

    downloadButton.addEventListener("click", () => {

        if (!climateChart) return;

        const link = document.createElement("a");
        link.download = "terrametrics-climate-chart.png";
        link.href = climateChart.toBase64Image();
        link.click();
    });

    await loadIndicators();

    if (countrySelect.value && indicatorSelect.value) {
        await loadChart();
    }
});
