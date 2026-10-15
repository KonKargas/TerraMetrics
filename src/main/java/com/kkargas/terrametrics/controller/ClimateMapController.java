package com.kkargas.terrametrics.controller;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.kkargas.terrametrics.service.ClimateMapService;

@Controller
@RequestMapping("/global-map")
public class ClimateMapController {
    private final ClimateMapService service;
    public ClimateMapController(ClimateMapService service) { this.service = service; }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("datasets", service.datasets());
        return "global-map";
    }

    @GetMapping("/api/indicators") @ResponseBody
    public List<Map<String,Object>> indicators(@RequestParam String dataset) {
        return service.indicators(dataset);
    }

    @GetMapping("/api/years") @ResponseBody
    public List<Map<String,Object>> years(@RequestParam long indicatorId) {
        return service.years(indicatorId);
    }

    @GetMapping("/api/data") @ResponseBody
    public Map<String,Object> data(@RequestParam long indicatorId,
            @RequestParam int year,
            @RequestParam(defaultValue="AVG") String aggregation) {
        return service.map(indicatorId, year, aggregation);
    }
}
