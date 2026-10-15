package com.kkargas.terrametrics.etl;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** Deterministic, DB-independent transformation of IMF datasets into a normalized CSV model. */
public final class ClimateEtl {
    private ClimateEtl() {}
    public static final Path DATA = Paths.get("src/main/resources/data");
    public static final Path OUT = Paths.get("src/main/resources/tf_data");
    public static final String[] INPUTS = {
        "Annual_Surface_Temperature_Change.csv", "Climate-related_Disasters_Frequency.csv",
        "Land_Cover_Accounts.csv", "Forest_and_Carbon.csv"
    };
    private static final String[] TYPES = {"TEMPERATURE", "DISASTERS", "LAND_COVER", "FOREST_CARBON"};
    private static final String[] OUTPUTS = {"Tf_Temperature_Data.csv", "Tf_Climate_Disasters_Data.csv", "Tf_Land_Cover_Data.csv", "Tf_Forest_Carbon_Data.csv"};
    private static final Pattern YEAR = Pattern.compile("F(\\d{4})");

    static final class Entity {
        final int id; final String iso3, iso2, name, kind; final String[] country;
        Entity(int id, String iso3, String iso2, String name, String kind, String[] country) {
            this.id=id; this.iso3=iso3; this.iso2=iso2; this.name=name; this.kind=kind; this.country=country;
        }
    }
    static final class Lookup {
        final LinkedHashMap<String,Integer> ids = new LinkedHashMap<>();
        int id(String key) { return ids.computeIfAbsent(key, k -> ids.size()+1); }
    }
    private static String clean(String s) { return s == null ? "" : s.strip().replace("\uFEFF", ""); }
    private static String get(Map<String,Integer> h, String[] row, String col) {
        Integer at = h.get(col); return at == null || at >= row.length ? "" : clean(row[at]);
    }
    private static Map<String,Integer> headers(String[] row) {
        Map<String,Integer> result = new LinkedHashMap<>();
        for (int i=0; i<row.length; i++) result.put(clean(row[i]), i);
        return result;
    }
    private static String key(String... fields) { return String.join("\u001F", fields); }
    private static String[] parts(String k) { return k.split("\u001F", -1); }

    /** Full run is required because all dimension IDs are allocated consistently across inputs. */
    public static void main(String[] args) throws Exception { transform(); }
    public static void transform() throws Exception {
        Files.createDirectories(OUT);
        LinkedHashMap<String,Entity> entities = new LinkedHashMap<>();
        try (CsvReader in=new CsvReader(DATA.resolve("Countries.csv"), StandardCharsets.ISO_8859_1)) {
            String[] head=in.next(); if(head==null) throw new IOException("Empty countries CSV");
            Map<String,Integer> h=headers(head); String[] row;
            while((row=in.next())!=null) {
                if (row.length!=head.length) throw new IOException("Bad countries column count");
                String iso3=get(h,row,"ISO3").toUpperCase(Locale.ROOT);
                if (iso3.isBlank()) throw new IOException("Country has empty ISO3");
                if(entities.containsKey(iso3)) throw new IOException("Duplicate ISO3 in Countries.csv: " + iso3);
                entities.put(iso3,new Entity(entities.size()+1,iso3,get(h,row,"ISO"),get(h,row,"Display_Name"),"COUNTRY",row.clone()));
            }
        }
        Lookup indicators=new Lookup(), units=new Lookup(), sources=new Lookup(), ctss=new Lookup(), influences=new Lookup();
        List<List<String[]>> output=new ArrayList<>();
        long missing=0, valid=0;
        for(int file=0;file<INPUTS.length;file++) {
            List<String[]> records = new ArrayList<>(); output.add(records);
            Set<String> unique = new HashSet<>();
            try(CsvReader in=new CsvReader(DATA.resolve(INPUTS[file]),StandardCharsets.UTF_8)) {
                String[] header=in.next(); if(header==null)throw new IOException("Empty file: "+ INPUTS[file]);
                Map<String,Integer> h=headers(header);
                for(String required : List.of("Country","ISO2","ISO3","Indicator","Unit","Source","CTS_Code","CTS_Name","CTS_Full_Descriptor"))
                    if(!h.containsKey(required))throw new IOException("Missing column "+required+" in "+INPUTS[file]);
                List<Integer> ys=new ArrayList<>();
                for(int j=0;j<header.length;j++) if(YEAR.matcher(clean(header[j])).matches()) ys.add(j);
                if(ys.isEmpty()) throw new IOException("No year columns in "+INPUTS[file]);
                String[] row;
                while((row=in.next())!=null) {
                    if(row.length!=header.length)throw new IOException("Column count mismatch in "+INPUTS[file]+" line " + in.line());
                    String iso3=get(h,row,"ISO3").toUpperCase(Locale.ROOT), country=get(h,row,"Country");
                    if(iso3.isBlank())throw new IOException("Missing ISO3 in "+INPUTS[file]+" line " + in.line());
                    Entity entity=entities.get(iso3);
                    if(entity==null) {
                        entity=new Entity(entities.size()+1,iso3,get(h,row,"ISO2"),country,"AGGREGATE_OR_UNMATCHED",null);
                        entities.put(iso3,entity);
                    }
                    String indicator=get(h,row,"Indicator"), unit=get(h,row,"Unit"), source=get(h,row,"Source");
                    if(indicator.isEmpty() || unit.isEmpty() || source.isEmpty()) throw new IOException("Blank indicator/unit/source in "+INPUTS[file]);
                    String cts=key(get(h,row,"CTS_Code"),get(h,row,"CTS_Name"),get(h,row,"CTS_Full_Descriptor"));
                    String influence=get(h,row,"Climate_Influence");
                    int uid=units.id(unit), cid=ctss.id(cts), sid=sources.id(source);
                    int iid=indicators.id(key(TYPES[file],indicator,String.valueOf(uid),String.valueOf(cid)));
                    int fid=influence.isEmpty()?0:influences.id(influence);
                    for(int j:ys) {
                        String value=clean(row[j]);
                        if(value.isBlank() || value.equalsIgnoreCase("NA") || value.equalsIgnoreCase("N/A")) {missing++;continue;}
                        final double n;
                        try { n=Double.parseDouble(value); }
                        catch(NumberFormatException e){throw new IOException("Invalid number '"+value+"' in "+INPUTS[file]+" at line "+in.line(),e);}
                        if(!Double.isFinite(n))throw new IOException("Non-finite value in "+INPUTS[file]);
                        String year=YEAR.matcher(clean(header[j])).replaceAll("$1");
                        String natural=key(String.valueOf(entity.id), String.valueOf(iid),String.valueOf(sid),String.valueOf(fid),year);
                        if(!unique.add(natural))throw new IOException("Duplicate measurement key in "+INPUTS[file]+" at line "+in.line()+" key="+natural.replace('\u001F','|'));
                        records.add(new String[]{String.valueOf(records.size()+1),String.valueOf(entity.id),String.valueOf(iid),String.valueOf(sid),fid==0?"":String.valueOf(fid),year,value});
                        valid++;
                    }
                }
            }
        }
        // Write only AFTER all inputs were validated (no half-complete outputs on parse error).
        try(CsvWriter w=new CsvWriter(OUT.resolve("Tf_Geographic_Entities.csv"))) {
            w.write("EntityID","ISO3","ISO2","Name","EntityType");
            for(Entity e:entities.values())w.write(String.valueOf(e.id),e.iso3,e.iso2,e.name,e.kind);
        }
        try(CsvWriter w=new CsvWriter(OUT.resolve("Tf_Countries_Data.csv"))) {
            w.write("EntityID","ISO","ISO3","ISO_Code","FIPS","Display_Name","Official_Name","Capital","Continent","CurrencyCode","CurrencyName","Phone","Region Code","Region Name","Sub-region Code","Sub-region Name","Intermediate Region Code","Intermediate Region Name","Status","Developed or Developing","Small Island Developing States (SIDS)","Land Locked Developing Countries (LLDC)","Least Developed Countries (LDC)","Area_SqKm","Population");
            for(Entity e:entities.values())if(e.country!=null) {
                String[] r=new String[e.country.length+1];r[0]=String.valueOf(e.id);System.arraycopy(e.country,0,r,1,e.country.length);w.write(r);
            }
        }
        writeSimple("Tf_Unit_Data.csv","UnitID","Value",units);
        writeSimple("Tf_Source_Data.csv","SourceID","Value",sources);
        writeSimple("Tf_Climate_Influence_Data.csv","InfluenceID","Value",influences);
        try(CsvWriter w=new CsvWriter(OUT.resolve("Tf_Cts_Data.csv"))) {
            w.write("CTS_ID","CTS_Code","CTS_Name","CTS_Full_Descriptor");
            for(var e:ctss.ids.entrySet()) {String[] p=parts(e.getKey());w.write(String.valueOf(e.getValue()),p[0],p[1],p[2]);}
        }
        try(CsvWriter w=new CsvWriter(OUT.resolve("Tf_Indicator_Data.csv"))) {
            w.write("IndicatorID","Dataset","Value","UnitID","CTS_ID");
            for(var e:indicators.ids.entrySet()) {String[] p=parts(e.getKey());w.write(String.valueOf(e.getValue()),p[0],p[1],p[2],p[3]);}
        }
        try(CsvWriter w=new CsvWriter(OUT.resolve("Tf_Climate_Records.csv"))) {
            w.write("RecordID","EntityID","IndicatorID","SourceID","InfluenceID","Year","Value");
            int next=1;
            for(int i=0;i<output.size();i++) {
                try(CsvWriter part=new CsvWriter(OUT.resolve(OUTPUTS[i]))) {
                    part.write("RecordID","EntityID","IndicatorID","SourceID","InfluenceID","Year","Value");
                    for(String[] r:output.get(i)) {part.write(r); String[] joined=r.clone(); joined[0]=String.valueOf(next++);w.write(joined);}
                }
            }
        }
        System.out.printf(Locale.ROOT,"ETL success: %d geographic entities, %d indicators, %d valid records, %d missing cells skipped.%n",entities.size(),indicators.ids.size(),valid,missing);
    }
    private static void writeSimple(String filename,String idName,String valueName,Lookup lookup)throws IOException {
        try(CsvWriter w=new CsvWriter(OUT.resolve(filename))) {w.write(idName,valueName);for(var e:lookup.ids.entrySet())w.write(String.valueOf(e.getValue()),e.getKey());}
    }

    /** RFC4180-style CSV reader: handles embedded newlines, doubled quotes, and BOM. */
    static final class CsvReader implements AutoCloseable {
        private final PushbackReader r; private long line=1;
        CsvReader(Path p,Charset charset)throws IOException {r=new PushbackReader(new BufferedReader(new InputStreamReader(Files.newInputStream(p),charset)),2);}
        long line(){return line;}
        String[] next()throws IOException {
            List<String> row=new ArrayList<>();StringBuilder field=new StringBuilder();boolean quoted=false,started=false,closed=false;
            while(true) {
                int ci=r.read();
                if(ci==-1) {if(quoted)throw new IOException("Unclosed CSV quotation at line "+line);if(!started&&row.isEmpty()&&field.length()==0)return null;row.add(field.toString());return row.toArray(String[]::new);}
                char c=(char)ci; started=true;
                if(quoted) {
                    if(c=='"') {int next=r.read();if(next=='"')field.append('"');else {quoted=false;closed=true;if(next!=-1)r.unread(next);}}
                    else {field.append(c);if(c=='\n')line++;}
                } else if(c==',' ){row.add(field.toString());field.setLength(0);closed=false;}
                else if(c=='\n'){line++;row.add(field.toString());return row.toArray(String[]::new);}
                else if(c=='\r'){int next=r.read();if(next!='\n'&&next!=-1)r.unread(next);line++;row.add(field.toString());return row.toArray(String[]::new);}
                else if(c=='"'&&field.length()==0&&!closed){quoted=true;}
                else if(closed)throw new IOException("Unexpected text after quoted CSV field at line "+line);
                else field.append(c);
            }
        }
        public void close()throws IOException{r.close();}
    }
    static final class CsvWriter implements AutoCloseable {
        final BufferedWriter w;
        CsvWriter(Path p)throws IOException {w=Files.newBufferedWriter(p,StandardCharsets.UTF_8);}
        void write(String... cells)throws IOException {for(int i=0;i<cells.length;i++){if(i>0)w.write(',');String s=cells[i]==null?"":cells[i];if(s.contains(",")||s.contains("\"")||s.contains("\n")||s.contains("\r")){w.write('"');w.write(s.replace("\"","\"\""));w.write('"');}else w.write(s);}w.newLine();}
        public void close()throws IOException{w.close();}
    }
}
