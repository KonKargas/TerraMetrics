
package com.kkargas.terrametrics.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.kkargas.terrametrics.repository.IndicatorsRepository;

@Service
public class IndicatorsService {

    private static final int PAGE_SIZE = 10;

    private final IndicatorsRepository repository;

    public IndicatorsService(IndicatorsRepository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> getIndicators(
            String search, String dataset, int page) {

        int offset = (page - 1) * PAGE_SIZE;

        return repository.findIndicators(
            search, dataset, PAGE_SIZE, offset);
    }

    public long countIndicators(String search, String dataset) {
        return repository.countIndicators(search, dataset);
    }

    public int getTotalPages(long total) {
        return Math.max(1,
            (int) Math.ceil((double) total / PAGE_SIZE));
    }

    public List<String> getDatasets() {
        return repository.findDatasets();
    }

    public Map<String, Object> getIndicator(long id) {
        return repository.findById(id);
    }

    public List<Map<String, Object>> getSources(long id) {
        return repository.findSources(id);
    }

    public List<Map<String, Object>> getCoverage(long id) {
        return repository.findGeographicCoverage(id, 15);
    }

    public List<Map<String, Object>> getDatasetSummary() {
        return repository.findDatasetSummary();
    }
}
