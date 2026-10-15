package com.kkargas.terrametrics.repository;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ClimateMapRepository {
    private final JdbcTemplate jdbc;
    public ClimateMapRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<String> datasets() {
        return jdbc.queryForList("SELECT DISTINCT Dataset FROM indicators ORDER BY Dataset", String.class);
    }

    public List<Map<String,Object>> indicators(String dataset) {
        return jdbc.queryForList("""
            SELECT i.IndicatorID AS id, i.Value AS name, u.Value AS unit
            FROM indicators i JOIN units u ON u.UnitID = i.UnitID
            WHERE i.Dataset = ? ORDER BY i.Value
            """, dataset);
    }

    public List<Map<String,Object>> years(long indicatorId) {
        return jdbc.queryForList("""
            SELECT DISTINCT r.Year AS year FROM climate_records r
            WHERE r.IndicatorID = ? ORDER BY r.Year DESC
            """, indicatorId);
    }

    public List<Map<String,Object>> values(long indicatorId, int year, String aggregation) {
        String fn = aggregation.equals("SUM") ? "SUM" : "AVG"; // strictly validated by service
        return jdbc.queryForList("""
            SELECT c.EntityID AS entityId, c.ISO_Code AS isoNumeric,
                   COALESCE(c.Display_Name, g.Name) AS country,
                   g.ISO3 AS iso3, 
            """ + fn + """ 
                   (r.Value) AS value, COUNT(*) AS observations
            FROM climate_records r
            JOIN countries c ON c.EntityID = r.EntityID
            JOIN geographic_entities g ON g.EntityID = c.EntityID
            WHERE r.IndicatorID = ? AND r.Year = ?
              AND c.ISO_Code IS NOT NULL AND c.ISO_Code > 0
            GROUP BY c.EntityID, c.ISO_Code, c.Display_Name, g.Name, g.ISO3
            ORDER BY c.EntityID
            """, indicatorId, year);
    }

    public Map<String,Object> indicator(long indicatorId) {
        var rows = jdbc.queryForList("""
            SELECT i.IndicatorID AS id, i.Value AS name, i.Dataset AS dataset,
                   u.Value AS unit FROM indicators i
            JOIN units u ON u.UnitID = i.UnitID WHERE i.IndicatorID = ?
            """, indicatorId);
        return rows.isEmpty() ? null : rows.get(0);
    }
}
