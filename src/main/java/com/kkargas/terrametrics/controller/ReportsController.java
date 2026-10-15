package com.kkargas.terrametrics.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.kkargas.terrametrics.service.ReportsService;

@Controller
@RequestMapping("/reports")
public class ReportsController {
    private final ReportsService service;
    public ReportsController(ReportsService service) {this.service=service;}

    @GetMapping
    public String index(Model model) {
        model.addAttribute("countries",service.countries());
        model.addAttribute("datasets",service.datasets());
        return "reports";
    }
    @GetMapping("/api/indicators") @ResponseBody
    public List<Map<String,Object>> indicators(@RequestParam String dataset) {
        return service.indicators(dataset);
    }
    @GetMapping("/api/preview") @ResponseBody
    public Map<String,Object> preview(@RequestParam List<Long> countryIds,
        @RequestParam long indicatorId,@RequestParam int startYear,@RequestParam int endYear,
        @RequestParam(defaultValue="1")int period,@RequestParam(defaultValue="AVG")String aggregation) {
        return service.report(countryIds,indicatorId,startYear,endYear,period,aggregation);
    }
    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> csv(@RequestParam List<Long> countryIds,
        @RequestParam long indicatorId,@RequestParam int startYear,@RequestParam int endYear,
        @RequestParam(defaultValue="1")int period,@RequestParam(defaultValue="AVG")String aggregation) {
        Map<String,Object> report=service.report(countryIds,indicatorId,startYear,endYear,period,aggregation);
        Map<?,?> indicator=(Map<?,?>)report.get("indicator");
        StringBuilder out=new StringBuilder("Country,EntityID,PeriodStart,PeriodEnd,Indicator,Dataset,Unit,Calculation,Value,Observations,CoveredYears,ExpectedYears,Complete\r\n");
        for (Map<String,Object> row:rows(report)) {
            Object[] cols={row.get("country"),row.get("entityId"),row.get("periodStart"),row.get("periodEnd"),
                indicator.get("name"),indicator.get("dataset"),indicator.get("unit"),aggregation,
                row.get("value"),row.get("observations"),row.get("coveredYears"),row.get("expectedYears"),row.get("complete")};
            for (int i=0;i<cols.length;i++) {
                if (i>0) out.append(',');
                String value=cols[i]==null?"":cols[i].toString();
                if ((i==0||i==4||i==5||i==6) && !value.isEmpty() && "=+-@".indexOf(value.charAt(0))>=0)
                    value="'"+value;
                out.append('"').append(value.replace("\"","\"\"")).append('"');
            }
            out.append("\r\n");
        }
        byte[] bytes=("\uFEFF"+out).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=terrametrics-report.csv")
            .contentType(new MediaType("text","csv",StandardCharsets.UTF_8)).body(bytes);
    }
    @SuppressWarnings("unchecked")
    private static List<Map<String,Object>> rows(Map<String,Object> report) {
        return (List<Map<String,Object>>) report.get("rows");
    }
    @GetMapping("/print")
    public String print(@RequestParam List<Long> countryIds,
        @RequestParam long indicatorId,@RequestParam int startYear,@RequestParam int endYear,
        @RequestParam(defaultValue="1")int period,@RequestParam(defaultValue="AVG")String aggregation, Model model) {
        model.addAttribute("report",service.report(countryIds,indicatorId,startYear,endYear,period,aggregation));
        return "report-print";
    }
}
