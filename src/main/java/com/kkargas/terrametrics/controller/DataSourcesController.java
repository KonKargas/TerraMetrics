package com.kkargas.terrametrics.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.kkargas.terrametrics.service.DataSourcesService;

@Controller
public class DataSourcesController {
    private final DataSourcesService service;
    public DataSourcesController(DataSourcesService service) { this.service = service; }

    @GetMapping("/data-sources")
    public String dataSources(Model model) {
        model.addAttribute("totals", service.totals());
        model.addAttribute("datasets", service.datasets());
        model.addAttribute("sources", service.sources());
        model.addAttribute("sourceDatasets", service.sourceDatasets());
        model.addAttribute("checks", service.integrityChecks());
        return "data-sources";
    }
}
