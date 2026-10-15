package com.kkargas.terrametrics.repository;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReportsRepository {
    private final JdbcTemplate jdbc;
    public ReportsRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String,Object>> countries() {
        return jdbc.queryForList("""
            SELECT c.EntityID AS id, COALESCE(c.Display_Name,g.Name) AS name
            FROM countries c JOIN geographic_entities g ON g.EntityID=c.EntityID ORDER BY name
            """);
    }
    public List<String> datasets() {
        return jdbc.queryForList("SELECT DISTINCT Dataset FROM indicators ORDER BY Dataset", String.class);
    }
    public List<Map<String,Object>> indicators(String dataset) {
        return jdbc.queryForList("""
            SELECT i.IndicatorID AS id, i.Value AS name, u.Value AS unit
            FROM indicators i JOIN units u ON u.UnitID=i.UnitID
            WHERE i.Dataset=? ORDER BY i.Value
            """, dataset);
    }
    public Map<String,Object> indicator(long id) {
        var rows = jdbc.queryForList("""
            SELECT i.IndicatorID AS id, i.Value AS name, i.Dataset AS dataset, u.Value AS unit
            FROM indicators i JOIN units u ON u.UnitID=i.UnitID WHERE i.IndicatorID=?
            """, id);
        return rows.isEmpty() ? null : rows.get(0);
    }
    public List<Map<String,Object>> selectedCountries(List<Long> ids) {
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        return jdbc.queryForList("""
            SELECT c.EntityID AS id, COALESCE(c.Display_Name,g.Name) AS name
            FROM countries c JOIN geographic_entities g ON g.EntityID=c.EntityID
            WHERE c.EntityID IN (
            """ + placeholders + ") ORDER BY name", ids.toArray());
    }
    public List<Map<String,Object>> measurements(List<Long> ids, long indicatorId,
            int start, int end, int period, String aggregation) {
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String function = aggregation.equals("SUM") ? "SUM" : "AVG";
        String sql = """
            SELECT r.EntityID AS entityId,
                CAST(r.Year / ? AS INTEGER) * ? AS periodStart,
            """ + function + """
                (r.Value) AS value,
                COUNT(*) AS observations,
                COUNT(DISTINCT r.Year) AS coveredYears
            FROM climate_records r
            WHERE r.EntityID IN (
            """ + placeholders + """
            ) AND r.IndicatorID=? AND r.Year BETWEEN ? AND ?
            GROUP BY r.EntityID, CAST(r.Year / ? AS INTEGER)
            ORDER BY periodStart,r.EntityID
            """;
        List<Object> args = new ArrayList<>();
        args.add(period); args.add(period); args.addAll(ids);
        args.add(indicatorId); args.add(start); args.add(end); args.add(period);
        return jdbc.queryForList(sql,args.toArray());
    }
}
