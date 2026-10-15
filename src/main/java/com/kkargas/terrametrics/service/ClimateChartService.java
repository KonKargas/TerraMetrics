
package com.kkargas.terrametrics.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.kkargas.terrametrics.repository.ClimateChartRepository;

@Service
public class ClimateChartService {

    private final ClimateChartRepository repository;

    public ClimateChartService(
            ClimateChartRepository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> getCountries() {
        return repository.findCountries();
    }

    public List<String> getDatasets() {
        return repository.findDatasets();
    }

    public List<Map<String, Object>> getIndicators(
            String dataset) {
        return repository.findIndicators(dataset);
    }

    public Map<String, Object> getChartData(
            long entityId,
            long indicatorId,
            int startYear,
            int endYear,
            int period) {

        if (entityId <= 0 || indicatorId <= 0
                || startYear < 1800 || endYear > 2200
                || startYear > endYear
                || (period != 1 && period != 5
                    && period != 10)) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid chart parameters");
        }

        if (!repository.countryExists(entityId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Country not found");
        }

        Map<String, Object> indicator =
            repository.findIndicator(indicatorId);

        if (indicator == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Indicator not found");
        }

        List<Map<String, Object>> records =
            repository.findChartData(
                entityId,
                indicatorId,
                startYear,
                endYear,
                period);

        return Map.of(
            "indicator", indicator,
            "period", period,
            "records", records
        );
    }
}
