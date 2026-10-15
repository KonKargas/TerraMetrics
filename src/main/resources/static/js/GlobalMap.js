document.addEventListener("DOMContentLoaded", async () => {
    const $ = id => document.getElementById(id);
    if (window.lucide) lucide.createIcons();
    const dataset = $("mapDataset"), indicator = $("mapIndicator"), year = $("mapYear");
    const aggregation = $("mapAggregation"), tooltip = $("mapTooltip");
    if (typeof d3 === "undefined" || typeof topojson === "undefined") {
        $("mapMessage").textContent = "D3 or TopoJSON failed to load. Check the CDN scripts.";
        return;
    }
    // One palette per dataset; both the map and the legend use these stops.
    const PALETTES = {
        TEMPERATURE: ["#fff9c4", "#ffdc73", "#ff9f43", "#ed553b", "#a71930"],
        DISASTERS: ["#fff3cf", "#ffc078", "#f28e4b", "#d94841", "#851c38"],
        FOREST_CARBON: ["#e6f4d7", "#abd59b", "#63af71", "#28834d", "#10552d"],
        LAND_COVER: ["#f4e5cb", "#ddc199", "#b99160", "#90613e", "#593b2b"]
    };
    const DEFAULT_PALETTE = ["#eef3f5", "#c0d6db", "#8bb5b9", "#4b8793", "#24576c"];
    const legend = document.querySelector(".legend-gradient");

    function applyPalette() {
        const stops = PALETTES[dataset.value] || DEFAULT_PALETTE;
        if (legend) legend.style.background = `linear-gradient(to right, ${stops.join(", ")})`;
        const stage = $("mapStage");
        stage.dataset.theme = dataset.value || "default";
        return stops;
    }
    applyPalette();

    const svg = d3.select("#worldMap");
    const projection = d3.geoNaturalEarth1().fitExtent([[14, 15], [986, 515]], {type:"Sphere"});
    const path = d3.geoPath(projection);
    const countriesLayer = svg.append("g");
    const zoom = d3.zoom().scaleExtent([1, 8]).on("zoom", ({transform}) => {
        countriesLayer.attr("transform", transform);
    });
    svg.call(zoom);
    $("resetZoom").addEventListener("click", () => svg.transition().duration(350).call(zoom.transform, d3.zoomIdentity));

    let shapes = [], current = new Map(), activeColor = () => "#dce4e9", latestRequest = 0;
    let indicatorVersion = 0, yearsVersion = 0;
    const format = n => Number(n).toLocaleString("en-US", {maximumFractionDigits:3});
    const status = text => { $("mapMessage").textContent = text; };
    const relativeUrl = path => new URL(path, document.baseURI).toString();

    async function json(url) {
        const response = await fetch(url);
        if (!response.ok) throw new Error(`Request failed (HTTP ${response.status})`);
        return response.json();
    }

    // world-atlas feature IDs are ISO 3166-1 numeric codes.
    // Our countries.ISO_Code stores the corresponding numeric code.
    try {
        if (typeof d3 === "undefined" || typeof topojson === "undefined") {
            throw new Error("D3 or TopoJSON is unavailable. Check the CDN scripts.");
        }
        status("Loading world boundaries...");
        const topology = await json("https://cdn.jsdelivr.net/npm/world-atlas@2/countries-110m.json");
        shapes = topojson.feature(topology, topology.objects.countries).features;
        countriesLayer.selectAll("path")
            .data(shapes)
            .join("path")
            .attr("class", "map-country")
            .attr("d", path)
            .attr("fill", "#dce4e9")
            .on("mousemove", (event, feature) => {
                const record = current.get(Number(feature.id));
                const name = record?.country ?? feature.properties?.name ?? "Unmapped country";
                tooltip.replaceChildren();
                const heading = document.createElement("strong"); heading.textContent = name;
                const details = document.createElement("span");
                details.textContent = record ? `${format(record.value)} ${currentUnit} · ${record.observations} record(s)` : "No observation for this selection";
                tooltip.append(heading, details);
                tooltip.hidden = false;
                const stage = $("mapStage").getBoundingClientRect();
                tooltip.style.left = Math.min(event.clientX - stage.left + 14, stage.width - 245) + "px";
                tooltip.style.top = Math.max(5, event.clientY - stage.top - 48) + "px";
            })
            .on("mouseleave", () => { tooltip.hidden = true; })
            .on("click", (event, feature) => {
                const record = current.get(Number(feature.id));
                if (record) window.location.href = relativeUrl(`countries/${record.entityId}`);
            });
        status("Map loaded. Choose a dataset and indicator.");
    } catch (error) {
        status(error.message);
        return;
    }

    let currentUnit = "";
    function clearData() {
        ++latestRequest;
        current = new Map();
        countriesLayer.selectAll("path").attr("fill", "#dce4e9");
        ["mappedCountries","minClimate","maxClimate","avgClimate","legendMin","legendMid","legendMax"].forEach(id => $(id).textContent = "—");
        $("mapTitle").textContent = "Climate distribution";
        $("mapSubtitle").textContent = "Choose filters and update the map.";
    }

    async function loadIndicators() {
        const version = ++indicatorVersion;
        ++yearsVersion;
        clearData();
        indicator.replaceChildren(new Option("Choose indicator", ""));
        indicator.disabled = true;
        year.replaceChildren(new Option("Choose year", ""));
        year.disabled = true;
        if (!dataset.value) return;
        try {
            const rows = await json(`/global-map/api/indicators?${new URLSearchParams({dataset:dataset.value})}`);
            if (version !== indicatorVersion) return;
            rows.forEach(i => indicator.add(new Option(`${i.name} (${i.unit ?? ''})`, i.id)));
            if (rows.length) { indicator.selectedIndex = 1; indicator.disabled = false; await loadYears(); }
        } catch (e) { if (version === indicatorVersion) status(e.message); }
    }
    async function loadYears() {
        const version = ++yearsVersion;
        clearData();
        year.replaceChildren(new Option("Choose year", ""));
        year.disabled = true;
        if (!indicator.value) return;
        try {
            const rows = await json(`/global-map/api/years?${new URLSearchParams({indicatorId:indicator.value})}`);
            if (version !== yearsVersion) return;
            rows.forEach(r => year.add(new Option(r.year, r.year)));
            if (rows.length) { year.selectedIndex = 1; year.disabled = false; await updateMap(); }
        } catch (e) { if (version === yearsVersion) status(e.message); }
    }
    async function updateMap() {
        if (!indicator.value || !year.value) return;
        const version = ++latestRequest;
        status("Loading climate values...");
        try {
            const query = new URLSearchParams({indicatorId:indicator.value,year:year.value,aggregation:aggregation.value});
            const data = await json(`/global-map/api/data?${query}`);
            if (version !== latestRequest) return;
            currentUnit = data.indicator.unit || "";
            current = new Map(data.countries.filter(r => r.isoNumeric != null).map(r => [Number(r.isoNumeric),r]));
            const values = [...current.values()].map(r => Number(r.value)).filter(Number.isFinite);
            if (!values.length) { clearData(); status("No observations for this year and indicator."); return; }
            const min = d3.min(values), max = d3.max(values);
            const stops = applyPalette();
            const scale = min === max
                ? () => stops[Math.floor(stops.length / 2)]
                : d3.scaleLinear()
                    .domain(stops.map((_, i) => min + (max - min) * i / (stops.length - 1)))
                    .range(stops)
                    .interpolate(d3.interpolateRgb)
                    .clamp(true);
            activeColor = scale;
            countriesLayer.selectAll("path").attr("fill", f => {
                const row = current.get(Number(f.id)); return row ? scale(Number(row.value)) : "#dce4e9";
            });
            $("mappedCountries").textContent = format(current.size);
            $("minClimate").textContent = format(min);
            $("maxClimate").textContent = format(max);
            $("avgClimate").textContent = format(d3.mean(values));
            $("legendMin").textContent = format(min);
            $("legendMid").textContent = format((min+max)/2);
            $("legendMax").textContent = format(max);
            $("mapTitle").textContent = data.indicator.name;
            $("mapSubtitle").textContent = `${data.year} · ${currentUnit} · ${data.aggregation} across available source records`;
            status(`${current.size} country codes with climate measurements. Hover to inspect; click a colored country to view its profile.`);
        } catch (e) { if (version === latestRequest) status(e.message); }
    }
    dataset.addEventListener("change", () => {
        applyPalette();
        loadIndicators();
    });
    indicator.addEventListener("change", loadYears);
    $("mapFilters").addEventListener("submit", event => { event.preventDefault(); updateMap(); });
    aggregation.addEventListener("change", updateMap);
    const preferred = [...dataset.options].find(o => o.value === "TEMPERATURE");
    if (preferred) { dataset.value = preferred.value; await loadIndicators(); }
});
