
package com.kkargas.terrametrics.repository;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IndicatorsRepository {

    private final JdbcTemplate jdbc;

    public IndicatorsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findIndicators(
            String search, String dataset,
            int limit, int offset) {

        return jdbc.queryForList("""
            SELECT
                i.IndicatorID AS id,
                i.Value AS name,
                i.Dataset AS dataset,
                u.Value AS unit,
                c.CTS_Code AS ctsCode,
                c.CTS_Name AS ctsName,
                COUNT(r.RecordID) AS observations,
                COUNT(DISTINCT r.EntityID) AS entities,
                MIN(r.Year) AS firstYear,
                MAX(r.Year) AS lastYear
            FROM indicators i
            JOIN units u ON u.UnitID = i.UnitID
            JOIN ctss c ON c.CTS_ID = i.CTS_ID
            LEFT JOIN climate_records r
                ON r.IndicatorID = i.IndicatorID
            WHERE
                (? = '' OR LOWER(i.Value)
                    LIKE '%' || LOWER(?) || '%'
                    OR LOWER(c.CTS_Name)
                    LIKE '%' || LOWER(?) || '%')
                AND (? = '' OR i.Dataset = ?)
            GROUP BY i.IndicatorID
            ORDER BY i.Dataset, i.Value
            LIMIT ? OFFSET ?
            """,
            search, search, search,
            dataset, dataset, limit, offset);
    }

    public long countIndicators(String search, String dataset) {
        Long result = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM indicators i
            JOIN ctss c ON c.CTS_ID = i.CTS_ID
            WHERE
                (? = '' OR LOWER(i.Value)
                    LIKE '%' || LOWER(?) || '%'
                    OR LOWER(c.CTS_Name)
                    LIKE '%' || LOWER(?) || '%')
                AND (? = '' OR i.Dataset = ?)
            """,
            Long.class,
            search, search, search, dataset, dataset);

        return result == null ? 0L : result;
    }

    public List<String> findDatasets() {
        return jdbc.queryForList("""
            SELECT DISTINCT Dataset
            FROM indicators
            ORDER BY Dataset
            """, String.class);
    }

    public Map<String, Object> findById(long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT
                i.IndicatorID AS id,
                i.Value AS name,
                i.Dataset AS dataset,
                u.Value AS unit,
                c.CTS_Code AS ctsCode,
                c.CTS_Name AS ctsName,
                c.CTS_Full_Descriptor AS description,
                COUNT(r.RecordID) AS observations,
                COUNT(DISTINCT r.EntityID) AS entities,
                MIN(r.Year) AS firstYear,
                MAX(r.Year) AS lastYear
            FROM indicators i
            JOIN units u ON u.UnitID = i.UnitID
            JOIN ctss c ON c.CTS_ID = i.CTS_ID
            LEFT JOIN climate_records r
                ON r.IndicatorID = i.IndicatorID
            WHERE i.IndicatorID = ?
            GROUP BY i.IndicatorID
            """, id);

        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Map<String, Object>> findSources(long id) {
        return jdbc.queryForList("""
            SELECT
                s.Value AS name,
                COUNT(*) AS observations
            FROM climate_records r
            JOIN sources s ON s.SourceID = r.SourceID
            WHERE r.IndicatorID = ?
            GROUP BY s.SourceID
            ORDER BY observations DESC
            """, id);
    }

    public List<Map<String, Object>> findGeographicCoverage(
            long id, int limit) {

        return jdbc.queryForList("""
            SELECT
                g.EntityID AS id,
                g.Name AS name,
                g.ISO3 AS iso3,
                COUNT(*) AS observations,
                MIN(r.Year) AS firstYear,
                MAX(r.Year) AS lastYear
            FROM climate_records r
            JOIN geographic_entities g
                ON g.EntityID = r.EntityID
            WHERE r.IndicatorID = ?
            GROUP BY g.EntityID
            ORDER BY observations DESC, g.Name
            LIMIT ?
            """, id, limit);
    }

    public List<Map<String, Object>> findDatasetSummary() {
        return jdbc.queryForList("""
            SELECT
                i.Dataset AS dataset,
                COUNT(DISTINCT i.IndicatorID) AS indicators,
                COUNT(r.RecordID) AS observations
            FROM indicators i
            LEFT JOIN climate_records r
                ON r.IndicatorID = i.IndicatorID
            GROUP BY i.Dataset
            ORDER BY i.Dataset
            """);
    }
}
