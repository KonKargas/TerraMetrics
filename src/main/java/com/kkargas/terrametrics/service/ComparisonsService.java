
package com.kkargas.terrametrics.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.kkargas.terrametrics.repository.ComparisonsRepository;

@Service
public class ComparisonsService {

    private final ComparisonsRepository repository;

    public ComparisonsService(ComparisonsRepository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> getCountries() {
        return repository.findCountries();
    }

    public List<String> getDatasets() {
        return repository.findDatasets();
    }

    public List<Map<String, Object>> getIndicators(String dataset) {
        return repository.findIndicators(dataset);
    }

    private void validateYears(int startYear, int endYear) {
        if (startYear < 1800 || endYear > 2200
                || startYear > endYear) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid year range");
        }
    }

    private Map<String, Object> requireIndicator(long id) {
        Map<String, Object> indicator =
            repository.findIndicator(id);

        if (indicator == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Indicator not found");
        }

        return indicator;
    }

    public Map<String, Object> compareCountries(
            List<Long> countryIds,
            long indicatorId,
            int startYear,
            int endYear,
            int period,
            String aggregation) {

        validateYears(startYear, endYear);

        if (countryIds == null || countryIds.isEmpty()
                || countryIds.size() > 5
                || countryIds.stream().anyMatch(id -> id <= 0)
                || countryIds.stream().distinct().count()
                    != countryIds.size()) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Select 1 to 5 distinct countries");
        }

        if (period != 1 && period != 5 && period != 10) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid aggregation period");
        }

        if (!List.of("AVG", "SUM").contains(aggregation)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid aggregation function");
        }

        Map<String, Object> indicator =
            requireIndicator(indicatorId);

        List<Map<String, Object>> countries =
            repository.findSelectedCountries(countryIds);

        if (countries.size() != countryIds.size()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "One or more countries were not found");
        }

        List<Map<String, Object>> records =
            repository.findComparison(
                countryIds, indicatorId, startYear,
                endYear, period, aggregation);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("countries", countries);
        result.put("indicator", indicator);
        result.put("period", period);
        result.put("aggregation", aggregation);
        result.put("records", records);

        return result;
    }

    public Map<String, Object> correlate(
            long countryId,
            long indicatorX,
            long indicatorY,
            int startYear,
            int endYear) {

        validateYears(startYear, endYear);

        if (indicatorX == indicatorY) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Select two different indicators");
        }

        Map<String, Object> x =
            requireIndicator(indicatorX);

        Map<String, Object> y =
            requireIndicator(indicatorY);

        List<Map<String, Object>> countries =
            repository.findSelectedCountries(
                List.of(countryId));

        if (countries.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Country not found");
        }

        List<Map<String, Object>> records =
            repository.findCorrelation(
                countryId, indicatorX, indicatorY,
                startYear, endYear);

        return Map.of(
            "country", countries.get(0),
            "indicatorX", x,
            "indicatorY", y,
            "records", records
        );
    }
}
