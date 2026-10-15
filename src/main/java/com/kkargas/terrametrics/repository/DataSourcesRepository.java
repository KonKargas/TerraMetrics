package com.kkargas.terrametrics.repository;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DataSourcesRepository {
    private final JdbcTemplate jdbc;
    public DataSourcesRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String, Object>> findSources() {
        return jdbc.queryForList("""
            SELECT s.SourceID AS id, s.Value AS citation,
                   COUNT(r.RecordID) AS observations,
                   COUNT(DISTINCT r.EntityID) AS entities,
                   COUNT(DISTINCT r.IndicatorID) AS indicators,
                   MIN(r.Year) AS firstYear, MAX(r.Year) AS lastYear
            FROM sources s
            LEFT JOIN climate_records r ON r.SourceID = s.SourceID
            GROUP BY s.SourceID, s.Value
            ORDER BY s.SourceID
            """);
    }

    public List<Map<String, Object>> findDatasets() {
        return jdbc.queryForList("""
            SELECT i.Dataset AS dataset,
                   COUNT(DISTINCT i.IndicatorID) AS indicators,
                   COUNT(r.RecordID) AS observations,
                   COUNT(DISTINCT r.EntityID) AS entities,
                   MIN(r.Year) AS firstYear, MAX(r.Year) AS lastYear
            FROM indicators i
            LEFT JOIN climate_records r ON r.IndicatorID = i.IndicatorID
            GROUP BY i.Dataset ORDER BY i.Dataset
            """);
    }

    public List<Map<String, Object>> findSourceDatasets() {
        return jdbc.queryForList("""
            SELECT r.SourceID AS sourceId, i.Dataset AS dataset,
                   COUNT(*) AS observations,
                   COUNT(DISTINCT r.IndicatorID) AS indicators
            FROM climate_records r
            JOIN indicators i ON i.IndicatorID = r.IndicatorID
            GROUP BY r.SourceID, i.Dataset
            ORDER BY r.SourceID, i.Dataset
            """);
    }

    public Map<String, Object> findTotals() {
        return jdbc.queryForMap("""
            SELECT (SELECT COUNT(*) FROM climate_records) AS observations,
                   (SELECT COUNT(*) FROM geographic_entities) AS entities,
                   (SELECT COUNT(*) FROM countries) AS countries,
                   (SELECT COUNT(*) FROM indicators) AS indicators,
                   (SELECT COUNT(*) FROM sources) AS sources,
                   (SELECT MIN(Year) FROM climate_records) AS firstYear,
                   (SELECT MAX(Year) FROM climate_records) AS lastYear
            """);
    }

    public List<Map<String, Object>> findIntegrityChecks() {
        return jdbc.queryForList("""
            SELECT 'Orphan geographic references' AS label, COUNT(*) AS failures
            FROM climate_records r LEFT JOIN geographic_entities g ON g.EntityID=r.EntityID
            WHERE g.EntityID IS NULL
            UNION ALL
            SELECT 'Orphan indicators', COUNT(*)
            FROM climate_records r LEFT JOIN indicators i ON i.IndicatorID=r.IndicatorID
            WHERE i.IndicatorID IS NULL
            UNION ALL
            SELECT 'Orphan sources', COUNT(*)
            FROM climate_records r LEFT JOIN sources s ON s.SourceID=r.SourceID
            WHERE s.SourceID IS NULL
            UNION ALL
            SELECT 'Invalid years', COUNT(*)
            FROM climate_records WHERE Year NOT BETWEEN 1800 AND 2200
            """);
    }
}
