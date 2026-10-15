
package com.kkargas.terrametrics.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.kkargas.terrametrics.service.CountriesService;

@Controller
@RequestMapping("/countries")
public class CountriesController {

    private final CountriesService service;

    public CountriesController(CountriesService service) {
        this.service = service;
    }

    @GetMapping
    public String getCountries(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String continent,
            @RequestParam(defaultValue = "1") int page,
            Model model) {

        page = Math.max(1, page);

        search = search.trim();
        continent = continent.trim();

        long total = service.getTotalCountries(
            search, continent);

        int totalPages = service.getTotalPages(total);
        page = Math.min(page, totalPages);

        model.addAttribute("countries",
            service.getCountries(search, continent, page));

        model.addAttribute("continents",
            service.getContinents());

        model.addAttribute("search", search);
        model.addAttribute("continent", continent);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCountries", total);

        return "countries";
    }

    @GetMapping("/{id}")
    public String getCountryDetails(
            @PathVariable long id,
            Model model) {

        Map<String, Object> country =
            service.getCountry(id);

        if (country == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Country not found");
        }

        model.addAttribute("country", country);
        model.addAttribute("statistics",
            service.getStatistics(id));
        model.addAttribute("datasets",
            service.getDatasets(id));

        return "country-details";
    }
}
