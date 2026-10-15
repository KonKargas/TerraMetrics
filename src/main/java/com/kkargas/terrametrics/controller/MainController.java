package com.kkargas.terrametrics.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    private final JdbcTemplate jdbcTemplate;

    public MainController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping({"/", "/index"})
    public String getIndex(Model model) {

        // General statistics
        model.addAttribute("totalCountries",
            count("SELECT COUNT(*) FROM countries"));

        model.addAttribute("totalEntities",
            count("SELECT COUNT(*) FROM geographic_entities"));

        model.addAttribute("totalIndicators",
            count("SELECT COUNT(*) FROM indicators"));

        model.addAttribute("totalRecords",
            count("SELECT COUNT(*) FROM climate_records"));

        // Available years
        model.addAttribute("firstYear",
            jdbcTemplate.queryForObject(
                "SELECT MIN(Year) FROM climate_records",
                Integer.class));

        model.addAttribute("lastYear",
            jdbcTemplate.queryForObject(
                "SELECT MAX(Year) FROM climate_records",
                Integer.class));

        // Records per dataset
        model.addAttribute("datasetStatistics",
            jdbcTemplate.queryForList("""
                SELECT
                    i.Dataset AS dataset,
                    COUNT(*) AS total
                FROM climate_records r
                JOIN indicators i
                    ON r.IndicatorID = i.IndicatorID
                GROUP BY i.Dataset
                ORDER BY i.Dataset
                """));

        return "index";
    }

    private long count(String sql) {
        Long result = jdbcTemplate.queryForObject(
            sql, Long.class);

        return result != null ? result : 0L;
    }
}
