
package com.kkargas.terrametrics.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.kkargas.terrametrics.service.ClimateChartService;

@Controller
@RequestMapping("/visualizations")
public class ClimateChartController {

    private final ClimateChartService service;

    public ClimateChartController(
            ClimateChartService service) {
        this.service = service;
    }

    @GetMapping
    public String getVisualizations(Model model) {

        model.addAttribute(
            "countries", service.getCountries());

        model.addAttribute(
            "datasets", service.getDatasets());

        return "visualizations";
    }

    @GetMapping("/api/indicators")
    @ResponseBody
    public List<Map<String, Object>> getIndicators(
            @RequestParam String dataset) {

        return service.getIndicators(dataset);
    }

    @GetMapping("/api/chart")
    @ResponseBody
    public Map<String, Object> getChart(
            @RequestParam long entityId,
            @RequestParam long indicatorId,
            @RequestParam int startYear,
            @RequestParam int endYear,
            @RequestParam(defaultValue = "1")
                int period) {

        return service.getChartData(
            entityId,
            indicatorId,
            startYear,
            endYear,
            period);
    }
}
