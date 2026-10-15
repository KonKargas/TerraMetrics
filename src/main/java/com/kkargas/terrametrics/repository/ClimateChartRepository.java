
package com.kkargas.terrametrics.repository;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ClimateChartRepository {

    private final JdbcTemplate jdbc;

    public ClimateChartRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findCountries() {
        return jdbc.queryForList("""
            SELECT
                g.EntityID AS id,
                COALESCE(c.Display_Name, g.Name) AS name
            FROM geographic_entities g
            JOIN countries c ON c.EntityID = g.EntityID
            ORDER BY name
            """);
    }

    public List<String> findDatasets() {
        return jdbc.queryForList("""
            SELECT DISTINCT Dataset
            FROM indicators
            ORDER BY Dataset
            """, String.class);
    }

    public List<Map<String, Object>> findIndicators(
            String dataset) {

        return jdbc.queryForList("""
            SELECT
                i.IndicatorID AS id,
                i.Value AS name,
                u.Value AS unit
            FROM indicators i
            JOIN units u ON u.UnitID = i.UnitID
            WHERE i.Dataset = ?
            ORDER BY i.Value
            """, dataset);
    }

    public List<Map<String, Object>> findChartData(
            long entityId,
            long indicatorId,
            int startYear,
            int endYear,
            int period) {

        return jdbc.queryForList("""
            SELECT
                CAST(r.Year / ? AS INTEGER) * ?
                    AS year,
                AVG(r.Value) AS value,
                COUNT(*) AS observations
            FROM climate_records r
            WHERE r.EntityID = ?
              AND r.IndicatorID = ?
              AND r.Year BETWEEN ? AND ?
            GROUP BY CAST(r.Year / ? AS INTEGER)
            ORDER BY year
            """,
            period, period,
            entityId, indicatorId,
            startYear, endYear,
            period);
    }

    public Map<String, Object> findIndicator(
            long indicatorId) {

        List<Map<String, Object>> result =
            jdbc.queryForList("""
                SELECT
                    i.IndicatorID AS id,
                    i.Value AS name,
                    i.Dataset AS dataset,
                    u.Value AS unit
                FROM indicators i
                JOIN units u ON u.UnitID = i.UnitID
                WHERE i.IndicatorID = ?
                """, indicatorId);

        return result.isEmpty() ? null : result.get(0);
    }

    public boolean countryExists(long entityId) {
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM countries
            WHERE EntityID = ?
            """, Integer.class, entityId);

        return count != null && count > 0;
    }
}
