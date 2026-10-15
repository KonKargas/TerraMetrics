package com.kkargas.terrametrics.service;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.kkargas.terrametrics.repository.ClimateMapRepository;

@Service
public class ClimateMapService {
    private final ClimateMapRepository repository;
    public ClimateMapService(ClimateMapRepository repository) { this.repository = repository; }

    public List<String> datasets() { return repository.datasets(); }
    public List<Map<String,Object>> indicators(String dataset) {
        if (dataset == null || dataset.isBlank()) return List.of();
        return repository.indicators(dataset);
    }
    public List<Map<String,Object>> years(long indicatorId) { return repository.years(indicatorId); }

    public Map<String,Object> map(long indicatorId, int year, String aggregation) {
        if (indicatorId <= 0 || year < 1800 || year > 2200 ||
                !List.of("AVG", "SUM").contains(aggregation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid map parameters");
        }
        var indicator = repository.indicator(indicatorId);
        if (indicator == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Indicator not found");
        return Map.of("indicator", indicator, "year", year,
                      "aggregation", aggregation,
                      "countries", repository.values(indicatorId, year, aggregation));
    }
}
