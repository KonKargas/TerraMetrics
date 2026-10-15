
package com.kkargas.terrametrics.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.kkargas.terrametrics.service.ComparisonsService;

@Controller
@RequestMapping("/comparisons")
public class ComparisonsController {

    private final ComparisonsService service;

    public ComparisonsController(ComparisonsService service) {
        this.service = service;
    }

    @GetMapping
    public String getComparisons(Model model) {

        model.addAttribute(
            "countries", service.getCountries());

        model.addAttribute(
            "datasets", service.getDatasets());

        return "comparisons";
    }

    @GetMapping("/api/indicators")
    @ResponseBody
    public List<Map<String, Object>> getIndicators(
            @RequestParam String dataset) {

        return service.getIndicators(dataset);
    }

    @GetMapping("/api/trends")
    @ResponseBody
    public Map<String, Object> getComparison(
            @RequestParam List<Long> countryIds,
            @RequestParam long indicatorId,
            @RequestParam int startYear,
            @RequestParam int endYear,
            @RequestParam(defaultValue = "1") int period,
            @RequestParam(defaultValue = "AVG")
                String aggregation) {

        return service.compareCountries(
            countryIds, indicatorId,
            startYear, endYear,
            period, aggregation);
    }

    @GetMapping("/api/correlation")
    @ResponseBody
    public Map<String, Object> getCorrelation(
            @RequestParam long countryId,
            @RequestParam long indicatorX,
            @RequestParam long indicatorY,
            @RequestParam int startYear,
            @RequestParam int endYear) {

        return service.correlate(
            countryId, indicatorX, indicatorY,
            startYear, endYear);
    }
}
