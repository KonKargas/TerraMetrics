
package com.kkargas.terrametrics.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.kkargas.terrametrics.service.IndicatorsService;

@Controller
@RequestMapping("/indicators")
public class IndicatorsController {

    private final IndicatorsService service;

    public IndicatorsController(IndicatorsService service) {
        this.service = service;
    }

    @GetMapping
    public String getIndicators(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String dataset,
            @RequestParam(defaultValue = "1") int page,
            Model model) {

        search = search.trim();
        dataset = dataset.trim();
        page = Math.max(1, page);

        long total = service.countIndicators(search, dataset);
        int totalPages = service.getTotalPages(total);
        page = Math.min(page, totalPages);

        model.addAttribute("indicators",
            service.getIndicators(search, dataset, page));

        model.addAttribute("datasetSummary",
            service.getDatasetSummary());

        model.addAttribute("datasets", service.getDatasets());
        model.addAttribute("search", search);
        model.addAttribute("dataset", dataset);
        model.addAttribute("totalIndicators", total);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        return "indicators";
    }

    @GetMapping("/{id}")
    public String getIndicatorDetails(
            @PathVariable long id, Model model) {

        Map<String, Object> indicator = service.getIndicator(id);

        if (indicator == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Indicator not found");
        }

        model.addAttribute("indicator", indicator);
        model.addAttribute("sources", service.getSources(id));
        model.addAttribute("coverage", service.getCoverage(id));

        return "indicator-details";
    }
}
