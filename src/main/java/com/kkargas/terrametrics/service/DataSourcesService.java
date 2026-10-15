package com.kkargas.terrametrics.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.kkargas.terrametrics.repository.DataSourcesRepository;

@Service
public class DataSourcesService {
    private final DataSourcesRepository repository;
    public DataSourcesService(DataSourcesRepository repository) { this.repository = repository; }
    public Map<String, Object> totals() { return repository.findTotals(); }
    public List<Map<String, Object>> datasets() { return repository.findDatasets(); }
    public List<Map<String, Object>> sources() { return repository.findSources(); }
    public List<Map<String, Object>> sourceDatasets() { return repository.findSourceDatasets(); }
    public List<Map<String, Object>> integrityChecks() { return repository.findIntegrityChecks(); }
}
