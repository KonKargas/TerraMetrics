
package com.kkargas.terrametrics.repository;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ComparisonsRepository {

    private final JdbcTemplate jdbc;

    public ComparisonsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findCountries() {
        return jdbc.queryForList("""
            SELECT
                g.EntityID AS id,
                COALESCE(c.Display_Name, g.Name) AS name
            FROM countries c
            JOIN geographic_entities g
                ON g.EntityID = c.EntityID
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

    public Map<String, Object> findIndicator(long id) {
        List<Map<String, Object>> rows =
            jdbc.queryForList("""
                SELECT
                    i.IndicatorID AS id,
                    i.Value AS name,
                    i.Dataset AS dataset,
                    u.Value AS unit
                FROM indicators i
                JOIN units u ON u.UnitID = i.UnitID
                WHERE i.IndicatorID = ?
                """, id);

        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Map<String, Object>> findSelectedCountries(
            List<Long> ids) {

        if (ids.isEmpty()) return List.of();

        String placeholders =
            String.join(",", java.util.Collections.nCopies(
                ids.size(), "?"));

        return jdbc.queryForList("""
            SELECT
                g.EntityID AS id,
                COALESCE(c.Display_Name, g.Name) AS name
            FROM countries c
            JOIN geographic_entities g
                ON g.EntityID = c.EntityID
            WHERE g.EntityID IN (
            """ + placeholders + ") ORDER BY name",
            ids.toArray());
    }

    public List<Map<String, Object>> findComparison(
            List<Long> ids,
            long indicatorId,
            int startYear,
            int endYear,
            int period,
            String aggregation) {

        String placeholders =
            String.join(",", java.util.Collections.nCopies(
                ids.size(), "?"));

        // SQL function is selected only from a strict whitelist.
        String function = aggregation.equals("SUM")
                ? "SUM" : "AVG";

        String sql = """
            SELECT
                r.EntityID AS entityId,
                CAST(r.Year / ? AS INTEGER) * ? AS year,
            """ + function + """
                (r.Value) AS value,
                COUNT(*) AS observations
            FROM climate_records r
            WHERE r.EntityID IN (
            """ + placeholders + """
            )
              AND r.IndicatorID = ?
              AND r.Year BETWEEN ? AND ?
            GROUP BY
                r.EntityID,
                CAST(r.Year / ? AS INTEGER)
            ORDER BY year, entityId
            """;

        java.util.ArrayList<Object> params =
            new java.util.ArrayList<>();

        params.add(period);
        params.add(period);
        params.addAll(ids);
        params.add(indicatorId);
        params.add(startYear);
        params.add(endYear);
        params.add(period);

        return jdbc.queryForList(sql, params.toArray());
    }

    public List<Map<String, Object>> findCorrelation(
            long entityId,
            long indicatorX,
            long indicatorY,
            int startYear,
            int endYear) {

        // Aggregate each indicator separately to one point
        // per year, then match only overlapping years.
        return jdbc.queryForList("""
            WITH x_values AS (
                SELECT Year, AVG(Value) AS x
                FROM climate_records
                WHERE EntityID = ?
                  AND IndicatorID = ?
                  AND Year BETWEEN ? AND ?
                GROUP BY Year
            ),
            y_values AS (
                SELECT Year, AVG(Value) AS y
                FROM climate_records
                WHERE EntityID = ?
                  AND IndicatorID = ?
                  AND Year BETWEEN ? AND ?
                GROUP BY Year
            )
            SELECT
                x_values.Year AS year,
                x_values.x AS x,
                y_values.y AS y
            FROM x_values
            INNER JOIN y_values
                ON x_values.Year = y_values.Year
            ORDER BY x_values.Year
            """,
            entityId, indicatorX, startYear, endYear,
            entityId, indicatorY, startYear, endYear);
    }
}
