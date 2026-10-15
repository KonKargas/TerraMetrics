package com.kkargas.terrametrics.etl;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Creates a new SQLite DB from generated reference CSVs and unified climate records. */
public final class CsvToDBImporter {
    private static final Path DB = Paths.get("data/climate.db");
    private static final String[][] TABLES = {
        {"geographic_entities","Tf_Geographic_Entities.csv"},
        {"countries","Tf_Countries_Data.csv"},
        {"units","Tf_Unit_Data.csv"}, {"ctss","Tf_Cts_Data.csv"},
        {"sources","Tf_Source_Data.csv"}, {"climate_influences","Tf_Climate_Influence_Data.csv"},
        {"indicators","Tf_Indicator_Data.csv"}, {"climate_records","Tf_Climate_Records.csv"}
    };
    public static void main(String[] args)throws Exception{importCsv();}
    public static void importCsv()throws Exception {
        Files.createDirectories(DB.getParent());
        Path temp=DB.resolveSibling("climate-import.tmp.db");Files.deleteIfExists(temp);
        try(Connection conn=DriverManager.getConnection("jdbc:sqlite:"+temp.toAbsolutePath())) {
            try(Statement s=conn.createStatement()){s.execute("PRAGMA foreign_keys=ON");s.execute("PRAGMA busy_timeout=10000");}
            conn.setAutoCommit(false);
            try {
                TableInitializationExecutor.createTables(conn);
                for(String[] t:TABLES) load(conn,t[0],ClimateEtl.OUT.resolve(t[1]));
                try(Statement s=conn.createStatement();ResultSet r=s.executeQuery("PRAGMA foreign_key_check")) {
                    if(r.next())throw new SQLException("Foreign key validation failed in "+r.getString(1));
                }
                conn.commit();
            }catch(Exception e){conn.rollback();throw e;}
        }catch(Exception e){Files.deleteIfExists(temp);throw e;}
        Files.move(temp,DB,StandardCopyOption.REPLACE_EXISTING);
        System.out.println("SQLite successfully loaded at " + DB.toAbsolutePath());
    }
    private static void load(Connection conn,String table,Path file)throws Exception {
        try(ClimateEtl.CsvReader r=new ClimateEtl.CsvReader(file,StandardCharsets.UTF_8)) {
            String[] columns=r.next();if(columns==null)throw new IllegalStateException("Empty CSV "+file);
            String sql="INSERT INTO \""+table+"\" ("+String.join(",",Arrays.stream(columns).map(c->"\""+c.replace("\"","\"\"")+"\"").toList())+") VALUES ("+String.join(",", Collections.nCopies(columns.length,"?"))+")";
            try(PreparedStatement p=conn.prepareStatement(sql)) {
                int count=0;String[] values;
                while((values=r.next())!=null) {
                    if(values.length!=columns.length)throw new IllegalStateException("Bad column count in "+file+" line "+r.line());
                    for(int i=0;i<values.length;i++) {
                        if(values[i].isEmpty())p.setNull(i+1,Types.NULL);
                        else p.setString(i+1,values[i]);
                    }
                    p.addBatch(); count++;
                    if(count%1000==0)p.executeBatch();
                }
                p.executeBatch();System.out.println(table+": "+count+" rows");
            }
        }
    }
}
