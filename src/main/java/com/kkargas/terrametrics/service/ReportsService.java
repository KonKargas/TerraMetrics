package com.kkargas.terrametrics.service;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.kkargas.terrametrics.repository.ReportsRepository;

@Service
public class ReportsService {
    private final ReportsRepository repo;
    public ReportsService(ReportsRepository repo) { this.repo=repo; }
    public List<Map<String,Object>> countries() { return repo.countries(); }
    public List<String> datasets() { return repo.datasets(); }
    public List<Map<String,Object>> indicators(String dataset) { return repo.indicators(dataset); }

    public Map<String,Object> report(List<Long> ids,long indicatorId,int start,int end,int period,String aggregation) {
        if (ids == null || ids.isEmpty() || ids.size()>5 || ids.stream().anyMatch(id->id==null||id<=0)
            || ids.stream().distinct().count()!=ids.size()) bad("Choose 1 to 5 different countries.");
        if (start<1800 || end>2200 || start>end) bad("Invalid year range.");
        if (period!=1 && period!=5 && period!=10) bad("Invalid period.");
        if (!List.of("AVG","SUM").contains(aggregation)) bad("Invalid aggregation.");
        Map<String,Object> indicator=repo.indicator(indicatorId);
        if (indicator==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Indicator not found");
        List<Map<String,Object>> countries=repo.selectedCountries(ids);
        if (countries.size()!=ids.size()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Country not found");
        Map<Long,String> names=countries.stream().collect(Collectors.toMap(
            c->((Number)c.get("id")).longValue(),c->String.valueOf(c.get("name"))));
        List<Map<String,Object>> rows=new ArrayList<>();
        for (var source:repo.measurements(ids,indicatorId,start,end,period,aggregation)) {
            long entityId=((Number)source.get("entityId")).longValue();
            int bucket=((Number)source.get("periodStart")).intValue();
            int left=Math.max(bucket,start),right=Math.min(bucket+period-1,end);
            int covered=((Number)source.get("coveredYears")).intValue();
            Map<String,Object> row=new LinkedHashMap<>();
            row.put("country",names.get(entityId)); row.put("entityId",entityId);
            row.put("periodStart",left); row.put("periodEnd",right);
            row.put("value",source.get("value")); row.put("observations",source.get("observations"));
            row.put("coveredYears",covered); row.put("expectedYears",right-left+1);
            row.put("complete",covered==right-left+1); rows.add(row);
        }
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("indicator",indicator); result.put("countries",countries);
        result.put("startYear",start); result.put("endYear",end);
        result.put("period",period); result.put("aggregation",aggregation);
        result.put("rows",rows); result.put("rowCount",rows.size()); return result;
    }
    private void bad(String reason) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST,reason);}
}
