
package com.kkargas.terrametrics.repository;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CountriesRepository {

    private final JdbcTemplate jdbc;

    public CountriesRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findCountries(
            String search,
            String continent,
            int limit,
            int offset) {

        String sql = """
            SELECT
                c.EntityID AS id,
                COALESCE(c.Display_Name, g.Name) AS name,
                g.ISO3 AS iso3,
                c.Continent AS continent,
                c.Population AS population,
                c.Capital AS capital
            FROM countries c
            JOIN geographic_entities g
                ON g.EntityID = c.EntityID
            WHERE (
                ? = ''
                OR LOWER(COALESCE(
                    c.Display_Name, g.Name
                )) LIKE '%' || LOWER(?) || '%'
                OR LOWER(g.ISO3) LIKE
                    '%' || LOWER(?) || '%'
            )
            AND (
                ? = ''
                OR c.Continent = ?
            )
            ORDER BY name
            LIMIT ? OFFSET ?
            """;

        return jdbc.queryForList(
            sql,
            search, search, search,
            continent, continent,
            limit, offset
        );
    }

    public long countCountries(
            String search,
            String continent) {

        String sql = """
            SELECT COUNT(*)
            FROM countries c
            JOIN geographic_entities g
                ON g.EntityID = c.EntityID
            WHERE (
                ? = ''
                OR LOWER(COALESCE(
                    c.Display_Name, g.Name
                )) LIKE '%' || LOWER(?) || '%'
                OR LOWER(g.ISO3) LIKE
                    '%' || LOWER(?) || '%'
            )
            AND (
                ? = ''
                OR c.Continent = ?
            )
            """;

        Long count = jdbc.queryForObject(
            sql, Long.class,
            search, search, search,
            continent, continent
        );

        return count == null ? 0L : count;
    }

    public List<String> findContinents() {

        return jdbc.queryForList("""
            SELECT DISTINCT Continent
            FROM countries
            WHERE Continent IS NOT NULL
              AND TRIM(Continent) <> ''
            ORDER BY Continent
            """, String.class);
    }

    public Map<String, Object> findById(long id) {

        List<Map<String, Object>> rows =
            jdbc.queryForList("""
                SELECT
                    c.EntityID AS id,
                    COALESCE(c.Display_Name, g.Name)
                        AS name,
                    g.ISO3 AS iso3,
                    g.ISO2 AS iso2,
                    c.Official_Name AS officialName,
                    c.Capital AS capital,
                    c.Continent AS continent,
                    c.Population AS population,
                    c.Area_SqKm AS area,
                    c.CurrencyName AS currency,
                    c.Status AS status
                FROM countries c
                JOIN geographic_entities g
                    ON g.EntityID = c.EntityID
                WHERE c.EntityID = ?
                """, id);

        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> countryStatistics(
            long id) {

        return jdbc.queryForMap("""
            SELECT
                COUNT(*) AS totalRecords,
                COUNT(DISTINCT IndicatorID)
                    AS totalIndicators,
                MIN(Year) AS firstYear,
                MAX(Year) AS lastYear
            FROM climate_records
            WHERE EntityID = ?
            """, id);
    }

    public List<Map<String, Object>> countryDatasets(
            long id) {

        return jdbc.queryForList("""
            SELECT
                i.Dataset AS dataset,
                COUNT(*) AS total
            FROM climate_records r
            JOIN indicators i
                ON i.IndicatorID = r.IndicatorID
            WHERE r.EntityID = ?
            GROUP BY i.Dataset
            ORDER BY i.Dataset
            """, id);
    }
}
