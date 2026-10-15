
package com.kkargas.terrametrics.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.kkargas.terrametrics.repository.CountriesRepository;

@Service
public class CountriesService {

    private static final int PAGE_SIZE = 20;

    private final CountriesRepository repository;

    public CountriesService(
            CountriesRepository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> getCountries(
            String search, String continent, int page) {

        int offset = (page - 1) * PAGE_SIZE;

        return repository.findCountries(
            search, continent, PAGE_SIZE, offset);
    }

    public long getTotalCountries(
            String search, String continent) {
        return repository.countCountries(
            search, continent);
    }

    public int getTotalPages(long total) {
        return Math.max(1,
            (int) Math.ceil(
                (double) total / PAGE_SIZE));
    }

    public List<String> getContinents() {
        return repository.findContinents();
    }

    public Map<String, Object> getCountry(long id) {
        return repository.findById(id);
    }

    public Map<String, Object> getStatistics(long id) {
        return repository.countryStatistics(id);
    }

    public List<Map<String, Object>> getDatasets(long id) {
        return repository.countryDatasets(id);
    }
}
